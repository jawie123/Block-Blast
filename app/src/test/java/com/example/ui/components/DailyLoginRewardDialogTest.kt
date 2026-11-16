package com.example.ui.components

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.audio.SoundManager
import com.example.data.GamePreferences
import com.example.model.DailyLoginRewardSchedule
import com.example.ui.GameScreen
import com.example.viewmodel.GameViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DailyLoginRewardDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `daily login reward dialog displays 7-day calendar claims reward and crafts hammer`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = GamePreferences(context).apply {
            hasSeenTutorial = true
            coinsBalance = 60
            hammerCount = 3
            lastLoginClaimDate = ""
            loginStreak = 0
            longestLoginStreak = 0
            totalLoginDaysClaimed = 0
            totalCoinsEarned = 60
            totalLoginHammersEarned = 0
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

        // Open Daily Login Rewards from ScoreHeader quick bar
        composeTestRule.onNodeWithTag("daily_login_reward_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("daily_login_reward_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("daily_login_day_1").assertExists()
        composeTestRule.onNodeWithTag("daily_login_day_7").assertExists()

        // Day 1 gives +50 coins (60 -> 110), enabling the 100-coin Hammer craft button
        composeTestRule.onNodeWithTag("exchange_coins_hammer_button").performScrollTo().assertIsNotEnabled()
        composeTestRule.onNodeWithTag("claim_daily_login_reward_button")
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        composeTestRule.waitForIdle()

        val afterClaim = viewModel.gameState.value
        assertEquals(110, afterClaim.coinsBalance)
        assertEquals(1, afterClaim.loginStreak)
        assertFalse(afterClaim.canClaimDailyLoginReward)

        composeTestRule.onNodeWithTag("claim_daily_login_reward_button").performScrollTo().assertIsNotEnabled()
        composeTestRule.onNodeWithText("✓ CLAIMED TODAY • COME BACK TOMORROW").assertExists()

        // Exchange 100 coins for +1 Hammer (110 -> 10 coins, 3 -> 4 hammers)
        composeTestRule.onNodeWithTag("exchange_coins_hammer_button")
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        composeTestRule.waitForIdle()

        val afterCraft = viewModel.gameState.value
        assertEquals(10, afterCraft.coinsBalance)
        assertEquals(4, afterCraft.hammerCount)
        assertEquals(4, prefs.hammerCount)
    }

    @Test
    fun `consecutive day login in viewModel grants Day 2 hammer reward and updates longest streak`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val soundManager = SoundManager(context)
        val prefs = GamePreferences(context).apply {
            hasSeenTutorial = true
            coinsBalance = 50
            hammerCount = 3
            loginStreak = 1
            longestLoginStreak = 1
            totalLoginDaysClaimed = 1
            totalCoinsEarned = 50
            totalLoginHammersEarned = 0
        }
        val viewModel = GameViewModel(
            context = context,
            soundManager = soundManager,
            preferences = prefs,
            localSessionStorage = null
        )
        // Simulate yesterday's claim after initial creation
        val yesterday = viewModel.getYesterdayDateString()
        prefs.lastLoginClaimDate = yesterday

        // Re-create ViewModel with yesterday's claim recorded
        val nextDayVm = GameViewModel(
            context = context,
            soundManager = soundManager,
            preferences = prefs,
            localSessionStorage = null
        )
        assertTrue(nextDayVm.gameState.value.canClaimDailyLoginReward)
        assertEquals(2, nextDayVm.gameState.value.dailyLoginNextCycleDay)

        nextDayVm.claimDailyLoginReward()
        val state = nextDayVm.gameState.value
        assertEquals(2, state.loginStreak)
        assertEquals(2, state.longestLoginStreak)
        assertEquals(3 + DailyLoginRewardSchedule.tierForStreak(2).hammers, state.hammerCount)
        assertFalse(state.canClaimDailyLoginReward)
    }
}
