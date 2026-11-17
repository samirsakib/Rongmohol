const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validRoomPayload(hostUid, overrides = {}) {
  const { serverTimestamp } = require("firebase/firestore");
  return {
    roomCode: "AB12CD",
    hostUid,
    hostName: "অরিত্র",
    title: "সান্ধ্য আসর",
    status: "waiting",
    maxPlayers: 4,
    playerUids: [hostUid],
    isPublic: true,
    createdAt: new Date(),
    updatedAt: new Date(),
    ...overrides,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
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
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or list game_rooms or users", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("game_rooms").where("isPublic", "==", true).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("Authenticated user can create and read their own profile, but cannot read Bob's profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "অরিত্র",
      matchesPlayed: 0,
      matchesWon: 0,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
});

test("Authenticated user cannot inject ghost fields into profile (Shadow Update)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "অরিত্র",
      matchesPlayed: 0,
      matchesWon: 0,
      isAdmin: true,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user can create public game room and query public rooms", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("game_rooms").doc("room_1").set({
      roomCode: "TAS101",
      hostUid: ALICE_UID,
      hostName: "অরিত্র",
      title: "পদ্মা আসর",
      status: "waiting",
      maxPlayers: 4,
      playerUids: [ALICE_UID],
      isPublic: true,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );

  await assertSucceeds(
    bobDb.collection("game_rooms").where("isPublic", "==", true).get()
  );
});

test("Authenticated user cannot query unfiltered game_rooms without matching rule predicate", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("game_rooms").get());
});

test("Authenticated user cannot read private room where they are not a participant", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("game_rooms").doc("private_room").set({
      roomCode: "PRV999",
      hostUid: ALICE_UID,
      hostName: "অরিত্র",
      title: "গোপন আসর",
      status: "waiting",
      maxPlayers: 4,
      playerUids: [ALICE_UID],
      isPublic: false,
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("game_rooms").doc("private_room").get());
});

test("Non-host participant can join public waiting room, but cannot delete room or change title", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("game_rooms").doc("room_2").set({
      roomCode: "JOIN22",
      hostUid: ALICE_UID,
      hostName: "অরিত্র",
      title: "বন্ধুদের আড্ডা",
      status: "waiting",
      maxPlayers: 4,
      playerUids: [ALICE_UID],
      isPublic: true,
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb.collection("game_rooms").doc("room_2").update({
      playerUids: [ALICE_UID, BOB_UID],
      updatedAt: new Date(),
    })
  );

  await assertFails(
    bobDb.collection("game_rooms").doc("room_2").update({
      title: "হ্যাকড",
      updatedAt: new Date(),
    })
  );

  await assertFails(bobDb.collection("game_rooms").doc("room_2").delete());
});

test("Finished room is locked against further updates", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("game_rooms").doc("room_done").set({
      roomCode: "DONE99",
      hostUid: ALICE_UID,
      hostName: "অরিত্র",
      title: "সমাপ্ত খেলা",
      status: "finished",
      maxPlayers: 4,
      playerUids: [ALICE_UID, BOB_UID],
      isPublic: true,
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("game_rooms").doc("room_done").update({
      status: "waiting",
      updatedAt: new Date(),
    })
  );
});
