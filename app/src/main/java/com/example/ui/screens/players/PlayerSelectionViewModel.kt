package com.example.ui.screens.players

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlayerSelectionUiState(
  val defaultName: String = "খেলোয়াড়",
  val selectedAvatarId: Int = 0,
  val useBengaliNumerals: Boolean = true,
)

@HiltViewModel
class PlayerSelectionViewModel @Inject constructor(
  private val preferencesRepository: PreferencesRepository,
  authRepository: AuthRepository,
) : ViewModel() {

  val uiState: StateFlow<PlayerSelectionUiState> = combine(
    preferencesRepository.preferences,
    authRepository.currentUser,
  ) { prefs, user ->
    PlayerSelectionUiState(
      defaultName = user?.displayName ?: "খেলোয়াড়",
      selectedAvatarId = prefs.selectedAvatarId,
      useBengaliNumerals = prefs.useBengaliNumerals,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000L),
    initialValue = PlayerSelectionUiState(),
  )

  fun selectAvatar(avatarId: Int) {
    viewModelScope.launch {
      preferencesRepository.setSelectedAvatarId(avatarId)
    }
  }
}
