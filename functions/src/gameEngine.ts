import { randomInt } from "crypto";
import {
  ActiveSuitColor,
  AuthoritativeRoomNode,
  DeckType,
  GameCard,
  PlayerMatchResult,
  RoomPlayer,
} from "./types";

const BASE_COLORS: ActiveSuitColor[] = ["RED", "BLUE", "GREEN", "YELLOW"];
const MAX_STORED_ACTION_IDS = 120;

export const DEFAULT_TURN_TIMEOUT_SECONDS = 25;
export const DISCONNECTED_GRACE_SECONDS = 12;

export function generateServerDeck(deckType: DeckType): GameCard[] {
  const cards: GameCard[] = [];
  let seq = 0;

  const nextId = (prefix: string) => `${prefix}_${++seq}`;

  for (const color of BASE_COLORS) {
    cards.push({
      id: nextId(`num_${color}_0`),
      color,
      type: "NUMBER",
      number: 0,
      pointValue: 0,
    });
    for (let num = 1; num <= 9; num++) {
      for (let copy = 0; copy < 2; copy++) {
        cards.push({
          id: nextId(`num_${color}_${num}`),
          color,
          type: "NUMBER",
          number: num,
          pointValue: num,
        });
      }
    }
    for (let copy = 0; copy < 2; copy++) {
      cards.push({
        id: nextId(`skip_${color}`),
        color,
        type: "SKIP",
        number: null,
        pointValue: 20,
      });
      cards.push({
        id: nextId(`rev_${color}`),
        color,
        type: "REVERSE",
        number: null,
        pointValue: 20,
      });
      cards.push({
        id: nextId(`d2_${color}`),
        color,
        type: "DRAW_TWO",
        number: null,
        pointValue: 20,
      });
    }
  }

  for (let i = 0; i < 4; i++) {
    cards.push({
      id: nextId("wild"),
      color: "WILD",
      type: "WILD",
      number: null,
      pointValue: 50,
    });
    cards.push({
      id: nextId("wild4"),
      color: "WILD",
      type: "WILD_DRAW_FOUR",
      number: null,
      pointValue: 50,
    });
  }

  if (deckType === "SPECIAL") {
    for (let i = 0; i < 2; i++) {
      cards.push({
        id: nextId("thunder3"),
        color: "WILD",
        type: "THUNDERBOLT_PLUS_THREE",
        number: null,
        pointValue: 45,
      });
      cards.push({
        id: nextId("swap"),
        color: "WILD",
        type: "HAND_SWAP",
        number: null,
        pointValue: 40,
      });
      cards.push({
        id: nextId("all1"),
        color: "WILD",
        type: "EVERYONE_DRAW_ONE",
        number: null,
        pointValue: 35,
      });
      cards.push({
        id: nextId("mystery"),
        color: "WILD",
        type: "MYSTERY",
        number: null,
        pointValue: 45,
      });
    }
    for (const color of BASE_COLORS) {
      cards.push({
        id: nextId(`bounce_${color}`),
        color,
        type: "BOUNCE_BACK",
        number: null,
        pointValue: 30,
      });
    }
  }

  return shuffleSecure(cards);
}

export function shuffleSecure<T>(items: T[]): T[] {
  const arr = [...items];
  for (let i = arr.length - 1; i > 0; i--) {
    const j = randomInt(0, i + 1);
    const tmp = arr[i];
    arr[i] = arr[j];
    arr[j] = tmp;
  }
  return arr;
}

export function isValidActiveColor(color: unknown): color is ActiveSuitColor {
  return color === "RED" || color === "BLUE" || color === "GREEN" || color === "YELLOW";
}

export function isCardPlayableOn(
  card: GameCard,
  topCard: GameCard,
  activeColor: ActiveSuitColor,
  pendingDrawPenalty: number,
): boolean {
  if (pendingDrawPenalty > 0) {
    if (card.type === "BOUNCE_BACK") return true;
    if (topCard.type === "DRAW_TWO" && card.type === "DRAW_TWO") return true;
    if (topCard.type === "WILD_DRAW_FOUR" && card.type === "WILD_DRAW_FOUR") return true;
    if (
      topCard.type === "THUNDERBOLT_PLUS_THREE" &&
      card.type === "THUNDERBOLT_PLUS_THREE"
    ) {
      return true;
    }
    return false;
  }

  if (card.color === "WILD") return true;
  if (card.color === activeColor) return true;
  if (card.type === "NUMBER" && topCard.type === "NUMBER") {
    return card.number !== null && card.number === topCard.number;
  }
  return card.type !== "NUMBER" && card.type === topCard.type;
}

