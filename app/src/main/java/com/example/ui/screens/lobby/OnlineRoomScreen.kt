package com.example.ui.screens.lobby

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.config.UiState
import com.example.core.config.toBengaliDigits
import com.example.domain.game.DeckType
import com.example.domain.model.OnlineMatchResultItem
import com.example.domain.model.RoomStatus
import com.example.ui.components.AnimatedDealEntryCard
import com.example.ui.components.AnimatedDiscardCardStage
import com.example.ui.components.AnimatedDrawPileDeck
import com.example.ui.components.AnimatedPlayableCardLift
import com.example.ui.components.BoardSpecialEffectOverlay
import com.example.ui.components.ConfettiCelebrationOverlay
import com.example.ui.components.PlayerAvatarBadge
import com.example.ui.components.RongmohalAvatar
import com.example.ui.components.RongmohalGameCard
import com.example.ui.components.RongmohalPrimaryButton
import com.example.ui.components.RongmohalSuit
import com.example.ui.components.TableTextureBackground
import com.example.ui.theme.GoldTrimAccent
import com.example.ui.theme.LocalRongmohalTokens

@Composable
fun OnlineRoomScreen(
  roomId: String,
  onNavigateBackToLobby: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: OnlineRoomViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  LaunchedEffect(roomId) {
    viewModel.bindRoom(roomId)
  }

  BackHandler {
    viewModel.leaveRoom(onNavigateBackToLobby)
  }

  TableTextureBackground(modifier = modifier.fillMaxSize()) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .safeDrawingPadding(),
      contentAlignment = Alignment.Center,
    ) {
      when (val state = uiState) {
        UiState.Loading -> CircularProgressIndicator(color = GoldTrimAccent)
        is UiState.Error -> {
          Card(
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
              .padding(24.dp)
              .widthIn(max = 420.dp),
          ) {
            Column(
              modifier = Modifier.padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
              Text(text = state.message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
              Button(
                onClick = onNavigateBackToLobby,
                modifier = Modifier.heightIn(min = 48.dp),
              ) {
                Text(stringResource(R.string.online_board_back_lobby))
              }
            }
          }
        }
        is UiState.Success -> {
          val data = state.data
          val meta = data.meta
          when (meta?.status) {
            RoomStatus.WAITING -> WaitingRoomContent(
              data = data,
              onToggleReady = viewModel::toggleReady,
              onStartMatch = viewModel::startMatch,
              onLeaveRoom = { viewModel.leaveRoom(onNavigateBackToLobby) },
            )
            RoomStatus.PLAYING -> OnlineGameBoardContent(
              data = data,
              onCardClick = viewModel::onCardClicked,
              onDrawClick = viewModel::onDrawCardClicked,
              onDeclareLastCardClick = viewModel::declareLastCard,
              onLeaveMatch = { viewModel.leaveRoom(onNavigateBackToLobby) },
              onPickWildSuit = viewModel::onWildSuitSelected,
              onDismissWild = viewModel::dismissWildChooser,
              onSelectSwapTarget = viewModel::onHandSwapTargetSelected,
              onDismissSwap = viewModel::dismissHandSwapChooser,
            )
            RoomStatus.FINISHED -> OnlineMatchResultsContent(
              data = data,
              onBackToLobby = onNavigateBackToLobby,
            )
            null -> CircularProgressIndicator(color = GoldTrimAccent)
          }
        }
      }
    }
  }
}

