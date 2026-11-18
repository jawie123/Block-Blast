package com.example.ui.components

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.audio.SoundManager
import com.example.data.GamePreferences
import com.example.model.MissionType
import com.example.ui.GameScreen
import com.example.viewmodel.GameViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DailyMissionsDrawerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `daily missions drawer opens shows objectives and rewards bonus coins on claim`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = GamePreferences(context).apply {
            hasSeenTutorial = true
            coinsBalance = 40
            totalCoinsEarned = 40
            hintCount = 1
            hammerCount = 2
        }
        val soundManager = SoundManager(context)
        val viewModel = GameViewModel(
            context = context,
            soundManager = soundManager,
            preferences = prefs,
            localSessionStorage = null
        )

        composeTestRule.setContent {
            GameScreen(viewModel = viewModel)
        }
        composeTestRule.waitForIdle()

        // Complete the 5-lines-in-one-move objective via ViewModel event
        val megaLineMission = viewModel.gameState.value.dailyMissions
            .first { it.type == MissionType.CLEAR_LINES_SINGLE_MOVE }
        viewModel.recordMissionEvent(MissionType.CLEAR_LINES_SINGLE_MOVE, 5)
        composeTestRule.waitForIdle()

        // Open dedicated Daily Missions drawer from ScoreHeader
        composeTestRule.onNodeWithTag("daily_missions_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("daily_missions_drawer").assertIsDisplayed()
        composeTestRule.onNodeWithText("DAILY MISSIONS").assertIsDisplayed()
        composeTestRule.onNodeWithTag("missions_drawer_coins_value").assertIsDisplayed()
        composeTestRule.onNodeWithText("40").assertIsDisplayed()
        composeTestRule.onNodeWithTag("missions_drawer_hint_value").assertIsDisplayed()

        composeTestRule.onNodeWithTag("missions_drawer_list")
            .performScrollToNode(hasText("Clear 5 lines in one move"))
        composeTestRule.onNodeWithText("Clear 5 lines in one move").assertIsDisplayed()

        composeTestRule.onNodeWithTag("missions_drawer_list")
            .performScrollToNode(hasText("Reach a score of 1000"))
        composeTestRule.onNodeWithText("Reach a score of 1000").assertIsDisplayed()

        // Scroll back to first mission card and claim bonus coin & hint reward from drawer
        composeTestRule.onNodeWithTag("missions_drawer_list").performScrollToIndex(0)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("claim_reward_button_${megaLineMission.id}")
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()
        composeTestRule.waitForIdle()

        val afterClaim = viewModel.gameState.value
        assertEquals(40 + megaLineMission.coinReward, afterClaim.coinsBalance)
        assertEquals(40 + megaLineMission.coinReward, prefs.coinsBalance)
        assertEquals(1 + megaLineMission.hintReward, afterClaim.hintCount)
        assertEquals(1 + megaLineMission.hintReward, prefs.hintCount)
        assertTrue(afterClaim.dailyMissions.first { it.id == megaLineMission.id }.isClaimed)
        composeTestRule.onNodeWithText("${40 + megaLineMission.coinReward}").assertIsDisplayed()

        // Close drawer via close button
        composeTestRule.onNodeWithTag("close_missions_drawer_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("daily_missions_drawer").assertDoesNotExist()

        // Tap Hint power-up button in ScoreHeader
        composeTestRule.onNodeWithTag("hint_powerup_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals(1 + megaLineMission.hintReward - 1, viewModel.gameState.value.hintCount)
        assertTrue(viewModel.gameState.value.activeHint != null)
    }
}
