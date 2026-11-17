package com.example.ui.screens.board

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.config.toBengaliDigits
import com.example.domain.ai.AiDifficulty
import com.example.ui.components.AnimatedDealEntryCard
import com.example.ui.components.AnimatedDiscardCardStage
import com.example.ui.components.AnimatedDrawPileDeck
import com.example.ui.components.AnimatedPlayableCardLift
import com.example.ui.components.BoardSpecialEffectOverlay
import com.example.ui.components.PlayerAvatarBadge
import com.example.ui.components.RongmohalGameCard
import com.example.ui.components.RongmohalSuit
import com.example.ui.components.TableTextureBackground
import com.example.ui.components.WinnerCelebrationOverlay
import com.example.ui.screens.mode.GameModeId
import com.example.ui.theme.GoldTrimAccent
import com.example.ui.theme.LocalRongmohalTokens

@Composable
fun GameBoardScreen(
  mode: GameModeId,
  playerCount: Int,
  playerName: String,
  avatarId: Int,
  onNavigateBack: () -> Unit,
  onMatchFinished: (isVictory: Boolean, playerScore: Int, opponentScore: Int) -> Unit,
  modifier: Modifier = Modifier,
  difficulty: AiDifficulty = AiDifficulty.MEDIUM,
  viewModel: GameBoardViewModel = hiltViewModel(),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val tokens = LocalRongmohalTokens.current

  BackHandler(enabled = !state.matchFinished) {
    viewModel.togglePause()
  }

  LaunchedEffect(mode, playerCount, playerName, avatarId, difficulty) {
    viewModel.initMatch(
      mode = mode,
      playerCount = playerCount,
      playerName = playerName,
      avatarId = avatarId,
      difficulty = difficulty,
    )
  }

  if (state.pendingSwapCard != null) {
    HandSwapTargetDialog(
      opponents = state.opponents,
      useBengaliDigits = state.useBengaliNumerals,
      onSelectOpponent = viewModel::onHandSwapTargetSelected,
      onDismiss = viewModel::dismissHandSwapChooser,
    )
  }

  if (state.pendingWildCard != null) {
    WildColorPickerDialog(
      onPickSuit = viewModel::onWildSuitSelected,
      onDismiss = viewModel::dismissWildChooser,
    )
  }

  if (state.mysteryOutcomeText != null) {
    MysteryOutcomeDialog(
      message = state.mysteryOutcomeText!!,
      onDismiss = viewModel::dismissMysteryModal,
    )
  }

  if (state.isPaused && !state.matchFinished) {
    GamePauseDialog(
      onResume = viewModel::togglePause,
      onRestart = viewModel::restartCurrentMatch,
      onExitToMenu = onNavigateBack,
    )
  }

  TableTextureBackground(modifier = modifier.fillMaxSize()) {
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxSize()
        .safeDrawingPadding(),
    ) {
      val isWideOrLandscape = maxWidth >= 600.dp || maxWidth > maxHeight
      val isCompactHeight = maxHeight < 520.dp
      val isSmallPhone = maxWidth < 380.dp

      val cardWidth: Dp = when {
        isCompactHeight -> 60.dp
        maxWidth >= 840.dp -> 88.dp
        isWideOrLandscape -> 72.dp
        isSmallPhone -> 64.dp
        else -> 74.dp
      }
      val cardHeight: Dp = (cardWidth * 1.44f)

      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(
            horizontal = if (isSmallPhone) 8.dp else 14.dp,
            vertical = if (isCompactHeight) 6.dp else 10.dp,
          ),
        verticalArrangement = Arrangement.spacedBy(if (isCompactHeight) 6.dp else 10.dp),
      ) {
        // 1. Top Bar: Pause Button, Title, Timer, Active Color & Direction Indicator
        BoardTopControlBar(
          mode = mode,
          activeSuit = state.activeSuit,
          isClockwise = state.isClockwise,
          turnSecondsLeft = state.turnTimerSeconds,
          turnTotalSeconds = state.turnTimerTotalSeconds,
          pendingPenalty = state.pendingDrawPenalty,
          useBengaliDigits = state.useBengaliNumerals,
          isCompact = isSmallPhone || isCompactHeight,
          onPauseClick = viewModel::togglePause,
        )

        // 2. Adaptive Table Arena (Opponents + Center Piles)
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
        ) {
          if (isWideOrLandscape && state.opponents.size == 3) {
            // 4-Player Wide/Landscape Arena: Top (North), Left (West), Right (East) around Center Table
            Row(
              modifier = Modifier.fillMaxSize(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              // West Opponent
              Box(
                modifier = Modifier.widthIn(min = 128.dp, max = 180.dp),
                contentAlignment = Alignment.CenterStart,
              ) {
                OpponentSeatCard(
                  opponent = state.opponents[1],
                  isActiveTurn = state.currentTurnIndex == 2,
                  useBengaliDigits = state.useBengaliNumerals,
                  compact = isCompactHeight,
                )
              }

              // Center Column: North Opponent + Center Piles
              Column(
                modifier = Modifier
                  .weight(1f)
                  .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly,
              ) {
                OpponentSeatCard(
                  opponent = state.opponents[0],
                  isActiveTurn = state.currentTurnIndex == 1,
                  useBengaliDigits = state.useBengaliNumerals,
                  compact = isCompactHeight,
                )

                CenterPilesArena(
                  state = state,
                  activeSuitColor = state.activeSuit.primaryColor(tokens.suitPalette),
                  cardWidth = cardWidth,
                  cardHeight = cardHeight,
                  compact = isCompactHeight,
                  onDrawClick = viewModel::onDrawCardClicked,
                )
              }

              // East Opponent
              Box(
                modifier = Modifier.widthIn(min = 128.dp, max = 180.dp),
                contentAlignment = Alignment.CenterEnd,
              ) {
                OpponentSeatCard(
                  opponent = state.opponents[2],
                  isActiveTurn = state.currentTurnIndex == 3,
                  useBengaliDigits = state.useBengaliNumerals,
                  compact = isCompactHeight,
                )
              }
            }
          } else {
            // Portrait Phone / 2-3 Player Arena
            Column(
              modifier = Modifier.fillMaxSize(),
              verticalArrangement = Arrangement.SpaceBetween,
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                state.opponents.forEachIndexed { idx, opp ->
                  OpponentSeatCard(
                    opponent = opp,
                    isActiveTurn = state.currentTurnIndex == idx + 1,
                    useBengaliDigits = state.useBengaliNumerals,
                    compact = isSmallPhone || isCompactHeight,
                  )
                }
              }

              CenterPilesArena(
                state = state,
                activeSuitColor = state.activeSuit.primaryColor(tokens.suitPalette),
                cardWidth = cardWidth,
                cardHeight = cardHeight,
                compact = isCompactHeight || isSmallPhone,
                onDrawClick = viewModel::onDrawCardClicked,
              )
            }
          }
        }

        // 3. Bottom Human Controls + Scrollable Card Hand
        BottomPlayerDock(
          state = state,
          cardWidth = cardWidth,
          cardHeight = cardHeight,
          isCompact = isSmallPhone || isCompactHeight,
          onDrawClick = viewModel::onDrawCardClicked,
          onDeclareLastCardClick = viewModel::declareLastCard,
          onCardClick = viewModel::onCardClicked,
        )
      }

      // 4. Special Effects Layer (Shuffle, Exchange, Attack, Shield)
      BoardSpecialEffectOverlay(
        effect = state.activeVisualEffect,
        modifier = Modifier.fillMaxSize(),
      )

      // 5. Winner Celebration + Confetti Overlay
      if (state.matchFinished) {
        WinnerCelebrationOverlay(
          winnerName = state.winnerName,
          winnerAvatar = state.winnerAvatar,
          isHumanWinner = state.playerWon,
          ctaLabel = stringResource(R.string.board_winner_banner_action),
          onContinueToResults = {
            onMatchFinished(
              state.playerWon,
              state.finalPlayerScore,
              state.finalOpponentScore,
            )
          },
          modifier = Modifier.testTag("winner_celebration_overlay"),
        )
      }
    }
  }
}

