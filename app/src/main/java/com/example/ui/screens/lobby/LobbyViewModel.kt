package com.example.ui.screens.lobby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.config.UiState
import com.example.core.network.NetworkConnectivityObserver
import com.example.domain.game.DeckType
import com.example.domain.model.AppPreferences
import com.example.domain.model.LeaderboardEntry
import com.example.domain.model.OnlineRoomSummary
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.OnlineMultiplayerRepository
import com.example.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LobbyContent(
  val isOnlineAccount: Boolean,
  val isNetworkConnected: Boolean,
  val currentUserId: String,
  val displayName: String,
  val avatarId: Int,
  val useBengaliNumerals: Boolean,
  val rooms: List<OnlineRoomSummary>,
  val leaderboard: List<LeaderboardEntry>,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LobbyViewModel @Inject constructor(
  private val multiplayerRepository: OnlineMultiplayerRepository,
  private val authRepository: AuthRepository,
  private val preferencesRepository: PreferencesRepository,
  networkObserver: NetworkConnectivityObserver,
) : ViewModel() {

  private val _actionError = MutableStateFlow<String?>(null)
  val actionError: StateFlow<String?> = _actionError.asStateFlow()

  private val _isSubmitting = MutableStateFlow(false)
  val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

  private val _navigateToRoomId = MutableStateFlow<String?>(null)
  val navigateToRoomId: StateFlow<String?> = _navigateToRoomId.asStateFlow()

  val uiState: StateFlow<UiState<LobbyContent>> = authRepository.currentUser
    .flatMapLatest { user ->
      if (user == null) {
        combine(
          preferencesRepository.preferences,
          networkObserver.isOnlineFlow,
        ) { prefs: AppPreferences, online: Boolean ->
          UiState.Success(
            LobbyContent(
              isOnlineAccount = false,
              isNetworkConnected = online,
              currentUserId = "guest_local",
              displayName = prefs.guestDisplayName,
              avatarId = prefs.selectedAvatarId,
              useBengaliNumerals = prefs.useBengaliNumerals,
              rooms = emptyList(),
              leaderboard = emptyList(),
            )
          ) as UiState<LobbyContent>
        }
      } else {
        combine(
          multiplayerRepository.observePublicWaitingRooms().catch { e ->
            if (e is CancellationException) throw e
            emit(emptyList())
          },
          multiplayerRepository.observeLeaderboard(25).catch { e ->
            if (e is CancellationException) throw e
            emit(emptyList())
          },
          preferencesRepository.preferences,
          networkObserver.isOnlineFlow,
        ) { rooms, leaderboard, prefs, online ->
          UiState.Success(
            LobbyContent(
              isOnlineAccount = true,
              isNetworkConnected = online,
              currentUserId = user.uid,
              displayName = user.displayName,
              avatarId = prefs.selectedAvatarId,
              useBengaliNumerals = prefs.useBengaliNumerals,
              rooms = rooms,
              leaderboard = leaderboard,
            )
          ) as UiState<LobbyContent>
        }.catch { error ->
          if (error is CancellationException) throw error
          emit(UiState.Error("অনলাইন আসর লোড করা যায়নি। সংযোগ পরীক্ষা করুন।"))
        }
      }
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = UiState.Loading,
    )

  fun createRoom(
    title: String,
    maxPlayers: Int,
    deckType: DeckType,
    isPrivate: Boolean,
    startingCards: Int,
  ) {
    val user = authRepository.getCurrentUserSnapshot()
    if (user == null) {
      _actionError.value = "রুম তৈরি করতে গুগল অ্যাকাউন্ট দিয়ে সাইন-ইন করুন।"
      return
    }
    val safeTitle = title.trim().ifBlank { "রঙমহল অনলাইন আসর" }
    val avatarId = (uiState.value as? UiState.Success)?.data?.avatarId ?: 0

    viewModelScope.launch {
      _isSubmitting.value = true
      _actionError.value = null
      val result = multiplayerRepository.createRoom(
        title = safeTitle,
        displayName = user.displayName,
        avatarId = avatarId,
        deckType = deckType,
        maxPlayers = maxPlayers.coerceIn(2, 4),
        isPrivate = isPrivate,
        startingCards = startingCards,
      )
      _isSubmitting.value = false
      result.onSuccess { info ->
        _navigateToRoomId.value = info.roomId
      }.onFailure { err ->
        _actionError.value = err.localizedMessage ?: "রুম তৈরি করা যায়নি। ইন্টারনেট ও সার্ভার পরীক্ষা করুন।"
      }
    }
  }

  fun joinByCode(roomCode: String) {
    val user = authRepository.getCurrentUserSnapshot()
    if (user == null) {
      _actionError.value = "রুমে যোগ দিতে গুগল অ্যাকাউন্ট দিয়ে সাইন-ইন করুন।"
      return
    }
    val cleanCode = roomCode.trim().uppercase()
    if (cleanCode.length < 4) {
      _actionError.value = "সঠিক রুম কোড লিখুন (যেমন: AB12CD)।"
      return
    }
    val avatarId = (uiState.value as? UiState.Success)?.data?.avatarId ?: 0
    viewModelScope.launch {
      _isSubmitting.value = true
      _actionError.value = null
      val result = multiplayerRepository.joinRoomByCode(
        roomCodeOrId = cleanCode,
        isRoomId = false,
        displayName = user.displayName,
        avatarId = avatarId,
      )
      _isSubmitting.value = false
      result.onSuccess { joined ->
        _navigateToRoomId.value = joined.roomId
      }.onFailure { err ->
        _actionError.value = err.localizedMessage ?: "এই কোডের রুম পাওয়া যায়নি বা আসর পূর্ণ হয়েছে।"
      }
    }
  }

  fun joinPublicRoom(roomId: String) {
    val user = authRepository.getCurrentUserSnapshot()
    if (user == null) {
      _actionError.value = "রুমে যোগ দিতে গুগল অ্যাকাউন্ট দিয়ে সাইন-ইন করুন।"
      return
    }
    val avatarId = (uiState.value as? UiState.Success)?.data?.avatarId ?: 0
    viewModelScope.launch {
      _isSubmitting.value = true
      _actionError.value = null
      val result = multiplayerRepository.joinRoomByCode(
        roomCodeOrId = roomId,
        isRoomId = true,
        displayName = user.displayName,
        avatarId = avatarId,
      )
      _isSubmitting.value = false
      result.onSuccess { joined ->
        _navigateToRoomId.value = joined.roomId
      }.onFailure { err ->
        _actionError.value = err.localizedMessage ?: "রুমে যোগ দেওয়া যায়নি।"
      }
    }
  }

  fun onGuestLinkedWithGoogle() {
    viewModelScope.launch {
      preferencesRepository.setGuestSession(enabled = false)
    }
  }

  fun consumeNavigation() {
    _navigateToRoomId.value = null
  }

  fun clearError() {
    _actionError.value = null
  }

  private companion object {
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
