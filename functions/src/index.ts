import { randomInt } from "crypto";
import { initializeApp } from "firebase-admin/app";
import { getDatabase, ServerValue } from "firebase-admin/database";
import { onValueUpdated } from "firebase-functions/v2/database";
import { CallableRequest, HttpsError, onCall } from "firebase-functions/v2/https";
import {
  applyAuthoritativeDrawCard,
  applyAuthoritativePlayCard,
  applyDeclareLastCard,
  createInitialMatchNode,
  DEFAULT_TURN_TIMEOUT_SECONDS,
  DISCONNECTED_GRACE_SECONDS,
} from "./gameEngine";
import { AuthoritativeRoomNode, DeckType, PlayerMatchResult, RoomPlayer } from "./types";

initializeApp();

const ROOM_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

function requireUid(request: CallableRequest<unknown>): string {
  if (!request.auth?.uid) {
    throw new HttpsError("unauthenticated", "গুগল অ্যাকাউন্টে প্রবেশ করা আবশ্যক।");
  }
  return request.auth.uid;
}

function generateRoomCode(): string {
  let code = "";
  for (let i = 0; i < 6; i++) {
    code += ROOM_CODE_ALPHABET[randomInt(0, ROOM_CODE_ALPHABET.length)];
  }
  return code;
}

function sanitizeDisplayName(raw: unknown, fallback = "খেলোয়াড়"): string {
  if (typeof raw !== "string") return fallback;
  const trimmed = raw.trim().slice(0, 40);
  return trimmed.length > 0 ? trimmed : fallback;
}

function sanitizeAvatarId(raw: unknown): number {
  const num = typeof raw === "number" ? Math.floor(raw) : 0;
  return Math.min(Math.max(num, 0), 5);
}

async function applyMatchResultsToLeaderboard(results: Record<string, PlayerMatchResult>) {
  const db = getDatabase();
  const updates = Object.values(results).map(async (res) => {
    const profileStatsRef = db.ref(`profiles/${res.uid}/stats`);
    await profileStatsRef.transaction((current) => {
      const prev = (current as Record<string, number> | null) ?? {};
      return {
        matchesPlayed: (prev.matchesPlayed ?? 0) + 1,
        matchesWon: (prev.matchesWon ?? 0) + (res.isWinner ? 1 : 0),
        totalPoints: (prev.totalPoints ?? 0) + res.awardedPoints,
        currentStreak: res.isWinner ? (prev.currentStreak ?? 0) + 1 : 0,
        updatedAt: ServerValue.TIMESTAMP,
      };
    });

    const lbRef = db.ref(`leaderboard/${res.uid}`);
    await lbRef.transaction((current) => {
      const prev = (current as Record<string, unknown> | null) ?? {};
      const prevPlayed = typeof prev.matchesPlayed === "number" ? prev.matchesPlayed : 0;
      const prevWon = typeof prev.matchesWon === "number" ? prev.matchesWon : 0;
      const prevPoints = typeof prev.totalPoints === "number" ? prev.totalPoints : 0;
      return {
        uid: res.uid,
        displayName: res.displayName,
        avatarId: res.avatarId,
        matchesPlayed: prevPlayed + 1,
        matchesWon: prevWon + (res.isWinner ? 1 : 0),
        totalPoints: prevPoints + res.awardedPoints,
        updatedAt: ServerValue.TIMESTAMP,
      };
    });
  });
  await Promise.all(updates);
}

export const updatePlayerProfile = onCall(async (request) => {
  const uid = requireUid(request);
  const data = (request.data as Record<string, unknown>) ?? {};
  const displayName = sanitizeDisplayName(data.displayName);
  const avatarId = sanitizeAvatarId(data.avatarId);

  const db = getDatabase();
  const profileRef = db.ref(`profiles/${uid}`);
  await profileRef.update({
    uid,
    displayName,
    avatarId,
    updatedAt: ServerValue.TIMESTAMP,
  });

  // Also refresh display name/avatar on existing leaderboard entry without altering server points
  const lbRef = db.ref(`leaderboard/${uid}`);
  await lbRef.transaction((current) => {
    if (!current) return current;
    return {
      ...(current as Record<string, unknown>),
      displayName,
      avatarId,
      updatedAt: ServerValue.TIMESTAMP,
    };
  });

  return { ok: true, uid, displayName, avatarId };
});