export function createInitialMatchNode(
  room: AuthoritativeRoomNode,
  nowMs: number,
): AuthoritativeRoomNode {
  const orderedPlayers = Object.values(room.players).sort(
    (a, b) => a.seatIndex - b.seatIndex,
  );
  if (orderedPlayers.length < 2 || orderedPlayers.length > 4) {
    throw new Error("MATCH_PLAYER_COUNT_INVALID");
  }

  const deck = generateServerDeck(room.meta.deckType);
  const startingCards = Math.min(Math.max(room.meta.startingCards || 7, 5), 10);
  const hands: Record<string, GameCard[]> = {};
  for (const player of orderedPlayers) {
    hands[player.uid] = [];
  }

  let cursor = 0;
  for (let round = 0; round < startingCards; round++) {
    for (const player of orderedPlayers) {
      hands[player.uid].push(deck[cursor++]);
    }
  }

  const remaining = deck.slice(cursor);
  let openerIdx = remaining.findIndex(
    (c) => c.color !== "WILD" && c.type === "NUMBER",
  );
  if (openerIdx < 0) {
    openerIdx = remaining.findIndex((c) => c.color !== "WILD");
  }
  if (openerIdx < 0) {
    openerIdx = 0;
  }

  const topDiscard = remaining[openerIdx];
  const drawPile = [
    ...remaining.slice(0, openerIdx),
    ...remaining.slice(openerIdx + 1),
  ];

  const activeColor: ActiveSuitColor = isValidActiveColor(topDiscard.color)
    ? topDiscard.color
    : "RED";

  const seatOrder = orderedPlayers.map((p) => p.uid);
  const playerCardCounts: Record<string, number> = {};
  const declaredLastCard: Record<string, boolean> = {};
  const playerHands: Record<string, GameCard[]> = {};

  for (const p of orderedPlayers) {
    playerCardCounts[p.uid] = hands[p.uid].length;
    declaredLastCard[p.uid] = false;
    playerHands[p.uid] = [...hands[p.uid]];
  }

  const timeoutSec = room.meta.turnTimeoutSeconds || DEFAULT_TURN_TIMEOUT_SECONDS;
  const firstUid = seatOrder[0];
  const firstPlayerName = room.players[firstUid]?.displayName ?? "আয়োজক";

  return {
    ...room,
    meta: {
      ...room.meta,
      status: "playing",
      updatedAt: nowMs,
    },
    publicState: {
      version: 1,
      deckType: room.meta.deckType,
      topDiscardCard: topDiscard,
      activeColor,
      drawPileCount: drawPile.length,
      discardPileCount: 1,
      pendingDrawPenalty: 0,
      direction: 1,
      currentTurnIndex: 0,
      currentTurnUid: firstUid,
      turnDeadlineAt: nowMs + timeoutSec * 1000,
      turnTimeoutSeconds: timeoutSec,
      seatOrder,
      playerCardCounts,
      declaredLastCard,
      lastEvent: {
        type: "MATCH_STARTED",
        actorUid: room.meta.hostUid,
        actorName: room.meta.hostName,
        messageBengali: `আসর শুরু হয়েছে! প্রথম চাল: ${firstPlayerName}`,
        timestamp: nowMs,
      },
      winnerUid: null,
      results: null,
    },
    playerHands,
    serverPrivate: {
      drawPile,
      discardPile: [topDiscard],
      hands,
      processedActionIds: {},
      lastActorUid: null,
    },
  };
}

function advanceIndex(
  currentIndex: number,
  direction: 1 | -1,
  playerCount: number,
  steps = 1,
): number {
  let idx = currentIndex;
  for (let i = 0; i < steps; i++) {
    idx = (idx + direction + playerCount) % playerCount;
  }
  return idx;
}

