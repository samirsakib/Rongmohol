package com.example.domain.ai

import com.example.domain.game.CardColor
import com.example.domain.game.CardGameEngine
import com.example.domain.game.CardValidator
import com.example.domain.game.EngineStepResult
import com.example.domain.game.GameTableState
import com.example.domain.game.ValidationResult
import kotlin.random.Random

/**
 * Pure Kotlin coordinator for AI opponents across multiple seats and difficulties.
 * Guarantees AI only sees [PublicGameObservation] and strictly obeys [CardValidator].
 */
class AiDecisionEngine(
  private val delayConfig: AiDelayConfig = AiDelayConfig(),
  private val random: Random = Random.Default,
) {
  private val easyStrategy: AiStrategy = EasyAiStrategy(random)
  private val mediumStrategy: AiStrategy = MediumAiStrategy()
  private val hardStrategy: AiStrategy = HardAiStrategy()

  fun strategyFor(difficulty: AiDifficulty): AiStrategy = when (difficulty) {
    AiDifficulty.EASY -> easyStrategy
    AiDifficulty.MEDIUM -> mediumStrategy
    AiDifficulty.HARD -> hardStrategy
  }

  fun computeThinkingDelayMillis(difficulty: AiDifficulty): Long {
    val range = delayConfig.rangeFor(difficulty)
    if (range.first == range.last) return range.first
    return random.nextLong(range.first, range.last + 1)
  }

  /**
   * Computes a verified legal [AiDecision] from a purely public observation.
   */
  fun decideFromObservation(
    observation: PublicGameObservation,
    profile: AiOpponentProfile,
    difficulty: AiDifficulty,
  ): AiDecision {
    val strategy = strategyFor(difficulty)
    return strategy.chooseMove(observation, profile.personality)
  }

  /**
   * Resolves and executes one AI turn against [CardGameEngine], enforcing strict public-only
   * observation and fallback validation so AI can never play an illegal card.
   */
  fun resolveAndExecuteTurn(
    engine: CardGameEngine,
    state: GameTableState,
    aiPlayerId: String,
    profile: AiOpponentProfile,
    difficulty: AiDifficulty,
    ledger: PublicPlayLedger = PublicPlayLedger(),
  ): EngineStepResult {
    val observation = PublicObservationBuilder.build(
      state = state,
      aiPlayerId = aiPlayerId,
      ledger = ledger,
    )

    val proposed = decideFromObservation(
      observation = observation,
      profile = profile,
      difficulty = difficulty,
    )

    val validatedDecision = sanitizeDecision(state, aiPlayerId, proposed, observation)

    return when (validatedDecision) {
      is AiDecision.PlayCard -> engine.playCard(
        state = state,
        playerId = aiPlayerId,
        cardId = validatedDecision.card.id,
        chosenColor = validatedDecision.chosenColor,
        targetPlayerId = validatedDecision.targetPlayerId,
      )
      AiDecision.DrawCards -> engine.drawCards(
        state = state,
        playerId = aiPlayerId,
      )
    }
  }

  private fun sanitizeDecision(
    state: GameTableState,
    aiPlayerId: String,
    proposed: AiDecision,
    observation: PublicGameObservation,
  ): AiDecision {
    if (proposed is AiDecision.PlayCard) {
      val resolvedColor = if (proposed.chosenColor.isStandard) proposed.chosenColor else CardColor.RED
      val validation = CardValidator.validatePlay(
        state = state,
        playerId = aiPlayerId,
        card = proposed.card,
        chosenColor = resolvedColor,
        targetPlayerId = proposed.targetPlayerId,
      )
      if (validation is ValidationResult.Valid) {
        return proposed.copy(chosenColor = resolvedColor)
      }
    }

    // Safe legal fallback if anything prevented proposed move
    for (candidate in observation.legalPlays) {
      val fallbackColor = if (candidate.color.isStandard) candidate.color else CardColor.RED
      val check = CardValidator.validatePlay(
        state = state,
        playerId = aiPlayerId,
        card = candidate,
        chosenColor = fallbackColor,
      )
      if (check is ValidationResult.Valid) {
        return AiDecision.PlayCard(
          card = candidate,
          chosenColor = fallbackColor,
          reasoningTag = "validated_fallback",
        )
      }
    }

    return AiDecision.DrawCards
  }
}
