package com.example.data.remote.rtdb

import com.example.domain.game.CardColor
import com.example.domain.game.CardType
import com.example.domain.game.GameCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RtdbCardMapperTest {

  @Test
  fun `parseCardFields maps backend MYSTERY and MYSTERY_CARD accurately to SpecialCard`() {
    val fromBackend = RtdbCardMapper.parseCardFields("mys_1", "WILD", "MYSTERY", null)
    val fromCanon = RtdbCardMapper.parseCardFields("mys_2", "WILD", "MYSTERY_CARD", null)

    assertTrue(fromBackend is GameCard.SpecialCard)
    assertEquals(CardType.MYSTERY_CARD, fromBackend.type)
    assertEquals(CardColor.WILD, fromBackend.color)

    assertTrue(fromCanon is GameCard.SpecialCard)
    assertEquals(CardType.MYSTERY_CARD, fromCanon.type)
  }

  @Test
  fun `parseCardFields maps all action, special, and number cards without crash`() {
    val num = RtdbCardMapper.parseCardFields("n1", "BLUE", "NUMBER", 7) as GameCard.NumberCard
    assertEquals(7, num.number)
    assertEquals(CardColor.BLUE, num.color)

    val wild4 = RtdbCardMapper.parseCardFields("w4", "RED", "WILD_DRAW_FOUR", null) as GameCard.ActionCard
    assertEquals(CardColor.WILD, wild4.color)
    assertEquals(CardType.WILD_DRAW_FOUR, wild4.type)

    val thunder = RtdbCardMapper.parseCardFields("t3", "WILD", "THUNDERBOLT_PLUS_THREE", null)
    assertEquals(CardType.THUNDERBOLT_PLUS_THREE, thunder.type)

    val bounce = RtdbCardMapper.parseCardFields("bb", "GREEN", "BOUNCE_BACK", null)
    assertEquals(CardType.BOUNCE_BACK, bounce.type)
    assertEquals(CardColor.GREEN, bounce.color)
  }
}
