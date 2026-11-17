package com.example.data.remote.rtdb

import com.example.domain.game.CardColor
import com.example.domain.game.CardType
import com.example.domain.game.DeckType
import com.example.domain.game.GameCard
import com.example.domain.model.LeaderboardEntry
import com.example.domain.model.OnlineMatchResultItem
import com.example.domain.model.OnlinePublicMatchState
import com.example.domain.model.OnlineRoomMeta
import com.example.domain.model.OnlineRoomPlayer
import com.example.domain.model.OnlineRoomSummary
import com.example.domain.model.RoomStatus
import com.google.firebase.database.DataSnapshot

internal object RtdbCardMapper {

  fun parseCard(snapshot: DataSnapshot): GameCard? {
    val id = snapshot.child("id").getValue(String::class.java) ?: return null
    val colorName = snapshot.child("color").getValue(String::class.java) ?: "RED"
    val typeName = snapshot.child("type").getValue(String::class.java) ?: "NUMBER"
    val number = snapshot.child("number").getValue(Long::class.java)?.toInt()
    return parseCardFields(id = id, colorName = colorName, typeName = typeName, number = number)
  }

  internal fun parseCardFields(
    id: String,
    colorName: String,
    typeName: String,
    number: Int?,
  ): GameCard {
    val color = CardColor.entries.firstOrNull { it.name == colorName } ?: CardColor.RED
    val type = when (typeName) {
      "MYSTERY", "MYSTERY_CARD" -> CardType.MYSTERY_CARD
      else -> CardType.entries.firstOrNull { it.name == typeName } ?: CardType.NUMBER
    }

    return when (type) {
      CardType.NUMBER -> GameCard.NumberCard(
        id = id,
        color = if (color == CardColor.WILD) CardColor.RED else color,
        number = (number ?: 0).coerceIn(0, 9),
      )
      CardType.SKIP,
      CardType.REVERSE,
      CardType.DRAW_TWO -> GameCard.ActionCard(
        id = id,
        color = if (color == CardColor.WILD) CardColor.RED else color,
        type = type,
      )
      CardType.WILD,
      CardType.WILD_DRAW_FOUR -> GameCard.ActionCard(
        id = id,
        color = CardColor.WILD,
        type = type,
      )
      CardType.THUNDERBOLT_PLUS_THREE,
      CardType.HAND_SWAP,
      CardType.BOUNCE_BACK,
      CardType.EVERYONE_DRAW_ONE,
      CardType.MYSTERY_CARD -> GameCard.SpecialCard(
        id = id,
        color = color,
        type = type,
      )
    }
  }

  fun parseRoomSummary(snapshot: DataSnapshot): OnlineRoomSummary? {
    val roomId = snapshot.child("roomId").getValue(String::class.java) ?: snapshot.key ?: return null
    val roomCode = snapshot.child("roomCode").getValue(String::class.java) ?: return null
    val title = snapshot.child("title").getValue(String::class.java) ?: "অনলাইন আসর"
    val hostUid = snapshot.child("hostUid").getValue(String::class.java) ?: ""
    val hostName = snapshot.child("hostName").getValue(String::class.java) ?: "আয়োজক"
    val statusRaw = snapshot.child("status").getValue(String::class.java) ?: "waiting"
    val deckRaw = snapshot.child("deckType").getValue(String::class.java) ?: "CLASSIC"
    val maxPlayers = snapshot.child("maxPlayers").getValue(Long::class.java)?.toInt() ?: 4
    val playerCount = snapshot.child("playerCount").getValue(Long::class.java)?.toInt() ?: 1
    val createdAt = snapshot.child("createdAt").getValue(Long::class.java) ?: 0L

    return OnlineRoomSummary(
      roomId = roomId,
      roomCode = roomCode,
      title = title,
      hostUid = hostUid,
      hostName = hostName,
      status = RoomStatus.fromRaw(statusRaw),
      deckType = if (deckRaw == "SPECIAL") DeckType.SPECIAL else DeckType.CLASSIC,
      maxPlayers = maxPlayers.coerceIn(2, 4),
      playerCount = playerCount.coerceIn(0, 4),
      createdAt = createdAt,
    )
  }

