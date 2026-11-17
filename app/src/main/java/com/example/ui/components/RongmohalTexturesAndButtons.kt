package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GoldTrimAccent
import com.example.ui.theme.LocalCardTableColors

@Composable
fun TableTextureBackground(
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit,
) {
  val tableColors = LocalCardTableColors.current
  Box(modifier = modifier) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      drawRect(
        brush = Brush.radialGradient(
          colors = listOf(
            tableColors.feltSurface,
            tableColors.feltSurface.copy(alpha = 0.85f),
            Color(0xFF041C16),
          ),
          center = Offset(size.width / 2f, size.height / 2f),
          radius = maxOf(size.width, size.height) * 0.72f,
        )
      )

      // Subtle Nakshi Kantha stitch-dot grid texture
      val step = 28.dp.toPx()
      var y = step / 2f
      while (y < size.height) {
        var x = step / 2f
        while (x < size.width) {
          drawCircle(
            color = Color.White.copy(alpha = 0.045f),
            radius = 1.4.dp.toPx(),
            center = Offset(x, y),
          )
          x += step
        }
        y += step
      }

      // Central Alpona ring guide on the felt table
      val center = Offset(size.width / 2f, size.height / 2f)
      val ringRadius = minOf(size.width, size.height) * 0.34f
      drawCircle(
        color = GoldTrimAccent.copy(alpha = 0.16f),
        radius = ringRadius,
        center = center,
        style = Stroke(width = 2.dp.toPx()),
      )
      drawCircle(
        color = GoldTrimAccent.copy(alpha = 0.08f),
        radius = ringRadius * 1.15f,
        center = center,
        style = Stroke(width = 1.dp.toPx()),
      )
    }
    content()
  }
}

@Composable
fun RongmohalPrimaryButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  enabled: Boolean = true,
  testTag: String? = null,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val pressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (pressed) 0.97f else 1f,
    animationSpec = spring(dampingRatio = 0.7f),
    label = "btn_scale",
  )

  Button(
    onClick = onClick,
    enabled = enabled,
    interactionSource = interactionSource,
    shape = RoundedCornerShape(16.dp),
    colors = ButtonDefaults.buttonColors(
      containerColor = MaterialTheme.colorScheme.primary,
      contentColor = MaterialTheme.colorScheme.onPrimary,
    ),
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 1.dp),
    modifier = modifier
      .heightIn(min = 52.dp)
      .graphicsLayer {
        scaleX = scale
        scaleY = scale
      }
      .let { if (testTag != null) it.testTag(testTag) else it },
  ) {
    if (icon != null) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(20.dp),
      )
      Spacer(modifier = Modifier.size(8.dp))
    }
    Text(text = text, style = MaterialTheme.typography.labelLarge)
  }
}

@Composable
fun RongmohalSecondaryButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  enabled: Boolean = true,
  testTag: String? = null,
) {
  Button(
    onClick = onClick,
    enabled = enabled,
    shape = RoundedCornerShape(16.dp),
    colors = ButtonDefaults.buttonColors(
      containerColor = MaterialTheme.colorScheme.secondary,
      contentColor = MaterialTheme.colorScheme.onSecondary,
    ),
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
    modifier = modifier
      .heightIn(min = 52.dp)
      .let { if (testTag != null) it.testTag(testTag) else it },
  ) {
    if (icon != null) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(20.dp),
      )
      Spacer(modifier = Modifier.size(8.dp))
    }
    Text(text = text, style = MaterialTheme.typography.labelLarge)
  }
}

@Composable
fun RongmohalOutlinedButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  testTag: String? = null,
) {
  OutlinedButton(
    onClick = onClick,
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
    modifier = modifier
      .heightIn(min = 50.dp)
      .let { if (testTag != null) it.testTag(testTag) else it },
  ) {
    if (icon != null) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(20.dp),
      )
      Spacer(modifier = Modifier.size(8.dp))
    }
    Text(text = text, style = MaterialTheme.typography.labelLarge)
  }
}
