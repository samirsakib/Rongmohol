package com.example.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.db.AppDatabase
import com.example.data.local.prefs.UserPreferencesDataSource
import com.example.data.repository.MatchHistoryRepositoryImpl
import com.example.data.repository.PreferencesRepositoryImpl
import com.example.domain.model.MatchRecord
import com.example.ui.screens.mode.GameModeId
import com.example.ui.screens.result.ResultViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomMatchHistoryIntegrationTest {

  private val dispatcher = StandardTestDispatcher()
  private lateinit var db: AppDatabase
  private lateinit var repository: MatchHistoryRepositoryImpl
  private lateinit var prefsRepository: PreferencesRepositoryImpl

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    repository = MatchHistoryRepositoryImpl(db.matchRecordDao())
    prefsRepository = PreferencesRepositoryImpl(UserPreferencesDataSource(context))
  }

  @After
  fun tearDown() {
    db.close()
    Dispatchers.resetMain()
  }

  @Test
  fun `offline Room persistence saves matches idempotently and updates summary totals`() = runTest(dispatcher) {
    val resultVm = ResultViewModel(repository, prefsRepository)
    resultVm.recordMatchOnce(
      matchKey = "CLASSIC_120_35_true",
      mode = GameModeId.CLASSIC,
      playerCount = 4,
      playerScore = 120,
      opponentScore = 35,
      isVictory = true,
    )
    // Duplicate composition call with same matchKey must not double-insert
    resultVm.recordMatchOnce(
      matchKey = "CLASSIC_120_35_true",
      mode = GameModeId.CLASSIC,
      playerCount = 4,
      playerScore = 120,
      opponentScore = 35,
      isVictory = true,
    )
    advanceUntilIdle()

    val recent = repository.observeRecentMatches(10).first()
    val summary = repository.observeSummary().first()
    assertEquals(1, recent.size)
    assertEquals(120, recent.first().playerScore)
    assertTrue(recent.first().isVictory)
    assertEquals(1, summary.totalMatches)
    assertEquals(1, summary.totalWins)

    repository.recordSampleMatch(
      MatchRecord(
        modeName = "ঝটপট চাল",
        opponentSummary = "২ জন প্রতিপক্ষ",
        playerScore = 24,
        opponentScore = 115,
        isVictory = false,
        playedAtEpochMs = System.currentTimeMillis(),
      )
    )
    val summaryAfterLoss = repository.observeSummary().first()
    assertEquals(2, summaryAfterLoss.totalMatches)
    assertEquals(1, summaryAfterLoss.totalWins)

    repository.clearAllMatches()
    assertEquals(0, repository.observeRecentMatches(10).first().size)
  }

  @Test
  fun `DataStore persists guest session and UI preferences reliably`() = runTest(dispatcher) {
    prefsRepository.setGuestSession(enabled = true, displayName = "রংপুর চ্যাম্পিয়ন")
    prefsRepository.setDarkThemeEnabled(true)
    prefsRepository.setUseBengaliNumerals(false)
    prefsRepository.setSelectedAvatarId(3)

    val prefs = prefsRepository.preferences.first()
    assertTrue(prefs.guestModeEnabled)
    assertEquals("রংপুর চ্যাম্পিয়ন", prefs.guestDisplayName)
    assertTrue(prefs.darkThemeEnabled)
    assertFalse(prefs.useBengaliNumerals)
    assertEquals(3, prefs.selectedAvatarId)
  }
}
