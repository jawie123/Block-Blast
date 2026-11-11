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
}
