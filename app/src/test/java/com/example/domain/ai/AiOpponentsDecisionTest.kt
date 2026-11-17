package com.example.domain.ai

import com.example.domain.game.CardColor
import com.example.domain.game.CardGameEngine
import com.example.domain.game.CardType
import com.example.domain.game.CardValidator
import com.example.domain.game.DeckType
import com.example.domain.game.DiscardPile
import com.example.domain.game.DrawPile
import com.example.domain.game.EngineStepResult
import com.example.domain.game.FisherYatesShuffleAlgorithm
import com.example.domain.game.GameCard
import com.example.domain.game.GameEvent
import com.example.domain.game.GameTableState
import com.example.domain.game.PlayerSeatState
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiOpponentsDecisionTest {

  private fun buildTable(
    aiHand: List<GameCard>,
    opp1Hand: List<GameCard>,
    opp2Hand: List<GameCard> = emptyList(),
    topCard: GameCard = GameCard.NumberCard("top_blue_5", CardColor.BLUE, 5),
    activeColor: CardColor = CardColor.BLUE,
    pendingPenalty: Int = 0,
    lastPenaltySenderId: String? = null,
    isClockwise: Boolean = true,
  ): GameTableState {
    val players = buildList {
      add(PlayerSeatState("ai_0", AiRoster.POLASH.bengaliName, aiHand))
      add(PlayerSeatState("opp_1", AiRoster.BOLTU.bengaliName, opp1Hand))
      if (opp2Hand.isNotEmpty()) {
        add(PlayerSeatState("opp_2", AiRoster.TUNTUNI.bengaliName, opp2Hand))
      }
    }
    val drawCards = listOf(
      GameCard.NumberCard("draw_1", CardColor.RED, 1),
      GameCard.NumberCard("draw_2", CardColor.GREEN, 2),
      GameCard.NumberCard("draw_3", CardColor.YELLOW, 3),
    )
    val allIds = (players.flatMap { it.hand } + drawCards + topCard).map { it.id }.toSet()
    return GameTableState(
      deckType = DeckType.SPECIAL,
      players = players,
      currentPlayerIndex = 0,
      isClockwise = isClockwise,
      drawPile = DrawPile(drawCards),
      discardPile = DiscardPile(listOf(topCard), activeColor),
      pendingDrawPenalty = pendingPenalty,
      lastPenaltySenderId = lastPenaltySenderId,
      allKnownCardIds = allIds,
    )
  }

  @Test
  fun `easy ai picks only legal cards and draws when no legal card exists`() {
    val engine = AiDecisionEngine(random = Random(42))
    val legalCard = GameCard.NumberCard("ai_red_5", CardColor.RED, 5)
    val illegalCard = GameCard.NumberCard("ai_yellow_9", CardColor.YELLOW, 9)
    val state = buildTable(
      aiHand = listOf(illegalCard, legalCard),
      opp1Hand = listOf(GameCard.NumberCard("o1", CardColor.GREEN, 4)),
      topCard = GameCard.NumberCard("top_blue_5", CardColor.BLUE, 5),
      activeColor = CardColor.BLUE,
    )

    val obs = PublicObservationBuilder.build(state, "ai_0")
    val decision = engine.decideFromObservation(obs, AiRoster.BOLTU, AiDifficulty.EASY)
    assertTrue(decision is AiDecision.PlayCard)
    assertEquals(legalCard.id, (decision as AiDecision.PlayCard).card.id)

    // No legal plays -> DrawCards
    val noPlayState = buildTable(
      aiHand = listOf(illegalCard),
      opp1Hand = listOf(GameCard.NumberCard("o1", CardColor.GREEN, 4)),
      topCard = GameCard.NumberCard("top_blue_5", CardColor.BLUE, 5),
      activeColor = CardColor.BLUE,
    )
    val noPlayObs = PublicObservationBuilder.build(noPlayState, "ai_0")
    val drawDecision = engine.decideFromObservation(noPlayObs, AiRoster.BOLTU, AiDifficulty.EASY)
    assertEquals(AiDecision.DrawCards, drawDecision)
  }

  @Test
  fun `medium ai prefers dominant hand color and saves powerful cards when opponents are safe`() {
    val engine = AiDecisionEngine()
    val redFive = GameCard.NumberCard("ai_red_5", CardColor.RED, 5)
    val greenFive = GameCard.NumberCard("ai_green_5", CardColor.GREEN, 5)
    val wildDrawFour = GameCard.ActionCard("ai_wd4", CardColor.WILD, CardType.WILD_DRAW_FOUR)
    val greenExtras = listOf(
      GameCard.NumberCard("ai_g1", CardColor.GREEN, 1),
      GameCard.NumberCard("ai_g2", CardColor.GREEN, 2),
      GameCard.NumberCard("ai_g3", CardColor.GREEN, 8),
    )

    // Opponent has 5 cards (safe) -> Medium saves WILD_DRAW_FOUR and picks greenFive (abundant GREEN)
    val state = buildTable(
      aiHand = listOf(redFive, greenFive, wildDrawFour) + greenExtras,
      opp1Hand = List(5) { GameCard.NumberCard("o_$it", CardColor.BLUE, it + 1) },
      topCard = GameCard.NumberCard("top_blue_5", CardColor.BLUE, 5),
      activeColor = CardColor.BLUE,
    )

    val obs = PublicObservationBuilder.build(state, "ai_0")
    val decision = engine.decideFromObservation(obs, AiRoster.TUNTUNI, AiDifficulty.MEDIUM)
    assertTrue(decision is AiDecision.PlayCard)
    val play = decision as AiDecision.PlayCard
    assertEquals("Medium AI should pick abundant green number card and save Wild Draw Four", greenFive.id, play.card.id)
  }

  @Test
  fun `hard ai uses public weakness ledger and counterplays when opponent has 1 card left`() {
    val engine = AiDecisionEngine()
    val aiSafeNum = GameCard.NumberCard("ai_blue_3", CardColor.BLUE, 3)
    val aiWild = GameCard.ActionCard("ai_wild", CardColor.WILD, CardType.WILD)
    val aiHandSwap = GameCard.SpecialCard("ai_swap", CardColor.WILD, CardType.HAND_SWAP)
    val aiExtra = GameCard.NumberCard("ai_red_7", CardColor.RED, 7)

    // Case A: 1-card opponent -> Hard AI steals their 1-card hand via HAND_SWAP!
    val threatState = buildTable(
      aiHand = listOf(aiSafeNum, aiWild, aiHandSwap, aiExtra),
      opp1Hand = listOf(GameCard.NumberCard("opp_last", CardColor.BLUE, 9)), // only 1 card!
      opp2Hand = listOf(
        GameCard.NumberCard("o2_1", CardColor.GREEN, 1),
        GameCard.NumberCard("o2_2", CardColor.GREEN, 4),
        GameCard.NumberCard("o2_3", CardColor.YELLOW, 6),
      ),
      topCard = GameCard.NumberCard("top_blue_5", CardColor.BLUE, 5),
      activeColor = CardColor.BLUE,
    )
    val threatObs = PublicObservationBuilder.build(threatState, "ai_0")
    val threatDecision = engine.decideFromObservation(threatObs, AiRoster.JHONTU, AiDifficulty.HARD) as AiDecision.PlayCard
    assertEquals(CardType.HAND_SWAP, threatDecision.card.type)
    assertEquals("opp_1", threatDecision.targetPlayerId)

    // Case B: PublicPlayLedger shows opp_1 had to draw on YELLOW (weakness) -> Wild picks YELLOW!
    var ledger = PublicPlayLedger()
    ledger = ledger.recordStateTransition(
      previousActiveColor = CardColor.YELLOW,
      event = GameEvent.CardsDrawn(
        playerId = "opp_1",
        drawnCards = listOf(GameCard.NumberCard("drawn_x", CardColor.RED, 1)),
        recycledFromDiscardCount = 0,
      ),
    )

    val wildOnlyState = buildTable(
      aiHand = listOf(aiWild, GameCard.NumberCard("ai_y1", CardColor.YELLOW, 2), GameCard.NumberCard("ai_r1", CardColor.RED, 2)),
      opp1Hand = List(4) { GameCard.NumberCard("opp_h_$it", CardColor.GREEN, it + 1) },
      topCard = GameCard.NumberCard("top_blue_5", CardColor.BLUE, 5),
      activeColor = CardColor.BLUE,
    )
    val ledgerObs = PublicObservationBuilder.build(wildOnlyState, "ai_0", ledger)
    val wildDecision = engine.decideFromObservation(ledgerObs, AiRoster.POLASH, AiDifficulty.HARD) as AiDecision.PlayCard
    assertEquals(CardColor.YELLOW, wildDecision.chosenColor)
  }

  @Test
  fun `hard ai defends itself with bounce back on penalty and avoids gifting reverse to 1-card previous player`() {
    val engine = AiDecisionEngine()
    val bounce = GameCard.SpecialCard("ai_bounce", CardColor.RED, CardType.BOUNCE_BACK)
    val drawTwo = GameCard.ActionCard("ai_d2", CardColor.BLUE, CardType.DRAW_TWO)

    // Facing pending penalty -> Bounce Back deflects it
    val penaltyState = buildTable(
      aiHand = listOf(bounce, drawTwo),
      opp1Hand = List(4) { GameCard.NumberCard("o1_$it", CardColor.GREEN, it + 1) },
      topCard = GameCard.ActionCard("top_d2", CardColor.BLUE, CardType.DRAW_TWO),
      activeColor = CardColor.BLUE,
      pendingPenalty = 2,
      lastPenaltySenderId = "opp_1",
    )
    val penaltyObs = PublicObservationBuilder.build(penaltyState, "ai_0")
    val penaltyDecision = engine.decideFromObservation(penaltyObs, AiRoster.MOFIZ, AiDifficulty.HARD) as AiDecision.PlayCard
    assertEquals(CardType.BOUNCE_BACK, penaltyDecision.card.type)

    // 3 players: previous player (opp_2) has 1 card left; next player (opp_1) has 5 cards.
    // Hard AI must NOT play REVERSE into opp_2 when a regular BLUE card is available!
    val reverseCard = GameCard.ActionCard("ai_rev", CardColor.BLUE, CardType.REVERSE)
    val safeBlue = GameCard.NumberCard("ai_blue_8", CardColor.BLUE, 8)
    val threePlayerState = buildTable(
      aiHand = listOf(reverseCard, safeBlue, GameCard.NumberCard("ai_g9", CardColor.GREEN, 9)),
      opp1Hand = List(5) { GameCard.NumberCard("o1_x_$it", CardColor.RED, it + 1) },
      opp2Hand = listOf(GameCard.NumberCard("o2_last", CardColor.YELLOW, 7)), // previous player has 1 card!
      topCard = GameCard.NumberCard("top_b5", CardColor.BLUE, 5),
      activeColor = CardColor.BLUE,
    )
    val revObs = PublicObservationBuilder.build(threePlayerState, "ai_0")
    val revDecision = engine.decideFromObservation(revObs, AiRoster.POLASH, AiDifficulty.HARD) as AiDecision.PlayCard
    assertNotEquals(CardType.REVERSE, revDecision.card.type)
    assertEquals(safeBlue.id, revDecision.card.id)
  }

  @Test
  fun `ai observation strictly hides opponent cards and decision is invariant to hidden opponent hands`() {
    val aiHand = listOf(
      GameCard.ActionCard("ai_wild", CardColor.WILD, CardType.WILD),
      GameCard.NumberCard("ai_r2", CardColor.RED, 2),
      GameCard.NumberCard("ai_b4", CardColor.BLUE, 4),
    )

    val hiddenHandVariantA = listOf(
      GameCard.NumberCard("hid_a1", CardColor.RED, 1),
      GameCard.NumberCard("hid_a2", CardColor.RED, 2),
      GameCard.NumberCard("hid_a3", CardColor.RED, 3),
    )
    val hiddenHandVariantB = listOf(
      GameCard.NumberCard("hid_b1", CardColor.YELLOW, 7),
      GameCard.NumberCard("hid_b2", CardColor.GREEN, 8),
      GameCard.NumberCard("hid_b3", CardColor.BLUE, 9),
    )

    val stateA = buildTable(aiHand = aiHand, opp1Hand = hiddenHandVariantA)
    val stateB = buildTable(aiHand = aiHand, opp1Hand = hiddenHandVariantB)

    val obsA = PublicObservationBuilder.build(stateA, "ai_0")
    val obsB = PublicObservationBuilder.build(stateB, "ai_0")

    assertEquals("Observation must be identical regardless of opponents' hidden cards", obsA, obsB)

    for (difficulty in AiDifficulty.entries) {
      val engineA = AiDecisionEngine(random = Random(11))
      val engineB = AiDecisionEngine(random = Random(11))
      val decA = engineA.decideFromObservation(obsA, AiRoster.MOFIZ, difficulty)
      val decB = engineB.decideFromObservation(obsB, AiRoster.MOFIZ, difficulty)
      assertEquals("AI decision ($difficulty) must never depend on hidden opponent cards", decA, decB)
    }
  }

  @Test
  fun `multi-ai 4-player match with distinct Bengali personalities executes legally and honors delay config`() {
    val gameEngine = CardGameEngine(
      shuffler = FisherYatesShuffleAlgorithm(Random(2025)),
      random = Random(2025),
    )
    val aiProfiles = AiRoster.selectOpponents(4) // বল্টু, মফিজ, টুনটুনি, ঝন্টু
    val difficulties = listOf(AiDifficulty.EASY, AiDifficulty.MEDIUM, AiDifficulty.HARD, AiDifficulty.HARD)
    val configs = aiProfiles.mapIndexed { idx, profile ->
      profile.id to AiPlayerConfig(profile.id, profile, difficulties[idx])
    }.toMap()

    var state = gameEngine.createMatch(
      deckType = DeckType.SPECIAL,
      players = aiProfiles.map { it.id to it.bengaliName },
      cardsPerPlayer = 7,
    )

    val customDelays = AiDelayConfig(
      easyRangeMs = 100L..200L,
      mediumRangeMs = 300L..400L,
      hardRangeMs = 500L..700L,
    )
    val aiEngine = AiDecisionEngine(delayConfig = customDelays, random = Random(9))

    assertTrue(aiEngine.computeThinkingDelayMillis(AiDifficulty.EASY) in 100L..200L)
    assertTrue(aiEngine.computeThinkingDelayMillis(AiDifficulty.MEDIUM) in 300L..400L)
    assertTrue(aiEngine.computeThinkingDelayMillis(AiDifficulty.HARD) in 500L..700L)

    var ledger = PublicPlayLedger()
    var turnsPlayed = 0
    while (!state.isMatchFinished && turnsPlayed < 40) {
      val activeSeat = state.currentPlayer
      val config = configs.getValue(activeSeat.playerId)
      val prevColor = state.activeColor

      val step = aiEngine.resolveAndExecuteTurn(
        engine = gameEngine,
        state = state,
        aiPlayerId = activeSeat.playerId,
        profile = config.profile,
        difficulty = config.difficulty,
        ledger = ledger,
      )
      assertTrue("Every AI turn must be valid, rejected with: $step", step is EngineStepResult.Success)
      state = (step as EngineStepResult.Success).state
      ledger = ledger.recordStateTransition(prevColor, state.lastEvent)
      assertTrue(CardValidator.validateStateIntegrity(state).isValid)
      turnsPlayed++
    }

    assertTrue(turnsPlayed > 0)
    assertFalse(aiProfiles.any { it.bengaliName.isBlank() })
  }
}
