import test from "node:test";
import assert from "node:assert/strict";
import {
  applyAuthoritativeDrawCard,
  applyAuthoritativePlayCard,
  applyDeclareLastCard,
  createInitialMatchNode,
  generateServerDeck,
} from "../lib/gameEngine.js";

function makeWaitingRoom(deckType = "SPECIAL") {
  const now = 1_700_000_000_000;
  return {
    meta: {
      roomId: "room_1",
      roomCode: "AB12CD",
      title: "টেস্ট আসর",
      hostUid: "u1",
      hostName: "সাকিব",
      status: "waiting",
      deckType,
      maxPlayers: 4,
      isPrivate: true,
      startingCards: 7,
      turnTimeoutSeconds: 25,
      createdAt: now,
      updatedAt: now,
    },
    players: {
      u1: {
        uid: "u1",
        displayName: "সাকিব",
        avatarId: 0,
        seatIndex: 0,
        ready: true,
        connected: true,
        lastSeenAt: now,
      },
      u2: {
        uid: "u2",
        displayName: "টুনটুনি",
        avatarId: 1,
        seatIndex: 1,
        ready: true,
        connected: true,
        lastSeenAt: now,
      },
    },
  };
}

test("server deck generates unique IDs for Classic and Special decks", () => {
  const classic = generateServerDeck("CLASSIC");
  const special = generateServerDeck("SPECIAL");
  assert.equal(classic.length, 108);
  assert.equal(new Set(classic.map((c) => c.id)).size, 108);
  assert.equal(special.length, 120);
  assert.equal(new Set(special.map((c) => c.id)).size, 120);
});

test("server rejects forged cards not in hand and non-turn plays", () => {
  const started = createInitialMatchNode(makeWaitingRoom("CLASSIC"), 1_700_000_001_000);
  assert.equal(started.meta.status, "playing");
  assert.equal(started.publicState.currentTurnUid, "u1");
  assert.equal(started.playerHands.u1.length, 7);
  assert.equal(started.playerHands.u2.length, 7);

  // Wrong turn (u2 tries to act on u1 turn)
  assert.throws(() => {
    applyAuthoritativeDrawCard(started, "u2", "act_wrong_turn", 1_700_000_002_000);
  }, /NOT_YOUR_TURN/);

  // Forged card ID not in u1's serverPrivate hand
  assert.throws(() => {
    applyAuthoritativePlayCard(
      started,
      "u1",
      "act_forged",
      "forged_wild4_id",
      "RED",
      undefined,
      1_700_000_002_000,
    );
  }, /CARD_NOT_IN_HAND/);
});

test("duplicate actionId is idempotent and does not draw twice", () => {
  const started = createInitialMatchNode(makeWaitingRoom("CLASSIC"), 1_700_000_001_000);
  const first = applyAuthoritativeDrawCard(started, "u1", "act_dup_1", 1_700_000_002_000);
  assert.equal(first.duplicateAction, false);
  assert.equal(first.room.publicState.playerCardCounts.u1, 8);
  assert.equal(first.room.publicState.currentTurnUid, "u2");

  const second = applyAuthoritativeDrawCard(first.room, "u1", "act_dup_1", 1_700_000_003_000);
  assert.equal(second.duplicateAction, true);
  assert.equal(second.room.publicState.playerCardCounts.u1, 8);
});

