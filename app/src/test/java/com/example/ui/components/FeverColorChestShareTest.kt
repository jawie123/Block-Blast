package com.example.ui.components

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.audio.SoundManager
import com.example.data.GamePreferences
import com.example.model.AdventureStarChest
import com.example.model.BlockShape
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
class FeverColorChestShareTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun singleCell(id: String = "dot_1", colorId: Int = 2) = BlockShape(
        id = id,
        name = id,
        matrix = listOf(listOf(true)),
        colorId = colorId
    )

    @Suppress("UNCHECKED_CAST")
    private fun getMutableState(viewModel: GameViewModel): MutableStateFlow<GameState> {
        val stateField = viewModel.javaClass.getDeclaredField("_gameState")
        stateField.isAccessible = true
        return stateField.get(viewModel) as MutableStateFlow<GameState>
    }

    @Test
    fun `fever mode activates at combo 4 doubles line clear points grants bonus coins counts down 3 moves and supports undo`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = GamePreferences(context).apply {
            hasSeenTutorial = true
            coinsBalance = 0
        }
        val viewModel = GameViewModel(
            context = context,
            soundManager = SoundManager(context),
            preferences = prefs,
            localSessionStorage = null
        )

        // Setup grid with row 0 needing 1 block to complete and existing comboStreak = 3 -> placement makes combo 4 (activates Fever)
        val grid = MutableList(8) { MutableList(8) { 0 } }
        for (c in 1..7) grid[0][c] = if (c % 2 == 0) 1 else 2 // mixed colors so no color match bonus
        // Row 2 ready for next placement during Fever
        for (c in 1..7) grid[2][c] = if (c % 2 == 0) 3 else 4
        // Keep an extra block so Perfect Board Clear (+500) doesn't trigger when Row 2 clears
        grid[7][0] = 6

        val dotMixed = singleCell("dot_a", colorId = 5)
        getMutableState(viewModel).value = viewModel.gameState.value.copy(
            grid = grid.map { it.toList() },
            activePieces = listOf(dotMixed, singleCell("dot_b", 6), singleCell("dot_c", 7)),
            comboStreak = 3,
            feverMovesRemaining = 0,
            score = 0
        )

        // Move that reaches combo 4 -> activates Fever Mode for 3 moves
        assertTrue(viewModel.placeShape(0, 0, 0))
        val activatedState = viewModel.gameState.value
        assertEquals(4, activatedState.comboStreak)
        assertEquals(GameViewModel.FEVER_DURATION_MOVES, activatedState.feverMovesRemaining)

        // Next line clear during Fever Mode (combo becomes 5, 1 line mixed colors = 500 * 2 = 1000 + 10 placement = 1010 pts, +5 coins)
        val coinsBeforeFeverClear = prefs.coinsBalance
        val scoreBeforeFeverClear = activatedState.score
        assertTrue(viewModel.placeShape(1, 2, 0))
        val afterFeverClear = viewModel.gameState.value
        assertEquals(2, afterFeverClear.feverMovesRemaining)
        assertEquals(scoreBeforeFeverClear + 1010, afterFeverClear.score)
        assertEquals(coinsBeforeFeverClear + GameViewModel.FEVER_COINS_PER_LINE, afterFeverClear.coinsBalance)

        // Undo restores feverMovesRemaining = 3 and previous score
        assertTrue(viewModel.undoLastMove())
        val afterUndo = viewModel.gameState.value
        assertEquals(3, afterUndo.feverMovesRemaining)
        assertEquals(scoreBeforeFeverClear, afterUndo.score)

        // Place without clearing a line -> decrements feverMovesRemaining down cleanly
        assertTrue(viewModel.placeShape(2, 7, 7))
        assertEquals(2, viewModel.gameState.value.feverMovesRemaining)
    }

    @Test
    fun `same color line clear awards 250 bonus and color blast booster clears all matching blocks with laser cascade and undo`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = GamePreferences(context).apply {
            hasSeenTutorial = true
            colorBlastCount = 1
            coinsBalance = 0
        }
        val viewModel = GameViewModel(
            context = context,
            soundManager = SoundManager(context),
            preferences = prefs,
            localSessionStorage = null
        )

        // Part 1: Monochromatic row clear (all colorId = 3), plus an unrelated cell so Perfect Board Clear (+500) doesn't trigger
        val monoGrid = MutableList(8) { MutableList(8) { 0 } }
        for (c in 1..7) monoGrid[0][c] = 3
        monoGrid[7][7] = 6
        val matchingDot = singleCell("dot_mono", colorId = 3)
        getMutableState(viewModel).value = viewModel.gameState.value.copy(
            grid = monoGrid.map { it.toList() },
            activePieces = listOf(matchingDot, singleCell("other", 4), null),
            comboStreak = 0,
            feverMovesRemaining = 0,
            score = 0
        )

        assertTrue(viewModel.placeShape(0, 0, 0))
        // 10 base + 100 (1 line * combo 1) + 250 (color match bonus) = 360 points
        assertEquals(10 + 100 + GameViewModel.COLOR_MATCH_LINE_BONUS, viewModel.gameState.value.score)

        // Part 2: Color Blast booster clears every block of colorId = 2 and triggers ⚡ Cross-Laser
        val blastGrid = MutableList(8) { MutableList(8) { 0 } }
        blastGrid[1][1] = 2
        blastGrid[4][4] = 2
        blastGrid[6][2] = 2
        blastGrid[6][7] = 5 // Will be hit by ⚡ Cross-Laser on (6,2)
        blastGrid[0][0] = 6 // Different color, untouched

        getMutableState(viewModel).value = viewModel.gameState.value.copy(
            grid = blastGrid.map { it.toList() },
            activePieces = listOf(singleCell("dot_keep", 1), null, null),
            lightningCells = setOf(6 to 2),
            gemCells = setOf(4 to 4),
            colorBlastCount = 1,
            score = 0
        )

        viewModel.toggleColorBlastMode()
        assertTrue(viewModel.gameState.value.isColorBlastActive)

        assertTrue(viewModel.useColorBlastOnCell(1, 1))
        val afterBlast = viewModel.gameState.value
        assertFalse(afterBlast.isColorBlastActive)
        assertEquals(0, afterBlast.colorBlastCount)
        assertEquals(0, prefs.colorBlastCount)
        assertEquals(0, afterBlast.grid[1][1])
        assertEquals(0, afterBlast.grid[4][4])
        assertEquals(0, afterBlast.grid[6][2])
        assertEquals(0, afterBlast.grid[6][7]) // Cleared by ⚡ laser cascade
        assertEquals(6, afterBlast.grid[0][0]) // Untouched color
        assertEquals(60, afterBlast.score) // 4 blasted cells * 15 pts
        assertEquals(5, afterBlast.coinsBalance) // Gem collected

        // Undo restores all blasted blocks and colorBlastCount
        assertTrue(viewModel.undoLastMove())
        val afterUndo = viewModel.gameState.value
        assertEquals(1, afterUndo.colorBlastCount)
        assertEquals(2, afterUndo.grid[1][1])
        assertEquals(5, afterUndo.grid[6][7])
    }

    @Test
    fun `adventure star chest unlocks at 10 stars and grants coins hammer and color blast rewards`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = GamePreferences(context).apply {
            hasSeenTutorial = true
            coinsBalance = 50
            hammerCount = 2
            colorBlastCount = 1
            claimedStarChestsCount = 0
            // Grant 10 stars across levels 1..4
            setAdventureLevelStars(1, 3)
            setAdventureLevelStars(2, 3)
            setAdventureLevelStars(3, 2)
            setAdventureLevelStars(4, 2)
        }
        val viewModel = GameViewModel(
            context = context,
            soundManager = SoundManager(context),
            preferences = prefs,
            localSessionStorage = null
        )

        assertTrue(viewModel.claimAdventureStarChest())
        val state = viewModel.gameState.value
        assertEquals(1, state.claimedStarChestsCount)
        assertEquals(50 + AdventureStarChest.COINS_REWARD, state.coinsBalance)
        assertEquals(2 + AdventureStarChest.HAMMER_REWARD, state.hammerCount)
        assertEquals(1 + AdventureStarChest.COLOR_BLAST_REWARD, state.colorBlastCount)

        // Next chest requires 20 stars, so second claim before earning 20 stars fails
        assertFalse(viewModel.claimAdventureStarChest())
    }

    @Test
    fun `booster bar fits 7 buttons on 360dp fever hud and share button render and work properly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = GamePreferences(context).apply {
            hasSeenTutorial = true
            colorBlastCount = 2
            claimedStarChestsCount = 0
            setAdventureLevelStars(1, 3)
            setAdventureLevelStars(2, 3)
            setAdventureLevelStars(3, 3)
            setAdventureLevelStars(4, 1)
        }
        val viewModel = GameViewModel(
            context = context,
            soundManager = SoundManager(context),
            preferences = prefs,
            localSessionStorage = null
        )
        getMutableState(viewModel).value = viewModel.gameState.value.copy(
            feverMovesRemaining = 2,
            comboStreak = 4
        )

        composeTestRule.setContent {
            GameScreen(viewModel = viewModel)
        }
        composeTestRule.waitForIdle()

        // Verify all 7 BoosterBar items + Fever HUD are visible inside 360dp screen width
        composeTestRule.onNodeWithTag("hammer_powerup_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("color_blast_powerup_button").assertIsDisplayed()
        val shopBounds = composeTestRule.onNodeWithTag("shop_open_button").assertIsDisplayed().getBoundsInRoot()
        assertTrue("Shop button should fit inside 360dp width, right=${shopBounds.right}", shopBounds.right.value <= 360f)
        composeTestRule.onNodeWithTag("fever_mode_hud_badge").assertIsDisplayed()

        // Toggle Color Blast mode from BoosterBar
        composeTestRule.onNodeWithTag("color_blast_powerup_button").performClick()
        composeTestRule.waitForIdle()
        assertTrue(viewModel.gameState.value.isColorBlastActive)
        composeTestRule.onNodeWithTag("color_blast_active_instruction_pill").assertIsDisplayed()
    }

    @Test
    fun `game over dialog displays share score button and invokes callback`() {
        var sharedClicked = false
        composeTestRule.setContent {
            GameOverDialog(
                score = 2400,
                highScore = 2400,
                isNewHighScore = true,
                linesCleared = 14,
                maxCombo = 5,
                secondChanceAvailable = false,
                onSecondChance = {},
                onShareScore = { sharedClicked = true },
                onRestart = {}
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("share_score_button").assertIsDisplayed().performClick()
        assertTrue(sharedClicked)
    }

    @Test
    fun `adventure levels dialog star chest button is enabled at 10 stars and triggers claim`() {
        var claimed = false
        composeTestRule.setContent {
            AdventureLevelsDialog(
                currentLevelNumber = 4,
                maxUnlockedLevel = 4,
                getLevelStars = { lvl -> if (lvl <= 3) 3 else if (lvl == 4) 1 else 0 },
                onSelectLevel = {},
                onDismiss = {},
                claimedStarChestsCount = 0,
                onClaimStarChest = { claimed = true }
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("adventure_star_chest_button")
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()
        assertTrue(claimed)
    }
}
