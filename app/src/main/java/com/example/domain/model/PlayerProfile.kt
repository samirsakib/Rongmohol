package com.example.domain.model

data class PlayerProfile(
  val userId: String,
  val displayName: String,
  val matchesPlayed: Long,
  val matchesWon: Long,
)
