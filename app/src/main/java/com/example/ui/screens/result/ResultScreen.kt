package com.example.ui.screens.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.config.toBengaliDigits
import com.example.ui.components.PlayerAvatarBadge
import com.example.ui.components.RongmohalAvatar
import com.example.ui.components.RongmohalOutlinedButton
import com.example.ui.components.RongmohalPrimaryButton
import com.example.ui.components.TableTextureBackground
import com.example.ui.screens.mode.GameModeId
import com.example.ui.theme.GoldTrimAccent

@Composable
fun ResultScreen(
  mode: GameModeId,
  playerName: String,
  avatarId: Int,
  playerCount: Int,
  isVictory: Boolean,
  playerScore: Int,
  opponentScore: Int,
  onPlayAgain: () -> Unit,
  onBackToMenu: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: ResultViewModel = hiltViewModel(),
) {
  val useBengaliDigits by viewModel.useBengaliNumerals.collectAsStateWithLifecycle()

  LaunchedEffect(mode, playerName, playerScore, opponentScore, isVictory) {
    viewModel.recordMatchOnce(
      matchKey = "${mode.name}_${playerScore}_${opponentScore}_$isVictory",
      mode = mode,
      playerCount = playerCount,
      playerScore = playerScore,
      opponentScore = opponentScore,
      isVictory = isVictory,
    )
  }

  TableTextureBackground(modifier = modifier.fillMaxSize()) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .safeDrawingPadding()
        .padding(20.dp),
      contentAlignment = Alignment.Center,
    ) {
      if (isVictory) {
        com.example.ui.components.ConfettiCelebrationOverlay()
      }
      Card(
        modifier = Modifier
          .widthIn(max = 620.dp)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          Surface(
            color = if (isVictory) GoldTrimAccent.copy(alpha = 0.24f) else MaterialTheme.colorScheme.secondaryContainer,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.size(72.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Filled.EmojiEvents,
                contentDescription = null,
                tint = if (isVictory) GoldTrimAccent else MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(40.dp),
              )
            }
          }

          Text(
            text = if (isVictory) {
              stringResource(R.string.result_victory_title)
            } else {
              stringResource(R.string.result_defeat_title)
            },
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
          )
          Text(
            text = "${stringResource(mode.titleRes)} • ${stringResource(R.string.result_subtitle)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
          )

          Text(
            text = stringResource(R.string.result_scoreboard_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.Start),
          )

          val playerAvatar = RongmohalAvatar.byId(avatarId)
          val aiProfiles = com.example.domain.ai.AiRoster.selectOpponents(
            count = (playerCount - 1).coerceIn(1, 3),
            seedOffset = avatarId % com.example.domain.ai.AiRoster.ALL.size,
          )
          val rows = buildList {
            add(Triple(playerName, playerAvatar, playerScore))
            aiProfiles.forEachIndexed { i, profile ->
              val botAvatar = RongmohalAvatar.byId(profile.preferredAvatarId)
              add(Triple(profile.bengaliName, botAvatar, (opponentScore - i * 12).coerceAtLeast(15)))
            }
          }.sortedByDescending { it.third }

          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rows.forEachIndexed { index, (name, avatar, score) ->
              Surface(
                color = if (index == 0) {
                  MaterialTheme.colorScheme.primaryContainer
                } else {
                  MaterialTheme.colorScheme.surfaceContainer
                },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                  ) {
                    Text(
                      text = "#${(index + 1).toBengaliDigits(useBengaliDigits)}",
                      style = MaterialTheme.typography.titleMedium,
                      color = if (index == 0) MaterialTheme.colorScheme.primary else Color.Gray,
                    )
                    PlayerAvatarBadge(avatar = avatar, size = 38.dp)
                    Text(
                      text = name,
                      style = MaterialTheme.typography.titleSmall,
                    )
                  }
                  Text(
                    text = "${score.toBengaliDigits(useBengaliDigits)} পয়েন্ট",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                  )
                }
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            RongmohalOutlinedButton(
              text = stringResource(R.string.result_back_to_menu),
              icon = Icons.Filled.Home,
              onClick = onBackToMenu,
              testTag = "result_home_button",
              modifier = Modifier.weight(1f),
            )
            RongmohalPrimaryButton(
              text = stringResource(R.string.result_play_again),
              icon = Icons.Filled.Replay,
              onClick = onPlayAgain,
              testTag = "result_play_again_button",
              modifier = Modifier.weight(1f),
            )
          }
        }
      }
    }
  }
}
