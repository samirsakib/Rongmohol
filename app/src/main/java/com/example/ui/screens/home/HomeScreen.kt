package com.example.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.R
import com.example.core.config.UiState
import com.example.core.config.toBengaliDigits
import com.example.domain.model.MatchRecord
import com.example.domain.usecase.HomeOverviewData
import com.example.ui.components.PlayerAvatarBadge
import com.example.ui.components.RongmohalAvatar
import com.example.ui.components.RongmohalBrandLogo
import com.example.ui.components.RongmohalPrimaryButton
import com.example.ui.components.RongmohalSecondaryButton
import com.example.ui.components.TwoPaneAdaptiveLayout

@Composable
fun HomeScreen(
  playerName: String,
  onPlayNow: () -> Unit,
  onOpenLobby: () -> Unit,
  onOpenProfile: () -> Unit,
  onOpenRules: () -> Unit,
  onOpenSettings: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: HomeViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    when (val state = uiState) {
      UiState.Loading -> {
        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
      }
      is UiState.Error -> {
        Text(
          text = state.message,
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.error,
          modifier = Modifier
            .align(Alignment.Center)
            .padding(24.dp),
        )
      }
      is UiState.Success -> {
        MainMenuContent(
          playerName = playerName,
          data = state.data,
          onPlayNow = onPlayNow,
          onOpenLobby = onOpenLobby,
          onOpenProfile = onOpenProfile,
          onOpenRules = onOpenRules,
          onOpenSettings = onOpenSettings,
          onAddSampleMatch = viewModel::addSampleMatchRecord,
          onClearMatches = viewModel::clearMatchHistory,
        )
      }
    }
  }
}

@Composable
private fun MainMenuContent(
  playerName: String,
  data: HomeOverviewData,
  onPlayNow: () -> Unit,
  onOpenLobby: () -> Unit,
  onOpenProfile: () -> Unit,
  onOpenRules: () -> Unit,
  onOpenSettings: () -> Unit,
  onAddSampleMatch: () -> Unit,
  onClearMatches: () -> Unit,
) {
  val useBengaliDigits = data.preferences.useBengaliNumerals
  val selectedAvatar = RongmohalAvatar.byId(data.preferences.selectedAvatarId)

  Column(
    modifier = Modifier
      .fillMaxSize()
      .widthIn(max = 1120.dp)
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 16.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(20.dp),
  ) {
    TwoPaneAdaptiveLayout(
      primaryPane = { paneModifier ->
        Column(
          modifier = paneModifier,
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          MainMenuHeroCard(
            playerName = playerName,
            avatar = selectedAvatar,
            onProfileClick = onOpenProfile,
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            RongmohalPrimaryButton(
              text = stringResource(R.string.menu_play_now),
              icon = Icons.Filled.PlayArrow,
              onClick = onPlayNow,
              testTag = "menu_play_now_button",
              modifier = Modifier.weight(1f),
            )
            RongmohalSecondaryButton(
              text = stringResource(R.string.menu_online_lobby),
              icon = Icons.Filled.Groups,
              onClick = onOpenLobby,
              testTag = "menu_online_lobby_button",
              modifier = Modifier.weight(1f),
            )
          }

          MenuTileCard(
            title = stringResource(R.string.menu_profile),
            description = stringResource(R.string.menu_profile_desc),
            icon = Icons.Filled.Person,
            testTag = "menu_profile_card",
            onClick = onOpenProfile,
          )
          MenuTileCard(
            title = stringResource(R.string.menu_rules),
            description = stringResource(R.string.menu_rules_desc),
            icon = Icons.Filled.MenuBook,
            testTag = "menu_rules_card",
            onClick = onOpenRules,
          )
          MenuTileCard(
            title = stringResource(R.string.menu_settings),
            description = stringResource(R.string.settings_subtitle),
            icon = Icons.Filled.Settings,
            testTag = "menu_settings_card",
            onClick = onOpenSettings,
          )
        }
      },
      secondaryPane = { paneModifier ->
        LocalStatsSection(
          totalMatches = data.summary.totalMatches,
          totalWins = data.summary.totalWins,
          recentMatches = data.recentMatches,
          useBengaliDigits = useBengaliDigits,
          onAddSampleMatch = onAddSampleMatch,
          onClearMatches = onClearMatches,
          modifier = paneModifier,
        )
      },
    )
    Spacer(modifier = Modifier.height(12.dp))
  }
}