  fun parseRoomMeta(snapshot: DataSnapshot): OnlineRoomMeta? {
    if (!snapshot.exists()) return null
    val roomId = snapshot.child("roomId").getValue(String::class.java) ?: return null
    val roomCode = snapshot.child("roomCode").getValue(String::class.java) ?: ""
    val title = snapshot.child("title").getValue(String::class.java) ?: "অনলাইন আসর"
    val hostUid = snapshot.child("hostUid").getValue(String::class.java) ?: ""
    val hostName = snapshot.child("hostName").getValue(String::class.java) ?: "আয়োজক"
    val statusRaw = snapshot.child("status").getValue(String::class.java) ?: "waiting"
    val deckRaw = snapshot.child("deckType").getValue(String::class.java) ?: "CLASSIC"
    val maxPlayers = snapshot.child("maxPlayers").getValue(Long::class.java)?.toInt() ?: 4
    val isPrivate = snapshot.child("isPrivate").getValue(Boolean::class.java) ?: false
    val startingCards = snapshot.child("startingCards").getValue(Long::class.java)?.toInt() ?: 7
    val turnTimeoutSeconds =
      snapshot.child("turnTimeoutSeconds").getValue(Long::class.java)?.toInt() ?: 25

    return OnlineRoomMeta(
      roomId = roomId,
      roomCode = roomCode,
      title = title,
      hostUid = hostUid,
      hostName = hostName,
      status = RoomStatus.fromRaw(statusRaw),
      deckType = if (deckRaw == "SPECIAL") DeckType.SPECIAL else DeckType.CLASSIC,
      maxPlayers = maxPlayers.coerceIn(2, 4),
      isPrivate = isPrivate,
      startingCards = startingCards,
      turnTimeoutSeconds = turnTimeoutSeconds,
    )
  }

  fun parsePlayers(snapshot: DataSnapshot): List<OnlineRoomPlayer> {
    if (!snapshot.exists()) return emptyList()
    return snapshot.children.mapNotNull { child ->
      val uid = child.child("uid").getValue(String::class.java) ?: child.key ?: return@mapNotNull null
      OnlineRoomPlayer(
        uid = uid,
        displayName = child.child("displayName").getValue(String::class.java) ?: "খেলোয়াড়",
        avatarId = (child.child("avatarId").getValue(Long::class.java)?.toInt() ?: 0).coerceIn(0, 5),
        seatIndex = child.child("seatIndex").getValue(Long::class.java)?.toInt() ?: 0,
        ready = child.child("ready").getValue(Boolean::class.java) ?: false,
        connected = child.child("connected").getValue(Boolean::class.java) ?: false,
        lastSeenAt = child.child("lastSeenAt").getValue(Long::class.java) ?: 0L,
      )
    }.sortedBy { it.seatIndex }
  }