@Composable
private fun BoardTopControlBar(
  mode: GameModeId,
  activeSuit: RongmohalSuit,
  isClockwise: Boolean,
  turnSecondsLeft: Int,
  turnTotalSeconds: Int,
  pendingPenalty: Int,
  useBengaliDigits: Boolean,
  isCompact: Boolean,
  onPauseClick: () -> Unit,
) {
  val tokens = LocalRongmohalTokens.current
  val suitColor by animateColorAsState(
    targetValue = activeSuit.primaryColor(tokens.suitPalette),
    animationSpec = tween(260),
    label = "active_suit_color",
  )
  val dirRotation by animateFloatAsState(
    targetValue = if (isClockwise) 0f else 180f,
    animationSpec = tween(300),
    label = "direction_spin",
  )
  val isUrgentTimer = turnSecondsLeft <= 5

  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.weight(1f),
      ) {
        Surface(
          color = Color.Black.copy(alpha = 0.38f),
          shape = CircleShape,
          border = BorderStroke(1.dp, GoldTrimAccent.copy(alpha = 0.65f)),
        ) {
          IconButton(
            onClick = onPauseClick,
            modifier = Modifier
              .size(48.dp)
              .testTag("board_pause_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.Pause,
              contentDescription = stringResource(R.string.board_pause_action),
              tint = Color.White,
            )
          }
        }
        Column(modifier = Modifier.padding(end = 6.dp)) {
          Text(
            text = stringResource(R.string.board_title),
            style = if (isCompact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = stringResource(mode.titleRes),
            style = MaterialTheme.typography.labelSmall,
            color = GoldTrimAccent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        if (pendingPenalty > 0) {
          Surface(
            color = tokens.suitPalette.sindoorPrimary,
            shape = MaterialTheme.shapes.small,
            border = BorderStroke(1.5.dp, GoldTrimAccent),
            modifier = Modifier.testTag("board_penalty_badge"),
          ) {
            Text(
              text = "+${pendingPenalty.toBengaliDigits(useBengaliDigits)}",
              style = MaterialTheme.typography.labelLarge,
              color = Color.White,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            )
          }
        }

        // Turn Timer Pill
        Surface(
          color = if (isUrgentTimer) tokens.suitPalette.sindoorDeep else Color.Black.copy(alpha = 0.42f),
          shape = MaterialTheme.shapes.small,
          border = BorderStroke(1.dp, if (isUrgentTimer) GoldTrimAccent else Color.White.copy(alpha = 0.3f)),
          modifier = Modifier.testTag("board_turn_timer"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.Timer,
              contentDescription = null,
              tint = if (isUrgentTimer) GoldTrimAccent else Color.White,
              modifier = Modifier.size(14.dp),
            )
            Text(
              text = "${turnSecondsLeft.toBengaliDigits(useBengaliDigits)}${stringResource(R.string.board_timer_seconds_suffix)}",
              style = MaterialTheme.typography.labelMedium,
              color = Color.White,
              maxLines = 1,
            )
          }
        }

        // Turn Direction Pill
        Surface(
          color = Color.Black.copy(alpha = 0.42f),
          shape = MaterialTheme.shapes.small,
          border = BorderStroke(1.dp, GoldTrimAccent.copy(alpha = 0.5f)),
          modifier = Modifier.testTag("board_direction_indicator"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              imageVector = if (isClockwise) Icons.Filled.RotateRight else Icons.Filled.RotateLeft,
              contentDescription = if (isClockwise) {
                stringResource(R.string.board_direction_clockwise)
              } else {
                stringResource(R.string.board_direction_counter)
              },
              tint = GoldTrimAccent,
              modifier = Modifier
                .size(16.dp)
                .graphicsLayer { rotationZ = dirRotation },
            )
            if (!isCompact) {
              Text(
                text = if (isClockwise) {
                  stringResource(R.string.board_direction_clockwise)
                } else {
                  stringResource(R.string.board_direction_counter)
                },
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                maxLines = 1,
              )
            }
          }
        }

        // Current Active Color Indicator
        Surface(
          color = Color.Black.copy(alpha = 0.45f),
          shape = MaterialTheme.shapes.small,
          border = BorderStroke(1.5.dp, suitColor),
          modifier = Modifier.testTag("board_active_color_pill"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            Box(
              modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(suitColor),
            )
            Text(
              text = activeSuit.bengaliName,
              style = MaterialTheme.typography.labelMedium,
              color = Color.White,
              maxLines = 1,
            )
          }
        }
      }
    }

    LinearProgressIndicator(
      progress = { (turnSecondsLeft.toFloat() / turnTotalSeconds.coerceAtLeast(1)).coerceIn(0f, 1f) },
      color = if (isUrgentTimer) tokens.suitPalette.sindoorPrimary else GoldTrimAccent,
      trackColor = Color.White.copy(alpha = 0.14f),
      modifier = Modifier
        .fillMaxWidth()
        .height(3.dp)
        .clip(CircleShape),
    )
  }
}

