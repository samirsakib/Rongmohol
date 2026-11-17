package com.example.data.repository

import com.example.data.local.datastore.UserPreferencesDataSource
import com.example.domain.model.AppPreferences
import com.example.domain.repository.PreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class PreferencesRepositoryImpl @Inject constructor(
  private val dataSource: UserPreferencesDataSource,
) : PreferencesRepository {
  override val preferences: Flow<AppPreferences> = dataSource.preferencesFlow

  override suspend fun setSoundEnabled(enabled: Boolean) =
    dataSource.updateSoundEnabled(enabled)

  override suspend fun setHapticsEnabled(enabled: Boolean) =
    dataSource.updateHapticsEnabled(enabled)

  override suspend fun setUseBengaliNumerals(enabled: Boolean) =
    dataSource.updateUseBengaliNumerals(enabled)

  override suspend fun setDarkThemeEnabled(enabled: Boolean) =
    dataSource.updateDarkThemeEnabled(enabled)

  override suspend fun setDynamicColorEnabled(enabled: Boolean) =
    dataSource.updateDynamicColorEnabled(enabled)

  override suspend fun setSelectedAvatarId(avatarId: Int) =
    dataSource.updateSelectedAvatarId(avatarId)

  override suspend fun setGuestSession(enabled: Boolean, displayName: String?) =
    dataSource.updateGuestSession(enabled, displayName)
}
