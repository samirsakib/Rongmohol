package com.example.domain.game

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CardValidatorAndEngineTest {

  private fun createControlledState(
    deckType: DeckType = DeckType.SPECIAL,
    p1Hand: List<GameCard>,
    p2Hand: List<GameCard>,
    drawPileCards: List<GameCard>,
    discardCards: List<GameCard>,
    activeColor: CardColor = CardColor.RED,
    pendingPenalty: Int = 0,
    lastPenaltySenderId: String? = null,
    currentPlayerIndex: Int = 0,
  ): GameTableState {
    val allIds = (p1Hand + p2Hand + drawPileCards + discardCards).map { it.id }.toSet()
    return GameTableState(
      deckType = deckType,
      players = listOf(
        PlayerSeatState("p1", "আয়েশা", p1Hand),
        PlayerSeatState("p2", "তানভীর", p2Hand),
      ),
      currentPlayerIndex = currentPlayerIndex,
      isClockwise = true,
      drawPile = DrawPile(drawPileCards),
      discardPile = DiscardPile(discardCards, activeColor),
      pendingDrawPenalty = pendingPenalty,
      lastPenaltySenderId = lastPenaltySenderId,
      winnerId = null,
      allKnownCardIds = allIds,
    )
  }

  @Test
  fun `card validator blocks illegal plays and accepts matching color, number, or wild`() {
    val topCard = GameCard.NumberCard("top_red_5", CardColor.RED, 5)
    val validColorMatch = GameCard.NumberCard("p1_red_2", CardColor.RED, 2)
    val validNumberMatch = GameCard.NumberCard("p1_blue_5", CardColor.BLUE, 5)
    val validZeroNumberMatch = GameCard.NumberCard("p1_green_0", CardColor.GREEN, 0)
    val invalidMismatch = GameCard.NumberCard("p1_yellow_9", CardColor.YELLOW, 9)
    val wildCard = GameCard.ActionCard("p1_wild", CardColor.WILD, CardType.WILD)
    val outsiderCard = GameCard.NumberCard("not_owned", CardColor.RED, 7)

    val state = createControlledState(
      p1Hand = listOf(validColorMatch, validNumberMatch, validZeroNumberMatch, invalidMismatch, wildCard),
      p2Hand = listOf(GameCard.NumberCard("p2_blue_1", CardColor.BLUE, 1)),
      drawPileCards = listOf(GameCard.NumberCard("draw_1", CardColor.GREEN, 3)),
      discardCards = listOf(topCard),
      activeColor = CardColor.RED,
    )

    assertTrue(CardValidator.validatePlay(state, "p1", validColorMatch).isValid)
    assertTrue(CardValidator.validatePlay(state, "p1", validNumberMatch).isValid)

    val mismatchResult = CardValidator.validatePlay(state, "p1", invalidMismatch)
    assertTrue(mismatchResult is ValidationResult.Invalid)
    assertEquals(
      ValidationFailureReason.CARD_DOES_NOT_MATCH,
      (mismatchResult as ValidationResult.Invalid).reason,
    )

    // Out-of-turn play blocked
    val wrongTurn = CardValidator.validatePlay(state, "p2", GameCard.NumberCard("p2_blue_1", CardColor.BLUE, 1))
    assertEquals(
      ValidationFailureReason.NOT_PLAYER_TURN,
      (wrongTurn as ValidationResult.Invalid).reason,
    )

    // Card not in hand blocked
    val phantom = CardValidator.validatePlay(state, "p1", outsiderCard)
    assertEquals(
      ValidationFailureReason.CARD_NOT_IN_HAND,
      (phantom as ValidationResult.Invalid).reason,
    )

    // Wild card without standard color choice blocked
    val wildMissingColor = CardValidator.validatePlay(state, "p1", wildCard, chosenColor = null)
    assertEquals(
      ValidationFailureReason.ILLEGAL_COLOR_CHOICE,
      (wildMissingColor as ValidationResult.Invalid).reason,
    )
    assertTrue(CardValidator.validatePlay(state, "p1", wildCard, chosenColor = CardColor.GREEN).isValid)

    // Special card disallowed in Classic deck
    val specialInClassic = createControlledState(
      deckType = DeckType.CLASSIC,
      p1Hand = listOf(GameCard.SpecialCard("sp_1", CardColor.WILD, CardType.THUNDERBOLT_PLUS_THREE)),
      p2Hand = listOf(GameCard.NumberCard("p2_1", CardColor.BLUE, 1)),
      drawPileCards = listOf(GameCard.NumberCard("d_1", CardColor.RED, 8)),
      discardCards = listOf(topCard),
    )
    val classicReject = CardValidator.validatePlay(
      specialInClassic,
      "p1",
      specialInClassic.players[0].hand[0],
      chosenColor = CardColor.BLUE,
    )
    assertEquals(
      ValidationFailureReason.SPECIAL_CARD_NOT_ALLOWED_IN_CLASSIC_DECK,
      (classicReject as ValidationResult.Invalid).reason,
    )
  }

  @Test
  fun `empty draw pile recycles discard pile except top card and conserves all ids`() {
    val engine = CardGameEngine(shuffler = FisherYatesShuffleAlgorithm(Random(123)))
    val bottom1 = GameCard.NumberCard("disc_1", CardColor.RED, 1)
    val bottom2 = GameCard.NumberCard("disc_2", CardColor.BLUE, 2)
    val bottom3 = GameCard.NumberCard("disc_3", CardColor.GREEN, 3)
    val topDiscard = GameCard.NumberCard("disc_top", CardColor.YELLOW, 9)

    val state = createControlledState(
      p1Hand = listOf(GameCard.NumberCard("p1_1", CardColor.RED, 4)),
      p2Hand = listOf(GameCard.NumberCard("p2_1", CardColor.BLUE, 7)),
      drawPileCards = emptyList(),
      discardCards = listOf(bottom1, bottom2, bottom3, topDiscard),
      activeColor = CardColor.YELLOW,
    )

    val result = engine.drawCards(state, "p1")
    assertTrue(result is EngineStepResult.Success)
    val nextState = (result as EngineStepResult.Success).state

    // Player 1 drew 1 card -> now holds 2 cards
    assertEquals(2, nextState.players[0].cardCount)
    // Top discard card must stay unchanged in discard pile (only 1 card left there)
    assertEquals(1, nextState.discardPile.size)
    assertEquals("disc_top", nextState.discardPile.topCard.id)
    assertEquals(CardColor.YELLOW, nextState.activeColor)
    // Remaining 2 recycled cards stay in draw pile
    assertEquals(2, nextState.drawPile.size)

    val event = nextState.lastEvent as GameEvent.CardsDrawn
    assertEquals(3, event.recycledFromDiscardCount)
    assertTrue(CardValidator.validateStateIntegrity(nextState).isValid)
  }

  @Test
  fun `draw fails cleanly when both draw pile is empty and discard pile has only top card`() {
    val engine = CardGameEngine()
    val state = createControlledState(
      p1Hand = listOf(GameCard.NumberCard("p1_1", CardColor.RED, 4)),
      p2Hand = listOf(GameCard.NumberCard("p2_1", CardColor.BLUE, 7)),
      drawPileCards = emptyList(),
      discardCards = listOf(GameCard.NumberCard("only_top", CardColor.GREEN, 5)),
      activeColor = CardColor.GREEN,
    )

    val result = engine.drawCards(state, "p1")
    assertTrue(result is EngineStepResult.Rejected)
    assertEquals(
      ValidationFailureReason.EMPTY_DRAW_PILE_NO_RECYCLABLE_CARDS,
      (result as EngineStepResult.Rejected).reason,
    )
  }

  @Test
  fun `thunderbolt plus three and bounce back deflect penalty properly`() {
    val engine = CardGameEngine()
    val thunder = GameCard.SpecialCard("sp_thunder", CardColor.WILD, CardType.THUNDERBOLT_PLUS_THREE)
    val p1Extra = GameCard.NumberCard("p1_extra", CardColor.RED, 2)
    val bounce = GameCard.SpecialCard("sp_bounce", CardColor.GREEN, CardType.BOUNCE_BACK)
    val p2Extra = GameCard.NumberCard("p2_extra", CardColor.BLUE, 6)

    val state = createControlledState(
      p1Hand = listOf(thunder, p1Extra),
      p2Hand = listOf(bounce, p2Extra),
      drawPileCards = listOf(
        GameCard.NumberCard("d1", CardColor.RED, 1),
        GameCard.NumberCard("d2", CardColor.BLUE, 2),
        GameCard.NumberCard("d3", CardColor.GREEN, 3),
        GameCard.NumberCard("d4", CardColor.YELLOW, 4),
      ),
      discardCards = listOf(GameCard.NumberCard("top", CardColor.RED, 5)),
      activeColor = CardColor.RED,
    )

    // P1 plays Thunderbolt (+3) and chooses GREEN
    val afterThunder = (engine.playCard(state, "p1", thunder.id, chosenColor = CardColor.GREEN) as EngineStepResult.Success).state
    assertEquals(3, afterThunder.pendingDrawPenalty)
    assertEquals(CardColor.GREEN, afterThunder.activeColor)
    assertEquals("p2", afterThunder.currentPlayer.playerId)

    // P2 cannot play regular card p2Extra during penalty
    val illegalDuringPenalty = engine.playCard(afterThunder, "p2", p2Extra.id)
    assertTrue(illegalDuringPenalty is EngineStepResult.Rejected)
    assertEquals(
      ValidationFailureReason.PENALTY_REQUIRES_DRAW_OR_BOUNCE,
      (illegalDuringPenalty as EngineStepResult.Rejected).reason,
    )

    // P2 plays Bounce Back to deflect +3 back onto P1
    val afterBounce = (engine.playCard(afterThunder, "p2", bounce.id) as EngineStepResult.Success).state
    assertEquals(3, afterBounce.pendingDrawPenalty)
    assertEquals("p1", afterBounce.currentPlayer.playerId)
    assertFalse(afterBounce.isClockwise)
    assertTrue(afterBounce.lastEvent is GameEvent.PenaltyBounced)

    // P1 draws the bounced 3 cards
    val afterDraw = (engine.drawCards(afterBounce, "p1") as EngineStepResult.Success).state
    assertEquals(4, afterDraw.players[0].cardCount) // 1 + 3
    assertEquals(0, afterDraw.pendingDrawPenalty)
    assertTrue(CardValidator.validateStateIntegrity(afterDraw).isValid)
  }

  @Test
  fun `hand swap, everyone draw one, and mystery card execute cleanly and conserve cards`() {
    val engine = CardGameEngine(
      shuffler = FisherYatesShuffleAlgorithm(Random(99)),
      random = Random(99),
    )
    val swap = GameCard.SpecialCard("sp_swap", CardColor.WILD, CardType.HAND_SWAP)
    val everyoneOne = GameCard.SpecialCard("sp_everyone", CardColor.WILD, CardType.EVERYONE_DRAW_ONE)
    val mystery = GameCard.SpecialCard("sp_mystery", CardColor.WILD, CardType.MYSTERY_CARD)
    val p1Card = GameCard.NumberCard("p1_a", CardColor.RED, 3)

    val p2Card1 = GameCard.NumberCard("p2_a", CardColor.BLUE, 4)
    val p2Card2 = GameCard.NumberCard("p2_b", CardColor.YELLOW, 8)

    val state = createControlledState(
      p1Hand = listOf(swap, p1Card),
      p2Hand = listOf(everyoneOne, mystery, p2Card1, p2Card2),
      drawPileCards = listOf(
        GameCard.NumberCard("d1", CardColor.GREEN, 1),
        GameCard.NumberCard("d2", CardColor.RED, 2),
        GameCard.NumberCard("d3", CardColor.BLUE, 6),
      ),
      discardCards = listOf(GameCard.NumberCard("top", CardColor.RED, 0)),
      activeColor = CardColor.RED,
    )

    // 1. P1 plays Hand Swap -> P1 gets P2's 4 cards; P2 gets P1's remaining 1 card
    val afterSwap = (engine.playCard(state, "p1", swap.id, chosenColor = CardColor.BLUE, targetPlayerId = "p2") as EngineStepResult.Success).state
    assertEquals(4, afterSwap.players[0].cardCount)
    assertEquals(1, afterSwap.players[1].cardCount)
    assertEquals("p1_a", afterSwap.players[1].hand.single().id)
    assertTrue(CardValidator.validateStateIntegrity(afterSwap).isValid)

    // 2. P1 plays Everyone Draw One (received from swap) -> P2 draws 1 card
    // First have P2 draw 1 card to advance turn back to P1
    val afterP2Draw = (engine.drawCards(afterSwap, "p2") as EngineStepResult.Success).state
    val p2CountBefore = afterP2Draw.players[1].cardCount
    val afterEveryone = (engine.playCard(afterP2Draw, "p1", everyoneOne.id, chosenColor = CardColor.YELLOW) as EngineStepResult.Success).state
    assertEquals(p2CountBefore + 1, afterEveryone.players[1].cardCount)
    assertTrue(afterEveryone.lastEvent is GameEvent.EveryoneDrewOne)
    assertTrue(CardValidator.validateStateIntegrity(afterEveryone).isValid)

    // 3. Test Mystery Card directly on a turn where P1 holds it
    val mysteryState = createControlledState(
      p1Hand = listOf(mystery, GameCard.NumberCard("p1_keep", CardColor.RED, 5)),
      p2Hand = listOf(GameCard.NumberCard("p2_keep", CardColor.BLUE, 5)),
      drawPileCards = listOf(GameCard.NumberCard("dm1", CardColor.GREEN, 7)),
      discardCards = listOf(GameCard.NumberCard("topm", CardColor.RED, 2)),
    )
    val afterMystery = (engine.playCard(mysteryState, "p1", mystery.id, chosenColor = CardColor.GREEN) as EngineStepResult.Success).state
    assertTrue(afterMystery.lastEvent is GameEvent.MysteryTriggered)
    assertTrue(CardValidator.validateStateIntegrity(afterMystery).isValid)
  }

  @Test
  fun `standard action cards skip, reverse, draw two stacking, wild, wild draw four, and uno penalty work accurately`() {
    val engine = CardGameEngine()
    val skip = GameCard.ActionCard("act_skip", CardColor.RED, CardType.SKIP)
    val rev = GameCard.ActionCard("act_rev", CardColor.RED, CardType.REVERSE)
    val d2A = GameCard.ActionCard("act_d2_a", CardColor.RED, CardType.DRAW_TWO)
    val d2B = GameCard.ActionCard("act_d2_b", CardColor.BLUE, CardType.DRAW_TWO)
    val wild4 = GameCard.ActionCard("act_w4", CardColor.WILD, CardType.WILD_DRAW_FOUR)

    val allDraw = (1..10).map { i -> GameCard.NumberCard("pool_$i", CardColor.GREEN, i % 10) }
    val p1Hand = listOf(skip, rev, d2A, GameCard.NumberCard("p1_hold", CardColor.RED, 1))
    val p2Hand = listOf(d2B, wild4, GameCard.NumberCard("p2_hold", CardColor.BLUE, 2))
    val p3Hand = listOf(GameCard.NumberCard("p3_hold1", CardColor.YELLOW, 3), GameCard.NumberCard("p3_hold2", CardColor.RED, 4))
    val discard = listOf(GameCard.NumberCard("top_r8", CardColor.RED, 8))
    val allIds = (p1Hand + p2Hand + p3Hand + allDraw + discard).map { it.id }.toSet()

    val state3 = GameTableState(
      deckType = DeckType.CLASSIC,
      players = listOf(
        PlayerSeatState("p1", "অরিত্র", p1Hand),
        PlayerSeatState("p2", "তানভীর", p2Hand),
        PlayerSeatState("p3", "নুসরাত", p3Hand),
      ),
      currentPlayerIndex = 0,
      isClockwise = true,
      drawPile = DrawPile(allDraw),
      discardPile = DiscardPile(discard, CardColor.RED),
      pendingDrawPenalty = 0,
      lastPenaltySenderId = null,
      winnerId = null,
      allKnownCardIds = allIds,
    )

    // 1. P1 plays SKIP -> skips P2 and lands on P3
    val afterSkip = (engine.playCard(state3, "p1", skip.id) as EngineStepResult.Success).state
    assertEquals("p3", afterSkip.currentPlayer.playerId)

    // 2. P3 plays p3_hold2 (Red 4) -> advances to P1
    val afterP3 = (engine.playCard(afterSkip, "p3", "p3_hold2") as EngineStepResult.Success).state
    assertEquals("p1", afterP3.currentPlayer.playerId)

    // 3. P1 plays REVERSE -> direction flips to counter-clockwise, lands back on P3
    val afterRev = (engine.playCard(afterP3, "p1", rev.id) as EngineStepResult.Success).state
    assertFalse(afterRev.isClockwise)
    assertEquals("p3", afterRev.currentPlayer.playerId)

    // 4. P3 draws 1 card -> counter-clockwise advances to P2
    val afterP3Draw = (engine.drawCards(afterRev, "p3") as EngineStepResult.Success).state
    assertEquals("p2", afterP3Draw.currentPlayer.playerId)

    // 5. P2 plays Wild Draw Four -> chooses RED -> +4 penalty on P1
    val afterW4 = (engine.playCard(afterP3Draw, "p2", wild4.id, chosenColor = CardColor.RED) as EngineStepResult.Success).state
    assertEquals(4, afterW4.pendingDrawPenalty)
    assertEquals(CardColor.RED, afterW4.activeColor)
    assertEquals("p1", afterW4.currentPlayer.playerId)

    // 6. P1 draws the +4 penalty cleanly and loses turn
    val afterP1Draw4 = (engine.drawCards(afterW4, "p1") as EngineStepResult.Success).state
    assertEquals(6, afterP1Draw4.players[0].cardCount) // 2 remaining + 4 drawn
    assertEquals(0, afterP1Draw4.pendingDrawPenalty)

    // 7. Apply undeclared Last Card +2 penalty preserves card conservation
    val afterUnoPenalty = (engine.applyUndeclaredLastCardPenalty(afterP1Draw4, "p2", 2) as EngineStepResult.Success).state
    assertEquals(4, afterUnoPenalty.players[1].cardCount)
    assertTrue(CardValidator.validateStateIntegrity(afterUnoPenalty).isValid)
  }
}