@Composable
private fun WaitingRoomContent(
  data: OnlineRoomScreenUiState,
  onToggleReady: () -> Unit,
  onStartMatch: () -> Unit,
  onLeaveRoom: () -> Unit,
) {
  val meta = data.meta ?: return
  val clipboard = LocalClipboardManager.current
  var copied by remember { mutableStateOf(false) }

  Card(
    shape = MaterialTheme.shapes.extraLarge,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    modifier = Modifier
      .padding(16.dp)
      .widthIn(max = 680.dp)
      .fillMaxWidth()
      .verticalScroll(rememberScrollState()),
  ) {
    Column(
      modifier = Modifier.padding(22.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(text = meta.title, style = MaterialTheme.typography.headlineSmall)
          val deckLabel = if (meta.deckType == DeckType.SPECIAL) "বিশেষ নকশা ডেক" else "ধ্রুপদী ডেক"
          val privacyLabel = if (meta.isPrivate) stringResource(R.string.online_room_private_badge) else stringResource(R.string.online_room_public_badge)
          Text(
            text = "$privacyLabel • $deckLabel • ${meta.maxPlayers.toBengaliDigits(data.useBengaliNumerals)} আসন",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        IconButton(
          onClick = onLeaveRoom,
          modifier = Modifier
            .size(48.dp)
            .testTag("waiting_leave_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
            contentDescription = stringResource(R.string.online_room_leave_cta),
          )
        }
      }

      // Prominent Room Code Card
      Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.5.dp, GoldTrimAccent),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column {
            Text(
              text = stringResource(R.string.online_room_code_label),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
              text = meta.roomCode,
              style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.testTag("waiting_room_code"),
            )
          }
          OutlinedButton(
            onClick = {
              clipboard.setText(AnnotatedString(meta.roomCode))
              copied = true
            },
            modifier = Modifier.heightIn(min = 48.dp),
          ) {
            Icon(imageVector = Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (copied) stringResource(R.string.online_room_copied) else stringResource(R.string.online_room_copy_code))
          }
        }
      }

      if (!data.isSocketConnected) {
        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(imageVector = Icons.Filled.WifiOff, contentDescription = null)
            Text(text = stringResource(R.string.online_board_reconnecting), style = MaterialTheme.typography.bodySmall)
          }
        }
      }

      if (data.errorMessage != null) {
        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
          Text(
            text = data.errorMessage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(10.dp),
          )
        }
      }

      Text(
        text = "আসরের খেলোয়াড়গণ (${data.players.size.toBengaliDigits(data.useBengaliNumerals)}/${meta.maxPlayers.toBengaliDigits(data.useBengaliNumerals)})",
        style = MaterialTheme.typography.titleMedium,
      )

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        data.players.forEach { player ->
          val isPlayerHost = player.uid == meta.hostUid
          val isReadyOrHost = isPlayerHost || player.ready
          Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("waiting_player_${player.uid}"),
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
              ) {
                PlayerAvatarBadge(
                  avatar = RongmohalAvatar.byId(player.avatarId),
                  size = 42.dp,
                  isSelected = isReadyOrHost,
                )
                Column {
                  Text(
                    text = if (player.uid == data.myUid) "${player.displayName} (আপনি)" else player.displayName,
                    style = MaterialTheme.typography.titleSmall,
                  )
                  Text(
                    text = if (player.connected) stringResource(R.string.online_room_connected) else stringResource(R.string.online_room_disconnected),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (player.connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                  )
                }
              }

              Surface(
                color = if (isReadyOrHost) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small,
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Icon(
                    imageVector = if (isReadyOrHost) Icons.Filled.CheckCircle else Icons.Filled.HourglassTop,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (isReadyOrHost) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Text(
                    text = when {
                      isPlayerHost -> stringResource(R.string.online_room_host_badge)
                      player.ready -> stringResource(R.string.online_room_ready_badge)
                      else -> stringResource(R.string.online_room_waiting_badge)
                    },
                    style = MaterialTheme.typography.labelMedium,
                  )
                }
              }
            }
          }
        }
      }

      if (data.isHost) {
        if (!data.canHostStartMatch) {
          Text(
            text = stringResource(R.string.online_room_need_players),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Button(
          onClick = onStartMatch,
          enabled = data.canHostStartMatch && !data.actionBusy,
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .testTag("start_online_match_button"),
        ) {
          Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text(stringResource(R.string.online_room_start_cta), style = MaterialTheme.typography.labelLarge)
        }
      } else {
        Button(
          onClick = onToggleReady,
          enabled = !data.actionBusy,
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .testTag("toggle_ready_button"),
        ) {
          Text(
            text = if (data.myReady) stringResource(R.string.online_room_unready_cta) else stringResource(R.string.online_room_ready_cta),
            style = MaterialTheme.typography.labelLarge,
          )
        }
      }
    }
  }
}

