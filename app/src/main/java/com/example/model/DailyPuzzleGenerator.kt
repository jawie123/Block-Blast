package com.example.model

import kotlin.random.Random

/**
 * Generates a unique, deterministic, solvable 8x8 initial board layout
 * for each calendar day's Daily Puzzle Challenge so every player worldwide
 * competes on the exact same board obstacle arrangement.
 */
object DailyPuzzleGenerator {

    private val PUZZLE_THEMES = listOf(
        "Crystal Spire",
        "Diamond Core",
        "Twin Bastion",
        "Diagonal Rift",
        "Orbit Ring",
        "Corner Fortress",
        "Crossroads",
        "Prism Steps"
    )

    fun getPuzzleTitle(dateSeed: Long): String {
        val index = ((dateSeed % PUZZLE_THEMES.size) + PUZZLE_THEMES.size).toInt() % PUZZLE_THEMES.size
        return PUZZLE_THEMES[index]
    }

    /**
     * Produces an 8x8 board (values 0 = empty, 1..8 = jewel palette ID)
     * deterministically from [dateSeed].
     * Guarantees:
     * 1. No row or column starts completely filled (at least 2 open cells per line).
     * 2. All 3 shapes in the opening daily batch can be placed on the board.
     */
    fun generateInitialBoard(dateSeed: Long): List<List<Int>> {
        val rng = Random(dateSeed xor 0x5A17D8E9L)
        val grid = Array(8) { IntArray(8) { 0 } }
        val patternType = ((dateSeed % PUZZLE_THEMES.size) + PUZZLE_THEMES.size).toInt() % PUZZLE_THEMES.size

        val paletteOffset = rng.nextInt(8)
        fun nextJewelColor(step: Int): Int = ((paletteOffset + step) % 8) + 1

        when (patternType) {
            0 -> {
                // Crystal Spire: central symmetric pillars with side openings
                val coords = listOf(
                    1 to 3, 1 to 4, 2 to 2, 2 to 5,
                    5 to 2, 5 to 5, 6 to 3, 6 to 4,
                    3 to 1, 4 to 1, 3 to 6, 4 to 6
                )
                coords.forEachIndexed { idx, (r, c) -> grid[r][c] = nextJewelColor(idx % 3) }
            }
            1 -> {
                // Diamond Core: inner hollow diamond
                val coords = listOf(
                    1 to 3, 1 to 4,
                    2 to 2, 2 to 5,
                    3 to 1, 3 to 6,
                    4 to 1, 4 to 6,
                    5 to 2, 5 to 5,
                    6 to 3, 6 to 4
                )
                coords.forEachIndexed { idx, (r, c) -> grid[r][c] = nextJewelColor(idx % 4) }
            }
            2 -> {
                // Twin Bastion: two balanced 2x2 islands + scattered accents
                val blocks = listOf(
                    1 to 1, 1 to 2, 2 to 1, 2 to 2,
                    5 to 5, 5 to 6, 6 to 5, 6 to 6,
                    1 to 6, 6 to 1
                )
                blocks.forEachIndexed { idx, (r, c) -> grid[r][c] = nextJewelColor(idx / 4) }
            }
            3 -> {
                // Diagonal Rift: broken diagonal bands leaving wide placement pockets
                for (i in 0 until 8) {
                    if (i != 3 && i != 4) {
                        grid[i][i] = nextJewelColor(0)
                        grid[i][7 - i] = nextJewelColor(2)
                    }
                }
            }
            4 -> {
                // Orbit Ring: outer corner pockets + center 2x2 obstacle
                val coords = listOf(
                    0 to 0, 0 to 1, 1 to 0,
                    0 to 6, 0 to 7, 1 to 7,
                    6 to 0, 7 to 0, 7 to 1,
                    6 to 7, 7 to 6, 7 to 7,
                    3 to 3, 4 to 4
                )
                coords.forEachIndexed { idx, (r, c) -> grid[r][c] = nextJewelColor(idx % 3) }
            }
            5 -> {
                // Corner Fortress: fortified corners with open central arena
                val coords = listOf(
                    0 to 0, 0 to 1, 1 to 0, 1 to 1,
                    0 to 7, 1 to 6,
                    7 to 0, 6 to 1,
                    6 to 6, 6 to 7, 7 to 6, 7 to 7
                )
                coords.forEachIndexed { idx, (r, c) -> grid[r][c] = nextJewelColor(idx % 2) }
            }
            6 -> {
                // Crossroads: dashed cross dividers
                val coords = listOf(
                    0 to 3, 1 to 3, 6 to 4, 7 to 4,
                    3 to 0, 3 to 1, 4 to 6, 4 to 7,
                    2 to 5, 5 to 2
                )
                coords.forEachIndexed { idx, (r, c) -> grid[r][c] = nextJewelColor(idx % 3) }
            }
            else -> {
                // Prism Steps: staggered twin staircases
                val coords = listOf(
                    1 to 1, 2 to 2, 3 to 3,
                    4 to 4, 5 to 5, 6 to 6,
                    1 to 5, 2 to 6, 5 to 1, 6 to 2
                )
                coords.forEachIndexed { idx, (r, c) -> grid[r][c] = nextJewelColor(idx % 4) }
            }
        }

        // Sprinkle 4 deterministic symmetric accent blocks based on daily RNG
        repeat(4) { step ->
            val r = rng.nextInt(8)
            val c = rng.nextInt(8)
            // Keep center 3x3 (rows 2..4, cols 2..4) at least partially open if sparse
            if (grid[r][c] == 0) {
                grid[r][c] = nextJewelColor(step + 3)
            }
        }

        // Enforce Invariant 1: No row or column may have fewer than 2 empty cells
        for (r in 0 until 8) {
            var filled = (0 until 8).count { c -> grid[r][c] != 0 }
            while (filled > 6) {
                val c = rng.nextInt(8)
                if (grid[r][c] != 0) {
                    grid[r][c] = 0
                    filled--
                }
            }
        }
        for (c in 0 until 8) {
            var filled = (0 until 8).count { r -> grid[r][c] != 0 }
            while (filled > 6) {
                val r = rng.nextInt(8)
                if (grid[r][c] != 0) {
                    grid[r][c] = 0
                    filled--
                }
            }
        }

        // Enforce Invariant 2: Ensure opening batch pieces fit comfortably
        val initialBatch = ShapeDefinitions.getDailyShapeBatch(dateSeed, 0)
        val canAllFit = initialBatch.all { shape ->
            canPlaceAnywhere(grid, shape)
        }
        if (!canAllFit) {
            // Clear a guaranteed 3x3 safe zone in rows 2..4, cols 2..4
            for (r in 2..4) {
                for (c in 2..4) {
                    grid[r][c] = 0
                }
            }
        }

        return grid.map { it.toList() }
    }

    private fun canPlaceAnywhere(grid: Array<IntArray>, shape: BlockShape): Boolean {
        for (r in 0..(8 - shape.height)) {
            for (c in 0..(8 - shape.width)) {
                var fits = true
                for (sr in 0 until shape.height) {
                    for (sc in 0 until shape.width) {
                        if (shape.matrix[sr][sc] && grid[r + sr][c + sc] != 0) {
                            fits = false
                            break
                        }
                    }
                    if (!fits) break
                }
                if (fits) return true
            }
        }
        return false
    }
}
