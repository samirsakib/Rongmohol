package com.example.domain.game

/**
 * Generates separate Classic and Special decks with configurable card counts
 * and strict duplicate-ID prevention.
 */
interface DeckGenerator {
  fun generateClassicDeck(config: ClassicDeckConfig = ClassicDeckConfig()): List<GameCard>

  fun generateSpecialCardsOnly(config: SpecialCardsConfig = SpecialCardsConfig()): List<GameCard>

  fun generateSpecialDeck(config: SpecialDeckConfig = SpecialDeckConfig()): List<GameCard>
}

class DefaultDeckGenerator : DeckGenerator {

  override fun generateClassicDeck(config: ClassicDeckConfig): List<GameCard> {
    val cards = mutableListOf<GameCard>()

    for (color in CardColor.STANDARD_COLORS) {
      val colorKey = color.name.lowercase()

      repeat(config.zeroCountPerColor) { copy ->
        cards += GameCard.NumberCard(
          id = "classic_${colorKey}_num_0_c${copy + 1}",
          color = color,
          number = 0,
        )
      }

      for (number in 1..9) {
        repeat(config.oneToNineCountPerColor) { copy ->
          cards += GameCard.NumberCard(
            id = "classic_${colorKey}_num_${number}_c${copy + 1}",
            color = color,
            number = number,
          )
        }
      }

      repeat(config.skipCountPerColor) { copy ->
        cards += GameCard.ActionCard(
          id = "classic_${colorKey}_skip_c${copy + 1}",
          color = color,
          type = CardType.SKIP,
        )
      }

      repeat(config.reverseCountPerColor) { copy ->
        cards += GameCard.ActionCard(
          id = "classic_${colorKey}_reverse_c${copy + 1}",
          color = color,
          type = CardType.REVERSE,
        )
      }

      repeat(config.drawTwoCountPerColor) { copy ->
        cards += GameCard.ActionCard(
          id = "classic_${colorKey}_draw_two_c${copy + 1}",
          color = color,
          type = CardType.DRAW_TWO,
        )
      }
    }

    repeat(config.wildCount) { copy ->
      cards += GameCard.ActionCard(
        id = "classic_wild_c${copy + 1}",
        color = CardColor.WILD,
        type = CardType.WILD,
      )
    }

    repeat(config.wildDrawFourCount) { copy ->
      cards += GameCard.ActionCard(
        id = "classic_wild_draw_four_c${copy + 1}",
        color = CardColor.WILD,
        type = CardType.WILD_DRAW_FOUR,
      )
    }

    ensureNoDuplicateIds(cards)
    return cards
  }

  override fun generateSpecialCardsOnly(config: SpecialCardsConfig): List<GameCard> {
    val cards = mutableListOf<GameCard>()

    repeat(config.thunderboltPlusThreeCount) { copy ->
      cards += GameCard.SpecialCard(
        id = "special_thunderbolt_c${copy + 1}",
        color = CardColor.WILD,
        type = CardType.THUNDERBOLT_PLUS_THREE,
      )
    }

    repeat(config.handSwapCount) { copy ->
      cards += GameCard.SpecialCard(
        id = "special_hand_swap_c${copy + 1}",
        color = CardColor.WILD,
        type = CardType.HAND_SWAP,
      )
    }

    for (color in CardColor.STANDARD_COLORS) {
      val colorKey = color.name.lowercase()
      repeat(config.bounceBackCountPerColor) { copy ->
        cards += GameCard.SpecialCard(
          id = "special_${colorKey}_bounce_back_c${copy + 1}",
          color = color,
          type = CardType.BOUNCE_BACK,
        )
      }
    }

    repeat(config.everyoneDrawOneCount) { copy ->
      cards += GameCard.SpecialCard(
        id = "special_everyone_draw_one_c${copy + 1}",
        color = CardColor.WILD,
        type = CardType.EVERYONE_DRAW_ONE,
      )
    }

    repeat(config.mysteryCardCount) { copy ->
      cards += GameCard.SpecialCard(
        id = "special_mystery_c${copy + 1}",
        color = CardColor.WILD,
        type = CardType.MYSTERY_CARD,
      )
    }

    ensureNoDuplicateIds(cards)
    return cards
  }

  override fun generateSpecialDeck(config: SpecialDeckConfig): List<GameCard> {
    val combined = generateClassicDeck(config.baseClassicConfig) +
      generateSpecialCardsOnly(config.specialCardsConfig)
    ensureNoDuplicateIds(combined)
    return combined
  }

  companion object {
    fun ensureNoDuplicateIds(cards: List<GameCard>) {
      val seen = HashSet<String>(cards.size)
      for (card in cards) {
        require(seen.add(card.id)) {
          "Duplicate card ID detected: ${card.id}"
        }
      }
    }
  }
}