@Composable
private fun OnlineGameBoardContent(
  data: OnlineRoomScreenUiState,
  onCardClick: (com.example.domain.game.GameCard) -> Unit,
  onDrawClick: () -> Unit,
  onDeclareLastCardClick: () -> Unit,
  onLeaveMatch: () -> Unit,
  onPickWildSuit: (RongmohalSuit) -> Unit,
  onDismissWild: () -> Unit,
  onSelectSwapTarget: (String) -> Unit,
  onDismissSwap: () -> Unit,
) {
  val tokens = LocalRongmohalTokens.current
  val suitColor by animateColorAsState(
    targetValue = data.activeSuit.primaryColor(tokens.suitPalette),
    animationSpec = tween(260),
    label = "online_active_suit",
  )

  if (data.pendingSwapCard != null) {
    AlertDialog(
      onDismissRequest = onDismissSwap,
      title = { Text(stringResource(R.string.board_pick_swap_target), style = MaterialTheme.typography.titleLarge) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          data.opponents.forEach { opp ->
            Surface(
              color = MaterialTheme.colorScheme.surfaceContainer,
              shape = MaterialTheme.shapes.medium,
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clickable { onSelectSwapTarget(opp.uid) },
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  PlayerAvatarBadge(avatar = opp.avatar, size = 36.dp)
                  Text(text = opp.name, style = MaterialTheme.typography.titleSmall)
                }
                Text(
                  text = "${opp.cardCount.toBengaliDigits(data.useBengaliNumerals)} ${stringResource(R.string.board_cards_left_suffix)}",
                  style = MaterialTheme.typography.labelMedium,
                )
              }
            }
          }
        }
      },
      confirmButton = { TextButton(onClick = onDismissSwap) { Text("বাতিল") } },
    )
  }

  if (data.pendingWildCard != null) {
    val suits = listOf(RongmohalSuit.SINDOOR, RongmohalSuit.PANNA, RongmohalSuit.NEELKANTHA, RongmohalSuit.SHONALI)
    AlertDialog(
      onDismissRequest = onDismissWild,
      title = { Text(stringResource(R.string.board_pick_wild_color), style = MaterialTheme.typography.titleLarge) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          suits.chunked(2).forEach { rowSuits ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              rowSuits.forEach { suit ->
                Button(
                  onClick = { onPickWildSuit(suit) },
                  colors = ButtonDefaults.buttonColors(
                    containerColor = suit.primaryColor(tokens.suitPalette),
                    contentColor = Color.White,
                  ),
                  modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
                ) {
                  Text(text = suit.bengaliName, style = MaterialTheme.typography.labelLarge)
                }
              }
            }
          }
        }
      },
      confirmButton = { TextButton(onClick = onDismissWild) { Text("বাতিল") } },
    )
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val isCompact = maxWidth < 380.dp || maxHeight < 520.dp
    val isWide = maxWidth >= 600.dp || maxWidth > maxHeight
    val cardWidth = when {
      maxHeight < 520.dp -> 60.dp
      maxWidth >= 840.dp -> 86.dp
      isWide -> 70.dp
      maxWidth < 380.dp -> 62.dp
      else -> 72.dp
    }
    val cardHeight = cardWidth * 1.44f

    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = if (isCompact) 8.dp else 14.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      // Top Control Bar
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
          Surface(
            color = Color.Black.copy(alpha = 0.4f),
            shape = CircleShape,
            border = BorderStroke(1.dp, GoldTrimAccent.copy(alpha = 0.65f)),
          ) {
            IconButton(onClick = onLeaveMatch, modifier = Modifier.size(48.dp)) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = stringResource(R.string.online_room_leave_cta),
                tint = Color.White,
              )
            }
          }
          Column {
            Text(
              text = data.meta?.title ?: "অনলাইন আসর",
              style = MaterialTheme.typography.titleSmall,
              color = Color.White,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "কোড: ${data.meta?.roomCode ?: ""}",
              style = MaterialTheme.typography.labelSmall,
              color = GoldTrimAccent,
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          if (data.pendingDrawPenalty > 0) {
            Surface(
              color = tokens.suitPalette.sindoorPrimary,
              shape = MaterialTheme.shapes.small,
              border = BorderStroke(1.5.dp, GoldTrimAccent),
            ) {
              Text(
                text = "+${data.pendingDrawPenalty.toBengaliDigits(data.useBengaliNumerals)}",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
              )
            }
          }
          Surface(
            color = if (data.turnSecondsLeft <= 5) tokens.suitPalette.sindoorDeep else Color.Black.copy(alpha = 0.42f),
            shape = MaterialTheme.shapes.small,
            border = BorderStroke(1.dp, GoldTrimAccent.copy(alpha = 0.55f)),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(imageVector = Icons.Filled.Timer, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
              Text(
                text = "${data.turnSecondsLeft.toBengaliDigits(data.useBengaliNumerals)}${stringResource(R.string.board_timer_seconds_suffix)}",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
              )
            }
          }
          Surface(color = Color.Black.copy(alpha = 0.42f), shape = MaterialTheme.shapes.small) {
            Icon(
              imageVector = if (data.isClockwise) Icons.Filled.RotateRight else Icons.Filled.RotateLeft,
              contentDescription = null,
              tint = GoldTrimAccent,
              modifier = Modifier.padding(6.dp).size(16.dp),
            )
          }
          Surface(
            color = Color.Black.copy(alpha = 0.45f),
            shape = MaterialTheme.shapes.small,
            border = BorderStroke(1.5.dp, suitColor),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
              Box(modifier = Modifier.size(11.dp).clip(CircleShape).background(suitColor))
              Text(text = data.activeSuit.bengaliName, style = MaterialTheme.typography.labelSmall, color = Color.White)
            }
          }
        }
      }

      LinearProgressIndicator(
        progress = { (data.turnSecondsLeft.toFloat() / data.turnTotalSeconds.coerceAtLeast(1)).coerceIn(0f, 1f) },
        color = if (data.turnSecondsLeft <= 5) tokens.suitPalette.sindoorPrimary else GoldTrimAccent,
        trackColor = Color.White.copy(alpha = 0.14f),
        modifier = Modifier.fillMaxWidth().height(3.dp).clip(CircleShape),
      )

      if (!data.isSocketConnected) {
        Surface(
          color = tokens.suitPalette.sindoorDeep,
          shape = MaterialTheme.shapes.small,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(imageVector = Icons.Filled.WifiOff, contentDescription = null, tint = GoldTrimAccent, modifier = Modifier.size(16.dp))
            Text(text = stringResource(R.string.online_board_reconnecting), style = MaterialTheme.typography.labelMedium, color = Color.White)
          }
        }
      }

      // Opponents Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
      ) {
        data.opponents.forEach { opp ->
          Surface(
            color = Color.Black.copy(alpha = if (opp.isCurrentTurn) 0.62f else 0.36f),
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(if (opp.isCurrentTurn) 2.dp else 1.dp, if (opp.isCurrentTurn) GoldTrimAccent else Color.White.copy(alpha = 0.25f)),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              PlayerAvatarBadge(avatar = opp.avatar, size = 34.dp, isSelected = opp.isCurrentTurn)
              Column {
                Text(text = opp.name, style = MaterialTheme.typography.labelMedium, color = Color.White, maxLines = 1)
                Text(
                  text = "${opp.cardCount.toBengaliDigits(data.useBengaliNumerals)} ${stringResource(R.string.board_cards_left_suffix)}" +
                    if (!opp.connected) " • বিচ্ছিন্ন" else "",
                  style = MaterialTheme.typography.labelSmall,
                  color = if (opp.connected) GoldTrimAccent else Color(0xFFFF8A80),
                  maxLines = 1,
                )
              }
            }
          }
        }
      }

      // Central Draw + Discard Arena
      Column(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(26.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AnimatedDrawPileDeck(
              remainingCards = data.drawPileCount,
              recycleTriggerKey = 0,
              drawPulseKey = data.drawPulseCount,
              cardWidth = cardWidth,
              cardHeight = cardHeight,
              enabled = data.isMyTurn && !data.actionBusy,
              onClick = onDrawClick,
            )
            Text(
              text = "${stringResource(R.string.board_draw_pile)} (${data.drawPileCount.toBengaliDigits(data.useBengaliNumerals)})",
              style = MaterialTheme.typography.labelSmall,
              color = GoldTrimAccent,
            )
          }

          data.topDiscardCard?.let { top ->
            AnimatedDiscardCardStage(
              topCard = top,
              activeSuitColor = suitColor,
              cardWidth = cardWidth + 6.dp,
              cardHeight = cardHeight + 8.dp,
              useBengaliDigits = data.useBengaliNumerals,
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Surface(
          color = Color.Black.copy(alpha = 0.45f),
          shape = MaterialTheme.shapes.small,
          border = BorderStroke(1.dp, GoldTrimAccent.copy(alpha = 0.5f)),
          modifier = Modifier.widthIn(max = 460.dp),
        ) {
          Text(
            text = data.errorMessage ?: data.statusMessage,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
          )
        }
      }

      // Bottom Dock
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          PlayerAvatarBadge(
            avatar = RongmohalAvatar.byId(data.myPlayer?.avatarId ?: 0),
            size = 38.dp,
            isSelected = data.isMyTurn,
          )
          Column {
            Text(
              text = data.myPlayer?.displayName ?: "আপনি",
              style = MaterialTheme.typography.titleSmall,
              color = if (data.isMyTurn) GoldTrimAccent else Color.White,
            )
            Text(
              text = "${data.myHand.size.toBengaliDigits(data.useBengaliNumerals)} ${stringResource(R.string.board_cards_left_suffix)}",
              style = MaterialTheme.typography.labelSmall,
              color = Color.White.copy(alpha = 0.8f),
            )
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(
            onClick = onDeclareLastCardClick,
            enabled = data.canDeclareLastCard && !data.actionBusy,
            colors = ButtonDefaults.buttonColors(
              containerColor = if (data.hasDeclaredLastCard) tokens.suitPalette.pannaPrimary else tokens.suitPalette.sindoorPrimary,
              contentColor = Color.White,
            ),
            modifier = Modifier.heightIn(min = 48.dp),
          ) {
            Icon(imageVector = Icons.Filled.Campaign, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = if (data.hasDeclaredLastCard) stringResource(R.string.board_declared_badge) else stringResource(R.string.board_declare_last_card),
              style = MaterialTheme.typography.labelMedium,
            )
          }

          Button(
            onClick = onDrawClick,
            enabled = data.isMyTurn && !data.actionBusy,
            colors = ButtonDefaults.buttonColors(containerColor = GoldTrimAccent, contentColor = Color(0xFF1A1404)),
            modifier = Modifier.heightIn(min = 48.dp),
          ) {
            Icon(imageVector = Icons.Filled.Layers, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = if (data.pendingDrawPenalty > 0) "+${data.pendingDrawPenalty.toBengaliDigits(data.useBengaliNumerals)} তুলুন" else stringResource(R.string.board_draw_button),
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            )
          }
        }
      }

      LazyRow(
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        itemsIndexed(data.myHand, key = { _, item -> item.domainCard.id }) { idx, item ->
          AnimatedDealEntryCard(indexInHand = idx) {
            AnimatedPlayableCardLift(isPlayable = item.isPlayable) {
              RongmohalGameCard(
                card = item.uiCard,
                cardWidth = cardWidth,
                cardHeight = cardHeight,
                useBengaliDigits = data.useBengaliNumerals,
                isPlayable = item.isPlayable,
                isHighlighted = item.isPlayable,
                onClick = { onCardClick(item.domainCard) },
              )
            }
          }
        }
      }
    }

    BoardSpecialEffectOverlay(
      effect = data.activeVisualEffect,
      modifier = Modifier.fillMaxSize(),
    )
  }
}

