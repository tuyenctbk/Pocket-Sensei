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

test("Unauthenticated user: cannot read profile or vocab", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("vocab").get());
});

test("Authenticated user: can create own profile with valid data", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      targetLanguage: "Japanese",
      currentStreak: 1,
      wordsMastered: 0,
      totalMinutesPracticed: 15,
      avgPronunciationScore: 85,
      createdAt: new Date(),
    })
  );
});

test("Authenticated user: cannot create profile for another user", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob Impersonator",
      targetLanguage: "Japanese",
      currentStreak: 0,
      wordsMastered: 0,
      createdAt: new Date(),
    })
  );
});

test("Authenticated user: can add and query own vocab item", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const vocabRef = aliceDb.collection("users").doc(ALICE_UID).collection("vocab").doc("vocab_1");
  await assertSucceeds(
    vocabRef.set({
      userId: ALICE_UID,
      word: "こんにちは",
      translation: "Hello",
      phonetic: "Konnichiwa",
      language: "Japanese",
      masteryLevel: 2,
      pronunciationScore: 92,
      createdAt: new Date(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("vocab").get()
  );
});

test("Authenticated user: Bob cannot read Alice's vocab item", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("vocab").doc("secret_word").set({
      userId: ALICE_UID,
      word: "秘密",
      translation: "Secret",
      language: "Japanese",
      masteryLevel: 5,
      pronunciationScore: 98,
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("vocab").doc("secret_word").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("vocab").get()
  );
});

test("Authenticated user: cannot update practice session (immutable session logs)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const sessionRef = aliceDb.collection("users").doc(ALICE_UID).collection("sessions").doc("sess_1");
  await assertSucceeds(
    sessionRef.set({
      userId: ALICE_UID,
      scenarioTitle: "Ordering Coffee at Tokyo Cafe",
      language: "Japanese",
      turnsCount: 6,
      avgScore: 88,
      feedbackSummary: "Good pitch accent on sumimasen",
      createdAt: new Date(),
    })
  );
  await assertFails(
    sessionRef.update({
      avgScore: 100,
    })
  );
});

test("Leaderboard: unauthenticated user cannot read or write", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("leaderboard").get());
  await assertFails(
    unauthDb.collection("leaderboard").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Anon",
      targetLanguage: "Japanese",
      leagueTier: "White Belt",
      totalXp: 100,
      streakDays: 1,
      avgAccuracy: 80,
      updatedAt: new Date(),
    })
  );
});

test("Leaderboard: signed-in user can write own entry, read all, and cannot overwrite another user", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("leaderboard").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      targetLanguage: "Japanese",
      leagueTier: "Bronze Tier",
      totalXp: 350,
      streakDays: 3,
      avgAccuracy: 91,
      updatedAt: new Date(),
    })
  );

  await assertSucceeds(bobDb.collection("leaderboard").get());

  await assertFails(
    bobDb.collection("leaderboard").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Bob Tampering",
      targetLanguage: "Japanese",
      leagueTier: "Black Belt Sensei",
      totalXp: 9999,
      streakDays: 99,
      avgAccuracy: 100,
      updatedAt: new Date(),
    })
  );
});

test("UsageQuota: authenticated owner can sync daily quota, unauth and cross-user denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();

  const aliceQuotaRef = aliceDb.collection("users").doc(ALICE_UID).collection("usageQuota").doc("daily");
  await assertSucceeds(
    aliceQuotaRef.set({
      userId: ALICE_UID,
      evaluationsUsed: 3,
      chatTurnsUsed: 1,
      topicPacksUsed: 0,
      bonusGraceCredits: 2,
      graceClaimed: true,
      ecoLiteMode: false,
      fallbackCascadeCount: 1,
      dateIso: "2026-11-15",
      updatedAt: new Date(),
    })
  );

  await assertSucceeds(aliceQuotaRef.get());

  // Negative evaluationsUsed rejected by schema rules
  await assertFails(
    aliceQuotaRef.set({
      userId: ALICE_UID,
      evaluationsUsed: -1,
      chatTurnsUsed: 0,
      topicPacksUsed: 0,
      bonusGraceCredits: 0,
      graceClaimed: false,
      ecoLiteMode: false,
      fallbackCascadeCount: 0,
      dateIso: "2026-11-15",
      updatedAt: new Date(),
    })
  );

  // Cross-user Bob and unauthenticated user cannot read or overwrite Alice's quota
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("usageQuota").doc("daily").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("usageQuota").doc("daily").set({
      userId: ALICE_UID,
      evaluationsUsed: 0,
      chatTurnsUsed: 0,
      topicPacksUsed: 0,
      bonusGraceCredits: 0,
      graceClaimed: false,
      ecoLiteMode: false,
      fallbackCascadeCount: 0,
      dateIso: "2026-11-15",
    })
  );
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("usageQuota").doc("daily").get());
});

