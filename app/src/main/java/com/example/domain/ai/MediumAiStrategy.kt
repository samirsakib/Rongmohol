package com.example.domain.ai

import com.example.domain.game.CardColor
import com.example.domain.game.CardType
import com.example.domain.game.GameCard

/**
 * Medium AI: prefers colors abundant in its hand, conserves powerful wild/special cards
 * until needed, reacts to opponents' remaining card counts, and uses special cards tactically.
 */
class MediumAiStrategy : AiStrategy {

  override val difficulty: AiDifficulty = AiDifficulty.MEDIUM

  override fun chooseMove(
    observation: PublicGameObservation,
    personality: AiPersonality,
  ): AiDecision {
    val legal = observation.legalPlays
    if (legal.isEmpty()) {
      return AiDecision.DrawCards
    }

    // Immediate win if playing a card leaves 0 cards
    val winningCard = legal.firstOrNull { observation.aiHand.size == 1 }
    if (winningCard != null) {
      val winColor = if (winningCard.color.isStandard) winningCard.color else pickDominantHandColor(observation.aiHand, winningCard)
      return AiDecision.PlayCard(
        card = winningCard,
        chosenColor = winColor,
        reasoningTag = "medium_winning_play",
      )
    }

    val nextOpponent = observation.opponents.firstOrNull { it.isImmediateNextInTurn }
    val lowestOpponent = observation.opponents.minByOrNull { it.remainingCardCount }
    val opponentPressing = (nextOpponent?.remainingCardCount ?: Int.MAX_VALUE) <= 2 ||
      (lowestOpponent?.remainingCardCount ?: Int.MAX_VALUE) <= 2

    val remainingAfterPlayCount = (observation.aiHand.size - 1).coerceAtLeast(0)

    var bestScore = Double.NEGATIVE_INFINITY
    var bestDecision: AiDecision.PlayCard? = null

    for (card in legal) {
      val candidateColor = if (card.color == CardColor.WILD) {
        pickDominantHandColor(observation.aiHand, excludingCard = card)
      } else {
        card.color
      }

      val remainingInColor = observation.aiHand.count { it.id != card.id && it.color == candidateColor }
      var score = remainingInColor * 14.0

      // Prefer clearing higher value number cards while saving powerful cards when safe
      when (card) {
        is GameCard.NumberCard -> {
          score += 20.0 + card.number * 0.8
          if (card.color == observation.activeColor) {
            score += 4.0
          }
        }

        is GameCard.ActionCard -> {
          score += when (card.type) {
            CardType.DRAW_TWO -> if (opponentPressing) 48.0 * personality.aggressiveBias else 12.0
            CardType.SKIP -> if ((nextOpponent?.remainingCardCount ?: 5) <= 2) 44.0 else 10.0
            CardType.REVERSE -> if ((nextOpponent?.remainingCardCount ?: 5) <= 2 && observation.opponents.size >= 2) 32.0 else 8.0
            CardType.WILD -> if (opponentPressing) 18.0 else -18.0 // Save wilds when non-wild options exist
            CardType.WILD_DRAW_FOUR -> if (opponentPressing) 54.0 * personality.aggressiveBias else -28.0
            else -> 0.0
          }
        }

        is GameCard.SpecialCard -> {
          score += when (card.type) {
            CardType.THUNDERBOLT_PLUS_THREE -> {
              if (opponentPressing) 52.0 * personality.aggressiveBias else -22.0
            }
            CardType.BOUNCE_BACK -> {
              if (observation.pendingDrawPenalty > 0) {
                90.0 * personality.defensiveBias
              } else {
                -24.0
              }
            }
            CardType.HAND_SWAP -> {
              val oppCards = lowestOpponent?.remainingCardCount ?: Int.MAX_VALUE
              if (oppCards < remainingAfterPlayCount) {
                (remainingAfterPlayCount - oppCards) * 24.0 * personality.tricksterBias
              } else {
                -35.0 // Never hand away a smaller hand casually
              }
            }
            CardType.EVERYONE_DRAW_ONE -> {
              if (opponentPressing || observation.aiHand.size <= 3) 36.0 else -8.0
            }
            CardType.MYSTERY_CARD -> 14.0 * personality.tricksterBias
            else -> 0.0
          }
        }
      }

      // Bonus for switching to a color strongly held in AI's hand
      score += remainingInColor * 3.0 * personality.colorControlBias

      val swapTarget = if (card.type == CardType.HAND_SWAP) {
        lowestOpponent?.playerId
      } else {
        null
      }

      if (score > bestScore) {
        bestScore = score
        bestDecision = AiDecision.PlayCard(
          card = card,
          chosenColor = candidateColor,
          targetPlayerId = swapTarget,
          reasoningTag = "medium_color_and_saving",
        )
      }
    }

    return bestDecision ?: AiDecision.DrawCards
  }

  private fun pickDominantHandColor(
    hand: List<GameCard>,
    excludingCard: GameCard,
  ): CardColor {
    val counts = CardColor.STANDARD_COLORS.associateWith { color ->
      hand.count { it.id != excludingCard.id && it.color == color }
    }
    return counts.maxByOrNull { it.value }?.key ?: CardColor.RED
  }
}
