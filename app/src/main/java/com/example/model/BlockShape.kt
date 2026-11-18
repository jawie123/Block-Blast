package com.example.model

import androidx.compose.runtime.Immutable
import java.util.UUID
import kotlin.random.Random

enum class DifficultyTier(
    val level: Int,
    val title: String,
    val minScore: Int,
    val maxScoreExclusive: Int?
) {
    STARTER(level = 1, title = "STARTER", minScore = 0, maxScoreExclusive = 500),
    CHALLENGER(level = 2, title = "CHALLENGER", minScore = 500, maxScoreExclusive = 1500),
    TACTICIAN(level = 3, title = "TACTICIAN", minScore = 1500, maxScoreExclusive = 3000),
    MASTER(level = 4, title = "MASTER", minScore = 3000, maxScoreExclusive = 6000),
    GRANDMASTER(level = 5, title = "GRANDMASTER", minScore = 6000, maxScoreExclusive = null);

    companion object {
        fun fromScore(score: Int): DifficultyTier {
            return when {
                score >= GRANDMASTER.minScore -> GRANDMASTER
                score >= MASTER.minScore -> MASTER
                score >= TACTICIAN.minScore -> TACTICIAN
                score >= CHALLENGER.minScore -> CHALLENGER
                else -> STARTER
            }
        }

        fun progressToNextTier(score: Int): Float {
            val tier = fromScore(score)
            val next = tier.maxScoreExclusive ?: return 1f
            val span = (next - tier.minScore).coerceAtLeast(1)
            return ((score - tier.minScore).toFloat() / span.toFloat()).coerceIn(0f, 1f)
        }
    }
}

@Immutable
data class BlockShape(
    val instanceId: String = UUID.randomUUID().toString(),
    val id: String,
    val name: String,
    val matrix: List<List<Boolean>>,
    val colorId: Int,
    val complexity: Int = 1
) {
    val height: Int = matrix.size
    val width: Int = if (matrix.isNotEmpty()) matrix[0].size else 0
    val cellCount: Int = matrix.sumOf { row -> row.count { it } }
}

data class ShapeSpec(
    val id: String,
    val matrix: List<List<Boolean>>,
    val colorId: Int,
    val minTier: DifficultyTier,
    val complexity: Int,
    val weightsByTier: List<Int>
) {
    val cellCount: Int = matrix.sumOf { row -> row.count { it } }

    fun weightForTier(tier: DifficultyTier): Int {
        return weightsByTier.getOrElse(tier.level - 1) { 0 }.coerceAtLeast(0)
    }

    fun weightForScore(score: Int): Int {
        val tier = DifficultyTier.fromScore(score)
        val baseWeight = weightForTier(tier)
        if (baseWeight <= 0) return 0
        val nextLevel = tier.level + 1
        if (nextLevel > DifficultyTier.GRANDMASTER.level) return baseWeight
        val nextWeight = weightsByTier.getOrElse(nextLevel - 1) { baseWeight }.coerceAtLeast(0)
        val progress = DifficultyTier.progressToNextTier(score)
        val interpolated = baseWeight + ((nextWeight - baseWeight) * progress * 0.5f)
        return interpolated.toInt().coerceAtLeast(1)
    }

    fun toBlockShape(): BlockShape {
        return BlockShape(
            id = id,
            name = id,
            matrix = matrix,
            colorId = colorId,
            complexity = complexity
        )
    }
}

object ShapeDefinitions {
    // 1x1 Dot
    private val DOT_1X1 = listOf(listOf(true))

    // Lines
    private val LINE_2_H = listOf(listOf(true, true))
    private val LINE_2_V = listOf(listOf(true), listOf(true))
    private val LINE_3_H = listOf(listOf(true, true, true))
    private val LINE_3_V = listOf(listOf(true), listOf(true), listOf(true))
    private val LINE_4_H = listOf(listOf(true, true, true, true))
    private val LINE_4_V = listOf(listOf(true), listOf(true), listOf(true), listOf(true))
    private val LINE_5_H = listOf(listOf(true, true, true, true, true))
    private val LINE_5_V = listOf(listOf(true), listOf(true), listOf(true), listOf(true), listOf(true))

