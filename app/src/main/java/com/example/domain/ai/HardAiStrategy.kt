package com.example.domain.ai

import com.example.domain.game.CardColor
import com.example.domain.game.CardType
import com.example.domain.game.CardValidator
import com.example.domain.game.GameCard

/**
 * Hard AI: evaluates future hand mobility, predicts opponent constraints from public discard/draw
 * history, chooses strategic attacks, protects itself with defensive cards, and triggers
 * urgent counterplay whenever an opponent reaches 1 card remaining.
 */
class HardAiStrategy : AiStrategy {

  override val difficulty: AiDifficulty = AiDifficulty.HARD

  override fun chooseMove(
    observation: PublicGameObservation,
    personality: AiPersonality,
  ): AiDecision {
    val legal = observation.legalPlays
    if (legal.isEmpty()) {
      return AiDecision.DrawCards
    }

    // Immediate guaranteed victory check
    if (observation.aiHand.size == 1) {
      val winCard = legal.first()
      val winColor = if (winCard.color.isStandard) winCard.color else pickOptimalWildColor(observation, winCard, personality)
      return AiDecision.PlayCard(
        card = winCard,
        chosenColor = winColor,
        reasoningTag = "hard_guaranteed_win",
      )
    }

    val nextOpp = observation.opponents.firstOrNull { it.isImmediateNextInTurn }
    val prevOpp = observation.opponents.firstOrNull { it.isImmediatePreviousInTurn }
    val lowestOpp = observation.opponents.minByOrNull { it.remainingCardCount }
    val oneCardThreatOpp = observation.opponents.firstOrNull { it.remainingCardCount == 1 }
    val nextOppHasOneCard = nextOpp?.remainingCardCount == 1
    val nextOppLow = (nextOpp?.remainingCardCount ?: 99) <= 2
    val remainingCountAfterPlay = observation.aiHand.size - 1

    var bestScore = Double.NEGATIVE_INFINITY
    var bestMove: AiDecision.PlayCard? = null

    for (card in legal) {
      val candidateColors = if (card.color == CardColor.WILD) {
        CardColor.STANDARD_COLORS
      } else {
        listOf(card.color)
      }

      for (chosenColor in candidateColors) {
        val score = scoreCandidateMove(
          observation = observation,
          card = card,
          chosenColor = chosenColor,
          nextOpp = nextOpp,
          prevOpp = prevOpp,
          lowestOpp = lowestOpp,
          oneCardThreatOpp = oneCardThreatOpp,
          nextOppHasOneCard = nextOppHasOneCard,
          nextOppLow = nextOppLow,
          remainingCountAfterPlay = remainingCountAfterPlay,
          personality = personality,
        )

        val targetId = if (card.type == CardType.HAND_SWAP) {
          (oneCardThreatOpp ?: lowestOpp)?.playerId
        } else {
          null
        }

        if (score > bestScore) {
          bestScore = score
          bestMove = AiDecision.PlayCard(
            card = card,
            chosenColor = chosenColor,
            targetPlayerId = targetId,
            reasoningTag = if (oneCardThreatOpp != null) {
              "hard_one_card_threat_response"
            } else if (observation.pendingDrawPenalty > 0) {
              "hard_defensive_counter"
            } else {
              "hard_lookahead_and_prediction"
            },
          )
        }
      }
    }

    return bestMove ?: AiDecision.DrawCards
  }

