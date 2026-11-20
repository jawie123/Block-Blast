package com.example.model

import androidx.compose.runtime.Immutable

enum class AdventureGoalType {
    COLLECT_GEMS,
    BREAK_ICE,
    BREAK_CHAINS,
    SCORE_TARGET
}

@Immutable
data class AdventureLevelSpec(
    val levelNumber: Int,
    val chapter: Int,
    val chapterTitle: String,
    val title: String,
    val goalType: AdventureGoalType,
    val targetCount: Int,
    val coinReward: Int,
    val hammerReward: Int = 0,
    val rotateReward: Int = 1,
    val twoStarScore: Int,
    val threeStarScore: Int,
    val initialGrid: List<List<Int>>,
    val initialIceCells: Map<Pair<Int, Int>, Int> = emptyMap(),
    val initialGemCells: Set<Pair<Int, Int>> = emptySet(),
    val initialLockedCells: Set<Pair<Int, Int>> = emptySet(),
    val initialMysteryCells: Set<Pair<Int, Int>> = emptySet()
)

object AdventureCatalog {

    val allLevels: List<AdventureLevelSpec> by lazy { buildLevels() }

    val totalLevels: Int get() = 30

    fun level(levelNumber: Int): AdventureLevelSpec {
        val clamped = levelNumber.coerceIn(1, totalLevels)
        return allLevels[clamped - 1]
    }

    fun computeStars(spec: AdventureLevelSpec, score: Int): Int {
        return when {
            score >= spec.threeStarScore -> 3
            score >= spec.twoStarScore -> 2
            else -> 1
        }
    }

