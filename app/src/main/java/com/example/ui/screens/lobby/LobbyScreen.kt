package com.example.ui.screens.lobby

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.config.UiState
import com.example.core.config.toBengaliDigits
import com.example.domain.game.DeckType
import com.example.domain.model.LeaderboardEntry
import com.example.domain.model.OnlineRoomSummary
import com.example.ui.auth.onGoogleSignInClicked
import com.example.ui.components.PlayerAvatarBadge
import com.example.ui.components.RongmohalAvatar
import com.example.ui.components.TwoPaneAdaptiveLayout
import com.example.ui.theme.GoldTrimAccent

@Composable
fun LobbyScreen(
  onEnterRoom: (String) -> Unit = {},
  onPlayOffline: () -> Unit = {},
  modifier: Modifier = Modifier,
  viewModel: LobbyViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val actionError by viewModel.actionError.collectAsStateWithLifecycle()
  val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()
  val navigateToRoomId by viewModel.navigateToRoomId.collectAsStateWithLifecycle()

  LaunchedEffect(navigateToRoomId) {
    val target = navigateToRoomId ?: return@LaunchedEffect
    viewModel.consumeNavigation()
    onEnterRoom(target)
  }

  var selectedTab by rememberSaveable { mutableIntStateOf(0) }
  var roomTitleInput by rememberSaveable { mutableStateOf("") }
  var joinCodeInput by rememberSaveable { mutableStateOf("") }
  var maxPlayers by rememberSaveable { mutableIntStateOf(4) }
  var isPrivateRoom by rememberSaveable { mutableStateOf(true) }
  var isSpecialDeck by rememberSaveable { mutableStateOf(false) }

  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  var googleError by remember { mutableStateOf<String?>(null) }

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
        text = stringResource(R.string.lobby_title),
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
      )
      Text(
        text = stringResource(R.string.lobby_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      when (val state = uiState) {
        UiState.Loading -> {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(220.dp),
            contentAlignment = Alignment.Center,
          ) {
            CircularProgressIndicator()
          }
        }
        is UiState.Error -> {
          Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = state.message,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.padding(16.dp),
            )
          }
        }
        is UiState.Success -> {
          val content = state.data

          if (!content.isNetworkConnected) {
            OfflineNoticeBanner(onPlayOffline = onPlayOffline)
          }

          if (!content.isOnlineAccount) {
            GuestUpgradeCard(
              errorText = googleError,
              onGoogleSignIn = {
                googleError = null
                onGoogleSignInClicked(
                  context = context,
                  credentialManager = credentialManager,
                  onAuthSuccess = viewModel::onGuestLinkedWithGoogle,
                  onAuthError = { googleError = it },
                  scope = coroutineScope,
                )
              },
              onPlayOffline = onPlayOffline,
            )
          } else {
            if (actionError != null) {
              Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
              ) {
                Text(
                  text = actionError!!,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSecondaryContainer,
                  modifier = Modifier.padding(12.dp),
                )
              }
            }

            TabRow(selectedTabIndex = selectedTab) {
              Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text(stringResource(R.string.lobby_tab_rooms)) },
                modifier = Modifier.testTag("lobby_tab_rooms"),
              )
              Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text(stringResource(R.string.lobby_tab_leaderboard)) },
                modifier = Modifier.testTag("lobby_tab_leaderboard"),
              )
            }

            if (selectedTab == 0) {
              TwoPaneAdaptiveLayout(
                primaryPane = { paneModifier ->
                  Column(
                    modifier = paneModifier,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                  ) {
                    JoinByCodeCard(
                      codeInput = joinCodeInput,
                      onCodeChange = {
                        joinCodeInput = it.uppercase().take(12)
                        viewModel.clearError()
                      },
                      isSubmitting = isSubmitting,
                      enabled = content.isNetworkConnected,
                      onJoinSubmit = { viewModel.joinByCode(joinCodeInput) },
                    )

                    CreateOnlineRoomCard(
                      roomTitle = roomTitleInput,
                      onRoomTitleChange = {
                        roomTitleInput = it
                        viewModel.clearError()
                      },
                      maxPlayers = maxPlayers,
                      onMaxPlayersChange = { maxPlayers = it },
                      isPrivate = isPrivateRoom,
                      onPrivateChange = { isPrivateRoom = it },
                      isSpecialDeck = isSpecialDeck,
                      onDeckChange = { isSpecialDeck = it },
                      useBengaliDigits = content.useBengaliNumerals,
                      isSubmitting = isSubmitting,
                      enabled = content.isNetworkConnected,
                      onSubmit = {
                        viewModel.createRoom(
                          title = roomTitleInput,
                          maxPlayers = maxPlayers,
                          deckType = if (isSpecialDeck) DeckType.SPECIAL else DeckType.CLASSIC,
                          isPrivate = isPrivateRoom,
                          startingCards = 7,
                        )
                      },
                    )
                  }
                },
                secondaryPane = { paneModifier ->
                  PublicWaitingRoomsPane(
                    rooms = content.rooms,
                    useBengaliNumerals = content.useBengaliNumerals,
                    onJoinRoom = viewModel::joinPublicRoom,
                    modifier = paneModifier,
                  )
                },
              )
            } else {
              LeaderboardPane(
                entries = content.leaderboard,
                currentUid = content.currentUserId,
                useBengaliNumerals = content.useBengaliNumerals,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun OfflineNoticeBanner(onPlayOffline: () -> Unit) {
  Surface(
    color = MaterialTheme.colorScheme.secondaryContainer,
    shape = MaterialTheme.shapes.medium,
    modifier = Modifier
      .fillMaxWidth()
      .testTag("lobby_offline_banner"),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Icon(
        imageVector = Icons.Filled.WifiOff,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSecondaryContainer,
      )
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          text = stringResource(R.string.lobby_offline_banner),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        OutlinedButton(
          onClick = onPlayOffline,
          modifier = Modifier.heightIn(min = 48.dp),
        ) {
          Text(stringResource(R.string.lobby_offline_play_cta))
        }
      }
    }
  }
}

