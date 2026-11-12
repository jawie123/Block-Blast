package com.example.model

enum class MissionType {
    CLEAR_LINES,     // Clear X lines
    REACH_COMBO,     // Reach an X combo streak
    SCORE_POINTS,    // Score X points in a game
    PLACE_BLOCKS,    // Place X blocks on the board
    USE_HAMMER       // Use the Hammer power-up
}

data class DailyMission(
    val id: String,
    val title: String,
    val description: String,
    val type: MissionType,
    val target: Int,
    val current: Int = 0,
    val isCompleted: Boolean = false,
    val isClaimed: Boolean = false,
    val hammerReward: Int = 1
) {
    val progressFraction: Float
        get() = if (target > 0) (current.toFloat() / target.toFloat()).coerceIn(0f, 1f) else 1f
}

object DailyMissionGenerator {
    fun generateMissionsForDate(dateStr: String): List<DailyMission> {
        val seed = dateStr.filter { it.isDigit() }.toLongOrNull() ?: 20261112L
        val random = java.util.Random(seed)

        // 1. Line Clear mission
        val lineTargets = listOf(4, 6, 8)
        val lineTarget = lineTargets[random.nextInt(lineTargets.size)]
        val m1 = DailyMission(
            id = "mission_lines_$dateStr",
            title = "Line Sweeper",
            description = "Clear $lineTarget rows or columns",
            type = MissionType.CLEAR_LINES,
            target = lineTarget,
            hammerReward = 1
        )

        // 2. Combo or Score mission
        val isCombo = random.nextBoolean()
        val m2 = if (isCombo) {
            val comboTarget = if (random.nextBoolean()) 3 else 4
            DailyMission(
                id = "mission_combo_$dateStr",
                title = "Combo Master",
                description = "Achieve a ${comboTarget}x combo streak",
                type = MissionType.REACH_COMBO,
                target = comboTarget,
                hammerReward = 2
            )
        } else {
            val scoreTargets = listOf(1500, 2500, 3500)
            val scoreTarget = scoreTargets[random.nextInt(scoreTargets.size)]
            DailyMission(
                id = "mission_score_$dateStr",
                title = "High Scorer",
                description = "Score $scoreTarget points in a game",
                type = MissionType.SCORE_POINTS,
                target = scoreTarget,
                hammerReward = 2
            )
        }

        // 3. Placement or Hammer mission
        val isPlacement = random.nextBoolean()
        val m3 = if (isPlacement) {
            val blockTargets = listOf(15, 20, 25)
            val blockTarget = blockTargets[random.nextInt(blockTargets.size)]
            DailyMission(
                id = "mission_blocks_$dateStr",
                title = "Block Builder",
                description = "Place $blockTarget blocks on the board",
                type = MissionType.PLACE_BLOCKS,
                target = blockTarget,
                hammerReward = 1
            )
        } else {
            DailyMission(
                id = "mission_hammer_$dateStr",
                title = "Smash Obstacle",
                description = "Use the Hammer power-up once",
                type = MissionType.USE_HAMMER,
                target = 1,
                hammerReward = 1
            )
        }

        return listOf(m1, m2, m3)
    }
}
