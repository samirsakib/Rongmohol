package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.config.toBengaliDigits
import com.example.ui.theme.CardBackIndigo
import com.example.ui.theme.CardBackTeal
import com.example.ui.theme.CardIvoryPaper
import com.example.ui.theme.GoldTrimAccent
import com.example.ui.theme.LocalRongmohalTokens
import com.example.ui.theme.RongmohalSuitPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class RongmohalSuit(
  val bengaliName: String,
  val emblemName: String,
) {
  SINDOOR("সিঁদুর", "শাপলা"),
  PANNA("পান্না", "পাতা"),
  NEELKANTHA("নীলকণ্ঠ", "নক্ষত্র"),
  SHONALI("সোনালী", "চক্র"),
  MOHAL_WILD("মহল", "বহুরঙা"),
  ;

  fun primaryColor(palette: RongmohalSuitPalette): Color = when (this) {
    SINDOOR -> palette.sindoorPrimary
    PANNA -> palette.pannaPrimary
    NEELKANTHA -> palette.neelkanthaPrimary
    SHONALI -> palette.shonaliPrimary
    MOHAL_WILD -> palette.mohalWildPrimary
  }

  fun deepColor(palette: RongmohalSuitPalette): Color = when (this) {
    SINDOOR -> palette.sindoorDeep
    PANNA -> palette.pannaDeep
    NEELKANTHA -> palette.neelkanthaDeep
    SHONALI -> palette.shonaliDeep
    MOHAL_WILD -> palette.mohalWildDeep
  }
}

enum class RongmohalActionType(
  val bengaliTitle: String,
  val shortBadge: String,
  val isWild: Boolean,
) {
  SKIP("থামাও", "⊘", false),
  REVERSE("ঘোরাও", "⇄", false),
  DRAW_TWO("দুই টানো", "+২", false),
  WILD_COLOR("রঙ বদল", "✦", true),
  WILD_DRAW_FOUR("মহাচাল", "+৪", true),
  THUNDERBOLT_PLUS_THREE("বজ্রপাত", "+৩", true),
  HAND_SWAP("হাতবদল", "⇄✋", true),
  BOUNCE_BACK("বাউন্স ব্যাক", "↩", false),
  EVERYONE_DRAW_ONE("সবাই +১", "+১", true),
  MYSTERY_CARD("রহস্য কার্ড", "?", true),
}

sealed interface RongmohalCardModel {
  val id: String
  val suit: RongmohalSuit

  data class NumberCard(
    override val id: String,
    override val suit: RongmohalSuit,
    val number: Int,
  ) : RongmohalCardModel

  data class ActionCard(
    override val id: String,
    override val suit: RongmohalSuit,
    val action: RongmohalActionType,
  ) : RongmohalCardModel
}

@Composable
fun GameCardBack(
  modifier: Modifier = Modifier,
  cardWidth: Dp = 76.dp,
  cardHeight: Dp = 110.dp,
  onClick: (() -> Unit)? = null,
) {
  val tokens = LocalRongmohalTokens.current
  val shape = RoundedCornerShape(tokens.cardMetrics.cornerRadius)
  val cardModifier = modifier
    .width(cardWidth)
    .height(cardHeight)
    .clip(shape)
    .let { base ->
      if (onClick != null) {
        base.clickable(onClick = onClick)
      } else {
        base
      }
    }
    .semantics { contentDescription = "তাসের পেছনের নকশা" }

  Card(
    modifier = cardModifier,
    shape = shape,
    border = BorderStroke(2.dp, GoldTrimAccent),
    elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      drawRect(
        brush = Brush.linearGradient(
          colors = listOf(CardBackIndigo, CardBackTeal),
        )
      )
      val w = size.width
      val h = size.height
      val margin = w * 0.1f

      // Inner gold frame
      drawRect(
        color = GoldTrimAccent.copy(alpha = 0.7f),
        topLeft = Offset(margin, margin),
        size = Size(w - margin * 2, h - margin * 2),
        style = Stroke(width = 1.5.dp.toPx()),
      )

      // Nakshi Kantha diagonal lattice
      val step = w * 0.22f
      var x = -h
      while (x < w + h) {
        drawLine(
          color = Color.White.copy(alpha = 0.08f),
          start = Offset(x, 0f),
          end = Offset(x + h, h),
          strokeWidth = 1.dp.toPx(),
        )
        drawLine(
          color = Color.White.copy(alpha = 0.08f),
          start = Offset(x + h, 0f),
          end = Offset(x, h),
          strokeWidth = 1.dp.toPx(),
        )
        x += step
      }

      // Central octagonal Alpona lotus seal
      val cx = w / 2f
      val cy = h / 2f
      val r = minOf(w, h) * 0.24f
      drawCircle(
        color = GoldTrimAccent.copy(alpha = 0.25f),
        radius = r * 1.2f,
        center = Offset(cx, cy),
      )
      drawLotusMedallion(center = Offset(cx, cy), radius = r, color = GoldTrimAccent)
    }
  }
}

