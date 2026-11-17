package com.example.domain.game

/**
 * Pure domain representation of a playing card with invariant validation.
 */
sealed interface GameCard {
  val id: String
  val color: CardColor
  val type: CardType
  val pointValue: Int

  data class NumberCard(
    override val id: String,
    override val color: CardColor,
    val number: Int,
  ) : GameCard {
    override val type: CardType = CardType.NUMBER
    override val pointValue: Int = number

    init {
      require(id.isNotBlank()) { "Card id must not be blank" }
      require(color.isStandard) { "NumberCard must have a standard color, got $color" }
      require(number in 0..9) { "NumberCard number must be between 0 and 9, got $number" }
    }
  }

  data class ActionCard(
    override val id: String,
    override val color: CardColor,
    override val type: CardType,
  ) : GameCard {
    override val pointValue: Int = when (type) {
      CardType.SKIP, CardType.REVERSE, CardType.DRAW_TWO -> 20
      CardType.WILD, CardType.WILD_DRAW_FOUR -> 50
      else -> 20
    }

    init {
      require(id.isNotBlank()) { "Card id must not be blank" }
      require(type.isAction && !type.isSpecial) {
        "ActionCard requires standard action type, got $type"
      }
      if (type == CardType.WILD || type == CardType.WILD_DRAW_FOUR) {
        require(color == CardColor.WILD) { "$type must use CardColor.WILD" }
      } else {
        require(color.isStandard) { "$type must use a standard color, got $color" }
      }
    }
  }

  data class SpecialCard(
    override val id: String,
    override val color: CardColor,
    override val type: CardType,
  ) : GameCard {
    override val pointValue: Int = 40

    init {
      require(id.isNotBlank()) { "Card id must not be blank" }
      require(type.isSpecial) { "SpecialCard requires a special CardType, got $type" }
      if (type == CardType.BOUNCE_BACK) {
        require(color.isStandard || color == CardColor.WILD)
      } else {
        require(color == CardColor.WILD || color.isStandard)
      }
    }
  }
}
