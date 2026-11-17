package com.example.data.remote.rtdb

import com.example.domain.game.CardColor
import com.example.domain.game.DeckType
import com.example.domain.game.GameCard
import com.example.domain.model.CommandAck
import com.example.domain.model.CreatedRoomInfo
import com.example.domain.model.JoinedRoomInfo
import com.example.domain.model.LeaderboardEntry
import com.example.domain.model.OnlinePublicMatchState
import com.example.domain.model.OnlineRoomMeta
import com.example.domain.model.OnlineRoomPlayer
import com.example.domain.model.OnlineRoomSnapshot
import com.example.domain.model.OnlineRoomSummary
import com.example.domain.model.RoomStatus
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.functions.FirebaseFunctions
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.tasks.await

@Singleton
class RtdbMultiplayerDataSource @Inject constructor(
  private val database: FirebaseDatabase,
  private val functions: FirebaseFunctions,
) {
  fun observePublicWaitingRooms(): Flow<List<OnlineRoomSummary>> = callbackFlow {
    val ref = database.getReference("publicRoomList")
      .orderByChild("createdAt")
      .limitToLast(30)

    val listener = object : ValueEventListener {
      override fun onDataChange(snapshot: DataSnapshot) {
        val rooms = snapshot.children
          .mapNotNull { RtdbCardMapper.parseRoomSummary(it) }
          .filter { it.status == RoomStatus.WAITING }
          .sortedByDescending { it.createdAt }
        trySend(rooms)
      }

      override fun onCancelled(error: DatabaseError) {
        close(error.toException())
      }
    }
    ref.addValueEventListener(listener)
    awaitClose { ref.removeEventListener(listener) }
  }

  fun observeLeaderboard(limit: Int): Flow<List<LeaderboardEntry>> = callbackFlow {
    val ref = database.getReference("leaderboard")
      .orderByChild("totalPoints")
      .limitToLast(limit.coerceIn(5, 50))

    val listener = object : ValueEventListener {
      override fun onDataChange(snapshot: DataSnapshot) {
        trySend(RtdbCardMapper.parseLeaderboard(snapshot))
      }

      override fun onCancelled(error: DatabaseError) {
        close(error.toException())
      }
    }
    ref.addValueEventListener(listener)
    awaitClose { ref.removeEventListener(listener) }
  }

  fun observeRtdbConnection(): Flow<Boolean> = callbackFlow {
    val ref = database.getReference(".info/connected")
    val listener = object : ValueEventListener {
      override fun onDataChange(snapshot: DataSnapshot) {
        val connected = snapshot.getValue(Boolean::class.java) ?: false
        trySend(connected)
      }

      override fun onCancelled(error: DatabaseError) {
        trySend(false)
      }
    }
    ref.addValueEventListener(listener)
    awaitClose { ref.removeEventListener(listener) }
  }

  fun observeRoomSession(roomId: String, myUid: String): Flow<OnlineRoomSnapshot?> {
    val metaFlow = observeNode("rooms/$roomId/meta") { RtdbCardMapper.parseRoomMeta(it) }
    val playersFlow = observeNode("rooms/$roomId/players") { RtdbCardMapper.parsePlayers(it) }
    val publicFlow = observeNode("rooms/$roomId/publicState") { RtdbCardMapper.parsePublicState(it) }
    val myHandFlow = observeNode("rooms/$roomId/playerHands/$myUid") { RtdbCardMapper.parseHand(it) }
    val connectionFlow = observePresenceLifecycle(roomId, myUid)

    return combine(
      metaFlow,
      playersFlow,
      publicFlow,
      myHandFlow,
      connectionFlow,
    ) { meta: OnlineRoomMeta?,
        players: List<OnlineRoomPlayer>,
        publicState: OnlinePublicMatchState?,
        myHand: List<GameCard>,
        isConnected: Boolean ->
      if (meta == null) {
        null
      } else {
        OnlineRoomSnapshot(
          meta = meta,
          players = players,
          publicState = publicState,
          myHand = myHand,
          isSocketConnected = isConnected,
        )
      }
    }
  }

  private fun observePresenceLifecycle(roomId: String, uid: String): Flow<Boolean> = callbackFlow {
    val infoRef = database.getReference(".info/connected")
    val playerRef = database.getReference("rooms/$roomId/players/$uid")
    val listener = object : ValueEventListener {
      override fun onDataChange(snapshot: DataSnapshot) {
        val connected = snapshot.getValue(Boolean::class.java) ?: false
        trySend(connected)
        if (connected) {
          playerRef.child("connected").onDisconnect().setValue(false)
          playerRef.child("lastSeenAt").onDisconnect().setValue(ServerValue.TIMESTAMP)
          playerRef.child("connected").setValue(true)
          playerRef.child("lastSeenAt").setValue(ServerValue.TIMESTAMP)
        }
      }

      override fun onCancelled(error: DatabaseError) {
        trySend(false)
      }
    }
    infoRef.addValueEventListener(listener)
    awaitClose {
      infoRef.removeEventListener(listener)
      playerRef.child("connected").onDisconnect().cancel()
      playerRef.child("lastSeenAt").onDisconnect().cancel()
    }
  }

  private fun <T> observeNode(
    path: String,
    mapper: (DataSnapshot) -> T,
  ): Flow<T> = callbackFlow {
    val ref: DatabaseReference = database.getReference(path)
    val listener = object : ValueEventListener {
      override fun onDataChange(snapshot: DataSnapshot) {
        trySend(mapper(snapshot))
      }

      override fun onCancelled(error: DatabaseError) {
        close(error.toException())
      }
    }
    ref.addValueEventListener(listener)
    awaitClose { ref.removeEventListener(listener) }
  }

  suspend fun updateCloudProfile(displayName: String, avatarId: Int): Result<Unit> = runCallable("updatePlayerProfile", mapOf(
    "displayName" to displayName,
    "avatarId" to avatarId,
  )) { Unit }

  suspend fun createRoom(
    title: String,
    displayName: String,
    avatarId: Int,
    deckType: DeckType,
    maxPlayers: Int,
    isPrivate: Boolean,
    startingCards: Int,
  ): Result<CreatedRoomInfo> = runCallable(
    name = "createMultiplayerRoom",
    payload = mapOf(
      "title" to title,
      "displayName" to displayName,
      "avatarId" to avatarId,
      "deckType" to deckType.name,
      "maxPlayers" to maxPlayers,
      "isPrivate" to isPrivate,
      "startingCards" to startingCards,
    ),
  ) { resultMap ->
    CreatedRoomInfo(
      roomId = resultMap["roomId"] as? String ?: error("Missing roomId"),
      roomCode = resultMap["roomCode"] as? String ?: "",
    )
  }

  suspend fun joinRoomByCode(
    roomCodeOrId: String,
    isRoomId: Boolean,
    displayName: String,
    avatarId: Int,
  ): Result<JoinedRoomInfo> = runCallable(
    name = "joinRoomByCode",
    payload = buildMap {
      if (isRoomId) {
        put("roomId", roomCodeOrId)
      } else {
        put("roomCode", roomCodeOrId.trim().uppercase())
      }
      put("displayName", displayName)
      put("avatarId", avatarId)
    },
  ) { resultMap ->
    JoinedRoomInfo(
      roomId = resultMap["roomId"] as? String ?: error("Missing roomId"),
      roomCode = resultMap["roomCode"] as? String ?: "",
      reconnected = resultMap["reconnected"] as? Boolean ?: false,
    )
  }

  suspend fun setPlayerReady(roomId: String, ready: Boolean): Result<Unit> = runCallable(
    name = "setPlayerReady",
    payload = mapOf("roomId" to roomId, "ready" to ready),
  ) { Unit }

  suspend fun leaveRoom(roomId: String): Result<Unit> = runCallable(
    name = "leaveMultiplayerRoom",
    payload = mapOf("roomId" to roomId),
  ) { Unit }

  suspend fun startMatch(roomId: String): Result<Unit> = runCallable(
    name = "startMultiplayerMatch",
    payload = mapOf("roomId" to roomId),
  ) { Unit }

  suspend fun submitPlayCard(
    roomId: String,
    actionId: String,
    expectedVersion: Int,
    cardId: String,
    chosenColor: CardColor?,
    targetPlayerId: String?,
  ): Result<CommandAck> = runCallable(
    name = "submitGameCommand",
    payload = buildMap {
      put("roomId", roomId)
      put("actionId", actionId)
      put("expectedVersion", expectedVersion)
      put("command", "PLAY_CARD")
      put("cardId", cardId)
      if (chosenColor != null && chosenColor != CardColor.WILD) {
        put("chosenColor", chosenColor.name)
      }
      if (!targetPlayerId.isNullOrBlank()) {
        put("targetPlayerId", targetPlayerId)
      }
    },
  ) { map ->
    CommandAck(
      version = (map["version"] as? Number)?.toInt() ?: expectedVersion,
      duplicate = map["duplicate"] as? Boolean ?: false,
      matchFinished = map["matchFinished"] as? Boolean ?: false,
    )
  }

  suspend fun submitDrawCard(
    roomId: String,
    actionId: String,
    expectedVersion: Int,
  ): Result<CommandAck> = runCallable(
    name = "submitGameCommand",
    payload = mapOf(
      "roomId" to roomId,
      "actionId" to actionId,
      "expectedVersion" to expectedVersion,
      "command" to "DRAW_CARD",
    ),
  ) { map ->
    CommandAck(
      version = (map["version"] as? Number)?.toInt() ?: expectedVersion,
      duplicate = map["duplicate"] as? Boolean ?: false,
      matchFinished = map["matchFinished"] as? Boolean ?: false,
    )
  }

  suspend fun submitDeclareLastCard(
    roomId: String,
    actionId: String,
    expectedVersion: Int,
  ): Result<CommandAck> = runCallable(
    name = "submitGameCommand",
    payload = mapOf(
      "roomId" to roomId,
      "actionId" to actionId,
      "expectedVersion" to expectedVersion,
      "command" to "DECLARE_LAST_CARD",
    ),
  ) { map ->
    CommandAck(
      version = (map["version"] as? Number)?.toInt() ?: expectedVersion,
      duplicate = map["duplicate"] as? Boolean ?: false,
      matchFinished = map["matchFinished"] as? Boolean ?: false,
    )
  }

  suspend fun claimTurnTimeout(roomId: String): Result<Unit> = runCallable(
    name = "claimTurnTimeout",
    payload = mapOf("roomId" to roomId),
  ) { Unit }

  private suspend inline fun <T> runCallable(
    name: String,
    payload: Map<String, Any>,
    crossinline mapper: (Map<*, *>) -> T,
  ): Result<T> {
    return try {
      val result = functions.getHttpsCallable(name).call(payload).await()
      @Suppress("UNCHECKED_CAST")
      val map = (result.getData() as? Map<*, *>) ?: emptyMap<String, Any>()
      Result.success(mapper(map))
    } catch (e: kotlinx.coroutines.CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