    private fun buildLevels(): List<AdventureLevelSpec> {
        return listOf(
            // Chapter 1: Neon Awakening (Levels 1-5)
            buildLevel(
                num = 1, chapter = 1, chapterTitle = "Neon Awakening", title = "Gem Spark",
                goalType = AdventureGoalType.COLLECT_GEMS, targetCount = 4,
                coinReward = 60, hammerReward = 0, rotateReward = 1,
                twoStar = 650, threeStar = 1100,
                iceHits = 1,
                pattern = listOf(
                    "..2..2..",
                    "........",
                    ".G....G.",
                    "...44...",
                    "...44...",
                    ".G....G.",
                    "........",
                    "..3..3.."
                )
            ),
            buildLevel(
                num = 2, chapter = 1, chapterTitle = "Neon Awakening", title = "First Blast",
                goalType = AdventureGoalType.SCORE_TARGET, targetCount = 700,
                coinReward = 70, hammerReward = 0, rotateReward = 1,
                twoStar = 1100, threeStar = 1650,
                iceHits = 1,
                pattern = listOf(
                    "........",
                    ".11..11.",
                    ".1....1.",
                    "........",
                    "........",
                    ".5....5.",
                    ".55..55.",
                    "........"
                )
            ),
            buildLevel(
                num = 3, chapter = 1, chapterTitle = "Neon Awakening", title = "Frost Crack",
                goalType = AdventureGoalType.BREAK_ICE, targetCount = 4,
                coinReward = 80, hammerReward = 1, rotateReward = 0,
                twoStar = 850, threeStar = 1400,
                iceHits = 1,
                pattern = listOf(
                    "........",
                    "..I..I..",
                    ".2....2.",
                    "........",
                    "........",
                    ".6....6.",
                    "..I..I..",
                    "........"
                )
            ),
            buildLevel(
                num = 4, chapter = 1, chapterTitle = "Neon Awakening", title = "Crystal Cross",
                goalType = AdventureGoalType.COLLECT_GEMS, targetCount = 6,
                coinReward = 90, hammerReward = 0, rotateReward = 1,
                twoStar = 1000, threeStar = 1600,
                iceHits = 1,
                pattern = listOf(
                    "...G....",
                    "....3...",
                    "..5..G..",
                    "G..11..G",
                    "...11...",
                    "..G..5..",
                    "...3....",
                    "....G..."
                )
            ),
            buildLevel(
                num = 5, chapter = 1, chapterTitle = "Neon Awakening", title = "Awakening Boss",
                goalType = AdventureGoalType.SCORE_TARGET, targetCount = 1300,
                coinReward = 120, hammerReward = 1, rotateReward = 1,
                twoStar = 1900, threeStar = 2600,
                iceHits = 1,
                pattern = listOf(
                    "1......1",
                    ".I.44.I.",
                    "..3..3..",
                    ".2....2.",
                    ".2....2.",
                    "..6..6..",
                    ".I.55.I.",
                    "1......1"
                )
            ),

            // Chapter 2: Frost Cavern (Levels 6-10)
            buildLevel(
                num = 6, chapter = 2, chapterTitle = "Frost Cavern", title = "Glacier Gate",
                goalType = AdventureGoalType.BREAK_ICE, targetCount = 6,
                coinReward = 90, hammerReward = 0, rotateReward = 1,
                twoStar = 1150, threeStar = 1800,
                iceHits = 1,
                pattern = listOf(
                    "..I..I..",
                    "........",
                    "I.4..4.I",
                    "........",
                    "........",
                    "I.5..5.I",
                    "........",
                    "..2..2.."
                )
            ),
            buildLevel(
                num = 7, chapter = 2, chapterTitle = "Frost Cavern", title = "Frozen Gems",
                goalType = AdventureGoalType.COLLECT_GEMS, targetCount = 8,
                coinReward = 100, hammerReward = 1, rotateReward = 0,
                twoStar = 1250, threeStar = 2000,
                iceHits = 1,
                pattern = listOf(
                    ".G....G.",
                    "..I..I..",
                    "3......3",
                    "..G..G..",
                    "..G..G..",
                    "4......4",
                    "..I..I..",
                    ".G....G."
                )
            ),
            buildLevel(
                num = 8, chapter = 2, chapterTitle = "Frost Cavern", title = "Double Chill",
                goalType = AdventureGoalType.BREAK_ICE, targetCount = 4,
                coinReward = 110, hammerReward = 0, rotateReward = 1,
                twoStar = 1400, threeStar = 2200,
                iceHits = 2,
                pattern = listOf(
                    "........",
                    "..2..2..",
                    ".I....I.",
                    "...66...",
                    "...66...",
                    ".I....I.",
                    "..4..4..",
                    "........"
                )
            ),
            buildLevel(
                num = 9, chapter = 2, chapterTitle = "Frost Cavern", title = "Snowdrift Surge",
                goalType = AdventureGoalType.SCORE_TARGET, targetCount = 1750,
                coinReward = 115, hammerReward = 1, rotateReward = 0,
                twoStar = 2400, threeStar = 3200,
                iceHits = 1,
                pattern = listOf(
                    "2......2",
                    ".3....3.",
                    "..I..I..",
                    "...55...",
                    "...55...",
                    "..I..I..",
                    ".4....4.",
                    "2......2"
                )
            ),
            buildLevel(
                num = 10, chapter = 2, chapterTitle = "Frost Cavern", title = "Cavern Guardian",
                goalType = AdventureGoalType.BREAK_ICE, targetCount = 6,
                coinReward = 150, hammerReward = 1, rotateReward = 2,
                twoStar = 2000, threeStar = 2900,
                iceHits = 2,
                pattern = listOf(
                    "I......I",
                    ".3....3.",
                    "..I..I..",
                    "...77...",
                    "...77...",
                    "........",
                    ".5.II.5.",
                    "........"
                )
            ),

            // Chapter 3: Crystal Vault (Levels 11-15)
            buildLevel(
                num = 11, chapter = 3, chapterTitle = "Crystal Vault", title = "Prism Hunt",
                goalType = AdventureGoalType.COLLECT_GEMS, targetCount = 10,
                coinReward = 120, hammerReward = 0, rotateReward = 1,
                twoStar = 1700, threeStar = 2600,
                iceHits = 2,
                pattern = listOf(
                    ".G.11.G.",
                    "........",
                    "G..II..G",
                    ".5....5.",
                    ".6....6.",
                    "G..44..G",
                    "........",
                    ".G.33.G."
                )
            ),
            buildLevel(
                num = 12, chapter = 3, chapterTitle = "Crystal Vault", title = "Vault Lock",
                goalType = AdventureGoalType.BREAK_ICE, targetCount = 7,
                coinReward = 130, hammerReward = 1, rotateReward = 0,
                twoStar = 2100, threeStar = 3000,
                iceHits = 2,
                pattern = listOf(
                    "..I..I..",
                    ".1....1.",
                    "I..55..I",
                    "........",
                    "........",
                    "I..33..I",
                    ".4....4.",
                    "...I...."
                )
            ),
            buildLevel(
                num = 13, chapter = 3, chapterTitle = "Crystal Vault", title = "Neon Surge",
                goalType = AdventureGoalType.SCORE_TARGET, targetCount = 2400,
                coinReward = 135, hammerReward = 0, rotateReward = 2,
                twoStar = 3100, threeStar = 4200,
                iceHits = 2,
                pattern = listOf(
                    "1..22..1",
                    "........",
                    ".I.44.I.",
                    "..6..6..",
                    "..6..6..",
                    ".I.55.I.",
                    "........",
                    "3..77..3"
                )
            ),
            buildLevel(
                num = 14, chapter = 3, chapterTitle = "Crystal Vault", title = "Treasure Matrix",
                goalType = AdventureGoalType.COLLECT_GEMS, targetCount = 12,
                coinReward = 145, hammerReward = 1, rotateReward = 1,
                twoStar = 2200, threeStar = 3300,
                iceHits = 2,
                pattern = listOf(
                    "G..33..G",
                    ".I....I.",
                    "..G..G..",
                    "4......4",
                    "5......5",
                    "..G..G..",
                    ".I....I.",
                    "G..66..G"
                )
            ),
            buildLevel(
                num = 15, chapter = 3, chapterTitle = "Crystal Vault", title = "Vault Warden",
                goalType = AdventureGoalType.BREAK_ICE, targetCount = 8,
                coinReward = 180, hammerReward = 1, rotateReward = 2,
                twoStar = 2600, threeStar = 3800,
                iceHits = 2,
                pattern = listOf(
                    "I..22..I",
                    ".4....4.",
                    "..I..I..",
                    "3..66..3",
                    "3..66..3",
                    "..I..I..",
                    ".5....5.",
                    "I..77..I"
                )
            ),

            // Chapter 4: Obsidian Crown (Levels 16-20)
            buildLevel(
                num = 16, chapter = 4, chapterTitle = "Obsidian Crown", title = "Royal Trial",
                goalType = AdventureGoalType.SCORE_TARGET, targetCount = 3200,
                coinReward = 160, hammerReward = 1, rotateReward = 1,
                twoStar = 4000, threeStar = 5200,
                iceHits = 2,
                pattern = listOf(
                    ".2.II.2.",
                    "1......1",
                    "..4..4..",
                    "I..88..I",
                    "I..88..I",
                    "..5..5..",
                    "3......3",
                    ".7.II.7."
                )
            ),
            buildLevel(
                num = 17, chapter = 4, chapterTitle = "Obsidian Crown", title = "Diamond Rain",
                goalType = AdventureGoalType.COLLECT_GEMS, targetCount = 14,
                coinReward = 170, hammerReward = 1, rotateReward = 2,
                twoStar = 2800, threeStar = 4000,
                iceHits = 2,
                pattern = listOf(
                    "G.I..I.G",
                    ".G....G.",
                    "2..G...2",
                    "...44..G",
                    "G..44...",
                    "6...G..6",
                    ".G....G.",
                    "G.I..I.G"
                )
            ),
            buildLevel(
                num = 18, chapter = 4, chapterTitle = "Obsidian Crown", title = "Permafrost",
                goalType = AdventureGoalType.BREAK_ICE, targetCount = 10,
                coinReward = 180, hammerReward = 2, rotateReward = 1,
                twoStar = 3200, threeStar = 4500,
                iceHits = 2,
                pattern = listOf(
                    "I.3..3.I",
                    ".I....I.",
                    "4..II..4",
                    "........",
                    "........",
                    "5..II..5",
                    ".I....I.",
                    "I.6..6.I"
                )
            ),
            buildLevel(
                num = 19, chapter = 4, chapterTitle = "Obsidian Crown", title = "Apex Challenge",
                goalType = AdventureGoalType.SCORE_TARGET, targetCount = 4200,
                coinReward = 200, hammerReward = 1, rotateReward = 2,
                twoStar = 5200, threeStar = 6600,
                iceHits = 2,
                pattern = listOf(
                    "8.I..I.8",
                    ".1.55.1.",
                    "I......I",
                    ".3.77.3.",
                    ".4.66.4.",
                    "I......I",
                    ".2.55.2.",
                    "8.I..I.8"
                )
            ),
            buildLevel(
                num = 20, chapter = 4, chapterTitle = "Obsidian Crown", title = "Grand Crown",
                goalType = AdventureGoalType.COLLECT_GEMS, targetCount = 16,
                coinReward = 300, hammerReward = 2, rotateReward = 3,
                twoStar = 4500, threeStar = 6500,
                iceHits = 2,
                pattern = listOf(
                    "G.I..I.G",
                    ".8.GG.8.",
                    "I.4..4.I",
                    ".G.22.G.",
                    ".G.33.G.",
                    "I.5..5.I",
                    ".8.GG.8.",
                    "G.I..I.G"
                )
            ),

            // Chapter 5: Astral Citadel (Levels 21-30) with Chained Tiles (L) & Mystery Crates (M)
            buildLevel(
                num = 21, chapter = 5, chapterTitle = "Astral Citadel", title = "Iron Gate",
                goalType = AdventureGoalType.BREAK_CHAINS, targetCount = 4,
                coinReward = 175, hammerReward = 1, rotateReward = 1,
                twoStar = 2400, threeStar = 3600,
                iceHits = 1,
                pattern = listOf(
                    "........",
                    ".L..M..L",
                    "..2..2..",
                    "........",
                    "........",
                    "..5..5..",
                    ".L..M..L",
                    "........"
                )
            ),
            buildLevel(
                num = 22, chapter = 5, chapterTitle = "Astral Citadel", title = "Starlit Crates",
                goalType = AdventureGoalType.COLLECT_GEMS, targetCount = 10,
                coinReward = 185, hammerReward = 0, rotateReward = 2,
                twoStar = 2800, threeStar = 4200,
                iceHits = 2,
                pattern = listOf(
                    "G..MM..G",
                    ".L....L.",
                    "..3..3..",
                    "G..44..G",
                    "G..55..G",
                    "..6..6..",
                    ".L....L.",
                    "G..MM..G"
                )
            ),
            buildLevel(
                num = 23, chapter = 5, chapterTitle = "Astral Citadel", title = "Chainbreaker",
                goalType = AdventureGoalType.BREAK_CHAINS, targetCount = 6,
                coinReward = 195, hammerReward = 1, rotateReward = 1,
                twoStar = 3000, threeStar = 4400,
                iceHits = 2,
                pattern = listOf(
                    "L......L",
                    ".2.MM.2.",
                    "..L..L..",
                    "...66...",
                    "...66...",
                    "..L..L..",
                    ".4....4.",
                    "........"
                )
            ),
            buildLevel(
                num = 24, chapter = 5, chapterTitle = "Astral Citadel", title = "Nebula Vault",
                goalType = AdventureGoalType.SCORE_TARGET, targetCount = 4800,
                coinReward = 210, hammerReward = 1, rotateReward = 2,
                twoStar = 5800, threeStar = 7400,
                iceHits = 2,
                pattern = listOf(
                    "M.1..1.M",
                    ".L....L.",
                    "..I..I..",
                    "3..77..3",
                    "4..77..4",
                    "..I..I..",
                    ".L....L.",
                    "M.5..5.M"
                )
            ),
            buildLevel(
                num = 25, chapter = 5, chapterTitle = "Astral Citadel", title = "Frost & Iron",
                goalType = AdventureGoalType.BREAK_ICE, targetCount = 8,
                coinReward = 220, hammerReward = 2, rotateReward = 1,
                twoStar = 3500, threeStar = 5000,
                iceHits = 2,
                pattern = listOf(
                    "I.L..L.I",
                    ".M....M.",
                    "I..22..I",
                    "..5..5..",
                    "..6..6..",
                    "I..33..I",
                    ".M....M.",
                    "I.L..L.I"
                )
            ),
            buildLevel(
                num = 26, chapter = 5, chapterTitle = "Astral Citadel", title = "Celestial Lock",
                goalType = AdventureGoalType.BREAK_CHAINS, targetCount = 8,
                coinReward = 230, hammerReward = 1, rotateReward = 2,
                twoStar = 3800, threeStar = 5400,
                iceHits = 2,
                pattern = listOf(
                    "L..22..L",
                    ".M....M.",
                    "L.I..I.L",
                    "...88...",
                    "...88...",
                    "L.I..I.L",
                    ".M....M.",
                    "L..33..L"
                )
            ),
            buildLevel(
                num = 27, chapter = 5, chapterTitle = "Astral Citadel", title = "Comet Treasures",
                goalType = AdventureGoalType.COLLECT_GEMS, targetCount = 14,
                coinReward = 245, hammerReward = 1, rotateReward = 2,
                twoStar = 4200, threeStar = 5900,
                iceHits = 2,
                pattern = listOf(
                    "G.M..M.G",
                    ".L.33.L.",
                    "G..44..G",
                    ".G....G.",
                    ".G....G.",
                    "G..55..G",
                    ".L.66.L.",
                    "G.M..M.G"
                )
            ),
            buildLevel(
                num = 28, chapter = 5, chapterTitle = "Astral Citadel", title = "Astral Siege",
                goalType = AdventureGoalType.BREAK_CHAINS, targetCount = 10,
                coinReward = 260, hammerReward = 2, rotateReward = 2,
                twoStar = 4500, threeStar = 6400,
                iceHits = 2,
                pattern = listOf(
                    "L.1..1.L",
                    ".L.MM.L.",
                    "3..LL..3",
                    "..I..I..",
                    "..I..I..",
                    "4..LL..4",
                    ".L.MM.L.",
                    "L.5..5.L"
                )
            ),
            buildLevel(
                num = 29, chapter = 5, chapterTitle = "Astral Citadel", title = "Citadel Ascent",
                goalType = AdventureGoalType.SCORE_TARGET, targetCount = 5800,
                coinReward = 280, hammerReward = 2, rotateReward = 2,
                twoStar = 7000, threeStar = 8800,
                iceHits = 2,
                pattern = listOf(
                    "M.L..L.M",
                    ".2.II.2.",
                    "L..66..L",
                    ".5.MM.5.",
                    ".7.MM.7.",
                    "L..88..L",
                    ".3.II.3.",
                    "M.L..L.M"
                )
            ),
            buildLevel(
                num = 30, chapter = 5, chapterTitle = "Astral Citadel", title = "Astral Sovereign",
                goalType = AdventureGoalType.BREAK_CHAINS, targetCount = 12,
                coinReward = 400, hammerReward = 3, rotateReward = 3,
                twoStar = 5500, threeStar = 7800,
                iceHits = 2,
                pattern = listOf(
                    "L.M..M.L",
                    ".L.II.L.",
                    "M.L..L.M",
                    ".G.88.G.",
                    ".G.88.G.",
                    "M.L..L.M",
                    ".L.II.L.",
                    "L.M..M.L"
                )
            )
        )
    }

