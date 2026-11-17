package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class RongmohalSpacing(
  val xs: Dp = 4.dp,
  val sm: Dp = 8.dp,
  val md: Dp = 16.dp,
  val lg: Dp = 24.dp,
  val xl: Dp = 32.dp,
)

@Immutable
data class RongmohalCardMetrics(
  val compactWidth: Dp = 66.dp,
  val compactHeight: Dp = 96.dp,
  val regularWidth: Dp = 84.dp,
  val regularHeight: Dp = 122.dp,
  val showcaseWidth: Dp = 104.dp,
  val showcaseHeight: Dp = 152.dp,
  val cornerRadius: Dp = 14.dp,
  val borderStroke: Dp = 2.dp,
)

@Immutable
data class RongmohalSuitPalette(
  val sindoorPrimary: Color = SuitSindoorCrimson,
  val sindoorDeep: Color = SuitSindoorDeep,
  val pannaPrimary: Color = SuitPannaEmerald,
  val pannaDeep: Color = SuitPannaDeep,
  val neelkanthaPrimary: Color = SuitNeelkanthaIndigo,
  val neelkanthaDeep: Color = SuitNeelkanthaDeep,
  val shonaliPrimary: Color = SuitShonaliMarigold,
  val shonaliDeep: Color = SuitShonaliDeep,
  val mohalWildPrimary: Color = SuitMohalPlum,
  val mohalWildDeep: Color = SuitMohalPlumDeep,
)

@Immutable
data class RongmohalDesignTokens(
  val spacing: RongmohalSpacing = RongmohalSpacing(),
  val cardMetrics: RongmohalCardMetrics = RongmohalCardMetrics(),
  val suitPalette: RongmohalSuitPalette = RongmohalSuitPalette(),
  val minTouchTarget: Dp = 48.dp,
  val tabletBreakpoint: Dp = 640.dp,
)

val LocalRongmohalTokens = staticCompositionLocalOf { RongmohalDesignTokens() }
