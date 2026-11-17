package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Immutable
data class CardTableColors(
  val feltSurface: Color,
  val goldTrim: Color,
  val cardFace: Color,
  val suitRed: Color,
  val suitBlack: Color,
)

val LocalCardTableColors = staticCompositionLocalOf {
  CardTableColors(
    feltSurface = FeltTableLight,
    goldTrim = GoldTrimAccent,
    cardFace = CardIvoryPaper,
    suitRed = SuitSindoorCrimson,
    suitBlack = DeepInkOnLight,
  )
}

private val LightColorScheme = lightColorScheme(
  primary = EmeraldBaizePrimary,
  onPrimary = Color.White,
  primaryContainer = EmeraldBaizeContainer,
  onPrimaryContainer = OnEmeraldBaizeContainer,
  secondary = AlponaCrimsonSecondary,
  onSecondary = Color.White,
  secondaryContainer = AlponaCrimsonContainer,
  onSecondaryContainer = OnAlponaCrimsonContainer,
  tertiary = AntiqueGoldTertiary,
  onTertiary = Color.White,
  tertiaryContainer = AntiqueGoldContainer,
  onTertiaryContainer = OnAntiqueGoldContainer,
  background = IvoryParchmentBackground,
  onBackground = DeepInkOnLight,
  surface = IvoryParchmentBackground,
  onSurface = DeepInkOnLight,
  surfaceVariant = IvoryCardSurface,
  onSurfaceVariant = MutedInkOnLight,
  surfaceContainerLow = IvoryCardSurface,
  surfaceContainer = IvorySurfaceVariant,
  outline = WarmOutlineLight,
)

private val DarkColorScheme = darkColorScheme(
  primary = DarkEmeraldPrimary,
  onPrimary = Color(0xFF003828),
  primaryContainer = DarkEmeraldContainer,
  onPrimaryContainer = OnDarkEmeraldContainer,
  secondary = DarkCrimsonSecondary,
  onSecondary = Color(0xFF600012),
  secondaryContainer = DarkCrimsonContainer,
  onSecondaryContainer = OnDarkCrimsonContainer,
  tertiary = DarkGoldTertiary,
  onTertiary = Color(0xFF442B00),
  tertiaryContainer = DarkGoldContainer,
  onTertiaryContainer = OnDarkGoldContainer,
  background = DarkVelvetBackground,
  onBackground = WarmIvoryOnDark,
  surface = DarkVelvetSurface,
  onSurface = WarmIvoryOnDark,
  surfaceVariant = DarkVelvetSurfaceVariant,
  onSurfaceVariant = MutedIvoryOnDark,
  surfaceContainerLow = DarkVelvetSurface,
  surfaceContainer = DarkVelvetSurfaceVariant,
  outline = WarmOutlineDark,
)

@Composable
fun BanglaCardGameTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }

  val tableColors = if (darkTheme) {
    CardTableColors(
      feltSurface = FeltTableDark,
      goldTrim = GoldTrimAccent,
      cardFace = CardIvoryPaper,
      suitRed = SuitSindoorCrimson,
      suitBlack = DeepInkOnLight,
    )
  } else {
    CardTableColors(
      feltSurface = FeltTableLight,
      goldTrim = GoldTrimAccent,
      cardFace = CardIvoryPaper,
      suitRed = SuitSindoorCrimson,
      suitBlack = DeepInkOnLight,
    )
  }

  CompositionLocalProvider(
    LocalCardTableColors provides tableColors,
    LocalRongmohalTokens provides RongmohalDesignTokens(),
  ) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = AppTypography,
      shapes = AppShapes,
      content = content,
    )
  }
}
