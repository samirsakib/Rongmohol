package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.GoldTrimAccent
import com.example.ui.theme.LocalRongmohalTokens
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RongmohalBrandLogo(
  modifier: Modifier = Modifier,
  emblemSize: Dp = 76.dp,
  showSubtitle: Boolean = true,
  horizontalLockup: Boolean = false,
) {
  val tokens = LocalRongmohalTokens.current
  val suitColors = listOf(
    tokens.suitPalette.sindoorPrimary,
    tokens.suitPalette.pannaPrimary,
    tokens.suitPalette.neelkanthaPrimary,
    tokens.suitPalette.shonaliPrimary,
  )

  val emblem: @Composable () -> Unit = {
    Canvas(modifier = Modifier.size(emblemSize)) {
      val w = size.width
      val h = size.height
      val cx = w / 2f
      val cy = h / 2f
      val radius = minOf(w, h) * 0.46f

      // Octagonal Alpona seal
      val octPath = Path()
      for (i in 0 until 8) {
        val angle = (i * 45.0 + 22.5) * PI / 180.0
        val px = cx + (radius * cos(angle)).toFloat()
        val py = cy + (radius * sin(angle)).toFloat()
        if (i == 0) octPath.moveTo(px, py) else octPath.lineTo(px, py)
      }
      octPath.close()

      drawPath(
        path = octPath,
        brush = Brush.linearGradient(
          colors = listOf(Color(0xFF084233), Color(0xFF0F2B47))
        ),
      )
      drawPath(
        path = octPath,
        color = GoldTrimAccent,
        style = Stroke(width = w * 0.045f),
      )

      // Inner Alpona circle
      drawCircle(
        color = GoldTrimAccent.copy(alpha = 0.55f),
        radius = radius * 0.72f,
        center = Offset(cx, cy),
        style = Stroke(width = w * 0.02f),
      )

      // Four fanned geometric cards inside the medallion
      val cardW = w * 0.22f
      val cardH = h * 0.31f
      val angles = listOf(-24f, -8f, 8f, 24f)
      val offsets = listOf(-0.14f, -0.05f, 0.05f, 0.14f)
      suitColors.forEachIndexed { idx, color ->
        rotate(degrees = angles[idx], pivot = Offset(cx, cy + h * 0.12f)) {
          val left = cx - cardW / 2f + w * offsets[idx]
          val top = cy - cardH / 2f
          drawRoundRect(
            color = color,
            topLeft = Offset(left, top),
            size = Size(cardW, cardH),
            cornerRadius = CornerRadius(w * 0.04f, w * 0.04f),
          )
          drawRoundRect(
            color = Color(0xFFFFFDF7),
            topLeft = Offset(left, top),
            size = Size(cardW, cardH),
            cornerRadius = CornerRadius(w * 0.04f, w * 0.04f),
            style = Stroke(width = w * 0.018f),
          )
        }
      }
    }
  }

  val titleBlock: @Composable (Alignment.Horizontal) -> Unit = { align ->
    Column(
      horizontalAlignment = align,
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
        text = stringResource(R.string.app_name),
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.primary,
      )
      if (showSubtitle) {
        Text(
          text = stringResource(R.string.app_subtitle),
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }

  if (horizontalLockup) {
    Row(
      modifier = modifier,
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      emblem()
      titleBlock(Alignment.Start)
    }
  } else {
    Column(
      modifier = modifier,
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      emblem()
      titleBlock(Alignment.CenterHorizontally)
    }
  }
}
