package com.example.data

import android.content.Context
import com.example.R
import com.example.model.BlockShape
import com.example.model.DailyLeaderboardEntry
import com.example.model.DailyLoginClaimResult
import com.example.model.DailyLoginRewardProfile
import com.example.model.DailyLoginRewardSchedule
import com.example.model.GameMode
import com.example.model.GlobalLeaderboardEntry
import com.example.model.SavedGameSessionData
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Repository handling Firebase Firestore operations for:
 * 1. Global Daily Puzzle Challenge leaderboard (/daily_challenges/{date}/scores/{uid})
 * 2. Cloud-synced active game sessions (/users/{uid}/sessions/{sessionId})
 */
class GameFirestoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    // Convenience constructor resolving the provisioned custom Firestore database ID
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    /**
     * Real-time top 50 Daily Leaderboard observer aligned with security rule `resource.data.score >= 0`.
     */
    fun observeDailyLeaderboard(dateStr: String): Flow<List<DailyLeaderboardEntry>> = flow {
        val uid = requireUserId()
        val path = "daily_challenges/$dateStr/scores"
        emitAll(
            db.collection("daily_challenges")
                .document(dateStr)
                .collection("scores")
                .whereGreaterThanOrEqualTo("score", 0)
                .orderBy("score", Query.Direction.DESCENDING)
                .limit(50)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        DailyLeaderboardEntry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    /**
     * Submits or updates the signed-in player's best score for a given Daily Challenge date.
     * Ensures an existing higher score is never downgraded.
     */
    suspend fun submitDailyScore(
        dateStr: String,
        displayName: String,
        score: Int,
        linesCleared: Int,
        maxCombo: Int
    ): Result<Unit> {
        return try {
            val uid = requireUserId()
            val safeName = displayName.trim().ifEmpty { "Player" }.take(60)
            val docRef = db.collection("daily_challenges")
                .document(dateStr)
                .collection("scores")
                .document(uid)

            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                val existingScore = (existingSnap.getLong("score") ?: 0L).toInt()
                if (score > existingScore) {
                    val updates = mapOf(
                        "displayName" to safeName,
                        "score" to score.toLong(),
                        "linesCleared" to linesCleared.toLong(),
                        "maxCombo" to maxCombo.toLong(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    docRef.update(updates).await()
                }
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "date" to dateStr,
                    "score" to score.toLong(),
                    "linesCleared" to linesCleared.toLong(),
                    "maxCombo" to maxCombo.toLong(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "daily_challenges/$dateStr/scores")
            Result.failure(e)
        }
    }

    /**
     * Real-time top 50 All-Time Global Leaderboard observer aligned with security rule
     * `resource.data.totalScore >= 0`.
     */
    fun observeGlobalLeaderboard(): Flow<List<GlobalLeaderboardEntry>> = flow {
        val uid = requireUserId()
        val path = "global_leaderboard"
        emitAll(
            db.collection("global_leaderboard")
                .whereGreaterThanOrEqualTo("totalScore", 0)
                .orderBy("totalScore", Query.Direction.DESCENDING)
                .limit(50)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        GlobalLeaderboardEntry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    /**
     * Real-time top 50 Global High Score Leaderboard observer aligned with security rule
     * `resource.data.bestScore >= 0`.
     */
    fun observeHighScoreLeaderboard(): Flow<List<GlobalLeaderboardEntry>> = flow {
        val uid = requireUserId()
        val path = "global_leaderboard"
        emitAll(
            db.collection("global_leaderboard")
                .whereGreaterThanOrEqualTo("bestScore", 0)
                .orderBy("bestScore", Query.Direction.DESCENDING)
                .limit(50)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        GlobalLeaderboardEntry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    /**
     * Synchronizes a player's local single-game high score and cumulative totals with
     * /global_leaderboard/{uid} (for example after playing in Guest Mode before signing in),
     * strictly honoring monotonic non-decreasing rules.
     */
    suspend fun syncHighScoreProfile(
        displayName: String,
        bestSingleGameScore: Int,
        totalScoreFloor: Long = 0L,
        totalLinesFloor: Long = 0L,
        highestCombo: Int = 0
    ): Result<Unit> {
        return try {
            val uid = requireUserId()
            val safeName = displayName.trim().ifEmpty { "Player" }.take(60)
            val safeBest = bestSingleGameScore.coerceAtLeast(0).toLong().coerceAtMost(10_000_000L)
            val safeTotalFloor = totalScoreFloor.coerceAtLeast(0L).coerceAtMost(1_000_000_000L)
            val safeLinesFloor = totalLinesFloor.coerceAtLeast(0L).coerceAtMost(10_000_000L)
            val safeCombo = highestCombo.coerceAtLeast(0).toLong().coerceAtMost(10_000L)

            if (safeBest == 0L && safeTotalFloor == 0L && safeLinesFloor == 0L && safeCombo == 0L) {
                return Result.success(Unit)
            }

            val docRef = db.collection("global_leaderboard").document(uid)
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                val existingName = existingSnap.getString("displayName") ?: ""
                val existingTotal = existingSnap.getLong("totalScore") ?: 0L
                val existingBest = existingSnap.getLong("bestScore") ?: 0L
                val existingLines = existingSnap.getLong("totalLinesCleared") ?: 0L
                val existingCombo = existingSnap.getLong("highestCombo") ?: 0L

                val nextTotal = maxOf(existingTotal, safeTotalFloor).coerceAtMost(1_000_000_000L)
                val nextBest = maxOf(existingBest, safeBest).coerceAtMost(10_000_000L)
                val nextLines = maxOf(existingLines, safeLinesFloor).coerceAtMost(10_000_000L)
                val nextCombo = maxOf(existingCombo, safeCombo).coerceAtMost(10_000L)

                if (nextTotal > existingTotal ||
                    nextBest > existingBest ||
                    nextLines > existingLines ||
                    nextCombo > existingCombo ||
                    existingName != safeName
                ) {
                    val updates = mapOf(
                        "displayName" to safeName,
                        "totalScore" to nextTotal,
                        "bestScore" to nextBest,
                        "totalLinesCleared" to nextLines,
                        "highestCombo" to nextCombo,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    docRef.update(updates).await()
                }
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "totalScore" to safeTotalFloor,
                    "bestScore" to safeBest,
                    "totalLinesCleared" to safeLinesFloor,
                    "highestCombo" to safeCombo,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "global_leaderboard")
            Result.failure(e)
        }
    }

    /**
     * Atomically creates or increments the signed-in player's all-time cumulative stats in
     * /global_leaderboard/{uid}. Enforces monotonic non-decreasing totals matching firestore.rules.
     */
    suspend fun submitAllTimeScoreDelta(
        displayName: String,
        scoreDelta: Int,
        bestSingleGameScore: Int,
        linesDelta: Int,
        highestCombo: Int
    ): Result<Unit> {
        return try {
            val uid = requireUserId()
            val safeName = displayName.trim().ifEmpty { "Player" }.take(60)
            val safeScoreDelta = scoreDelta.coerceAtLeast(0).toLong()
            val safeBestScore = bestSingleGameScore.coerceAtLeast(0).toLong()
            val safeLinesDelta = linesDelta.coerceAtLeast(0).toLong()
            val safeHighestCombo = highestCombo.coerceAtLeast(0).toLong()
            val docRef = db.collection("global_leaderboard").document(uid)
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                val existingBest = existingSnap.getLong("bestScore") ?: 0L
                val existingCombo = existingSnap.getLong("highestCombo") ?: 0L
                val updates = mapOf(
                    "displayName" to safeName,
                    "totalScore" to FieldValue.increment(safeScoreDelta),
                    "bestScore" to maxOf(existingBest, safeBestScore).coerceAtMost(10_000_000L),
                    "totalLinesCleared" to FieldValue.increment(safeLinesDelta),
                    "highestCombo" to maxOf(existingCombo, safeHighestCombo).coerceAtMost(10_000L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "totalScore" to safeScoreDelta.coerceAtMost(1_000_000_000L),
                    "bestScore" to safeBestScore.coerceAtMost(10_000_000L),
                    "totalLinesCleared" to safeLinesDelta.coerceAtMost(10_000_000L),
                    "highestCombo" to safeHighestCombo.coerceAtMost(10_000L),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "global_leaderboard")
            Result.failure(e)
        }
    }

    /**
     * Saves the active game session to Firestore under /users/{uid}/sessions/{sessionId}.
     */
    suspend fun saveSession(session: SavedGameSessionData): Result<Unit> {
        val sessionId = sessionDocId(session.gameMode) ?: return Result.success(Unit)
        var targetPath = "users/unknown/sessions/$sessionId"
        return try {
            val uid = requireUserId()
            targetPath = "users/$uid/sessions/$sessionId"
            val docRef = db.collection("users")
                .document(uid)
                .collection("sessions")
                .document(sessionId)

            val gridFlat = ArrayList<Long>(64)
            for (r in 0 until 8) {
                for (c in 0 until 8) {
                    gridFlat.add((session.grid.getOrNull(r)?.getOrNull(c) ?: 0).toLong())
                }
            }

            val piecesMaps = (0 until 3).map { index ->
                val shape = session.activePieces.getOrNull(index)
                if (shape == null) {
                    mapOf("present" to false)
                } else {
                    val flatMatrix = mutableListOf<Long>()
                    for (row in shape.matrix) {
                        for (cell in row) {
                            flatMatrix.add(if (cell) 1L else 0L)
                        }
                    }
                    mapOf(
                        "present" to true,
                        "id" to shape.id,
                        "name" to shape.name,
                        "colorId" to shape.colorId.toLong(),
                        "width" to shape.width.toLong(),
                        "height" to shape.height.toLong(),
                        "flatMatrix" to flatMatrix
                    )
                }
            }

            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                val updates = mapOf(
                    "gameMode" to session.gameMode.name,
                    "gridFlat" to gridFlat,
                    "pieces" to piecesMaps,
                    "score" to session.score.toLong(),
                    "comboStreak" to session.comboStreak.toLong(),
                    "maxComboSession" to session.maxComboSession.toLong(),
                    "linesClearedSession" to session.linesClearedSession.toLong(),
                    "isGameOver" to session.isGameOver,
                    "secondChanceAvailable" to session.secondChanceAvailable,
                    "dailyDate" to session.dailyDate,
                    "dailyBatchIndex" to session.dailyBatchIndex.toLong(),
                    "hammerCount" to session.hammerCount.toLong(),
                    "clientUpdatedAt" to session.clientUpdatedAt,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "gameMode" to session.gameMode.name,
                    "gridFlat" to gridFlat,
                    "pieces" to piecesMaps,
                    "score" to session.score.toLong(),
                    "comboStreak" to session.comboStreak.toLong(),
                    "maxComboSession" to session.maxComboSession.toLong(),
                    "linesClearedSession" to session.linesClearedSession.toLong(),
                    "isGameOver" to session.isGameOver,
                    "secondChanceAvailable" to session.secondChanceAvailable,
                    "dailyDate" to session.dailyDate,
                    "dailyBatchIndex" to session.dailyBatchIndex.toLong(),
                    "hammerCount" to session.hammerCount.toLong(),
                    "clientUpdatedAt" to session.clientUpdatedAt,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, targetPath)
            Result.failure(e)
        }
    }

    /**
     * Loads a saved game session for the specified mode from Firestore.
     */
    suspend fun loadSession(mode: GameMode): Result<SavedGameSessionData?> {
        val sessionId = sessionDocId(mode) ?: return Result.success(null)
        var targetPath = "users/unknown/sessions/$sessionId"
        return try {
            val uid = requireUserId()
            targetPath = "users/$uid/sessions/$sessionId"
            val snap = db.collection("users")
                .document(uid)
                .collection("sessions")
                .document(sessionId)
                .get()
                .await()
            if (!snap.exists()) {
                Result.success(null)
            } else {
                Result.success(mapSnapshotToSession(snap))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, targetPath)
            Result.failure(e)
        }
    }

    /**
     * Lists all saved sessions owned by the current authenticated user.
     */
    suspend fun loadAllSessions(): Result<List<SavedGameSessionData>> {
        var targetPath = "users/unknown/sessions"
        return try {
            val uid = requireUserId()
            targetPath = "users/$uid/sessions"
            val querySnap = db.collection("users")
                .document(uid)
                .collection("sessions")
                .get()
                .await()
            val sessions = querySnap.documents.mapNotNull { mapSnapshotToSession(it) }
            Result.success(sessions)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, targetPath)
            Result.failure(e)
        }
    }

    /**
     * Deletes a saved session from Firestore (e.g., on explicit restart/clear).
     */
    suspend fun clearSession(mode: GameMode): Result<Unit> {
        val sessionId = sessionDocId(mode) ?: return Result.success(Unit)
        var targetPath = "users/unknown/sessions/$sessionId"
        return try {
            val uid = requireUserId()
            targetPath = "users/$uid/sessions/$sessionId"
            db.collection("users")
                .document(uid)
                .collection("sessions")
                .document(sessionId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, targetPath)
            Result.failure(e)
        }
    }

    private fun sessionDocId(mode: GameMode): String? = when (mode) {
        GameMode.CLASSIC -> "classic"
        GameMode.DAILY_CHALLENGE -> "daily"
        GameMode.TIME_ATTACK, GameMode.ADVENTURE, GameMode.ZEN -> null
    }

    /**
     * Real-time observer for the signed-in user's daily login streak and currency balance at
     * /users/{uid}/rewards/daily_login.
     */
    fun observeDailyLoginReward(): Flow<DailyLoginRewardProfile?> = flow {
        val uid = requireUserId()
        val path = "users/$uid/rewards/$DAILY_LOGIN_DOC_ID"
        emitAll(
            db.collection("users")
                .document(uid)
                .collection("rewards")
                .document(DAILY_LOGIN_DOC_ID)
                .snapshots()
                .map { snapshot ->
                    if (!snapshot.exists()) {
                        null
                    } else {
                        snapshot.toObject(
                            DailyLoginRewardProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    /**
     * Claims today's escalating daily login reward in Firestore, advancing consecutive streak
     * or resetting to Day 1 if a day was missed, while strictly preventing duplicate claims
     * for the same [todayDate].
     */
    suspend fun claimDailyLoginReward(
        todayDate: String,
        yesterdayDate: String,
        localSeed: DailyLoginRewardProfile? = null
    ): Result<DailyLoginClaimResult> {
        var targetPath = "users/unknown/rewards/$DAILY_LOGIN_DOC_ID"
        return try {
            val uid = requireUserId()
            targetPath = "users/$uid/rewards/$DAILY_LOGIN_DOC_ID"
            val docRef = db.collection("users")
                .document(uid)
                .collection("rewards")
                .document(DAILY_LOGIN_DOC_ID)

            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                val currentProfile = existingSnap.toObject(
                    DailyLoginRewardProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: DailyLoginRewardProfile(id = DAILY_LOGIN_DOC_ID, userId = uid)

                if (currentProfile.lastClaimDate == todayDate) {
                    return Result.success(
                        DailyLoginClaimResult(
                            claimed = false,
                            grantedTier = null,
                            profile = currentProfile
                        )
                    )
                }

                val nextStreak = DailyLoginRewardSchedule.computeNextStreak(
                    lastClaimDate = currentProfile.lastClaimDate,
                    storedStreak = currentProfile.loginStreak.toInt(),
                    todayDate = todayDate,
                    yesterdayDate = yesterdayDate
                ).toLong().coerceIn(1L, 100_000L)
                val tier = DailyLoginRewardSchedule.tierForStreak(nextStreak.toInt())
                val nextLongest = maxOf(currentProfile.longestLoginStreak, nextStreak).coerceIn(1L, 100_000L)
                val nextTotalDays = (currentProfile.totalDaysClaimed + 1L).coerceAtMost(100_000L)
                val nextCoins = (currentProfile.coinsBalance + tier.coins.toLong()).coerceAtMost(100_000_000L)
                val nextTotalCoins = (currentProfile.totalCoinsEarned + tier.coins.toLong()).coerceAtMost(100_000_000L)
                val nextTotalHammers = (currentProfile.totalHammersEarned + tier.hammers.toLong()).coerceAtMost(1_000_000L)

                val updates = mapOf(
                    "loginStreak" to nextStreak,
                    "longestLoginStreak" to nextLongest,
                    "lastClaimDate" to todayDate,
                    "totalDaysClaimed" to nextTotalDays,
                    "coinsBalance" to nextCoins,
                    "totalCoinsEarned" to nextTotalCoins,
                    "totalHammersEarned" to nextTotalHammers,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()

                val updatedProfile = currentProfile.copy(
                    id = DAILY_LOGIN_DOC_ID,
                    userId = uid,
                    loginStreak = nextStreak,
                    longestLoginStreak = nextLongest,
                    lastClaimDate = todayDate,
                    totalDaysClaimed = nextTotalDays,
                    coinsBalance = nextCoins,
                    totalCoinsEarned = nextTotalCoins,
                    totalHammersEarned = nextTotalHammers
                )
                Result.success(DailyLoginClaimResult(claimed = true, grantedTier = tier, profile = updatedProfile))
            } else {
                val seedLastDate = localSeed?.lastClaimDate ?: ""
                val seedStreak = (localSeed?.loginStreak ?: 0L).toInt()
                val seedLongest = (localSeed?.longestLoginStreak ?: 0L).coerceIn(0L, 100_000L)
                val seedTotalDays = (localSeed?.totalDaysClaimed ?: 0L).coerceIn(0L, 100_000L)
                val seedCoins = (localSeed?.coinsBalance ?: 0L).coerceIn(0L, 100_000_000L)
                val seedTotalCoins = (localSeed?.totalCoinsEarned ?: 0L).coerceIn(0L, 100_000_000L)
                val seedTotalHammers = (localSeed?.totalHammersEarned ?: 0L).coerceIn(0L, 1_000_000L)

                if (seedLastDate == todayDate && seedTotalDays > 0L) {
                    val payload = mapOf(
                        "userId" to uid,
                        "loginStreak" to seedStreak.toLong().coerceIn(1L, 100_000L),
                        "longestLoginStreak" to maxOf(seedLongest, seedStreak.toLong()).coerceIn(1L, 100_000L),
                        "lastClaimDate" to todayDate,
                        "totalDaysClaimed" to seedTotalDays,
                        "coinsBalance" to seedCoins,
                        "totalCoinsEarned" to maxOf(seedTotalCoins, seedCoins),
                        "totalHammersEarned" to seedTotalHammers,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    docRef.set(payload).await()
                    val syncedProfile = DailyLoginRewardProfile(
                        id = DAILY_LOGIN_DOC_ID,
                        userId = uid,
                        loginStreak = seedStreak.toLong().coerceIn(1L, 100_000L),
                        longestLoginStreak = maxOf(seedLongest, seedStreak.toLong()).coerceIn(1L, 100_000L),
                        lastClaimDate = todayDate,
                        totalDaysClaimed = seedTotalDays,
                        coinsBalance = seedCoins,
                        totalCoinsEarned = maxOf(seedTotalCoins, seedCoins),
                        totalHammersEarned = seedTotalHammers
                    )
                    return Result.success(
                        DailyLoginClaimResult(claimed = false, grantedTier = null, profile = syncedProfile)
                    )
                }

                val nextStreak = DailyLoginRewardSchedule.computeNextStreak(
                    lastClaimDate = seedLastDate,
                    storedStreak = seedStreak,
                    todayDate = todayDate,
                    yesterdayDate = yesterdayDate
                ).toLong().coerceIn(1L, 100_000L)
                val tier = DailyLoginRewardSchedule.tierForStreak(nextStreak.toInt())
                val nextLongest = maxOf(seedLongest, nextStreak).coerceIn(1L, 100_000L)
                val nextTotalDays = (seedTotalDays + 1L).coerceAtMost(100_000L)
                val nextCoins = (seedCoins + tier.coins.toLong()).coerceAtMost(100_000_000L)
                val nextTotalCoins = (maxOf(seedTotalCoins, seedCoins) + tier.coins.toLong()).coerceAtMost(100_000_000L)
                val nextTotalHammers = (seedTotalHammers + tier.hammers.toLong()).coerceAtMost(1_000_000L)

                val payload = mapOf(
                    "userId" to uid,
                    "loginStreak" to nextStreak,
                    "longestLoginStreak" to nextLongest,
                    "lastClaimDate" to todayDate,
                    "totalDaysClaimed" to nextTotalDays,
                    "coinsBalance" to nextCoins,
                    "totalCoinsEarned" to nextTotalCoins,
                    "totalHammersEarned" to nextTotalHammers,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()

                val createdProfile = DailyLoginRewardProfile(
                    id = DAILY_LOGIN_DOC_ID,
                    userId = uid,
                    loginStreak = nextStreak,
                    longestLoginStreak = nextLongest,
                    lastClaimDate = todayDate,
                    totalDaysClaimed = nextTotalDays,
                    coinsBalance = nextCoins,
                    totalCoinsEarned = nextTotalCoins,
                    totalHammersEarned = nextTotalHammers
                )
                Result.success(DailyLoginClaimResult(claimed = true, grantedTier = tier, profile = createdProfile))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, targetPath)
            Result.failure(e)
        }
    }

    /**
     * Synchronizes locally earned Guest Mode daily login progress to Firestore after sign-in,
     * keeping monotonic totals intact.
     */
    suspend fun syncDailyLoginRewardProfile(
        localProfile: DailyLoginRewardProfile
    ): Result<DailyLoginRewardProfile?> {
        var targetPath = "users/unknown/rewards/$DAILY_LOGIN_DOC_ID"
        return try {
            val uid = requireUserId()
            targetPath = "users/$uid/rewards/$DAILY_LOGIN_DOC_ID"
            val docRef = db.collection("users")
                .document(uid)
                .collection("rewards")
                .document(DAILY_LOGIN_DOC_ID)

            val existingSnap = docRef.get().await()
            if (!existingSnap.exists()) {
                if (localProfile.totalDaysClaimed <= 0L && localProfile.coinsBalance <= 0L) {
                    return Result.success(null)
                }
                val safeCoins = localProfile.coinsBalance.coerceIn(0L, 100_000_000L)
                val safeTotalCoins = maxOf(localProfile.totalCoinsEarned, safeCoins).coerceIn(0L, 100_000_000L)
                val safeStreak = localProfile.loginStreak.coerceIn(0L, 100_000L)
                val safeLongest = maxOf(localProfile.longestLoginStreak, safeStreak).coerceIn(0L, 100_000L)
                val safeDays = localProfile.totalDaysClaimed.coerceIn(0L, 100_000L)
                val safeHammers = localProfile.totalHammersEarned.coerceIn(0L, 1_000_000L)
                val payload = mapOf(
                    "userId" to uid,
                    "loginStreak" to safeStreak,
                    "longestLoginStreak" to safeLongest,
                    "lastClaimDate" to localProfile.lastClaimDate,
                    "totalDaysClaimed" to safeDays,
                    "coinsBalance" to safeCoins,
                    "totalCoinsEarned" to safeTotalCoins,
                    "totalHammersEarned" to safeHammers,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                return Result.success(
                    localProfile.copy(
                        id = DAILY_LOGIN_DOC_ID,
                        userId = uid,
                        loginStreak = safeStreak,
                        longestLoginStreak = safeLongest,
                        totalDaysClaimed = safeDays,
                        coinsBalance = safeCoins,
                        totalCoinsEarned = safeTotalCoins,
                        totalHammersEarned = safeHammers
                    )
                )
            }

            val cloud = existingSnap.toObject(
                DailyLoginRewardProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: return Result.success(null)

            if (localProfile.totalDaysClaimed > cloud.totalDaysClaimed ||
                localProfile.totalCoinsEarned > cloud.totalCoinsEarned
            ) {
                val mergedLongest = maxOf(cloud.longestLoginStreak, localProfile.longestLoginStreak).coerceIn(0L, 100_000L)
                val mergedDays = maxOf(cloud.totalDaysClaimed, localProfile.totalDaysClaimed).coerceIn(0L, 100_000L)
                val mergedTotalCoins = maxOf(cloud.totalCoinsEarned, localProfile.totalCoinsEarned).coerceIn(0L, 100_000_000L)
                val mergedTotalHammers = maxOf(cloud.totalHammersEarned, localProfile.totalHammersEarned).coerceIn(0L, 1_000_000L)
                val mergedCoins = maxOf(cloud.coinsBalance, localProfile.coinsBalance).coerceIn(0L, 100_000_000L)
                val mergedLastDate = maxOf(cloud.lastClaimDate, localProfile.lastClaimDate)
                val mergedStreak = if (mergedLastDate == localProfile.lastClaimDate) {
                    localProfile.loginStreak
                } else {
                    cloud.loginStreak
                }.coerceIn(0L, 100_000L)

                val updates = mapOf(
                    "loginStreak" to mergedStreak,
                    "longestLoginStreak" to mergedLongest,
                    "lastClaimDate" to mergedLastDate,
                    "totalDaysClaimed" to mergedDays,
                    "coinsBalance" to mergedCoins,
                    "totalCoinsEarned" to mergedTotalCoins,
                    "totalHammersEarned" to mergedTotalHammers,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
                return Result.success(
                    cloud.copy(
                        loginStreak = mergedStreak,
                        longestLoginStreak = mergedLongest,
                        lastClaimDate = mergedLastDate,
                        totalDaysClaimed = mergedDays,
                        coinsBalance = mergedCoins,
                        totalCoinsEarned = mergedTotalCoins,
                        totalHammersEarned = mergedTotalHammers
                    )
                )
            }
            Result.success(cloud)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, targetPath)
            Result.failure(e)
        }
    }

    /**
     * Deducts [costCoins] from the signed-in user's Firestore Blast Coins balance when crafting
     * an extra Hammer power-up.
     */
    suspend fun spendCoinsForHammer(
        costCoins: Int = DailyLoginRewardSchedule.COIN_TO_HAMMER_COST
    ): Result<Long> {
        var targetPath = "users/unknown/rewards/$DAILY_LOGIN_DOC_ID"
        return try {
            val uid = requireUserId()
            require(costCoins > 0) { "Cost must be positive." }
            targetPath = "users/$uid/rewards/$DAILY_LOGIN_DOC_ID"
            val docRef = db.collection("users")
                .document(uid)
                .collection("rewards")
                .document(DAILY_LOGIN_DOC_ID)

            val snap = docRef.get().await()
            if (!snap.exists()) {
                return Result.failure(IllegalStateException("No daily login reward wallet found in Firestore."))
            }
            val currentCoins = snap.getLong("coinsBalance") ?: 0L
            if (currentCoins < costCoins.toLong()) {
                return Result.failure(IllegalStateException("Insufficient Blast Coins ($currentCoins < $costCoins)."))
            }
            val remaining = (currentCoins - costCoins.toLong()).coerceAtLeast(0L)
            docRef.update(
                mapOf(
                    "coinsBalance" to remaining,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(remaining)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, targetPath)
            Result.failure(e)
        }
    }

    /**
     * Credits bonus Blast Coins earned from Daily Missions to the signed-in user's
     * Firestore rewards wallet (`users/{uid}/rewards/daily_login`).
     */
    suspend fun creditBonusCoins(
        coins: Int,
        localSeed: DailyLoginRewardProfile? = null
    ): Result<Long> {
        var targetPath = "users/unknown/rewards/$DAILY_LOGIN_DOC_ID"
        return try {
            val uid = requireUserId()
            if (coins <= 0) return Result.success(localSeed?.coinsBalance ?: 0L)
            targetPath = "users/$uid/rewards/$DAILY_LOGIN_DOC_ID"
            val docRef = db.collection("users")
                .document(uid)
                .collection("rewards")
                .document(DAILY_LOGIN_DOC_ID)

            val newBalance = db.runTransaction { transaction ->
                val snap = transaction.get(docRef)
                if (!snap.exists()) {
                    val initialBalance = (localSeed?.coinsBalance ?: coins.toLong())
                        .coerceIn(coins.toLong(), 100_000_000L)
                    val initialTotal = maxOf(
                        localSeed?.totalCoinsEarned ?: coins.toLong(),
                        initialBalance
                    ).coerceIn(0L, 100_000_000L)
                    val payload = mapOf(
                        "userId" to uid,
                        "loginStreak" to (localSeed?.loginStreak ?: 0L).coerceIn(0L, 100_000L),
                        "longestLoginStreak" to (localSeed?.longestLoginStreak ?: 0L).coerceIn(0L, 100_000L),
                        "lastClaimDate" to (localSeed?.lastClaimDate ?: ""),
                        "totalDaysClaimed" to (localSeed?.totalDaysClaimed ?: 0L).coerceIn(0L, 100_000L),
                        "coinsBalance" to initialBalance,
                        "totalCoinsEarned" to initialTotal,
                        "totalHammersEarned" to (localSeed?.totalHammersEarned ?: 0L).coerceIn(0L, 1_000_000L),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    transaction.set(docRef, payload)
                    initialBalance
                } else {
                    val currentCoins = snap.getLong("coinsBalance") ?: 0L
                    val currentTotal = snap.getLong("totalCoinsEarned") ?: 0L
                    val updatedCoins = (currentCoins + coins.toLong()).coerceAtMost(100_000_000L)
                    val updatedTotal = maxOf(currentTotal + coins.toLong(), updatedCoins)
                        .coerceAtMost(100_000_000L)
                    transaction.update(
                        docRef,
                        mapOf(
                            "coinsBalance" to updatedCoins,
                            "totalCoinsEarned" to updatedTotal,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    updatedCoins
                }
            }.await()
            Result.success(newBalance)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, targetPath)
            Result.failure(e)
        }
    }

    private companion object {
        const val DAILY_LOGIN_DOC_ID = "daily_login"
    }

    @Suppress("UNCHECKED_CAST")
    private fun mapSnapshotToSession(doc: DocumentSnapshot): SavedGameSessionData? {
        val data = doc.data ?: return null
        val modeName = data["gameMode"] as? String ?: return null
        val gameMode = runCatching { GameMode.valueOf(modeName) }.getOrNull() ?: return null

        val gridFlatRaw = data["gridFlat"] as? List<*> ?: return null
        if (gridFlatRaw.size != 64) return null
        val grid = (0 until 8).map { r ->
            (0 until 8).map { c ->
                (gridFlatRaw[r * 8 + c] as? Number)?.toInt() ?: 0
            }
        }

        val piecesRaw = data["pieces"] as? List<*> ?: return null
        val pieces = (0 until 3).map { idx ->
            val pieceMap = piecesRaw.getOrNull(idx) as? Map<*, *> ?: return@map null
            val present = pieceMap["present"] as? Boolean ?: false
            if (!present) return@map null

            val id = pieceMap["id"] as? String ?: ""
            val name = pieceMap["name"] as? String ?: id
            val colorId = (pieceMap["colorId"] as? Number)?.toInt() ?: 1
            val width = (pieceMap["width"] as? Number)?.toInt() ?: 0
            val height = (pieceMap["height"] as? Number)?.toInt() ?: 0
            val flatMatrix = pieceMap["flatMatrix"] as? List<*> ?: return@map null
            if (width <= 0 || height <= 0 || flatMatrix.size != width * height) return@map null

            val matrix = (0 until height).map { r ->
                (0 until width).map { c ->
                    ((flatMatrix[r * width + c] as? Number)?.toInt() ?: 0) != 0
                }
            }
            BlockShape(
                instanceId = UUID.randomUUID().toString(),
                id = id,
                name = name,
                matrix = matrix,
                colorId = colorId
            )
        }

        return SavedGameSessionData(
            gameMode = gameMode,
            grid = grid,
            activePieces = pieces,
            score = (data["score"] as? Number)?.toInt() ?: 0,
            comboStreak = (data["comboStreak"] as? Number)?.toInt() ?: 0,
            maxComboSession = (data["maxComboSession"] as? Number)?.toInt() ?: 0,
            linesClearedSession = (data["linesClearedSession"] as? Number)?.toInt() ?: 0,
            isGameOver = data["isGameOver"] as? Boolean ?: false,
            secondChanceAvailable = data["secondChanceAvailable"] as? Boolean ?: true,
            dailyDate = data["dailyDate"] as? String ?: "",
            dailyBatchIndex = (data["dailyBatchIndex"] as? Number)?.toInt() ?: 0,
            hammerCount = (data["hammerCount"] as? Number)?.toInt() ?: 3,
            clientUpdatedAt = (data["clientUpdatedAt"] as? Number)?.toLong() ?: 0L
        )
    }
}
