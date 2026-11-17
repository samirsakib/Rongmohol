package com.example.ui.screens.rules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.AnimatedFlippableCard
import com.example.ui.components.GameActionCard
import com.example.ui.components.GameCardBack
import com.example.ui.components.GameNumberCard
import com.example.ui.components.RongmohalActionType
import com.example.ui.components.RongmohalSuit
import com.example.ui.components.TwoPaneAdaptiveLayout
import com.example.ui.theme.HindSiliguriFontFamily
import com.example.ui.theme.LocalRongmohalTokens
import com.example.ui.theme.NotoSansBengaliFontFamily
import com.example.ui.theme.NotoSerifBengaliFontFamily

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RulesScreen(modifier: Modifier = Modifier) {
  val tokens = LocalRongmohalTokens.current
  val baseSuits = listOf(
    RongmohalSuit.SINDOOR,
    RongmohalSuit.PANNA,
    RongmohalSuit.NEELKANTHA,
    RongmohalSuit.SHONALI,
  )
  var isDemoCardFaceUp by rememberSaveable { mutableStateOf(false) }

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 1120.dp)
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text(
        text = stringResource(R.string.rules_title),
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
      )
      Text(
        text = stringResource(R.string.rules_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      TwoPaneAdaptiveLayout(
        primaryPane = { paneModifier ->
          Column(
            modifier = paneModifier,
            verticalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            Card(
              shape = MaterialTheme.shapes.large,
              colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
              ),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
              ) {
                Text(
                  text = stringResource(R.string.rules_suits_title),
                  style = MaterialTheme.typography.titleLarge,
                )
                FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  baseSuits.forEach { suit ->
                    Surface(
                      color = suit.primaryColor(tokens.suitPalette),
                      shape = MaterialTheme.shapes.small,
                    ) {
                      Text(
                        text = "${suit.bengaliName} • ${suit.emblemName}",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                      )
                    }
                  }
                }

                Text(
                  text = stringResource(R.string.rules_number_cards_title),
                  style = MaterialTheme.typography.titleMedium,
                )
                FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                  verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  GameNumberCard(suit = RongmohalSuit.SINDOOR, number = 0, cardWidth = 72.dp, cardHeight = 104.dp)
                  GameNumberCard(suit = RongmohalSuit.PANNA, number = 3, cardWidth = 72.dp, cardHeight = 104.dp)
                  GameNumberCard(suit = RongmohalSuit.NEELKANTHA, number = 6, cardWidth = 72.dp, cardHeight = 104.dp)
                  GameNumberCard(suit = RongmohalSuit.SHONALI, number = 9, cardWidth = 72.dp, cardHeight = 104.dp)
                }
              }
            }

            Card(
              shape = MaterialTheme.shapes.large,
              colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
              ),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Text(
                  text = stringResource(R.string.rules_action_cards_title),
                  style = MaterialTheme.typography.titleLarge,
                )
                FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                  verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  GameActionCard(RongmohalSuit.SINDOOR, RongmohalActionType.SKIP, cardWidth = 74.dp, cardHeight = 108.dp)
                  GameActionCard(RongmohalSuit.PANNA, RongmohalActionType.REVERSE, cardWidth = 74.dp, cardHeight = 108.dp)
                  GameActionCard(RongmohalSuit.NEELKANTHA, RongmohalActionType.DRAW_TWO, cardWidth = 74.dp, cardHeight = 108.dp)
                  GameActionCard(RongmohalSuit.MOHAL_WILD, RongmohalActionType.WILD_COLOR, cardWidth = 74.dp, cardHeight = 108.dp)
                  GameActionCard(RongmohalSuit.MOHAL_WILD, RongmohalActionType.WILD_DRAW_FOUR, cardWidth = 74.dp, cardHeight = 108.dp)
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = stringResource(R.string.rules_special_cards_title),
                  style = MaterialTheme.typography.titleLarge,
                )
                FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                  verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  GameActionCard(RongmohalSuit.MOHAL_WILD, RongmohalActionType.THUNDERBOLT_PLUS_THREE, cardWidth = 74.dp, cardHeight = 108.dp)
                  GameActionCard(RongmohalSuit.MOHAL_WILD, RongmohalActionType.HAND_SWAP, cardWidth = 74.dp, cardHeight = 108.dp)
                  GameActionCard(RongmohalSuit.PANNA, RongmohalActionType.BOUNCE_BACK, cardWidth = 74.dp, cardHeight = 108.dp)
                  GameActionCard(RongmohalSuit.MOHAL_WILD, RongmohalActionType.EVERYONE_DRAW_ONE, cardWidth = 74.dp, cardHeight = 108.dp)
                  GameActionCard(RongmohalSuit.MOHAL_WILD, RongmohalActionType.MYSTERY_CARD, cardWidth = 74.dp, cardHeight = 108.dp)
                }
              }
            }
          }
        },
        secondaryPane = { paneModifier ->
          Column(
            modifier = paneModifier,
            verticalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            Card(
              shape = MaterialTheme.shapes.large,
              colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
              ),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Text(
                  text = stringResource(R.string.rules_how_to_play_title),
                  style = MaterialTheme.typography.titleLarge,
                )
                Text(
                  text = stringResource(R.string.rules_how_to_play_body),
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }
            }

            Card(
              shape = MaterialTheme.shapes.large,
              colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
              ),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier
                  .clickable { isDemoCardFaceUp = !isDemoCardFaceUp }
                  .padding(20.dp)
                  .testTag("rules_flip_card_demo"),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                AnimatedFlippableCard(
                  isFaceUp = isDemoCardFaceUp,
                  cardWidth = 82.dp,
                  cardHeight = 118.dp,
                  frontContent = {
                    GameActionCard(
                      suit = RongmohalSuit.MOHAL_WILD,
                      action = RongmohalActionType.THUNDERBOLT_PLUS_THREE,
                      cardWidth = 82.dp,
                      cardHeight = 118.dp,
                    )
                  },
                  backContent = {
                    GameCardBack(cardWidth = 82.dp, cardHeight = 118.dp)
                  },
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                  Text(
                    text = stringResource(R.string.rules_card_back_title),
                    style = MaterialTheme.typography.titleMedium,
                  )
                  Text(
                    text = "নকশীকাঁথা জালিকা ও অষ্টভুজ শাপলা সীলযুক্ত তাস। উল্টে দেখতে কার্ডে ট্যাপ করুন!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }

            Card(
              shape = MaterialTheme.shapes.large,
              colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
              ),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Text(
                  text = stringResource(R.string.rules_typography_title),
                  style = MaterialTheme.typography.titleLarge,
                )
                Text(
                  text = stringResource(R.string.rules_typography_sample_serif),
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = NotoSerifBengaliFontFamily,
                  ),
                  color = MaterialTheme.colorScheme.primary,
                )
                Text(
                  text = stringResource(R.string.rules_typography_sample_sans),
                  style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = HindSiliguriFontFamily,
                  ),
                )
                Text(
                  text = stringResource(R.string.rules_typography_digits),
                  style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = NotoSansBengaliFontFamily,
                  ),
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        },
      )
      Spacer(modifier = Modifier.height(8.dp))
    }
  }
}