    // Squares & Rectangles
    private val SQUARE_2X2 = listOf(
        listOf(true, true),
        listOf(true, true)
    )
    private val SQUARE_3X3 = listOf(
        listOf(true, true, true),
        listOf(true, true, true),
        listOf(true, true, true)
    )
    private val RECT_2X3 = listOf(
        listOf(true, true, true),
        listOf(true, true, true)
    )
    private val RECT_3X2 = listOf(
        listOf(true, true),
        listOf(true, true),
        listOf(true, true)
    )

    // Small Corners (2x2 with 3 blocks)
    private val CORNER_TL = listOf(
        listOf(true, true),
        listOf(true, false)
    )
    private val CORNER_TR = listOf(
        listOf(true, true),
        listOf(false, true)
    )
    private val CORNER_BL = listOf(
        listOf(true, false),
        listOf(true, true)
    )
    private val CORNER_BR = listOf(
        listOf(false, true),
        listOf(true, true)
    )

    // L-Shapes (3x2, 4 blocks)
    private val L_NORMAL_1 = listOf(
        listOf(true, false),
        listOf(true, false),
        listOf(true, true)
    )
    private val L_NORMAL_2 = listOf(
        listOf(false, true),
        listOf(false, true),
        listOf(true, true)
    )
    private val L_NORMAL_3 = listOf(
        listOf(true, true),
        listOf(true, false),
        listOf(true, false)
    )
    private val L_NORMAL_4 = listOf(
        listOf(true, true),
        listOf(false, true),
        listOf(false, true)
    )
    private val L_FLAT_1 = listOf(
        listOf(true, true, true),
        listOf(true, false, false)
    )
    private val L_FLAT_2 = listOf(
        listOf(true, true, true),
        listOf(false, false, true)
    )
    private val L_FLAT_3 = listOf(
        listOf(true, false, false),
        listOf(true, true, true)
    )
    private val L_FLAT_4 = listOf(
        listOf(false, false, true),
        listOf(true, true, true)
    )

    // Big Corners (3x3, 5 blocks)
    private val BIG_CORNER_TL = listOf(
        listOf(true, true, true),
        listOf(true, false, false),
        listOf(true, false, false)
    )
    private val BIG_CORNER_TR = listOf(
        listOf(true, true, true),
        listOf(false, false, true),
        listOf(false, false, true)
    )
    private val BIG_CORNER_BL = listOf(
        listOf(true, false, false),
        listOf(true, false, false),
        listOf(true, true, true)
    )
    private val BIG_CORNER_BR = listOf(
        listOf(false, false, true),
        listOf(false, false, true),
        listOf(true, true, true)
    )

    // T-Shapes
    private val T_DOWN = listOf(
        listOf(true, true, true),
        listOf(false, true, false)
    )
    private val T_UP = listOf(
        listOf(false, true, false),
        listOf(true, true, true)
    )
    private val T_LEFT = listOf(
        listOf(false, true),
        listOf(true, true),
        listOf(false, true)
    )
    private val T_RIGHT = listOf(
        listOf(true, false),
        listOf(true, true),
        listOf(true, false)
    )

    // Big 3x3 Elongated T-Shapes (5 blocks, Grandmaster)
    private val BIG_T_DOWN = listOf(
        listOf(true, true, true),
        listOf(false, true, false),
        listOf(false, true, false)
    )
    private val BIG_T_UP = listOf(
        listOf(false, true, false),
        listOf(false, true, false),
        listOf(true, true, true)
    )
    private val BIG_T_LEFT = listOf(
        listOf(true, false, false),
        listOf(true, true, true),
        listOf(true, false, false)
    )
    private val BIG_T_RIGHT = listOf(
        listOf(false, false, true),
        listOf(true, true, true),
        listOf(false, false, true)
    )