@Composable
private fun GuestUpgradeCard(
  errorText: String?,
  onGoogleSignIn: () -> Unit,
  onPlayOffline: () -> Unit,
) {
  Card(
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(22.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = stringResource(R.string.lobby_guest_gate_title),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = stringResource(R.string.lobby_guest_gate_desc),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      if (errorText != null) {
        Text(
          text = errorText,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.error,
        )
      }
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Button(
          onClick = onGoogleSignIn,
          modifier = Modifier
            .weight(1f)
            .heightIn(min = 48.dp)
            .testTag("lobby_google_signin_button"),
        ) {
          Icon(imageVector = Icons.AutoMirrored.Filled.Login, contentDescription = null)
          Spacer(modifier = Modifier.size(6.dp))
          Text(stringResource(R.string.auth_google_button), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        OutlinedButton(
          onClick = onPlayOffline,
          modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag("lobby_offline_fallback_button"),
        ) {
          Text(stringResource(R.string.lobby_offline_play_cta), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
      }
    }
  }
}

@Composable
private fun JoinByCodeCard(
  codeInput: String,
  onCodeChange: (String) -> Unit,
  isSubmitting: Boolean,
  enabled: Boolean,
  onJoinSubmit: () -> Unit,
) {
  Card(
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(imageVector = Icons.Filled.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
          text = stringResource(R.string.lobby_join_code_title),
          style = MaterialTheme.typography.titleMedium,
        )
      }
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedTextField(
          value = codeInput,
          onValueChange = onCodeChange,
          placeholder = { Text(stringResource(R.string.lobby_join_code_hint)) },
          singleLine = true,
          modifier = Modifier
            .weight(1f)
            .testTag("join_room_code_input"),
        )
        Button(
          onClick = onJoinSubmit,
          enabled = enabled && !isSubmitting && codeInput.trim().length >= 4,
          modifier = Modifier
            .heightIn(min = 52.dp)
            .testTag("join_room_code_button"),
        ) {
          Text(stringResource(R.string.lobby_join_code_button))
        }
      }
    }
  }
}

@Composable
private fun CreateOnlineRoomCard(
  roomTitle: String,
  onRoomTitleChange: (String) -> Unit,
  maxPlayers: Int,
  onMaxPlayersChange: (Int) -> Unit,
  isPrivate: Boolean,
  onPrivateChange: (Boolean) -> Unit,
  isSpecialDeck: Boolean,
  onDeckChange: (Boolean) -> Unit,
  useBengaliDigits: Boolean,
  isSubmitting: Boolean,
  enabled: Boolean,
  onSubmit: () -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = stringResource(R.string.lobby_create_room),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )

      OutlinedTextField(
        value = roomTitle,
        onValueChange = onRoomTitleChange,
        label = { Text(stringResource(R.string.lobby_room_title_label)) },
        placeholder = { Text(stringResource(R.string.lobby_room_title_hint)) },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("room_title_input"),
      )

      Text(
        text = "খেলোয়াড় সংখ্যা (২–৪ জন)",
        style = MaterialTheme.typography.labelLarge,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(2, 3, 4).forEach { count ->
          FilterChip(
            selected = maxPlayers == count,
            onClick = { onMaxPlayersChange(count) },
            label = { Text("${count.toBengaliDigits(useBengaliDigits)} জন") },
            modifier = Modifier
              .heightIn(min = 48.dp)
              .testTag("room_count_$count"),
          )
        }
      }

      Text(
        text = "কার্ড ডেক",
        style = MaterialTheme.typography.labelLarge,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
          selected = !isSpecialDeck,
          onClick = { onDeckChange(false) },
          label = { Text("ধ্রুপদী (Classic)") },
          modifier = Modifier.heightIn(min = 48.dp),
        )
        FilterChip(
          selected = isSpecialDeck,
          onClick = { onDeckChange(true) },
          label = { Text("বিশেষ নকশা (+৩/বর্ম)") },
          modifier = Modifier.heightIn(min = 48.dp),
        )
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.weight(1f),
        ) {
          Icon(
            imageVector = if (isPrivate) Icons.Filled.Lock else Icons.Filled.Public,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = stringResource(R.string.lobby_private_room_toggle),
            style = MaterialTheme.typography.bodyMedium,
          )
        }
        Switch(
          checked = isPrivate,
          onCheckedChange = onPrivateChange,
          modifier = Modifier.testTag("private_room_switch"),
        )
      }

      Button(
        onClick = onSubmit,
        enabled = enabled && !isSubmitting,
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 48.dp)
          .testTag("create_room_button"),
      ) {
        if (isSubmitting) {
          CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            strokeWidth = 2.dp,
          )
        } else {
          Icon(
            imageVector = Icons.Filled.AddCircleOutline,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
          )
          Spacer(modifier = Modifier.size(8.dp))
          Text(
            text = stringResource(R.string.lobby_create_submit),
            style = MaterialTheme.typography.labelLarge,
          )
        }
      }
    }
  }
}

