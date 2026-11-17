package com.example.ui.screens.mode

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.RongmohalPrimaryButton
import com.example.ui.components.TwoPaneAdaptiveLayout
import com.example.ui.theme.LocalRongmohalTokens

enum class GameModeId(
  val titleRes: Int,
  val descRes: Int,
  val badgeText: String,
  val startingCards: Int,
) {
  CLASSIC(R.string.mode_classic_title, R.string.mode_classic_desc, "৭ তাস", 7),
  BLITZ(R.string.mode_blitz_title, R.string.mode_blitz_desc, "৫ তাস", 5),
  TACTICAL(R.string.mode_tactical_title, R.string.mode_tactical_desc, "কৌশল", 7),
  ONLINE(R.string.mode_online_title, R.string.mode_online_desc, "লাইভ", 7),
}

@Composable
fun GameModeSelectionScreen(
  onNavigateBack: () -> Unit,
  onSelectLocalMode: (GameModeId) -> Unit,
  onOpenOnlineLobby: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val tokens = LocalRongmohalTokens.current
  var selectedMode by rememberSaveable { mutableStateOf(GameModeId.CLASSIC) }

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 1080.dp)
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        IconButton(
          onClick = onNavigateBack,
          modifier = Modifier
            .size(48.dp)
            .testTag("mode_back_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.board_back_confirm),
          )
        }
        Column {
          Text(
            text = stringResource(R.string.mode_title),
            style = MaterialTheme.typography.headlineMedium,
          )
          Text(
            text = stringResource(R.string.mode_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      TwoPaneAdaptiveLayout(
        primaryPane = { paneModifier ->
          Column(
            modifier = paneModifier,
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            GameModeOptionCard(
              mode = GameModeId.CLASSIC,
              icon = Icons.Filled.Style,
              accentColor = tokens.suitPalette.pannaPrimary,
              selected = selectedMode == GameModeId.CLASSIC,
              onClick = { selectedMode = GameModeId.CLASSIC },
            )
            GameModeOptionCard(
              mode = GameModeId.BLITZ,
              icon = Icons.Filled.Bolt,
              accentColor = tokens.suitPalette.shonaliPrimary,
              selected = selectedMode == GameModeId.BLITZ,
              onClick = { selectedMode = GameModeId.BLITZ },
            )
          }
        },
        secondaryPane = { paneModifier ->
          Column(
            modifier = paneModifier,
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            GameModeOptionCard(
              mode = GameModeId.TACTICAL,
              icon = Icons.Filled.Psychology,
              accentColor = tokens.suitPalette.sindoorPrimary,
              selected = selectedMode == GameModeId.TACTICAL,
              onClick = { selectedMode = GameModeId.TACTICAL },
            )
            GameModeOptionCard(
              mode = GameModeId.ONLINE,
              icon = Icons.Filled.Groups,
              accentColor = tokens.suitPalette.neelkanthaPrimary,
              selected = selectedMode == GameModeId.ONLINE,
              onClick = { selectedMode = GameModeId.ONLINE },
            )
          }
        },
      )

      Spacer(modifier = Modifier.height(8.dp))

      RongmohalPrimaryButton(
        text = if (selectedMode == GameModeId.ONLINE) {
          stringResource(R.string.menu_online_lobby)
        } else {
          stringResource(R.string.mode_continue)
        },
        icon = Icons.AutoMirrored.Filled.ArrowForward,
        onClick = {
          if (selectedMode == GameModeId.ONLINE) {
            onOpenOnlineLobby()
          } else {
            onSelectLocalMode(selectedMode)
          }
        },
        testTag = "mode_continue_button",
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}

@Composable
private fun GameModeOptionCard(
  mode: GameModeId,
  icon: ImageVector,
  accentColor: Color,
  selected: Boolean,
  onClick: () -> Unit,
) {
  Card(
    onClick = onClick,
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(
      containerColor = if (selected) {
        accentColor.copy(alpha = 0.12f)
      } else {
        MaterialTheme.colorScheme.surfaceContainerLow
      },
    ),
    border = if (selected) BorderStroke(2.dp, accentColor) else null,
    modifier = Modifier
      .fillMaxWidth()
      .testTag("mode_card_${mode.name.lowercase()}"),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Surface(
        color = accentColor,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.size(52.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = icon,
            contentDescription = stringResource(mode.titleRes),
            tint = Color.White,
          )
        }
      }
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = stringResource(mode.titleRes),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Surface(
            color = accentColor.copy(alpha = 0.18f),
            shape = MaterialTheme.shapes.extraSmall,
          ) {
            Text(
              text = mode.badgeText,
              style = MaterialTheme.typography.labelSmall,
              color = accentColor,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
          }
        }
        Text(
          text = stringResource(mode.descRes),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}
