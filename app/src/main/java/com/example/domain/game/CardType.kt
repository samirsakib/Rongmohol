package com.example.domain.game

/**
 * Separates Classic and Special game decks cleanly.
 */
enum class DeckType {
  CLASSIC,
  SPECIAL,
}

/**
 * Complete card type taxonomy covering number cards (0-9),
 * standard action cards, and original Bengali special cards.
 */
enum class CardType(
  val isAction: Boolean,
  val isSpecial: Boolean,
  val requiresColorChoice: Boolean,
  val defaultPenaltyDraw: Int,
) {
  NUMBER(
    isAction = false,
    isSpecial = false,
    requiresColorChoice = false,
    defaultPenaltyDraw = 0,
  ),
  SKIP(
    isAction = true,
    isSpecial = false,
    requiresColorChoice = false,
    defaultPenaltyDraw = 0,
  ),
  REVERSE(
    isAction = true,
    isSpecial = false,
    requiresColorChoice = false,
    defaultPenaltyDraw = 0,
  ),
  DRAW_TWO(
    isAction = true,
    isSpecial = false,
    requiresColorChoice = false,
    defaultPenaltyDraw = 2,
  ),
  WILD(
    isAction = true,
    isSpecial = false,
    requiresColorChoice = true,
    defaultPenaltyDraw = 0,
  ),
  WILD_DRAW_FOUR(
    isAction = true,
    isSpecial = false,
    requiresColorChoice = true,
    defaultPenaltyDraw = 4,
  ),

  // Original Bengali Special Cards
  THUNDERBOLT_PLUS_THREE(
    isAction = true,
    isSpecial = true,
    requiresColorChoice = true,
    defaultPenaltyDraw = 3,
  ),
  HAND_SWAP(
    isAction = true,
    isSpecial = true,
    requiresColorChoice = true,
    defaultPenaltyDraw = 0,
  ),
  BOUNCE_BACK(
    isAction = true,
    isSpecial = true,
    requiresColorChoice = false,
    defaultPenaltyDraw = 0,
  ),
  EVERYONE_DRAW_ONE(
    isAction = true,
    isSpecial = true,
    requiresColorChoice = true,
    defaultPenaltyDraw = 1,
  ),
  MYSTERY_CARD(
    isAction = true,
    isSpecial = true,
    requiresColorChoice = true,
    defaultPenaltyDraw = 0,
  ),
  ;

  val isClassicType: Boolean
    get() = !isSpecial

  val isWildColoredType: Boolean
    get() = this == WILD ||
      this == WILD_DRAW_FOUR ||
      this == THUNDERBOLT_PLUS_THREE ||
      this == HAND_SWAP ||
      this == EVERYONE_DRAW_ONE ||
      this == MYSTERY_CARD
}

enum class MysteryOutcome {
  NEXT_PLAYER_DRAWS_TWO,
  GIFT_RANDOM_CARD_TO_NEXT,
  SHIELD_AND_SKIP_NEXT,
  REVERSE_AND_ROTATE_COLOR,
}
