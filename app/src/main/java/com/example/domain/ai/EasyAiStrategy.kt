package com.example.domain.ai

import com.example.domain.game.CardColor
import com.example.domain.game.CardType
import kotlin.random.Random

/**
 * Easy AI: random valid card selection, basic random color choice,
 * and simple decision-making without strategic saving or threat tracking.
 */
class EasyAiStrategy(
  private val random: Random = Random.Default,
) : AiStrategy {

  override val difficulty: AiDifficulty = AiDifficulty.EASY

  override fun chooseMove(
    observation: PublicGameObservation,
    personality: AiPersonality,
  ): AiDecision {
    val legal = observation.legalPlays
    if (legal.isEmpty()) {
      return AiDecision.DrawCards
    }

    val chosenCard = legal[random.nextInt(legal.size)]
    val chosenColor = if (chosenCard.color == CardColor.WILD) {
      CardColor.STANDARD_COLORS[random.nextInt(CardColor.STANDARD_COLORS.size)]
    } else {
      chosenCard.color
    }

    val swapTarget = if (chosenCard.type == CardType.HAND_SWAP && observation.opponents.isNotEmpty()) {
      observation.opponents[random.nextInt(observation.opponents.size)].playerId
    } else {
      null
    }

    return AiDecision.PlayCard(
      card = chosenCard,
      chosenColor = chosenColor,
      targetPlayerId = swapTarget,
      reasoningTag = "easy_random_valid",
    )
  }
}
