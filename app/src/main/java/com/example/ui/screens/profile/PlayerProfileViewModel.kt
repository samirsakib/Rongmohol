package com.example.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.MatchRecord
import com.example.domain.model.PlayerProfile
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.AuthenticatedUser
import com.example.domain.repository.LocalMatchSummary
import com.example.domain.repository.MatchHistoryRepository
import com.example.domain.repository.MultiplayerLobbyRepository
import com.example.domain.repository.OnlineMultiplayerRepository
import com.example.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlayerProfileUiState(
  val user: AuthenticatedUser? = null,
  val isGuestMode: Boolean = false,
  val guestDisplayName: String = "অতিথি খেলোয়াড়",
  val cloudProfile: PlayerProfile? = null,
  val localSummary: LocalMatchSummary = LocalMatchSummary(),
  val recentMatches: List<MatchRecord> = emptyList(),
  val selectedAvatarId: Int = 0,
  val useBengaliNumerals: Boolean = true,
  val cloudSyncMessage: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlayerProfileViewModel @Inject constructor(
  private val authRepository: AuthRepository,
  private val lobbyRepository: MultiplayerLobbyRepository,
  private val onlineMultiplayerRepository: OnlineMultiplayerRepository,
  matchHistoryRepository: MatchHistoryRepository,
  private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

  private val syncMessageFlow = MutableStateFlow<String?>(null)

  val uiState: StateFlow<PlayerProfileUiState> = authRepository.currentUser
    .flatMapLatest { user ->
      val cloudFlow = if (user != null) {
        lobbyRepository.observePlayerProfile(user.uid).catch { emit(null) }
      } else {
        flowOf(null)
      }
      combine(
        cloudFlow,
        matchHistoryRepository.observeSummary(),
        matchHistoryRepository.observeRecentMatches(10),
        preferencesRepository.preferences,
        syncMessageFlow,
      ) { cloud, summary, recent, prefs, syncMsg ->
        PlayerProfileUiState(
          user = user,
          isGuestMode = user == null && prefs.guestModeEnabled,
          guestDisplayName = prefs.guestDisplayName,
          cloudProfile = cloud,
          localSummary = summary,
          recentMatches = recent,
          selectedAvatarId = prefs.selectedAvatarId,
          useBengaliNumerals = prefs.useBengaliNumerals,
          cloudSyncMessage = syncMsg,
        )
      }
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = PlayerProfileUiState(),
    )

  fun selectAvatar(avatarId: Int) {
    viewModelScope.launch {
      preferencesRepository.setSelectedAvatarId(avatarId)
      val user = authRepository.getCurrentUserSnapshot() ?: return@launch
      val prefs = preferencesRepository.preferences.first()
      val currentName = prefs.guestDisplayName.ifBlank { user.displayName }
      onlineMultiplayerRepository.updateCloudProfile(currentName, avatarId)
    }
  }

  fun updateDisplayName(newName: String) {
    val cleanName = newName.trim().take(24)
    if (cleanName.isBlank()) return
    viewModelScope.launch {
      val prefs = preferencesRepository.preferences.first()
      preferencesRepository.setGuestSession(
        enabled = authRepository.getCurrentUserSnapshot() == null && prefs.guestModeEnabled,
        displayName = cleanName,
      )
      if (authRepository.getCurrentUserSnapshot() != null) {
        onlineMultiplayerRepository.updateCloudProfile(cleanName, prefs.selectedAvatarId)
          .onSuccess { syncMessageFlow.value = "ক্লাউড প্রোফাইল হালনাগাদ হয়েছে।" }
          .onFailure { syncMessageFlow.value = "নাম স্থানীয়ভাবে সংরক্ষিত হয়েছে।" }
      } else {
        syncMessageFlow.value = "অতিথি নাম সংরক্ষিত হয়েছে।"
      }
    }
  }

  fun clearSyncMessage() {
    syncMessageFlow.value = null
  }
}
