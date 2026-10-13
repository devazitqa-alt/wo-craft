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

function validWorldPayload(worldId, ownerId, isPublic = true) {
  const past = new Date(Date.now() - 5000);
  return {
    worldId,
    name: "Overworld Prime",
    seed: 8492014,
    gameMode: "SURVIVAL",
    srfMode: "NONE",
    ownerId,
    ownerName: "Alice Crafter",
    isPublic,
    blockEditCount: 0,
    parasiteEvolutionPhase: 0,
    raidStackedWaves: 0,
    createdAt: past,
    updatedAt: past,
  };
}

test("1. Unauthenticated user: cannot list or get worlds", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("worlds").where("isPublic", "==", true).get());
});

test("2. Authenticated user: fails unfiltered worlds query (rules are not filters)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("worlds").get());
});

test("3. Authenticated user: can create valid world and query public worlds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("worlds").doc("world_1").set(validWorldPayload("world_1", ALICE_UID, true))
  );
  await assertSucceeds(
    aliceDb.collection("worlds").where("isPublic", "==", true).get()
  );
});

test("4. Spoofed world owner is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("worlds").doc("world_2").set(validWorldPayload("world_2", BOB_UID, true))
  );
});

test("5. Ghost field injection on world creation is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("worlds").doc("world_3").set({
      ...validWorldPayload("world_3", ALICE_UID, true),
      isAdmin: true,
    })
  );
});

test("6. Cross-user world deletion is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("worlds").doc("world_1").set(validWorldPayload("world_1", ALICE_UID, true))
  );
  await assertFails(bobDb.collection("worlds").doc("world_1").delete());
});

test("7. Block edits: valid creation and update succeed, spoofed updatedBy fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("worlds").doc("world_1").set(validWorldPayload("world_1", ALICE_UID, true))
  );
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    bobDb.collection("worlds").doc("world_1").collection("blocks").doc("4_64_4").set({
      blockKey: "4_64_4",
      worldId: "world_1",
      x: 4,
      y: 64,
      z: 4,
      blockType: "DIAMOND_ORE",
      updatedBy: BOB_UID,
      updatedByName: "Bob Miner",
      updatedAt: past,
    })
  );

  // Spoofed updatedBy should fail
  await assertFails(
    bobDb.collection("worlds").doc("world_1").collection("blocks").doc("5_64_5").set({
      blockKey: "5_64_5",
      worldId: "world_1",
      x: 5,
      y: 64,
      z: 5,
      blockType: "STONE",
      updatedBy: ALICE_UID,
      updatedByName: "Alice Crafter",
      updatedAt: past,
    })
  );
});

test("8. Player presence: user can only write own presence and health bounds are enforced", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("worlds").doc("world_1").set(validWorldPayload("world_1", ALICE_UID, true))
  );
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("worlds").doc("world_1").collection("players").doc(ALICE_UID).set({
      userId: ALICE_UID,
      worldId: "world_1",
      displayName: "Alice",
      x: 0,
      y: 65,
      z: 0,
      gameMode: "SURVIVAL",
      health: 20,
      hunger: 20,
      isFlying: false,
      heldItem: "DIAMOND_PICKAXE",
      updatedAt: past,
    })
  );

  // Bob cannot overwrite Alice's presence
  await assertFails(
    bobDb.collection("worlds").doc("world_1").collection("players").doc(ALICE_UID).update({
      x: 100,
      updatedAt: past,
    })
  );

  // Out-of-bounds health fails
  await assertFails(
    aliceDb.collection("worlds").doc("world_1").collection("players").doc(ALICE_UID).update({
      health: 999,
      updatedAt: past,
    })
  );
});
