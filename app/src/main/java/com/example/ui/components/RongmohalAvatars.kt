package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GoldTrimAccent

enum class RongmohalAvatar(
  val id: Int,
  val bengaliTitle: String,
  val primaryColor: Color,
  val secondaryColor: Color,
) {
  BAUL(0, "বাউল", Color(0xFFB8332A), Color(0xFFF39C12)),
  SHAPLA(1, "শাপলা", Color(0xFF0E8A6B), Color(0xFF8AD6B8)),
  ROYAL_TIGER(2, "বাঘ", Color(0xFFC96A00), Color(0xFFFFD166)),
  PEACOCK(3, "ময়ূর", Color(0xFF1D5BB8), Color(0xFF4CC9F0)),
  RIVER_BOAT(4, "সাম্পান", Color(0xFF5D2A8A), Color(0xFFC77DFF)),
  PRADIP(5, "প্রদীপ", Color(0xFF8D142C), Color(0xFFFFB703));

  companion object {
    fun byId(id: Int): RongmohalAvatar =
      entries.firstOrNull { it.id == id } ?: BAUL
  }
}

@Composable
fun PlayerAvatarBadge(
  avatar: RongmohalAvatar,
  modifier: Modifier = Modifier,
  size: Dp = 56.dp,
  isSelected: Boolean = false,
  showLabel: Boolean = false,
  onClick: (() -> Unit)? = null,
) {
  Column(
    modifier = modifier
      .let { base ->
        if (onClick != null) {
          base.clickable(onClick = onClick)
        } else {
          base
        }
      }
      .semantics { contentDescription = "অবতার: ${avatar.bengaliTitle}" },
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Surface(
      shape = CircleShape,
      border = BorderStroke(
        width = if (isSelected) 3.dp else 1.5.dp,
        color = if (isSelected) GoldTrimAccent else Color.White.copy(alpha = 0.7f),
      ),
      shadowElevation = if (isSelected) 6.dp else 2.dp,
      modifier = Modifier.size(size),
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .clip(CircleShape)
          .background(
            Brush.radialGradient(
              colors = listOf(avatar.secondaryColor, avatar.primaryColor)
            )
          ),
      ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(size * 0.18f)) {
          val w = this.size.width
          val h = this.size.height
          val cx = w / 2f
          val cy = h / 2f
          val r = minOf(w, h) * 0.45f

          when (avatar) {
            RongmohalAvatar.BAUL -> {
              // Ektara motif
              drawCircle(Color.White, radius = r * 0.48f, center = Offset(cx, cy + r * 0.35f))
              drawLine(
                color = Color.White,
                start = Offset(cx - r * 0.25f, cy + r * 0.1f),
                end = Offset(cx - r * 0.1f, cy - r * 0.9f),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round,
              )
              drawLine(
                color = Color.White,
                start = Offset(cx + r * 0.25f, cy + r * 0.1f),
                end = Offset(cx + r * 0.1f, cy - r * 0.9f),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round,
              )
            }
            RongmohalAvatar.SHAPLA -> {
              val petal = Path().apply {
                moveTo(cx, cy - r)
                quadraticTo(cx + r * 0.7f, cy, cx, cy + r * 0.7f)
                quadraticTo(cx - r * 0.7f, cy, cx, cy - r)
                close()
              }
              drawPath(petal, color = Color.White)
              drawCircle(avatar.primaryColor, radius = r * 0.16f, center = Offset(cx, cy))
            }
            RongmohalAvatar.ROYAL_TIGER -> {
              // Bold geometric tiger crest
              drawCircle(Color.White, radius = r * 0.78f, center = Offset(cx, cy))
              drawLine(avatar.primaryColor, Offset(cx - r * 0.4f, cy - r * 0.3f), Offset(cx + r * 0.4f, cy - r * 0.3f), 2.5.dp.toPx())
              drawLine(avatar.primaryColor, Offset(cx - r * 0.3f, cy), Offset(cx + r * 0.3f, cy), 2.5.dp.toPx())
              drawLine(avatar.primaryColor, Offset(cx - r * 0.2f, cy + r * 0.3f), Offset(cx + r * 0.2f, cy + r * 0.3f), 2.5.dp.toPx())
            }
            RongmohalAvatar.PEACOCK -> {
              drawCircle(Color.White, radius = r * 0.85f, center = Offset(cx, cy), style = Stroke(2.2.dp.toPx()))
              drawCircle(Color.White, radius = r * 0.48f, center = Offset(cx, cy + r * 0.1f))
              drawCircle(avatar.primaryColor, radius = r * 0.22f, center = Offset(cx, cy + r * 0.1f))
            }
            RongmohalAvatar.RIVER_BOAT -> {
              val hull = Path().apply {
                moveTo(cx - r, cy + r * 0.2f)
                lineTo(cx + r, cy + r * 0.2f)
                lineTo(cx + r * 0.65f, cy + r * 0.65f)
                lineTo(cx - r * 0.65f, cy + r * 0.65f)
                close()
              }
              val sail = Path().apply {
                moveTo(cx - r * 0.1f, cy + r * 0.1f)
                lineTo(cx - r * 0.1f, cy - r * 0.85f)
                lineTo(cx + r * 0.65f, cy + r * 0.1f)
                close()
              }
              drawPath(hull, Color.White)
              drawPath(sail, Color.White)
            }
            RongmohalAvatar.PRADIP -> {
              val flame = Path().apply {
                moveTo(cx, cy - r * 0.9f)
                quadraticTo(cx + r * 0.45f, cy - r * 0.1f, cx, cy + r * 0.1f)
                quadraticTo(cx - r * 0.45f, cy - r * 0.1f, cx, cy - r * 0.9f)
                close()
              }
              val bowl = Path().apply {
                moveTo(cx - r * 0.85f, cy + r * 0.2f)
                lineTo(cx + r * 0.85f, cy + r * 0.2f)
                lineTo(cx + r * 0.45f, cy + r * 0.7f)
                lineTo(cx - r * 0.45f, cy + r * 0.7f)
                close()
              }
              drawPath(flame, Color.White)
              drawPath(bowl, Color.White)
            }
          }
        }
      }
    }

    if (showLabel) {
      Text(
        text = avatar.bengaliTitle,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}
