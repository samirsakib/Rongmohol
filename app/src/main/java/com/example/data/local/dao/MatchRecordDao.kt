package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.MatchRecordEntity
import kotlinx.coroutines.flow.Flow

data class MatchSummaryTuple(
  val totalMatches: Int,
  val totalWins: Int,
)

@Dao
interface MatchRecordDao {
  @Query("SELECT * FROM match_records ORDER BY playedAtEpochMs DESC LIMIT :limit")
  fun observeRecentMatches(limit: Int): Flow<List<MatchRecordEntity>>

  @Query(
    "SELECT COUNT(*) AS totalMatches, " +
      "COALESCE(SUM(CASE WHEN isVictory = 1 THEN 1 ELSE 0 END), 0) AS totalWins " +
      "FROM match_records"
  )
  fun observeSummary(): Flow<MatchSummaryTuple>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMatch(entity: MatchRecordEntity)

  @Query("DELETE FROM match_records")
  suspend fun deleteAllMatches()
}