function drawFromServerDeck(
  drawPile: GameCard[],
  discardPile: GameCard[],
  count: number,
): { drawn: GameCard[]; nextDrawPile: GameCard[]; nextDiscardPile: GameCard[]; recycledCount: number } {
  let currentDraw = [...drawPile];
  let currentDiscard = [...discardPile];
  const drawn: GameCard[] = [];
  let recycledCount = 0;

  for (let i = 0; i < count; i++) {
    if (currentDraw.length === 0 && currentDiscard.length > 1) {
      const top = currentDiscard[currentDiscard.length - 1];
      const recyclable = currentDiscard.slice(0, currentDiscard.length - 1);
      recycledCount += recyclable.length;
      currentDraw = shuffleSecure(recyclable);
      currentDiscard = [top];
    }
    if (currentDraw.length === 0) {
      break;
    }
    const card = currentDraw.shift()!;
    drawn.push(card);
  }

  return {
    drawn,
    nextDrawPile: currentDraw,
    nextDiscardPile: currentDiscard,
    recycledCount,
  };
}

function colorToBengali(color: ActiveSuitColor): string {
  switch (color) {
    case "RED":
      return "সিঁদুর";
    case "GREEN":
      return "পান্না";
    case "BLUE":
      return "নীলকণ্ঠ";
    case "YELLOW":
      return "সোনালি";
  }
}

function cardToBengaliTitle(card: GameCard): string {
  switch (card.type) {
    case "NUMBER":
      return `${card.color === "WILD" ? "" : colorToBengali(card.color as ActiveSuitColor) + " "}${card.number ?? ""}`.trim();
    case "SKIP":
      return "গতি থামান";
    case "REVERSE":
      return "পালা বদল";
    case "DRAW_TWO":
      return "দুই টানুন (+২)";
    case "WILD":
      return "রঙ বদল";
    case "WILD_DRAW_FOUR":
      return "মহাচাল (+৪)";
    case "THUNDERBOLT_PLUS_THREE":
      return "বজ্রপাত (+৩)";
    case "HAND_SWAP":
      return "হাতবদল";
    case "BOUNCE_BACK":
      return "বাউন্স ব্যাক";
    case "EVERYONE_DRAW_ONE":
      return "সবাই মিলে +১";
    case "MYSTERY":
      return "রহস্য কার্ড";
  }
}

function recordProcessedAction(
  processed: Record<string, number> | undefined,
  actionId: string,
  nowMs: number,
): Record<string, number> {
  const entries = Object.entries(processed ?? {});
  entries.push([actionId, nowMs]);
  if (entries.length > MAX_STORED_ACTION_IDS) {
    entries.sort((a, b) => a[1] - b[1]);
    entries.splice(0, entries.length - MAX_STORED_ACTION_IDS);
  }
  return Object.fromEntries(entries);
}

function computeAuthoritativeResults(
  players: Record<string, RoomPlayer>,
  hands: Record<string, GameCard[]>,
  winnerUid: string,
): Record<string, PlayerMatchResult> {
  const allUids = Object.keys(players);
  const totalOpponentPoints = allUids
    .filter((u) => u !== winnerUid)
    .reduce(
      (sum, u) => sum + (hands[u] ?? []).reduce((s, c) => s + c.pointValue, 0),
      0,
    );

  const results: Record<string, PlayerMatchResult> = {};
  for (const uid of allUids) {
    const hand = hands[uid] ?? [];
    const remainingPts = hand.reduce((s, c) => s + c.pointValue, 0);
    const isWinner = uid === winnerUid;
    const awardedPoints = isWinner
      ? 50 + totalOpponentPoints
      : Math.max(10, 40 - Math.floor(remainingPts / 3));

    results[uid] = {
      uid,
      displayName: players[uid]?.displayName ?? "খেলোয়াড়",
      avatarId: players[uid]?.avatarId ?? 0,
      cardsRemaining: hand.length,
      handPointsRemaining: remainingPts,
      awardedPoints,
      isWinner,
    };
  }
  return results;
}

export interface EngineMutationOutcome {
  room: AuthoritativeRoomNode;
  duplicateAction: boolean;
  matchFinishedNow: boolean;
}