  fun parsePublicState(snapshot: DataSnapshot): OnlinePublicMatchState? {
    if (!snapshot.exists()) return null
    val topCard = parseCard(snapshot.child("topDiscardCard")) ?: return null
    val deckRaw = snapshot.child("deckType").getValue(String::class.java) ?: "CLASSIC"
    val activeColorRaw = snapshot.child("activeColor").getValue(String::class.java) ?: "RED"
    val activeColor = CardColor.entries.firstOrNull { it.name == activeColorRaw } ?: CardColor.RED

    val seatOrder = snapshot.child("seatOrder").children.mapNotNull { it.getValue(String::class.java) }
    val counts = snapshot.child("playerCardCounts").children.mapNotNull { child ->
      val k = child.key ?: return@mapNotNull null
      val v = child.getValue(Long::class.java)?.toInt() ?: 0
      k to v
    }.toMap()
    val declared = snapshot.child("declaredLastCard").children.mapNotNull { child ->
      val k = child.key ?: return@mapNotNull null
      val v = child.getValue(Boolean::class.java) ?: false
      k to v
    }.toMap()

    val results = snapshot.child("results").children.mapNotNull { child ->
      val uid = child.child("uid").getValue(String::class.java) ?: child.key ?: return@mapNotNull null
      OnlineMatchResultItem(
        uid = uid,
        displayName = child.child("displayName").getValue(String::class.java) ?: "খেলোয়াড়",
        avatarId = (child.child("avatarId").getValue(Long::class.java)?.toInt() ?: 0).coerceIn(0, 5),
        cardsRemaining = child.child("cardsRemaining").getValue(Long::class.java)?.toInt() ?: 0,
        handPointsRemaining = child.child("handPointsRemaining").getValue(Long::class.java)?.toInt() ?: 0,
        awardedPoints = child.child("awardedPoints").getValue(Long::class.java)?.toInt() ?: 0,
        isWinner = child.child("isWinner").getValue(Boolean::class.java) ?: false,
      )
    }.sortedByDescending { it.awardedPoints }

    val lastEventNode = snapshot.child("lastEvent")

    return OnlinePublicMatchState(
      version = snapshot.child("version").getValue(Long::class.java)?.toInt() ?: 1,
      deckType = if (deckRaw == "SPECIAL") DeckType.SPECIAL else DeckType.CLASSIC,
      topDiscardCard = topCard,
      activeColor = activeColor,
      drawPileCount = snapshot.child("drawPileCount").getValue(Long::class.java)?.toInt() ?: 0,
      discardPileCount = snapshot.child("discardPileCount").getValue(Long::class.java)?.toInt() ?: 1,
      pendingDrawPenalty = snapshot.child("pendingDrawPenalty").getValue(Long::class.java)?.toInt() ?: 0,
      isClockwise = (snapshot.child("direction").getValue(Long::class.java)?.toInt() ?: 1) >= 0,
      currentTurnIndex = snapshot.child("currentTurnIndex").getValue(Long::class.java)?.toInt() ?: 0,
      currentTurnUid = snapshot.child("currentTurnUid").getValue(String::class.java) ?: "",
      turnDeadlineAt = snapshot.child("turnDeadlineAt").getValue(Long::class.java) ?: 0L,
      turnTimeoutSeconds = snapshot.child("turnTimeoutSeconds").getValue(Long::class.java)?.toInt() ?: 25,
      seatOrder = seatOrder,
      playerCardCounts = counts,
      declaredLastCard = declared,
      lastEventType = lastEventNode.child("type").getValue(String::class.java) ?: "",
      lastEventMessage = lastEventNode.child("messageBengali").getValue(String::class.java) ?: "",
      lastEventTimestamp = lastEventNode.child("timestamp").getValue(Long::class.java) ?: 0L,
      winnerUid = snapshot.child("winnerUid").getValue(String::class.java),
      results = results,
    )
  }

  fun parseHand(snapshot: DataSnapshot): List<GameCard> {
    if (!snapshot.exists()) return emptyList()
    return snapshot.children.mapNotNull { parseCard(it) }
  }

  fun parseLeaderboard(snapshot: DataSnapshot): List<LeaderboardEntry> {
    if (!snapshot.exists()) return emptyList()
    val raw = snapshot.children.mapNotNull { child ->
      val uid = child.child("uid").getValue(String::class.java) ?: child.key ?: return@mapNotNull null
      LeaderboardEntry(
        rank = 0,
        uid = uid,
        displayName = child.child("displayName").getValue(String::class.java) ?: "খেলোয়াড়",
        avatarId = (child.child("avatarId").getValue(Long::class.java)?.toInt() ?: 0).coerceIn(0, 5),
        matchesPlayed = child.child("matchesPlayed").getValue(Long::class.java)?.toInt() ?: 0,
        matchesWon = child.child("matchesWon").getValue(Long::class.java)?.toInt() ?: 0,
        totalPoints = child.child("totalPoints").getValue(Long::class.java)?.toInt() ?: 0,
      )
    }.sortedWith(
      compareByDescending<LeaderboardEntry> { it.totalPoints }
        .thenByDescending { it.matchesWon }
    )
    return raw.mapIndexed { idx, entry -> entry.copy(rank = idx + 1) }
  }
}
