package com.example.ui.screens.board

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.ai.AiDecisionEngine
import com.example.domain.ai.AiDelayConfig
import com.example.domain.ai.AiDifficulty
import com.example.domain.ai.AiPlayerConfig
import com.example.domain.ai.AiRoster
import com.example.domain.ai.PublicPlayLedger
import com.example.domain.game.CardColor
import com.example.domain.game.CardGameEngine
import com.example.domain.game.CardType
import com.example.domain.game.CardValidator
import com.example.domain.game.DeckType
import com.example.domain.game.EngineStepResult
import com.example.domain.game.GameCard
import com.example.domain.game.GameEvent
import com.example.domain.game.GameTableState
import com.example.domain.game.MysteryOutcome
import com.example.domain.game.ValidationResult
import com.example.domain.repository.PreferencesRepository
import com.example.ui.components.BoardEffectType
import com.example.ui.components.BoardVisualEffect
import com.example.ui.components.RongmohalAvatar
import com.example.ui.components.RongmohalCardModel
import com.example.ui.components.RongmohalSuit
import com.example.ui.components.toBengaliColorName
import com.example.ui.components.toBengaliDescription
import com.example.ui.components.toBengaliTitle
import com.example.ui.components.toCardColor
import com.example.ui.components.toRongmohalSuit
import com.example.ui.components.toUiCardModel
import com.example.ui.screens.mode.GameModeId
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OpponentSeatState(
  val playerId: String,
  val name: String,
  val personaSubtitle: String,
  val avatar: RongmohalAvatar,
  val cardCount: Int,
  val seatLabel: String,
)

data class HandCardItemUi(
  val domainCard: GameCard,
  val uiCard: RongmohalCardModel,
  val isPlayable: Boolean,
)

data class GameBoardUiState(
  val mode: GameModeId = GameModeId.CLASSIC,
  val deckType: DeckType = DeckType.CLASSIC,
  val difficulty: AiDifficulty = AiDifficulty.MEDIUM,
  val playerName: String = "আপনি",
  val playerAvatar: RongmohalAvatar = RongmohalAvatar.BAUL,
  val opponents: List<OpponentSeatState> = emptyList(),
  val playerHand: List<HandCardItemUi> = emptyList(),
  val topDiscardCard: RongmohalCardModel = RongmohalCardModel.NumberCard("init", RongmohalSuit.SINDOOR, 5),
  val activeSuit: RongmohalSuit = RongmohalSuit.SINDOOR,
  val drawPileCount: Int = 0,
  val discardPileCount: Int = 1,
  val drawPulseCount: Int = 0,
  val recyclePulseCount: Int = 0,
  val pendingDrawPenalty: Int = 0,
  val currentTurnIndex: Int = 0,
  val isClockwise: Boolean = true,
  val turnTimerSeconds: Int = TURN_DURATION_SECONDS,
  val turnTimerTotalSeconds: Int = TURN_DURATION_SECONDS,
  val hasDeclaredLastCard: Boolean = false,
  val canDeclareLastCard: Boolean = false,
  val isPaused: Boolean = false,
  val pendingWildCard: GameCard? = null,
  val pendingSwapCard: GameCard? = null,
  val pendingSwapTargetId: String? = null,
  val mysteryOutcomeText: String? = null,
  val activeVisualEffect: BoardVisualEffect? = null,
  val statusMessage: String = "আপনার চাল — মিলিয়ে তাস ফেলুন",
  val useBengaliNumerals: Boolean = true,
  val matchFinished: Boolean = false,
  val playerWon: Boolean = false,
  val winnerName: String = "",
  val winnerAvatar: RongmohalAvatar = RongmohalAvatar.BAUL,
  val finalPlayerScore: Int = 0,
  val finalOpponentScore: Int = 0,
) {
  companion object {
    const val TURN_DURATION_SECONDS = 25
  }
}

