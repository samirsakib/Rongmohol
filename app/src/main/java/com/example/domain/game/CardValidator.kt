package com.example.domain.game

enum class ValidationFailureReason {
  GAME_ALREADY_FINISHED,
  NOT_PLAYER_TURN,
  CARD_NOT_IN_HAND,
  SPECIAL_CARD_NOT_ALLOWED_IN_CLASSIC_DECK,
  ILLEGAL_COLOR_CHOICE,
  INVALID_SWAP_TARGET,
  PENALTY_REQUIRES_DRAW_OR_BOUNCE,
  CARD_DOES_NOT_MATCH,
  EMPTY_DRAW_PILE_NO_RECYCLABLE_CARDS,
  DUPLICATE_CARD_ID_DETECTED,
  CONSERVATION_VIOLATION,
}

sealed interface ValidationResult {
  data object Valid : ValidationResult
  data class Invalid(
    val reason: ValidationFailureReason,
    val message: String,
  ) : ValidationResult

  val isValid: Boolean
    get() = this is Valid
}

/**
 * Validates all legal plays, draws, card movements, and whole-table ID conservation.
 */
object CardValidator {

  fun isCardPlayableOn(
    card: GameCard,
    topCard: GameCard,
    activeColor: CardColor,
    pendingDrawPenalty: Int = 0,
  ): Boolean {
    if (pendingDrawPenalty > 0) {
      if (card.type == CardType.BOUNCE_BACK) return true
      val isMatchingStackPenalty = when (topCard.type) {
        CardType.DRAW_TWO -> card.type == CardType.DRAW_TWO
        CardType.WILD_DRAW_FOUR -> card.type == CardType.WILD_DRAW_FOUR
        CardType.THUNDERBOLT_PLUS_THREE -> card.type == CardType.THUNDERBOLT_PLUS_THREE
        else -> false
      }
      return isMatchingStackPenalty
    }

    if (card.color == CardColor.WILD) return true
    if (card.color == activeColor) return true

    return when {
      card is GameCard.NumberCard && topCard is GameCard.NumberCard ->
        card.number == topCard.number
      card.type.isAction && card.type == topCard.type ->
        true
      else -> false
    }
  }

