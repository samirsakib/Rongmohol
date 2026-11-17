package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GoldTrimAccent
import com.example.ui.theme.SuitNeelkanthaIndigo
import com.example.ui.theme.SuitPannaEmerald
import com.example.ui.theme.SuitShonaliMarigold
import com.example.ui.theme.SuitSindoorCrimson
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class BoardEffectType {
  SHUFFLE,
  CARD_EXCHANGE,
  ATTACK,
  SHIELD,
}

data class BoardVisualEffect(
  val effectId: Long,
  val type: BoardEffectType,
  val title: String,
  val subtitle: String = "",
)

/**
 * 3D flip animation rotating smoothly around the Y axis between card back and front.
 */
@Composable
fun AnimatedFlippableCard(
  isFaceUp: Boolean,
  modifier: Modifier = Modifier,
  cardWidth: Dp = 84.dp,
  cardHeight: Dp = 122.dp,
  frontContent: @Composable () -> Unit,
  backContent: @Composable () -> Unit = {
    GameCardBack(cardWidth = cardWidth, cardHeight = cardHeight)
  },
) {
  val rotation by animateFloatAsState(
    targetValue = if (isFaceUp) 0f else 180f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioLowBouncy,
      stiffness = Spring.StiffnessMediumLow,
    ),
    label = "card_flip_rotation",
  )
  val density = LocalDensity.current.density

  Box(
    modifier = modifier.graphicsLayer {
      rotationY = rotation
      cameraDistance = 12f * density
    },
    contentAlignment = Alignment.Center,
  ) {
    if (rotation <= 90f) {
      frontContent()
    } else {
      Box(
        modifier = Modifier.graphicsLayer { rotationY = 180f },
      ) {
        backContent()
      }
    }
  }
}

/**
 * Staggered entrance animation when cards are dealt or drawn into the hand.
 */
@Composable
fun AnimatedDealEntryCard(
  indexInHand: Int,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  var visible by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) {
    visible = true
  }

  AnimatedVisibility(
    visible = visible,
    enter = fadeIn(
      animationSpec = tween(
        durationMillis = 220,
        delayMillis = (indexInHand * 35).coerceAtMost(280),
      )
    ) + slideInVertically(
      animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow,
      ),
      initialOffsetY = { it / 3 },
    ) + scaleIn(
      initialScale = 0.85f,
      animationSpec = tween(durationMillis = 220),
    ),
    modifier = modifier,
  ) {
    content()
  }
}

/**
 * Lifts playable cards upward with subtle elevation and smooth spring transition.
 */
@Composable
fun AnimatedPlayableCardLift(
  isPlayable: Boolean,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  val liftOffset by animateDpAsState(
    targetValue = if (isPlayable) (-10).dp else 0.dp,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMedium,
    ),
    label = "card_playable_lift",
  )

  Box(
    modifier = modifier.offset(y = liftOffset),
  ) {
    content()
  }
}

/**
 * Center discard pile with play pop-and-settle rotation transition + active suit halo ring.
 */
@Composable
fun AnimatedDiscardCardStage(
  topCard: RongmohalCardModel,
  activeSuitColor: Color,
  cardWidth: Dp,
  cardHeight: Dp,
  useBengaliDigits: Boolean,
  modifier: Modifier = Modifier,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "discard_halo")
  val haloPulse by infiniteTransition.animateFloat(
    initialValue = 0.96f,
    targetValue = 1.05f,
    animationSpec = infiniteRepeatable(
      animation = tween(1400, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "halo_pulse",
  )

  Box(
    modifier = modifier,
    contentAlignment = Alignment.Center,
  ) {
    Canvas(
      modifier = Modifier
        .size(maxOf(cardWidth, cardHeight) + 18.dp)
        .graphicsLayer {
          scaleX = haloPulse
          scaleY = haloPulse
        },
    ) {
      drawCircle(
        color = activeSuitColor.copy(alpha = 0.22f),
        radius = size.minDimension * 0.48f,
      )
      drawCircle(
        color = activeSuitColor.copy(alpha = 0.72f),
        radius = size.minDimension * 0.48f,
        style = Stroke(width = 2.5.dp.toPx()),
      )
    }

    AnimatedContent(
      targetState = topCard,
      transitionSpec = {
        (fadeIn(tween(180)) + scaleIn(
          initialScale = 1.24f,
          animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
          ),
        )) togetherWith (fadeOut(tween(140)) + scaleOut(targetScale = 0.88f))
      },
      label = "discard_pile_transition",
    ) { card ->
      RongmohalGameCard(
        card = card,
        cardWidth = cardWidth,
        cardHeight = cardHeight,
        useBengaliDigits = useBengaliDigits,
        isPlayable = true,
      )
    }
  }
}

