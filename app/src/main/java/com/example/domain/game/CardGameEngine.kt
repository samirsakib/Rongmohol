package com.example.domain.game

import kotlin.random.Random

sealed interface EngineStepResult {
  data class Success(val state: GameTableState) : EngineStepResult
  data class Rejected(
    val reason: ValidationFailureReason,
    val message: String,
  ) : EngineStepResult
}

/**
 * Pure Kotlin card game engine handling deck creation, dealing, validated card movements,
 * empty draw-pile recycling (preserving the top discard card), and all Classic + Special cards.
 */
class CardGameEngine(
  private val deckGenerator: DeckGenerator = DefaultDeckGenerator(),
  private val shuffler: ShuffleAlgorithm = FisherYatesShuffleAlgorithm(),
  private val random: Random = Random.Default,
) {
  private val distributor = CardDistributor(shuffler = shuffler)

  fun createMatch(
    deckType: DeckType,
    players: List<Pair<String, String>>,
    cardsPerPlayer: Int = 7,
    classicConfig: ClassicDeckConfig = ClassicDeckConfig(),
    specialConfig: SpecialDeckConfig = SpecialDeckConfig(),
  ): GameTableState {
    val playerIds = players.map { it.first }
    val deck = when (deckType) {
      DeckType.CLASSIC -> deckGenerator.generateClassicDeck(classicConfig)
      DeckType.SPECIAL -> deckGenerator.generateSpecialDeck(specialConfig)
    }
    val dealt = distributor.distribute(
      deck = deck,
      playerIds = playerIds,
      cardsPerPlayer = cardsPerPlayer,
    )
    val seats = players.map { (id, name) ->
      PlayerSeatState(
        playerId = id,
        displayName = name,
        hand = dealt.hands.getValue(id),
      )
    }
    val initialState = GameTableState(
      deckType = deckType,
      players = seats,
      currentPlayerIndex = 0,
      isClockwise = true,
      drawPile = dealt.drawPile,
      discardPile = dealt.discardPile,
      pendingDrawPenalty = 0,
      lastPenaltySenderId = null,
      winnerId = null,
      allKnownCardIds = deck.map { it.id }.toSet(),
      lastEvent = GameEvent.MatchStarted(deckType = deckType, playerCount = seats.size),
      turnNumber = 1,
    )
    require(CardValidator.validateStateIntegrity(initialState).isValid)
    return initialState
  }

  fun playCard(
    state: GameTableState,
    playerId: String,
    cardId: String,
    chosenColor: CardColor? = null,
    targetPlayerId: String? = null,
  ): EngineStepResult {
    val player = state.playerById(playerId)
      ?: return EngineStepResult.Rejected(
        reason = ValidationFailureReason.NOT_PLAYER_TURN,
        message = "খেলোয়াড় খুঁজে পাওয়া যায়নি",
      )

    val card = player.hand.firstOrNull { it.id == cardId }
      ?: return EngineStepResult.Rejected(
        reason = ValidationFailureReason.CARD_NOT_IN_HAND,
        message = "এই তাসটি খেলোয়াড়ের হাতে নেই",
      )

    val validation = CardValidator.validatePlay(
      state = state,
      playerId = playerId,
      card = card,
      chosenColor = chosenColor,
      targetPlayerId = targetPlayerId,
    )
    if (validation is ValidationResult.Invalid) {
      return EngineStepResult.Rejected(validation.reason, validation.message)
    }

    val resolvedColor = if (card.color.isStandard) card.color else chosenColor!!
    val updatedHand = player.hand.filterNot { it.id == card.id }
    val updatedPlayers = state.players.toMutableList()
    val playerIdx = updatedPlayers.indexOfFirst { it.playerId == playerId }
    updatedPlayers[playerIdx] = player.copy(hand = updatedHand)

    val updatedDiscard = state.discardPile.push(card, resolvedColor)

    // Immediate win if player emptied their hand
    if (updatedHand.isEmpty()) {
      val winState = state.copy(
        players = updatedPlayers.toList(),
        discardPile = updatedDiscard,
        pendingDrawPenalty = 0,
        lastPenaltySenderId = null,
        winnerId = player.playerId,
        lastEvent = GameEvent.MatchWon(
          winnerId = player.playerId,
          winnerName = player.displayName,
        ),
        turnNumber = state.turnNumber + 1,
      )
      return finalizeIfValid(winState)
    }

    var drawPile = state.drawPile
    var discardPile = updatedDiscard
    var clockwise = state.isClockwise
    var skipNext = false
    var nextPenalty = state.pendingDrawPenalty
    var penaltySenderId = state.lastPenaltySenderId
    var explicitNextTurnIndex: Int? = null
    var resolvedEvent: GameEvent = GameEvent.CardPlayed(
      playerId = playerId,
      card = card,
      resolvedColor = resolvedColor,
    )

    when (card.type) {
      CardType.NUMBER, CardType.WILD -> {
        nextPenalty = 0
        penaltySenderId = null
      }

      CardType.SKIP -> {
        skipNext = true
        nextPenalty = 0
        penaltySenderId = null
      }

      CardType.REVERSE -> {
        clockwise = !clockwise
        if (updatedPlayers.size == 2) {
          skipNext = true
        }
        nextPenalty = 0
        penaltySenderId = null
      }

      CardType.DRAW_TWO -> {
        nextPenalty += 2
        penaltySenderId = playerId
      }

      CardType.WILD_DRAW_FOUR -> {
        nextPenalty += 4
        penaltySenderId = playerId
      }

      CardType.THUNDERBOLT_PLUS_THREE -> {
        nextPenalty += 3
        penaltySenderId = playerId
      }

      CardType.HAND_SWAP -> {
        nextPenalty = 0
        penaltySenderId = null
        val fallbackIdx = advanceIndex(playerIdx, updatedPlayers.size, clockwise)
        val resolvedTargetId = targetPlayerId ?: updatedPlayers[fallbackIdx].playerId
        val targetIdx = updatedPlayers.indexOfFirst { it.playerId == resolvedTargetId }
        if (targetIdx >= 0 && targetIdx != playerIdx) {
          val myCards = updatedPlayers[playerIdx].hand
          val targetCards = updatedPlayers[targetIdx].hand
          updatedPlayers[playerIdx] = updatedPlayers[playerIdx].copy(hand = targetCards)
          updatedPlayers[targetIdx] = updatedPlayers[targetIdx].copy(hand = myCards)
          resolvedEvent = GameEvent.HandsSwapped(
            playerId = playerId,
            targetPlayerId = resolvedTargetId,
          )
        }
      }

      CardType.BOUNCE_BACK -> {
        if (state.pendingDrawPenalty > 0) {
          clockwise = !clockwise
          val bouncedPenalty = state.pendingDrawPenalty
          val targetIdx = state.lastPenaltySenderId
            ?.let { sender -> updatedPlayers.indexOfFirst { it.playerId == sender } }
            ?.takeIf { it >= 0 && it != playerIdx }
            ?: advanceIndex(playerIdx, updatedPlayers.size, clockwise)
          explicitNextTurnIndex = targetIdx
          nextPenalty = bouncedPenalty
          penaltySenderId = playerId
          resolvedEvent = GameEvent.PenaltyBounced(
            bouncedByPlayerId = playerId,
            redirectedToPlayerId = updatedPlayers[targetIdx].playerId,
            penaltyCount = bouncedPenalty,
          )
        } else {
          clockwise = !clockwise
          if (updatedPlayers.size == 2) {
            skipNext = true
          }
          nextPenalty = 0
          penaltySenderId = null
        }
      }

      CardType.EVERYONE_DRAW_ONE -> {
        nextPenalty = 0
        penaltySenderId = null
        var totalRecycled = 0
        val affected = mutableListOf<String>()
        for (idx in updatedPlayers.indices) {
          if (idx == playerIdx) continue
          val drawResult = pullSingleCardWithRecycle(drawPile, discardPile)
          if (drawResult != null) {
            drawPile = drawResult.drawPile
            discardPile = drawResult.discardPile
            totalRecycled += drawResult.recycledCount
            val seat = updatedPlayers[idx]
            updatedPlayers[idx] = seat.copy(hand = seat.hand + drawResult.card)
            affected += seat.playerId
          }
        }
        resolvedEvent = GameEvent.EveryoneDrewOne(
          initiatorPlayerId = playerId,
          affectedPlayerIds = affected,
          recycledFromDiscardCount = totalRecycled,
        )
      }

      CardType.MYSTERY_CARD -> {
        nextPenalty = 0
        penaltySenderId = null
        val outcomes = MysteryOutcome.entries
        val outcome = outcomes[random.nextInt(outcomes.size)]
        when (outcome) {
          MysteryOutcome.NEXT_PLAYER_DRAWS_TWO -> {
            nextPenalty = 2
            penaltySenderId = playerId
          }
          MysteryOutcome.GIFT_RANDOM_CARD_TO_NEXT -> {
            val currentHand = updatedPlayers[playerIdx].hand
            if (currentHand.isNotEmpty()) {
              val nextIdx = advanceIndex(playerIdx, updatedPlayers.size, clockwise)
              val giftIndex = random.nextInt(currentHand.size)
              val giftedCard = currentHand[giftIndex]
              updatedPlayers[playerIdx] = updatedPlayers[playerIdx].copy(
                hand = currentHand.filterNot { it.id == giftedCard.id },
              )
              updatedPlayers[nextIdx] = updatedPlayers[nextIdx].copy(
                hand = updatedPlayers[nextIdx].hand + giftedCard,
              )
            }
          }
          MysteryOutcome.SHIELD_AND_SKIP_NEXT -> {
            skipNext = true
          }
          MysteryOutcome.REVERSE_AND_ROTATE_COLOR -> {
            clockwise = !clockwise
            val shiftedColor = CardColor.STANDARD_COLORS
              .first { it != resolvedColor }
            discardPile = discardPile.copy(activeColor = shiftedColor)
          }
        }
        resolvedEvent = GameEvent.MysteryTriggered(
          playerId = playerId,
          outcome = outcome,
        )
      }
    }

    // Check if Mystery gift emptied the active player's hand
    if (updatedPlayers[playerIdx].hand.isEmpty()) {
      val winState = state.copy(
        players = updatedPlayers.toList(),
        drawPile = drawPile,
        discardPile = discardPile,
        isClockwise = clockwise,
        pendingDrawPenalty = 0,
        lastPenaltySenderId = null,
        winnerId = player.playerId,
        lastEvent = GameEvent.MatchWon(
          winnerId = player.playerId,
          winnerName = player.displayName,
        ),
        turnNumber = state.turnNumber + 1,
      )
      return finalizeIfValid(winState)
    }

    val nextIndex = explicitNextTurnIndex ?: run {
      val firstStep = advanceIndex(playerIdx, updatedPlayers.size, clockwise)
      if (skipNext) advanceIndex(firstStep, updatedPlayers.size, clockwise) else firstStep
    }

    val nextState = state.copy(
      players = updatedPlayers.toList(),
      currentPlayerIndex = nextIndex,
      isClockwise = clockwise,
      drawPile = drawPile,
      discardPile = discardPile,
      pendingDrawPenalty = nextPenalty,
      lastPenaltySenderId = penaltySenderId,
      lastEvent = resolvedEvent,
      turnNumber = state.turnNumber + 1,
    )
    return finalizeIfValid(nextState)
  }

  fun drawCards(
    state: GameTableState,
    playerId: String,
  ): EngineStepResult {
    val validation = CardValidator.validateDraw(state, playerId)
    if (validation is ValidationResult.Invalid) {
      return EngineStepResult.Rejected(validation.reason, validation.message)
    }

    val countToDraw = if (state.pendingDrawPenalty > 0) state.pendingDrawPenalty else 1
    var currentDrawPile = state.drawPile
    var currentDiscardPile = state.discardPile
    val drawnCards = mutableListOf<GameCard>()
    var totalRecycledCount = 0

    repeat(countToDraw) {
      val result = pullSingleCardWithRecycle(currentDrawPile, currentDiscardPile)
      if (result != null) {
        currentDrawPile = result.drawPile
        currentDiscardPile = result.discardPile
        drawnCards += result.card
        totalRecycledCount += result.recycledCount
      }
    }

    if (drawnCards.isEmpty()) {
      return EngineStepResult.Rejected(
        reason = ValidationFailureReason.EMPTY_DRAW_PILE_NO_RECYCLABLE_CARDS,
        message = "ড্র করার মতো কোনো তাস অবশিষ্ট নেই",
      )
    }

    val updatedPlayers = state.players.toMutableList()
    val playerIdx = state.currentPlayerIndex
    val seat = updatedPlayers[playerIdx]
    updatedPlayers[playerIdx] = seat.copy(hand = seat.hand + drawnCards)

    val nextIndex = advanceIndex(playerIdx, updatedPlayers.size, state.isClockwise)
    val nextState = state.copy(
      players = updatedPlayers.toList(),
      currentPlayerIndex = nextIndex,
      drawPile = currentDrawPile,
      discardPile = currentDiscardPile,
      pendingDrawPenalty = 0,
      lastPenaltySenderId = null,
      lastEvent = GameEvent.CardsDrawn(
        playerId = playerId,
        drawnCards = drawnCards.toList(),
        recycledFromDiscardCount = totalRecycledCount,
      ),
      turnNumber = state.turnNumber + 1,
    )
    return finalizeIfValid(nextState)
  }

  /**
   * Draws penalty cards for a player who dropped to 1 card without declaring "শেষ তাস!",
   * preserving turn flow and full-table card ID conservation.
   */
  fun applyUndeclaredLastCardPenalty(
    state: GameTableState,
    playerId: String,
    penaltyCount: Int = 2,
  ): EngineStepResult {
    if (state.isMatchFinished) {
      return EngineStepResult.Success(state)
    }
    val playerIdx = state.players.indexOfFirst { it.playerId == playerId }
    if (playerIdx < 0) {
      return EngineStepResult.Rejected(
        reason = ValidationFailureReason.NOT_PLAYER_TURN,
        message = "খেলোয়াড় খুঁজে পাওয়া যায়নি",
      )
    }

    var currentDrawPile = state.drawPile
    var currentDiscardPile = state.discardPile
    val drawnCards = mutableListOf<GameCard>()
    var totalRecycledCount = 0

    repeat(penaltyCount.coerceAtLeast(1)) {
      val result = pullSingleCardWithRecycle(currentDrawPile, currentDiscardPile)
      if (result != null) {
        currentDrawPile = result.drawPile
        currentDiscardPile = result.discardPile
        drawnCards += result.card
        totalRecycledCount += result.recycledCount
      }
    }

    if (drawnCards.isEmpty()) {
      return EngineStepResult.Success(state)
    }

    val updatedPlayers = state.players.toMutableList()
    val seat = updatedPlayers[playerIdx]
    updatedPlayers[playerIdx] = seat.copy(hand = seat.hand + drawnCards)

    val nextState = state.copy(
      players = updatedPlayers.toList(),
      drawPile = currentDrawPile,
      discardPile = currentDiscardPile,
      lastEvent = GameEvent.CardsDrawn(
        playerId = playerId,
        drawnCards = drawnCards.toList(),
        recycledFromDiscardCount = totalRecycledCount,
      ),
    )
    return finalizeIfValid(nextState)
  }

  private fun pullSingleCardWithRecycle(
    drawPile: DrawPile,
    discardPile: DiscardPile,
  ): PullResult? {
    var activeDrawPile = drawPile
    var activeDiscardPile = discardPile
    var recycledCount = 0

    if (activeDrawPile.isEmpty) {
      if (!activeDiscardPile.canRecycle()) {
        return null
      }
      val recycled = activeDiscardPile.recycleKeepingTop(shuffler)
      recycledCount = recycled.recycledCards.size
      activeDiscardPile = recycled.remainingDiscardPile
      activeDrawPile = activeDrawPile.replenish(recycled.recycledCards, shuffler)
    }

    val single = activeDrawPile.drawTop() ?: return null
    return PullResult(
      card = single.drawnCard,
      drawPile = single.remainingPile,
      discardPile = activeDiscardPile,
      recycledCount = recycledCount,
    )
  }

  private fun advanceIndex(current: Int, playerCount: Int, clockwise: Boolean): Int {
    if (playerCount <= 1) return 0
    return if (clockwise) {
      (current + 1) % playerCount
    } else {
      (current - 1 + playerCount) % playerCount
    }
  }

  private fun finalizeIfValid(candidate: GameTableState): EngineStepResult {
    return when (val integrity = CardValidator.validateStateIntegrity(candidate)) {
      ValidationResult.Valid -> EngineStepResult.Success(candidate)
      is ValidationResult.Invalid -> EngineStepResult.Rejected(integrity.reason, integrity.message)
    }
  }

  private data class PullResult(
    val card: GameCard,
    val drawPile: DrawPile,
    val discardPile: DiscardPile,
    val recycledCount: Int,
  )
}
