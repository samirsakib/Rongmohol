package com.example.domain.model

data class AppPreferences(
  val soundEnabled: Boolean = true,
  val hapticsEnabled: Boolean = true,
  val useBengaliNumerals: Boolean = true,
  val darkThemeEnabled: Boolean = false,
  val dynamicColorEnabled: Boolean = false,
  val selectedAvatarId: Int = 0,
  val guestModeEnabled: Boolean = false,
  val guestDisplayName: String = "অতিথি খেলোয়াড়",
)