export const createMultiplayerRoom = onCall(async (request) => {
  const uid = requireUid(request);
  const data = (request.data as Record<string, unknown>) ?? {};
  const title = sanitizeDisplayName(data.title, "রঙমহল অনলাইন আসর").slice(0, 60);
  const hostName = sanitizeDisplayName(data.displayName ?? request.auth?.token?.name, "আয়োজক");
  const avatarId = sanitizeAvatarId(data.avatarId);
  const deckType: DeckType = data.deckType === "SPECIAL" ? "SPECIAL" : "CLASSIC";
  const maxPlayersRaw = typeof data.maxPlayers === "number" ? Math.floor(data.maxPlayers) : 4;
  const maxPlayers = Math.min(Math.max(maxPlayersRaw, 2), 4);
  const isPrivate = Boolean(data.isPrivate);
  const startingCards = data.startingCards === 5 ? 5 : 7;

  const db = getDatabase();
  const roomRef = db.ref("rooms").push();
  const roomId = roomRef.key!;
  let roomCode = "";

  // Atomically reserve unique 6-character invite code
  for (let attempt = 0; attempt < 8; attempt++) {
    const candidate = generateRoomCode();
    const codeRef = db.ref(`roomCodes/${candidate}`);
    const tx = await codeRef.transaction((existing) => {
      if (existing !== null) return undefined;
      return roomId;
    });
    if (tx.committed) {
      roomCode = candidate;
      break;
    }
  }
  if (!roomCode) {
    throw new HttpsError("internal", "রুম কোড তৈরি করা যায়নি। আবার চেষ্টা করুন।");
  }

  const now = Date.now();
  const hostSeat: RoomPlayer = {
    uid,
    displayName: hostName,
    avatarId,
    seatIndex: 0,
    ready: true,
    connected: true,
    lastSeenAt: now,
  };

  const roomNode: AuthoritativeRoomNode = {
    meta: {
      roomId,
      roomCode,
      title,
      hostUid: uid,
      hostName,
      status: "waiting",
      deckType,
      maxPlayers,
      isPrivate,
      startingCards,
      turnTimeoutSeconds: DEFAULT_TURN_TIMEOUT_SECONDS,
      createdAt: now,
      updatedAt: now,
    },
    players: {
      [uid]: hostSeat,
    },
  };

  await roomRef.set(roomNode);

  if (!isPrivate) {
    await db.ref(`publicRoomList/${roomId}`).set({
      roomId,
      roomCode,
      title,
      hostUid: uid,
      hostName,
      status: "waiting",
      deckType,
      maxPlayers,
      playerCount: 1,
      createdAt: now,
    });
  }

  return { roomId, roomCode };
});

export const joinRoomByCode = onCall(async (request) => {
  const uid = requireUid(request);
  const data = (request.data as Record<string, unknown>) ?? {};
  const rawCode = typeof data.roomCode === "string" ? data.roomCode.trim().toUpperCase() : "";
  const explicitRoomId = typeof data.roomId === "string" ? data.roomId.trim() : "";
  const displayName = sanitizeDisplayName(data.displayName ?? request.auth?.token?.name);
  const avatarId = sanitizeAvatarId(data.avatarId);

  const db = getDatabase();
  let targetRoomId = explicitRoomId;

  if (!targetRoomId) {
    if (rawCode.length < 4 || rawCode.length > 12) {
      throw new HttpsError("invalid-argument", "সঠিক রুম কোড লিখুন।");
    }
    const codeSnap = await db.ref(`roomCodes/${rawCode}`).get();
    if (!codeSnap.exists()) {
      throw new HttpsError("not-found", "এই কোডের কোনো রুম পাওয়া যায়নি।");
    }
    targetRoomId = String(codeSnap.val());
  }

  const roomRef = db.ref(`rooms/${targetRoomId}`);
  const tx = await roomRef.transaction((current) => {
    const room = current as AuthoritativeRoomNode | null;
    if (!room || !room.meta) {
      return undefined;
    }

    const existingPlayers = room.players ?? {};
    const now = Date.now();

    // Idempotent reconnection if already seated in room (even while playing)
    if (existingPlayers[uid]) {
      room.players[uid] = {
        ...existingPlayers[uid],
        displayName,
        avatarId,
        connected: true,
        lastSeenAt: now,
      };
      room.meta.updatedAt = now;
      return room;
    }

    if (room.meta.status !== "waiting") {
      return undefined;
    }

    const seats = Object.values(existingPlayers);
    if (seats.length >= room.meta.maxPlayers) {
      return undefined;
    }

    const usedIndices = new Set(seats.map((s) => s.seatIndex));
    let nextSeat = 0;
    while (usedIndices.has(nextSeat)) nextSeat++;

    room.players = {
      ...existingPlayers,
      [uid]: {
        uid,
        displayName,
        avatarId,
        seatIndex: nextSeat,
        ready: false,
        connected: true,
        lastSeenAt: now,
      },
    };
    room.meta.updatedAt = now;
    return room;
  });

  if (!tx.committed || !tx.snapshot.exists()) {
    throw new HttpsError("failed-precondition", "রুমে প্রবেশ করা সম্ভব হয়নি (রুম পূর্ণ বা শুরু হয়ে গেছে)।");
  }

  const updated = tx.snapshot.val() as AuthoritativeRoomNode;
  if (!updated.meta.isPrivate) {
    await db.ref(`publicRoomList/${targetRoomId}`).update({
      playerCount: Object.keys(updated.players ?? {}).length,
      status: updated.meta.status,
    });
  }

  return {
    roomId: targetRoomId,
    roomCode: updated.meta.roomCode,
    reconnected: updated.meta.status === "playing",
  };
});

