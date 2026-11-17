package com.example.domain.game

/**
 * Immutable discard pile tracking played cards and active color,
 * and recycling all cards except the current top card when draw pile empties.
 */
data class DiscardPile(
  val cards: List<GameCard>,
  val activeColor: CardColor,
) {
  init {
    require(cards.isNotEmpty()) { "DiscardPile must contain at least the starter card" }
    require(activeColor.isStandard) { "Active color on DiscardPile must be standard, got $activeColor" }
    DefaultDeckGenerator.ensureNoDuplicateIds(cards)
  }

  val size: Int
    get() = cards.size

  val topCard: GameCard
    get() = cards.last()

  fun canRecycle(): Boolean = cards.size > 1

  fun push(card: GameCard, nextActiveColor: CardColor): DiscardPile {
    require(nextActiveColor.isStandard) {
      "Next active color must be RED, BLUE, GREEN, or YELLOW, got $nextActiveColor"
    }
    val updated = cards + card
    DefaultDeckGenerator.ensureNoDuplicateIds(updated)
    return DiscardPile(cards = updated, activeColor = nextActiveColor)
  }

  /**
   * Extracts every card below [topCard], leaving [topCard] and [activeColor] intact.
   */
  fun recycleKeepingTop(shuffler: ShuffleAlgorithm): RecycleResult {
    if (cards.size <= 1) {
      return RecycleResult(
        recycledCards = emptyList(),
        remainingDiscardPile = this,
      )
    }
    val underTop = cards.dropLast(1)
    val top = cards.last()
    val shuffled = shuffler.shuffle(underTop)
    return RecycleResult(
      recycledCards = shuffled,
      remainingDiscardPile = DiscardPile(cards = listOf(top), activeColor = activeColor),
    )
  }

  data class RecycleResult(
    val recycledCards: List<GameCard>,
    val remainingDiscardPile: DiscardPile,
  )
}