@Composable
private fun PublicWaitingRoomsPane(
  rooms: List<OnlineRoomSummary>,
  useBengaliNumerals: Boolean,
  onJoinRoom: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text(
      text = stringResource(R.string.lobby_public_rooms),
      style = MaterialTheme.typography.titleLarge,
      color = MaterialTheme.colorScheme.onBackground,
    )

    if (rooms.isEmpty()) {
      Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.MeetingRoom,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(36.dp),
          )
          Text(
            text = stringResource(R.string.lobby_empty_rooms),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    } else {
      rooms.forEach { room ->
        Card(
          shape = MaterialTheme.shapes.medium,
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("room_card_${room.roomId}"),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Text(
                text = room.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = "আয়োজক: ${room.hostName} • কোড: ${room.roomCode}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              val deckLabel = if (room.deckType == DeckType.SPECIAL) "বিশেষ নকশা" else "ধ্রুপদী"
              val countText =
                "${room.playerCount.toBengaliDigits(useBengaliNumerals)}/${room.maxPlayers.toBengaliDigits(useBengaliNumerals)} ${stringResource(R.string.lobby_room_players_suffix)} • $deckLabel"
              Text(
                text = countText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
              )
            }

            if (room.playerCount < room.maxPlayers) {
              FilledTonalButton(
                onClick = { onJoinRoom(room.roomId) },
                modifier = Modifier
                  .heightIn(min = 48.dp)
                  .testTag("join_room_${room.roomId}"),
              ) {
                Icon(
                  imageVector = Icons.Filled.GroupAdd,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(text = "যোগ দিন", style = MaterialTheme.typography.labelLarge)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun LeaderboardPane(
  entries: List<LeaderboardEntry>,
  currentUid: String,
  useBengaliNumerals: Boolean,
) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Text(
      text = stringResource(R.string.lobby_leaderboard_title),
      style = MaterialTheme.typography.titleLarge,
    )
    if (entries.isEmpty()) {
      Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.EmojiEvents,
            contentDescription = null,
            tint = GoldTrimAccent,
            modifier = Modifier.size(40.dp),
          )
          Text(
            text = stringResource(R.string.lobby_leaderboard_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    } else {
      entries.forEach { entry ->
        val isMe = entry.uid == currentUid
        Surface(
          color = if (isMe) {
            MaterialTheme.colorScheme.primaryContainer
          } else {
            MaterialTheme.colorScheme.surfaceContainerLow
          },
          shape = MaterialTheme.shapes.medium,
          border = if (entry.rank <= 3) BorderStroke(1.5.dp, GoldTrimAccent) else null,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("leaderboard_row_${entry.uid}"),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.weight(1f),
            ) {
              Text(
                text = "#${entry.rank.toBengaliDigits(useBengaliNumerals)}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (entry.rank == 1) GoldTrimAccent else MaterialTheme.colorScheme.primary,
              )
              PlayerAvatarBadge(
                avatar = RongmohalAvatar.byId(entry.avatarId),
                size = 38.dp,
                isSelected = isMe,
              )
              Column {
                Text(
                  text = if (isMe) "${entry.displayName} (আপনি)" else entry.displayName,
                  style = MaterialTheme.typography.titleSmall,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = "জয়: ${entry.matchesWon.toBengaliDigits(useBengaliNumerals)} / ${entry.matchesPlayed.toBengaliDigits(useBengaliNumerals)} আসর",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            Text(
              text = "${entry.totalPoints.toBengaliDigits(useBengaliNumerals)} পয়েন্ট",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }
    }
  }
}
