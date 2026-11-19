package com.example.ui.components

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.audio.SoundManager
import com.example.data.GamePreferences
import com.example.model.AdventureCatalog
import com.example.model.BlockShape
import com.example.model.GameMode
import com.example.model.GameState
import com.example.ui.GameScreen
import com.example.viewmodel.GameViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class AdventureAndRotateTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `piece rotate transforms matrix 90 degrees clockwise decrements count supports undo and skips symmetric shapes`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = GamePreferences(context).apply {
            hasSeenTutorial = true
            rotateCount = 3
        }
        val viewModel = GameViewModel(
            context = context,
            soundManager = SoundManager(context),
            preferences = prefs,
            localSessionStorage = null
        )

        val lShape = BlockShape(
            id = "test_l3",
            name = "Test L",
            matrix = listOf(
                listOf(true, false),
                listOf(true, true)
            ),
            colorId = 2
        )
        val squareShape = BlockShape(
            id = "test_sq2",
            name = "Test Square",
            matrix = listOf(
                listOf(true, true),
                listOf(true, true)
            ),
            colorId = 3
        )

        val stateField = viewModel.javaClass.getDeclaredField("_gameState")
        stateField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = stateField.get(viewModel) as MutableStateFlow<GameState>
        stateFlow.value = stateFlow.value.copy(
            activePieces = listOf(lShape, squareShape, null)
        )

        // Symmetric 2x2 square should not spend a rotate charge
        assertFalse(viewModel.useRotateOnPiece(1))
        assertEquals(3, viewModel.gameState.value.rotateCount)

        // Asymmetric L-shape rotates 90 deg CW: [[true, false], [true, true]] -> [[true, true], [true, false]]
        assertTrue(viewModel.useRotateOnPiece(0))
        val rotated = viewModel.gameState.value.activePieces[0]!!
        assertEquals(
            listOf(
                listOf(true, true),
                listOf(true, false)
            ),
            rotated.matrix
        )
        assertEquals(2, viewModel.gameState.value.rotateCount)
        assertEquals(2, prefs.rotateCount)

        // Undo restores original orientation and rotateCount
        assertTrue(viewModel.undoLastMove())
        assertEquals(3, viewModel.gameState.value.rotateCount)
        assertEquals(lShape.matrix, viewModel.gameState.value.activePieces[0]!!.matrix)
    }

    @Test
    fun `clearing 2-hit ice cracks to 1 then shatters collecting gems and completing adventure stage`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = GamePreferences(context).apply {
            hasSeenTutorial = true
            coinsBalance = 100
            adventureCurrentLevel = 1
            adventureMaxUnlockedLevel = 1
        }
        val viewModel = GameViewModel(
            context = context,
            soundManager = SoundManager(context),
            preferences = prefs,
            localSessionStorage = null
        )

        viewModel.switchGameMode(GameMode.ADVENTURE)
        assertEquals(GameMode.ADVENTURE, viewModel.gameState.value.gameMode)
        assertEquals(1, viewModel.gameState.value.adventureLevelNumber)

        // Set up row 0 nearly full: cols 1..5 regular, col 6 has a Gem, col 7 has 2-hit Ice; leave col 0 empty
        val grid = MutableList(8) { MutableList(8) { 0 } }
        for (c in 1..7) grid[0][c] = 5
        // Also place a stray block at (7, 7) so row 0 clear does NOT trigger Full Board Clear
        grid[7][7] = 1

        val stateField = viewModel.javaClass.getDeclaredField("_gameState")
        stateField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = stateField.get(viewModel) as MutableStateFlow<GameState>
        val dot = BlockShape(id = "dot", name = "Dot", matrix = listOf(listOf(true)), colorId = 2)
        stateFlow.value = stateFlow.value.copy(
            grid = grid.map { it.toList() },
            gemCells = setOf(0 to 6),
            iceCells = mapOf(0 to 7 to 2),
            adventureGemsCollected = 3, // level 1 target is 4 gems -> collecting (0, 6) completes stage!
            adventureIceBroken = 0,
            adventureInitialIceTotal = 1,
            adventureLevelCompleted = false,
            activePieces = listOf(dot, dot, dot)
        )

        // Clear row 0 by placing dot at (0, 0)
        assertTrue(viewModel.placeShape(0, 0, 0))

        val afterFirstClear = viewModel.gameState.value
        // 2-hit Ice cracked down to 1 layer and remains frozen in grid[0][7]
        assertEquals(1, afterFirstClear.iceCells[0 to 7])
        assertTrue(afterFirstClear.grid[0][7] > 0)
        // Gem at (0, 6) was collected -> +5 coins & level 1 completed!
        assertEquals(4, afterFirstClear.adventureGemsCollected)
        assertTrue(afterFirstClear.adventureLevelCompleted)
        assertTrue(afterFirstClear.coinsBalance >= 100 + 5 + AdventureCatalog.level(1).coinReward)
        assertEquals(2, afterFirstClear.adventureMaxUnlockedLevel)
        assertTrue(afterFirstClear.adventureEarnedStars >= 1)

        // Verify hitting 1-layer Ice with Hammer shatters it cleanly
        val afterStateRef = stateField.get(viewModel) as MutableStateFlow<GameState>
        afterStateRef.value = afterStateRef.value.copy(adventureLevelCompleted = false, hammerCount = 1, isHammerActive = true)
        assertTrue(viewModel.useHammerOnCell(0, 7))
        assertFalse((0 to 7) in viewModel.gameState.value.iceCells)
        assertEquals(0, viewModel.gameState.value.grid[0][7])
    }

    @Test
    fun `perfect board clear grants 500 points and 25 bonus coins plus screen shake`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = GamePreferences(context).apply {
            hasSeenTutorial = true
            coinsBalance = 50
        }
        val viewModel = GameViewModel(
            context = context,
            soundManager = SoundManager(context),
            preferences = prefs,
            localSessionStorage = null
        )

        val grid = MutableList(8) { MutableList(8) { 0 } }
        for (c in 1..7) grid[0][c] = 4

        val stateField = viewModel.javaClass.getDeclaredField("_gameState")
        stateField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = stateField.get(viewModel) as MutableStateFlow<GameState>
        val dot = BlockShape(id = "dot", name = "Dot", matrix = listOf(listOf(true)), colorId = 4)
        stateFlow.value = stateFlow.value.copy(
            grid = grid.map { it.toList() },
            activePieces = listOf(dot, dot, dot)
        )

        assertTrue(viewModel.placeShape(0, 0, 0))
        val afterClear = viewModel.gameState.value
        assertEquals(0, afterClear.grid.sumOf { row -> row.count { it > 0 } })
        assertTrue("Expected score >= 500 on Perfect Clear, was ${afterClear.score}", afterClear.score >= 500)
        assertEquals(75, afterClear.coinsBalance)
        assertTrue(afterClear.boardClearEventId > 0L)
        assertTrue(afterClear.shakeEventId > 0L)
    }

    @Test
    fun `ui switches to adventure mode opens level map and rotates tray piece`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = GamePreferences(context).apply {
            hasSeenTutorial = true
            rotateCount = 3
        }
        val viewModel = GameViewModel(
            context = context,
            soundManager = SoundManager(context),
            preferences = prefs,
            localSessionStorage = null
        )

        val lShape = BlockShape(
            id = "ui_l3",
            name = "UI L",
            matrix = listOf(
                listOf(true, false),
                listOf(true, true)
            ),
            colorId = 2
        )
        val stateField = viewModel.javaClass.getDeclaredField("_gameState")
        stateField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = stateField.get(viewModel) as MutableStateFlow<GameState>

        composeTestRule.setContent {
            GameScreen(viewModel = viewModel)
        }
        composeTestRule.waitForIdle()

        // Switch to Adventure mode via tab
        composeTestRule.onNodeWithTag("mode_tab_adventure").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("adventure_level_banner").assertIsDisplayed()
        composeTestRule.onNodeWithTag("adventure_map_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("rotate_powerup_button").assertIsDisplayed()

        // Open Adventure Map modal
        composeTestRule.onNodeWithTag("adventure_map_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("adventure_levels_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("adventure_level_card_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dismiss_adventure_map_button").performClick()
        composeTestRule.waitForIdle()

        // Inject asymmetric L piece into slot 0 and activate Rotate via BoosterBar
        composeTestRule.runOnUiThread {
            stateFlow.value = stateFlow.value.copy(
                activePieces = listOf(lShape, null, null),
                selectedShapeIndex = null,
                isRotateActive = false
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("rotate_powerup_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("rotate_active_instruction_pill").assertIsDisplayed()

        // Tap slot 0 to rotate it
        composeTestRule.onNodeWithTag("piece_slot_0").performClick()
        composeTestRule.waitForIdle()

        assertEquals(2, viewModel.gameState.value.rotateCount)
        assertEquals(
            listOf(listOf(true, true), listOf(true, false)),
            viewModel.gameState.value.activePieces[0]!!.matrix
        )
    }
}
