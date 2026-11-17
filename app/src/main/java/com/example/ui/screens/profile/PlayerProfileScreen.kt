package com.example.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.example.ui.components.PlayerAvatarBadge
import com.example.ui.components.RongmohalAvatar
import com.example.ui.components.TwoPaneAdaptiveLayout

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayerProfileScreen(
  modifier: Modifier = Modifier,
  viewModel: PlayerProfileViewModel = hiltViewModel(),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val activeAvatar = RongmohalAvatar.byId(state.selectedAvatarId)
  val digits = state.useBengaliNumerals
  val user = state.user
  val resolvedDisplayName = if (user != null) {
    state.cloudProfile?.displayName?.ifBlank { null } ?: user.displayName
  } else {
    state.guestDisplayName.ifBlank { "অতিথি খেলোয়াড়" }
  }
  var editableName by rememberSaveable(resolvedDisplayName) { mutableStateOf(resolvedDisplayName) }

  LaunchedEffect(state.cloudSyncMessage) {
    if (state.cloudSyncMessage != null) {
      kotlinx.coroutines.delay(2500L)
      viewModel.clearSyncMessage()
    }
  }

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
        text = stringResource(R.string.profile_title),
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
      )
      Text(
        text = stringResource(R.string.profile_subtitle),
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
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                  PlayerAvatarBadge(avatar = activeAvatar, size = 72.dp, isSelected = true)
                  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                      text = resolvedDisplayName,
                      style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                      text = if (state.user != null) {
                        state.user?.email ?: ""
                      } else {
                        "অতিথি মোড (স্থানীয় সংরক্ষণ)"
                      },
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                      text = "অবতার: ${activeAvatar.bengaliTitle}",
                      style = MaterialTheme.typography.labelLarge,
                      color = MaterialTheme.colorScheme.primary,
                    )
                  }
                }

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  OutlinedTextField(
                    value = editableName,
                    onValueChange = { editableName = it.take(24) },
                    label = { Text("প্রদর্শিত নাম") },
                    singleLine = true,
                    modifier = Modifier
                      .weight(1f)
                      .testTag("profile_name_input"),
                  )
                  Button(
                    onClick = { viewModel.updateDisplayName(editableName) },
                    enabled = editableName.isNotBlank() && editableName.trim() != resolvedDisplayName,
                    modifier = Modifier.testTag("profile_save_name_button"),
                  ) {
                    Icon(Icons.Filled.Check, contentDescription = "সংরক্ষণ")
                  }
                }

                state.cloudSyncMessage?.let { msg ->
                  Text(
                    text = msg,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
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
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Text(
                  text = stringResource(R.string.profile_choose_avatar),
                  style = MaterialTheme.typography.titleLarge,
                )
                FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(14.dp),
                  verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  RongmohalAvatar.entries.forEach { avatar ->
                    PlayerAvatarBadge(
                      avatar = avatar,
                      size = 56.dp,
                      isSelected = avatar.id == state.selectedAvatarId,
                      showLabel = true,
                      onClick = { viewModel.selectAvatar(avatar.id) },
                      modifier = Modifier.testTag("profile_avatar_${avatar.id}"),
                    )
                  }
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
                  text = stringResource(R.string.profile_cloud_stats),
                  style = MaterialTheme.typography.titleLarge,
                )
                val totalMatches = state.localSummary.totalMatches
                val totalWins = state.localSummary.totalWins
                val winRate = if (totalMatches > 0) (totalWins * 100) / totalMatches else 0
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  ProfileStatChip(
                    label = stringResource(R.string.home_stats_matches),
                    value = totalMatches.toBengaliDigits(digits),
                    modifier = Modifier.weight(1f),
                  )
                  ProfileStatChip(
                    label = stringResource(R.string.home_stats_wins),
                    value = totalWins.toBengaliDigits(digits),
                    modifier = Modifier.weight(1f),
                  )
                  ProfileStatChip(
                    label = stringResource(R.string.profile_win_rate),
                    value = "${winRate.toBengaliDigits(digits)}%",
                    modifier = Modifier.weight(1f),
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
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Text(
                  text = stringResource(R.string.profile_local_history),
                  style = MaterialTheme.typography.titleLarge,
                )
                if (state.recentMatches.isEmpty()) {
                  Text(
                    text = stringResource(R.string.home_stats_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                } else {
                  state.recentMatches.forEach { match ->
                    Surface(
                      color = MaterialTheme.colorScheme.surfaceContainer,
                      shape = MaterialTheme.shapes.small,
                      modifier = Modifier.fillMaxWidth(),
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                      ) {
                        Column {
                          Text(text = match.modeName, style = MaterialTheme.typography.titleSmall)
                          Text(
                            text = match.opponentSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                          )
                        }
                        Text(
                          text = if (match.isVictory) "বিজয়ী" else "পরাজিত",
                          style = MaterialTheme.typography.labelLarge,
                          color = if (match.isVictory) {
                            MaterialTheme.colorScheme.primary
                          } else {
                            MaterialTheme.colorScheme.secondary
                          },
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        },
      )
    }
  }
}

@Composable
private fun ProfileStatChip(
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
      modifier = Modifier.padding(12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
        text = value,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.primary,
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
