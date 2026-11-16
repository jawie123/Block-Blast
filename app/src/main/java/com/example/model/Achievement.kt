package com.example.model

enum class AchievementMetric {
    MAX_COMBO,
    HIGH_SCORE,
    DAILY_STREAK,
    TOTAL_LINES,
    DAILY_COMPLETED
}

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val badge: String,
    val metric: AchievementMetric,
    val target: Int,
    val current: Int = 0,
    val isUnlocked: Boolean = false,
    val accentHex: Long = 0xFF00E5FFL
) {
    val progressFraction: Float
        get() = if (target <= 0) 1f else (current.toFloat() / target.toFloat()).coerceIn(0f, 1f)
}

object AchievementCatalog {

    val definitions: List<Achievement> = listOf(
        Achievement(
            id = "combo_10",
            title = "10-Line Combo",
            description = "Reach a 10x combo streak in a single run",
            badge = "🔥",
            metric = AchievementMetric.MAX_COMBO,
            target = 10,
            accentHex = 0xFFFF5252L
        ),
        Achievement(
            id = "high_scorer",
            title = "High Scorer",
            description = "Score 2,500 or more points in a single game",
            badge = "🏆",
            metric = AchievementMetric.HIGH_SCORE,
            target = 2500,
            accentHex = 0xFFFFD700L
        ),
        Achievement(
            id = "daily_streak",
            title = "Daily Streak",
            description = "Play Daily Challenge 3 days in a row",
            badge = "📅",
            metric = AchievementMetric.DAILY_STREAK,
            target = 3,
            accentHex = 0xFFFF9800L
        ),
        Achievement(
            id = "line_master",
            title = "Line Master",
            description = "Clear 50 total lines across all games",
            badge = "⚡",
            metric = AchievementMetric.TOTAL_LINES,
            target = 50,
            accentHex = 0xFF00E5FFL
        ),
        Achievement(
            id = "puzzle_solver",
            title = "Puzzle Solver",
            description = "Complete a Daily Puzzle Challenge target score",
            badge = "🧩",
            metric = AchievementMetric.DAILY_COMPLETED,
            target = 1,
            accentHex = 0xFF00E676L
        )
    )

    fun evaluateAll(
        maxCombo: Int,
        bestScore: Int,
        dailyStreak: Int,
        totalLines: Int,
        dailyCompletedCount: Int,
        previouslyUnlockedIds: Set<String> = emptySet(),
        previousProgressById: Map<String, Int> = emptyMap()
    ): List<Achievement> {
        return definitions.map { def ->
            val liveValue = when (def.metric) {
                AchievementMetric.MAX_COMBO -> maxCombo
                AchievementMetric.HIGH_SCORE -> bestScore
                AchievementMetric.DAILY_STREAK -> dailyStreak
                AchievementMetric.TOTAL_LINES -> totalLines
                AchievementMetric.DAILY_COMPLETED -> dailyCompletedCount
            }.coerceAtLeast(0)

            val savedBest = previousProgressById[def.id] ?: 0
            val wasUnlocked = def.id in previouslyUnlockedIds
            val effectiveCurrent = if (wasUnlocked) {
                def.target
            } else {
                maxOf(liveValue, savedBest).coerceAtMost(def.target)
            }
            val unlocked = wasUnlocked || effectiveCurrent >= def.target

            def.copy(
                current = if (unlocked) def.target else effectiveCurrent,
                isUnlocked = unlocked
            )
        }
    }
}