export const setPlayerReady = onCall(async (request) => {
  const uid = requireUid(request);
  const data = (request.data as Record<string, unknown>) ?? {};
  const roomId = typeof data.roomId === "string" ? data.roomId.trim() : "";
  const ready = Boolean(data.ready);

  if (!roomId) {
    throw new HttpsError("invalid-argument", "রুম আইডি পাওয়া যায়নি।");
  }

  const db = getDatabase();
  const roomRef = db.ref(`rooms/${roomId}`);
  const tx = await roomRef.transaction((current) => {
    const room = current as AuthoritativeRoomNode | null;
    if (!room || !room.meta || room.meta.status !== "waiting" || !room.players?.[uid]) {
      return undefined;
    }
    room.players[uid] = {
      ...room.players[uid],
      ready,
      connected: true,
      lastSeenAt: Date.now(),
    };
    room.meta.updatedAt = Date.now();
    return room;
  });

  if (!tx.committed) {
    throw new HttpsError("failed-precondition", "প্রস্তুত অবস্থা পরিবর্তন করা যায়নি।");
  }
  return { ok: true, ready };
});

export const leaveMultiplayerRoom = onCall(async (request) => {
  const uid = requireUid(request);
  const data = (request.data as Record<string, unknown>) ?? {};
  const roomId = typeof data.roomId === "string" ? data.roomId.trim() : "";
  if (!roomId) {
    throw new HttpsError("invalid-argument", "রুম আইডি পাওয়া যায়নি।");
  }

  const db = getDatabase();
  const roomRef = db.ref(`rooms/${roomId}`);
  let roomCodeToDelete: string | null = null;
  let wasPublic = false;

  const tx = await roomRef.transaction((current) => {
    const room = current as AuthoritativeRoomNode | null;
    if (!room || !room.meta || !room.players?.[uid]) {
      return undefined;
    }

    wasPublic = !room.meta.isPrivate;

    if (room.meta.status === "playing") {
      // Do not erase seated hand mid-match; mark disconnected so player can reconnect later
      room.players[uid] = {
        ...room.players[uid],
        connected: false,
        lastSeenAt: Date.now(),
      };
      return room;
    }

    const nextPlayers = { ...room.players };
    delete nextPlayers[uid];
    const remaining = Object.values(nextPlayers).sort((a, b) => a.seatIndex - b.seatIndex);
    if (remaining.length === 0) {
      roomCodeToDelete = room.meta.roomCode;
      return null;
    }

    if (room.meta.hostUid === uid) {
      const nextHost = remaining[0];
      room.meta.hostUid = nextHost.uid;
      room.meta.hostName = nextHost.displayName;
      nextPlayers[nextHost.uid] = { ...nextHost, ready: true };
    }

    room.players = nextPlayers;
    room.meta.updatedAt = Date.now();
    return room;
  });

  if (!tx.committed) {
    throw new HttpsError("failed-precondition", "রুম ত্যাগ করা যায়নি।");
  }

  if (roomCodeToDelete) {
    await db.ref(`roomCodes/${roomCodeToDelete}`).remove();
    if (wasPublic) await db.ref(`publicRoomList/${roomId}`).remove();
  } else if (tx.snapshot.exists()) {
    const updated = tx.snapshot.val() as AuthoritativeRoomNode;
    if (!updated.meta.isPrivate) {
      await db.ref(`publicRoomList/${roomId}`).update({
        hostUid: updated.meta.hostUid,
        hostName: updated.meta.hostName,
        playerCount: Object.keys(updated.players ?? {}).length,
      });
    }
  }

  return { ok: true };
});

