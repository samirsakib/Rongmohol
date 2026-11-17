package com.example.data.repository

import com.example.data.local.dao.MatchRecordDao
import com.example.data.local.entity.MatchRecordEntity
import com.example.domain.model.MatchRecord
import com.example.domain.repository.LocalMatchSummary
import com.example.domain.repository.MatchHistoryRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class MatchHistoryRepositoryImpl @Inject constructor(
  private val matchRecordDao: MatchRecordDao,
) : MatchHistoryRepository {
  override fun observeRecentMatches(limit: Int): Flow<List<MatchRecord>> =
    matchRecordDao.observeRecentMatches(limit).map { entities ->
      entities.map { it.toDomain() }
    }

  override fun observeSummary(): Flow<LocalMatchSummary> =
    matchRecordDao.observeSummary().map { tuple ->
      LocalMatchSummary(
        totalMatches = tuple.totalMatches,
        totalWins = tuple.totalWins,
      )
    }

  override suspend fun recordSampleMatch(record: MatchRecord) {
    matchRecordDao.insertMatch(MatchRecordEntity.fromDomain(record))
  }

  override suspend fun clearAllMatches() {
    matchRecordDao.deleteAllMatches()
  }
}