    // Z and S shapes
    private val Z_SHAPE = listOf(
        listOf(true, true, false),
        listOf(false, true, true)
    )
    private val S_SHAPE = listOf(
        listOf(false, true, true),
        listOf(true, true, false)
    )
    private val Z_VERT = listOf(
        listOf(false, true),
        listOf(true, true),
        listOf(true, false)
    )
    private val S_VERT = listOf(
        listOf(true, false),
        listOf(true, true),
        listOf(false, true)
    )

    // Plus Shape (3x3, 5 blocks)
    private val PLUS = listOf(
        listOf(false, true, false),
        listOf(true, true, true),
        listOf(false, true, false)
    )

    // U-Shapes (hollow cups, 5 blocks, Master+)
    private val U_UP = listOf(
        listOf(true, false, true),
        listOf(true, true, true)
    )
    private val U_DOWN = listOf(
        listOf(true, true, true),
        listOf(true, false, true)
    )
    private val U_LEFT = listOf(
        listOf(true, true),
        listOf(true, false),
        listOf(true, true)
    )
    private val U_RIGHT = listOf(
        listOf(true, true),
        listOf(false, true),
        listOf(true, true)
    )

    // Irregular 3x3 Stepped Hooks (5 blocks, Master+)
    private val J_BIG_1 = listOf(
        listOf(true, true, false),
        listOf(false, true, false),
        listOf(false, true, true)
    )
    private val J_BIG_2 = listOf(
        listOf(false, true, true),
        listOf(false, true, false),
        listOf(true, true, false)
    )

    // 3x3 H-Bridges & C-Frames (6-7 blocks, Grandmaster)
    private val H_BRIDGE_V = listOf(
        listOf(true, false, true),
        listOf(true, true, true),
        listOf(true, false, true)
    )
    private val H_BRIDGE_H = listOf(
        listOf(true, true, true),
        listOf(false, true, false),
        listOf(true, true, true)
    )
    private val C_FRAME_1 = listOf(
        listOf(true, true, true),
        listOf(true, false, false),
        listOf(true, true, false)
    )
    private val C_FRAME_2 = listOf(
        listOf(true, true, true),
        listOf(false, false, true),
        listOf(false, true, true)
    )

    // 3x3 Staircase / W-Pentominoes (5 blocks, Master+)
    private val STAIR_3_A = listOf(
        listOf(true, false, false),
        listOf(true, true, false),
        listOf(false, true, true)
    )
    private val STAIR_3_B = listOf(
        listOf(false, false, true),
        listOf(false, true, true),
        listOf(true, true, false)
    )

    // Diagonal Steps (2 & 3 blocks)
    private val DIAG_1 = listOf(
        listOf(true, false),
        listOf(false, true)
    )
    private val DIAG_2 = listOf(
        listOf(false, true),
        listOf(true, false)
    )
    private val DIAG_3_A = listOf(
        listOf(true, false, false),
        listOf(false, true, false),
        listOf(false, false, true)
    )
    private val DIAG_3_B = listOf(
        listOf(false, false, true),
        listOf(false, true, false),
        listOf(true, false, false)
    )