export const startMultiplayerMatch = onCall(async (request) => {
  const uid = requireUid(request);
  const data = (request.data as Record<string, unknown>) ?? {};
  const roomId = typeof data.roomId === "string" ? data.roomId.trim() : "";
  if (!roomId) {
    throw new HttpsError("invalid-argument", "রুম আইডি পাওয়া যায়নি।");
  }

  const db = getDatabase();
  const roomRef = db.ref(`rooms/${roomId}`);
  let rejectionReason: string | null = null;

  const tx = await roomRef.transaction((current) => {
    const room = current as AuthoritativeRoomNode | null;
    if (!room || !room.meta) {
      rejectionReason = "রুম পাওয়া যায়নি।";
      return undefined;
    }
    if (room.meta.hostUid !== uid) {
      rejectionReason = "শুধুমাত্র রুমের আয়োজক খেলা শুরু করতে পারবেন।";
      return undefined;
    }
    if (room.meta.status !== "waiting") {
      rejectionReason = "খেলা ইতিমধ্যেই শুরু হয়েছে।";
      return undefined;
    }

    const players = Object.values(room.players ?? {});
    if (players.length < 2) {
      rejectionReason = "শুরু করতে কমপক্ষে ২ জন খেলোয়াড় প্রয়োজন।";
      return undefined;
    }
    const allReady = players.every((p) => p.uid === room.meta.hostUid || p.ready);
    if (!allReady) {
      rejectionReason = "সব খেলোয়াড় এখনো প্রস্তুত (Ready) হননি।";
      return undefined;
    }

    return createInitialMatchNode(room, Date.now());
  });

  if (!tx.committed) {
    throw new HttpsError(
      "failed-precondition",
      rejectionReason ?? "ম্যাচ শুরু করা যায়নি।",
    );
  }

  const started = tx.snapshot.val() as AuthoritativeRoomNode;
  if (!started.meta.isPrivate) {
    await db.ref(`publicRoomList/${roomId}`).update({ status: "playing" });
  }

  return { ok: true, version: started.publicState?.version ?? 1 };
});

export const submitGameCommand = onCall(async (request) => {
  const uid = requireUid(request);
  const data = (request.data as Record<string, unknown>) ?? {};
  const roomId = typeof data.roomId === "string" ? data.roomId.trim() : "";
  const actionId = typeof data.actionId === "string" ? data.actionId.trim() : "";
  const command = typeof data.command === "string" ? data.command : "";
  const expectedVersion =
    typeof data.expectedVersion === "number" ? data.expectedVersion : undefined;

  if (!roomId || !actionId) {
    throw new HttpsError("invalid-argument", "রুম আইডি ও অ্যাকশন আইডি প্রয়োজন।");
  }

  const db = getDatabase();
  const roomRef = db.ref(`rooms/${roomId}`);
  let errorReason: string | null = null;
  let duplicate = false;
  let finishedResults: Record<string, PlayerMatchResult> | null = null;
  let isPrivateRoom = true;

  const tx = await roomRef.transaction((current) => {
    const room = current as AuthoritativeRoomNode | null;
    if (!room || !room.meta || !room.players?.[uid]) {
      errorReason = "আপনি এই রুমের সদস্য নন।";
      return undefined;
    }
    isPrivateRoom = room.meta.isPrivate;

    if (!room.publicState || !room.serverPrivate || room.meta.status !== "playing") {
      if (room.serverPrivate?.processedActionIds?.[actionId]) {
        duplicate = true;
        return room;
      }
      errorReason = "ম্যাচ বর্তমানে চালু নেই।";
      return undefined;
    }

    if (room.serverPrivate.processedActionIds?.[actionId]) {
      duplicate = true;
      return room;
    }

    if (expectedVersion !== undefined && room.publicState.version !== expectedVersion) {
      errorReason = "বোর্ডের অবস্থা পরিবর্তিত হয়েছে, অনুগ্রহ করে সিঙ্ক করুন।";
      return undefined;
    }

    try {
      const now = Date.now();
      const outcome =
        command === "PLAY_CARD"
          ? applyAuthoritativePlayCard(
              room,
              uid,
              actionId,
              String(data.cardId ?? ""),
              data.chosenColor as "RED" | "BLUE" | "GREEN" | "YELLOW" | undefined,
              typeof data.targetPlayerId === "string" ? data.targetPlayerId : undefined,
              now,
            )
          : command === "DRAW_CARD"
            ? applyAuthoritativeDrawCard(room, uid, actionId, now, false)
            : command === "DECLARE_LAST_CARD"
              ? applyDeclareLastCard(room, uid, actionId, now)
              : (() => {
                  throw new Error("UNKNOWN_COMMAND");
                })();

      duplicate = outcome.duplicateAction;
      if (outcome.matchFinishedNow && outcome.room.publicState?.results) {
        finishedResults = outcome.room.publicState.results;
      }
      return outcome.room;
    } catch (err) {
      errorReason = err instanceof Error ? err.message : "INVALID_COMMAND";
      return undefined;
    }
  });

  if (!tx.committed) {
    throw new HttpsError("failed-precondition", errorReason ?? "চাল গ্রহণ করা যায়নি।");
  }

  if (finishedResults) {
    await applyMatchResultsToLeaderboard(finishedResults);
    if (!isPrivateRoom) {
      await db.ref(`publicRoomList/${roomId}`).update({ status: "finished" });
    }
  }

  const finalRoom = tx.snapshot.val() as AuthoritativeRoomNode;
  return {
    ok: true,
    duplicate,
    version: finalRoom.publicState?.version ?? 0,
    matchFinished: finalRoom.meta.status === "finished",
  };
});

