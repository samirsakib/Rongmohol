package com.example.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.config.UiState
import com.example.domain.model.MatchRecord
import com.example.domain.repository.MatchHistoryRepository
import com.example.domain.usecase.HomeOverviewData
import com.example.domain.usecase.ObserveLobbyOverviewUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
  observeLobbyOverviewUseCase: ObserveLobbyOverviewUseCase,
  private val matchHistoryRepository: MatchHistoryRepository,
) : ViewModel() {

  val uiState: StateFlow<UiState<HomeOverviewData>> = observeLobbyOverviewUseCase()
    .map<HomeOverviewData, UiState<HomeOverviewData>> { UiState.Success(it) }
    .catch { error ->
      emit(UiState.Error(error.message ?: "তথ্য লোড করতে সমস্যা হয়েছে"))
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = UiState.Loading,
    )

  fun addSampleMatchRecord() {
    viewModelScope.launch {
      val now = System.currentTimeMillis()
      val isWin = (now % 2L) == 0L
      matchHistoryRepository.recordSampleMatch(
        MatchRecord(
          modeName = "অনুশীলন আসর",
          opponentSummary = "৩ জন রোবট প্রতিপক্ষ",
          playerScore = if (isWin) 120 else 85,
          opponentScore = if (isWin) 90 else 115,
          isVictory = isWin,
          playedAtEpochMs = now,
        )
      )
    }
  }

  fun clearMatchHistory() {
    viewModelScope.launch {
      matchHistoryRepository.clearAllMatches()
    }
  }
}