test("special cards (Thunderbolt +3, Bounce Back, Everyone Draw +1) and winner scoring work authoritatively", () => {
  const base = createInitialMatchNode(makeWaitingRoom("SPECIAL"), 1_700_000_001_000);
  // Set deterministic controlled hands for exact rule verification
  base.publicState.topDiscardCard = {
    id: "top_red_5",
    color: "RED",
    type: "NUMBER",
    number: 5,
    pointValue: 5,
  };
  base.publicState.activeColor = "RED";
  base.serverPrivate.discardPile = [base.publicState.topDiscardCard];

  const thunderCard = {
    id: "t3_1",
    color: "WILD",
    type: "THUNDERBOLT_PLUS_THREE",
    number: null,
    pointValue: 45,
  };
  const bounceCard = {
    id: "bb_1",
    color: "BLUE",
    type: "BOUNCE_BACK",
    number: null,
    pointValue: 30,
  };
  const winCard = {
    id: "win_r9",
    color: "RED",
    type: "NUMBER",
    number: 9,
    pointValue: 9,
  };

  base.serverPrivate.hands.u1 = [thunderCard, winCard];
  base.serverPrivate.hands.u2 = [bounceCard];
  base.playerHands.u1 = [thunderCard, winCard];
  base.playerHands.u2 = [bounceCard];
  base.publicState.playerCardCounts = { u1: 2, u2: 1 };

  // u1 declares last card cleanly
  const declared = applyDeclareLastCard(base, "u1", "act_decl", 1_700_000_002_000);
  assert.equal(declared.room.publicState.declaredLastCard.u1, true);

  // u1 plays Thunderbolt +3 choosing RED
  const afterThunder = applyAuthoritativePlayCard(
    declared.room,
    "u1",
    "act_t3",
    "t3_1",
    "RED",
    undefined,
    1_700_000_003_000,
  );
  assert.equal(afterThunder.room.publicState.pendingDrawPenalty, 3);
  assert.equal(afterThunder.room.publicState.currentTurnUid, "u2");

  // u2 defends with Bounce Back -> redirects +3 penalty back to u1 AND u2 empties their hand -> u2 wins!
  const afterBounce = applyAuthoritativePlayCard(
    afterThunder.room,
    "u2",
    "act_bb",
    "bb_1",
    undefined,
    undefined,
    1_700_000_004_000,
  );
  assert.equal(afterBounce.matchFinishedNow, true);
  assert.equal(afterBounce.room.meta.status, "finished");
  assert.equal(afterBounce.room.publicState.winnerUid, "u2");
  assert.ok(afterBounce.room.publicState.results.u2.awardedPoints > 50);
  assert.equal(afterBounce.room.publicState.results.u2.isWinner, true);
});

test("server enforces +2 penalty when dropping to 1 card without declaring and handles calm Bounce Back", () => {
  const room = makeWaitingRoom("SPECIAL");
  room.players.u3 = {
    uid: "u3",
    displayName: "পলাশ",
    avatarId: 2,
    seatIndex: 2,
    ready: true,
    connected: true,
    lastSeenAt: 1_700_000_000_000,
  };
  const base = createInitialMatchNode(room, 1_700_000_001_000);
  base.publicState.topDiscardCard = {
    id: "top_red_5",
    color: "RED",
    type: "NUMBER",
    number: 5,
    pointValue: 5,
  };
  base.publicState.activeColor = "RED";
  base.serverPrivate.discardPile = [base.publicState.topDiscardCard];

  const red3 = { id: "r3_a", color: "RED", type: "NUMBER", number: 3, pointValue: 3 };
  const red7 = { id: "r7_a", color: "RED", type: "NUMBER", number: 7, pointValue: 7 };
  const calmBounce = { id: "bb_red", color: "RED", type: "BOUNCE_BACK", number: null, pointValue: 30 };

  base.serverPrivate.hands.u1 = [red3, red7];
  base.serverPrivate.hands.u2 = [calmBounce, { id: "g1", color: "GREEN", type: "NUMBER", number: 1, pointValue: 1 }];
  base.playerHands.u1 = base.serverPrivate.hands.u1;
  base.playerHands.u2 = base.serverPrivate.hands.u2;
  base.publicState.playerCardCounts = { u1: 2, u2: 2, u3: 7 };

  // u1 plays red3 dropping to 1 card WITHOUT calling applyDeclareLastCard -> +2 penalty
  const afterUndeclared = applyAuthoritativePlayCard(
    base,
    "u1",
    "act_no_uno",
    "r3_a",
    undefined,
    undefined,
    1_700_000_002_000,
  );
  assert.equal(afterUndeclared.room.publicState.lastEvent.type, "UNO_PENALTY");
  assert.equal(afterUndeclared.room.publicState.playerCardCounts.u1, 3);
  assert.equal(afterUndeclared.room.publicState.currentTurnUid, "u2");

  // u2 plays Bounce Back calmly (no pending penalty) -> reverses direction to -1 in 3-player game -> next turn is u1
  const afterCalmBounce = applyAuthoritativePlayCard(
    afterUndeclared.room,
    "u2",
    "act_calm_bb",
    "bb_red",
    undefined,
    undefined,
    1_700_000_003_000,
  );
  assert.equal(afterCalmBounce.room.publicState.direction, -1);
  assert.equal(afterCalmBounce.room.publicState.currentTurnUid, "u1");
});

