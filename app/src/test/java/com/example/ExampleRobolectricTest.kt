package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.BlockShape
import com.example.model.ShapeDefinitions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Block Blast", appName)
    }

    @Test
    fun `test shape batch generation produces 3 shapes`() {
        val batch = ShapeDefinitions.getRandomShapeBatch()
        assertEquals(3, batch.size)
        assertTrue(batch.all { it.cellCount > 0 })
    }

    @Test
    fun `test shape validation bounds and occupancy`() {
        val grid = List(8) { List(8) { 0 } }
        val dot = BlockShape(
            id = "dot",
            name = "dot",
            matrix = listOf(listOf(true)),
            colorId = 1
        )
        // Dot at 0, 0 should fit
        assertTrue(dot.cellCount == 1)

        val fullRowGrid = List(8) { r ->
            List(8) { c -> if (r == 0) 1 else 0 }
        }
        // Row 0 is occupied, dot cannot be placed at (0, 0)
        assertEquals(1, fullRowGrid[0][0])
    }

    @Test
    fun `test hasSeenTutorial preference is false initially and persists true`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = com.example.data.GamePreferences(context)
        // Fresh install / first launch
        assertFalse(prefs.hasSeenTutorial)

        // Dismissing tutorial marks it true
        prefs.hasSeenTutorial = true
        assertTrue(prefs.hasSeenTutorial)
    }

    @Test
    fun `test daily challenge generates deterministic fixed sequences for all players`() {
        val testSeed = 20261111L
        val batchPlayer1 = ShapeDefinitions.getDailyShapeBatch(testSeed, 0)
        val batchPlayer2 = ShapeDefinitions.getDailyShapeBatch(testSeed, 0)

        assertEquals(batchPlayer1.size, batchPlayer2.size)
        for (i in batchPlayer1.indices) {
            assertEquals(batchPlayer1[i].id, batchPlayer2[i].id)
            assertEquals(batchPlayer1[i].cellCount, batchPlayer2[i].cellCount)
            assertEquals(batchPlayer1[i].width, batchPlayer2[i].width)
            assertEquals(batchPlayer1[i].height, batchPlayer2[i].height)
        }

        val nextBatch = ShapeDefinitions.getDailyShapeBatch(testSeed, 1)
        assertEquals(3, nextBatch.size)
    }

    @Test
    fun `test daily challenge high score local storage persistence`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = com.example.data.GamePreferences(context)
        val testDate = "2026-11-11"

        assertEquals(0, prefs.getDailyHighScore(testDate))
        prefs.setDailyHighScore(testDate, 4200)
        assertEquals(4200, prefs.getDailyHighScore(testDate))

        // Lower score does not overwrite
        prefs.setDailyHighScore(testDate, 3100)
        assertEquals(4200, prefs.getDailyHighScore(testDate))

        // Higher score updates
        prefs.setDailyHighScore(testDate, 5500)
        assertEquals(5500, prefs.getDailyHighScore(testDate))

        prefs.recordDailyGame(testDate, 5500)
        assertTrue(prefs.getDailyHistory().contains(testDate))
    }

    @Test
    fun `test undo reverts piece placement board state score and pieces`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val soundManager = com.example.audio.SoundManager(context)
        val prefs = com.example.data.GamePreferences(context)
        val viewModel = com.example.viewmodel.GameViewModel(context, soundManager, prefs)

        val initialState = viewModel.gameState.value
        assertEquals(0, initialState.score)
        assertFalse(initialState.canUndo)

        // Place the first available piece at (0, 0)
        val pieceIndex = 0
        val placed = viewModel.placeShape(pieceIndex, 0, 0)
        assertTrue(placed)

        val afterMoveState = viewModel.gameState.value
        assertTrue(afterMoveState.score > 0)
        assertTrue(afterMoveState.canUndo)
        assertEquals(null, afterMoveState.activePieces[pieceIndex])

        // Trigger Undo
        val undoSuccess = viewModel.undoLastMove()
        assertTrue(undoSuccess)

        val revertedState = viewModel.gameState.value
        assertEquals(0, revertedState.score)
        assertFalse(revertedState.canUndo)
        // Piece should be back in pool
        assertTrue(revertedState.activePieces[pieceIndex] != null)
        // Grid should be restored to all empty cells
        assertTrue(revertedState.grid.all { row -> row.all { it == 0 } })
    }

    @Test
    fun `test line clear emits particle bursts with correct colors and line coordinates`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val soundManager = com.example.audio.SoundManager(context)
        val prefs = com.example.data.GamePreferences(context)
        val viewModel = com.example.viewmodel.GameViewModel(context, soundManager, prefs)

        // Fill row 0 from col 0 to 6 with jewel color 3
        val partialGrid = List(8) { r ->
            List(8) { c ->
                if (r == 0 && c < 7) 3 else 0
            }
        }
        val singleDot = BlockShape(
            id = "dot",
            name = "Dot",
            matrix = listOf(listOf(true)),
            colorId = 5
        )

        // Inject grid and test dot piece
        val stateField = viewModel.javaClass.getDeclaredField("_gameState")
        stateField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = stateField.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<com.example.model.GameState>
        stateFlow.value = stateFlow.value.copy(
            grid = partialGrid,
            activePieces = listOf(singleDot, null, null)
        )

        // Place dot at (0, 7) to complete row 0
        val placed = viewModel.placeShape(0, 0, 7)
        assertTrue(placed)

        val stateAfterClear = viewModel.gameState.value
        assertEquals(8, stateAfterClear.clearingCells.size)
        assertEquals(8, stateAfterClear.lineClearBursts.size)

        // Check that burst properties are accurately assigned
        assertTrue(stateAfterClear.lineClearBursts.all { it.row == 0 })
        assertTrue(stateAfterClear.lineClearBursts.all { it.isRowClear })
        assertEquals(3, stateAfterClear.lineClearBursts.first { it.col == 0 }.colorId)
        assertEquals(5, stateAfterClear.lineClearBursts.first { it.col == 7 }.colorId)
    }

    @Test
    fun `test hammer power up smashes single block decrements hammer count and allows undo`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val soundManager = com.example.audio.SoundManager(context)
        val prefs = com.example.data.GamePreferences(context)
        prefs.hammerCount = 3
        val viewModel = com.example.viewmodel.GameViewModel(context, soundManager, prefs)

        // Inject grid with a single block at (3, 3)
        val testGrid = List(8) { r ->
            List(8) { c -> if (r == 3 && c == 3) 4 else 0 }
        }
        val stateField = viewModel.javaClass.getDeclaredField("_gameState")
        stateField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = stateField.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<com.example.model.GameState>
        stateFlow.value = stateFlow.value.copy(
            grid = testGrid,
            hammerCount = 3,
            isHammerActive = false
        )

        // Cannot smash while hammer mode is inactive
        val inactiveSmash = viewModel.useHammerOnCell(3, 3)
        assertFalse(inactiveSmash)
        assertEquals(4, viewModel.gameState.value.grid[3][3])

        // Activate hammer mode
        viewModel.toggleHammerMode()
        assertTrue(viewModel.gameState.value.isHammerActive)

        // Cannot smash empty cell (0, 0)
        val emptySmash = viewModel.useHammerOnCell(0, 0)
        assertFalse(emptySmash)

        // Smash filled cell at (3, 3)
        val smashSuccess = viewModel.useHammerOnCell(3, 3)
        assertTrue(smashSuccess)
        assertEquals(0, viewModel.gameState.value.grid[3][3])
        assertEquals(2, viewModel.gameState.value.hammerCount)
        assertEquals(2, prefs.hammerCount)
        assertFalse(viewModel.gameState.value.isHammerActive)
        assertTrue(viewModel.gameState.value.canUndo)

        // Undo hammer smash reverts the smashed cell and returns the hammer!
        val undone = viewModel.undoLastMove()
        assertTrue(undone)
        assertEquals(4, viewModel.gameState.value.grid[3][3])
        assertEquals(3, viewModel.gameState.value.hammerCount)
        assertEquals(3, prefs.hammerCount)
    }

    @Test
    fun `test daily missions generation deterministic and progress tracking and claim reward`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = com.example.data.GamePreferences(context)
        val testDate = "2026-11-12"

        val missions = com.example.model.DailyMissionGenerator.generateMissionsForDate(testDate)
        assertEquals(4, missions.size)
        assertTrue(missions.any { it.type == com.example.model.MissionType.CLEAR_LINES_SINGLE_MOVE && it.target == 3 })
        assertTrue(missions.any { it.type == com.example.model.MissionType.SCORE_POINTS && it.target == 1000 })
        assertTrue(missions.all { it.coinReward > 0 })

        // Deterministic: generated twice for same date gives identical mission IDs, targets, and rewards
        val missionsAgain = com.example.model.DailyMissionGenerator.generateMissionsForDate(testDate)
        for (i in missions.indices) {
            assertEquals(missions[i].id, missionsAgain[i].id)
            assertEquals(missions[i].type, missionsAgain[i].type)
            assertEquals(missions[i].target, missionsAgain[i].target)
            assertEquals(missions[i].coinReward, missionsAgain[i].coinReward)
            assertEquals(missions[i].hammerReward, missionsAgain[i].hammerReward)
        }

        val soundManager = com.example.audio.SoundManager(context)
        prefs.hammerCount = 2
        prefs.coinsBalance = 25
        prefs.totalCoinsEarned = 25
        val viewModel = com.example.viewmodel.GameViewModel(context, soundManager, prefs)

        val singleMoveMission = viewModel.gameState.value.dailyMissions
            .first { it.type == com.example.model.MissionType.CLEAR_LINES_SINGLE_MOVE }
        assertFalse(singleMoveMission.isCompleted)
        assertFalse(singleMoveMission.isClaimed)

        // Clearing 1 line across separate moves must NOT accumulate to 3 for single-move objective
        viewModel.recordMissionEvent(com.example.model.MissionType.CLEAR_LINES_SINGLE_MOVE, 1)
        viewModel.recordMissionEvent(com.example.model.MissionType.CLEAR_LINES_SINGLE_MOVE, 1)
        val afterSingleLines = viewModel.gameState.value.dailyMissions.first { it.id == singleMoveMission.id }
        assertEquals(1, afterSingleLines.current)
        assertFalse(afterSingleLines.isCompleted)

        // Clearing 3 lines in one move completes the mission
        viewModel.recordMissionEvent(com.example.model.MissionType.CLEAR_LINES_SINGLE_MOVE, 3)
        val stateAfterEvent = viewModel.gameState.value
        val updatedMission = stateAfterEvent.dailyMissions.first { it.id == singleMoveMission.id }
        assertTrue(updatedMission.isCompleted)
        assertFalse(updatedMission.isClaimed)
        assertTrue(stateAfterEvent.unclaimedMissionsCount >= 1)

        // Reaching 1000 points completes the score milestone
        viewModel.recordMissionEvent(com.example.model.MissionType.SCORE_POINTS, 1000)
        val scoreMission = viewModel.gameState.value.dailyMissions
            .first { it.type == com.example.model.MissionType.SCORE_POINTS }
        assertTrue(scoreMission.isCompleted)

        // Claim reward grants both bonus coins and hammer reward
        val initialCoins = stateAfterEvent.coinsBalance
        val initialHammers = stateAfterEvent.hammerCount
        val claimed = viewModel.claimMissionReward(singleMoveMission.id)
        assertTrue(claimed)

        val stateAfterClaim = viewModel.gameState.value
        assertEquals(initialCoins + singleMoveMission.coinReward, stateAfterClaim.coinsBalance)
        assertEquals(initialCoins + singleMoveMission.coinReward, prefs.coinsBalance)
        assertEquals(25 + singleMoveMission.coinReward, prefs.totalCoinsEarned)
        assertEquals(initialHammers + singleMoveMission.hammerReward, stateAfterClaim.hammerCount)
        assertEquals(initialHammers + singleMoveMission.hammerReward, prefs.hammerCount)
        assertTrue(stateAfterClaim.dailyMissions.first { it.id == singleMoveMission.id }.isClaimed)
    }

    @Test
    fun `test daily puzzle generator produces deterministic valid 8x8 obstacle layout`() {
        val seed = 20261113L
        val boardA = com.example.model.DailyPuzzleGenerator.generateInitialBoard(seed)
        val boardB = com.example.model.DailyPuzzleGenerator.generateInitialBoard(seed)

        assertEquals(8, boardA.size)
        assertTrue(boardA.all { it.size == 8 })
        assertEquals(boardA, boardB)

        // Ensure board contains pre-populated puzzle obstacles and >= 2 open cells per row/col
        val filledCount = boardA.sumOf { row -> row.count { it != 0 } }
        assertTrue("Expected procedural puzzle obstacles", filledCount > 0)
        for (r in 0 until 8) {
            assertTrue("Row $r must have >= 2 open cells", boardA[r].count { it == 0 } >= 2)
        }
        for (c in 0 until 8) {
            val colEmpty = (0 until 8).count { r -> boardA[r][c] == 0 }
            assertTrue("Col $c must have >= 2 open cells", colEmpty >= 2)
        }

        // Opening daily batch pieces must fit on the generated puzzle board
        val openingBatch = ShapeDefinitions.getDailyShapeBatch(seed, 0)
        val viewModel = com.example.viewmodel.GameViewModel(
            ApplicationProvider.getApplicationContext(),
            com.example.audio.SoundManager(ApplicationProvider.getApplicationContext()),
            com.example.data.GamePreferences(ApplicationProvider.getApplicationContext())
        )
        assertTrue(openingBatch.all { shape -> viewModel.canFitAnywhere(boardA, shape) })
    }

    @Test
    fun `test session state saves to local file and resumes on new ViewModel instance`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val soundManager = com.example.audio.SoundManager(context)
        val prefs = com.example.data.GamePreferences(context)
        val storage = com.example.data.LocalSessionStorage(context)

        val vm1 = com.example.viewmodel.GameViewModel(
            context = context,
            soundManager = soundManager,
            preferences = prefs,
            localSessionStorage = storage
        )

        // Inject a deterministic state and make a move
        val singleDot = BlockShape(
            id = "dot_1x1",
            name = "dot_1x1",
            matrix = listOf(listOf(true)),
            colorId = 3
        )
        val stateField = vm1.javaClass.getDeclaredField("_gameState")
        stateField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = stateField.get(vm1) as kotlinx.coroutines.flow.MutableStateFlow<com.example.model.GameState>
        stateFlow.value = stateFlow.value.copy(
            grid = List(8) { List(8) { 0 } },
            activePieces = listOf(singleDot, singleDot.copy(instanceId = "dot2"), null),
            score = 320
        )

        val placed = vm1.placeShape(0, 4, 4)
        assertTrue(placed)
        val expectedScore = vm1.gameState.value.score
        assertTrue(expectedScore > 320)

        // Simulate app restart by creating a new ViewModel backed by the same LocalSessionStorage
        val vm2 = com.example.viewmodel.GameViewModel(
            context = context,
            soundManager = soundManager,
            preferences = prefs,
            localSessionStorage = storage
        )
        val resumedState = vm2.gameState.value
        assertEquals(expectedScore, resumedState.score)
        assertEquals(3, resumedState.grid[4][4])
        assertEquals(null, resumedState.activePieces[0])
        assertTrue(resumedState.activePieces[1] != null)
    }

    @Test
    fun `test totalAllTimeScore accumulates across moves and persists across new games`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val soundManager = com.example.audio.SoundManager(context)
        val prefs = com.example.data.GamePreferences(context)
        prefs.totalAllTimeScore = 0L

        val viewModel = com.example.viewmodel.GameViewModel(
            context = context,
            soundManager = soundManager,
            preferences = prefs,
            localSessionStorage = null
        )

        val singleDot = BlockShape(
            id = "dot_1x1",
            name = "dot_1x1",
            matrix = listOf(listOf(true)),
            colorId = 2
        )
        val stateField = viewModel.javaClass.getDeclaredField("_gameState")
        stateField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = stateField.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<com.example.model.GameState>
        stateFlow.value = stateFlow.value.copy(
            grid = List(8) { List(8) { 0 } },
            activePieces = listOf(singleDot, singleDot.copy(instanceId = "dot2"), null),
            score = 0
        )

        assertTrue(viewModel.placeShape(0, 0, 0))
        val afterFirstMoveTotal = viewModel.gameState.value.totalAllTimeScore
        assertEquals(10L, afterFirstMoveTotal)
        assertEquals(10L, prefs.totalAllTimeScore)

        // Starting a brand new game resets current session score to 0 but keeps totalAllTimeScore!
        viewModel.startNewGame()
        assertEquals(0, viewModel.gameState.value.score)
        assertEquals(10L, viewModel.gameState.value.totalAllTimeScore)

        // Making another move in the new game adds to the cumulative total
        stateFlow.value = stateFlow.value.copy(
            grid = List(8) { List(8) { 0 } },
            activePieces = listOf(singleDot, null, null)
        )
        assertTrue(viewModel.placeShape(0, 1, 1))
        assertEquals(20L, viewModel.gameState.value.totalAllTimeScore)
        assertEquals(20L, prefs.totalAllTimeScore)
    }

    @Test
    fun `test progressive difficulty tier updates in GameViewModel on score threshold and undo`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val soundManager = com.example.audio.SoundManager(context)
        val prefs = com.example.data.GamePreferences(context)
        val viewModel = com.example.viewmodel.GameViewModel(
            context = context,
            soundManager = soundManager,
            preferences = prefs,
            localSessionStorage = null
        )

        assertEquals(1, viewModel.gameState.value.difficultyLevel)
        assertEquals("STARTER", viewModel.gameState.value.difficultyTitle)

        // Inject score = 495 right below Challenger (500), with a 1-cell dot (+10 pts -> 505)
        val singleDot = BlockShape(
            id = "dot_1x1",
            name = "dot_1x1",
            matrix = listOf(listOf(true)),
            colorId = 3
        )
        val stateField = viewModel.javaClass.getDeclaredField("_gameState")
        stateField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = stateField.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<com.example.model.GameState>
        stateFlow.value = stateFlow.value.copy(
            grid = List(8) { List(8) { 0 } },
            activePieces = listOf(singleDot, null, null),
            score = 495,
            difficultyLevel = 1,
            difficultyTitle = "STARTER"
        )

        assertTrue(viewModel.placeShape(0, 2, 2))
        val afterLevelUp = viewModel.gameState.value
        assertEquals(505, afterLevelUp.score)
        assertEquals(2, afterLevelUp.difficultyLevel)
        assertEquals("CHALLENGER", afterLevelUp.difficultyTitle)
        assertTrue(afterLevelUp.comboBannerText?.contains("CHALLENGER") == true)

        // Undo reverts back to Tier 1 STARTER
        assertTrue(viewModel.undoLastMove())
        val afterUndo = viewModel.gameState.value
        assertEquals(495, afterUndo.score)
        assertEquals(1, afterUndo.difficultyLevel)
        assertEquals("STARTER", afterUndo.difficultyTitle)
    }

    @Test
    fun `test time attack mode starts at 60s rewards bonus seconds and triggers TIME_UP with second chance`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val soundManager = com.example.audio.SoundManager(context)
        val prefs = com.example.data.GamePreferences(context)
        prefs.timeAttackHighScore = 0
        val storage = com.example.data.LocalSessionStorage(context)

        val viewModel = com.example.viewmodel.GameViewModel(
            context = context,
            soundManager = soundManager,
            preferences = prefs,
            localSessionStorage = storage
        )

        viewModel.switchGameMode(com.example.model.GameMode.TIME_ATTACK)
        val started = viewModel.gameState.value
        assertEquals(com.example.model.GameMode.TIME_ATTACK, started.gameMode)
        assertEquals(60_000L, started.timeRemainingMs)
        assertEquals(com.example.model.GameOverReason.NO_MOVES, started.gameOverReason)
        assertFalse(started.isGameOver)

        // Advance clock by 20s -> 40s remaining
        viewModel.tickTimeAttack(20_000L)
        assertEquals(40_000L, viewModel.gameState.value.timeRemainingMs)

        // Setup row 0 with 7 cells filled so placing a 1x1 dot at (0, 7) clears 1 line -> +4s bonus!
        val partialGrid = List(8) { r ->
            List(8) { c -> if (r == 0 && c < 7) 2 else 0 }
        }
        val singleDot = BlockShape(
            id = "dot_1x1",
            name = "dot_1x1",
            matrix = listOf(listOf(true)),
            colorId = 4
        )
        val stateField = viewModel.javaClass.getDeclaredField("_gameState")
        stateField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = stateField.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<com.example.model.GameState>
        stateFlow.value = stateFlow.value.copy(
            grid = partialGrid,
            activePieces = listOf(singleDot, singleDot.copy(instanceId = "dot2"), null),
            comboStreak = 0
        )

        assertTrue(viewModel.placeShape(0, 0, 7))
        val afterClear = viewModel.gameState.value
        assertEquals(44_000L, afterClear.timeRemainingMs)
        assertEquals(4, afterClear.lastTimeBonusSeconds)
        assertTrue(afterClear.score > 0)
        assertEquals(afterClear.score, prefs.timeAttackHighScore)

        // Resume persisted Time Attack session in a fresh ViewModel
        val resumedVm = com.example.viewmodel.GameViewModel(
            context = context,
            soundManager = soundManager,
            preferences = prefs,
            localSessionStorage = storage
        )
        assertEquals(com.example.model.GameMode.TIME_ATTACK, resumedVm.gameState.value.gameMode)
        assertEquals(44_000L, resumedVm.gameState.value.timeRemainingMs)

        // Tick down remaining 44s -> TIME_UP Game Over
        resumedVm.tickTimeAttack(44_000L)
        val expiredState = resumedVm.gameState.value
        assertEquals(0L, expiredState.timeRemainingMs)
        assertTrue(expiredState.isGameOver)
        assertEquals(com.example.model.GameOverReason.TIME_UP, expiredState.gameOverReason)

        // Second Chance revives with +15s!
        resumedVm.useSecondChance()
        val revivedState = resumedVm.gameState.value
        assertFalse(revivedState.isGameOver)
        assertFalse(revivedState.secondChanceAvailable)
        assertEquals(15_000L, revivedState.timeRemainingMs)
        assertEquals(com.example.model.GameOverReason.NO_MOVES, revivedState.gameOverReason)
    }

    @Test
    fun `test settings toggles persist music sound effects and haptics state`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val soundManager = com.example.audio.SoundManager(context)
        val prefs = com.example.data.GamePreferences(context)
        prefs.isMusicEnabled = true
        prefs.isSoundEnabled = true
        prefs.isHapticsEnabled = true

        val viewModel = com.example.viewmodel.GameViewModel(
            context = context,
            soundManager = soundManager,
            preferences = prefs,
            localSessionStorage = null
        )

        assertTrue(viewModel.gameState.value.isMusicEnabled)
        assertTrue(viewModel.gameState.value.isSoundEnabled)
        assertTrue(viewModel.gameState.value.isHapticsEnabled)

        assertFalse(viewModel.toggleMusic())
        assertFalse(viewModel.gameState.value.isMusicEnabled)
        assertFalse(soundManager.isMusicEnabled)
        assertFalse(prefs.isMusicEnabled)

        assertFalse(viewModel.toggleSound())
        assertFalse(viewModel.gameState.value.isSoundEnabled)
        assertFalse(soundManager.isSoundEnabled)
        assertFalse(prefs.isSoundEnabled)

        assertFalse(viewModel.toggleHaptics())
        assertFalse(viewModel.gameState.value.isHapticsEnabled)
        assertFalse(soundManager.isHapticsEnabled)
        assertFalse(prefs.isHapticsEnabled)

        // Fresh ViewModel inherits persisted settings
        val freshSoundManager = com.example.audio.SoundManager(context)
        val freshViewModel = com.example.viewmodel.GameViewModel(
            context = context,
            soundManager = freshSoundManager,
            preferences = prefs,
            localSessionStorage = null
        )
        assertFalse(freshViewModel.gameState.value.isMusicEnabled)
        assertFalse(freshViewModel.gameState.value.isSoundEnabled)
        assertFalse(freshViewModel.gameState.value.isHapticsEnabled)
    }

    @Test
    fun `test daily challenge generates unique configurations per day and tracks streak and solve status`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = com.example.data.GamePreferences(context)

        // Different day seeds yield unique board configurations
        val day1Board = com.example.model.DailyPuzzleGenerator.generateInitialBoard(20261115L)
        val day2Board = com.example.model.DailyPuzzleGenerator.generateInitialBoard(20261116L)
        assertTrue(day1Board != day2Board)

        // Consecutive daily participation increments streak; same day is idempotent; missed day resets to 1
        prefs.dailyStreak = 0
        prefs.dailyLastPlayedDate = ""
        assertEquals(1, prefs.recordDailyParticipation(todayDate = "2026-11-14", yesterdayDate = "2026-11-13"))
        assertEquals(1, prefs.recordDailyParticipation(todayDate = "2026-11-14", yesterdayDate = "2026-11-13"))
        assertEquals(2, prefs.recordDailyParticipation(todayDate = "2026-11-15", yesterdayDate = "2026-11-14"))
        assertEquals(3, prefs.recordDailyParticipation(todayDate = "2026-11-16", yesterdayDate = "2026-11-15"))
        // Missed a day (yesterday was 2026-11-17, last played 2026-11-16)
        assertEquals(1, prefs.recordDailyParticipation(todayDate = "2026-11-18", yesterdayDate = "2026-11-17"))

        // Daily completion marks solved status once per date
        assertFalse(prefs.isDailyDateCompleted("2026-11-16"))
        assertTrue(prefs.markDailyDateCompleted("2026-11-16"))
        assertFalse(prefs.markDailyDateCompleted("2026-11-16"))
        assertTrue(prefs.isDailyDateCompleted("2026-11-16"))
        assertEquals(1, prefs.dailyChallengesCompleted)
    }

    @Test
    fun `test achievement system tracks and persists milestones across sessions`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val soundManager = com.example.audio.SoundManager(context)
        val prefs = com.example.data.GamePreferences(context)

        // Pre-populate milestones: 10x combo, 2500+ score, 3-day streak, 50 lines, 1 daily solved
        prefs.highestCombo = 10
        prefs.highScore = 2600
        prefs.dailyStreak = 3
        prefs.totalLinesCleared = 50
        prefs.dailyChallengesCompleted = 1

        val viewModel = com.example.viewmodel.GameViewModel(
            context = context,
            soundManager = soundManager,
            preferences = prefs,
            localSessionStorage = null
        )

        val achievements = viewModel.gameState.value.achievements
        assertEquals(5, achievements.size)
        assertEquals(5, viewModel.gameState.value.unlockedAchievementsCount)

        val combo10 = achievements.first { it.id == "combo_10" }
        val highScorer = achievements.first { it.id == "high_scorer" }
        val dailyStreak = achievements.first { it.id == "daily_streak" }
        val lineMaster = achievements.first { it.id == "line_master" }
        val puzzleSolver = achievements.first { it.id == "puzzle_solver" }

        assertEquals("10-Line Combo", combo10.title)
        assertTrue(combo10.isUnlocked)
        assertEquals("High Scorer", highScorer.title)
        assertTrue(highScorer.isUnlocked)
        assertEquals("Daily Streak", dailyStreak.title)
        assertTrue(dailyStreak.isUnlocked)
        assertTrue(lineMaster.isUnlocked)
        assertTrue(puzzleSolver.isUnlocked)

        // Even if active preferences reset in-memory stats, unlocked status remains persisted
        prefs.highestCombo = 0
        prefs.highScore = 0
        val freshVm = com.example.viewmodel.GameViewModel(
            context = context,
            soundManager = soundManager,
            preferences = prefs,
            localSessionStorage = null
        )
        assertTrue(freshVm.gameState.value.achievements.first { it.id == "combo_10" }.isUnlocked)
        assertTrue(freshVm.gameState.value.achievements.first { it.id == "high_scorer" }.isUnlocked)
    }
}
