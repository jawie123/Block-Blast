package com.example.ui.components

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.GamePreferences
import com.example.model.GameMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuthScreenGuestTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `auth screen displays google sign in and continue as guest buttons`() {
        var guestEntered = false

        composeTestRule.setContent {
            AuthScreen(
                onAuthSuccess = {},
                onContinueAsGuest = { guestEntered = true }
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("auth_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("google_sign_in_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("guest_sign_in_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Continue as Guest").assertIsDisplayed()

        composeTestRule.onNodeWithTag("guest_sign_in_button").performClick()
        composeTestRule.waitForIdle()
        assertTrue(guestEntered)
    }

    @Test
    fun `score header displays guest badge and keeps sign in switch button visible`() {
        composeTestRule.setContent {
            ScoreHeader(
                score = 200,
                highScore = 400,
                comboStreak = 0,
                isGuestMode = true,
                onShowStats = {},
                onRestart = {}
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("guest_mode_badge").assertIsDisplayed()
        composeTestRule.onNodeWithText("GUEST").assertIsDisplayed()
        composeTestRule.onNodeWithTag("sign_out_button").assertIsDisplayed()
    }

    @Test
    fun `leaderboard dialog shows guest sign in prompt when playing as guest`() {
        var signInTriggered = false

        composeTestRule.setContent {
            DailyLeaderboardDialog(
                entries = emptyList(),
                isLoading = false,
                errorMessage = null,
                dailyFormattedDate = "Nov 16, 2026",
                dailyPuzzleTitle = "Daily Grid",
                currentUserId = "",
                playerBestScore = 0,
                gameMode = GameMode.CLASSIC,
                onPlayDaily = {},
                onDismiss = {},
                globalEntries = emptyList(),
                isGlobalLoading = false,
                globalErrorMessage = null,
                playerTotalScore = 0L,
                isGuestMode = true,
                onSignInWithGoogle = { signInTriggered = true }
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("leaderboard_guest_prompt").assertIsDisplayed()
        composeTestRule.onNodeWithText("Guest Mode Active").assertIsDisplayed()
        composeTestRule.onNodeWithTag("leaderboard_guest_sign_in_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("leaderboard_guest_sign_in_button").performClick()
        composeTestRule.waitForIdle()
        assertTrue(signInTriggered)
    }

    @Test
    fun `guest session preference persists across preference instances`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs1 = GamePreferences(context)
        assertFalse(prefs1.isGuestSession)

        prefs1.isGuestSession = true
        val prefs2 = GamePreferences(context)
        assertTrue(prefs2.isGuestSession)

        prefs2.isGuestSession = false
        assertFalse(GamePreferences(context).isGuestSession)
    }
}