@Composable
fun RongmohalGameCard(
  card: RongmohalCardModel,
  modifier: Modifier = Modifier,
  cardWidth: Dp = 84.dp,
  cardHeight: Dp = 122.dp,
  useBengaliDigits: Boolean = true,
  isPlayable: Boolean = true,
  isHighlighted: Boolean = false,
  onClick: (() -> Unit)? = null,
) {
  val scale by animateFloatAsState(
    targetValue = if (isHighlighted) 1.06f else 1f,
    animationSpec = spring(dampingRatio = 0.7f),
    label = "card_scale",
  )

  when (card) {
    is RongmohalCardModel.NumberCard -> GameNumberCard(
      suit = card.suit,
      number = card.number,
      useBengaliDigits = useBengaliDigits,
      cardWidth = cardWidth,
      cardHeight = cardHeight,
      isPlayable = isPlayable,
      onClick = onClick,
      modifier = modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
      },
    )
    is RongmohalCardModel.ActionCard -> GameActionCard(
      suit = card.suit,
      action = card.action,
      cardWidth = cardWidth,
      cardHeight = cardHeight,
      isPlayable = isPlayable,
      onClick = onClick,
      modifier = modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
      },
    )
  }
}

@Composable
fun GameNumberCard(
  suit: RongmohalSuit,
  number: Int,
  modifier: Modifier = Modifier,
  useBengaliDigits: Boolean = true,
  cardWidth: Dp = 84.dp,
  cardHeight: Dp = 122.dp,
  isPlayable: Boolean = true,
  onClick: (() -> Unit)? = null,
) {
  val tokens = LocalRongmohalTokens.current
  val primary = suit.primaryColor(tokens.suitPalette)
  val deep = suit.deepColor(tokens.suitPalette)
  val numText = number.toBengaliDigits(useBengaliDigits)
  val shape = RoundedCornerShape(tokens.cardMetrics.cornerRadius)
  val alpha = if (isPlayable) 1f else 0.58f

  Card(
    modifier = modifier
      .width(cardWidth)
      .height(cardHeight)
      .graphicsLayer { this.alpha = alpha }
      .clip(shape)
      .let { if (onClick != null) it.clickable(enabled = isPlayable, onClick = onClick) else it }
      .semantics { contentDescription = "${suit.bengaliName} $numText" },
    shape = shape,
    colors = CardDefaults.cardColors(containerColor = CardIvoryPaper),
    border = BorderStroke(2.dp, primary),
    elevation = CardDefaults.cardElevation(defaultElevation = if (isPlayable) 6.dp else 1.dp),
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      // Stepped geometric corner bands (original Rongmohal frame, not an UNO oval)
      Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val pad = w * 0.08f
        drawRect(
          color = primary.copy(alpha = 0.10f),
          topLeft = Offset(pad, pad),
          size = Size(w - pad * 2, h - pad * 2),
        )
        drawRect(
          color = primary.copy(alpha = 0.5f),
          topLeft = Offset(pad, pad),
          size = Size(w - pad * 2, h - pad * 2),
          style = Stroke(width = 1.2.dp.toPx()),
        )
        // Central Alpona suit medallion
        drawSuitEmblem(
          suit = suit,
          center = Offset(w / 2f, h / 2f),
          radius = minOf(w, h) * 0.24f,
          color = primary,
          accent = deep,
        )
      }

      // Top-left Bengali numeral & mini emblem
      Column(
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(start = 8.dp, top = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          text = numText,
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = (cardWidth.value * 0.22f).coerceIn(13f, 22f).sp,
          ),
          color = deep,
        )
      }

      // Bottom-right suit label + numeral
      Row(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(end = 8.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
      ) {
        Text(
          text = suit.bengaliName,
          style = MaterialTheme.typography.labelSmall.copy(
            fontSize = (cardWidth.value * 0.11f).coerceIn(8f, 12f).sp,
          ),
          color = deep.copy(alpha = 0.85f),
        )
        Text(
          text = numText,
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
          color = deep,
        )
      }
    }
  }
}