export function applyAuthoritativePlayCard(
  room: AuthoritativeRoomNode,
  uid: string,
  actionId: string,
  cardId: string,
  chosenColor: ActiveSuitColor | undefined,
  targetPlayerId: string | undefined,
  nowMs: number,
): EngineMutationOutcome {
  if (room.meta.status !== "playing" || !room.publicState || !room.serverPrivate) {
    throw new Error("MATCH_NOT_ACTIVE");
  }
  if (room.serverPrivate.processedActionIds?.[actionId]) {
    return { room, duplicateAction: true, matchFinishedNow: false };
  }
  const pub = room.publicState;
  const priv = room.serverPrivate;
  if (pub.currentTurnUid !== uid) {
    throw new Error("NOT_YOUR_TURN");
  }

  const hand = [...(priv.hands[uid] ?? [])];
  const cardIndex = hand.findIndex((c) => c.id === cardId);
  if (cardIndex < 0) {
    throw new Error("CARD_NOT_IN_HAND");
  }
  const card = hand[cardIndex];

  if (!isCardPlayableOn(card, pub.topDiscardCard, pub.activeColor, pub.pendingDrawPenalty)) {
    throw new Error("ILLEGAL_CARD_PLAY");
  }

  const resolvedColor: ActiveSuitColor =
    card.color === "WILD"
      ? (isValidActiveColor(chosenColor) ? chosenColor : pub.activeColor)
      : (card.color as ActiveSuitColor);

  if (card.color === "WILD" && !isValidActiveColor(chosenColor)) {
    throw new Error("WILD_COLOR_REQUIRED");
  }

  // Remove played card from hand and push to discard
  hand.splice(cardIndex, 1);
  const nextHands: Record<string, GameCard[]> = {
    ...priv.hands,
    [uid]: hand,
  };
  let nextDrawPile = [...priv.drawPile];
  let nextDiscardPile = [...priv.discardPile, card];
  let nextDirection: 1 | -1 = pub.direction;
  let nextPendingPenalty = pub.pendingDrawPenalty;
  let stepAdvance = 1;
  const seatCount = pub.seatOrder.length;
  const actorName = room.players[uid]?.displayName ?? "খেলোয়াড়";
  let eventMessage = `${actorName} খেলেছেন: ${cardToBengaliTitle(card)}`;
  let eventType = "CARD_PLAYED";
  let targetUidForEvent: string | undefined;

  if (card.type === "BOUNCE_BACK" && pub.pendingDrawPenalty > 0) {
    const fallbackPrev =
      priv.lastActorUid && nextHands[priv.lastActorUid]
        ? priv.lastActorUid
        : pub.seatOrder[advanceIndex(pub.currentTurnIndex, -nextDirection as 1 | -1, seatCount, 1)];
    const targetIdx = pub.seatOrder.indexOf(fallbackPrev);
    const redirectedIdx = targetIdx >= 0 ? targetIdx : advanceIndex(pub.currentTurnIndex, nextDirection, seatCount, 1);
    const redirectedUid = pub.seatOrder[redirectedIdx];

    const bounceDraw = drawFromServerDeck(nextDrawPile, nextDiscardPile, pub.pendingDrawPenalty);
    nextDrawPile = bounceDraw.nextDrawPile;
    nextDiscardPile = bounceDraw.nextDiscardPile;
    nextHands[redirectedUid] = [...(nextHands[redirectedUid] ?? []), ...bounceDraw.drawn];
    nextPendingPenalty = 0;
    eventType = "PENALTY_BOUNCED";
    targetUidForEvent = redirectedUid;
    eventMessage = `বাউন্স ব্যাক! ${actorName} +${pub.pendingDrawPenalty} পেনাল্টি ${room.players[redirectedUid]?.displayName ?? "প্রতিপক্ষ"}-এর দিকে ফেরত পাঠিয়েছেন`;
  } else {
    switch (card.type) {
      case "BOUNCE_BACK":
        if (seatCount === 2) {
          stepAdvance = 2;
        } else {
          nextDirection = nextDirection === 1 ? -1 : 1;
        }
        break;
      case "SKIP":
        stepAdvance = 2;
        break;
      case "REVERSE":
        if (seatCount === 2) {
          stepAdvance = 2;
        } else {
          nextDirection = nextDirection === 1 ? -1 : 1;
        }
        break;
      case "DRAW_TWO":
        nextPendingPenalty += 2;
        break;
      case "WILD_DRAW_FOUR":
        nextPendingPenalty += 4;
        break;
      case "THUNDERBOLT_PLUS_THREE":
        nextPendingPenalty += 3;
        break;
      case "HAND_SWAP": {
        const candidates = pub.seatOrder.filter((s) => s !== uid);
        const chosenTarget =
          targetPlayerId && candidates.includes(targetPlayerId)
            ? targetPlayerId
            : candidates[0];
        if (chosenTarget) {
          const ownHand = [...nextHands[uid]];
          const targetHand = [...(nextHands[chosenTarget] ?? [])];
          nextHands[uid] = targetHand;
          nextHands[chosenTarget] = ownHand;
          targetUidForEvent = chosenTarget;
          eventType = "HANDS_SWAPPED";
          eventMessage = `হাতবদল! ${actorName} ও ${room.players[chosenTarget]?.displayName ?? "প্রতিপক্ষ"} তাস অদলবদল করেছেন`;
        }
        break;
      }
      case "EVERYONE_DRAW_ONE": {
        for (const otherUid of pub.seatOrder) {
          if (otherUid !== uid) {
            const drawRes = drawFromServerDeck(nextDrawPile, nextDiscardPile, 1);
            nextDrawPile = drawRes.nextDrawPile;
            nextDiscardPile = drawRes.nextDiscardPile;
            nextHands[otherUid] = [...(nextHands[otherUid] ?? []), ...drawRes.drawn];
          }
        }
        eventType = "EVERYONE_DREW_ONE";
        eventMessage = `সবাই মিলে +১! ${actorName}-এর চালে অন্যরা ১টি করে তাস টেনেছেন`;
        break;
      }
      case "MYSTERY": {
        const roll = randomInt(0, 3);
        eventType = "MYSTERY_TRIGGERED";
        if (roll === 0) {
          const nextIdx = advanceIndex(pub.currentTurnIndex, nextDirection, seatCount, 1);
          const nextUid = pub.seatOrder[nextIdx];
          const drawRes = drawFromServerDeck(nextDrawPile, nextDiscardPile, 2);
          nextDrawPile = drawRes.nextDrawPile;
          nextDiscardPile = drawRes.nextDiscardPile;
          nextHands[nextUid] = [...(nextHands[nextUid] ?? []), ...drawRes.drawn];
          eventMessage = `রহস্য কার্ড! পরবর্তী খেলোয়াড় ${room.players[nextUid]?.displayName ?? ""} ২টি তাস টেনেছেন`;
        } else if (roll === 1) {
          stepAdvance = 2;
          eventMessage = `রহস্য বর্ম! ${actorName} পরবর্তী চাল এড়িয়ে গেছেন`;
        } else {
          if (seatCount > 2) {
            nextDirection = nextDirection === 1 ? -1 : 1;
          }
          const drawRes = drawFromServerDeck(nextDrawPile, nextDiscardPile, 1);
          nextDrawPile = drawRes.nextDrawPile;
          nextDiscardPile = drawRes.nextDiscardPile;
          nextHands[uid] = [...nextHands[uid], ...drawRes.drawn];
          eventMessage = `রহস্য চমক! দিক ঘুরেছে ও ${actorName} ১টি তাস পেয়েছেন`;
        }
        break;
      }
      default:
        break;
    }
  }

  // Enforce UNO / Last Card declaration penalty when dropping to 1 card without declaring
  const hadDeclaredLastCard = Boolean(pub.declaredLastCard?.[uid]);
  if ((nextHands[uid]?.length ?? 0) === 1 && !hadDeclaredLastCard) {
    const unoPenaltyDraw = drawFromServerDeck(nextDrawPile, nextDiscardPile, 2);
    nextDrawPile = unoPenaltyDraw.nextDrawPile;
    nextDiscardPile = unoPenaltyDraw.nextDiscardPile;
    nextHands[uid] = [...nextHands[uid], ...unoPenaltyDraw.drawn];
    eventType = "UNO_PENALTY";
    eventMessage = `${actorName} শেষ তাস ঘোষণা না করায় +${unoPenaltyDraw.drawn.length} পেনাল্টি পেয়েছেন!`;
  }

  // Check if any player won (0 cards left, including after hand swap)
  const winnerUid = pub.seatOrder.find((seatUid) => (nextHands[seatUid]?.length ?? 0) === 0) ?? null;
  const matchFinishedNow = winnerUid !== null;

  const nextTurnIndex = matchFinishedNow
    ? pub.currentTurnIndex
    : advanceIndex(pub.currentTurnIndex, nextDirection, seatCount, stepAdvance);
  const nextTurnUid = pub.seatOrder[nextTurnIndex];
  const timeoutSec = pub.turnTimeoutSeconds || DEFAULT_TURN_TIMEOUT_SECONDS;

  const nextCounts: Record<string, number> = {};
  const nextDeclared: Record<string, boolean> = {};
  const nextPlayerHands: Record<string, GameCard[]> = {};
  for (const seatUid of pub.seatOrder) {
    const seatHand = nextHands[seatUid] ?? [];
    nextCounts[seatUid] = seatHand.length;
    nextDeclared[seatUid] = seatHand.length <= 2 ? Boolean(pub.declaredLastCard?.[seatUid]) : false;
    nextPlayerHands[seatUid] = seatHand;
  }

  const authoritativeResults = matchFinishedNow && winnerUid
    ? computeAuthoritativeResults(room.players, nextHands, winnerUid)
    : null;

  const updatedRoom: AuthoritativeRoomNode = {
    ...room,
    meta: {
      ...room.meta,
      status: matchFinishedNow ? "finished" : "playing",
      updatedAt: nowMs,
    },
    publicState: {
      ...pub,
      version: pub.version + 1,
      topDiscardCard: card,
      activeColor: resolvedColor,
      drawPileCount: nextDrawPile.length,
      discardPileCount: nextDiscardPile.length,
      pendingDrawPenalty: nextPendingPenalty,
      direction: nextDirection,
      currentTurnIndex: nextTurnIndex,
      currentTurnUid: nextTurnUid,
      turnDeadlineAt: matchFinishedNow ? nowMs : nowMs + timeoutSec * 1000,
      playerCardCounts: nextCounts,
      declaredLastCard: nextDeclared,
      lastEvent: {
        type: matchFinishedNow ? "MATCH_WON" : eventType,
        actorUid: winnerUid ?? uid,
        actorName: winnerUid ? (room.players[winnerUid]?.displayName ?? actorName) : actorName,
        targetUid: targetUidForEvent,
        cardType: card.type,
        resolvedColor,
        messageBengali: matchFinishedNow
          ? `${room.players[winnerUid!]?.displayName ?? actorName} সব তাস শেষ করে বিজয়ী হয়েছেন!`
          : eventMessage,
        timestamp: nowMs,
      },
      winnerUid,
      results: authoritativeResults,
    },
    playerHands: nextPlayerHands,
    serverPrivate: {
      drawPile: nextDrawPile,
      discardPile: nextDiscardPile,
      hands: nextHands,
      processedActionIds: recordProcessedAction(priv.processedActionIds, actionId, nowMs),
      lastActorUid: uid,
    },
  };

  return { room: updatedRoom, duplicateAction: false, matchFinishedNow };
}

