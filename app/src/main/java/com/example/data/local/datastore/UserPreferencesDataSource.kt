package com.example.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.domain.model.AppPreferences
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

@Singleton
class UserPreferencesDataSource @Inject constructor(
  private val dataStore: DataStore<Preferences>,
) {
  private object Keys {
    val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
    val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
    val USE_BENGALI_NUMERALS = booleanPreferencesKey("use_bengali_numerals")
    val DARK_THEME_ENABLED = booleanPreferencesKey("dark_theme_enabled")
    val DYNAMIC_COLOR_ENABLED = booleanPreferencesKey("dynamic_color_enabled")
    val SELECTED_AVATAR_ID = intPreferencesKey("selected_avatar_id")
    val GUEST_MODE_ENABLED = booleanPreferencesKey("guest_mode_enabled")
    val GUEST_DISPLAY_NAME = stringPreferencesKey("guest_display_name")
  }

  val preferencesFlow: Flow<AppPreferences> = dataStore.data
    .catch { exception ->
      if (exception is IOException) {
        emit(emptyPreferences())
      } else {
        throw exception
      }
    }
    .map { prefs ->
      AppPreferences(
        soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
        hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
        useBengaliNumerals = prefs[Keys.USE_BENGALI_NUMERALS] ?: true,
        darkThemeEnabled = prefs[Keys.DARK_THEME_ENABLED] ?: false,
        dynamicColorEnabled = prefs[Keys.DYNAMIC_COLOR_ENABLED] ?: false,
        selectedAvatarId = (prefs[Keys.SELECTED_AVATAR_ID] ?: 0).coerceIn(0, 5),
        guestModeEnabled = prefs[Keys.GUEST_MODE_ENABLED] ?: false,
        guestDisplayName = prefs[Keys.GUEST_DISPLAY_NAME]?.takeIf { it.isNotBlank() } ?: "অতিথি খেলোয়াড়",
      )
    }

  suspend fun updateSoundEnabled(enabled: Boolean) {
    dataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
  }

  suspend fun updateHapticsEnabled(enabled: Boolean) {
    dataStore.edit { it[Keys.HAPTICS_ENABLED] = enabled }
  }

  suspend fun updateUseBengaliNumerals(enabled: Boolean) {
    dataStore.edit { it[Keys.USE_BENGALI_NUMERALS] = enabled }
  }

  suspend fun updateDarkThemeEnabled(enabled: Boolean) {
    dataStore.edit { it[Keys.DARK_THEME_ENABLED] = enabled }
  }

  suspend fun updateDynamicColorEnabled(enabled: Boolean) {
    dataStore.edit { it[Keys.DYNAMIC_COLOR_ENABLED] = enabled }
  }

  suspend fun updateSelectedAvatarId(avatarId: Int) {
    dataStore.edit { it[Keys.SELECTED_AVATAR_ID] = avatarId.coerceIn(0, 5) }
  }

  suspend fun updateGuestSession(enabled: Boolean, displayName: String?) {
    dataStore.edit { prefs ->
      prefs[Keys.GUEST_MODE_ENABLED] = enabled
      if (displayName != null) {
        val clean = displayName.trim().take(30).ifBlank { "অতিথি খেলোয়াড়" }
        prefs[Keys.GUEST_DISPLAY_NAME] = clean
      }
    }
  }
}
