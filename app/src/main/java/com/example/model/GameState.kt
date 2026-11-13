package com.example.model

data class ScorePopup(
    val id: Long = System.nanoTime() + (0..9999).random(),
    val text: String,
    val row: Float,
    val col: Float,
    val isCombo: Boolean = false,
    val comboMultiplier: Int = 1
)

enum class GameMode {
    CLASSIC,
    TIME_ATTACK,
    DAILY_CHALLENGE
}

enum class GameOverReason {
    NO_MOVES,
    TIME_UP
}

data class LineClearBurst(
    val id: Long = System.nanoTime() + (0..9999).random(),
    val row: Int,
    val col: Int,
    val colorId: Int,
    val comboMultiplier: Int = 1,
    val isRowClear: Boolean = false,
    val isColClear: Boolean = false
)

data class GameState(
    val grid: List<List<Int>> = List(8) { List(8) { 0 } },
    val activePieces: List<BlockShape?> = emptyList(),
    val score: Int = 0,
    val highScore: Int = 0,
    val comboStreak: Int = 0,
    val comboEventId: Long = 0L,
    val lastClearedLinesCount: Int = 0,
    val maxComboSession: Int = 0,
    val linesClearedSession: Int = 0,
    val isGameOver: Boolean = false,
    val isNewHighScore: Boolean = false,
    val secondChanceAvailable: Boolean = true,
    val selectedShapeIndex: Int? = null,
    val hoverRow: Int? = null,
    val hoverCol: Int? = null,
    val hoverPreviewCells: Set<Pair<Int, Int>> = emptySet(),
    val prospectiveRows: Set<Int> = emptySet(),
    val prospectiveCols: Set<Int> = emptySet(),
    val clearingCells: Set<Pair<Int, Int>> = emptySet(),
    val lineClearBursts: List<LineClearBurst> = emptyList(),
    val comboBannerText: String? = null,
    val scorePopups: List<ScorePopup> = emptyList(),
    val gameMode: GameMode = GameMode.CLASSIC,
    val dailyDate: String = "",
    val dailyFormattedDate: String = "",
    val dailyPuzzleTitle: String = "",
    val dailyBatchIndex: Int = 0,
    val dailyTargetScore: Int = 5000,
    val dailyGlobalRecord: Int = 8540,
    val canUndo: Boolean = false,
    val hammerCount: Int = 3,
    val isHammerActive: Boolean = false,
    val dailyMissions: List<DailyMission> = emptyList(),
    val unclaimedMissionsCount: Int = 0,
    val difficultyLevel: Int = 1,
    val difficultyTitle: String = DifficultyTier.STARTER.title,
    val difficultyProgress: Float = 0f,
    val nextDifficultyScore: Int? = DifficultyTier.STARTER.maxScoreExclusive,
    val timeRemainingMs: Long = 60_000L,
    val timeMaxMs: Long = 99_000L,
    val lastTimeBonusSeconds: Int = 0,
    val gameOverReason: GameOverReason = GameOverReason.NO_MOVES,
    val leaderboardEntries: List<DailyLeaderboardEntry> = emptyList(),
    val isLeaderboardLoading: Boolean = false,
    val leaderboardError: String? = null,
    val totalAllTimeScore: Long = 0L,
    val globalLeaderboardEntries: List<GlobalLeaderboardEntry> = emptyList(),
    val isGlobalLeaderboardLoading: Boolean = false,
    val globalLeaderboardError: String? = null,
    val isMusicEnabled: Boolean = true,
    val isSoundEnabled: Boolean = true,
    val isHapticsEnabled: Boolean = true,
    val currentUserId: String = "",
    val currentUserDisplayName: String = "Player"
)
