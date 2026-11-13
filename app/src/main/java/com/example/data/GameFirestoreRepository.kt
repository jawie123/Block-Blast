package com.example.data

import android.content.Context
import com.example.R
import com.example.model.BlockShape
import com.example.model.DailyLeaderboardEntry
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
        GameMode.TIME_ATTACK -> null
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
