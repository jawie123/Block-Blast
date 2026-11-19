package com.example.ui.components

import android.app.Activity
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.ads.AdUiState
import com.example.ads.RewardedAdController
import com.example.ads.RewardedAdRewardType
import com.example.audio.SoundManager
import com.example.data.GamePreferences
import com.example.model.BlockShape
import com.example.model.GameState
import com.example.model.LuckySpinCatalog
import com.example.model.SpinRewardType
import com.example.ui.GameScreen
import com.example.viewmodel.GameViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class HoldLaserSpinTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun singleCell(id: String = "dot_1", colorId: Int = 2) = BlockShape(
        id = id,
        name = id,
        matrix = listOf(listOf(true)),
        colorId = colorId
    )

    private fun twoByTwo(id: String = "sq_2", colorId: Int = 4) = BlockShape(
        id = id,
        name = id,
        matrix = listOf(
            listOf(true, true),
            listOf(true, true)
        ),
        colorId = colorId
    )

    @Suppress("UNCHECKED_CAST")
    private fun getMutableState(viewModel: GameViewModel): MutableStateFlow<GameState> {
        val stateField = viewModel.javaClass.getDeclaredField("_gameState")
        stateField.isAccessible = true
        return stateField.get(viewModel) as MutableStateFlow<GameState>
    }

    @Test
    fun `hold piece pocket stores swaps locks per turn recharges on placement and supports undo`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = GamePreferences(context).apply { hasSeenTutorial = true }
        val viewModel = GameViewModel(
            context = context,
            soundManager = SoundManager(context),
            preferences = prefs,
            localSessionStorage = null
        )

        val pieceA = singleCell("dot_a", colorId = 1)
        val pieceB = twoByTwo("sq_b", colorId = 3)
        val pieceC = singleCell("dot_c", colorId = 5)
        getMutableState(viewModel).value = viewModel.gameState.value.copy(
            grid = List(8) { List(8) { 0 } },
            activePieces = listOf(pieceA, pieceB, pieceC),
            heldPiece = null,
            canHoldThisTurn = true
        )

        // Select slot 1 (pieceB) and hold it
        viewModel.selectPieceForTap(1)
        assertTrue(viewModel.holdOrSwapPiece())

        val afterHold = viewModel.gameState.value
        assertEquals("sq_b", afterHold.heldPiece?.id)
        assertNull(afterHold.activePieces[1])
        assertFalse(afterHold.canHoldThisTurn)

        // Second hold before placing a block must be rejected
        assertFalse(viewModel.holdOrSwapPiece(0))

        // Undo restores original tray and hold availability
        assertTrue(viewModel.undoLastMove())
        assertNull(viewModel.gameState.value.heldPiece)
        assertEquals("sq_b", viewModel.gameState.value.activePieces[1]?.id)
        assertTrue(viewModel.gameState.value.canHoldThisTurn)

        // Hold pieceB again, then place pieceA -> Hold recharges
        assertTrue(viewModel.holdOrSwapPiece(1))
        assertFalse(viewModel.gameState.value.canHoldThisTurn)
        assertTrue(viewModel.placeShape(0, 0, 0))
        assertTrue(viewModel.gameState.value.canHoldThisTurn)

        // Now swap held piece (pieceB) with slot 2 (pieceC)
        assertTrue(viewModel.holdOrSwapPiece(2))
        assertEquals("dot_c", viewModel.gameState.value.heldPiece?.id)
        assertEquals("sq_b", viewModel.gameState.value.activePieces[2]?.id)
    }

    @Test
    fun `cross laser spawns on multi-line clear and blasts intersecting row and column when cleared`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = GamePreferences(context).apply { hasSeenTutorial = true }
        val viewModel = GameViewModel(
            context = context,
            soundManager = SoundManager(context),
            preferences = prefs,
            localSessionStorage = null
        )

        // Part 1: Clearing 2 lines at once spawns a ⚡ Cross-Laser on a remaining occupied cell
        val spawnGrid = MutableList(8) { MutableList(8) { 0 } }
        for (c in 1..7) {
            spawnGrid[0][c] = 1
            spawnGrid[1][c] = 2
        }
        spawnGrid[6][6] = 4 // Survivor block eligible to receive ⚡ charge
        val vBar2 = BlockShape(
            id = "v2_spawn",
            name = "V2",
            matrix = listOf(listOf(true), listOf(true)),
            colorId = 5
        )
        getMutableState(viewModel).value = viewModel.gameState.value.copy(
            grid = spawnGrid.map { it.toList() },
            activePieces = listOf(vBar2, singleCell(), null),
            lightningCells = emptySet()
        )
        assertTrue(viewModel.placeShape(0, 0, 0))
        assertTrue(
            "Expected ⚡ Cross-Laser to spawn on survivor block (6,6)",
            (6 to 6) in viewModel.gameState.value.lightningCells
        )

        // Part 2: Clearing a row containing a ⚡ cell blasts its entire intersecting row + column
        val cascadeGrid = MutableList(8) { MutableList(8) { 0 } }
        for (c in 1..7) cascadeGrid[2][c] = 3
        // Place extra blocks along col 5 (where ⚡ sits at (2,5)) and row 2
        cascadeGrid[0][5] = 4
        cascadeGrid[5][5] = 6
        cascadeGrid[7][5] = 7

        val dotPiece = singleCell("dot_trigger", colorId = 2)
        getMutableState(viewModel).value = viewModel.gameState.value.copy(
            grid = cascadeGrid.map { it.toList() },
            activePieces = listOf(dotPiece, singleCell(), singleCell()),
            lightningCells = setOf(2 to 5),
            iceCells = mapOf((5 to 5) to 1),
            gemCells = setOf(7 to 5),
            score = 0
        )

        assertTrue(viewModel.placeShape(0, 2, 0))
        val afterCascade = viewModel.gameState.value
        assertEquals(0, afterCascade.grid[0][5])
        assertEquals(0, afterCascade.grid[5][5])
        assertEquals(0, afterCascade.grid[7][5])
        assertTrue(afterCascade.lightningCells.isEmpty())
        assertTrue(afterCascade.iceCells.isEmpty())
        assertTrue(afterCascade.gemCells.isEmpty())
        assertTrue("Score should include laser extra points", afterCascade.score > 110)

        // Undo restores grid & lightningCells
        assertTrue(viewModel.undoLastMove())
        assertEquals(setOf(2 to 5), viewModel.gameState.value.lightningCells)
        assertEquals(4, viewModel.gameState.value.grid[0][5])
    }

    @Test
    fun `lucky spin awards daily free spin once per day and supports ad-earned bonus spins`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = GamePreferences(context).apply {
            hasSeenTutorial = true
            coinsBalance = 10
            hammerCount = 2
            lastLuckySpinDate = ""
            bonusSpinCount = 0
        }
        val viewModel = GameViewModel(
            context = context,
            soundManager = SoundManager(context),
            preferences = prefs,
            localSessionStorage = null
        )

        assertTrue(viewModel.gameState.value.canClaimFreeLuckySpin)

        // Force landing on slice 4 (+100 COINS)
        val firstResult = viewModel.spinLuckyWheel(forcedSliceIndex = 4)
        assertNotNull(firstResult)
        assertTrue(firstResult!!.usedFreeDailySpin)
        assertEquals(SpinRewardType.COINS, firstResult.slice.rewardType)
        assertEquals(110, viewModel.gameState.value.coinsBalance)
        assertFalse(viewModel.gameState.value.canClaimFreeLuckySpin)

        // Second spin without bonus spin must return null
        assertNull(viewModel.spinLuckyWheel())

        // Grant +1 Bonus Spin via rewarded ad and force slice 1 (+1 HAMMER)
        viewModel.grantBonusSpinAfterAd()
        assertEquals(1, viewModel.gameState.value.bonusSpinCount)
        val secondResult = viewModel.spinLuckyWheel(forcedSliceIndex = 1)
        assertNotNull(secondResult)
        assertFalse(secondResult!!.usedFreeDailySpin)
        assertEquals(3, viewModel.gameState.value.hammerCount)
        assertEquals(0, viewModel.gameState.value.bonusSpinCount)
        assertEquals(LuckySpinCatalog.slices[1], secondResult.slice)
    }

    @Test
    fun `hold pocket and lucky spin dialog render and work cleanly on w360dp-h800dp`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = GamePreferences(context).apply {
            hasSeenTutorial = true
            coinsBalance = 40
            lastLuckySpinDate = ""
            bonusSpinCount = 0
        }
        val viewModel = GameViewModel(
            context = context,
            soundManager = SoundManager(context),
            preferences = prefs,
            localSessionStorage = null
        )

        val fakeAds = object : RewardedAdController {
            override val adState: StateFlow<AdUiState> =
                MutableStateFlow(AdUiState(isRewardedAdReady = true)).asStateFlow()

            override fun initializeAndGatherConsent(activity: Activity) = Unit
            override fun preloadRewardedAd() = Unit
            override fun showRewardedAd(
                activity: Activity,
                rewardType: RewardedAdRewardType,
                onAdStarted: () -> Unit,
                onRewardEarned: (RewardedAdRewardType) -> Unit,
                onAdClosedOrFailed: (String?) -> Unit
            ) {
                onRewardEarned(rewardType)
                onAdClosedOrFailed(null)
            }

            override fun showPrivacyOptionsForm(activity: Activity, onComplete: (String?) -> Unit) = Unit
        }

        composeTestRule.setContent {
            GameScreen(viewModel = viewModel, adCoordinator = fakeAds)
        }
        composeTestRule.waitForIdle()

        // Verify Hold Pocket, tray slots, board, and Lucky Spin pill are all displayed cleanly
        composeTestRule.onNodeWithTag("hold_piece_slot").assertIsDisplayed()
        composeTestRule.onNodeWithTag("piece_slot_0").assertIsDisplayed()
        composeTestRule.onNodeWithTag("grid_board_container").assertIsDisplayed()
        composeTestRule.onNodeWithTag("high_score_pill").assertIsDisplayed()
        composeTestRule.onNodeWithTag("lucky_spin_button").assertIsDisplayed()

        // Tap Hold Pocket to store first tray piece
        composeTestRule.onNodeWithTag("hold_piece_slot").performClick()
        composeTestRule.waitForIdle()
        assertNotNull(viewModel.gameState.value.heldPiece)
        assertFalse(viewModel.gameState.value.canHoldThisTurn)

        // Open Lucky Spin Wheel dialog and claim free daily spin
        composeTestRule.onNodeWithTag("lucky_spin_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("lucky_spin_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("lucky_spin_trigger_button").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("lucky_spin_result_banner").assertIsDisplayed()
        assertFalse(viewModel.gameState.value.canClaimFreeLuckySpin)
    }
}