    private fun buildLevel(
        num: Int,
        chapter: Int,
        chapterTitle: String,
        title: String,
        goalType: AdventureGoalType,
        targetCount: Int,
        coinReward: Int,
        hammerReward: Int,
        rotateReward: Int,
        twoStar: Int,
        threeStar: Int,
        iceHits: Int,
        pattern: List<String>
    ): AdventureLevelSpec {
        val iceMap = mutableMapOf<Pair<Int, Int>, Int>()
        val gemSet = mutableSetOf<Pair<Int, Int>>()
        val lockedSet = mutableSetOf<Pair<Int, Int>>()
        val mysterySet = mutableSetOf<Pair<Int, Int>>()
        val grid = List(8) { r ->
            val rowStr = pattern.getOrElse(r) { "........" }
            List(8) { c ->
                when (val ch = rowStr.getOrElse(c) { '.' }) {
                    '.' -> 0
                    'G' -> {
                        gemSet.add(r to c)
                        ((r + c + num) % 6) + 1
                    }
                    'I' -> {
                        iceMap[r to c] = iceHits.coerceIn(1, 2)
                        5 // Ice-bound cyan crystal block
                    }
                    'L' -> {
                        lockedSet.add(r to c)
                        ((r * 2 + c + num) % 6) + 1
                    }
                    'M' -> {
                        mysterySet.add(r to c)
                        ((r + c * 2 + num) % 6) + 1
                    }
                    in '1'..'8' -> ch.digitToInt()
                    else -> 0
                }
            }
        }
        return AdventureLevelSpec(
            levelNumber = num,
            chapter = chapter,
            chapterTitle = chapterTitle,
            title = title,
            goalType = goalType,
            targetCount = targetCount,
            coinReward = coinReward,
            hammerReward = hammerReward,
            rotateReward = rotateReward,
            twoStarScore = twoStar,
            threeStarScore = threeStar,
            initialGrid = grid,
            initialIceCells = iceMap,
            initialGemCells = gemSet,
            initialLockedCells = lockedSet,
            initialMysteryCells = mysterySet
        )
    }
}

object AdventureStarChest {
    const val STARS_PER_CHEST = 10
    const val MAX_CHESTS = 9
    const val COINS_REWARD = 100
    const val HAMMER_REWARD = 1
    const val COLOR_BLAST_REWARD = 1

    fun nextChestTarget(claimedCount: Int): Int? {
        if (claimedCount >= MAX_CHESTS) return null
        return (claimedCount + 1) * STARS_PER_CHEST
    }

    fun hasClaimableChest(totalStars: Int, claimedCount: Int): Boolean {
        val target = nextChestTarget(claimedCount) ?: return false
        return totalStars >= target
    }
}
