package com.example.ui.components

import com.example.domain.game.CardColor
import com.example.domain.game.CardType
import com.example.domain.game.GameCard
import com.example.domain.game.MysteryOutcome

/**
 * Maps pure domain card models into Bengali UI visual models and labels.
 * Keeps UI presentation strictly separated from domain game logic.
 */
fun CardColor.toRongmohalSuit(): RongmohalSuit = when (this) {
  CardColor.RED -> RongmohalSuit.SINDOOR
  CardColor.GREEN -> RongmohalSuit.PANNA
  CardColor.BLUE -> RongmohalSuit.NEELKANTHA
  CardColor.YELLOW -> RongmohalSuit.SHONALI
  CardColor.WILD -> RongmohalSuit.MOHAL_WILD
}

fun RongmohalSuit.toCardColor(): CardColor = when (this) {
  RongmohalSuit.SINDOOR -> CardColor.RED
  RongmohalSuit.PANNA -> CardColor.GREEN
  RongmohalSuit.NEELKANTHA -> CardColor.BLUE
  RongmohalSuit.SHONALI -> CardColor.YELLOW
  RongmohalSuit.MOHAL_WILD -> CardColor.WILD
}

fun CardColor.toBengaliColorName(): String = when (this) {
  CardColor.RED -> "লাল (সিঁদুর)"
  CardColor.BLUE -> "নীল (নীলকণ্ঠ)"
  CardColor.GREEN -> "সবুজ (পান্না)"
  CardColor.YELLOW -> "হলুদ (সোনালী)"
  CardColor.WILD -> "বহুরঙা (মহল)"
}

fun CardType.toRongmohalActionType(): RongmohalActionType = when (this) {
  CardType.SKIP -> RongmohalActionType.SKIP
  CardType.REVERSE -> RongmohalActionType.REVERSE
  CardType.DRAW_TWO -> RongmohalActionType.DRAW_TWO
  CardType.WILD -> RongmohalActionType.WILD_COLOR
  CardType.WILD_DRAW_FOUR -> RongmohalActionType.WILD_DRAW_FOUR
  CardType.THUNDERBOLT_PLUS_THREE -> RongmohalActionType.THUNDERBOLT_PLUS_THREE
  CardType.HAND_SWAP -> RongmohalActionType.HAND_SWAP
  CardType.BOUNCE_BACK -> RongmohalActionType.BOUNCE_BACK
  CardType.EVERYONE_DRAW_ONE -> RongmohalActionType.EVERYONE_DRAW_ONE
  CardType.MYSTERY_CARD -> RongmohalActionType.MYSTERY_CARD
  CardType.NUMBER -> RongmohalActionType.SKIP
}

fun GameCard.toUiCardModel(): RongmohalCardModel = when (this) {
  is GameCard.NumberCard -> RongmohalCardModel.NumberCard(
    id = id,
    suit = color.toRongmohalSuit(),
    number = number,
  )
  is GameCard.ActionCard -> RongmohalCardModel.ActionCard(
    id = id,
    suit = color.toRongmohalSuit(),
    action = type.toRongmohalActionType(),
  )
  is GameCard.SpecialCard -> RongmohalCardModel.ActionCard(
    id = id,
    suit = color.toRongmohalSuit(),
    action = type.toRongmohalActionType(),
  )
}

fun CardType.toBengaliTitle(): String = when (this) {
  CardType.NUMBER -> "সংখ্যা কার্ড"
  CardType.SKIP -> "থামাও"
  CardType.REVERSE -> "ঘোরাও"
  CardType.DRAW_TWO -> "দুই টানো (+২)"
  CardType.WILD -> "রঙ বদল"
  CardType.WILD_DRAW_FOUR -> "মহাচাল (+৪)"
  CardType.THUNDERBOLT_PLUS_THREE -> "বজ্রপাত (+৩)"
  CardType.HAND_SWAP -> "হাতবদল"
  CardType.BOUNCE_BACK -> "বাউন্স ব্যাক"
  CardType.EVERYONE_DRAW_ONE -> "সবাই মিলে +১"
  CardType.MYSTERY_CARD -> "রহস্য কার্ড"
}

fun MysteryOutcome.toBengaliDescription(): String = when (this) {
  MysteryOutcome.NEXT_PLAYER_DRAWS_TWO -> "রহস্য চমক: পরের খেলোয়াড়কে +২ তাস টানতে হবে!"
  MysteryOutcome.GIFT_RANDOM_CARD_TO_NEXT -> "রহস্য উপহার: আপনার ১টি তাস পরের খেলোয়াড়ের হাতে গেল!"
  MysteryOutcome.SHIELD_AND_SKIP_NEXT -> "রহস্য বর্ম: পরের খেলোয়াড়ের চাল বাদ পড়েছে!"
  MysteryOutcome.REVERSE_AND_ROTATE_COLOR -> "রহস্য ঘূর্ণি: দিক উল্টে গেছে ও টেবিলের রঙ বদলেছে!"
}
