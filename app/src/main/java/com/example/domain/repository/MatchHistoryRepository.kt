package com.example.domain.repository

import com.example.domain.model.MatchRecord
import kotlinx.coroutines.flow.Flow

data class LocalMatchSummary(
  val totalMatches: Int = 0,
  val totalWins: Int = 0,
)

interface MatchHistoryRepository {
  fun observeRecentMatches(limit: Int = 10): Flow<List<MatchRecord>>

  fun observeSummary(): Flow<LocalMatchSummary>

  suspend fun recordSampleMatch(record: MatchRecord)

  suspend fun clearAllMatches()
}
