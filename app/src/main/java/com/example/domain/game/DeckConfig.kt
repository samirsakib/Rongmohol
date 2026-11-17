package com.example.domain.game

/**
 * Configurable card counts for the Classic deck (standard colors + standard actions).
 * Default configuration builds a standard 108-card classic deck.
 */
data class ClassicDeckConfig(
  val zeroCountPerColor: Int = 1,
  val oneToNineCountPerColor: Int = 2,
  val skipCountPerColor: Int = 2,
  val reverseCountPerColor: Int = 2,
  val drawTwoCountPerColor: Int = 2,
  val wildCount: Int = 4,
  val wildDrawFourCount: Int = 4,
) {
  init {
    require(zeroCountPerColor >= 0) { "zeroCountPerColor must be >= 0" }
    require(oneToNineCountPerColor >= 0) { "oneToNineCountPerColor must be >= 0" }
    require(skipCountPerColor >= 0) { "skipCountPerColor must be >= 0" }
    require(reverseCountPerColor >= 0) { "reverseCountPerColor must be >= 0" }
    require(drawTwoCountPerColor >= 0) { "drawTwoCountPerColor must be >= 0" }
    require(wildCount >= 0) { "wildCount must be >= 0" }
    require(wildDrawFourCount >= 0) { "wildDrawFourCount must be >= 0" }
    require(expectedCardCount() > 0) { "ClassicDeckConfig must produce at least 1 card" }
  }

  fun expectedCardCount(): Int {
    val colorCount = CardColor.STANDARD_COLORS.size
    val perColor =
      zeroCountPerColor +
        (oneToNineCountPerColor * 9) +
        skipCountPerColor +
        reverseCountPerColor +
        drawTwoCountPerColor
    return (perColor * colorCount) + wildCount + wildDrawFourCount
  }
}

/**
 * Configurable counts for the 5 original Bengali special cards.
 */
data class SpecialCardsConfig(
  val thunderboltPlusThreeCount: Int = 3,
  val handSwapCount: Int = 2,
  val bounceBackCountPerColor: Int = 1,
  val everyoneDrawOneCount: Int = 3,
  val mysteryCardCount: Int = 3,
) {
  init {
    require(thunderboltPlusThreeCount >= 0) { "thunderboltPlusThreeCount must be >= 0" }
    require(handSwapCount >= 0) { "handSwapCount must be >= 0" }
    require(bounceBackCountPerColor >= 0) { "bounceBackCountPerColor must be >= 0" }
    require(everyoneDrawOneCount >= 0) { "everyoneDrawOneCount must be >= 0" }
    require(mysteryCardCount >= 0) { "mysteryCardCount must be >= 0" }
  }

  fun expectedSpecialCount(): Int {
    return thunderboltPlusThreeCount +
      handSwapCount +
      (bounceBackCountPerColor * CardColor.STANDARD_COLORS.size) +
      everyoneDrawOneCount +
      mysteryCardCount
  }
}

/**
 * Special deck configuration combining base standard cards with original special cards.
 */
data class SpecialDeckConfig(
  val baseClassicConfig: ClassicDeckConfig = ClassicDeckConfig(),
  val specialCardsConfig: SpecialCardsConfig = SpecialCardsConfig(),
) {
  init {
    require(specialCardsConfig.expectedSpecialCount() > 0) {
      "SpecialDeckConfig must include at least 1 special card"
    }
  }

  fun expectedCardCount(): Int =
    baseClassicConfig.expectedCardCount() + specialCardsConfig.expectedSpecialCount()
}
