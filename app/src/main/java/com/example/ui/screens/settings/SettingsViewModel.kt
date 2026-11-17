package com.example.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.AppPreferences
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.AuthenticatedUser
import com.example.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
  val preferences: AppPreferences = AppPreferences(),
  val user: AuthenticatedUser? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
  private val preferencesRepository: PreferencesRepository,
  authRepository: AuthRepository,
) : ViewModel() {

  val uiState: StateFlow<SettingsUiState> = combine(
    preferencesRepository.preferences,
    authRepository.currentUser,
  ) { prefs, user ->
    SettingsUiState(preferences = prefs, user = user)
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000L),
    initialValue = SettingsUiState(),
  )

  fun setSoundEnabled(enabled: Boolean) {
    viewModelScope.launch { preferencesRepository.setSoundEnabled(enabled) }
  }

  fun setHapticsEnabled(enabled: Boolean) {
    viewModelScope.launch { preferencesRepository.setHapticsEnabled(enabled) }
  }

  fun setUseBengaliNumerals(enabled: Boolean) {
    viewModelScope.launch { preferencesRepository.setUseBengaliNumerals(enabled) }
  }

  fun setDarkThemeEnabled(enabled: Boolean) {
    viewModelScope.launch { preferencesRepository.setDarkThemeEnabled(enabled) }
  }

  fun setDynamicColorEnabled(enabled: Boolean) {
    viewModelScope.launch { preferencesRepository.setDynamicColorEnabled(enabled) }
  }

  fun leaveGuestMode() {
    viewModelScope.launch { preferencesRepository.setGuestSession(enabled = false) }
  }
}
