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
}
