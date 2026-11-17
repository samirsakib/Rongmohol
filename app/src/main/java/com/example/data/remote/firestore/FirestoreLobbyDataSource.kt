package com.example.data.remote.firestore

import com.example.core.config.AppConfig
import com.example.domain.model.GameRoom
import com.example.domain.model.PlayerProfile
import com.example.domain.model.RoomStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

@Singleton
class FirestoreLobbyDataSource @Inject constructor(
  private val firestore: FirebaseFirestore,
  private val auth: FirebaseAuth,
) {
  fun observePublicRooms(): Flow<List<GameRoom>> {
    val path = GAME_ROOMS_COLLECTION
    return firestore.collection(GAME_ROOMS_COLLECTION)
      .whereEqualTo("isPublic", true)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(
          FirestoreGameRoom::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        ).map { it.toDomain() }
      }
      .catch { error ->
        logSnapshotErrorIfSignedIn(error, OperationType.LIST, path)
        throw error
      }
  }

  fun observeMyRooms(userId: String): Flow<List<GameRoom>> {
    val path = GAME_ROOMS_COLLECTION
    return firestore.collection(GAME_ROOMS_COLLECTION)
      .whereArrayContains("playerUids", userId)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(
          FirestoreGameRoom::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        ).map { it.toDomain() }
      }
      .catch { error ->
        logSnapshotErrorIfSignedIn(error, OperationType.LIST, path)
        throw error
      }
  }

  fun observePlayerProfile(userId: String): Flow<PlayerProfile?> {
    val docRef = firestore.collection(USERS_COLLECTION).document(userId)
    return docRef.snapshots()
      .map { snapshot ->
        snapshot.toObject(
          FirestoreUserProfile::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        )?.toDomain()
      }
      .catch { error ->
        logSnapshotErrorIfSignedIn(error, OperationType.GET, docRef.path)
        throw error
      }
  }

  suspend fun ensurePlayerProfile(userId: String, displayName: String): Result<Unit> {
    val docRef = firestore.collection(USERS_COLLECTION).document(userId)
    return try {
      val existing = docRef.get().await()
      if (!existing.exists()) {
        val safeName = displayName.trim().ifBlank { "খেলোয়াড়" }.take(50)
        val payload = mapOf(
          "userId" to userId,
          "displayName" to safeName,
          "matchesPlayed" to 0L,
          "matchesWon" to 0L,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        docRef.set(payload).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun createRoom(
    userId: String,
    hostName: String,
    title: String,
    maxPlayers: Int,
    isPublic: Boolean,
  ): Result<String> {
    val docRef = firestore.collection(GAME_ROOMS_COLLECTION).document()
    val safeHostName = hostName.trim().ifBlank { "আয়োজক" }.take(50)
    val safeTitle = title.trim().ifBlank { "নতুন আসর" }.take(AppConfig.MAX_ROOM_TITLE_LENGTH)
    val clampedPlayers = maxPlayers.coerceIn(AppConfig.MIN_PLAYERS, AppConfig.DEFAULT_MAX_PLAYERS)
    val payload = mapOf(
      "roomCode" to generateRoomCode(),
      "hostUid" to userId,
      "hostName" to safeHostName,
      "title" to safeTitle,
      "status" to RoomStatus.WAITING.rawValue,
      "maxPlayers" to clampedPlayers.toLong(),
      "playerUids" to listOf(userId),
      "isPublic" to isPublic,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    return try {
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun joinRoom(roomId: String, userId: String): Result<Unit> {
    val docRef = firestore.collection(GAME_ROOMS_COLLECTION).document(roomId)
    return try {
      docRef.update(
        mapOf(
          "playerUids" to FieldValue.arrayUnion(userId),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteRoom(roomId: String): Result<Unit> {
    val docRef = firestore.collection(GAME_ROOMS_COLLECTION).document(roomId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  private fun logSnapshotErrorIfSignedIn(
    error: Throwable,
    operationType: OperationType,
    path: String,
  ) {
    val firestoreError = generateSequence(error) { it.cause }
      .filterIsInstance<FirebaseFirestoreException>()
      .firstOrNull()
    if (firestoreError == null && error is CancellationException) return
    if (auth.currentUser != null) {
      handleFirestoreError(firestoreError ?: error, operationType, path)
    }
  }

  private fun generateRoomCode(): String {
    val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    return buildString(AppConfig.ROOM_CODE_LENGTH) {
      repeat(AppConfig.ROOM_CODE_LENGTH) {
        append(alphabet[Random.nextInt(alphabet.length)])
      }
    }
  }

  private companion object {
    const val USERS_COLLECTION = "users"
    const val GAME_ROOMS_COLLECTION = "game_rooms"
  }
}
