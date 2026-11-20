package com.example.ui.components

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.audio.SoundManager
import com.example.data.GamePreferences
import com.example.model.AdventureCatalog
import com.example.model.AdventureGoalType
import com.example.model.AdventureStarChest
import com.example.model.BlockShape
import com.example.model.GameMode
import com.example.model.GameState
import com.example.model.PlayerAvatarCatalog
import com.example.model.RankTier
import com.example.model.RankTierCatalog
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
class V14FeaturesTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `adventure chapter 5 has 30 stages and chained tiles break without removing block while mystery crate rewards coins and undo restores`() {
        assertEquals(30, AdventureCatalog.totalLevels)
        assertEquals(9, AdventureStarChest.MAX_CHESTS)
        val level21 = AdventureCatalog.level(21)
        assertEquals(5, level21.chapter)
        assertEquals(AdventureGoalType.BREAK_CHAINS, level21.goalType)
        assertTrue(level21.initialLockedCells.isNotEmpty())

        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = GamePreferences(context).apply {
            hasSeenTutorial = true
            coinsBalance = 100
            hammerCount = 1
            rotateCount = 1
            colorBlastCount = 1
            adventureMaxUnlockedLevel = 30
            adventureCurrentLevel = 21
        }
        val viewModel = GameViewModel(
            context = context,
            soundManager = SoundManager(context),
            preferences = prefs,
            localSessionStorage = null
        )

        viewModel.switchGameMode(GameMode.ADVENTURE)
        viewModel.startAdventureLevel(21)
        assertEquals(21, viewModel.gameState.value.adventureLevelNumber)
        assertTrue(viewModel.gameState.value.lockedCells.isNotEmpty())

        // Prepare row 0 with cols 1..7 filled (col 6 chained 'L', col 7 mystery crate 'M') + 1x1 piece at (0,0)
        // Leave (1,1) filled so perfectBoardClear is not triggered
        val grid = MutableList(8) { MutableList(8) { 0 } }
        for (c in 1..7) grid[0][c] = 4
        grid[1][1] = 2

        val dotPiece = BlockShape(
            id = "test_dot_v14",
            name = "Dot",
            matrix = listOf(listOf(true)),
            colorId = 3
        )
        val fillerPiece = BlockShape(
            id = "test_sq_v14",
            name = "Sq",
            matrix = listOf(listOf(true, true)),
            colorId = 2
        )

        val stateField = viewModel.javaClass.getDeclaredField("_gameState")
        stateField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = stateField.get(viewModel) as MutableStateFlow<GameState>
        stateFlow.value = stateFlow.value.copy(
            grid = grid.map { it.toList() },
            lockedCells = setOf(0 to 6),
            mysteryCells = setOf(0 to 7),
            iceCells = emptyMap(),
            gemCells = emptySet(),
            adventureChainsBroken = 0,
            adventureInitialChainsTotal = 1,
            activePieces = listOf(dotPiece, fillerPiece, null)
        )

        val coinsBefore = prefs.coinsBalance
        val placed = viewModel.placeShape(0, 0, 0)
        assertTrue(placed)

        val afterClear = viewModel.gameState.value
        // Chain broken and removed from lockedCells, but underlying block at (0,6) stays!
        assertFalse((0 to 6) in afterClear.lockedCells)
        assertTrue(afterClear.grid[0][6] != 0)
        assertEquals(1, afterClear.adventureChainsBroken)

        // Mystery crate opened, removed from mysteryCells, block cleared, +25 coins + 1 booster granted
        assertFalse((0 to 7) in afterClear.mysteryCells)
        assertEquals(0, afterClear.grid[0][7])
        assertTrue(afterClear.coinsBalance >= coinsBefore + 25)
        assertTrue(afterClear.adventureLevelCompleted)

