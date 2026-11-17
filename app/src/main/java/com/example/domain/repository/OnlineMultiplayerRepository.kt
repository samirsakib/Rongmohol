package com.example.domain.repository

import com.example.domain.game.CardColor
import com.example.domain.game.DeckType
import com.example.domain.model.CommandAck
import com.example.domain.model.CreatedRoomInfo
import com.example.domain.model.JoinedRoomInfo
import com.example.domain.model.LeaderboardEntry
import com.example.domain.model.OnlineRoomSnapshot
import com.example.domain.model.OnlineRoomSummary
import kotlinx.coroutines.flow.Flow

interface OnlineMultiplayerRepository {
  fun observePublicWaitingRooms(): Flow<List<OnlineRoomSummary>>

  fun observeLeaderboard(limit: Int = 25): Flow<List<LeaderboardEntry>>

  fun observeRoomSession(roomId: String, myUid: String): Flow<OnlineRoomSnapshot?>

  fun observeRtdbConnection(): Flow<Boolean>

  suspend fun updateCloudProfile(displayName: String, avatarId: Int): Result<Unit>

  suspend fun createRoom(
    title: String,
    displayName: String,
    avatarId: Int,
    deckType: DeckType,
    maxPlayers: Int,
    isPrivate: Boolean,
    startingCards: Int,
  ): Result<CreatedRoomInfo>

  suspend fun joinRoomByCode(
    roomCodeOrId: String,
    isRoomId: Boolean,
    displayName: String,
    avatarId: Int,
  ): Result<JoinedRoomInfo>

  suspend fun setPlayerReady(roomId: String, ready: Boolean): Result<Unit>

  suspend fun leaveRoom(roomId: String): Result<Unit>

  suspend fun startMatch(roomId: String): Result<Unit>

  suspend fun submitPlayCard(
    roomId: String,
    actionId: String,
    expectedVersion: Int,
    cardId: String,
    chosenColor: CardColor?,
    targetPlayerId: String?,
  ): Result<CommandAck>

  suspend fun submitDrawCard(
    roomId: String,
    actionId: String,
    expectedVersion: Int,
  ): Result<CommandAck>

  suspend fun submitDeclareLastCard(
    roomId: String,
    actionId: String,
    expectedVersion: Int,
  ): Result<CommandAck>

  suspend fun claimTurnTimeout(roomId: String): Result<Unit>
}
