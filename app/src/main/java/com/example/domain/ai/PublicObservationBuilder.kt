package com.example.domain.ai

import com.example.domain.game.CardColor
import com.example.domain.game.CardValidator
import com.example.domain.game.GameEvent
import com.example.domain.game.GameTableState

/**
 * Tracks strictly public table history events (colors played or failed to match when drawing)
 * so Hard AI can make realistic predictions without accessing hidden cards.
 */
data class PublicPlayLedger(
  val weakColorsByPlayer: Map<String, Set<CardColor>> = emptyMap(),
  val playedColorsByPlayer: Map<String, Map<CardColor, Int>> = emptyMap(),
) {
  fun recordStateTransition(
    previousActiveColor: CardColor,
    event: GameEvent?,
  ): PublicPlayLedger {
    return when (event) {
      is GameEvent.CardsDrawn -> {
        val currentWeak = weakColorsByPlayer[event.playerId].orEmpty()
        copy(
          weakColorsByPlayer = weakColorsByPlayer + (event.playerId to (currentWeak + previousActiveColor)),
        )
      }
      is GameEvent.CardPlayed -> {
        val color = event.resolvedColor
        if (!color.isStandard) return this
        val existingCounts = playedColorsByPlayer[event.playerId].orEmpty().toMutableMap()
        existingCounts[color] = (existingCounts[color] ?: 0) + 1
        val existingWeak = weakColorsByPlayer[event.playerId].orEmpty() - color
        copy(
          playedColorsByPlayer = playedColorsByPlayer + (event.playerId to existingCounts.toMap()),
          weakColorsByPlayer = weakColorsByPlayer + (event.playerId to existingWeak),
        )
      }
      else -> this
    }
  }
}

/**
 * Builds [PublicGameObservation] from [GameTableState] for a single AI seat,
 * stripping out every opponent's hidden hand and the draw pile contents.
 */
object PublicObservationBuilder {

  fun build(
    state: GameTableState,
    aiPlayerId: String,
    ledger: PublicPlayLedger = PublicPlayLedger(),
  ): PublicGameObservation {
    val aiIndex = state.players.indexOfFirst { it.playerId == aiPlayerId }
    require(aiIndex >= 0) { "AI player $aiPlayerId does not exist in table state" }

    val aiSeat = state.players[aiIndex]
    val totalPlayers = state.players.size
    val legalPlays = aiSeat.hand.filter { card ->
      CardValidator.isCardPlayableOn(
        card = card,
        topCard = state.topDiscardCard,
        activeColor = state.activeColor,
        pendingDrawPenalty = state.pendingDrawPenalty,
      )
    }

    val directionStep = if (state.isClockwise) 1 else -1
    val immediateNextIdx = (aiIndex + directionStep + totalPlayers) % totalPlayers
    val immediatePrevIdx = (aiIndex - directionStep + totalPlayers) % totalPlayers

    val opponents = mutableListOf<PublicOpponentInfo>()
    for (offset in 1 until totalPlayers) {
      val oppIdx = (aiIndex + (offset * directionStep) + totalPlayers) % totalPlayers
      val seat = state.players[oppIdx]
      opponents += PublicOpponentInfo(
        playerId = seat.playerId,
        displayName = seat.displayName,
        remainingCardCount = seat.cardCount, // Public count only; hand list never exposed
        seatOrderFromAi = offset,
        isImmediateNextInTurn = oppIdx == immediateNextIdx,
        isImmediatePreviousInTurn = oppIdx == immediatePrevIdx,
        publiclyWeakColors = ledger.weakColorsByPlayer[seat.playerId].orEmpty(),
        publiclyPlayedColors = ledger.playedColorsByPlayer[seat.playerId].orEmpty(),
      )
    }

    return PublicGameObservation(
      aiPlayerId = aiPlayerId,
      aiHand = aiSeat.hand,
      legalPlays = legalPlays,
      topDiscardCard = state.topDiscardCard,
      activeColor = state.activeColor,
      discardPileHistory = state.discardPile.cards,
      drawPileRemainingCount = state.drawPile.size,
      isClockwise = state.isClockwise,
      pendingDrawPenalty = state.pendingDrawPenalty,
      opponents = opponents,
    )
  }
}
