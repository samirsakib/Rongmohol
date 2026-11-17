package com.example.domain.model

import com.example.domain.game.CardColor
import com.example.domain.game.DeckType
import com.example.domain.game.GameCard

data class OnlineRoomSummary(
  val roomId: String,
  val roomCode: String,
  val title: String,
  val hostUid: String,
  val hostName: String,
  val status: RoomStatus,
  val deckType: DeckType,
  val maxPlayers: Int,
  val playerCount: Int,
  val createdAt: Long,
)

data class OnlineRoomMeta(
  val roomId: String,
  val roomCode: String,
  val title: String,
  val hostUid: String,
  val hostName: String,
  val status: RoomStatus,
  val deckType: DeckType,
  val maxPlayers: Int,
  val isPrivate: Boolean,
  val startingCards: Int,
  val turnTimeoutSeconds: Int,
)

data class OnlineRoomPlayer(
  val uid: String,
  val displayName: String,
  val avatarId: Int,
  val seatIndex: Int,
  val ready: Boolean,
  val connected: Boolean,
  val lastSeenAt: Long,
)

data class OnlineMatchResultItem(
  val uid: String,
  val displayName: String,
  val avatarId: Int,
  val cardsRemaining: Int,
  val handPointsRemaining: Int,
  val awardedPoints: Int,
  val isWinner: Boolean,
)

data class OnlinePublicMatchState(
  val version: Int,
  val deckType: DeckType,
  val topDiscardCard: GameCard,
  val activeColor: CardColor,
  val drawPileCount: Int,
  val discardPileCount: Int,
  val pendingDrawPenalty: Int,
  val isClockwise: Boolean,
  val currentTurnIndex: Int,
  val currentTurnUid: String,
  val turnDeadlineAt: Long,
  val turnTimeoutSeconds: Int,
  val seatOrder: List<String>,
  val playerCardCounts: Map<String, Int>,
  val declaredLastCard: Map<String, Boolean>,
  val lastEventType: String,
  val lastEventMessage: String,
  val lastEventTimestamp: Long,
  val winnerUid: String?,
  val results: List<OnlineMatchResultItem>,
)

data class OnlineRoomSnapshot(
  val meta: OnlineRoomMeta,
  val players: List<OnlineRoomPlayer>,
  val publicState: OnlinePublicMatchState?,
  val myHand: List<GameCard>,
  val isSocketConnected: Boolean,
)

data class CreatedRoomInfo(
  val roomId: String,
  val roomCode: String,
)

data class JoinedRoomInfo(
  val roomId: String,
  val roomCode: String,
  val reconnected: Boolean,
)

data class CommandAck(
  val version: Int,
  val duplicate: Boolean,
  val matchFinished: Boolean,
)

data class LeaderboardEntry(
  val rank: Int,
  val uid: String,
  val displayName: String,
  val avatarId: Int,
  val matchesPlayed: Int,
  val matchesWon: Int,
  val totalPoints: Int,
)
