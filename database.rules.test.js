const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const PROJECT_ID = process.env.GCP_PROJECT || "demo-rongmohal-rtdb";
const [dbHost, dbPortStr] = (process.env.FIREBASE_DATABASE_EMULATOR_HOST || "127.0.0.1:9000").split(":");
const dbPort = parseInt(dbPortStr, 10);

const ALICE_UID = "alice_uid";
const BOB_UID = "bob_uid";
const OUTSIDER_UID = "outsider_uid";

let testEnv;

before(async () => {
  const rules = fs.readFileSync("./database.rules.json", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    database: {
      rules,
      host: dbHost,
      port: dbPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      await ctx.database().ref().set({
        roomCodes: {
          AB12CD: "room_1",
          PR99ZZ: "room_private",
        },
        leaderboard: {
          [ALICE_UID]: {
            uid: ALICE_UID,
            displayName: "অরিত্র",
            avatarId: 0,
            matchesPlayed: 3,
            matchesWon: 2,
            totalPoints: 180,
          },
        },
        publicRoomList: {
          room_1: {
            roomId: "room_1",
            roomCode: "AB12CD",
            title: "উন্মুক্ত আসর",
            status: "waiting",
          },
        },
        rooms: {
          room_1: {
            meta: {
              roomId: "room_1",
              roomCode: "AB12CD",
              hostUid: ALICE_UID,
              status: "playing",
              isPrivate: false,
            },
            players: {
              [ALICE_UID]: { uid: ALICE_UID, displayName: "অরিত্র", seatIndex: 0, ready: true, connected: true },
              [BOB_UID]: { uid: BOB_UID, displayName: "সাব্বির", seatIndex: 1, ready: true, connected: true },
            },
            publicState: {
              version: 2,
              currentTurnUid: ALICE_UID,
              activeColor: "RED",
            },
            playerHands: {
              [ALICE_UID]: {
                card_a: { id: "card_a", color: "RED", type: "NUMBER", number: 5 },
              },
              [BOB_UID]: {
                card_b: { id: "card_b", color: "BLUE", type: "SKIP" },
              },
            },
            serverPrivate: {
              drawPile: [{ id: "card_secret", color: "GREEN", type: "NUMBER", number: 9 }],
            },
          },
          room_private: {
            meta: {
              roomId: "room_private",
              roomCode: "PR99ZZ",
              hostUid: ALICE_UID,
              status: "waiting",
              isPrivate: true,
            },
            players: {
              [ALICE_UID]: { uid: ALICE_UID, displayName: "অরিত্র", seatIndex: 0, ready: true, connected: true },
            },
          },
        },
      });
    });
  }
});

test("Unauthenticated users cannot read or write any RTDB path", async () => {
  const unauthDb = testEnv.unauthenticatedContext().database();
  await assertFails(unauthDb.ref("publicRoomList").get());
  await assertFails(unauthDb.ref("leaderboard").get());
  await assertFails(unauthDb.ref("rooms/room_1/publicState").get());
  await assertFails(unauthDb.ref("profiles/x/displayName").set("হ্যাকার"));
});

test("serverPrivate and roomCodes are blocked from all client reads and writes", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).database();
  await assertFails(aliceDb.ref("rooms/room_1/serverPrivate").get());
  await assertFails(aliceDb.ref("rooms/room_1/serverPrivate/drawPile").set([]));
  await assertFails(aliceDb.ref("roomCodes/AB12CD").get());
  await assertFails(aliceDb.ref("roomCodes/NEW999").set("room_1"));
});

test("Players can only read their own playerHands and cannot tamper with hands", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).database();
  const bobDb = testEnv.authenticatedContext(BOB_UID).database();
  const outsiderDb = testEnv.authenticatedContext(OUTSIDER_UID).database();

  await assertSucceeds(aliceDb.ref(`rooms/room_1/playerHands/${ALICE_UID}`).get());
  await assertFails(aliceDb.ref(`rooms/room_1/playerHands/${BOB_UID}`).get());
  await assertFails(bobDb.ref(`rooms/room_1/playerHands/${ALICE_UID}`).get());
  await assertFails(outsiderDb.ref(`rooms/room_1/playerHands/${ALICE_UID}`).get());

  await assertFails(
    aliceDb.ref(`rooms/room_1/playerHands/${ALICE_UID}/cheat`).set({
      id: "cheat",
      color: "WILD",
      type: "WILD_DRAW_FOUR",
    })
  );
});

test("Public match state and room metadata are readable by members, private rooms hidden from outsiders, client writes forbidden", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).database();
  const outsiderDb = testEnv.authenticatedContext(OUTSIDER_UID).database();

  await assertSucceeds(aliceDb.ref("rooms/room_1/publicState").get());
  await assertFails(outsiderDb.ref("rooms/room_1/publicState").get());

  await assertSucceeds(aliceDb.ref("rooms/room_private/meta").get());
  await assertFails(outsiderDb.ref("rooms/room_private/meta").get());
  await assertFails(outsiderDb.ref("rooms/room_private/players").get());

  await assertFails(aliceDb.ref("rooms/room_1/publicState/winnerUid").set(ALICE_UID));
  await assertFails(aliceDb.ref("rooms/room_1/meta/hostUid").set(BOB_UID));
});

test("Leaderboard and stats are readable by authenticated users but strictly non-writable by clients", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).database();
  await assertSucceeds(bobDb.ref("leaderboard").get());
  await assertFails(
    bobDb.ref(`leaderboard/${BOB_UID}`).set({
      uid: BOB_UID,
      totalPoints: 99999,
      matchesWon: 100,
    })
  );
  await assertFails(bobDb.ref(`profiles/${BOB_UID}/stats/totalPoints`).set(9999));
});

test("Seated members can update their own presence connected/lastSeenAt only, not spoofing others or joining directly", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).database();
  const outsiderDb = testEnv.authenticatedContext(OUTSIDER_UID).database();

  await assertSucceeds(aliceDb.ref(`rooms/room_1/players/${ALICE_UID}/connected`).set(false));
  await assertSucceeds(aliceDb.ref(`rooms/room_1/players/${ALICE_UID}/lastSeenAt`).set(1700000000000));

  await assertFails(aliceDb.ref(`rooms/room_1/players/${BOB_UID}/connected`).set(false));
  await assertFails(outsiderDb.ref(`rooms/room_1/players/${OUTSIDER_UID}/connected`).set(true));
});
