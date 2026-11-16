package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.example.model.BlockShape
import com.example.model.GameMode
import com.example.model.SavedGameSessionData
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameFirestoreRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private fun sampleSession(mode: GameMode, score: Int): SavedGameSessionData {
        val grid = List(8) { r -> List(8) { c -> if (r == 0 && c < 3) 2 else 0 } }
        val shape = BlockShape(
            id = "line_2_h",
            name = "line_2_h",
            matrix = listOf(listOf(true, true)),
            colorId = 5
        )
        return SavedGameSessionData(
            gameMode = mode,
            grid = grid,
            activePieces = listOf(shape, null, null),
            score = score,
            comboStreak = 2,
            maxComboSession = 3,
            linesClearedSession = 4,
            isGameOver = false,
            secondChanceAvailable = true,
            dailyDate = TEST_DATE,
            dailyBatchIndex = 1,
            hammerCount = 3,
            clientUpdatedAt = System.currentTimeMillis()
        )
    }

    @Test
    fun submitDailyScore_authenticatedUser_createsUpdatesAndReadsLeaderboard() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repo = GameFirestoreRepository(firestore, auth)

        val submit1 = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.submitDailyScore(TEST_DATE, "Alice", 1450, 8, 2)
        }
        assertTrue(submit1.isSuccess)

        // Higher score updates existing entry
        val submit2 = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.submitDailyScore(TEST_DATE, "Alice", 2250, 12, 4)
        }
        assertTrue(submit2.isSuccess)

        val entries = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeDailyLeaderboard(TEST_DATE).first { list ->
                list.any { it.userId == aliceUid && it.score == 2250L }
            }
        }
        val aliceEntry = entries.first { it.userId == aliceUid }
        assertEquals("Alice", aliceEntry.displayName)
        assertEquals(2250L, aliceEntry.score)
        assertEquals(12L, aliceEntry.linesCleared)
        assertEquals(4L, aliceEntry.maxCombo)
    }

    @Test
    fun observeDailyLeaderboard_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = GameFirestoreRepository(firestore, auth)

        val exception = try {
            withTimeout(FLOW_TIMEOUT_MS) {
                firestore.collection("daily_challenges")
                    .document(TEST_DATE)
                    .collection("scores")
                    .whereGreaterThanOrEqualTo("score", 0)
                    .get()
                    .await()
            }
            null
        } catch (e: Exception) {
            e
        }
        assertNotNull(exception)
        val firestoreErr = exception?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreErr?.code)

        // Also ensure repo fails fast when unauthenticated
        val repoResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.submitDailyScore(TEST_DATE, "Anon", 500, 1, 1)
        }
        assertTrue(repoResult.isFailure)
    }

    @Test
    fun saveAndLoadSession_authenticatedOwner_persistsAndResumes() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repo = GameFirestoreRepository(firestore, auth)

        val session = sampleSession(GameMode.CLASSIC, 780)
        val saveRes = withTimeout(DEFAULT_TIMEOUT_MS) { repo.saveSession(session) }
        assertTrue(saveRes.isSuccess)

        // Update session
        val updatedSession = session.copy(score = 1240, comboStreak = 3)
        val updateRes = withTimeout(DEFAULT_TIMEOUT_MS) { repo.saveSession(updatedSession) }
        assertTrue(updateRes.isSuccess)

        val loaded = withTimeout(DEFAULT_TIMEOUT_MS) { repo.loadSession(GameMode.CLASSIC) }.getOrThrow()
        assertNotNull(loaded)
        assertEquals(1240, loaded!!.score)
        assertEquals(3, loaded.comboStreak)
        assertEquals(2, loaded.grid[0][0])
        assertEquals("line_2_h", loaded.activePieces[0]?.id)
        assertEquals(null, loaded.activePieces[1])

        // Clean up
        val clearRes = withTimeout(DEFAULT_TIMEOUT_MS) { repo.clearSession(GameMode.CLASSIC) }
        assertTrue(clearRes.isSuccess)
    }

    @Test
    fun readSavedSession_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = GameFirestoreRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.saveSession(sampleSession(GameMode.DAILY_CHALLENGE, 990)).getOrThrow()
        }

        // Switch to Bob and try accessing Alice's private session directly
        signInTestUser(BOB_EMAIL)
        val exception = try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users")
                    .document(aliceUid)
                    .collection("sessions")
                    .document("daily")
                    .get()
                    .await()
            }
            null
        } catch (e: Exception) {
            e
        }
        assertNotNull(exception)
        val firestoreErr = exception?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreErr?.code)
    }

    @Test
    fun submitAllTimeScore_authenticatedUsers_createsIncrementsAndObservesRankedLeaderboard() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection("global_leaderboard").document(aliceUid).delete().await()
        }
        val aliceRepo = GameFirestoreRepository(firestore, auth)

        val res1 = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.submitAllTimeScoreDelta("Alice", scoreDelta = 400, bestSingleGameScore = 400, linesDelta = 2, highestCombo = 1)
        }
        assertTrue("res1 failed: ${res1.exceptionOrNull()?.stackTraceToString()}", res1.isSuccess)

        // Increment Alice's cumulative total score across a second move/game
        val res2 = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.submitAllTimeScoreDelta("Alice", scoreDelta = 650, bestSingleGameScore = 650, linesDelta = 3, highestCombo = 3)
        }
        assertTrue("res2 failed: ${res2.exceptionOrNull()?.stackTraceToString()}", res2.isSuccess)

        // Sign in Bob with a higher total score and verify descending order
        val bobUid = signInTestUser(BOB_EMAIL)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection("global_leaderboard").document(bobUid).delete().await()
        }
        val bobRepo = GameFirestoreRepository(firestore, auth)
        val resBob = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.submitAllTimeScoreDelta("Bob", scoreDelta = 1500, bestSingleGameScore = 900, linesDelta = 6, highestCombo = 4)
        }
        assertTrue("resBob failed: ${resBob.exceptionOrNull()?.stackTraceToString()}", resBob.isSuccess)

        val entries = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeGlobalLeaderboard().first { list ->
                list.any { it.userId == aliceUid } && list.any { it.userId == bobUid }
            }
        }
        val bobIndex = entries.indexOfFirst { it.userId == bobUid }
        val aliceIndex = entries.indexOfFirst { it.userId == aliceUid }
        assertTrue("Bob (1500) should rank ahead of Alice (1050)", bobIndex in 0 until aliceIndex)

        val aliceEntry = entries[aliceIndex]
        assertEquals(1050L, aliceEntry.totalScore)
        assertEquals(650L, aliceEntry.bestScore)
        assertEquals(5L, aliceEntry.totalLinesCleared)
        assertEquals(3L, aliceEntry.highestCombo)

        val bobEntry = entries[bobIndex]
        assertEquals(1500L, bobEntry.totalScore)
        assertEquals(900L, bobEntry.bestScore)
    }

    @Test
    fun observeGlobalLeaderboard_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = GameFirestoreRepository(firestore, auth)

        val exception = try {
            withTimeout(FLOW_TIMEOUT_MS) {
                firestore.collection("global_leaderboard")
                    .whereGreaterThanOrEqualTo("totalScore", 0)
                    .get()
                    .await()
            }
            null
        } catch (e: Exception) {
            e
        }
        assertNotNull(exception)
        val firestoreErr = exception?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreErr?.code)

        val repoResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.submitAllTimeScoreDelta("Anon", 100, 100, 1, 1)
        }
        assertTrue(repoResult.isFailure)
    }

    @Test
    fun observeHighScoreLeaderboard_authenticatedUsers_ranksByBestScoreAndSyncsProfile() = runBlocking {
        // Alice has higher cumulative totalScore (4000) but lower single-game bestScore (1200)
        val aliceUid = signInTestUser(ALICE_EMAIL)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection("global_leaderboard").document(aliceUid).delete().await()
        }
        val aliceRepo = GameFirestoreRepository(firestore, auth)
        val syncAlice = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.syncHighScoreProfile(
                displayName = "Alice",
                bestSingleGameScore = 1200,
                totalScoreFloor = 4000L,
                totalLinesFloor = 25L,
                highestCombo = 3
            )
        }
        assertTrue("syncAlice failed: ${syncAlice.exceptionOrNull()?.stackTraceToString()}", syncAlice.isSuccess)

        // Bob has lower totalScore (2500) but higher single-game bestScore (2500)
        val bobUid = signInTestUser(BOB_EMAIL)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection("global_leaderboard").document(bobUid).delete().await()
        }
        val bobRepo = GameFirestoreRepository(firestore, auth)
        val syncBob = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.syncHighScoreProfile(
                displayName = "Bob",
                bestSingleGameScore = 2500,
                totalScoreFloor = 2500L,
                totalLinesFloor = 18L,
                highestCombo = 5
            )
        }
        assertTrue("syncBob failed: ${syncBob.exceptionOrNull()?.stackTraceToString()}", syncBob.isSuccess)

        val rankedByBestScore = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeHighScoreLeaderboard().first { list ->
                list.any { it.userId == aliceUid } && list.any { it.userId == bobUid }
            }
        }
        val bobIndex = rankedByBestScore.indexOfFirst { it.userId == bobUid }
        val aliceIndex = rankedByBestScore.indexOfFirst { it.userId == aliceUid }
        assertTrue(
            "Bob (bestScore 2500) should rank ahead of Alice (bestScore 1200) even with lower totalScore",
            bobIndex in 0 until aliceIndex
        )
        assertEquals(2500L, rankedByBestScore[bobIndex].bestScore)
        assertEquals(1200L, rankedByBestScore[aliceIndex].bestScore)
    }

    @Test
    fun observeHighScoreLeaderboard_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = GameFirestoreRepository(firestore, auth)

        val exception = try {
            withTimeout(FLOW_TIMEOUT_MS) {
                firestore.collection("global_leaderboard")
                    .whereGreaterThanOrEqualTo("bestScore", 0)
                    .get()
                    .await()
            }
            null
        } catch (e: Exception) {
            e
        }
        assertNotNull(exception)
        val firestoreErr = exception?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreErr?.code)

        val repoResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.syncHighScoreProfile("Anon", bestSingleGameScore = 900)
        }
        assertTrue(repoResult.isFailure)
    }

    @Test
    fun claimDailyLoginReward_consecutiveDays_advancesStreakAndPreventsDuplicateSameDay() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection("users").document(aliceUid)
                .collection("rewards").document("daily_login").delete().await()
        }
        val repo = GameFirestoreRepository(firestore, auth)

        // Day 1 claim (+50 Coins)
        val day1 = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.claimDailyLoginReward(todayDate = "2026-11-12", yesterdayDate = "2026-11-11")
        }.getOrThrow()
        assertTrue(day1.claimed)
        assertEquals(1, day1.grantedTier?.cycleDay)
        assertEquals(1L, day1.profile.loginStreak)
        assertEquals(1L, day1.profile.longestLoginStreak)
        assertEquals(50L, day1.profile.coinsBalance)
        assertEquals(50L, day1.profile.totalCoinsEarned)

        // Duplicate claim on the same day must be a no-op
        val duplicate = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.claimDailyLoginReward(todayDate = "2026-11-12", yesterdayDate = "2026-11-11")
        }.getOrThrow()
        assertEquals(false, duplicate.claimed)
        assertEquals(50L, duplicate.profile.coinsBalance)
        assertEquals(1L, duplicate.profile.totalDaysClaimed)

        // Consecutive Day 2 claim (+1 Hammer)
        val day2 = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.claimDailyLoginReward(todayDate = "2026-11-13", yesterdayDate = "2026-11-12")
        }.getOrThrow()
        assertTrue(day2.claimed)
        assertEquals(2, day2.grantedTier?.cycleDay)
        assertEquals(2L, day2.profile.loginStreak)
        assertEquals(2L, day2.profile.longestLoginStreak)
        assertEquals(2L, day2.profile.totalDaysClaimed)
        assertEquals(1L, day2.profile.totalHammersEarned)

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeDailyLoginReward().first { it != null && it.loginStreak == 2L }
        }
        assertNotNull(observed)
        assertEquals("2026-11-13", observed?.lastClaimDate)
    }

    @Test
    fun claimDailyLoginReward_missedDayAndCoinSpend_preservesMonotonicTotals() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection("users").document(aliceUid)
                .collection("rewards").document("daily_login").delete().await()
        }
        val repo = GameFirestoreRepository(firestore, auth)

        // 3 consecutive days: Day 1 (+50), Day 2 (+1 hammer), Day 3 (+100) -> 150 coins
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.claimDailyLoginReward("2026-11-10", "2026-11-09").getOrThrow()
            repo.claimDailyLoginReward("2026-11-11", "2026-11-10").getOrThrow()
            repo.claimDailyLoginReward("2026-11-12", "2026-11-11").getOrThrow()
        }

        val spentRemaining = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.spendCoinsForHammer(100)
        }.getOrThrow()
        assertEquals(50L, spentRemaining)

        // Miss 2 days -> streak resets to 1, longest stays 3, totalDaysClaimed becomes 4
        val afterGap = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.claimDailyLoginReward("2026-11-15", "2026-11-14")
        }.getOrThrow()
        assertTrue(afterGap.claimed)
        assertEquals(1L, afterGap.profile.loginStreak)
        assertEquals(3L, afterGap.profile.longestLoginStreak)
        assertEquals(4L, afterGap.profile.totalDaysClaimed)
        assertEquals(100L, afterGap.profile.coinsBalance)
        assertEquals(200L, afterGap.profile.totalCoinsEarned)
    }

    @Test
    fun dailyLoginReward_crossUserAndUnauthenticatedAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = GameFirestoreRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.claimDailyLoginReward("2026-11-13", "2026-11-12").getOrThrow()
        }

        // Bob tries to read Alice's private reward profile
        signInTestUser(BOB_EMAIL)
        val crossUserErr = try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users")
                    .document(aliceUid)
                    .collection("rewards")
                    .document("daily_login")
                    .get()
                    .await()
            }
            null
        } catch (e: Exception) {
            e
        }
        assertNotNull(crossUserErr)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            crossUserErr?.unwrapFirestoreException()?.code
        )

        // Unauthenticated client
        auth.signOut()
        val unauthErr = try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users")
                    .document(aliceUid)
                    .collection("rewards")
                    .document("daily_login")
                    .get()
                    .await()
            }
            null
        } catch (e: Exception) {
            e
        }
        assertNotNull(unauthErr)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            unauthErr?.unwrapFirestoreException()?.code
        )
        val repoResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.claimDailyLoginReward("2026-11-14", "2026-11-13")
        }
        assertTrue(repoResult.isFailure)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_blockblast@test.com"
        const val BOB_EMAIL = "bob_blockblast@test.com"
        const val TEST_DATE = "2026-11-13"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