@Composable
fun GameActionCard(
  suit: RongmohalSuit,
  action: RongmohalActionType,
  modifier: Modifier = Modifier,
  cardWidth: Dp = 84.dp,
  cardHeight: Dp = 122.dp,
  isPlayable: Boolean = true,
  onClick: (() -> Unit)? = null,
) {
  val tokens = LocalRongmohalTokens.current
  val primary = suit.primaryColor(tokens.suitPalette)
  val deep = suit.deepColor(tokens.suitPalette)
  val shape = RoundedCornerShape(tokens.cardMetrics.cornerRadius)
  val alpha = if (isPlayable) 1f else 0.58f

  Card(
    modifier = modifier
      .width(cardWidth)
      .height(cardHeight)
      .graphicsLayer { this.alpha = alpha }
      .clip(shape)
      .let { if (onClick != null) it.clickable(enabled = isPlayable, onClick = onClick) else it }
      .semantics { contentDescription = "${suit.bengaliName} ${action.bengaliTitle}" },
    shape = shape,
    border = BorderStroke(2.dp, GoldTrimAccent),
    elevation = CardDefaults.cardElevation(defaultElevation = if (isPlayable) 7.dp else 2.dp),
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          brush = if (action.isWild) {
            Brush.linearGradient(
              colors = listOf(
                tokens.suitPalette.mohalWildDeep,
                tokens.suitPalette.mohalWildPrimary,
                tokens.suitPalette.sindoorDeep,
              )
            )
          } else {
            Brush.verticalGradient(
              colors = listOf(primary, deep)
            )
          }
        )
        .padding(7.dp),
    ) {
      // Corner Action Badge
      Surface(
        color = CardIvoryPaper,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.align(Alignment.TopStart),
      ) {
        Text(
          text = action.shortBadge,
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          color = deep,
          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
      }

      // Central geometric Alpona action icon
      Canvas(
        modifier = Modifier
          .size(minOf(cardWidth, cardHeight) * 0.48f)
          .align(Alignment.Center)
      ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = minOf(size.width, size.height) * 0.44f
        drawCircle(
          color = GoldTrimAccent.copy(alpha = 0.28f),
          radius = r * 1.05f,
          center = Offset(cx, cy),
        )
        if (action.isWild) {
          val quadColors = listOf(
            tokens.suitPalette.sindoorPrimary,
            tokens.suitPalette.pannaPrimary,
            tokens.suitPalette.neelkanthaPrimary,
            tokens.suitPalette.shonaliPrimary,
          )
          quadColors.forEachIndexed { idx, color ->
            drawArc(
              color = color,
              startAngle = idx * 90f,
              sweepAngle = 84f,
              useCenter = true,
              topLeft = Offset(cx - r, cy - r),
              size = Size(r * 2, r * 2),
            )
          }
          drawCircle(
            color = CardIvoryPaper,
            radius = r * 0.36f,
            center = Offset(cx, cy),
          )
        } else {
          drawCircle(
            color = CardIvoryPaper.copy(alpha = 0.92f),
            radius = r * 0.85f,
            center = Offset(cx, cy),
            style = Stroke(width = 2.2.dp.toPx()),
          )
          drawSuitEmblem(
            suit = suit,
            center = Offset(cx, cy),
            radius = r * 0.56f,
            color = GoldTrimAccent,
            accent = CardIvoryPaper,
          )
        }
      }

      // Bottom Bengali action title banner
      Surface(
        color = Color.Black.copy(alpha = 0.32f),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth(),
      ) {
        Text(
          text = action.bengaliTitle,
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = (cardWidth.value * 0.125f).coerceIn(9f, 13f).sp,
          ),
          color = Color.White,
          textAlign = TextAlign.Center,
          maxLines = 1,
          modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
        )
      }
    }
  }
}