@Composable
private fun MainMenuHeroCard(
  playerName: String,
  avatar: RongmohalAvatar,
  onProfileClick: () -> Unit,
) {
  ElevatedCard(
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.elevatedCardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column {
      Image(
        painter = painterResource(id = R.drawable.img_lobby_hero),
        contentDescription = stringResource(R.string.home_hero_desc),
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .fillMaxWidth()
          .height(165.dp)
          .clip(MaterialTheme.shapes.large),
      )
      Column(
        modifier = Modifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          RongmohalBrandLogo(
            emblemSize = 52.dp,
            showSubtitle = true,
            horizontalLockup = true,
            modifier = Modifier.weight(1f),
          )
          AssistChip(
            onClick = {},
            label = {
              Text(
                text = stringResource(R.string.app_edition_badge),
                style = MaterialTheme.typography.labelMedium,
              )
            },
          )
        }

        Surface(
          onClick = onProfileClick,
          color = MaterialTheme.colorScheme.surfaceContainer,
          shape = MaterialTheme.shapes.medium,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_profile_pill"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            PlayerAvatarBadge(avatar = avatar, size = 42.dp)
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "${stringResource(R.string.home_greeting_prefix)} $playerName",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = "অবতার: ${avatar.bengaliTitle}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForward,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun MenuTileCard(
  title: String,
  description: String,
  icon: ImageVector,
  testTag: String,
  onClick: () -> Unit,
) {
  Card(
    onClick = onClick,
    shape = MaterialTheme.shapes.medium,
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ),
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = 68.dp)
      .testTag(testTag),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.size(46.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
          )
        }
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = description,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
      )
    }
  }
}

@Composable
private fun LocalStatsSection(
  totalMatches: Int,
  totalWins: Int,
  recentMatches: List<MatchRecord>,
  useBengaliDigits: Boolean,
  onAddSampleMatch: () -> Unit,
  onClearMatches: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text(
        text = stringResource(R.string.home_local_stats_title),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        StatMetricBox(
          label = stringResource(R.string.home_stats_matches),
          value = totalMatches.toBengaliDigits(useBengaliDigits),
          modifier = Modifier.weight(1f),
        )
        StatMetricBox(
          label = stringResource(R.string.home_stats_wins),
          value = totalWins.toBengaliDigits(useBengaliDigits),
          modifier = Modifier.weight(1f),
        )
      }

      if (BuildConfig.DEBUG || recentMatches.isNotEmpty()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          if (BuildConfig.DEBUG) {
            FilledTonalButton(
              onClick = onAddSampleMatch,
              contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
              modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .testTag("seed_sample_match_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.size(6.dp))
              Text(
                text = stringResource(R.string.home_seed_sample_match),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
          if (recentMatches.isNotEmpty()) {
            OutlinedButton(
              onClick = onClearMatches,
              modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag("clear_matches_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.DeleteOutline,
                contentDescription = stringResource(R.string.home_clear_matches),
              )
            }
          }
        }
      }

      if (recentMatches.isEmpty()) {
        Text(
          text = stringResource(R.string.home_stats_empty),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          recentMatches.take(5).forEach { record ->
            MatchRecordRow(record = record, useBengaliDigits = useBengaliDigits)
          }
        }
      }
    }
  }
}

@Composable
private fun StatMetricBox(
  label: String,
  value: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    color = MaterialTheme.colorScheme.surfaceContainer,
    shape = MaterialTheme.shapes.medium,
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        text = value,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.primary,
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun MatchRecordRow(
  record: MatchRecord,
  useBengaliDigits: Boolean,
) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = MaterialTheme.shapes.small,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = record.modeName,
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = record.opponentSummary,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      val scoreLabel =
        "${record.playerScore.toBengaliDigits(useBengaliDigits)} - ${record.opponentScore.toBengaliDigits(useBengaliDigits)}"
      val badgeText = if (record.isVictory) "জয় ($scoreLabel)" else "পরাজয় ($scoreLabel)"
      Text(
        text = badgeText,
        style = MaterialTheme.typography.labelMedium,
        color = if (record.isVictory) {
          MaterialTheme.colorScheme.primary
        } else {
          MaterialTheme.colorScheme.secondary
        },
      )
    }
  }
}
