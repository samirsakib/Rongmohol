package com.example.data.repository

import com.example.data.remote.rtdb.RtdbMultiplayerDataSource
import com.example.domain.game.CardColor
import com.example.domain.game.DeckType
import com.example.domain.model.CommandAck
import com.example.domain.model.CreatedRoomInfo
import com.example.domain.model.JoinedRoomInfo
import com.example.domain.model.LeaderboardEntry
import com.example.domain.model.OnlineRoomSnapshot
import com.example.domain.model.OnlineRoomSummary
import com.example.domain.repository.OnlineMultiplayerRepository
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class OnlineMultiplayerRepositoryImpl @Inject constructor(
  private val dataSource: RtdbMultiplayerDataSource,
  private val auth: FirebaseAuth,
) : OnlineMultiplayerRepository {

  private fun requireAuthenticatedUid(): String =
    auth.currentUser?.uid ?: error("অনলাইন আসরের জন্য গুগল অ্যাকাউন্টে প্রবেশ আবশ্যক।")

  override fun observePublicWaitingRooms(): Flow<List<OnlineRoomSummary>> {
    requireAuthenticatedUid()
    return dataSource.observePublicWaitingRooms()
  }

  override fun observeLeaderboard(limit: Int): Flow<List<LeaderboardEntry>> {
    requireAuthenticatedUid()
    return dataSource.observeLeaderboard(limit)
  }

  override fun observeRoomSession(roomId: String, myUid: String): Flow<OnlineRoomSnapshot?> {
    val activeUid = requireAuthenticatedUid()
    require(activeUid == myUid) { "ইউজার সেশন অমিল।" }
    return dataSource.observeRoomSession(roomId, activeUid)
  }

  override fun observeRtdbConnection(): Flow<Boolean> = dataSource.observeRtdbConnection()

  override suspend fun updateCloudProfile(displayName: String, avatarId: Int): Result<Unit> {
    requireAuthenticatedUid()
    return dataSource.updateCloudProfile(displayName, avatarId)
  }

  override suspend fun createRoom(
    title: String,
    displayName: String,
    avatarId: Int,
    deckType: DeckType,
    maxPlayers: Int,
    isPrivate: Boolean,
    startingCards: Int,
  ): Result<CreatedRoomInfo> {
    requireAuthenticatedUid()
    return dataSource.createRoom(
      title = title,
      displayName = displayName,
      avatarId = avatarId,
      deckType = deckType,
      maxPlayers = maxPlayers,
      isPrivate = isPrivate,
      startingCards = startingCards,
    )
  }

  override suspend fun joinRoomByCode(
    roomCodeOrId: String,
    isRoomId: Boolean,
    displayName: String,
    avatarId: Int,
  ): Result<JoinedRoomInfo> {
    requireAuthenticatedUid()
    return dataSource.joinRoomByCode(roomCodeOrId, isRoomId, displayName, avatarId)
  }

  override suspend fun setPlayerReady(roomId: String, ready: Boolean): Result<Unit> {
    requireAuthenticatedUid()
    return dataSource.setPlayerReady(roomId, ready)
  }

  override suspend fun leaveRoom(roomId: String): Result<Unit> {
    requireAuthenticatedUid()
    return dataSource.leaveRoom(roomId)
  }

  override suspend fun startMatch(roomId: String): Result<Unit> {
    requireAuthenticatedUid()
    return dataSource.startMatch(roomId)
  }

  override suspend fun submitPlayCard(
    roomId: String,
    actionId: String,
    expectedVersion: Int,
    cardId: String,
    chosenColor: CardColor?,
    targetPlayerId: String?,
  ): Result<CommandAck> {
    requireAuthenticatedUid()
    return dataSource.submitPlayCard(
      roomId = roomId,
      actionId = actionId,
      expectedVersion = expectedVersion,
      cardId = cardId,
      chosenColor = chosenColor,
      targetPlayerId = targetPlayerId,
    )
  }

  override suspend fun submitDrawCard(
    roomId: String,
    actionId: String,
    expectedVersion: Int,
  ): Result<CommandAck> {
    requireAuthenticatedUid()
    return dataSource.submitDrawCard(roomId, actionId, expectedVersion)
  }

  override suspend fun submitDeclareLastCard(
    roomId: String,
    actionId: String,
    expectedVersion: Int,
  ): Result<CommandAck> {
    requireAuthenticatedUid()
    return dataSource.submitDeclareLastCard(roomId, actionId, expectedVersion)
  }

  override suspend fun claimTurnTimeout(roomId: String): Result<Unit> {
    requireAuthenticatedUid()
    return dataSource.claimTurnTimeout(roomId)
  }
}
