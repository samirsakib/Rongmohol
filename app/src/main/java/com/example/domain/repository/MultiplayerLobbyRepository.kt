package com.example.domain.repository

import com.example.domain.model.GameRoom
import com.example.domain.model.PlayerProfile
import kotlinx.coroutines.flow.Flow

interface MultiplayerLobbyRepository {
  fun observePublicRooms(): Flow<List<GameRoom>>

  fun observeMyRooms(userId: String): Flow<List<GameRoom>>

  fun observePlayerProfile(userId: String): Flow<PlayerProfile?>

  suspend fun ensurePlayerProfile(userId: String, displayName: String): Result<Unit>

  suspend fun createRoom(
    userId: String,
    hostName: String,
    title: String,
    maxPlayers: Int,
    isPublic: Boolean,
  ): Result<String>

  suspend fun joinRoom(roomId: String, userId: String): Result<Unit>

  suspend fun deleteRoom(roomId: String): Result<Unit>
}