export function applyAuthoritativeDrawCard(
  room: AuthoritativeRoomNode,
  uid: string,
  actionId: string,
  nowMs: number,
  isTimeoutForced = false,
): EngineMutationOutcome {
  if (room.meta.status !== "playing" || !room.publicState || !room.serverPrivate) {
    throw new Error("MATCH_NOT_ACTIVE");
  }
  if (room.serverPrivate.processedActionIds?.[actionId]) {
    return { room, duplicateAction: true, matchFinishedNow: false };
  }
  const pub = room.publicState;
  const priv = room.serverPrivate;
  if (pub.currentTurnUid !== uid) {
    throw new Error("NOT_YOUR_TURN");
  }

  const drawCount = pub.pendingDrawPenalty > 0 ? pub.pendingDrawPenalty : 1;
  const drawRes = drawFromServerDeck(priv.drawPile, priv.discardPile, drawCount);
  if (drawRes.drawn.length === 0) {
    // Pass turn cleanly if both piles exhausted
  }

  const nextHands: Record<string, GameCard[]> = {
    ...priv.hands,
    [uid]: [...(priv.hands[uid] ?? []), ...drawRes.drawn],
  };
  const seatCount = pub.seatOrder.length;
  const nextTurnIndex = advanceIndex(pub.currentTurnIndex, pub.direction, seatCount, 1);
  const nextTurnUid = pub.seatOrder[nextTurnIndex];
  const timeoutSec = pub.turnTimeoutSeconds || DEFAULT_TURN_TIMEOUT_SECONDS;
  const actorName = room.players[uid]?.displayName ?? "খেলোয়াড়";

  const nextCounts: Record<string, number> = {};
  const nextDeclared: Record<string, boolean> = {};
  const nextPlayerHands: Record<string, GameCard[]> = {};
  for (const seatUid of pub.seatOrder) {
    const seatHand = nextHands[seatUid] ?? [];
    nextCounts[seatUid] = seatHand.length;
    nextDeclared[seatUid] = seatHand.length <= 2 ? Boolean(pub.declaredLastCard?.[seatUid]) : false;
    nextPlayerHands[seatUid] = seatHand;
  }

  const message = isTimeoutForced
    ? `সময় শেষ! ${actorName} ${drawRes.drawn.length}টি তাস টেনেছেন`
    : drawRes.recycledCount > 0
      ? `ডিসকার্ড শাফল হয়ে ${actorName} ${drawRes.drawn.length}টি তাস টেনেছেন`
      : `${actorName} ${drawRes.drawn.length}টি তাস টেনেছেন`;

  const updatedRoom: AuthoritativeRoomNode = {
    ...room,
    meta: {
      ...room.meta,
      updatedAt: nowMs,
    },
    publicState: {
      ...pub,
      version: pub.version + 1,
      drawPileCount: drawRes.nextDrawPile.length,
      discardPileCount: drawRes.nextDiscardPile.length,
      pendingDrawPenalty: 0,
      currentTurnIndex: nextTurnIndex,
      currentTurnUid: nextTurnUid,
      turnDeadlineAt: nowMs + timeoutSec * 1000,
      playerCardCounts: nextCounts,
      declaredLastCard: nextDeclared,
      lastEvent: {
        type: isTimeoutForced ? "TURN_TIMEOUT_DRAW" : "CARDS_DRAWN",
        actorUid: uid,
        actorName,
        count: drawRes.drawn.length,
        messageBengali: message,
        timestamp: nowMs,
      },
    },
    playerHands: nextPlayerHands,
    serverPrivate: {
      drawPile: drawRes.nextDrawPile,
      discardPile: drawRes.nextDiscardPile,
      hands: nextHands,
      processedActionIds: recordProcessedAction(priv.processedActionIds, actionId, nowMs),
      lastActorUid: uid,
    },
  };

  return { room: updatedRoom, duplicateAction: false, matchFinishedNow: false };
}

