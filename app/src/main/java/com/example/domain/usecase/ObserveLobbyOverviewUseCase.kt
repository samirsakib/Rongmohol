package com.example.domain.usecase

import com.example.domain.model.AppPreferences
import com.example.domain.model.MatchRecord
import com.example.domain.repository.LocalMatchSummary
import com.example.domain.repository.MatchHistoryRepository
import com.example.domain.repository.PreferencesRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class HomeOverviewData(
  val preferences: AppPreferences,
  val summary: LocalMatchSummary,
  val recentMatches: List<MatchRecord>,
)

class ObserveLobbyOverviewUseCase @Inject constructor(
  private val preferencesRepository: PreferencesRepository,
  private val matchHistoryRepository: MatchHistoryRepository,
) {
  operator fun invoke(): Flow<HomeOverviewData> = combine(
    preferencesRepository.preferences,
    matchHistoryRepository.observeSummary(),
    matchHistoryRepository.observeRecentMatches(),
  ) { prefs, summary, recent ->
    HomeOverviewData(
      preferences = prefs,
      summary = summary,
      recentMatches = recent,
    )
  }
}