    private val ALL_SHAPE_SPECS = listOf(
        // Complexity 1: Trivial 1-2 block fillers
        ShapeSpec("dot_1x1", DOT_1X1, 3, DifficultyTier.STARTER, 1, listOf(12, 8, 5, 3, 2)),
        ShapeSpec("line_2_h", LINE_2_H, 5, DifficultyTier.STARTER, 1, listOf(12, 9, 5, 3, 2)),
        ShapeSpec("line_2_v", LINE_2_V, 5, DifficultyTier.STARTER, 1, listOf(12, 9, 5, 3, 2)),

        // Complexity 2: Clean 3-4 block compact pieces
        ShapeSpec("line_3_h", LINE_3_H, 2, DifficultyTier.STARTER, 2, listOf(14, 10, 7, 4, 3)),
        ShapeSpec("line_3_v", LINE_3_V, 2, DifficultyTier.STARTER, 2, listOf(14, 10, 7, 4, 3)),
        ShapeSpec("square_2x2", SQUARE_2X2, 4, DifficultyTier.STARTER, 2, listOf(12, 10, 8, 5, 3)),
        ShapeSpec("corner_tl", CORNER_TL, 7, DifficultyTier.STARTER, 2, listOf(10, 9, 7, 5, 3)),
        ShapeSpec("corner_tr", CORNER_TR, 7, DifficultyTier.STARTER, 2, listOf(10, 9, 7, 5, 3)),
        ShapeSpec("corner_bl", CORNER_BL, 7, DifficultyTier.STARTER, 2, listOf(10, 9, 7, 5, 3)),
        ShapeSpec("corner_br", CORNER_BR, 7, DifficultyTier.STARTER, 2, listOf(10, 9, 7, 5, 3)),

        // Complexity 3: Medium L, 4-line, and T pieces
        ShapeSpec("l_norm_1", L_NORMAL_1, 8, DifficultyTier.STARTER, 3, listOf(4, 9, 10, 8, 6)),
        ShapeSpec("l_norm_2", L_NORMAL_2, 8, DifficultyTier.STARTER, 3, listOf(4, 9, 10, 8, 6)),
        ShapeSpec("l_norm_3", L_NORMAL_3, 8, DifficultyTier.STARTER, 3, listOf(4, 9, 10, 8, 6)),
        ShapeSpec("l_norm_4", L_NORMAL_4, 8, DifficultyTier.STARTER, 3, listOf(4, 9, 10, 8, 6)),
        ShapeSpec("l_flat_1", L_FLAT_1, 2, DifficultyTier.STARTER, 3, listOf(4, 9, 10, 8, 6)),
        ShapeSpec("l_flat_2", L_FLAT_2, 2, DifficultyTier.STARTER, 3, listOf(4, 9, 10, 8, 6)),
        ShapeSpec("l_flat_3", L_FLAT_3, 2, DifficultyTier.STARTER, 3, listOf(4, 9, 10, 8, 6)),
        ShapeSpec("l_flat_4", L_FLAT_4, 2, DifficultyTier.STARTER, 3, listOf(4, 9, 10, 8, 6)),
        ShapeSpec("line_4_h", LINE_4_H, 1, DifficultyTier.CHALLENGER, 3, listOf(0, 10, 11, 9, 7)),
        ShapeSpec("line_4_v", LINE_4_V, 1, DifficultyTier.CHALLENGER, 3, listOf(0, 10, 11, 9, 7)),
        ShapeSpec("t_down", T_DOWN, 5, DifficultyTier.CHALLENGER, 3, listOf(0, 9, 11, 10, 8)),
        ShapeSpec("t_up", T_UP, 5, DifficultyTier.CHALLENGER, 3, listOf(0, 9, 11, 10, 8)),
        ShapeSpec("t_left", T_LEFT, 5, DifficultyTier.CHALLENGER, 3, listOf(0, 9, 11, 10, 8)),
        ShapeSpec("t_right", T_RIGHT, 5, DifficultyTier.CHALLENGER, 3, listOf(0, 9, 11, 10, 8)),

        // Complexity 4: Big corners, zigzags, 5-lines, diagonals, and Plus
        ShapeSpec("big_corner_tl", BIG_CORNER_TL, 6, DifficultyTier.CHALLENGER, 4, listOf(0, 6, 10, 11, 10)),
        ShapeSpec("big_corner_tr", BIG_CORNER_TR, 6, DifficultyTier.CHALLENGER, 4, listOf(0, 6, 10, 11, 10)),
        ShapeSpec("big_corner_bl", BIG_CORNER_BL, 6, DifficultyTier.CHALLENGER, 4, listOf(0, 6, 10, 11, 10)),
        ShapeSpec("big_corner_br", BIG_CORNER_BR, 6, DifficultyTier.CHALLENGER, 4, listOf(0, 6, 10, 11, 10)),
        ShapeSpec("z_shape", Z_SHAPE, 1, DifficultyTier.TACTICIAN, 4, listOf(0, 0, 11, 12, 11)),
        ShapeSpec("s_shape", S_SHAPE, 4, DifficultyTier.TACTICIAN, 4, listOf(0, 0, 11, 12, 11)),
        ShapeSpec("z_vert", Z_VERT, 1, DifficultyTier.TACTICIAN, 4, listOf(0, 0, 11, 12, 11)),
        ShapeSpec("s_vert", S_VERT, 4, DifficultyTier.TACTICIAN, 4, listOf(0, 0, 11, 12, 11)),
        ShapeSpec("line_5_h", LINE_5_H, 6, DifficultyTier.TACTICIAN, 4, listOf(0, 0, 9, 11, 12)),
        ShapeSpec("line_5_v", LINE_5_V, 6, DifficultyTier.TACTICIAN, 4, listOf(0, 0, 9, 11, 12)),
        ShapeSpec("diag_1", DIAG_1, 8, DifficultyTier.TACTICIAN, 4, listOf(0, 0, 8, 10, 10)),
        ShapeSpec("diag_2", DIAG_2, 8, DifficultyTier.TACTICIAN, 4, listOf(0, 0, 8, 10, 10)),
        ShapeSpec("plus", PLUS, 7, DifficultyTier.TACTICIAN, 4, listOf(0, 0, 8, 12, 13)),

        // Complexity 5: Expert large bricks, hollow cups, staircases, 3-diagonals, big Ts, H-bridges & C-frames
        ShapeSpec("square_3x3", SQUARE_3X3, 3, DifficultyTier.MASTER, 5, listOf(0, 0, 0, 12, 16)),
        ShapeSpec("u_up", U_UP, 7, DifficultyTier.MASTER, 5, listOf(0, 0, 0, 10, 13)),
        ShapeSpec("u_down", U_DOWN, 7, DifficultyTier.MASTER, 5, listOf(0, 0, 0, 10, 13)),
        ShapeSpec("u_left", U_LEFT, 7, DifficultyTier.MASTER, 5, listOf(0, 0, 0, 9, 12)),
        ShapeSpec("u_right", U_RIGHT, 7, DifficultyTier.MASTER, 5, listOf(0, 0, 0, 9, 12)),
        ShapeSpec("j_big_1", J_BIG_1, 5, DifficultyTier.MASTER, 5, listOf(0, 0, 0, 9, 13)),
        ShapeSpec("j_big_2", J_BIG_2, 5, DifficultyTier.MASTER, 5, listOf(0, 0, 0, 9, 13)),
        ShapeSpec("stair_3_a", STAIR_3_A, 6, DifficultyTier.MASTER, 5, listOf(0, 0, 0, 10, 14)),
        ShapeSpec("stair_3_b", STAIR_3_B, 6, DifficultyTier.MASTER, 5, listOf(0, 0, 0, 10, 14)),
        ShapeSpec("diag_3_a", DIAG_3_A, 1, DifficultyTier.GRANDMASTER, 5, listOf(0, 0, 0, 0, 12)),
        ShapeSpec("diag_3_b", DIAG_3_B, 1, DifficultyTier.GRANDMASTER, 5, listOf(0, 0, 0, 0, 12)),
        ShapeSpec("rect_2x3", RECT_2X3, 4, DifficultyTier.GRANDMASTER, 5, listOf(0, 0, 0, 0, 15)),
        ShapeSpec("rect_3x2", RECT_3X2, 4, DifficultyTier.GRANDMASTER, 5, listOf(0, 0, 0, 0, 15)),
        ShapeSpec("big_t_down", BIG_T_DOWN, 3, DifficultyTier.GRANDMASTER, 5, listOf(0, 0, 0, 0, 13)),
        ShapeSpec("big_t_up", BIG_T_UP, 3, DifficultyTier.GRANDMASTER, 5, listOf(0, 0, 0, 0, 13)),
        ShapeSpec("big_t_left", BIG_T_LEFT, 3, DifficultyTier.GRANDMASTER, 5, listOf(0, 0, 0, 0, 12)),
        ShapeSpec("big_t_right", BIG_T_RIGHT, 3, DifficultyTier.GRANDMASTER, 5, listOf(0, 0, 0, 0, 12)),
        ShapeSpec("h_bridge_v", H_BRIDGE_V, 2, DifficultyTier.GRANDMASTER, 5, listOf(0, 0, 0, 0, 10)),
        ShapeSpec("h_bridge_h", H_BRIDGE_H, 2, DifficultyTier.GRANDMASTER, 5, listOf(0, 0, 0, 0, 10)),
        ShapeSpec("c_frame_1", C_FRAME_1, 8, DifficultyTier.GRANDMASTER, 5, listOf(0, 0, 0, 0, 11)),
        ShapeSpec("c_frame_2", C_FRAME_2, 8, DifficultyTier.GRANDMASTER, 5, listOf(0, 0, 0, 0, 11))
    )

