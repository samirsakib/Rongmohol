package com.example.domain.game

/**
 * Result of initial shuffling and dealing to players + draw pile + starter discard pile.
 */
data class DistributedSetup(
  val hands: Map<String, List<GameCard>>,
  val drawPile: DrawPile,
  val discardPile: DiscardPile,
)

/**
 * Distributes cards round-robin to players while validating uniqueness and minimum pile sizes.
 */
class CardDistributor(
  private val shuffler: ShuffleAlgorithm = FisherYatesShuffleAlgorithm(),
) {

  fun distribute(
    deck: List<GameCard>,
    playerIds: List<String>,
    cardsPerPlayer: Int,
  ): DistributedSetup {
    require(playerIds.size >= 2) { "At least 2 players are required, got ${playerIds.size}" }
    require(playerIds.distinct().size == playerIds.size) { "Player IDs must be unique" }
    require(cardsPerPlayer >= 1) { "cardsPerPlayer must be >= 1, got $cardsPerPlayer" }
    DefaultDeckGenerator.ensureNoDuplicateIds(deck)

    val minimumRequiredCards = (playerIds.size * cardsPerPlayer) + 2
    require(deck.size >= minimumRequiredCards) {
      "Deck has ${deck.size} cards, but at least $minimumRequiredCards are required for dealing"
    }

    val shuffled = shuffler.shuffle(deck).toMutableList()
    val starterIndex = shuffled.indexOfFirst { it is GameCard.NumberCard }
      .takeIf { it >= 0 }
      ?: 0
    val starterCard = shuffled.removeAt(starterIndex)
    val starterColor = if (starterCard.color.isStandard) {
      starterCard.color
    } else {
      CardColor.RED
    }

    val handsMap = playerIds.associateWith { mutableListOf<GameCard>() }
    repeat(cardsPerPlayer) {
      for (playerId in playerIds) {
        val nextCard = shuffled.removeAt(0)
        handsMap.getValue(playerId).add(nextCard)
      }
    }

    val finalHands = handsMap.mapValues { it.value.toList() }
    val drawPile = DrawPile(shuffled.toList())
    val discardPile = DiscardPile(cards = listOf(starterCard), activeColor = starterColor)

    // Verify card conservation and uniqueness after dealing
    val allDealtCards = finalHands.values.flatten() + drawPile.cards + discardPile.cards
    DefaultDeckGenerator.ensureNoDuplicateIds(allDealtCards)
    require(allDealtCards.size == deck.size) {
      "Card conservation check failed after deal: expected ${deck.size}, got ${allDealtCards.size}"
    }

    return DistributedSetup(
      hands = finalHands,
      drawPile = drawPile,
      discardPile = discardPile,
    )
  }
}