@Composable
private fun CenterPilesArena(
  state: GameBoardUiState,
  activeSuitColor: Color,
  cardWidth: Dp,
  cardHeight: Dp,
  compact: Boolean,
  onDrawClick: () -> Unit,
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp),
  ) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(if (compact) 18.dp else 28.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        AnimatedDrawPileDeck(
          remainingCards = state.drawPileCount,
          recycleTriggerKey = state.recyclePulseCount,
          drawPulseKey = state.drawPulseCount,
          cardWidth = cardWidth,
          cardHeight = cardHeight,
          enabled = state.currentTurnIndex == 0 && !state.matchFinished && !state.isPaused,
          onClick = onDrawClick,
          modifier = Modifier.testTag("draw_pile_card"),
        )
        Text(
          text = "${stringResource(R.string.board_draw_pile)} (${state.drawPileCount.toBengaliDigits(state.useBengaliNumerals)})",
          style = MaterialTheme.typography.labelSmall,
          color = GoldTrimAccent,
          maxLines = 1,
        )
      }

      AnimatedDiscardCardStage(
        topCard = state.topDiscardCard,
        activeSuitColor = activeSuitColor,
        cardWidth = cardWidth + 6.dp,
        cardHeight = cardHeight + 8.dp,
        useBengaliDigits = state.useBengaliNumerals,
        modifier = Modifier.testTag("discard_pile_top"),
      )
    }

    Surface(
      color = Color.Black.copy(alpha = 0.45f),
      shape = MaterialTheme.shapes.small,
      border = BorderStroke(1.dp, GoldTrimAccent.copy(alpha = 0.5f)),
      modifier = Modifier.widthIn(max = 460.dp),
    ) {
      Text(
        text = state.statusMessage,
        style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
        color = Color.White,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
      )
    }
  }
}