/**
 * Draw pile with layered backing cards and recoil shake when drawing or shuffling.
 */
@Composable
fun AnimatedDrawPileDeck(
  remainingCards: Int,
  recycleTriggerKey: Int,
  drawPulseKey: Int,
  cardWidth: Dp,
  cardHeight: Dp,
  enabled: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val shakeOffset = remember { Animatable(0f) }
  val recoilScale = remember { Animatable(1f) }

  LaunchedEffect(recycleTriggerKey) {
    if (recycleTriggerKey > 0) {
      val sequence = listOf(-12f, 12f, -8f, 8f, -4f, 0f)
      for (target in sequence) {
        shakeOffset.animateTo(
          targetValue = target,
          animationSpec = tween(durationMillis = 55, easing = FastOutSlowInEasing),
        )
      }
    }
  }

  LaunchedEffect(drawPulseKey) {
    if (drawPulseKey > 0) {
      recoilScale.animateTo(0.92f, tween(80))
      recoilScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    }
  }

  Box(
    modifier = modifier.graphicsLayer {
      translationX = shakeOffset.value
      scaleX = recoilScale.value
      scaleY = recoilScale.value
      alpha = if (enabled || remainingCards > 0) 1f else 0.55f
    },
    contentAlignment = Alignment.Center,
  ) {
    if (remainingCards > 6) {
      GameCardBack(
        cardWidth = cardWidth,
        cardHeight = cardHeight,
        modifier = Modifier.offset(x = (-4).dp, y = 4.dp),
      )
    }
    if (remainingCards > 2) {
      GameCardBack(
        cardWidth = cardWidth,
        cardHeight = cardHeight,
        modifier = Modifier.offset(x = (-2).dp, y = 2.dp),
      )
    }
    GameCardBack(
      cardWidth = cardWidth,
      cardHeight = cardHeight,
      onClick = if (enabled) onClick else null,
    )
  }
}

/**
 * Overlay rendering Shuffle, Card Exchange, Attack, and Shield animations.
 */
