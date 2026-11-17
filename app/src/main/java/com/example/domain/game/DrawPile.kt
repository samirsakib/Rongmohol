package com.example.domain.game

/**
 * Immutable draw pile enforcing unique card IDs and safe draw/replenish transitions.
 */
data class DrawPile(
  val cards: List<GameCard>,
) {
  init {
    DefaultDeckGenerator.ensureNoDuplicateIds(cards)
  }

  val size: Int
    get() = cards.size

  val isEmpty: Boolean
    get() = cards.isEmpty()

  fun peek(): GameCard? = cards.firstOrNull()

  fun drawTop(): DrawSingleResult? {
    if (cards.isEmpty()) return null
    val top = cards.first()
    val remaining = cards.drop(1)
    return DrawSingleResult(drawnCard = top, remainingPile = DrawPile(remaining))
  }

  fun replenish(
    recycledCards: List<GameCard>,
    shuffler: ShuffleAlgorithm,
  ): DrawPile {
    if (recycledCards.isEmpty()) return this
    val shuffled = shuffler.shuffle(recycledCards)
    val merged = cards + shuffled
    DefaultDeckGenerator.ensureNoDuplicateIds(merged)
    return DrawPile(merged)
  }

  data class DrawSingleResult(
    val drawnCard: GameCard,
    val remainingPile: DrawPile,
  )
}