export const claimTurnTimeout = onCall(async (request) => {
  const uid = requireUid(request);
  const data = (request.data as Record<string, unknown>) ?? {};
  const roomId = typeof data.roomId === "string" ? data.roomId.trim() : "";
  if (!roomId) {
    throw new HttpsError("invalid-argument", "রুম আইডি পাওয়া যায়নি।");
  }

  const db = getDatabase();
  const roomRef = db.ref(`rooms/${roomId}`);
  let reason: string | null = null;

  const tx = await roomRef.transaction((current) => {
    const room = current as AuthoritativeRoomNode | null;
    if (
      !room ||
      !room.meta ||
      room.meta.status !== "playing" ||
      !room.publicState ||
      !room.serverPrivate ||
      !room.players?.[uid]
    ) {
      reason = "সদস্যপদ বা সক্রিয় ম্যাচ পাওয়া যায়নি।";
      return undefined;
    }

    const pub = room.publicState;
    const now = Date.now();
    const activePlayer = room.players[pub.currentTurnUid];
    const effectiveDeadline =
      activePlayer && !activePlayer.connected
        ? Math.min(
            pub.turnDeadlineAt,
            activePlayer.lastSeenAt + DISCONNECTED_GRACE_SECONDS * 1000,
          )
        : pub.turnDeadlineAt;

    if (now < effectiveDeadline) {
      reason = "টার্ন টাইমআউট এখনো হয়নি।";
      return undefined;
    }

    const actionId = `timeout_${pub.version}_${pub.currentTurnUid}`;
    return applyAuthoritativeDrawCard(room, pub.currentTurnUid, actionId, now, true).room;
  });

  if (!tx.committed) {
    throw new HttpsError("failed-precondition", reason ?? "টাইমআউট প্রক্রিয়া করা যায়নি।");
  }

  const updated = tx.snapshot.val() as AuthoritativeRoomNode;
  return { ok: true, version: updated.publicState?.version ?? 0 };
});

export const onPlayerPresenceChanged = onValueUpdated(
  "/rooms/{roomId}/players/{uid}/connected",
  async (event) => {
    const roomId = event.params.roomId;
    const uid = event.params.uid;
    const isConnected = Boolean(event.data.after.val());
    const db = getDatabase();
    const roomRef = db.ref(`rooms/${roomId}`);

    await roomRef.transaction((current) => {
      const room = current as AuthoritativeRoomNode | null;
      if (!room || !room.players?.[uid] || !room.publicState || room.meta.status !== "playing") {
        return undefined;
      }

      const playerName = room.players[uid].displayName;
      const now = Date.now();
      room.publicState = {
        ...room.publicState,
        lastEvent: {
          type: isConnected ? "PLAYER_RECONNECTED" : "PLAYER_DISCONNECTED",
          actorUid: uid,
          actorName: playerName,
          messageBengali: isConnected
            ? `${playerName} পুনরায় আসরে যুক্ত হয়েছেন`
            : `${playerName} সংযোগ হারিয়েছেন — পুনঃসংযোগের অপেক্ষা`,
          timestamp: now,
        },
      };
      return room;
    });
  },
);
