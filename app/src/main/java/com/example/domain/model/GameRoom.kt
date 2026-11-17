package com.example.domain.model

enum class RoomStatus(val rawValue: String) {
  WAITING("waiting"),
  PLAYING("playing"),
  FINISHED("finished");

  companion object {
    fun fromRaw(raw: String): RoomStatus =
      entries.firstOrNull { it.rawValue == raw } ?: WAITING
  }
}

data class GameRoom(
  val id: String,
  val roomCode: String,
  val hostUid: String,
  val hostName: String,
  val title: String,
  val status: RoomStatus,
  val maxPlayers: Int,
  val playerUids: List<String>,
  val isPublic: Boolean,
)
