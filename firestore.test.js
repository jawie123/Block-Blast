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
const TEST_DATE = "2026-11-13";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function serverTs() {
  return new Date(Date.now() - 1000);
}

function validLeaderboardPayload(uid, score = 1500) {
  return {
    userId: uid,
    displayName: "Player " + uid,
    date: TEST_DATE,
    score,
    linesCleared: 10,
    maxCombo: 3,
    createdAt: serverTs(),
    updatedAt: serverTs(),
  };
}

function validSessionPayload(uid, score = 420) {
  return {
    userId: uid,
    gameMode: "CLASSIC",
    gridFlat: new Array(64).fill(0),
    pieces: [{ present: false }, { present: false }, { present: false }],
    score,
    comboStreak: 1,
    maxComboSession: 2,
    linesClearedSession: 3,
    isGameOver: false,
    secondChanceAvailable: true,
    dailyDate: TEST_DATE,
    dailyBatchIndex: 0,
    hammerCount: 3,
    clientUpdatedAt: 1700000000000,
    createdAt: serverTs(),
    updatedAt: serverTs(),
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

// --- DAILY LEADERBOARD SECURITY TESTS ---

test("Unauthenticated user: cannot read or write daily leaderboard", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection(`daily_challenges/${TEST_DATE}/scores`).where("score", ">=", 0).get()
  );
  await assertFails(
    unauthDb.collection(`daily_challenges/${TEST_DATE}/scores`).doc(ALICE_UID).set(validLeaderboardPayload(ALICE_UID))
  );
});

test("Authenticated user: can submit and query daily leaderboard with aligned score filter", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ref = aliceDb.collection(`daily_challenges/${TEST_DATE}/scores`).doc(ALICE_UID);
  await assertSucceeds(ref.set(validLeaderboardPayload(ALICE_UID, 2000)));

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb.collection(`daily_challenges/${TEST_DATE}/scores`)
      .where("score", ">=", 0)
      .orderBy("score", "desc")
      .limit(50)
      .get()
  );
});

test("Authenticated user: fails unfiltered leaderboard list query (rules are not filters)", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection(`daily_challenges/${TEST_DATE}/scores`).get());
});

test("Authenticated user: cannot spoof another user's leaderboard entry or add ghost field", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection(`daily_challenges/${TEST_DATE}/scores`).doc(BOB_UID).set(validLeaderboardPayload(BOB_UID))
  );
  await assertFails(
    aliceDb.collection(`daily_challenges/${TEST_DATE}/scores`).doc(ALICE_UID).set({
      ...validLeaderboardPayload(ALICE_UID),
      isVerified: true,
    })
  );
});

test("Authenticated user: cannot downgrade existing daily high score", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ref = aliceDb.collection(`daily_challenges/${TEST_DATE}/scores`).doc(ALICE_UID);
  await assertSucceeds(ref.set(validLeaderboardPayload(ALICE_UID, 3000)));
  await assertFails(
    ref.update({
      score: 1200,
      updatedAt: serverTs(),
    })
  );
  await assertSucceeds(
    ref.update({
      score: 3500,
      updatedAt: serverTs(),
    })
  );
});

// --- SAVED SESSION SECURITY TESTS ---

test("Unauthenticated user: cannot read or write saved sessions", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/sessions`).get());
  await assertFails(
    unauthDb.collection(`users/${ALICE_UID}/sessions`).doc("classic").set(validSessionPayload(ALICE_UID))
  );
});

test("Authenticated user: can create, update, list, and delete own saved session", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb.collection(`users/${ALICE_UID}/sessions`).doc("classic");
  await assertSucceeds(docRef.set(validSessionPayload(ALICE_UID, 500)));
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/sessions`).get());
  await assertSucceeds(
    docRef.update({
      score: 900,
      clientUpdatedAt: 1700000005000,
      updatedAt: serverTs(),
    })
  );
  await assertSucceeds(docRef.delete());
});

test("Authenticated user: cannot read or write another user's saved session", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection(`users/${BOB_UID}/sessions`).doc("classic").set(validSessionPayload(BOB_UID));
  });
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection(`users/${BOB_UID}/sessions`).doc("classic").get());
  await assertFails(aliceDb.collection(`users/${BOB_UID}/sessions`).get());
  await assertFails(
    aliceDb.collection(`users/${BOB_UID}/sessions`).doc("classic").set(validSessionPayload(ALICE_UID))
  );
});