        // Undo should restore lockedCells, mysteryCells, and chainsBroken
        assertTrue(viewModel.undoLastMove())
        val afterUndo = viewModel.gameState.value
        assertTrue((0 to 6) in afterUndo.lockedCells)
        assertTrue((0 to 7) in afterUndo.mysteryCells)
        assertEquals(0, afterUndo.adventureChainsBroken)
    }

    @Test
    fun `zen mode never triggers game over and recovers when tray cannot fit`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = GamePreferences(context).apply {
            hasSeenTutorial = true
        }
        val viewModel = GameViewModel(
            context = context,
            soundManager = SoundManager(context),
            preferences = prefs,
            localSessionStorage = null
        )

        viewModel.switchGameMode(GameMode.ZEN)
        assertEquals(GameMode.ZEN, viewModel.gameState.value.gameMode)

        // Fill almost all cells in rows 1..7 so 3x3 piece cannot fit after placing dot at (0,0)
        val grid = MutableList(8) { r ->
            MutableList(8) { c ->
                if (r == 0) 0 else if (c % 2 == 0) 1 else 2
            }
        }
        // Leave (0,1..7) partially blocked so no 3x3 can fit anywhere after placing dot at (0,0)
        for (c in 1..6) grid[0][c] = 3

        val dotPiece = BlockShape(
            id = "zen_dot",
            name = "Dot",
            matrix = listOf(listOf(true)),
            colorId = 5
        )
        val huge3x3 = BlockShape(
            id = "zen_3x3",
            name = "3x3",
            matrix = List(3) { List(3) { true } },
            colorId = 6
        )

        val stateField = viewModel.javaClass.getDeclaredField("_gameState")
        stateField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = stateField.get(viewModel) as MutableStateFlow<GameState>
        stateFlow.value = stateFlow.value.copy(
            grid = grid.map { it.toList() },
            activePieces = listOf(dotPiece, huge3x3, null),
            isGameOver = false
        )

        assertTrue(viewModel.placeShape(0, 0, 0))
        val afterMove = viewModel.gameState.value
        assertFalse(afterMove.isGameOver)
        assertTrue(viewModel.refreshZenTray())
        assertFalse(viewModel.gameState.value.isGameOver)
    }

    @Test
    fun `score header displays 5 mode tabs including zen tab and zen banner within 360dp`() {
        var switchedMode: GameMode? = null
        var refreshClicked = false

        composeTestRule.setContent {
            ScoreHeader(
                score = 1200,
                highScore = 4500,
                comboStreak = 0,
                gameMode = GameMode.ZEN,
                onShowStats = {},
                onSwitchMode = { switchedMode = it },
                onRefreshZenTray = { refreshClicked = true },
                onRestart = {}
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("mode_tab_classic").assertIsDisplayed()
        composeTestRule.onNodeWithTag("mode_tab_time_attack").assertIsDisplayed()
        composeTestRule.onNodeWithTag("mode_tab_daily").assertIsDisplayed()
        composeTestRule.onNodeWithTag("mode_tab_adventure").assertIsDisplayed()
        composeTestRule.onNodeWithTag("mode_tab_zen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("zen_mode_banner").assertIsDisplayed()
        composeTestRule.onNodeWithTag("zen_refresh_tray_button").assertIsDisplayed().performClick()
        assertTrue(refreshClicked)

        composeTestRule.onNodeWithTag("mode_tab_classic").performClick()
        assertEquals(GameMode.CLASSIC, switchedMode)
    }

    @Test
    fun `rank tier progression and avatar badge showcase unlock and render cleanly`() {
        assertEquals(RankTier.BRONZE, RankTierCatalog.fromProgress(0L, 0))
        assertEquals(RankTier.SILVER, RankTierCatalog.fromProgress(5_000L, 1_500))
        assertEquals(RankTier.GOLD, RankTierCatalog.fromProgress(0L, 4_500))
        assertEquals(RankTier.GRANDMASTER, RankTierCatalog.fromProgress(500_000L, 1_000))

        var selectedAvatar = "rookie"
        composeTestRule.setContent {
            DailyLeaderboardDialog(
                entries = emptyList(),
                isLoading = false,
                errorMessage = null,
                dailyFormattedDate = "Nov 20, 2026",
                dailyPuzzleTitle = "Astral Vault",
                currentUserId = "uid_test",
                playerBestScore = 2400,
                gameMode = GameMode.CLASSIC,
                onPlayDaily = {},
                onDismiss = {},
                playerTotalScore = 26_000L,
                playerOverallBestScore = 4_200,
                selectedAvatarId = selectedAvatar,
                playerTotalStars = 12,
                onSelectAvatar = { selectedAvatar = it }
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("player_rank_badge").assertIsDisplayed()
        composeTestRule.onNodeWithText("🥇 GOLD", substring = true).assertIsDisplayed()

        // Hunter requires GOLD rank, unlocked at 26_000 total / 4_200 best
        val hunterSpec = PlayerAvatarCatalog.byId("hunter")
        assertTrue(hunterSpec.isUnlocked(RankTier.GOLD, 12))
        composeTestRule.onNodeWithTag("avatar_badge_hunter").assertIsDisplayed().performClick()
        assertEquals("hunter", selectedAvatar)
    }

    @Test
    fun `settings reminder switch and stats rank banner render and respond`() {
        var toggled = false
        composeTestRule.setContent {
            SettingsDialog(
                isMusicEnabled = true,
                isSoundEnabled = true,
                isHapticsEnabled = true,
                isDailyRemindersEnabled = false,
                onToggleMusic = {},
                onToggleSound = {},
                onToggleHaptics = {},
                onToggleReminders = { toggled = true },
                onDismiss = {}
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_reminders_switch").assertIsDisplayed().performClick()
        assertTrue(toggled)
    }

    @Test
    fun `stats dialog displays rank tier banner`() {
        composeTestRule.setContent {
            StatsDialog(
                highScore = 8200,
                highestCombo = 5,
                totalLines = 44,
                gamesPlayed = 10,
                onDismiss = {},
                totalAllTimeScore = 80_000L,
                selectedAvatarId = "artist",
                playerTotalStars = 28
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("stats_rank_banner").assertIsDisplayed()
        composeTestRule.onNodeWithText("💠 PLATINUM TIER", substring = true).assertIsDisplayed()
    }
}
