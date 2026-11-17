package com.example.domain.model

data class MatchRecord(
  val id: Long = 0L,
  val modeName: String,
  val opponentSummary: String,
  val playerScore: Int,
  val opponentScore: Int,
  val isVictory: Boolean,
  val playedAtEpochMs: Long,
)
