package com.example.model

import java.util.UUID
import kotlin.random.Random

data class BlockShape(
    val instanceId: String = UUID.randomUUID().toString(),
    val id: String,
    val name: String,
    val matrix: List<List<Boolean>>,
    val colorId: Int
) {
    val height: Int = matrix.size
    val width: Int = if (matrix.isNotEmpty()) matrix[0].size else 0
    val cellCount: Int = matrix.sumOf { row -> row.count { it } }
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

    // Squares
    private val SQUARE_2X2 = listOf(
        listOf(true, true),
        listOf(true, true)
    )
    private val SQUARE_3X3 = listOf(
        listOf(true, true, true),
        listOf(true, true, true),
        listOf(true, true, true)
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

    // Diagonal Step (2 blocks)
    private val DIAG_1 = listOf(
        listOf(true, false),
        listOf(false, true)
    )
    private val DIAG_2 = listOf(
        listOf(false, true),
        listOf(true, false)
    )

    private val ALL_SHAPES_WITH_COLOR = listOf(
        Triple("dot_1x1", DOT_1X1, 3), // Golden
        Triple("line_2_h", LINE_2_H, 5), // Cyan
        Triple("line_2_v", LINE_2_V, 5),
        Triple("line_3_h", LINE_3_H, 2), // Orange
        Triple("line_3_v", LINE_3_V, 2),
        Triple("line_4_h", LINE_4_H, 1), // Red
        Triple("line_4_v", LINE_4_V, 1),
        Triple("line_5_h", LINE_5_H, 6), // Purple
        Triple("line_5_v", LINE_5_V, 6),
        Triple("square_2x2", SQUARE_2X2, 4), // Green
        Triple("square_3x3", SQUARE_3X3, 3), // Amber
        Triple("corner_tl", CORNER_TL, 7), // Pink
        Triple("corner_tr", CORNER_TR, 7),
        Triple("corner_bl", CORNER_BL, 7),
        Triple("corner_br", CORNER_BR, 7),
        Triple("l_norm_1", L_NORMAL_1, 8), // Aqua
        Triple("l_norm_2", L_NORMAL_2, 8),
        Triple("l_norm_3", L_NORMAL_3, 8),
        Triple("l_norm_4", L_NORMAL_4, 8),
        Triple("l_flat_1", L_FLAT_1, 2),
        Triple("l_flat_2", L_FLAT_2, 2),
        Triple("l_flat_3", L_FLAT_3, 2),
        Triple("l_flat_4", L_FLAT_4, 2),
        Triple("big_corner_tl", BIG_CORNER_TL, 6),
        Triple("big_corner_tr", BIG_CORNER_TR, 6),
        Triple("big_corner_bl", BIG_CORNER_BL, 6),
        Triple("big_corner_br", BIG_CORNER_BR, 6),
        Triple("t_down", T_DOWN, 5),
        Triple("t_up", T_UP, 5),
        Triple("t_left", T_LEFT, 5),
        Triple("t_right", T_RIGHT, 5),
        Triple("z_shape", Z_SHAPE, 1),
        Triple("s_shape", S_SHAPE, 4),
        Triple("z_vert", Z_VERT, 1),
        Triple("s_vert", S_VERT, 4),
        Triple("plus", PLUS, 7),
        Triple("diag_1", DIAG_1, 8),
        Triple("diag_2", DIAG_2, 8)
    )

    fun getRandomShapeBatch(): List<BlockShape> {
        val result = mutableListOf<BlockShape>()
        var hasSmallPiece = false

        for (i in 0 until 3) {
            val candidate = if (i == 2 && !hasSmallPiece) {
                // Ensure at least one small piece (< 4 blocks) in the batch
                ALL_SHAPES_WITH_COLOR.filter { it.second.sumOf { r -> r.count { cell -> cell } } <= 3 }.random()
            } else {
                ALL_SHAPES_WITH_COLOR.random()
            }

            val cells = candidate.second.sumOf { r -> r.count { it } }
            if (cells <= 3) hasSmallPiece = true

            result.add(
                BlockShape(
                    id = candidate.first,
                    name = candidate.first,
                    matrix = candidate.second,
                    colorId = candidate.third
                )
            )
        }
        return result
    }
}
