package com.example.data.repository

import com.example.data.remote.firestore.FirestoreLobbyDataSource
import com.example.domain.model.GameRoom
import com.example.domain.model.PlayerProfile
import com.example.domain.repository.MultiplayerLobbyRepository
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class MultiplayerLobbyRepositoryImpl @Inject constructor(
  private val dataSource: FirestoreLobbyDataSource,
  private val auth: FirebaseAuth,
) : MultiplayerLobbyRepository {
  private fun requireUserId(): String =
    auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

  override fun observePublicRooms(): Flow<List<GameRoom>> = dataSource.observePublicRooms()

  override fun observeMyRooms(userId: String): Flow<List<GameRoom>> =
    dataSource.observeMyRooms(userId)

  override fun observePlayerProfile(userId: String): Flow<PlayerProfile?> =
    dataSource.observePlayerProfile(userId)

  override suspend fun ensurePlayerProfile(userId: String, displayName: String): Result<Unit> {
    requireUserId()
    return dataSource.ensurePlayerProfile(userId, displayName)
  }

  override suspend fun createRoom(
    userId: String,
    hostName: String,
    title: String,
    maxPlayers: Int,
    isPublic: Boolean,
  ): Result<String> {
    val activeUid = requireUserId()
    return dataSource.createRoom(activeUid, hostName, title, maxPlayers, isPublic)
  }

  override suspend fun joinRoom(roomId: String, userId: String): Result<Unit> {
    val activeUid = requireUserId()
    return dataSource.joinRoom(roomId, activeUid)
  }

  override suspend fun deleteRoom(roomId: String): Result<Unit> {
    requireUserId()
    return dataSource.deleteRoom(roomId)
  }
}
