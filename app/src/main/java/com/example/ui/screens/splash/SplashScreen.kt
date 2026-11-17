package com.example.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.RongmohalBrandLogo
import com.example.ui.components.TableTextureBackground
import com.example.ui.theme.GoldTrimAccent
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
  onSplashFinished: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val scaleAnim = remember { Animatable(0.75f) }
  val alphaAnim = remember { Animatable(0f) }

  LaunchedEffect(Unit) {
    scaleAnim.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
    )
    alphaAnim.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 400),
    )
    delay(650L)
    onSplashFinished()
  }

  Surface(modifier = modifier.fillMaxSize()) {
    TableTextureBackground(modifier = Modifier.fillMaxSize()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .safeDrawingPadding()
          .padding(24.dp),
        contentAlignment = Alignment.Center,
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(16.dp),
          modifier = Modifier.graphicsLayer {
            scaleX = scaleAnim.value
            scaleY = scaleAnim.value
          },
        ) {
          Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = Color.White.copy(alpha = 0.94f),
            shadowElevation = 8.dp,
            modifier = Modifier.testTag("splash_logo_card"),
          ) {
            RongmohalBrandLogo(
              emblemSize = 104.dp,
              showSubtitle = true,
              modifier = Modifier.padding(horizontal = 28.dp, vertical = 22.dp),
            )
          }

          Text(
            text = stringResource(R.string.splash_tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.92f),
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer { alpha = alphaAnim.value },
          )

          Spacer(modifier = Modifier.height(12.dp))

          CircularProgressIndicator(
            color = GoldTrimAccent,
            strokeWidth = 3.dp,
            modifier = Modifier
              .size(28.dp)
              .graphicsLayer { alpha = alphaAnim.value },
          )
          Text(
            text = stringResource(R.string.splash_loading),
            style = MaterialTheme.typography.labelMedium,
            color = GoldTrimAccent,
            modifier = Modifier.graphicsLayer { alpha = alphaAnim.value },
          )
        }
      }
    }
  }
}
