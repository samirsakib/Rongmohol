package com.example.ui.screens.board

import com.example.domain.ai.AiDifficulty
import com.example.domain.game.DeckType
import com.example.domain.model.AppPreferences
import com.example.domain.repository.PreferencesRepository
import com.example.ui.screens.mode.GameModeId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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

@OptIn(ExperimentalCoroutinesApi::class)
class GameBoardViewModelTest {

  private val dispatcher = StandardTestDispatcher()

  private class FakePreferencesRepository : PreferencesRepository {
    private val state = MutableStateFlow(AppPreferences())
    override val preferences: Flow<AppPreferences> = state
    override suspend fun setSoundEnabled(enabled: Boolean) = Unit
    override suspend fun setHapticsEnabled(enabled: Boolean) = Unit
    override suspend fun setUseBengaliNumerals(enabled: Boolean) = Unit
    override suspend fun setDarkThemeEnabled(enabled: Boolean) = Unit
    override suspend fun setDynamicColorEnabled(enabled: Boolean) = Unit
    override suspend fun setSelectedAvatarId(avatarId: Int) = Unit
    override suspend fun setGuestSession(enabled: Boolean, displayName: String?) = Unit
  }

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `initMatch connects to real CardGameEngine and populates opponents, hand, draw pile, and timer`() = runTest(dispatcher) {
    val viewModel = GameBoardViewModel(FakePreferencesRepository())
    viewModel.initMatch(
      mode = GameModeId.TACTICAL,
      playerCount = 4,
      playerName = "সাকিব",
      avatarId = 0,
      difficulty = AiDifficulty.HARD,
    )

    // Advance coroutine for initial match setup without running infinite timer loop indefinitely
    dispatcher.scheduler.runCurrent()
    val state = viewModel.uiState.value

    assertEquals(DeckType.SPECIAL, state.deckType)
    assertEquals(3, state.opponents.size)
    assertEquals(7, state.playerHand.size)
    assertTrue(state.drawPileCount > 50)
    assertEquals(GameBoardUiState.TURN_DURATION_SECONDS, state.turnTimerSeconds)
    assertFalse(state.isPaused)

    // Toggle pause cleanly stops timer/bots
    viewModel.togglePause()
    assertTrue(viewModel.uiState.value.isPaused)
    viewModel.togglePause()
    assertFalse(viewModel.uiState.value.isPaused)

    // Drawing a card updates real hand count & draw pile count
    val handBeforeDraw = viewModel.uiState.value.playerHand.size
    val drawBefore = viewModel.uiState.value.drawPileCount
    viewModel.onDrawCardClicked()
    dispatcher.scheduler.runCurrent()

    assertEquals(handBeforeDraw + 1, viewModel.uiState.value.playerHand.size)
    assertEquals(drawBefore - 1, viewModel.uiState.value.drawPileCount)
    assertEquals(1, viewModel.uiState.value.drawPulseCount)

    // Cleanly pause so background loops settle for test teardown
    viewModel.togglePause()
    advanceUntilIdle()
  }
}
