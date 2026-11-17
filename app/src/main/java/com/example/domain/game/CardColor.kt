package com.example.domain.game

/**
 * Standard four colors plus [WILD] for uncolored wild/special cards.
 * Pure domain enum with zero Android or UI framework dependencies.
 */
enum class CardColor(val isStandard: Boolean) {
  RED(isStandard = true),
  BLUE(isStandard = true),
  GREEN(isStandard = true),
  YELLOW(isStandard = true),
  WILD(isStandard = false);

  companion object {
    val STANDARD_COLORS: List<CardColor> = listOf(RED, BLUE, GREEN, YELLOW)
  }
}
