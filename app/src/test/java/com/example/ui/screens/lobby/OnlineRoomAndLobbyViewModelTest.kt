package com.example.ui.screens.lobby

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.config.UiState
import com.example.core.network.NetworkConnectivityObserver
import com.example.domain.game.CardColor
import com.example.domain.game.CardType
import com.example.domain.game.DeckType
import com.example.domain.game.GameCard
import com.example.domain.model.AppPreferences
import com.example.domain.model.CommandAck
import com.example.domain.model.CreatedRoomInfo
import com.example.domain.model.JoinedRoomInfo
import com.example.domain.model.LeaderboardEntry
import com.example.domain.model.OnlineMatchResultItem
import com.example.domain.model.OnlinePublicMatchState
import com.example.domain.model.OnlineRoomMeta
import com.example.domain.model.OnlineRoomPlayer
import com.example.domain.model.OnlineRoomSnapshot
import com.example.domain.model.OnlineRoomSummary
import com.example.domain.model.RoomStatus
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.AuthenticatedUser
import com.example.domain.repository.OnlineMultiplayerRepository
import com.example.domain.repository.PreferencesRepository
import com.example.ui.components.RongmohalSuit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OnlineRoomAndLobbyViewModelTest {

  private val testDispatcher = StandardTestDispatcher()

  private lateinit var fakeAuthRepository: FakeAuthRepository
  private lateinit var fakePrefsRepository: FakePreferencesRepository
  private lateinit var fakeMultiplayerRepository: FakeOnlineMultiplayerRepository
  private lateinit var networkObserver: NetworkConnectivityObserver

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    fakeAuthRepository = FakeAuthRepository(AuthenticatedUser("alice", "অরিত্র", "alice@test.dev"))
    fakePrefsRepository = FakePreferencesRepository()
    fakeMultiplayerRepository = FakeOnlineMultiplayerRepository()
    val context = ApplicationProvider.getApplicationContext<Context>()
    networkObserver = NetworkConnectivityObserver(context)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun lobbyViewModel_guestSession_exposesOfflineGuestState() = runTest {
    fakeAuthRepository.userFlow.value = null
    fakePrefsRepository.setGuestSession(true, "অতিথি বন্ধু")

    val vm = LobbyViewModel(
      multiplayerRepository = fakeMultiplayerRepository,
      authRepository = fakeAuthRepository,
      preferencesRepository = fakePrefsRepository,
      networkObserver = networkObserver,
    )
    val collectJob = launch { vm.uiState.collect {} }
    advanceUntilIdle()

    val success = vm.uiState.value as UiState.Success
    assertFalse(success.data.isOnlineAccount)
    assertEquals("অতিথি বন্ধু", success.data.displayName)
    collectJob.cancel()
  }

  @Test
  fun lobbyViewModel_createRoomAndJoinByCode_navigatesToRoom() = runTest {
    val vm = LobbyViewModel(
      multiplayerRepository = fakeMultiplayerRepository,
      authRepository = fakeAuthRepository,
      preferencesRepository = fakePrefsRepository,
      networkObserver = networkObserver,
    )
    val collectJob = launch { vm.uiState.collect {} }
    advanceUntilIdle()

    vm.createRoom(
      title = "বাংলা টেবিল",
      maxPlayers = 4,
      deckType = DeckType.SPECIAL,
      isPrivate = true,
      startingCards = 7,
    )
    advanceUntilIdle()
    assertEquals("room_created_1", vm.navigateToRoomId.value)
    assertEquals(DeckType.SPECIAL, fakeMultiplayerRepository.lastCreatedDeckType)
    vm.consumeNavigation()
    assertNull(vm.navigateToRoomId.value)

    vm.joinByCode("ab12cd")
    advanceUntilIdle()
    assertEquals("room_joined_1", vm.navigateToRoomId.value)
    assertEquals("AB12CD", fakeMultiplayerRepository.lastJoinCode)
    collectJob.cancel()
  }

  @Test
  fun onlineRoomViewModel_waitingRoomAndStartPermission() = runTest {
    val vm = OnlineRoomViewModel(
      multiplayerRepository = fakeMultiplayerRepository,
      authRepository = fakeAuthRepository,
      preferencesRepository = fakePrefsRepository,
    )
    fakeMultiplayerRepository.roomFlow.value = sampleSnapshot(
      status = RoomStatus.WAITING,
      aliceReady = true,
      bobReady = false,
      myHand = emptyList(),
    )

    vm.bindRoom("room_1")
    advanceUntilIdle()

    val state1 = (vm.uiState.value as UiState.Success).data
    assertTrue(state1.isHost)
    assertFalse(state1.canHostStartMatch)

    fakeMultiplayerRepository.roomFlow.value = sampleSnapshot(
      status = RoomStatus.WAITING,
      aliceReady = true,
      bobReady = true,
      myHand = emptyList(),
    )
    advanceUntilIdle()

    val state2 = (vm.uiState.value as UiState.Success).data
    assertTrue(state2.canHostStartMatch)

    vm.startMatch()
    advanceUntilIdle()
    assertEquals("room_1", fakeMultiplayerRepository.lastStartedRoomId)
  }

  @Test
  fun onlineRoomViewModel_validatesIllegalCardAndSubmitsPlayWithVersionAndUniqueActionId() = runTest {
    val red5 = GameCard.NumberCard("c_r5", CardColor.RED, 5)
    val blue9 = GameCard.NumberCard("c_b9", CardColor.BLUE, 9)
    val wildCard = GameCard.ActionCard("c_wild", CardColor.WILD, CardType.WILD)

    val vm = OnlineRoomViewModel(
      multiplayerRepository = fakeMultiplayerRepository,
      authRepository = fakeAuthRepository,
      preferencesRepository = fakePrefsRepository,
    )
    fakeMultiplayerRepository.roomFlow.value = sampleSnapshot(
      status = RoomStatus.PLAYING,
      aliceReady = true,
      bobReady = true,
      myHand = listOf(red5, blue9, wildCard),
      topCard = GameCard.NumberCard("top_r2", CardColor.RED, 2),
      activeColor = CardColor.RED,
      version = 7,
    )

    vm.bindRoom("room_1")
    advanceUntilIdle()

    // Illegal card click sets Bengali feedback and does not send command
    vm.onCardClicked(blue9)
    advanceUntilIdle()
    val afterIllegal = (vm.uiState.value as UiState.Success).data
    assertNotNull(afterIllegal.errorMessage)
    assertNull(fakeMultiplayerRepository.lastPlayCardId)

    // Legal number card sends authoritative command with version=7
    vm.onCardClicked(red5)
    advanceUntilIdle()
    assertEquals("c_r5", fakeMultiplayerRepository.lastPlayCardId)
    assertEquals(7, fakeMultiplayerRepository.lastPlayExpectedVersion)
    val firstActionId = fakeMultiplayerRepository.lastPlayActionId
    assertNotNull(firstActionId)

    // Re-emit snapshot after server increments version -> wild flow
    fakeMultiplayerRepository.roomFlow.value = sampleSnapshot(
      status = RoomStatus.PLAYING,
      aliceReady = true,
      bobReady = true,
      myHand = listOf(wildCard),
      topCard = red5,
      activeColor = CardColor.RED,
      version = 8,
    )
    advanceUntilIdle()

    vm.onCardClicked(wildCard)
    val wildPromptState = (vm.uiState.value as UiState.Success).data
    assertEquals(wildCard, wildPromptState.pendingWildCard)

    vm.onWildSuitSelected(RongmohalSuit.PANNA)
    advanceUntilIdle()
    assertEquals("c_wild", fakeMultiplayerRepository.lastPlayCardId)
    assertEquals(CardColor.GREEN, fakeMultiplayerRepository.lastPlayChosenColor)
    assertEquals(8, fakeMultiplayerRepository.lastPlayExpectedVersion)
    assertTrue(firstActionId != fakeMultiplayerRepository.lastPlayActionId)
  }

  private fun sampleSnapshot(
    status: RoomStatus,
    aliceReady: Boolean,
    bobReady: Boolean,
    myHand: List<GameCard>,
    topCard: GameCard = GameCard.NumberCard("top_0", CardColor.RED, 3),
    activeColor: CardColor = CardColor.RED,
    version: Int = 1,
  ): OnlineRoomSnapshot {
    val meta = OnlineRoomMeta(
      roomId = "room_1",
      roomCode = "AB12CD",
      title = "সান্ধ্য আসর",
      hostUid = "alice",
      hostName = "অরিত্র",
      status = status,
      deckType = DeckType.SPECIAL,
      maxPlayers = 4,
      isPrivate = false,
      startingCards = 7,
      turnTimeoutSeconds = 25,
    )
    val players = listOf(
      OnlineRoomPlayer("alice", "অরিত্র", 0, 0, aliceReady, true, 1000L),
      OnlineRoomPlayer("bob", "সাব্বির", 1, 1, bobReady, true, 1000L),
    )
    val pub = if (status == RoomStatus.WAITING) null else OnlinePublicMatchState(
      version = version,
      deckType = DeckType.SPECIAL,
      topDiscardCard = topCard,
      activeColor = activeColor,
      drawPileCount = 40,
      discardPileCount = 1,
      pendingDrawPenalty = 0,
      isClockwise = true,
      currentTurnIndex = 0,
      currentTurnUid = "alice",
      turnDeadlineAt = System.currentTimeMillis() + 25_000L,
      turnTimeoutSeconds = 25,
      seatOrder = listOf("alice", "bob"),
      playerCardCounts = mapOf("alice" to myHand.size, "bob" to 6),
      declaredLastCard = emptyMap(),
      lastEventType = "CARD_PLAYED",
      lastEventMessage = "অরিত্র চাল দিয়েছে",
      lastEventTimestamp = 100L,
      winnerUid = null,
      results = emptyList<OnlineMatchResultItem>(),
    )
    return OnlineRoomSnapshot(
      meta = meta,
      players = players,
      publicState = pub,
      myHand = myHand,
      isSocketConnected = true,
    )
  }

  private class FakeAuthRepository(initial: AuthenticatedUser?) : AuthRepository {
    val userFlow = MutableStateFlow(initial)
    override val currentUser: Flow<AuthenticatedUser?> = userFlow
    override fun getCurrentUserSnapshot(): AuthenticatedUser? = userFlow.value
  }

  private class FakePreferencesRepository : PreferencesRepository {
    private val flow = MutableStateFlow(AppPreferences())
    override val preferences: Flow<AppPreferences> = flow.asStateFlow()
    override suspend fun setSoundEnabled(enabled: Boolean) { flow.value = flow.value.copy(soundEnabled = enabled) }
    override suspend fun setHapticsEnabled(enabled: Boolean) { flow.value = flow.value.copy(hapticsEnabled = enabled) }
    override suspend fun setUseBengaliNumerals(enabled: Boolean) { flow.value = flow.value.copy(useBengaliNumerals = enabled) }
    override suspend fun setDarkThemeEnabled(enabled: Boolean) { flow.value = flow.value.copy(darkThemeEnabled = enabled) }
    override suspend fun setDynamicColorEnabled(enabled: Boolean) { flow.value = flow.value.copy(dynamicColorEnabled = enabled) }
    override suspend fun setSelectedAvatarId(avatarId: Int) { flow.value = flow.value.copy(selectedAvatarId = avatarId) }
    override suspend fun setGuestSession(enabled: Boolean, displayName: String?) {
      flow.value = flow.value.copy(
        guestModeEnabled = enabled,
        guestDisplayName = displayName ?: flow.value.guestDisplayName,
      )
    }
  }

  private class FakeOnlineMultiplayerRepository : OnlineMultiplayerRepository {
    val publicRoomsFlow = MutableStateFlow<List<OnlineRoomSummary>>(emptyList())
    val leaderboardFlow = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val roomFlow = MutableStateFlow<OnlineRoomSnapshot?>(null)
    val connectedFlow = MutableStateFlow(true)

    var lastCreatedDeckType: DeckType? = null
    var lastJoinCode: String? = null
    var lastStartedRoomId: String? = null
    var lastPlayCardId: String? = null
    var lastPlayChosenColor: CardColor? = null
    var lastPlayExpectedVersion: Int? = null
    var lastPlayActionId: String? = null

    override fun observePublicWaitingRooms(): Flow<List<OnlineRoomSummary>> = publicRoomsFlow
    override fun observeLeaderboard(limit: Int): Flow<List<LeaderboardEntry>> = leaderboardFlow
    override fun observeRoomSession(roomId: String, myUid: String): Flow<OnlineRoomSnapshot?> = roomFlow
    override fun observeRtdbConnection(): Flow<Boolean> = connectedFlow

    override suspend fun updateCloudProfile(displayName: String, avatarId: Int): Result<Unit> =
      Result.success(Unit)

    override suspend fun createRoom(
      title: String,
      displayName: String,
      avatarId: Int,
      deckType: DeckType,
      maxPlayers: Int,
      isPrivate: Boolean,
      startingCards: Int,
    ): Result<CreatedRoomInfo> {
      lastCreatedDeckType = deckType
      return Result.success(CreatedRoomInfo("room_created_1", "XY78KL"))
    }

    override suspend fun joinRoomByCode(
      roomCodeOrId: String,
      isRoomId: Boolean,
      displayName: String,
      avatarId: Int,
    ): Result<JoinedRoomInfo> {
      lastJoinCode = roomCodeOrId
      return Result.success(JoinedRoomInfo("room_joined_1", roomCodeOrId, false))
    }

    override suspend fun setPlayerReady(roomId: String, ready: Boolean): Result<Unit> = Result.success(Unit)
    override suspend fun leaveRoom(roomId: String): Result<Unit> = Result.success(Unit)
    override suspend fun startMatch(roomId: String): Result<Unit> {
      lastStartedRoomId = roomId
      return Result.success(Unit)
    }

    override suspend fun submitPlayCard(
      roomId: String,
      actionId: String,
      expectedVersion: Int,
      cardId: String,
      chosenColor: CardColor?,
      targetPlayerId: String?,
    ): Result<CommandAck> {
      lastPlayCardId = cardId
      lastPlayChosenColor = chosenColor
      lastPlayExpectedVersion = expectedVersion
      lastPlayActionId = actionId
      return Result.success(CommandAck(expectedVersion + 1, duplicate = false, matchFinished = false))
    }

    override suspend fun submitDrawCard(roomId: String, actionId: String, expectedVersion: Int): Result<CommandAck> =
      Result.success(CommandAck(expectedVersion + 1, duplicate = false, matchFinished = false))

    override suspend fun submitDeclareLastCard(roomId: String, actionId: String, expectedVersion: Int): Result<CommandAck> =
      Result.success(CommandAck(expectedVersion + 1, duplicate = false, matchFinished = false))

    override suspend fun claimTurnTimeout(roomId: String): Result<Unit> = Result.success(Unit)
  }
}