@Composable
private fun OnlineMatchResultsContent(
  data: OnlineRoomScreenUiState,
  onBackToLobby: () -> Unit,
) {
  val iWon = data.winnerUid == data.myUid
  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    if (iWon) {
      ConfettiCelebrationOverlay()
    }
    Card(
      shape = MaterialTheme.shapes.extraLarge,
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
      modifier = Modifier
        .padding(20.dp)
        .widthIn(max = 560.dp)
        .fillMaxWidth()
        .verticalScroll(rememberScrollState()),
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Icon(imageVector = Icons.Filled.EmojiEvents, contentDescription = null, tint = GoldTrimAccent, modifier = Modifier.size(48.dp))
        Text(
          text = if (iWon) stringResource(R.string.result_victory_title) else "${data.winnerName} আসর জিতেছেন!",
          style = MaterialTheme.typography.headlineSmall,
          textAlign = TextAlign.Center,
        )
        Text(
          text = stringResource(R.string.online_board_result_title),
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
          data.results.forEachIndexed { idx, row ->
            Surface(
              color = if (row.isWinner) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
              shape = MaterialTheme.shapes.medium,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                  Text(
                    text = "#${(idx + 1).toBengaliDigits(data.useBengaliNumerals)}",
                    style = MaterialTheme.typography.titleMedium,
                  )
                  PlayerAvatarBadge(avatar = RongmohalAvatar.byId(row.avatarId), size = 36.dp)
                  Column {
                    Text(
                      text = if (row.uid == data.myUid) "${row.displayName} (আপনি)" else row.displayName,
                      style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                      text = "বাকি তাস: ${row.cardsRemaining.toBengaliDigits(data.useBengaliNumerals)}",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
                Text(
                  text = "+${row.awardedPoints.toBengaliDigits(data.useBengaliNumerals)} পয়েন্ট",
                  style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.primary,
                )
              }
            }
          }
        }

        RongmohalPrimaryButton(
          text = stringResource(R.string.online_board_back_lobby),
          icon = Icons.Filled.EmojiEvents,
          onClick = onBackToLobby,
          testTag = "online_result_back_lobby",
        )
      }
    }
  }
}