  private fun scoreCandidateMove(
    observation: PublicGameObservation,
    card: GameCard,
    chosenColor: CardColor,
    nextOpp: PublicOpponentInfo?,
    prevOpp: PublicOpponentInfo?,
    lowestOpp: PublicOpponentInfo?,
    oneCardThreatOpp: PublicOpponentInfo?,
    nextOppHasOneCard: Boolean,
    nextOppLow: Boolean,
    remainingCountAfterPlay: Int,
    personality: AiPersonality,
  ): Double {
    var score = 0.0
    val remainingHand = observation.aiHand.filterNot { it.id == card.id }

    // 1. Evaluate future moves: how well the remaining hand chains next turn
    score += evaluateFutureHandSynergy(
      remainingHand = remainingHand,
      resultingTopCard = card,
      resultingColor = chosenColor,
    ) * personality.colorControlBias

    // 2. Public info opponent prediction: target next opponent's publicly weak colors
    if (nextOpp != null) {
      if (chosenColor in nextOpp.publiclyWeakColors) {
        score += 24.0
      }
      val playedCountInColor = nextOpp.publiclyPlayedColors[chosenColor] ?: 0
      score -= playedCountInColor * 3.5

      // Public scarcity: colors seen often in discard/self-hand are scarcer for opponents
      val visibleColorOccurrences = observation.discardPileHistory.count { it.color == chosenColor } +
        observation.aiHand.count { it.color == chosenColor }
      score += visibleColorOccurrences * 1.4
    }

    // 3. Defensive self-protection when facing a penalty
    if (observation.pendingDrawPenalty > 0) {
      score += when (card.type) {
        CardType.BOUNCE_BACK -> 140.0 * personality.defensiveBias
        CardType.WILD_DRAW_FOUR, CardType.THUNDERBOLT_PLUS_THREE, CardType.DRAW_TWO -> 80.0
        else -> 0.0
      }
      return score
    }

    // 4. Urgent 1-card threat handling + strategic attack selection
    if (oneCardThreatOpp != null) {
      when (card.type) {
        CardType.HAND_SWAP -> {
          if (remainingCountAfterPlay >= 2) {
            // Steal the 1-card leader's hand immediately
            score += 220.0
          }
        }
        CardType.THUNDERBOLT_PLUS_THREE -> {
          score += if (nextOppHasOneCard) 175.0 else 95.0
        }
        CardType.WILD_DRAW_FOUR -> {
          score += if (nextOppHasOneCard) 170.0 else 90.0
        }
        CardType.DRAW_TWO -> {
          score += if (nextOppHasOneCard) 160.0 else 75.0
        }
        CardType.EVERYONE_DRAW_ONE -> {
          // Forces the 1-card opponent anywhere at the table to draw back up to 2 cards!
          score += 185.0
        }
        CardType.SKIP -> {
          score += if (nextOppHasOneCard) 145.0 else 30.0
        }
        CardType.REVERSE -> {
          if (observation.opponents.size >= 2) {
            if (nextOppHasOneCard && (prevOpp?.remainingCardCount ?: 5) > 1) {
              score += 130.0 // Divert turn away from the 1-card player
            } else if (!nextOppHasOneCard && prevOpp?.remainingCardCount == 1) {
              score -= 110.0 // Do NOT hand the turn back to a 1-card player!
            }
          }
        }
        else -> {
          // Leaving a 1-card next player an easy number match in a color they previously played is dangerous
          if (nextOppHasOneCard && nextOpp != null && chosenColor in nextOpp.publiclyPlayedColors) {
            score -= 22.0
          }
        }
      }
    } else if (nextOppLow) {
      // Opponent has 2 cards: apply measured pressure
      when (card.type) {
        CardType.DRAW_TWO -> score += 60.0 * personality.aggressiveBias
        CardType.SKIP -> score += 52.0 * personality.aggressiveBias
        CardType.THUNDERBOLT_PLUS_THREE -> score += 62.0 * personality.aggressiveBias
        CardType.WILD_DRAW_FOUR -> score += 58.0 * personality.aggressiveBias
        CardType.EVERYONE_DRAW_ONE -> score += 48.0 * personality.aggressiveBias
        CardType.HAND_SWAP -> {
          val oppCount = lowestOpp?.remainingCardCount ?: Int.MAX_VALUE
          score += if (oppCount + 1 < remainingCountAfterPlay) 85.0 else -35.0
        }
        else -> Unit
      }
    } else {
      // Safe phase: conserve defensive & power cards for emergencies or checkmates
      when (card.type) {
        CardType.BOUNCE_BACK -> score -= 45.0 * personality.defensiveBias
        CardType.WILD_DRAW_FOUR -> score -= 38.0
        CardType.THUNDERBOLT_PLUS_THREE -> score -= 30.0
        CardType.WILD -> score -= 24.0
        CardType.HAND_SWAP -> {
          val oppCount = lowestOpp?.remainingCardCount ?: Int.MAX_VALUE
          score += if (oppCount + 2 <= remainingCountAfterPlay) 55.0 else -60.0
        }
        CardType.EVERYONE_DRAW_ONE -> score -= if (observation.aiHand.size > 4) 15.0 else -12.0
        CardType.MYSTERY_CARD -> score += 6.0 * personality.tricksterBias
        CardType.NUMBER -> score += 22.0 + card.pointValue * 0.6
        else -> score += 12.0
      }
    }

    // Two-card endgame: set up guaranteed last card win
    if (remainingHand.size == 1) {
      val lastCard = remainingHand.first()
      if (lastCard.color == CardColor.WILD || lastCard.color == chosenColor) {
        score += 95.0
      }
    }

    return score
  }

  private fun evaluateFutureHandSynergy(
    remainingHand: List<GameCard>,
    resultingTopCard: GameCard,
    resultingColor: CardColor,
  ): Double {
    if (remainingHand.isEmpty()) return 300.0

    val playableNextCount = remainingHand.count { nextCard ->
      CardValidator.isCardPlayableOn(
        card = nextCard,
        topCard = resultingTopCard,
        activeColor = resultingColor,
        pendingDrawPenalty = 0,
      )
    }

    val colorGroups = remainingHand.filter { it.color.isStandard }.groupBy { it.color }
    val dominantColorSize = colorGroups.maxOfOrNull { it.value.size } ?: 0

    // Number bridges let AI pivot between colors safely
    val numberBridgeCount = remainingHand
      .filterIsInstance<GameCard.NumberCard>()
      .groupBy { it.number }
      .count { it.value.size >= 2 }

    val matchingNewColorCount = remainingHand.count { it.color == resultingColor }

    return (playableNextCount * 11.0) +
      (matchingNewColorCount * 9.0) +
      (dominantColorSize * 4.5) +
      (numberBridgeCount * 6.0)
  }

  private fun pickOptimalWildColor(
    observation: PublicGameObservation,
    excludingCard: GameCard,
    personality: AiPersonality,
  ): CardColor {
    val nextOpp = observation.opponents.firstOrNull { it.isImmediateNextInTurn }
    return CardColor.STANDARD_COLORS.maxByOrNull { color ->
      val handCount = observation.aiHand.count { it.id != excludingCard.id && it.color == color }
      val weakBonus = if (nextOpp != null && color in nextOpp.publiclyWeakColors) 2.5 else 0.0
      (handCount * personality.colorControlBias) + weakBonus
    } ?: CardColor.RED
  }
}
