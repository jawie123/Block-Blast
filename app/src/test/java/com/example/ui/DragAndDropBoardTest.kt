package com.example.ui

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import com.example.audio.SoundManager
import com.example.data.GamePreferences
import com.example.model.BlockShape
import com.example.model.GameState
import com.example.viewmodel.GameViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DragAndDropBoardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `dragging piece from deck shows floating overlay ghost preview and drops directly onto board`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = GamePreferences(context).apply { hasSeenTutorial = true }
        val soundManager = SoundManager(context)
        val viewModel = GameViewModel(
            context = context,
            soundManager = soundManager,
            preferences = prefs,
            localSessionStorage = null
        )

        // Ensure deterministic 1x1 dot in slot 0 so any board cell is valid
        val dot = BlockShape(
            id = "dot_1x1",
            name = "dot_1x1",
            matrix = listOf(listOf(true)),
            colorId = 3
        )
        val stateField = viewModel.javaClass.getDeclaredField("_gameState")
        stateField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = stateField.get(viewModel) as MutableStateFlow<GameState>
        stateFlow.value = stateFlow.value.copy(
            grid = List(8) { List(8) { 0 } },
            activePieces = listOf(dot, dot.copy(instanceId = "dot_2"), dot.copy(instanceId = "dot_3")),
            score = 0
        )

        composeTestRule.setContent {
            GameScreen(viewModel = viewModel)
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("piece_slot_0").assertIsDisplayed()
        composeTestRule.onNodeWithTag("grid_board_container").assertIsDisplayed()

        // Measure board center and slot 0 center in root coordinates
        val boardBounds = composeTestRule.onNodeWithTag("grid_board_container").fetchSemanticsNode().boundsInRoot
        val slotBounds = composeTestRule.onNodeWithTag("piece_slot_0").fetchSemanticsNode().boundsInRoot

        val density = context.resources.displayMetrics.density
        val boardCellPx = boardBounds.width / 8f
        val expectedLiftPx = (boardCellPx * 1.85f).coerceIn(48f * density, 84f * density)

        val startCenter = Offset(slotBounds.width / 2f, slotBounds.height / 2f)
        // Drag so lifted piece lands cleanly at the center of the board
        val targetTouchRootY = boardBounds.center.y + expectedLiftPx
        val deltaX = boardBounds.center.x - slotBounds.center.x
        val deltaY = targetTouchRootY - slotBounds.center.y

        composeTestRule.onNodeWithTag("piece_slot_0").performTouchInput {
            down(startCenter)
            // Initial move to trigger pickup
            moveBy(Offset(0f, -20f))
        }
        composeTestRule.waitForIdle()

        // Floating overlay should now be visible
        composeTestRule.onNodeWithTag("dragging_piece_overlay").assertIsDisplayed()

        // Drag onto the board area
        composeTestRule.onNodeWithTag("piece_slot_0").performTouchInput {
            moveBy(Offset(deltaX, deltaY + 20f))
        }
        composeTestRule.waitForIdle()

        // Ghost preview cells must be visible on the board while hovering
        val ghostCount = composeTestRule.onAllNodesWithTag("block_cell_ghost").fetchSemanticsNodes().size
        assertTrue("Expected ghost preview cell on board during drag", ghostCount >= 1)

        // Release finger to drop directly onto the board
        composeTestRule.onNodeWithTag("piece_slot_0").performTouchInput {
            up()
        }
        composeTestRule.waitForIdle()

        // Overlay disappears, slot 0 is consumed, and board & score updated!
        composeTestRule.onNodeWithTag("dragging_piece_overlay").assertDoesNotExist()
        val updated = viewModel.gameState.value
        assertNull(updated.activePieces[0])
        assertEquals(10, updated.score)
        assertTrue(updated.grid.flatten().count { it == 3 } == 1)
    }

    @Test
    fun `tapping piece slot selects piece and tapping board cell places it`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = GamePreferences(context).apply { hasSeenTutorial = true }
        val soundManager = SoundManager(context)
        val viewModel = GameViewModel(
            context = context,
            soundManager = soundManager,
            preferences = prefs,
            localSessionStorage = null
        )

        val dot = BlockShape(
            id = "dot_1x1",
            name = "dot_1x1",
            matrix = listOf(listOf(true)),
            colorId = 4
        )
        val stateField = viewModel.javaClass.getDeclaredField("_gameState")
        stateField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = stateField.get(viewModel) as MutableStateFlow<GameState>
        stateFlow.value = stateFlow.value.copy(
            grid = List(8) { List(8) { 0 } },
            activePieces = listOf(dot, dot.copy(instanceId = "dot_2"), dot.copy(instanceId = "dot_3")),
            score = 0
        )

        composeTestRule.setContent {
            GameScreen(viewModel = viewModel)
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("piece_slot_1").performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, viewModel.gameState.value.selectedShapeIndex)

        composeTestRule.onNodeWithTag("grid_cell_2_3").performClick()
        composeTestRule.waitForIdle()

        val updated = viewModel.gameState.value
        assertNull(updated.activePieces[1])
        assertNull(updated.selectedShapeIndex)
        assertEquals(10, updated.score)
        assertEquals(4, updated.grid[2][3])
    }
}
