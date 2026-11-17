package com.example.domain.ai

import com.example.domain.game.CardColor
import com.example.domain.game.GameCard

enum class AiDifficulty(val baseDelayRangeMs: LongRange) {
  EASY(450L..750L),
  MEDIUM(650L..1050L),
  HARD(850L..1350L),
}

enum class AiPersonality(
  val aggressiveBias: Double,
  val defensiveBias: Double,
  val colorControlBias: Double,
  val tricksterBias: Double,
) {
  IMPULSIVE_BLITZER(
    aggressiveBias = 1.25,
    defensiveBias = 0.9,
    colorControlBias = 1.0,
    tricksterBias = 1.1,
  ),
  CAUTIOUS_DEFENDER(
    aggressiveBias = 0.9,
    defensiveBias = 1.3,
    colorControlBias = 1.05,
    tricksterBias = 0.95,
  ),
  COLOR_WEAVER(
    aggressiveBias = 1.0,
    defensiveBias = 1.0,
    colorControlBias = 1.3,
    tricksterBias = 1.0,
  ),
  TRICKSTER_TACTICIAN(
    aggressiveBias = 1.1,
    defensiveBias = 1.0,
    colorControlBias = 1.0,
    tricksterBias = 1.3,
  ),
  CALM_ANALYST(
    aggressiveBias = 1.05,
    defensiveBias = 1.15,
    colorControlBias = 1.15,
    tricksterBias = 1.05,
  ),
}

data class AiOpponentProfile(
  val id: String,
  val bengaliName: String,
  val personaTitleBengali: String,
  val personality: AiPersonality,
  val preferredAvatarId: Int,
)

object AiRoster {
  val BOLTU = AiOpponentProfile(
    id = "ai_boltu",
    bengaliName = "বল্টু",
    personaTitleBengali = "অস্থির আক্রমণকারী",
    personality = AiPersonality.IMPULSIVE_BLITZER,
    preferredAvatarId = 2,
  )
  val MOFIZ = AiOpponentProfile(
    id = "ai_mofiz",
    bengaliName = "মফিজ",
    personaTitleBengali = "সতর্ক প্রহরী",
    personality = AiPersonality.CAUTIOUS_DEFENDER,
    preferredAvatarId = 0,
  )
  val TUNTUNI = AiOpponentProfile(
    id = "ai_tuntuni",
    bengaliName = "টুনটুনি",
    personaTitleBengali = "রঙের জাদুকর",
    personality = AiPersonality.COLOR_WEAVER,
    preferredAvatarId = 3,
  )
  val JHONTU = AiOpponentProfile(
    id = "ai_jhontu",
    bengaliName = "ঝন্টু",
    personaTitleBengali = "চতুর কৌশলী",
    personality = AiPersonality.TRICKSTER_TACTICIAN,
    preferredAvatarId = 4,
  )
  val POLASH = AiOpponentProfile(
    id = "ai_polash",
    bengaliName = "পলাশ",
    personaTitleBengali = "শান্ত বিশ্লেষক",
    personality = AiPersonality.CALM_ANALYST,
    preferredAvatarId = 1,
  )

  val ALL: List<AiOpponentProfile> = listOf(BOLTU, MOFIZ, TUNTUNI, JHONTU, POLASH)

  fun selectOpponents(count: Int, seedOffset: Int = 0): List<AiOpponentProfile> {
    val safeCount = count.coerceIn(1, ALL.size)
    return (0 until safeCount).map { idx ->
      ALL[(idx + seedOffset) % ALL.size]
    }
  }

  fun findById(id: String): AiOpponentProfile? = ALL.firstOrNull { it.id == id }
}

/**
 * Configurable thinking delay settings so UI tempo and fast tests stay independent.
 */
data class AiDelayConfig(
  val easyRangeMs: LongRange = AiDifficulty.EASY.baseDelayRangeMs,
  val mediumRangeMs: LongRange = AiDifficulty.MEDIUM.baseDelayRangeMs,
  val hardRangeMs: LongRange = AiDifficulty.HARD.baseDelayRangeMs,
) {
  init {
    require(easyRangeMs.first >= 0L && easyRangeMs.last >= easyRangeMs.first)
    require(mediumRangeMs.first >= 0L && mediumRangeMs.last >= mediumRangeMs.first)
    require(hardRangeMs.first >= 0L && hardRangeMs.last >= hardRangeMs.first)
  }

  fun rangeFor(difficulty: AiDifficulty): LongRange = when (difficulty) {
    AiDifficulty.EASY -> easyRangeMs
    AiDifficulty.MEDIUM -> mediumRangeMs
    AiDifficulty.HARD -> hardRangeMs
  }

  companion object {
    val ZERO_DELAY = AiDelayConfig(0L..0L, 0L..0L, 0L..0L)
  }
}

data class AiPlayerConfig(
  val playerId: String,
  val profile: AiOpponentProfile,
  val difficulty: AiDifficulty,
)

/**
 * Publicly observable information about an opponent.
 * Intentionally omits hidden cards so AI can never cheat.
 */
data class PublicOpponentInfo(
  val playerId: String,
  val displayName: String,
  val remainingCardCount: Int,
  val seatOrderFromAi: Int,
  val isImmediateNextInTurn: Boolean,
  val isImmediatePreviousInTurn: Boolean,
  val publiclyWeakColors: Set<CardColor> = emptySet(),
  val publiclyPlayedColors: Map<CardColor, Int> = emptyMap(),
)

/**
 * Strictly public table view + AI's own hand.
 * Contains zero references to hidden opponent hands or hidden draw pile cards.
 */
data class PublicGameObservation(
  val aiPlayerId: String,
  val aiHand: List<GameCard>,
  val legalPlays: List<GameCard>,
  val topDiscardCard: GameCard,
  val activeColor: CardColor,
  val discardPileHistory: List<GameCard>,
  val drawPileRemainingCount: Int,
  val isClockwise: Boolean,
  val pendingDrawPenalty: Int,
  val opponents: List<PublicOpponentInfo>,
)

sealed interface AiDecision {
  data class PlayCard(
    val card: GameCard,
    val chosenColor: CardColor,
    val targetPlayerId: String? = null,
    val reasoningTag: String = "",
  ) : AiDecision

  data object DrawCards : AiDecision
}

interface AiStrategy {
  val difficulty: AiDifficulty

  fun chooseMove(
    observation: PublicGameObservation,
    personality: AiPersonality,
  ): AiDecision
}