export function applyDeclareLastCard(
  room: AuthoritativeRoomNode,
  uid: string,
  actionId: string,
  nowMs: number,
): EngineMutationOutcome {
  if (room.meta.status !== "playing" || !room.publicState || !room.serverPrivate) {
    throw new Error("MATCH_NOT_ACTIVE");
  }
  if (room.serverPrivate.processedActionIds?.[actionId]) {
    return { room, duplicateAction: true, matchFinishedNow: false };
  }
  const pub = room.publicState;
  const priv = room.serverPrivate;
  const handCount = priv.hands[uid]?.length ?? 0;
  if (handCount < 1 || handCount > 2) {
    throw new Error("CANNOT_DECLARE_LAST_CARD");
  }
  const actorName = room.players[uid]?.displayName ?? "খেলোয়াড়";

  return {
    duplicateAction: false,
    matchFinishedNow: false,
    room: {
      ...room,
      publicState: {
        ...pub,
        version: pub.version + 1,
        declaredLastCard: {
          ...(pub.declaredLastCard ?? {}),
          [uid]: true,
        },
        lastEvent: {
          type: "LAST_CARD_DECLARED",
          actorUid: uid,
          actorName,
          messageBengali: `শেষ তাস! ${actorName} জয়ের শেষ ঘোষণা দিয়েছেন`,
          timestamp: nowMs,
        },
      },
      serverPrivate: {
        ...priv,
        processedActionIds: recordProcessedAction(priv.processedActionIds, actionId, nowMs),
      },
    },
  };
}