private fun DrawScope.drawSuitEmblem(
  suit: RongmohalSuit,
  center: Offset,
  radius: Float,
  color: Color,
  accent: Color,
) {
  when (suit) {
    RongmohalSuit.SINDOOR -> drawLotusMedallion(center, radius, color)
    RongmohalSuit.PANNA -> {
      // Banyan Leaf silhouette
      val leaf = Path().apply {
        moveTo(center.x, center.y - radius)
        quadraticTo(center.x + radius * 1.1f, center.y - radius * 0.1f, center.x, center.y + radius)
        quadraticTo(center.x - radius * 1.1f, center.y - radius * 0.1f, center.x, center.y - radius)
        close()
      }
      drawPath(leaf, color = color)
      drawLine(
        color = accent,
        start = Offset(center.x, center.y - radius * 0.65f),
        end = Offset(center.x, center.y + radius * 0.7f),
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round,
      )
    }
    RongmohalSuit.NEELKANTHA -> {
      // 8-point Alpona Star
      val star = Path()
      for (i in 0 until 16) {
        val r = if (i % 2 == 0) radius else radius * 0.45f
        val angle = (i * 22.5 - 90.0) * PI / 180.0
        val px = center.x + (r * cos(angle)).toFloat()
        val py = center.y + (r * sin(angle)).toFloat()
        if (i == 0) star.moveTo(px, py) else star.lineTo(px, py)
      }
      star.close()
      drawPath(star, color = color)
      drawCircle(color = CardIvoryPaper, radius = radius * 0.2f, center = center)
    }
    RongmohalSuit.SHONALI -> {
      // Sun Wheel (Chakra)
      drawCircle(color = color, radius = radius * 0.58f, center = center)
      for (i in 0 until 8) {
        val angle = (i * 45.0) * PI / 180.0
        val sx = center.x + (radius * 0.68f * cos(angle)).toFloat()
        val sy = center.y + (radius * 0.68f * sin(angle)).toFloat()
        val ex = center.x + (radius * 1.05f * cos(angle)).toFloat()
        val ey = center.y + (radius * 1.05f * sin(angle)).toFloat()
        drawLine(
          color = color,
          start = Offset(sx, sy),
          end = Offset(ex, ey),
          strokeWidth = 2.4.dp.toPx(),
          cap = StrokeCap.Round,
        )
      }
      drawCircle(color = CardIvoryPaper, radius = radius * 0.25f, center = center)
    }
    RongmohalSuit.MOHAL_WILD -> {
      drawLotusMedallion(center, radius, color)
    }
  }
}

private fun DrawScope.drawLotusMedallion(
  center: Offset,
  radius: Float,
  color: Color,
) {
  for (i in 0 until 8) {
    val angle = (i * 45.0) * PI / 180.0
    val petalTip = Offset(
      x = center.x + (radius * cos(angle)).toFloat(),
      y = center.y + (radius * sin(angle)).toFloat(),
    )
    drawLine(
      color = color,
      start = center,
      end = petalTip,
      strokeWidth = radius * 0.32f,
      cap = StrokeCap.Round,
    )
  }
  drawCircle(
    color = CardIvoryPaper,
    radius = radius * 0.28f,
    center = center,
  )
}