test("Authenticated user: cannot write invalid session id, bad grid length, or ghost field", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/sessions`).doc("hacked").set(validSessionPayload(ALICE_UID))
  );
  const badGrid = validSessionPayload(ALICE_UID);
  badGrid.gridFlat = [0, 1, 2];
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/sessions`).doc("classic").set(badGrid)
  );
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/sessions`).doc("classic").set({
      ...validSessionPayload(ALICE_UID),
      isAdmin: true,
    })
  );
});

// --- ALL-TIME GLOBAL LEADERBOARD SECURITY TESTS ---

function validGlobalLeaderboardPayload(uid, totalScore = 5200) {
  return {
    userId: uid,
    displayName: "Player " + uid,
    totalScore,
    bestScore: 1800,
    totalLinesCleared: 42,
    highestCombo: 4,
    createdAt: serverTs(),
    updatedAt: serverTs(),
  };
}

test("Unauthenticated user: cannot read or write all-time global leaderboard", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("global_leaderboard").where("totalScore", ">=", 0).get()
  );
  await assertFails(
    unauthDb.collection("global_leaderboard").where("bestScore", ">=", 0).get()
  );
  await assertFails(
    unauthDb.collection("global_leaderboard").doc(ALICE_UID).set(validGlobalLeaderboardPayload(ALICE_UID))
  );
});

test("Authenticated user: can create, monotonically increment, and query global leaderboard by totalScore and bestScore", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ref = aliceDb.collection("global_leaderboard").doc(ALICE_UID);
  await assertSucceeds(ref.set(validGlobalLeaderboardPayload(ALICE_UID, 5200)));

  // Monotonic increase succeeds
  await assertSucceeds(
    ref.update({
      totalScore: 5650,
      bestScore: 1950,
      totalLinesCleared: 45,
      highestCombo: 4,
      updatedAt: serverTs(),
    })
  );

  // Monotonic decrease on totalScore or bestScore is rejected
  await assertFails(
    ref.update({
      totalScore: 4000,
      updatedAt: serverTs(),
    })
  );
  await assertFails(
    ref.update({
      bestScore: 1200,
      updatedAt: serverTs(),
    })
  );

  // Aligned totalScore and bestScore queries succeed for another signed-in player
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb.collection("global_leaderboard")
      .where("totalScore", ">=", 0)
      .orderBy("totalScore", "desc")
      .limit(50)
      .get()
  );
  await assertSucceeds(
    bobDb.collection("global_leaderboard")
      .where("bestScore", ">=", 0)
      .orderBy("bestScore", "desc")
      .limit(50)
      .get()
  );

  // Unfiltered list query fails
  await assertFails(bobDb.collection("global_leaderboard").get());
});

test("Authenticated user: cannot spoof another user's global leaderboard doc or inject ghost field", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("global_leaderboard").doc(BOB_UID).set(validGlobalLeaderboardPayload(BOB_UID))
  );
  await assertFails(
    aliceDb.collection("global_leaderboard").doc(ALICE_UID).set({
      ...validGlobalLeaderboardPayload(ALICE_UID),
      role: "admin",
    })
  );
});

// --- DAILY LOGIN REWARD & STREAK SECURITY TESTS ---

function validDailyLoginRewardPayload(uid, streak = 1, lastClaimDate = TEST_DATE) {
  return {
    userId: uid,
    loginStreak: streak,
    longestLoginStreak: streak,
    lastClaimDate,
    totalDaysClaimed: streak,
    coinsBalance: 50,
    totalCoinsEarned: 50,
    totalHammersEarned: 0,
    createdAt: serverTs(),
    updatedAt: serverTs(),
  };
}

test("Unauthenticated user: cannot read or write daily login reward profile", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection(`users/${ALICE_UID}/rewards`).doc("daily_login").get()
  );
  await assertFails(
    unauthDb.collection(`users/${ALICE_UID}/rewards`).doc("daily_login").set(validDailyLoginRewardPayload(ALICE_UID))
  );
});

test("Authenticated user: can create, advance streak, spend coins, and read own daily login reward profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb.collection(`users/${ALICE_UID}/rewards`).doc("daily_login");
  await assertSucceeds(docRef.set(validDailyLoginRewardPayload(ALICE_UID, 1, "2026-11-15")));
  await assertSucceeds(docRef.get());

  // Advance streak to Day 2 (+1 hammer)
  await assertSucceeds(
    docRef.update({
      loginStreak: 2,
      longestLoginStreak: 2,
      lastClaimDate: "2026-11-16",
      totalDaysClaimed: 2,
      coinsBalance: 150,
      totalCoinsEarned: 150,
      totalHammersEarned: 1,
      updatedAt: serverTs(),
    })
  );

  // Spend 100 coins for a hammer (coinsBalance decreases, totalCoinsEarned unchanged)
  await assertSucceeds(
    docRef.update({
      coinsBalance: 50,
      updatedAt: serverTs(),
    })
  );
});

test("Authenticated user: cannot access another user's reward profile, use wrong doc id, inject ghost fields, or downgrade totals", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb.collection(`users/${ALICE_UID}/rewards`).doc("daily_login");
  await assertSucceeds(docRef.set(validDailyLoginRewardPayload(ALICE_UID, 3, "2026-11-16")));

  // Downgrading totalDaysClaimed is rejected
  await assertFails(
    docRef.update({
      totalDaysClaimed: 1,
      updatedAt: serverTs(),
    })
  );

  // Wrong document id is rejected
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/rewards`).doc("vip_bonus").set(validDailyLoginRewardPayload(ALICE_UID))
  );

  // Ghost field is rejected
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/rewards`).doc("daily_login").set({
      ...validDailyLoginRewardPayload(ALICE_UID),
      unlimitedCoins: true,
    })
  );

  // Cross-user read/write is rejected
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection(`users/${ALICE_UID}/rewards`).doc("daily_login").get());
  await assertFails(
    bobDb.collection(`users/${ALICE_UID}/rewards`).doc("daily_login").set(validDailyLoginRewardPayload(BOB_UID))
  );
});
