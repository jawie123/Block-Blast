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

    private companion object {
        const val ALICE_EMAIL = "alice_blockblast@test.com"
        const val BOB_EMAIL = "bob_blockblast@test.com"
        const val TEST_DATE = "2026-11-13"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
