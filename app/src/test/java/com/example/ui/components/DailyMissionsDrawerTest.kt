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

        // Complete the 3-lines-in-one-move objective via ViewModel event
        val tripleLineMission = viewModel.gameState.value.dailyMissions
            .first { it.type == MissionType.CLEAR_LINES_SINGLE_MOVE }
        viewModel.recordMissionEvent(MissionType.CLEAR_LINES_SINGLE_MOVE, 3)
        composeTestRule.waitForIdle()

        // Open dedicated Daily Missions drawer from ScoreHeader
        composeTestRule.onNodeWithTag("daily_missions_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("daily_missions_drawer").assertIsDisplayed()
        composeTestRule.onNodeWithText("DAILY MISSIONS").assertIsDisplayed()
        composeTestRule.onNodeWithTag("missions_drawer_coins_value").assertIsDisplayed()
        composeTestRule.onNodeWithText("40").assertIsDisplayed()

        composeTestRule.onNodeWithTag("missions_drawer_list")
            .performScrollToNode(hasText("Clear 3 lines in one move"))
        composeTestRule.onNodeWithText("Clear 3 lines in one move").assertIsDisplayed()

        composeTestRule.onNodeWithTag("missions_drawer_list")
            .performScrollToNode(hasText("Reach 1,000 points in a game"))
        composeTestRule.onNodeWithText("Reach 1,000 points in a game").assertIsDisplayed()

        // Scroll back to claim bonus coin reward from drawer
        composeTestRule.onNodeWithTag("missions_drawer_list")
            .performScrollToNode(hasTestTag("claim_reward_button_${tripleLineMission.id}"))
        composeTestRule.onNodeWithTag("claim_reward_button_${tripleLineMission.id}")
            .assertIsEnabled()
            .performClick()
        composeTestRule.waitForIdle()

        val afterClaim = viewModel.gameState.value
        assertEquals(40 + tripleLineMission.coinReward, afterClaim.coinsBalance)
        assertEquals(40 + tripleLineMission.coinReward, prefs.coinsBalance)
        assertTrue(afterClaim.dailyMissions.first { it.id == tripleLineMission.id }.isClaimed)
        composeTestRule.onNodeWithText("${40 + tripleLineMission.coinReward}").assertIsDisplayed()

        // Close drawer via close button
        composeTestRule.onNodeWithTag("close_missions_drawer_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("daily_missions_drawer").assertDoesNotExist()
    }
}
