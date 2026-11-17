package com.example.ui.screens.players

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.config.toBengaliDigits
import com.example.domain.ai.AiDifficulty
import com.example.domain.ai.AiRoster
import com.example.ui.components.PlayerAvatarBadge
import com.example.ui.components.RongmohalAvatar
import com.example.ui.components.RongmohalPrimaryButton
import com.example.ui.components.TwoPaneAdaptiveLayout
import com.example.ui.screens.mode.GameModeId

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayerSelectionScreen(
  mode: GameModeId,
  onNavigateBack: () -> Unit,
  onStartGame: (playerCount: Int, playerName: String, avatarId: Int, difficulty: AiDifficulty) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: PlayerSelectionViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  var playerCount by rememberSaveable { mutableIntStateOf(4) }
  var playerName by rememberSaveable { mutableStateOf("") }
  var selectedDifficultyName by rememberSaveable {
    mutableStateOf(
      if (mode == GameModeId.TACTICAL) AiDifficulty.HARD.name else AiDifficulty.MEDIUM.name
    )
  }
  val selectedDifficulty = AiDifficulty.entries.firstOrNull { it.name == selectedDifficultyName }
    ?: AiDifficulty.MEDIUM

  LaunchedEffect(uiState.defaultName) {
    if (playerName.isBlank()) {
      playerName = uiState.defaultName
    }
  }

  val previewOpponents = AiRoster.selectOpponents(
    count = (playerCount - 1).coerceIn(1, 3),
    seedOffset = uiState.selectedAvatarId % AiRoster.ALL.size,
  )

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
            .testTag("player_sel_back_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.board_back_confirm),
          )
        }
        Column {
          Text(
            text = stringResource(R.string.player_sel_title),
            style = MaterialTheme.typography.headlineMedium,
          )
          Text(
            text = "${stringResource(mode.titleRes)} • ${stringResource(R.string.player_sel_subtitle)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      TwoPaneAdaptiveLayout(
        primaryPane = { paneModifier ->
          Card(
            modifier = paneModifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
          ) {
            Column(
              modifier = Modifier.padding(20.dp),
              verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
              OutlinedTextField(
                value = playerName,
                onValueChange = { playerName = it.take(24) },
                label = { Text(stringResource(R.string.player_sel_name_label)) },
                singleLine = true,
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("player_name_input"),
              )

              Text(
                text = stringResource(R.string.player_sel_count_label),
                style = MaterialTheme.typography.titleMedium,
              )
              Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(2, 3, 4).forEach { count ->
                  val label = "${count.toBengaliDigits(uiState.useBengaliNumerals)} জন"
                  FilterChip(
                    selected = playerCount == count,
                    onClick = { playerCount = count },
                    label = { Text(label) },
                    modifier = Modifier.testTag("player_count_$count"),
                  )
                }
              }

              Text(
                text = stringResource(R.string.player_sel_difficulty_label),
                style = MaterialTheme.typography.titleMedium,
              )
              Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AiDifficulty.entries.forEach { diff ->
                  val labelRes = when (diff) {
                    AiDifficulty.EASY -> R.string.ai_diff_easy
                    AiDifficulty.MEDIUM -> R.string.ai_diff_medium
                    AiDifficulty.HARD -> R.string.ai_diff_hard
                  }
                  FilterChip(
                    selected = selectedDifficulty == diff,
                    onClick = { selectedDifficultyName = diff.name },
                    label = { Text(stringResource(labelRes)) },
                    modifier = Modifier.testTag("ai_diff_${diff.name.lowercase()}"),
                  )
                }
              }

              Text(
                text = stringResource(R.string.player_sel_avatar_label),
                style = MaterialTheme.typography.titleMedium,
              )
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                RongmohalAvatar.entries.forEach { avatar ->
                  PlayerAvatarBadge(
                    avatar = avatar,
                    size = 56.dp,
                    isSelected = uiState.selectedAvatarId == avatar.id,
                    showLabel = true,
                    onClick = { viewModel.selectAvatar(avatar.id) },
                    modifier = Modifier.testTag("avatar_option_${avatar.id}"),
                  )
                }
              }
            }
          }
        },
        secondaryPane = { paneModifier ->
          Card(
            modifier = paneModifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
          ) {
            Column(
              modifier = Modifier.padding(20.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Text(
                text = stringResource(R.string.player_sel_opponents_label),
                style = MaterialTheme.typography.titleLarge,
              )
              SeatPreviewRow(
                name = playerName.ifBlank { uiState.defaultName },
                roleLabel = "আপনি (দক্ষিণ আসন)",
                avatar = RongmohalAvatar.byId(uiState.selectedAvatarId),
              )
              val opponentSeats = listOf("উত্তর আসন", "পশ্চিম আসন", "পূর্ব আসন")
              previewOpponents.forEachIndexed { idx, aiProfile ->
                val diffLabel = when (selectedDifficulty) {
                  AiDifficulty.EASY -> stringResource(R.string.ai_diff_easy)
                  AiDifficulty.MEDIUM -> stringResource(R.string.ai_diff_medium)
                  AiDifficulty.HARD -> stringResource(R.string.ai_diff_hard)
                }
                SeatPreviewRow(
                  name = "${aiProfile.bengaliName} (${aiProfile.personaTitleBengali})",
                  roleLabel = "${opponentSeats[idx]} • দক্ষতা: $diffLabel",
                  avatar = RongmohalAvatar.byId(aiProfile.preferredAvatarId),
                )
              }
            }
          }
        },
      )

      Spacer(modifier = Modifier.height(6.dp))

      RongmohalPrimaryButton(
        text = stringResource(R.string.player_sel_start_match),
        icon = Icons.Filled.PlayArrow,
        onClick = {
          onStartGame(
            playerCount,
            playerName.trim().ifBlank { uiState.defaultName },
            uiState.selectedAvatarId,
            selectedDifficulty,
          )
        },
        testTag = "start_match_button",
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}

@Composable
private fun SeatPreviewRow(
  name: String,
  roleLabel: String,
  avatar: RongmohalAvatar,
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainer,
    shape = MaterialTheme.shapes.medium,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      PlayerAvatarBadge(avatar = avatar, size = 44.dp)
      Column {
        Text(
          text = name,
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = roleLabel,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}
