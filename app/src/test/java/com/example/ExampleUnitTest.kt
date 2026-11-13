package com.example

import com.example.model.BlockShape
import com.example.model.ShapeDefinitions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testShapeDefinitions() {
        val batch = ShapeDefinitions.getRandomShapeBatch()
        assertEquals(3, batch.size)
        assertTrue(batch.all { it.cellCount > 0 })
    }

    @Test
    fun testPlacementValidation() {
        val emptyGrid = List(8) { List(8) { 0 } }
        val dot = BlockShape(
            id = "dot",
            name = "dot",
            matrix = listOf(listOf(true)),
            colorId = 1
        )

        // Dot should fit anywhere in 8x8
        for (r in 0 until 8) {
            for (c in 0 until 8) {
                assertTrue(canPlaceTest(emptyGrid, dot, r, c))
            }
        }

        // Out of bounds
        assertFalse(canPlaceTest(emptyGrid, dot, -1, 0))
        assertFalse(canPlaceTest(emptyGrid, dot, 8, 0))
        assertFalse(canPlaceTest(emptyGrid, dot, 0, 8))

        // Occupied cell
        val occupiedGrid = emptyGrid.mapIndexed { r, row ->
            row.mapIndexed { c, v -> if (r == 3 && c == 4) 2 else v }
        }
        assertFalse(canPlaceTest(occupiedGrid, dot, 3, 4))
        assertTrue(canPlaceTest(occupiedGrid, dot, 3, 5))
    }

    @Test
    fun testSquare2x2Bounds() {
        val emptyGrid = List(8) { List(8) { 0 } }
        val square2x2 = BlockShape(
            id = "sq2",
            name = "sq2",
            matrix = listOf(listOf(true, true), listOf(true, true)),
            colorId = 2
        )

        assertTrue(canPlaceTest(emptyGrid, square2x2, 0, 0))
        assertTrue(canPlaceTest(emptyGrid, square2x2, 6, 6))
        assertFalse(canPlaceTest(emptyGrid, square2x2, 7, 7)) // Bottom-right would overflow
    }

    @Test
    fun testCheckGameOverDetection() {
        // Full board: no empty cells
        val fullGrid = List(8) { List(8) { 1 } }
        val shapes = listOf(
            BlockShape(
                id = "dot",
                name = "dot",
                matrix = listOf(listOf(true)),
                colorId = 1
            )
        )
        // With all cells occupied, dot cannot fit anywhere -> Game Over is true
        val isGameOver = checkGameOverTest(fullGrid, shapes)
        assertTrue(isGameOver)

        // Board with one cell empty
        val almostFullGrid = fullGrid.mapIndexed { r, row ->
            row.mapIndexed { c, v -> if (r == 4 && c == 4) 0 else v }
        }
        val isNotGameOver = checkGameOverTest(almostFullGrid, shapes)
        assertFalse(isNotGameOver)
    }

    @Test
    fun testProgressiveDifficultyTiersAndThresholds() {
        assertEquals(com.example.model.DifficultyTier.STARTER, com.example.model.DifficultyTier.fromScore(0))
        assertEquals(com.example.model.DifficultyTier.STARTER, com.example.model.DifficultyTier.fromScore(499))
        assertEquals(com.example.model.DifficultyTier.CHALLENGER, com.example.model.DifficultyTier.fromScore(500))
        assertEquals(com.example.model.DifficultyTier.CHALLENGER, com.example.model.DifficultyTier.fromScore(1499))
        assertEquals(com.example.model.DifficultyTier.TACTICIAN, com.example.model.DifficultyTier.fromScore(1500))
        assertEquals(com.example.model.DifficultyTier.TACTICIAN, com.example.model.DifficultyTier.fromScore(2999))
        assertEquals(com.example.model.DifficultyTier.MASTER, com.example.model.DifficultyTier.fromScore(3000))
        assertEquals(com.example.model.DifficultyTier.MASTER, com.example.model.DifficultyTier.fromScore(5999))
        assertEquals(com.example.model.DifficultyTier.GRANDMASTER, com.example.model.DifficultyTier.fromScore(6000))
        assertEquals(com.example.model.DifficultyTier.GRANDMASTER, com.example.model.DifficultyTier.fromScore(15000))
    }

    @Test
    fun testProgressiveDifficultyIncreasesShapeComplexityWithScore() {
        val sampleCount = 300
        val starterShapes = (0 until sampleCount).flatMap { i ->
            ShapeDefinitions.getShapeBatch(kotlin.random.Random(1000L + i), currentScore = 0)
        }
        val tacticianShapes = (0 until sampleCount).flatMap { i ->
            ShapeDefinitions.getShapeBatch(kotlin.random.Random(2000L + i), currentScore = 2000)
        }
        val grandmasterShapes = (0 until sampleCount).flatMap { i ->
            ShapeDefinitions.getShapeBatch(kotlin.random.Random(3000L + i), currentScore = 7000)
        }

        // Starter should never spawn high-tier expert or advanced irregular blocks
        val forbiddenAtStarter = setOf("square_3x3", "plus", "z_shape", "s_shape", "line_5_h", "line_5_v", "rect_2x3", "diag_3_a")
        assertTrue(starterShapes.none { it.id in forbiddenAtStarter })

        // Grandmaster unlocks expert shapes
        val expertIds = setOf("square_3x3", "u_up", "u_down", "stair_3_a", "stair_3_b", "rect_2x3", "rect_3x2", "diag_3_a", "diag_3_b")
        assertTrue(grandmasterShapes.any { it.id in expertIds })

        val starterAvgComplexity = starterShapes.map { it.complexity }.average()
        val tacticianAvgComplexity = tacticianShapes.map { it.complexity }.average()
        val grandmasterAvgComplexity = grandmasterShapes.map { it.complexity }.average()

        assertTrue(
            "Expected Tactician complexity ($tacticianAvgComplexity) > Starter ($starterAvgComplexity)",
            tacticianAvgComplexity > starterAvgComplexity
        )
        assertTrue(
            "Expected Grandmaster complexity ($grandmasterAvgComplexity) > Tactician ($tacticianAvgComplexity)",
            grandmasterAvgComplexity > tacticianAvgComplexity
        )

        val starterAvgCells = starterShapes.map { it.cellCount }.average()
        val grandmasterAvgCells = grandmasterShapes.map { it.cellCount }.average()
        assertTrue(
            "Expected Grandmaster cellCount ($grandmasterAvgCells) > Starter ($starterAvgCells)",
            grandmasterAvgCells > starterAvgCells
        )
    }

    private fun canPlaceTest(
        grid: List<List<Int>>,
        shape: BlockShape,
        startRow: Int,
        startCol: Int
    ): Boolean {
        for (r in 0 until shape.height) {
            for (c in 0 until shape.width) {
                if (shape.matrix[r][c]) {
                    val targetR = startRow + r
                    val targetC = startCol + c
                    if (targetR !in 0 until 8 || targetC !in 0 until 8) return false
                    if (grid[targetR][targetC] != 0) return false
                }
            }
        }
        return true
    }

    private fun checkGameOverTest(
        grid: List<List<Int>>,
        remainingPieces: List<BlockShape?>
    ): Boolean {
        val pieces = remainingPieces.filterNotNull()
        if (pieces.isEmpty()) return false
        for (piece in pieces) {
            for (r in 0 until 8) {
                for (c in 0 until 8) {
                    if (canPlaceTest(grid, piece, r, c)) {
                        return false
                    }
                }
            }
        }
        return true
    }
}