@Composable
fun BoardSpecialEffectOverlay(
  effect: BoardVisualEffect?,
  modifier: Modifier = Modifier,
) {
  AnimatedVisibility(
    visible = effect != null,
    enter = fadeIn(tween(180)) + scaleIn(
      initialScale = 0.72f,
      animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
    ),
    exit = fadeOut(tween(240)) + scaleOut(targetScale = 1.08f),
    modifier = modifier,
  ) {
    if (effect != null) {
      val progress = remember(effect.effectId) { Animatable(0f) }
      LaunchedEffect(effect.effectId) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 900, easing = FastOutSlowInEasing))
      }

      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color.Black.copy(alpha = 0.28f)),
        contentAlignment = Alignment.Center,
      ) {
        when (effect.type) {
          BoardEffectType.SHUFFLE -> ShuffleBurstGraphic(progress.value)
          BoardEffectType.CARD_EXCHANGE -> CardExchangeGraphic(progress.value)
          BoardEffectType.ATTACK -> AttackShockwaveGraphic(progress.value)
          BoardEffectType.SHIELD -> ShieldBarrierGraphic(progress.value)
        }

        Surface(
          color = Color(0xFF0A241C).copy(alpha = 0.94f),
          shape = MaterialTheme.shapes.large,
          border = BorderStroke(
            2.dp,
            when (effect.type) {
              BoardEffectType.ATTACK -> SuitSindoorCrimson
              BoardEffectType.SHIELD -> SuitPannaEmerald
              BoardEffectType.CARD_EXCHANGE -> SuitNeelkanthaIndigo
              BoardEffectType.SHUFFLE -> GoldTrimAccent
            },
          ),
          shadowElevation = 10.dp,
          modifier = Modifier
            .padding(horizontal = 24.dp)
            .widthIn(max = 320.dp),
        ) {
          Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Text(
              text = effect.title,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = GoldTrimAccent,
              textAlign = TextAlign.Center,
            )
            if (effect.subtitle.isNotBlank()) {
              Text(
                text = effect.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                textAlign = TextAlign.Center,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ShuffleBurstGraphic(progress: Float) {
  Canvas(modifier = Modifier.size(220.dp)) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val cardW = 48.dp.toPx()
    val cardH = 70.dp.toPx()
    val spread = sin(progress * PI).toFloat()

    for (i in -2..2) {
      val angle = i * 18f * spread
      val dx = i * 26.dp.toPx() * spread
      rotate(degrees = angle, pivot = Offset(cx + dx, cy)) {
        drawRect(
          color = Color(0xFF13294B),
          topLeft = Offset(cx + dx - cardW / 2f, cy - cardH / 2f),
          size = Size(cardW, cardH),
        )
        drawRect(
          color = GoldTrimAccent,
          topLeft = Offset(cx + dx - cardW / 2f, cy - cardH / 2f),
          size = Size(cardW, cardH),
          style = Stroke(2.dp.toPx()),
        )
      }
    }
  }
}

@Composable
private fun CardExchangeGraphic(progress: Float) {
  Box(contentAlignment = Alignment.Center) {
    Canvas(modifier = Modifier.size(220.dp)) {
      val cx = size.width / 2f
      val cy = size.height / 2f
      val radius = size.minDimension * 0.36f
      val angleOffset = progress * 360f
      val aX = cx + radius * cos((angleOffset) * PI / 180.0).toFloat()
      val aY = cy + radius * sin((angleOffset) * PI / 180.0).toFloat()
      val bX = cx + radius * cos((angleOffset + 180f) * PI / 180.0).toFloat()
      val bY = cy + radius * sin((angleOffset + 180f) * PI / 180.0).toFloat()

      drawCircle(
        color = SuitNeelkanthaIndigo.copy(alpha = 0.45f),
        radius = radius,
        center = Offset(cx, cy),
        style = Stroke(width = 3.dp.toPx()),
      )
      drawCircle(color = SuitSindoorCrimson, radius = 14.dp.toPx(), center = Offset(aX, aY))
      drawCircle(color = GoldTrimAccent, radius = 14.dp.toPx(), center = Offset(bX, bY))
    }
    Icon(
      imageVector = Icons.Filled.SwapHoriz,
      contentDescription = null,
      tint = Color.White,
      modifier = Modifier
        .size(48.dp)
        .offset(y = (-68).dp),
    )
  }
}

@Composable
private fun AttackShockwaveGraphic(progress: Float) {
  Box(contentAlignment = Alignment.Center) {
    Canvas(modifier = Modifier.size(240.dp)) {
      val cx = size.width / 2f
      val cy = size.height / 2f
      val maxR = size.minDimension * 0.48f
      val r1 = maxR * progress
      val alpha = (1f - progress).coerceIn(0f, 1f)
      drawCircle(
        color = SuitSindoorCrimson.copy(alpha = alpha * 0.65f),
        radius = r1,
        center = Offset(cx, cy),
        style = Stroke(width = 6.dp.toPx()),
      )
      for (i in 0 until 8) {
        val angle = (i * 45.0) * PI / 180.0
        val start = Offset(cx + (r1 * 0.45f * cos(angle)).toFloat(), cy + (r1 * 0.45f * sin(angle)).toFloat())
        val end = Offset(cx + (r1 * cos(angle)).toFloat(), cy + (r1 * sin(angle)).toFloat())
        drawLine(
          color = GoldTrimAccent.copy(alpha = alpha),
          start = start,
          end = end,
          strokeWidth = 3.dp.toPx(),
        )
      }
    }
    Icon(
      imageVector = Icons.Filled.Bolt,
      contentDescription = null,
      tint = GoldTrimAccent,
      modifier = Modifier
        .size(44.dp)
        .offset(y = (-68).dp),
    )
  }
}

@Composable
private fun ShieldBarrierGraphic(progress: Float) {
  Box(contentAlignment = Alignment.Center) {
    Canvas(modifier = Modifier.size(220.dp)) {
      val cx = size.width / 2f
      val cy = size.height / 2f
      val maxR = size.minDimension * 0.45f
      val currentR = maxR * (0.4f + 0.6f * progress)
      val alpha = (1f - progress * 0.65f).coerceIn(0f, 1f)
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(SuitPannaEmerald.copy(alpha = alpha * 0.45f), Color.Transparent),
          center = Offset(cx, cy),
          radius = currentR,
        ),
        radius = currentR,
        center = Offset(cx, cy),
      )
      drawCircle(
        color = GoldTrimAccent.copy(alpha = alpha),
        radius = currentR,
        center = Offset(cx, cy),
        style = Stroke(width = 3.5.dp.toPx()),
      )
    }
    Icon(
      imageVector = Icons.Filled.Security,
      contentDescription = null,
      tint = Color.White,
      modifier = Modifier
        .size(44.dp)
        .offset(y = (-68).dp),
    )
  }
}

private data class ConfettiParticle(
  val xFraction: Float,
  val speed: Float,
  val sizeDp: Float,
  val color: Color,
  val phase: Float,
  val drift: Float,
)

/**
 * 60fps pure Compose Canvas confetti particles using Rongmohal suit & brass colors.
 */
@Composable
fun ConfettiCelebrationOverlay(
  modifier: Modifier = Modifier,
) {
  val palette = listOf(
    SuitSindoorCrimson,
    SuitPannaEmerald,
    SuitNeelkanthaIndigo,
    SuitShonaliMarigold,
    GoldTrimAccent,
    Color(0xFFC77DFF),
  )

  val particles = remember {
    val rng = Random(2026)
    List(55) { idx ->
      ConfettiParticle(
        xFraction = rng.nextFloat(),
        speed = 0.55f + rng.nextFloat() * 0.85f,
        sizeDp = 6f + rng.nextFloat() * 7f,
        color = palette[idx % palette.size],
        phase = rng.nextFloat(),
        drift = (rng.nextFloat() - 0.5f) * 36f,
      )
    }
  }

  val infinite = rememberInfiniteTransition(label = "confetti_fall")
  val tick by infinite.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 3600, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "confetti_tick",
  )

  Canvas(modifier = modifier.fillMaxSize()) {
    val w = size.width
    val h = size.height
    particles.forEach { p ->
      val yProg = (p.phase + tick * p.speed) % 1f
      val cx = (p.xFraction * w + sin((yProg * 6f + p.phase) * PI).toFloat() * p.drift.dp.toPx())
        .coerceIn(0f, w)
      val cy = yProg * (h + 40f) - 20f
      val s = p.sizeDp.dp.toPx()
      rotate(degrees = (yProg * 360f * p.speed) % 360f, pivot = Offset(cx, cy)) {
        drawRect(
          color = p.color.copy(alpha = 0.9f),
          topLeft = Offset(cx - s / 2f, cy - s / 3f),
          size = Size(s, s * 0.65f),
        )
      }
    }
  }
}

