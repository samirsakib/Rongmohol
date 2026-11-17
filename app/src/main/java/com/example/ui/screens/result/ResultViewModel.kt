package com.example.ui.screens.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.MatchRecord
import com.example.domain.repository.MatchHistoryRepository
import com.example.domain.repository.PreferencesRepository
import com.example.ui.screens.mode.GameModeId
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ResultViewModel @Inject constructor(
  private val matchHistoryRepository: MatchHistoryRepository,
  preferencesRepository: PreferencesRepository,
) : ViewModel() {

  val useBengaliNumerals: StateFlow<Boolean> = preferencesRepository.preferences
    .map { it.useBengaliNumerals }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = true,
    )

  private var recordedMatchKey: String? = null

  fun recordMatchOnce(
    matchKey: String,
    mode: GameModeId,
    playerCount: Int,
    playerScore: Int,
    opponentScore: Int,
    isVictory: Boolean,
  ) {
    if (recordedMatchKey == matchKey) return
    recordedMatchKey = matchKey
    viewModelScope.launch {
      val modeLabel = when (mode) {
        GameModeId.CLASSIC -> "ধ্রুপদী রঙমহল"
        GameModeId.BLITZ -> "ঝটপট চাল"
        GameModeId.TACTICAL -> "নকশা চ্যালেঞ্জ"
        GameModeId.ONLINE -> "অনলাইন আসর"
      }
      matchHistoryRepository.recordSampleMatch(
        MatchRecord(
          modeName = modeLabel,
          opponentSummary = "${(playerCount - 1).coerceAtLeast(1)} জন প্রতিপক্ষ",
          playerScore = playerScore,
          opponentScore = opponentScore,
          isVictory = isVictory,
          playedAtEpochMs = System.currentTimeMillis(),
        )
      )
    }
  }
}