@Composable
private fun BottomPlayerDock(
  state: GameBoardUiState,
  cardWidth: Dp,
  cardHeight: Dp,
  isCompact: Boolean,
  onDrawClick: () -> Unit,
  onDeclareLastCardClick: () -> Unit,
  onCardClick: (com.example.domain.game.GameCard) -> Unit,
) {
  val isHumanTurn = state.currentTurnIndex == 0 && !state.matchFinished && !state.isPaused
  val tokens = LocalRongmohalTokens.current

  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(if (isCompact) 4.dp else 8.dp),
  ) {
    // Player info + accessible Draw Card button & UNO/Last-Card declaration button
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.weight(1f),
      ) {
        PlayerAvatarBadge(
          avatar = state.playerAvatar,
          size = if (isCompact) 34.dp else 40.dp,
          isSelected = isHumanTurn,
        )
        Column {
          Text(
            text = state.playerName,
            style = MaterialTheme.typography.titleSmall,
            color = if (isHumanTurn) GoldTrimAccent else Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "${state.playerHand.size.toBengaliDigits(state.useBengaliNumerals)} ${stringResource(R.string.board_cards_left_suffix)}",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.82f),
            maxLines = 1,
          )
        }
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        // UNO / Last Card Declaration Button
        val declareActive = state.canDeclareLastCard || state.hasDeclaredLastCard
        Button(
          onClick = onDeclareLastCardClick,
          enabled = state.canDeclareLastCard,
          colors = ButtonDefaults.buttonColors(
            containerColor = if (state.hasDeclaredLastCard) {
              tokens.suitPalette.pannaPrimary
            } else {
              tokens.suitPalette.sindoorPrimary
            },
            contentColor = Color.White,
            disabledContainerColor = if (state.hasDeclaredLastCard) {
              tokens.suitPalette.pannaPrimary
            } else {
              Color.White.copy(alpha = 0.16f)
            },
            disabledContentColor = if (state.hasDeclaredLastCard) {
              Color.White
            } else {
              Color.White.copy(alpha = 0.55f)
            },
          ),
          border = if (declareActive) BorderStroke(1.5.dp, GoldTrimAccent) else null,
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
          modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag("declare_uno_button"),
        ) {
          Icon(
            imageVector = Icons.Filled.Campaign,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(5.dp))
          Text(
            text = if (state.hasDeclaredLastCard) {
              stringResource(R.string.board_declared_badge)
            } else {
              stringResource(R.string.board_declare_last_card)
            },
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
          )
        }

        // Dedicated Draw Card Button
        Button(
          onClick = onDrawClick,
          enabled = isHumanTurn,
          colors = ButtonDefaults.buttonColors(
            containerColor = if (state.pendingDrawPenalty > 0) {
              tokens.suitPalette.shonaliPrimary
            } else {
              GoldTrimAccent
            },
            contentColor = Color(0xFF1A1404),
            disabledContainerColor = Color.White.copy(alpha = 0.16f),
            disabledContentColor = Color.White.copy(alpha = 0.55f),
          ),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
          modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag("draw_card_button"),
        ) {
          Icon(
            imageVector = Icons.Filled.Layers,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(5.dp))
          Text(
            text = if (state.pendingDrawPenalty > 0) {
              "+${state.pendingDrawPenalty.toBengaliDigits(state.useBengaliNumerals)} তুলুন"
            } else {
              stringResource(R.string.board_draw_button)
            },
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
          )
        }
      }
    }

    LazyRow(
      contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 8.dp),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("player_hand_row"),
    ) {
      itemsIndexed(state.playerHand, key = { _, item -> item.domainCard.id }) { index, item ->
        AnimatedDealEntryCard(indexInHand = index) {
          AnimatedPlayableCardLift(isPlayable = item.isPlayable) {
            RongmohalGameCard(
              card = item.uiCard,
              cardWidth = cardWidth,
              cardHeight = cardHeight,
              useBengaliDigits = state.useBengaliNumerals,
              isPlayable = item.isPlayable,
              isHighlighted = item.isPlayable,
              onClick = { onCardClick(item.domainCard) },
              modifier = Modifier.testTag("hand_card_${item.domainCard.id}"),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun OpponentSeatCard(
  opponent: OpponentSeatState,
  isActiveTurn: Boolean,
  useBengaliDigits: Boolean,
  compact: Boolean = false,
) {
  Surface(
    color = Color.Black.copy(alpha = if (isActiveTurn) 0.62f else 0.36f),
    shape = MaterialTheme.shapes.medium,
    border = BorderStroke(
      width = if (isActiveTurn) 2.dp else 1.dp,
      color = if (isActiveTurn) GoldTrimAccent else Color.White.copy(alpha = 0.25f),
    ),
    modifier = Modifier.testTag("opponent_seat_${opponent.playerId}"),
  ) {
    Row(
      modifier = Modifier.padding(
        horizontal = if (compact) 8.dp else 10.dp,
        vertical = if (compact) 5.dp else 7.dp,
      ),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      PlayerAvatarBadge(
        avatar = opponent.avatar,
        size = if (compact) 32.dp else 38.dp,
        isSelected = isActiveTurn,
      )
      Column {
        Text(
          text = opponent.name,
          style = MaterialTheme.typography.labelMedium,
          color = Color.White,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        if (!compact) {
          Text(
            text = opponent.personaSubtitle,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.72f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Text(
          text = "${opponent.cardCount.toBengaliDigits(useBengaliDigits)} ${stringResource(R.string.board_cards_left_suffix)}",
          style = MaterialTheme.typography.labelSmall,
          color = GoldTrimAccent,
          maxLines = 1,
        )
      }
    }
  }
}

@Composable
private fun WildColorPickerDialog(
  onPickSuit: (RongmohalSuit) -> Unit,
  onDismiss: () -> Unit,
) {
  val tokens = LocalRongmohalTokens.current
  val suits = listOf(
    RongmohalSuit.SINDOOR,
    RongmohalSuit.PANNA,
    RongmohalSuit.NEELKANTHA,
    RongmohalSuit.SHONALI,
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = stringResource(R.string.board_pick_wild_color),
        style = MaterialTheme.typography.titleLarge,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        suits.chunked(2).forEach { rowSuits ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            rowSuits.forEach { suit ->
              Button(
                onClick = { onPickSuit(suit) },
                colors = ButtonDefaults.buttonColors(
                  containerColor = suit.primaryColor(tokens.suitPalette),
                  contentColor = Color.White,
                ),
                modifier = Modifier
                  .weight(1f)
                  .heightIn(min = 48.dp)
                  .testTag("wild_suit_${suit.name.lowercase()}"),
              ) {
                Text(
                  text = suit.bengaliName,
                  style = MaterialTheme.typography.labelLarge,
                  maxLines = 1,
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("বাতিল")
      }
    },
  )
}

@Composable
private fun HandSwapTargetDialog(
  opponents: List<OpponentSeatState>,
  useBengaliDigits: Boolean,
  onSelectOpponent: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = stringResource(R.string.board_pick_swap_target),
        style = MaterialTheme.typography.titleLarge,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        opponents.forEach { opp ->
          Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(min = 52.dp)
              .clickable { onSelectOpponent(opp.playerId) }
              .testTag("swap_target_${opp.playerId}"),
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                PlayerAvatarBadge(avatar = opp.avatar, size = 40.dp)
                Column {
                  Text(text = opp.name, style = MaterialTheme.typography.titleSmall)
                  Text(
                    text = opp.personaSubtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
              Text(
                text = "${opp.cardCount.toBengaliDigits(useBengaliDigits)} ${stringResource(R.string.board_cards_left_suffix)}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("বাতিল")
      }
    },
  )
}

@Composable
private fun MysteryOutcomeDialog(
  message: String,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Filled.AutoAwesome,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
      )
    },
    title = {
      Text(
        text = stringResource(R.string.board_mystery_title),
        style = MaterialTheme.typography.titleLarge,
      )
    },
    text = {
      Text(
        text = message,
        style = MaterialTheme.typography.bodyLarge,
      )
    },
    confirmButton = {
      Button(
        onClick = onDismiss,
        modifier = Modifier.heightIn(min = 48.dp),
      ) {
        Text(stringResource(R.string.board_mystery_ok))
      }
    },
  )
}

@Composable
private fun GamePauseDialog(
  onResume: () -> Unit,
  onRestart: () -> Unit,
  onExitToMenu: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onResume,
    title = {
      Text(
        text = stringResource(R.string.board_pause_title),
        style = MaterialTheme.typography.headlineSmall,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          text = stringResource(R.string.board_pause_subtitle),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
          onClick = onResume,
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("pause_resume_button"),
        ) {
          Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text(stringResource(R.string.board_pause_resume))
        }
        OutlinedButton(
          onClick = onRestart,
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("pause_restart_button"),
        ) {
          Icon(imageVector = Icons.Filled.Replay, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text(stringResource(R.string.board_pause_restart))
        }
        OutlinedButton(
          onClick = onExitToMenu,
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("pause_exit_button"),
        ) {
          Icon(imageVector = Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text(stringResource(R.string.board_pause_exit))
        }
      }
    },
    confirmButton = {},
  )
}
