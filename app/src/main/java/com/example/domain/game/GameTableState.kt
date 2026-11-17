package com.example.domain.game

/**
 * Pure domain state for a player's seat and held cards.
 */
data class PlayerSeatState(
  val playerId: String,
  val displayName: String,
  val hand: List<GameCard>,
) {
  val cardCount: Int
    get() = hand.size
}

/**
 * Domain-level game events emitted on state transitions for UI/animation observation
 * without embedding any UI code inside the domain layer.
 */
sealed interface GameEvent {
  data class MatchStarted(val deckType: DeckType, val playerCount: Int) : GameEvent

  data class CardPlayed(
    val playerId: String,
    val card: GameCard,
    val resolvedColor: CardColor,
  ) : GameEvent

  data class CardsDrawn(
    val playerId: String,
    val drawnCards: List<GameCard>,
    val recycledFromDiscardCount: Int,
  ) : GameEvent

  data class PenaltyBounced(
    val bouncedByPlayerId: String,
    val redirectedToPlayerId: String,
    val penaltyCount: Int,
  ) : GameEvent

  data class HandsSwapped(
    val playerId: String,
    val targetPlayerId: String,
  ) : GameEvent

  data class EveryoneDrewOne(
    val initiatorPlayerId: String,
    val affectedPlayerIds: List<String>,
    val recycledFromDiscardCount: Int,
  ) : GameEvent

  data class MysteryTriggered(
    val playerId: String,
    val outcome: MysteryOutcome,
  ) : GameEvent

  data class MatchWon(
    val winnerId: String,
    val winnerName: String,
  ) : GameEvent
}

/**
 * Complete immutable snapshot of an active card table.
 */
data class GameTableState(
  val deckType: DeckType,
  val players: List<PlayerSeatState>,
  val currentPlayerIndex: Int,
  val isClockwise: Boolean,
  val drawPile: DrawPile,
  val discardPile: DiscardPile,
  val pendingDrawPenalty: Int = 0,
  val lastPenaltySenderId: String? = null,
  val winnerId: String? = null,
  val allKnownCardIds: Set<String>,
  val lastEvent: GameEvent? = null,
  val turnNumber: Int = 1,
) {
  val currentPlayer: PlayerSeatState
    get() = players[currentPlayerIndex]

  val isMatchFinished: Boolean
    get() = winnerId != null

  val activeColor: CardColor
    get() = discardPile.activeColor

  val topDiscardCard: GameCard
    get() = discardPile.topCard

  fun playerById(playerId: String): PlayerSeatState? =
    players.firstOrNull { it.playerId == playerId }
}
