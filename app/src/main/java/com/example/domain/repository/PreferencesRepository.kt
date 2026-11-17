package com.example.domain.repository

import com.example.domain.model.AppPreferences
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
  val preferences: Flow<AppPreferences>

  suspend fun setSoundEnabled(enabled: Boolean)

  suspend fun setHapticsEnabled(enabled: Boolean)

  suspend fun setUseBengaliNumerals(enabled: Boolean)

  suspend fun setDarkThemeEnabled(enabled: Boolean)

  suspend fun setDynamicColorEnabled(enabled: Boolean)

  suspend fun setSelectedAvatarId(avatarId: Int)

  suspend fun setGuestSession(enabled: Boolean, displayName: String? = null)
}
