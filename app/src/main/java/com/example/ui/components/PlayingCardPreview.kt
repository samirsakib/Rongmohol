package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalCardTableColors

enum class CardSuit(val symbol: String, val bengaliName: String, val isRed: Boolean) {
  SPADE("♠", "ইশকাপন", false),
  HEART("♥", "হরতন", true),
  DIAMOND("♦", "রুইতন", true),
  CLUB("♣", "চিড়িয়া", false),
}

@Composable
fun PlayingCardPreview(
  rankLabel: String,
  suit: CardSuit,
  modifier: Modifier = Modifier,
  cardWidth: Dp = 74.dp,
  cardHeight: Dp = 108.dp,
) {
  val tableColors = LocalCardTableColors.current
  val inkColor = if (suit.isRed) tableColors.suitRed else tableColors.suitBlack

  Card(
    modifier = modifier
      .width(cardWidth)
      .height(cardHeight),
    shape = MaterialTheme.shapes.small,
    colors = CardDefaults.cardColors(containerColor = tableColors.cardFace),
    border = BorderStroke(1.5.dp, tableColors.goldTrim.copy(alpha = 0.7f)),
    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(6.dp),
    ) {
      Column(
        modifier = Modifier.align(Alignment.TopStart),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          text = rankLabel,
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          color = inkColor,
        )
        Text(
          text = suit.symbol,
          style = MaterialTheme.typography.labelSmall,
          color = inkColor,
        )
      }

      Text(
        text = suit.symbol,
        style = MaterialTheme.typography.headlineMedium,
        color = inkColor,
        modifier = Modifier.align(Alignment.Center),
      )

      Text(
        text = suit.bengaliName,
        style = MaterialTheme.typography.labelSmall,
        color = inkColor.copy(alpha = 0.75f),
        modifier = Modifier.align(Alignment.BottomEnd),
      )
    }
  }
}

@Composable
fun TwoPaneAdaptiveLayout(
  primaryPane: @Composable (Modifier) -> Unit,
  secondaryPane: @Composable (Modifier) -> Unit,
  modifier: Modifier = Modifier,
  breakpoint: Dp = 640.dp,
) {
  BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
    if (maxWidth >= breakpoint) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.Top,
      ) {
        primaryPane(Modifier.weight(1.1f))
        secondaryPane(Modifier.weight(0.9f))
      }
    } else {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        primaryPane(Modifier.fillMaxWidth())
        secondaryPane(Modifier.fillMaxWidth())
      }
    }
  }
}
