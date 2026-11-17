package com.example.data.remote.firestore

import com.example.domain.model.GameRoom
import com.example.domain.model.PlayerProfile
import com.example.domain.model.RoomStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class FirestoreUserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val displayName: String = "",
  val matchesPlayed: Long = 0L,
  val matchesWon: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun toDomain(): PlayerProfile = PlayerProfile(
    userId = userId.ifBlank { id },
    displayName = displayName,
    matchesPlayed = matchesPlayed,
    matchesWon = matchesWon,
  )
}

@IgnoreExtraProperties
data class FirestoreGameRoom(
  @DocumentId val id: String = "",
  val roomCode: String = "",
  val hostUid: String = "",
  val hostName: String = "",
  val title: String = "",
  val status: String = RoomStatus.WAITING.rawValue,
  val maxPlayers: Long = 4L,
  val playerUids: List<String> = emptyList(),
  val isPublic: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun toDomain(): GameRoom = GameRoom(
    id = id,
    roomCode = roomCode,
    hostUid = hostUid,
    hostName = hostName,
    title = title,
    status = RoomStatus.fromRaw(status),
    maxPlayers = maxPlayers.toInt().coerceIn(2, 4),
    playerUids = playerUids,
    isPublic = isPublic,
  )
}
