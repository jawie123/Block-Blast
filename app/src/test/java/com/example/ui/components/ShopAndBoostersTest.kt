package com.example.ui.components

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.audio.SoundManager
import com.example.data.GamePreferences
import com.example.model.BlockShape
import com.example.model.BoardThemeId
import com.example.model.GameState
import com.example.model.ShopBoosterType
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
class ShopAndBoostersTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `piece shuffle rerolls active tray pieces decrements shuffleCount and supports undo`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = GamePreferences(context).apply {
            hasSeenTutorial = true
            shuffleCount = 2
        }
        val viewModel = GameViewModel(
            context = context,
            soundManager = SoundManager(context),
            preferences = prefs,
            localSessionStorage = null
        )

        val beforeIds = viewModel.gameState.value.activePieces.map { it?.instanceId }
        assertEquals(2, viewModel.gameState.value.shuffleCount)

        val shuffled = viewModel.useShuffle()
        assertTrue(shuffled)

        val afterState = viewModel.gameState.value
        assertEquals(1, afterState.shuffleCount)
        assertEquals(1, prefs.shuffleCount)
        assertEquals(3, afterState.activePieces.count { it != null })
        assertTrue(afterState.activePieces.map { it?.instanceId } != beforeIds)
        assertTrue(afterState.canUndo)

        // Undo reverts tray and restores shuffle count
        assertTrue(viewModel.undoLastMove())
        val undoneState = viewModel.gameState.value
        assertEquals(2, undoneState.shuffleCount)
        assertEquals(beforeIds, undoneState.activePieces.map { it?.instanceId })
    }

    @Test
    fun `area bomb 3x3 clears surrounding blocks emits bursts decrements bombCount and supports undo`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = GamePreferences(context).apply {
            hasSeenTutorial = true
            bombCount = 1
        }
        val viewModel = GameViewModel(
            context = context,
            soundManager = SoundManager(context),
            preferences = prefs,
            localSessionStorage = null
        )

        // Fill a 3x3 block cluster centered at (4, 4) plus an outside block at (0, 0)
        val populatedGrid = List(8) { r ->
            List(8) { c ->
                when {
                    r in 3..5 && c in 3..5 -> 2
                    r == 0 && c == 0 -> 5
                    else -> 0
                }
            }
        }
        val stateField = viewModel.javaClass.getDeclaredField("_gameState")
        stateField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = stateField.get(viewModel) as MutableStateFlow<GameState>
        stateFlow.value = stateFlow.value.copy(
            grid = populatedGrid,
            activePieces = listOf(
                BlockShape(id = "dot", name = "Dot", matrix = listOf(listOf(true)), colorId = 1),
                null,
                null
            )
        )

        viewModel.toggleBombMode()
        assertTrue(viewModel.gameState.value.isBombActive)

        val detonated = viewModel.useBombOnCell(4, 4)
        assertTrue(detonated)

        val afterBlast = viewModel.gameState.value
        assertFalse(afterBlast.isBombActive)
        assertEquals(0, afterBlast.bombCount)
        assertEquals(0, prefs.bombCount)
        assertEquals(9, afterBlast.lineClearBursts.size)
        for (r in 3..5) {
            for (c in 3..5) {
                assertEquals(0, afterBlast.grid[r][c])
            }
        }
        // Outside cell remains intact
        assertEquals(5, afterBlast.grid[0][0])

        // Undo restores the 3x3 cluster and bomb count
        assertTrue(viewModel.undoLastMove())
        val afterUndo = viewModel.gameState.value
        assertEquals(1, afterUndo.bombCount)
        assertEquals(2, afterUndo.grid[4][4])
    }

    @Test
    fun `shop dialog unlocks cozy wood theme and purchases boosters with blast coins`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = GamePreferences(context).apply {
            hasSeenTutorial = true
            coinsBalance = 300
            hammerCount = 1
            bombCount = 0
        }
        val viewModel = GameViewModel(
            context = context,
            soundManager = SoundManager(context),
            preferences = prefs,
            localSessionStorage = null
        )

        composeTestRule.setContent {
            GameScreen(viewModel = viewModel)
        }
        composeTestRule.waitForIdle()

        // Verify BoosterBar and non-cramped ScoreHeader pills are displayed
        composeTestRule.onNodeWithTag("hammer_powerup_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("hint_powerup_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("shuffle_powerup_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bomb_powerup_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("shop_open_button").assertIsDisplayed()

        // High score pill should remain wider than tall (single-line label & value, not wrapping letter-by-letter)
        val highScoreBounds = composeTestRule.onNodeWithTag("high_score_pill").getBoundsInRoot()
        val boardBounds = composeTestRule.onNodeWithTag("grid_board_container").getBoundsInRoot()
        assertTrue(
            "Expected high_score_pill width (${highScoreBounds.right - highScoreBounds.left}) > height (${highScoreBounds.bottom - highScoreBounds.top})",
            (highScoreBounds.right - highScoreBounds.left) > (highScoreBounds.bottom - highScoreBounds.top)
        )
        assertTrue(
            "Expected 8x8 board width >= 300.dp on 360dp phone, got ${boardBounds.right - boardBounds.left}",
            (boardBounds.right - boardBounds.left).value >= 300f
        )

        // Open Shop from BoosterBar
        composeTestRule.onNodeWithTag("shop_open_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("shop_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("shop_coins_balance").assertIsDisplayed()

        // Unlock & equip Cozy Wood theme (120 coins)
        composeTestRule.onNodeWithTag("theme_action_button_${BoardThemeId.COZY_WOOD.name}")
            .assertIsEnabled()
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(BoardThemeId.COZY_WOOD.name, viewModel.gameState.value.selectedThemeId)
        assertEquals(180, viewModel.gameState.value.coinsBalance)
        assertTrue(prefs.isThemeUnlocked(BoardThemeId.COZY_WOOD.name))
        composeTestRule.onNodeWithText("ACTIVE").assertIsDisplayed()

        // Switch to Boosters tab and purchase a 3x3 Area Bomb (75 coins)
        composeTestRule.onNodeWithTag("shop_tab_boosters").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("shop_buy_booster_${ShopBoosterType.BOMB.name}")
            .assertIsEnabled()
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, viewModel.gameState.value.bombCount)
        assertEquals(105, viewModel.gameState.value.coinsBalance)
    }
}
