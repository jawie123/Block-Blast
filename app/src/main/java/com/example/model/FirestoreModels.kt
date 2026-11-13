package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class DailyLeaderboardEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val date: String = "",
    val score: Long = 0L,
    val linesCleared: Long = 0L,
    val maxCombo: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class GlobalLeaderboardEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val totalScore: Long = 0L,
    val bestScore: Long = 0L,
    val totalLinesCleared: Long = 0L,
    val highestCombo: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

/**
 * Domain representation of a serialized game session for local file storage and Firestore sync.
 */
data class SavedGameSessionData(
    val gameMode: GameMode,
    val grid: List<List<Int>>,
    val activePieces: List<BlockShape?>,
    val score: Int,
    val comboStreak: Int,
    val maxComboSession: Int,
    val linesClearedSession: Int,
    val isGameOver: Boolean,
    val secondChanceAvailable: Boolean,
    val dailyDate: String,
    val dailyBatchIndex: Int,
    val hammerCount: Int,
    val clientUpdatedAt: Long,
    val timeRemainingMs: Long = 60_000L
)
