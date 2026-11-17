package com.example.domain.game

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeckGeneratorAndDistributionTest {

  private val generator = DefaultDeckGenerator()

  @Test
  fun `classic deck generates exact default 108 cards with 0 to 9 and zero special cards`() {
    val classicDeck = generator.generateClassicDeck()

    assertEquals(108, classicDeck.size)
    assertTrue("Classic deck must not contain any special cards", classicDeck.none { it.type.isSpecial })

    // Verify zero duplicate card IDs
    val uniqueIds = classicDeck.map { it.id }.toSet()
    assertEquals(classicDeck.size, uniqueIds.size)

    // Verify 0..9 distribution for each standard color
    for (color in CardColor.STANDARD_COLORS) {
      val numbersForColor = classicDeck
        .filterIsInstance<GameCard.NumberCard>()
        .filter { it.color == color }
      assertEquals(1, numbersForColor.count { it.number == 0 })
      for (n in 1..9) {
        assertEquals(2, numbersForColor.count { it.number == n })
      }
      assertEquals(2, classicDeck.count { it.color == color && it.type == CardType.SKIP })
      assertEquals(2, classicDeck.count { it.color == color && it.type == CardType.REVERSE })
      assertEquals(2, classicDeck.count { it.color == color && it.type == CardType.DRAW_TWO })
    }

    assertEquals(4, classicDeck.count { it.type == CardType.WILD })
    assertEquals(4, classicDeck.count { it.type == CardType.WILD_DRAW_FOUR })
  }

  @Test
  fun `special deck keeps special cards separate and honors configurable counts`() {
    val customClassic = ClassicDeckConfig(
      zeroCountPerColor = 2,
      oneToNineCountPerColor = 1,
      skipCountPerColor = 1,
      reverseCountPerColor = 1,
      drawTwoCountPerColor = 1,
      wildCount = 2,
      wildDrawFourCount = 2,
    )
    val customSpecial = SpecialCardsConfig(
      thunderboltPlusThreeCount = 4,
      handSwapCount = 3,
      bounceBackCountPerColor = 2,
      everyoneDrawOneCount = 2,
      mysteryCardCount = 5,
    )
    val specialDeck = generator.generateSpecialDeck(
      SpecialDeckConfig(
        baseClassicConfig = customClassic,
        specialCardsConfig = customSpecial,
      )
    )

    val expectedTotal = customClassic.expectedCardCount() + customSpecial.expectedSpecialCount()
    assertEquals(expectedTotal, specialDeck.size)
    assertEquals(specialDeck.size, specialDeck.map { it.id }.toSet().size)

    assertEquals(4, specialDeck.count { it.type == CardType.THUNDERBOLT_PLUS_THREE })
    assertEquals(3, specialDeck.count { it.type == CardType.HAND_SWAP })
    assertEquals(8, specialDeck.count { it.type == CardType.BOUNCE_BACK })
    assertEquals(2, specialDeck.count { it.type == CardType.EVERYONE_DRAW_ONE })
    assertEquals(5, specialDeck.count { it.type == CardType.MYSTERY_CARD })
  }

  @Test(expected = IllegalArgumentException::class)
  fun `duplicate card ID check rejects duplicate ids`() {
    val cardA = GameCard.NumberCard("dup_1", CardColor.RED, 4)
    val cardB = GameCard.NumberCard("dup_1", CardColor.BLUE, 4)
    DefaultDeckGenerator.ensureNoDuplicateIds(listOf(cardA, cardB))
  }

  @Test
  fun `fisher yates shuffle preserves exact cards without duplicates and changes order`() {
    val classicDeck = generator.generateClassicDeck()
    val shuffler = FisherYatesShuffleAlgorithm(Random(42))
    val shuffled = shuffler.shuffle(classicDeck)

    assertEquals(classicDeck.size, shuffled.size)
    assertEquals(classicDeck.map { it.id }.toSet(), shuffled.map { it.id }.toSet())
    assertNotEquals(classicDeck.map { it.id }, shuffled.map { it.id })
  }

  @Test
  fun `card distributor deals exact hand sizes and conserves every card id`() {
    val deck = generator.generateSpecialDeck()
    val distributor = CardDistributor(FisherYatesShuffleAlgorithm(Random(7)))
    val players = listOf("p1", "p2", "p3", "p4")

    val setup = distributor.distribute(
      deck = deck,
      playerIds = players,
      cardsPerPlayer = 7,
    )

    assertEquals(4, setup.hands.size)
    players.forEach { pid ->
      assertEquals(7, setup.hands.getValue(pid).size)
    }
    assertEquals(1, setup.discardPile.size)
    assertTrue(setup.discardPile.topCard is GameCard.NumberCard)
    assertEquals(deck.size - (4 * 7) - 1, setup.drawPile.size)

    val allDistributedIds = setup.hands.values.flatten().map { it.id } +
      setup.drawPile.cards.map { it.id } +
      setup.discardPile.cards.map { it.id }

    assertEquals(deck.size, allDistributedIds.size)
    assertEquals(deck.map { it.id }.toSet(), allDistributedIds.toSet())
  }

  @Test(expected = IllegalArgumentException::class)
  fun `card distributor rejects dealing when deck is too small`() {
    val tinyConfig = ClassicDeckConfig(
      zeroCountPerColor = 1,
      oneToNineCountPerColor = 0,
      skipCountPerColor = 0,
      reverseCountPerColor = 0,
      drawTwoCountPerColor = 0,
      wildCount = 0,
      wildDrawFourCount = 0,
    )
    val tinyDeck = generator.generateClassicDeck(tinyConfig) // 4 cards
    val distributor = CardDistributor(FisherYatesShuffleAlgorithm(Random(1)))

    distributor.distribute(
      deck = tinyDeck,
      playerIds = listOf("p1", "p2"),
      cardsPerPlayer = 3,
    )
  }

  @Test
  fun `draw pile rejects creation when given duplicate cards`() {
    val card1 = GameCard.NumberCard("same", CardColor.GREEN, 2)
    val card2 = GameCard.NumberCard("same", CardColor.YELLOW, 2)
    var thrown = false
    try {
      DrawPile(listOf(card1, card2))
    } catch (e: IllegalArgumentException) {
      thrown = true
    }
    assertTrue(thrown)
    assertFalse(!thrown)
  }
}
