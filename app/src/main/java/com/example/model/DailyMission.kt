package com.example.model

enum class MissionType {
    CLEAR_LINES,             // Clear X lines total
    CLEAR_LINES_SINGLE_MOVE, // Clear X lines in a single move
    REACH_COMBO,             // Reach an X combo streak
    SCORE_POINTS,            // Reach X points in a game
    PLACE_BLOCKS,            // Place X blocks on the board
    USE_HAMMER               // Use the Hammer power-up
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
    val coinReward: Int = 50,
    val hammerReward: Int = 1
) {
    val progressFraction: Float
        get() = if (target > 0) (current.toFloat() / target.toFloat()).coerceIn(0f, 1f) else 1f
}

object DailyMissionGenerator {
    fun generateMissionsForDate(dateStr: String): List<DailyMission> {
        val seed = dateStr.filter { it.isDigit() }.toLongOrNull() ?: 20261112L
        val random = java.util.Random(seed)

        // 1. Signature Single-Move Multi-Line Blast mission
        val m1 = DailyMission(
            id = "mission_single_move_lines_$dateStr",
            title = "Triple Line Blast",
            description = "Clear 3 lines in one move",
            type = MissionType.CLEAR_LINES_SINGLE_MOVE,
            target = 3,
            coinReward = 100,
            hammerReward = 1
        )

        // 2. Signature Score Milestone mission
        val m2 = DailyMission(
            id = "mission_score_1000_$dateStr",
            title = "Point Milestone",
            description = "Reach 1,000 points in a game",
            type = MissionType.SCORE_POINTS,
            target = 1000,
            coinReward = 75,
            hammerReward = 1
        )

        // 3. Rotating Line Sweeper or Combo Master mission
        val isLineSweep = random.nextBoolean()
        val m3 = if (isLineSweep) {
            val lineTargets = listOf(5, 7, 10)
            val lineTarget = lineTargets[random.nextInt(lineTargets.size)]
            DailyMission(
                id = "mission_lines_$dateStr",
                title = "Line Sweeper",
                description = "Clear $lineTarget rows or columns today",
                type = MissionType.CLEAR_LINES,
                target = lineTarget,
                coinReward = 50 + (lineTarget * 5),
                hammerReward = 1
            )
        } else {
            val comboTarget = if (random.nextBoolean()) 3 else 4
            DailyMission(
                id = "mission_combo_$dateStr",
                title = "Combo Master",
                description = "Achieve a ${comboTarget}x combo streak",
                type = MissionType.REACH_COMBO,
                target = comboTarget,
                coinReward = comboTarget * 25,
                hammerReward = 1
            )
        }

        // 4. Rotating Block Builder or Hammer Smash mission
        val isPlacement = random.nextBoolean()
        val m4 = if (isPlacement) {
            val blockTargets = listOf(15, 20, 25)
            val blockTarget = blockTargets[random.nextInt(blockTargets.size)]
            DailyMission(
                id = "mission_blocks_$dateStr",
                title = "Block Builder",
                description = "Place $blockTarget blocks on the board",
                type = MissionType.PLACE_BLOCKS,
                target = blockTarget,
                coinReward = 50,
                hammerReward = 1
            )
        } else {
            DailyMission(
                id = "mission_hammer_$dateStr",
                title = "Smash Obstacle",
                description = "Use the Hammer power-up once",
                type = MissionType.USE_HAMMER,
                target = 1,
                coinReward = 60,
                hammerReward = 1
            )
        }

        return listOf(m1, m2, m3, m4)
    }
}