  fun validatePlay(
    state: GameTableState,
    playerId: String,
    card: GameCard,
    chosenColor: CardColor? = null,
    targetPlayerId: String? = null,
  ): ValidationResult {
    if (state.isMatchFinished) {
      return ValidationResult.Invalid(
        reason = ValidationFailureReason.GAME_ALREADY_FINISHED,
        message = "খেলা ইতিমধ্যেই শেষ হয়েছে",
      )
    }

    if (state.currentPlayer.playerId != playerId) {
      return ValidationResult.Invalid(
        reason = ValidationFailureReason.NOT_PLAYER_TURN,
        message = "এখন আপনার চাল নয়",
      )
    }

    val player = state.playerById(playerId)
    if (player == null || player.hand.none { it.id == card.id && it == card }) {
      return ValidationResult.Invalid(
        reason = ValidationFailureReason.CARD_NOT_IN_HAND,
        message = "এই তাসটি খেলোয়াড়ের হাতে নেই",
      )
    }

    if (state.deckType == DeckType.CLASSIC && card.type.isSpecial) {
      return ValidationResult.Invalid(
        reason = ValidationFailureReason.SPECIAL_CARD_NOT_ALLOWED_IN_CLASSIC_DECK,
        message = "ধ্রুপদী ডেকে বিশেষ কার্ড খেলা অনুমোদিত নয়",
      )
    }

    if (card.color == CardColor.WILD) {
      if (chosenColor == null || !chosenColor.isStandard) {
        return ValidationResult.Invalid(
          reason = ValidationFailureReason.ILLEGAL_COLOR_CHOICE,
          message = "ওয়াইল্ড বা বিশেষ তাসের জন্য একটি বৈধ রঙ নির্বাচন করুন",
        )
      }
    } else if (chosenColor != null && chosenColor != card.color) {
      return ValidationResult.Invalid(
        reason = ValidationFailureReason.ILLEGAL_COLOR_CHOICE,
        message = "নির্বাচিত রঙ তাসের মূল রঙের সাথে মিলছে না",
      )
    }

    if (card.type == CardType.HAND_SWAP && targetPlayerId != null) {
      val validTarget = targetPlayerId != playerId && state.playerById(targetPlayerId) != null
      if (!validTarget) {
        return ValidationResult.Invalid(
          reason = ValidationFailureReason.INVALID_SWAP_TARGET,
          message = "হাতবদলের জন্য বৈধ প্রতিপক্ষ পাওয়া যায়নি",
        )
      }
    }

    if (state.pendingDrawPenalty > 0) {
      val allowedDuringPenalty = isCardPlayableOn(
        card = card,
        topCard = state.topDiscardCard,
        activeColor = state.activeColor,
        pendingDrawPenalty = state.pendingDrawPenalty,
      )
      if (!allowedDuringPenalty) {
        return ValidationResult.Invalid(
          reason = ValidationFailureReason.PENALTY_REQUIRES_DRAW_OR_BOUNCE,
          message = "পেনাল্টি চলাকালীন তাস টানুন অথবা বাউন্স ব্যাক / সমজাতীয় পেনাল্টি কার্ড খেলুন",
        )
      }
      return ValidationResult.Valid
    }

    val matches = isCardPlayableOn(
      card = card,
      topCard = state.topDiscardCard,
      activeColor = state.activeColor,
      pendingDrawPenalty = 0,
    )
    if (!matches) {
      return ValidationResult.Invalid(
        reason = ValidationFailureReason.CARD_DOES_NOT_MATCH,
        message = "সক্রিয় রঙ, সংখ্যা বা প্রতীকের সাথে তাসটি মিলছে না",
      )
    }

    return ValidationResult.Valid
  }

  fun validateDraw(
    state: GameTableState,
    playerId: String,
  ): ValidationResult {
    if (state.isMatchFinished) {
      return ValidationResult.Invalid(
        reason = ValidationFailureReason.GAME_ALREADY_FINISHED,
        message = "খেলা ইতিমধ্যেই শেষ হয়েছে",
      )
    }

    if (state.currentPlayer.playerId != playerId) {
      return ValidationResult.Invalid(
        reason = ValidationFailureReason.NOT_PLAYER_TURN,
        message = "এখন আপনার চাল নয়",
      )
    }

    if (state.drawPile.isEmpty && !state.discardPile.canRecycle()) {
      return ValidationResult.Invalid(
        reason = ValidationFailureReason.EMPTY_DRAW_PILE_NO_RECYCLABLE_CARDS,
        message = "ড্র পাইল ও ডিসকার্ড পাইল উভয়ই খালি",
      )
    }

    return ValidationResult.Valid
  }

  /**
   * Validates full card-movement integrity across hands, draw pile, and discard pile.
   */
  fun validateStateIntegrity(state: GameTableState): ValidationResult {
    val allCards = buildList {
      state.players.forEach { addAll(it.hand) }
      addAll(state.drawPile.cards)
      addAll(state.discardPile.cards)
    }

    val seenIds = HashSet<String>(allCards.size)
    for (card in allCards) {
      if (!seenIds.add(card.id)) {
        return ValidationResult.Invalid(
          reason = ValidationFailureReason.DUPLICATE_CARD_ID_DETECTED,
          message = "ডুপ্লিকেট কার্ড আইডি পাওয়া গেছে: ${card.id}",
        )
      }
    }

    if (seenIds != state.allKnownCardIds) {
      return ValidationResult.Invalid(
        reason = ValidationFailureReason.CONSERVATION_VIOLATION,
        message = "টেবিলের মোট তাসের সংখ্যা ও আইডি অক্ষুণ্ণ নেই",
      )
    }

    return ValidationResult.Valid
  }
}
