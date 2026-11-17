package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.MatchRecord

@Entity(tableName = "match_records")
data class MatchRecordEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val modeName: String,
  val opponentSummary: String,
  val playerScore: Int,
  val opponentScore: Int,
  val isVictory: Boolean,
  val playedAtEpochMs: Long,
) {
  fun toDomain(): MatchRecord = MatchRecord(
    id = id,
    modeName = modeName,
    opponentSummary = opponentSummary,
    playerScore = playerScore,
    opponentScore = opponentScore,
    isVictory = isVictory,
    playedAtEpochMs = playedAtEpochMs,
  )

  companion object {
    fun fromDomain(record: MatchRecord): MatchRecordEntity = MatchRecordEntity(
      id = record.id,
      modeName = record.modeName,
      opponentSummary = record.opponentSummary,
      playerScore = record.playerScore,
      opponentScore = record.opponentScore,
      isVictory = record.isVictory,
      playedAtEpochMs = record.playedAtEpochMs,
    )
  }
}