@HiltViewModel
class GameBoardViewModel @Inject constructor(
  private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(GameBoardUiState())
  val uiState: StateFlow<GameBoardUiState> = _uiState.asStateFlow()

  private val engine = CardGameEngine()
  private val aiEngine = AiDecisionEngine(delayConfig = AiDelayConfig())
  private var aiPlayerConfigs: Map<String, AiPlayerConfig> = emptyMap()
  private var publicLedger: PublicPlayLedger = PublicPlayLedger()
  private var tableState: GameTableState? = null
  private var initializedKey: String? = null

  private var drawPulseCounter: Int = 0
  private var recycleCounter: Int = 0
  private var effectCounter: Long = 0L
  private var turnJob: Job? = null
  private var timerJob: Job? = null
  private var effectDismissJob: Job? = null

  fun initMatch(
    mode: GameModeId,
    playerCount: Int,
    playerName: String,
    avatarId: Int,
    difficulty: AiDifficulty = AiDifficulty.MEDIUM,
    forceRestart: Boolean = false,
  ) {
    val key = "${mode.name}_${playerCount}_${playerName}_${avatarId}_${difficulty.name}"
    if (!forceRestart && initializedKey == key) return
    initializedKey = key

    turnJob?.cancel()
    timerJob?.cancel()
    effectDismissJob?.cancel()

    viewModelScope.launch {
      val prefs = preferencesRepository.preferences.first()
      val deckType = if (mode == GameModeId.TACTICAL) DeckType.SPECIAL else DeckType.CLASSIC

      val seatCount = (playerCount - 1).coerceIn(1, 3)
      val selectedProfiles = AiRoster.selectOpponents(
        count = seatCount,
        seedOffset = avatarId % AiRoster.ALL.size,
      )

      val configs = mutableMapOf<String, AiPlayerConfig>()
      val players = buildList {
        add(HUMAN_PLAYER_ID to playerName)
        selectedProfiles.forEachIndexed { idx, profile ->
          val botId = "bot_${idx + 1}"
          configs[botId] = AiPlayerConfig(
            playerId = botId,
            profile = profile,
            difficulty = difficulty,
          )
          add(botId to profile.bengaliName)
        }
      }
      aiPlayerConfigs = configs
      publicLedger = PublicPlayLedger()
      drawPulseCounter = 0
      recycleCounter = 0

      val initialTable = engine.createMatch(
        deckType = deckType,
        players = players,
        cardsPerPlayer = mode.startingCards,
      )
      tableState = initialTable

      publishTableState(
        mode = mode,
        difficulty = difficulty,
        avatarId = avatarId,
        useBengaliNumerals = prefs.useBengaliNumerals,
        state = initialTable,
        statusOverride = "তাস বাঁটা হয়েছে — আপনার চাল!",
        hasDeclaredLastCard = false,
        isPaused = false,
      )
      triggerVisualEffect(
        type = BoardEffectType.SHUFFLE,
        title = "তাস শাফল ও বণ্টন",
        subtitle = "${selectedProfiles.joinToString(", ") { it.bengaliName }}-এর সাথে আসর শুরু!",
      )
      restartTurnTimer()
    }
  }

  fun restartCurrentMatch() {
    val current = _uiState.value
    initMatch(
      mode = current.mode,
      playerCount = current.opponents.size + 1,
      playerName = current.playerName,
      avatarId = current.playerAvatar.id,
      difficulty = current.difficulty,
      forceRestart = true,
    )
  }

  fun togglePause() {
    val current = _uiState.value
    if (current.matchFinished) return
    val nextPaused = !current.isPaused
    _uiState.update { it.copy(isPaused = nextPaused) }
    if (nextPaused) {
      timerJob?.cancel()
      turnJob?.cancel()
    } else {
      resumeTurnTimerIfNeeded()
      resumeBotTurnsIfNeeded()
    }
  }

  fun declareLastCard() {
    val current = _uiState.value
    if (!current.canDeclareLastCard || current.hasDeclaredLastCard || current.matchFinished) return
    _uiState.update {
      it.copy(
        hasDeclaredLastCard = true,
        canDeclareLastCard = false,
        statusMessage = "শেষ তাস ঘোষিত হয়েছে! জয়ের আর মাত্র এক ধাপ!",
      )
    }
    triggerVisualEffect(
      type = BoardEffectType.SHIELD,
      title = "শেষ তাস ঘোষণা!",
      subtitle = "${current.playerName} জয়ের পথে শেষ ঘোষণা দিয়েছেন",
    )
  }

  fun dismissMysteryModal() {
    _uiState.update { it.copy(mysteryOutcomeText = null) }
    resumeTurnTimerIfNeeded()
    resumeBotTurnsIfNeeded()
  }

  fun onCardClicked(card: GameCard) {
    val currentUi = _uiState.value
    val state = tableState ?: return
    if (currentUi.isPaused || state.currentPlayer.playerId != HUMAN_PLAYER_ID || state.isMatchFinished) return

    val preCheck = CardValidator.validatePlay(
      state = state,
      playerId = HUMAN_PLAYER_ID,
      card = card,
      chosenColor = if (card.color == CardColor.WILD) CardColor.RED else card.color,
    )
    if (!preCheck.isValid) {
      if (preCheck is ValidationResult.Invalid) {
        _uiState.update { it.copy(statusMessage = preCheck.message) }
      }
      return
    }

    if (card.type == CardType.HAND_SWAP && currentUi.opponents.size > 1) {
      timerJob?.cancel()
      _uiState.update { it.copy(pendingSwapCard = card, pendingSwapTargetId = null) }
      return
    }

    if (card.color == CardColor.WILD) {
      timerJob?.cancel()
      val defaultSwapTarget = if (card.type == CardType.HAND_SWAP) {
        currentUi.opponents.firstOrNull()?.playerId
      } else {
        null
      }
      _uiState.update {
        it.copy(
          pendingWildCard = card,
          pendingSwapTargetId = defaultSwapTarget,
        )
      }
      return
    }

    executeHumanPlay(card = card, chosenColor = card.color, targetPlayerId = null)
  }

  fun onHandSwapTargetSelected(targetPlayerId: String) {
    val swapCard = _uiState.value.pendingSwapCard ?: return
    _uiState.update {
      it.copy(
        pendingSwapCard = null,
        pendingSwapTargetId = targetPlayerId,
        pendingWildCard = swapCard,
      )
    }
  }

  fun dismissHandSwapChooser() {
    _uiState.update { it.copy(pendingSwapCard = null, pendingSwapTargetId = null) }
    resumeTurnTimerIfNeeded()
  }

  fun onWildSuitSelected(suit: RongmohalSuit) {
    val current = _uiState.value
    val pending = current.pendingWildCard ?: return
    val swapTarget = current.pendingSwapTargetId
    _uiState.update {
      it.copy(
        pendingWildCard = null,
        pendingSwapTargetId = null,
      )
    }
    executeHumanPlay(
      card = pending,
      chosenColor = suit.toCardColor(),
      targetPlayerId = swapTarget,
    )
  }

  fun dismissWildChooser() {
    _uiState.update { it.copy(pendingWildCard = null, pendingSwapTargetId = null) }
    resumeTurnTimerIfNeeded()
  }

  fun onDrawCardClicked() {
    val currentUi = _uiState.value
    val state = tableState ?: return
    if (currentUi.isPaused || state.currentPlayer.playerId != HUMAN_PLAYER_ID || state.isMatchFinished) return
    val previousColor = state.activeColor

    when (val result = engine.drawCards(state, HUMAN_PLAYER_ID)) {
      is EngineStepResult.Success -> {
        tableState = result.state
        publicLedger = publicLedger.recordStateTransition(previousColor, result.state.lastEvent)
        drawPulseCounter++
        val drawnEvent = result.state.lastEvent as? GameEvent.CardsDrawn
        if ((drawnEvent?.recycledFromDiscardCount ?: 0) > 0) {
          recycleCounter++
          triggerVisualEffect(
            type = BoardEffectType.SHUFFLE,
            title = "ডিসকার্ড স্তূপ পুনর্ব্যবহার",
            subtitle = "${drawnEvent?.recycledFromDiscardCount ?: 0}টি তাস নতুন করে শাফল হয়েছে",
          )
        }
        val status = if (drawnEvent != null && drawnEvent.recycledFromDiscardCount > 0) {
          "ডিসকার্ড পাইল শাফল করে ${drawnEvent.drawnCards.size}টি তাস টানা হয়েছে"
        } else {
          "আপনি ${drawnEvent?.drawnCards?.size ?: 1}টি তাস টেনেছেন"
        }
        publishCurrentUi(result.state, status)
        restartTurnTimer()
        resumeBotTurnsIfNeeded()
      }
      is EngineStepResult.Rejected -> {
        _uiState.update { it.copy(statusMessage = result.message) }
      }
    }
  }

  private fun executeHumanPlay(
    card: GameCard,
    chosenColor: CardColor,
    targetPlayerId: String?,
  ) {
    val state = tableState ?: return
    val previousColor = state.activeColor
    val wasTwoCards = state.players.first().hand.size == 2
    val wasDeclared = _uiState.value.hasDeclaredLastCard
    when (
      val result = engine.playCard(
        state = state,
        playerId = HUMAN_PLAYER_ID,
        cardId = card.id,
        chosenColor = chosenColor,
        targetPlayerId = targetPlayerId,
      )
    ) {
      is EngineStepResult.Success -> {
        var resolvedState = result.state
        var status = describeEvent(resolvedState.lastEvent, resolvedState)
        val droppedToOneWithoutDeclare =
          !resolvedState.isMatchFinished &&
            resolvedState.players.first().hand.size == 1 &&
            wasTwoCards &&
            !wasDeclared

        if (droppedToOneWithoutDeclare) {
          val penalized = engine.applyUndeclaredLastCardPenalty(resolvedState, HUMAN_PLAYER_ID, 2)
          if (penalized is EngineStepResult.Success &&
            penalized.state.players.first().hand.size > resolvedState.players.first().hand.size
          ) {
            resolvedState = penalized.state
            drawPulseCounter++
            val drawnCount = resolvedState.players.first().hand.size - 1
            status = "শেষ তাস ঘোষণা না করায় +${drawnCount} পেনাল্টি পেয়েছেন!"
            triggerVisualEffect(
              type = BoardEffectType.ATTACK,
              title = "শেষ তাস ঘোষণা হয়নি!",
              subtitle = "+${drawnCount} তাস পেনাল্টি যোগ হয়েছে",
            )
          }
        } else {
          handleEventEffects(resolvedState.lastEvent, resolvedState)
        }

        tableState = resolvedState
        publicLedger = publicLedger.recordStateTransition(previousColor, resolvedState.lastEvent)
        publishCurrentUi(resolvedState, status)
        restartTurnTimer()
        if (_uiState.value.mysteryOutcomeText == null) {
          resumeBotTurnsIfNeeded()
        }
      }
      is EngineStepResult.Rejected -> {
        _uiState.update { it.copy(statusMessage = result.message) }
      }
    }
  }

  private fun resumeBotTurnsIfNeeded() {
    turnJob?.cancel()
    turnJob = viewModelScope.launch {
      while (true) {
        val state = tableState ?: break
        val currentUi = _uiState.value
        if (state.isMatchFinished ||
          currentUi.isPaused ||
          currentUi.mysteryOutcomeText != null ||
          state.currentPlayer.playerId == HUMAN_PLAYER_ID
        ) {
          break
        }

        val botSeat = state.currentPlayer
        val botConfig = aiPlayerConfigs[botSeat.playerId] ?: break
        _uiState.update {
          it.copy(statusMessage = "${botConfig.profile.bengaliName} চাল ভাবছেন…")
        }

        val delayMs = aiEngine.computeThinkingDelayMillis(botConfig.difficulty)
        delay(delayMs)

        val latest = tableState ?: break
        val latestUi = _uiState.value
        if (latest.isMatchFinished ||
          latestUi.isPaused ||
          latestUi.mysteryOutcomeText != null ||
          latest.currentPlayer.playerId != botSeat.playerId
        ) {
          break
        }

        val previousColor = latest.activeColor
        val step = aiEngine.resolveAndExecuteTurn(
          engine = engine,
          state = latest,
          aiPlayerId = botSeat.playerId,
          profile = botConfig.profile,
          difficulty = botConfig.difficulty,
          ledger = publicLedger,
        )

        when (step) {
          is EngineStepResult.Success -> {
            tableState = step.state
            publicLedger = publicLedger.recordStateTransition(previousColor, step.state.lastEvent)
            val event = step.state.lastEvent
            if (event is GameEvent.CardsDrawn) {
              drawPulseCounter++
              if (event.recycledFromDiscardCount > 0) {
                recycleCounter++
              }
            } else if (event is GameEvent.EveryoneDrewOne && event.recycledFromDiscardCount > 0) {
              recycleCounter++
            }
            handleEventEffects(event, step.state)
            val message = describeEvent(event, step.state)
            publishCurrentUi(step.state, message)
            restartTurnTimer()
          }
          is EngineStepResult.Rejected -> {
            _uiState.update { it.copy(statusMessage = step.message) }
            break
          }
        }
      }

      val finalState = tableState
      if (finalState != null &&
        !finalState.isMatchFinished &&
        !_uiState.value.isPaused &&
        finalState.currentPlayer.playerId == HUMAN_PLAYER_ID
      ) {
        val turnHint = if (finalState.pendingDrawPenalty > 0) {
          "আপনার ওপর +${finalState.pendingDrawPenalty} পেনাল্টি! তাস টানুন অথবা বাউন্স ব্যাক দিন"
        } else {
          "আপনার চাল — মিলিয়ে তাস ফেলুন"
        }
        _uiState.update { it.copy(statusMessage = turnHint) }
      }
    }
  }

  private fun resumeTurnTimerIfNeeded() {
    restartTurnTimer(resetToFull = false)
  }

  private fun restartTurnTimer(resetToFull: Boolean = true) {
    timerJob?.cancel()
    val state = tableState ?: return
    if (state.isMatchFinished) {
      _uiState.update { it.copy(turnTimerSeconds = 0) }
      return
    }

    if (resetToFull) {
      _uiState.update {
        it.copy(turnTimerSeconds = GameBoardUiState.TURN_DURATION_SECONDS)
      }
    }

    val currentUi = _uiState.value
    if (currentUi.isPaused ||
      currentUi.pendingWildCard != null ||
      currentUi.pendingSwapCard != null ||
      currentUi.mysteryOutcomeText != null
    ) {
      return
    }

    val startingSeconds = currentUi.turnTimerSeconds.coerceAtLeast(1)
    timerJob = viewModelScope.launch {
      for (nextSec in (startingSeconds - 1) downTo 0) {
        delay(1000L)
        val latestUi = _uiState.value
        val activeState = tableState ?: break
        if (activeState.isMatchFinished ||
          latestUi.isPaused ||
          latestUi.pendingWildCard != null ||
          latestUi.pendingSwapCard != null ||
          latestUi.mysteryOutcomeText != null
        ) {
          break
        }

        _uiState.update { it.copy(turnTimerSeconds = nextSec) }

        if (nextSec == 0) {
          if (activeState.currentPlayer.playerId == HUMAN_PLAYER_ID) {
            onDrawCardClicked()
          }
          break
        }
      }
    }
  }

  private fun handleEventEffects(event: GameEvent?, state: GameTableState) {
    when (event) {
      is GameEvent.CardPlayed -> {
        when (event.card.type) {
          CardType.DRAW_TWO -> triggerVisualEffect(
            type = BoardEffectType.ATTACK,
            title = "দুই টানো (+২) আক্রমণ!",
            subtitle = "${event.resolvedColor.toBengaliColorName()} রঙ সক্রিয়",
          )
          CardType.WILD_DRAW_FOUR -> triggerVisualEffect(
            type = BoardEffectType.ATTACK,
            title = "মহাচাল (+৪) আক্রমণ!",
            subtitle = "নতুন রঙ: ${event.resolvedColor.toBengaliColorName()}",
          )
          CardType.THUNDERBOLT_PLUS_THREE -> triggerVisualEffect(
            type = BoardEffectType.ATTACK,
            title = "বজ্রপাত (+৩) আঘাত!",
            subtitle = "নতুন রঙ: ${event.resolvedColor.toBengaliColorName()}",
          )
          else -> Unit
        }
      }
      is GameEvent.CardsDrawn -> {
        if (event.recycledFromDiscardCount > 0) {
          triggerVisualEffect(
            type = BoardEffectType.SHUFFLE,
            title = "তাস শাফল হয়েছে",
            subtitle = "ডিসকার্ড পাইল থেকে ${event.recycledFromDiscardCount}টি তাস ফেরত এসেছে",
          )
        }
      }
      is GameEvent.PenaltyBounced -> {
        val targetName = state.playerById(event.redirectedToPlayerId)?.displayName ?: "প্রতিপক্ষ"
        triggerVisualEffect(
          type = BoardEffectType.SHIELD,
          title = "বাউন্স ব্যাক বর্ম!",
          subtitle = "+${event.penaltyCount} পেনাল্টি $targetName-এর দিকে ফেরত গেছে",
        )
      }
      is GameEvent.HandsSwapped -> {
        val actorName = state.playerById(event.playerId)?.displayName ?: "খেলোয়াড়"
        val targetName = state.playerById(event.targetPlayerId)?.displayName ?: "প্রতিপক্ষ"
        triggerVisualEffect(
          type = BoardEffectType.CARD_EXCHANGE,
          title = "হাতবদল সম্পন্ন!",
          subtitle = "$actorName ⇄ $targetName",
        )
      }
      is GameEvent.EveryoneDrewOne -> {
        triggerVisualEffect(
          type = BoardEffectType.ATTACK,
          title = "সবাই মিলে +১!",
          subtitle = "অন্য সব খেলোয়াড় ১টি করে তাস টেনেছেন",
        )
      }
      is GameEvent.MysteryTriggered -> {
        val actorName = state.playerById(event.playerId)?.displayName ?: "খেলোয়াড়"
        when (event.outcome) {
          MysteryOutcome.SHIELD_AND_SKIP_NEXT -> triggerVisualEffect(
            type = BoardEffectType.SHIELD,
            title = "রহস্য বর্ম সক্রিয়!",
            subtitle = event.outcome.toBengaliDescription(),
          )
          MysteryOutcome.NEXT_PLAYER_DRAWS_TWO -> triggerVisualEffect(
            type = BoardEffectType.ATTACK,
            title = "রহস্য আঘাত (+২)!",
            subtitle = event.outcome.toBengaliDescription(),
          )
          else -> triggerVisualEffect(
            type = BoardEffectType.CARD_EXCHANGE,
            title = "রহস্য কার্ড!",
            subtitle = event.outcome.toBengaliDescription(),
          )
        }
        if (event.playerId == HUMAN_PLAYER_ID) {
          _uiState.update {
            it.copy(mysteryOutcomeText = "$actorName: ${event.outcome.toBengaliDescription()}")
          }
        }
      }
      else -> Unit
    }
  }

  private fun triggerVisualEffect(
    type: BoardEffectType,
    title: String,
    subtitle: String,
  ) {
    effectDismissJob?.cancel()
    val effect = BoardVisualEffect(
      effectId = ++effectCounter,
      type = type,
      title = title,
      subtitle = subtitle,
    )
    _uiState.update { it.copy(activeVisualEffect = effect) }
    effectDismissJob = viewModelScope.launch {
      delay(1150L)
      _uiState.update { state ->
        if (state.activeVisualEffect?.effectId == effect.effectId) {
          state.copy(activeVisualEffect = null)
        } else {
          state
        }
      }
    }
  }

  private fun publishCurrentUi(state: GameTableState, statusMessage: String) {
    val current = _uiState.value
    val declared = if (state.players.first().cardCount > 2) false else current.hasDeclaredLastCard
    publishTableState(
      mode = current.mode,
      difficulty = current.difficulty,
      avatarId = current.playerAvatar.id,
      useBengaliNumerals = current.useBengaliNumerals,
      state = state,
      statusOverride = statusMessage,
      hasDeclaredLastCard = declared,
      isPaused = current.isPaused,
    )
  }

  private fun publishTableState(
    mode: GameModeId,
    difficulty: AiDifficulty,
    avatarId: Int,
    useBengaliNumerals: Boolean,
    state: GameTableState,
    statusOverride: String,
    hasDeclaredLastCard: Boolean,
    isPaused: Boolean,
  ) {
    val seatLabels = listOf("উত্তর", "পশ্চিম", "পূর্ব")
    val humanSeat = state.players.first()
    val opponentSeats = state.players.drop(1).mapIndexed { idx, seat ->
      val botConfig = aiPlayerConfigs[seat.playerId]
      val avatar = botConfig?.let { RongmohalAvatar.byId(it.profile.preferredAvatarId) }
        ?: RongmohalAvatar.entries[(avatarId + idx + 1) % RongmohalAvatar.entries.size]
      OpponentSeatState(
        playerId = seat.playerId,
        name = seat.displayName,
        personaSubtitle = botConfig?.profile?.personaTitleBengali ?: "বট",
        avatar = avatar,
        cardCount = seat.cardCount,
        seatLabel = seatLabels.getOrElse(idx) { "উত্তর" },
      )
    }

    val isHumanTurn = state.currentPlayer.playerId == HUMAN_PLAYER_ID && !state.isMatchFinished
    val handUi = humanSeat.hand.map { card ->
      val playable = isHumanTurn && CardValidator.isCardPlayableOn(
        card = card,
        topCard = state.topDiscardCard,
        activeColor = state.activeColor,
        pendingDrawPenalty = state.pendingDrawPenalty,
      )
      HandCardItemUi(
        domainCard = card,
        uiCard = card.toUiCardModel(),
        isPlayable = playable,
      )
    }

    // Calculate real point totals from held cards when match finishes
    val humanWon = state.winnerId == HUMAN_PLAYER_ID
    val winnerSeat = state.winnerId?.let { state.playerById(it) }
    val winnerAvatar = if (humanWon) {
      RongmohalAvatar.byId(avatarId)
    } else {
      aiPlayerConfigs[state.winnerId]?.let { RongmohalAvatar.byId(it.profile.preferredAvatarId) }
        ?: RongmohalAvatar.byId(avatarId)
    }

    val humanPointsLeft = humanSeat.hand.sumOf { it.pointValue }
    val opponentsPointsLeft = state.players.drop(1).sumOf { seat -> seat.hand.sumOf { it.pointValue } }
    val winnerSeatPointsLeft = winnerSeat?.hand?.sumOf { it.pointValue } ?: 0
    val totalLosersPointsLeft = (humanPointsLeft + opponentsPointsLeft - winnerSeatPointsLeft).coerceAtLeast(0)
    val finalWinnerScore = 50 + totalLosersPointsLeft

    val finalHumanScore = if (humanWon) {
      finalWinnerScore
    } else {
      (40 - humanPointsLeft / 3).coerceIn(10, finalWinnerScore - 10)
    }
    val finalOppScore = if (!humanWon) {
      finalWinnerScore
    } else {
      (40 - opponentsPointsLeft / 4).coerceIn(15, finalWinnerScore - 10)
    }

    val canDeclare = !state.isMatchFinished && humanSeat.cardCount in 1..2 && !hasDeclaredLastCard

    _uiState.update { prev ->
      prev.copy(
        mode = mode,
        deckType = state.deckType,
        difficulty = difficulty,
        playerName = humanSeat.displayName,
        playerAvatar = RongmohalAvatar.byId(avatarId),
        opponents = opponentSeats,
        playerHand = handUi,
        topDiscardCard = state.topDiscardCard.toUiCardModel(),
        activeSuit = state.activeColor.toRongmohalSuit(),
        drawPileCount = state.drawPile.size,
        discardPileCount = state.discardPile.size,
        drawPulseCount = drawPulseCounter,
        recyclePulseCount = recycleCounter,
        pendingDrawPenalty = state.pendingDrawPenalty,
        currentTurnIndex = state.currentPlayerIndex,
        isClockwise = state.isClockwise,
        hasDeclaredLastCard = hasDeclaredLastCard,
        canDeclareLastCard = canDeclare,
        isPaused = isPaused,
        statusMessage = statusOverride,
        useBengaliNumerals = useBengaliNumerals,
        matchFinished = state.isMatchFinished,
        playerWon = humanWon,
        winnerName = winnerSeat?.displayName ?: "",
        winnerAvatar = winnerAvatar,
        finalPlayerScore = finalHumanScore,
        finalOpponentScore = finalOppScore,
      )
    }
  }

  private fun describeEvent(event: GameEvent?, state: GameTableState): String {
    val actorName = when (event) {
      is GameEvent.CardPlayed -> state.playerById(event.playerId)?.displayName ?: "খেলোয়াড়"
      is GameEvent.CardsDrawn -> state.playerById(event.playerId)?.displayName ?: "খেলোয়াড়"
      is GameEvent.PenaltyBounced -> state.playerById(event.bouncedByPlayerId)?.displayName ?: "খেলোয়াড়"
      is GameEvent.HandsSwapped -> state.playerById(event.playerId)?.displayName ?: "খেলোয়াড়"
      is GameEvent.EveryoneDrewOne -> state.playerById(event.initiatorPlayerId)?.displayName ?: "খেলোয়াড়"
      is GameEvent.MysteryTriggered -> state.playerById(event.playerId)?.displayName ?: "খেলোয়াড়"
      is GameEvent.MatchWon -> event.winnerName
      else -> ""
    }

    return when (event) {
      is GameEvent.CardPlayed -> {
        val cardTitle = if (event.card is GameCard.NumberCard) {
          "${event.card.color.toBengaliColorName()} ${event.card.number}"
        } else {
          "${event.card.type.toBengaliTitle()} (${event.resolvedColor.toBengaliColorName()})"
        }
        "$actorName খেলেছেন: $cardTitle"
      }
      is GameEvent.CardsDrawn -> "$actorName ${event.drawnCards.size}টি তাস টেনেছেন"
      is GameEvent.PenaltyBounced -> {
        val targetName = state.playerById(event.redirectedToPlayerId)?.displayName ?: "প্রতিপক্ষ"
        "বাউন্স ব্যাক! $actorName +${event.penaltyCount} পেনাল্টি $targetName-এর দিকে ফেরত পাঠিয়েছেন"
      }
      is GameEvent.HandsSwapped -> {
        val targetName = state.playerById(event.targetPlayerId)?.displayName ?: "প্রতিপক্ষ"
        "হাতবদল! $actorName এবং $targetName তাস অদলবদল করেছেন"
      }
      is GameEvent.EveryoneDrewOne -> "সবাই মিলে +১! অন্য সব খেলোয়াড় ১টি করে তাস টেনেছেন"
      is GameEvent.MysteryTriggered -> "$actorName রহস্য কার্ড খেলেছেন — ${event.outcome.toBengaliDescription()}"
      is GameEvent.MatchWon -> "${event.winnerName} সব তাস শেষ করে বিজয়ী হয়েছেন!"
      else -> "আসর চলমান"
    }
  }

  override fun onCleared() {
    super.onCleared()
    turnJob?.cancel()
    timerJob?.cancel()
    effectDismissJob?.cancel()
  }

  companion object {
    const val HUMAN_PLAYER_ID = "player_human"
  }
}