/**
 * In-board winner celebration overlay with confetti, avatar spotlight, and CTA button.
 */
@Composable
fun WinnerCelebrationOverlay(
  winnerName: String,
  winnerAvatar: RongmohalAvatar,
  isHumanWinner: Boolean,
  ctaLabel: String,
  onContinueToResults: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val trophyScale = remember { Animatable(0.5f) }
  LaunchedEffect(Unit) {
    trophyScale.animateTo(
      targetValue = 1f,
      animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow,
      ),
    )
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color.Black.copy(alpha = 0.68f)),
    contentAlignment = Alignment.Center,
  ) {
    ConfettiCelebrationOverlay()

    Surface(
      color = Color(0xFF0A281F),
      shape = MaterialTheme.shapes.extraLarge,
      border = BorderStroke(2.dp, GoldTrimAccent),
      shadowElevation = 14.dp,
      modifier = Modifier
        .padding(24.dp)
        .widthIn(max = 420.dp)
        .graphicsLayer {
          scaleX = trophyScale.value
          scaleY = trophyScale.value
        },
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Box(
          modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(GoldTrimAccent.copy(alpha = 0.22f)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Filled.EmojiEvents,
            contentDescription = null,
            tint = GoldTrimAccent,
            modifier = Modifier.size(38.dp),
          )
        }

        PlayerAvatarBadge(
          avatar = winnerAvatar,
          size = 76.dp,
          isSelected = true,
        )

        Text(
          text = if (isHumanWinner) "অভিনন্দন! আপনি বিজয়ী!" else "$winnerName আসর জিতেছেন!",
          style = MaterialTheme.typography.headlineSmall,
          color = Color.White,
          textAlign = TextAlign.Center,
        )
        Text(
          text = "আসরের চূড়ান্ত হিসাব ও পয়েন্ট তালিকা তৈরি হয়েছে।",
          style = MaterialTheme.typography.bodyMedium,
          color = Color.White.copy(alpha = 0.8f),
          textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(4.dp))

        RongmohalPrimaryButton(
          text = ctaLabel,
          icon = Icons.Filled.EmojiEvents,
          onClick = onContinueToResults,
          testTag = "winner_continue_button",
        )
      }
    }
  }
}
