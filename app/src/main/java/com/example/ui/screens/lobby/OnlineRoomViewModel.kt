package com.example.ui.screens.lobby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.config.UiState
import com.example.domain.game.CardColor
import com.example.domain.game.CardType
import com.example.domain.game.CardValidator
import com.example.domain.game.GameCard
import com.example.domain.model.OnlineMatchResultItem
import com.example.domain.model.OnlinePublicMatchState
import com.example.domain.model.OnlineRoomMeta
import com.example.domain.model.OnlineRoomPlayer
import com.example.domain.model.OnlineRoomSnapshot
import com.example.domain.model.RoomStatus
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.OnlineMultiplayerRepository
import com.example.domain.repository.PreferencesRepository
import com.example.ui.components.BoardEffectType
import com.example.ui.components.BoardVisualEffect
import com.example.ui.components.RongmohalAvatar
import com.example.ui.components.RongmohalCardModel
import com.example.ui.components.RongmohalSuit
import com.example.ui.components.toCardColor
import com.example.ui.components.toRongmohalSuit
import com.example.ui.components.toUiCardModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnlineOpponentUi(
  val uid: String,
  val name: String,
  val avatar: RongmohalAvatar,
  val cardCount: Int,
  val connected: Boolean,
  val declaredLastCard: Boolean,
  val isCurrentTurn: Boolean,
)

data class OnlineHandCardUi(
  val domainCard: GameCard,
  val uiCard: RongmohalCardModel,
  val isPlayable: Boolean,
)

data class OnlineRoomScreenUiState(
  val roomId: String = "",
  val myUid: String = "",
  val meta: OnlineRoomMeta? = null,
  val players: List<OnlineRoomPlayer> = emptyList(),
  val isHost: Boolean = false,
  val myReady: Boolean = false,
  val canHostStartMatch: Boolean = false,
  val isSocketConnected: Boolean = true,
  val useBengaliNumerals: Boolean = true,
  val opponents: List<OnlineOpponentUi> = emptyList(),
  val myPlayer: OnlineRoomPlayer? = null,
  val myHand: List<OnlineHandCardUi> = emptyList(),
  val topDiscardCard: RongmohalCardModel? = null,
  val activeSuit: RongmohalSuit = RongmohalSuit.SINDOOR,
  val drawPileCount: Int = 0,
  val pendingDrawPenalty: Int = 0,
  val isClockwise: Boolean = true,
  val isMyTurn: Boolean = false,
  val turnSecondsLeft: Int = 25,
  val turnTotalSeconds: Int = 25,
  val canDeclareLastCard: Boolean = false,
  val hasDeclaredLastCard: Boolean = false,
  val statusMessage: String = "সার্ভারের সাথে যুক্ত হচ্ছে…",
  val pendingWildCard: GameCard? = null,
  val pendingSwapCard: GameCard? = null,
  val pendingSwapTargetId: String? = null,
  val activeVisualEffect: BoardVisualEffect? = null,
  val winnerUid: String? = null,
  val winnerName: String = "",
  val winnerAvatar: RongmohalAvatar = RongmohalAvatar.BAUL,
  val results: List<OnlineMatchResultItem> = emptyList(),
  val stateVersion: Int = 1,
  val drawPulseCount: Int = 0,
  val actionBusy: Boolean = false,
  val errorMessage: String? = null,
)