    val totalShapeVarietyCount: Int
        get() = ALL_SHAPE_SPECS.size

    fun getTierForScore(score: Int): DifficultyTier = DifficultyTier.fromScore(score)

    fun getAvailableSpecsForScore(score: Int): List<ShapeSpec> {
        val tier = DifficultyTier.fromScore(score)
        return ALL_SHAPE_SPECS.filter { it.weightForTier(tier) > 0 }
    }

    fun getUnlockedVarietyCount(score: Int): Int {
        return getAvailableSpecsForScore(score).size
    }

    fun getNewShapesUnlockedCount(previousScore: Int, newScore: Int): Int {
        return (getUnlockedVarietyCount(newScore) - getUnlockedVarietyCount(previousScore)).coerceAtLeast(0)
    }

    private fun pickWeightedSpec(
        candidates: List<ShapeSpec>,
        score: Int,
        random: Random
    ): ShapeSpec {
        val totalWeight = candidates.sumOf { it.weightForScore(score) }
        if (totalWeight <= 0) return candidates[random.nextInt(candidates.size)]

        var roll = random.nextInt(totalWeight)
        for (spec in candidates) {
            roll -= spec.weightForScore(score)
            if (roll < 0) return spec
        }
        return candidates.last()
    }

    fun getShapeBatch(random: Random, currentScore: Int = 0): List<BlockShape> {
        val tier = DifficultyTier.fromScore(currentScore)
        val tierPool = ALL_SHAPE_SPECS.filter { it.weightForTier(tier) > 0 }
        val reliefMaxCells = if (tier.level >= DifficultyTier.MASTER.level) 4 else 3

        val result = mutableListOf<BlockShape>()
        val usedIds = mutableSetOf<String>()
        var hasReliefPiece = false

        for (i in 0 until 3) {
            val distinctPool = tierPool.filter { it.id !in usedIds }.ifEmpty { tierPool }
            val candidate = if (i == 2 && !hasReliefPiece) {
                val reliefPool = distinctPool.filter { it.cellCount <= reliefMaxCells }
                    .ifEmpty { tierPool.filter { it.cellCount <= reliefMaxCells } }
                if (reliefPool.isNotEmpty()) {
                    pickWeightedSpec(reliefPool, currentScore, random)
                } else {
                    pickWeightedSpec(distinctPool, currentScore, random)
                }
            } else {
                pickWeightedSpec(distinctPool, currentScore, random)
            }

            if (candidate.cellCount <= reliefMaxCells) {
                hasReliefPiece = true
            }
            usedIds.add(candidate.id)
            result.add(candidate.toBlockShape())
        }
        return result
    }

    fun getRandomShapeBatch(currentScore: Int = 0): List<BlockShape> {
        return getShapeBatch(Random.Default, currentScore)
    }

    /**
     * Deterministic batch generation for Daily Challenge mode.
     * Given the same dateSeed, batchIndex, and score bracket, players globally receive identical pieces.
     */
    fun getDailyShapeBatch(dateSeed: Long, batchIndex: Int, currentScore: Int = 0): List<BlockShape> {
        val seededRandom = Random(dateSeed + (batchIndex * 1337L))
        return getShapeBatch(seededRandom, currentScore)
    }
}