@HiltViewModel
class OnlineRoomViewModel @Inject constructor(
  private val multiplayerRepository: OnlineMultiplayerRepository,
  private val authRepository: AuthRepository,
  private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow<UiState<OnlineRoomScreenUiState>>(UiState.Loading)
  val uiState: StateFlow<UiState<OnlineRoomScreenUiState>> = _uiState.asStateFlow()

  private var observeJob: Job? = null
  private var timerJob: Job? = null
  private var effectDismissJob: Job? = null
  private var activeRoomId: String? = null
  private var lastHandledEventTimestamp: Long = 0L
  private var lastTimeoutClaimKey: String? = null
  private var effectCounter: Long = 0L
  private var drawPulseCounter: Int = 0

  fun bindRoom(roomId: String) {
    if (activeRoomId == roomId && observeJob?.isActive == true) return
    activeRoomId = roomId
    observeJob?.cancel()
    timerJob?.cancel()

    val user = authRepository.getCurrentUserSnapshot()
    if (user == null) {
      _uiState.value = UiState.Error("অনলাইন আসরের জন্য গুগল অ্যাকাউন্টে প্রবেশ আবশ্যক।")
      return
    }

    observeJob = viewModelScope.launch {
      combine(
        multiplayerRepository.observeRoomSession(roomId, user.uid),
        preferencesRepository.preferences,
      ) { snapshot, prefs ->
        Pair(snapshot, prefs.useBengaliNumerals)
      }.catch { err ->
        if (err is CancellationException) throw err
        _uiState.value = UiState.Error(err.localizedMessage ?: "রুম তথ্য পাওয়া যায়নি।")
      }.collect { (snapshot, useBengaliDigits) ->
        if (snapshot == null) {
          _uiState.value = UiState.Error("এই রুমটি বাতিল হয়েছে বা আর সক্রিয় নেই।")
          timerJob?.cancel()
          return@collect
        }
        applySnapshot(snapshot, user.uid, useBengaliDigits)
      }
    }
  }

  private fun applySnapshot(
    snapshot: OnlineRoomSnapshot,
    myUid: String,
    useBengaliDigits: Boolean,
  ) {
    val meta = snapshot.meta
    val players = snapshot.players
    val me = players.firstOrNull { it.uid == myUid }
    val isHost = meta.hostUid == myUid
    val allReady = players.size >= 2 && players.all { it.uid == meta.hostUid || it.ready }
    val pub = snapshot.publicState

    val prev = (_uiState.value as? UiState.Success)?.data ?: OnlineRoomScreenUiState()

    val isMyTurn = meta.status == RoomStatus.PLAYING && pub?.currentTurnUid == myUid
    val handUi = snapshot.myHand.map { card ->
      val playable = isMyTurn && pub != null && CardValidator.isCardPlayableOn(
        card = card,
        topCard = pub.topDiscardCard,
        activeColor = pub.activeColor,
        pendingDrawPenalty = pub.pendingDrawPenalty,
      )
      OnlineHandCardUi(
        domainCard = card,
        uiCard = card.toUiCardModel(),
        isPlayable = playable,
      )
    }

    val opponents = players
      .filter { it.uid != myUid }
      .map { opp ->
        OnlineOpponentUi(
          uid = opp.uid,
          name = opp.displayName,
          avatar = RongmohalAvatar.byId(opp.avatarId),
          cardCount = pub?.playerCardCounts?.get(opp.uid) ?: 0,
          connected = opp.connected,
          declaredLastCard = pub?.declaredLastCard?.get(opp.uid) == true,
          isCurrentTurn = meta.status == RoomStatus.PLAYING && pub?.currentTurnUid == opp.uid,
        )
      }

    if (pub != null && pub.lastEventTimestamp > lastHandledEventTimestamp) {
      lastHandledEventTimestamp = pub.lastEventTimestamp
      if (pub.lastEventType == "CARDS_DRAWN" || pub.lastEventType == "TURN_TIMEOUT_DRAW") {
        drawPulseCounter++
      }
      mapEventToEffect(pub)
    }

    val winnerPlayer = pub?.winnerUid?.let { winUid -> players.firstOrNull { it.uid == winUid } }
    val myDeclared = pub?.declaredLastCard?.get(myUid) == true
    val canDeclare =
      meta.status == RoomStatus.PLAYING && snapshot.myHand.size in 1..2 && !myDeclared

    val secondsRemaining = if (pub != null && meta.status == RoomStatus.PLAYING) {
      ((pub.turnDeadlineAt - System.currentTimeMillis()) / 1000L).toInt().coerceAtLeast(0)
    } else {
      meta.turnTimeoutSeconds
    }

    val mapped = prev.copy(
      roomId = meta.roomId,
      myUid = myUid,
      meta = meta,
      players = players,
      isHost = isHost,
      myReady = me?.ready == true,
      canHostStartMatch = isHost && meta.status == RoomStatus.WAITING && allReady,
      isSocketConnected = snapshot.isSocketConnected,
      useBengaliNumerals = useBengaliDigits,
      opponents = opponents,
      myPlayer = me,
      myHand = handUi,
      topDiscardCard = pub?.topDiscardCard?.toUiCardModel(),
      activeSuit = pub?.activeColor?.toRongmohalSuit() ?: RongmohalSuit.SINDOOR,
      drawPileCount = pub?.drawPileCount ?: 0,
      pendingDrawPenalty = pub?.pendingDrawPenalty ?: 0,
      isClockwise = pub?.isClockwise ?: true,
      isMyTurn = isMyTurn,
      turnSecondsLeft = secondsRemaining,
      turnTotalSeconds = pub?.turnTimeoutSeconds ?: meta.turnTimeoutSeconds,
      canDeclareLastCard = canDeclare,
      hasDeclaredLastCard = myDeclared,
      statusMessage = pub?.lastEventMessage?.takeIf { it.isNotBlank() }
        ?: if (isMyTurn) "আপনার চাল — মিলিয়ে তাস ফেলুন" else "অন্য খেলোয়াড়ের চালের অপেক্ষা…",
      winnerUid = pub?.winnerUid,
      winnerName = winnerPlayer?.displayName ?: "",
      winnerAvatar = RongmohalAvatar.byId(winnerPlayer?.avatarId ?: 0),
      results = pub?.results ?: emptyList(),
      stateVersion = pub?.version ?: 1,
      drawPulseCount = drawPulseCounter,
      actionBusy = false,
    )

    _uiState.value = UiState.Success(mapped)

    if (meta.status == RoomStatus.PLAYING && pub != null) {
      syncServerTimer(meta.roomId, pub)
    } else {
      timerJob?.cancel()
    }
  }

  private fun syncServerTimer(roomId: String, pub: OnlinePublicMatchState) {
    timerJob?.cancel()
    timerJob = viewModelScope.launch {
      while (true) {
        val left = ((pub.turnDeadlineAt - System.currentTimeMillis()) / 1000L).toInt().coerceAtLeast(0)
        updateSuccess { it.copy(turnSecondsLeft = left) }
        if (left == 0) {
          val claimKey = "${pub.version}_${pub.currentTurnUid}"
          val connected = (_uiState.value as? UiState.Success)?.data?.isSocketConnected == true
          if (connected && lastTimeoutClaimKey != claimKey) {
            lastTimeoutClaimKey = claimKey
            multiplayerRepository.claimTurnTimeout(roomId)
          }
          break
        }
        delay(1000L)
      }
    }
  }

  private fun mapEventToEffect(pub: OnlinePublicMatchState) {
    val effect = when (pub.lastEventType) {
      "MATCH_STARTED" -> BoardEffectType.SHUFFLE to "আসর শুরু!"
      "HANDS_SWAPPED" -> BoardEffectType.CARD_EXCHANGE to "হাতবদল সম্পন্ন!"
      "PENALTY_BOUNCED", "LAST_CARD_DECLARED" -> BoardEffectType.SHIELD to "বর্ম ও ঘোষণা!"
      "EVERYONE_DREW_ONE" -> BoardEffectType.ATTACK to "সবাই মিলে +১!"
      "UNO_PENALTY" -> BoardEffectType.ATTACK to "শেষ তাস ঘোষণা হয়নি (+২)!"
      "CARD_PLAYED" -> when (pub.topDiscardCard.type) {
        CardType.DRAW_TWO -> BoardEffectType.ATTACK to "+২ আক্রমণ!"
        CardType.WILD_DRAW_FOUR -> BoardEffectType.ATTACK to "মহাচাল +৪!"
        CardType.THUNDERBOLT_PLUS_THREE -> BoardEffectType.ATTACK to "বজ্রপাত +৩!"
        else -> null
      }
      "MYSTERY_TRIGGERED" -> BoardEffectType.CARD_EXCHANGE to "রহস্য কার্ড!"
      else -> null
    } ?: return

    effectDismissJob?.cancel()
    val visual = BoardVisualEffect(
      effectId = ++effectCounter,
      type = effect.first,
      title = effect.second,
      subtitle = pub.lastEventMessage,
    )
    updateSuccess { it.copy(activeVisualEffect = visual) }
    effectDismissJob = viewModelScope.launch {
      delay(1150L)
      updateSuccess { current ->
        if (current.activeVisualEffect?.effectId == visual.effectId) {
          current.copy(activeVisualEffect = null)
        } else {
          current
        }
      }
    }
  }

  fun toggleReady() {
    val current = (_uiState.value as? UiState.Success)?.data ?: return
    if (current.actionBusy) return
    val nextReady = !current.myReady
    viewModelScope.launch {
      updateSuccess { it.copy(actionBusy = true, errorMessage = null) }
      val res = multiplayerRepository.setPlayerReady(current.roomId, nextReady)
      res.onFailure { err ->
        updateSuccess { it.copy(actionBusy = false, errorMessage = err.localizedMessage) }
      }
    }
  }

  fun startMatch() {
    val current = (_uiState.value as? UiState.Success)?.data ?: return
    if (current.actionBusy || !current.canHostStartMatch) return
    viewModelScope.launch {
      updateSuccess { it.copy(actionBusy = true, errorMessage = null) }
      val res = multiplayerRepository.startMatch(current.roomId)
      res.onFailure { err ->
        updateSuccess {
          it.copy(
            actionBusy = false,
            errorMessage = err.localizedMessage ?: "ম্যাচ শুরু করা যায়নি।",
          )
        }
      }
    }
  }

  fun leaveRoom(onLeft: () -> Unit) {
    val roomId = activeRoomId ?: run {
      onLeft()
      return
    }
    viewModelScope.launch {
      multiplayerRepository.leaveRoom(roomId)
      onLeft()
    }
  }

  fun onCardClicked(card: GameCard) {
    val current = (_uiState.value as? UiState.Success)?.data ?: return
    if (!current.isMyTurn || current.actionBusy) return
    val handItem = current.myHand.firstOrNull { it.domainCard.id == card.id }
    if (handItem?.isPlayable != true) {
      updateSuccess { it.copy(errorMessage = "এই তাসটি বর্তমান রঙ বা সংখ্যার সাথে মিলছে না।") }
      return
    }

    if (card.type == CardType.HAND_SWAP && current.opponents.size > 1) {
      updateSuccess { it.copy(pendingSwapCard = card, pendingSwapTargetId = null, errorMessage = null) }
      return
    }

    if (card.color == CardColor.WILD) {
      val defaultTarget = if (card.type == CardType.HAND_SWAP) current.opponents.firstOrNull()?.uid else null
      updateSuccess { it.copy(pendingWildCard = card, pendingSwapTargetId = defaultTarget, errorMessage = null) }
      return
    }

    sendPlayCommand(card = card, chosenColor = card.color, targetUid = null)
  }

  fun onHandSwapTargetSelected(targetUid: String) {
    val current = (_uiState.value as? UiState.Success)?.data ?: return
    val swapCard = current.pendingSwapCard ?: return
    updateSuccess {
      it.copy(
        pendingSwapCard = null,
        pendingSwapTargetId = targetUid,
        pendingWildCard = swapCard,
      )
    }
  }

  fun dismissHandSwapChooser() {
    updateSuccess { it.copy(pendingSwapCard = null, pendingSwapTargetId = null) }
  }

  fun onWildSuitSelected(suit: RongmohalSuit) {
    val current = (_uiState.value as? UiState.Success)?.data ?: return
    val pending = current.pendingWildCard ?: return
    val targetUid = current.pendingSwapTargetId
    updateSuccess { it.copy(pendingWildCard = null, pendingSwapTargetId = null) }
    sendPlayCommand(card = pending, chosenColor = suit.toCardColor(), targetUid = targetUid)
  }

  fun dismissWildChooser() {
    updateSuccess { it.copy(pendingWildCard = null, pendingSwapTargetId = null) }
  }

  fun onDrawCardClicked() {
    val current = (_uiState.value as? UiState.Success)?.data ?: return
    if (!current.isMyTurn || current.actionBusy) return
    val actionId = UUID.randomUUID().toString()
    viewModelScope.launch {
      updateSuccess { it.copy(actionBusy = true, errorMessage = null) }
      val res = multiplayerRepository.submitDrawCard(
        roomId = current.roomId,
        actionId = actionId,
        expectedVersion = current.stateVersion,
      )
      res.onFailure { err ->
        updateSuccess { it.copy(actionBusy = false, errorMessage = err.localizedMessage ?: "তাস টানা যায়নি।") }
      }
    }
  }

  fun declareLastCard() {
    val current = (_uiState.value as? UiState.Success)?.data ?: return
    if (!current.canDeclareLastCard || current.hasDeclaredLastCard || current.actionBusy) return
    val actionId = UUID.randomUUID().toString()
    viewModelScope.launch {
      updateSuccess { it.copy(actionBusy = true, errorMessage = null) }
      val res = multiplayerRepository.submitDeclareLastCard(
        roomId = current.roomId,
        actionId = actionId,
        expectedVersion = current.stateVersion,
      )
      res.onFailure { err ->
        updateSuccess { it.copy(actionBusy = false, errorMessage = err.localizedMessage ?: "ঘোষণা পাঠানো যায়নি।") }
      }
    }
  }

  private fun sendPlayCommand(card: GameCard, chosenColor: CardColor?, targetUid: String?) {
    val current = (_uiState.value as? UiState.Success)?.data ?: return
    val actionId = UUID.randomUUID().toString()
    viewModelScope.launch {
      updateSuccess { it.copy(actionBusy = true, errorMessage = null) }
      val res = multiplayerRepository.submitPlayCard(
        roomId = current.roomId,
        actionId = actionId,
        expectedVersion = current.stateVersion,
        cardId = card.id,
        chosenColor = chosenColor,
        targetPlayerId = targetUid,
      )
      res.onFailure { err ->
        updateSuccess { it.copy(actionBusy = false, errorMessage = err.localizedMessage ?: "চাল পাঠানো যায়নি।") }
      }
    }
  }

  fun clearError() {
    updateSuccess { it.copy(errorMessage = null) }
  }

  private inline fun updateSuccess(transform: (OnlineRoomScreenUiState) -> OnlineRoomScreenUiState) {
    _uiState.update { state ->
      if (state is UiState.Success) UiState.Success(transform(state.data)) else state
    }
  }

  override fun onCleared() {
    super.onCleared()
    observeJob?.cancel()
    timerJob?.cancel()
    effectDismissJob?.cancel()
  }
}
