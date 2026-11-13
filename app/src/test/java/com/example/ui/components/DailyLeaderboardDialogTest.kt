package com.example.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.model.DailyLeaderboardEntry
import com.example.model.GameMode
import com.example.model.GlobalLeaderboardEntry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DailyLeaderboardDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `global leaderboard tab displays all-time rankings and switches to daily tab`() {
        val globalEntries = listOf(
            GlobalLeaderboardEntry(
                id = "uid_bob",
                userId = "uid_bob",
                displayName = "BobMaster",
                totalScore = 45200L,
                bestScore = 6200L,
                totalLinesCleared = 120L,
                highestCombo = 7L
            ),
            GlobalLeaderboardEntry(
                id = "uid_alice",
                userId = "uid_alice",
                displayName = "AlicePlayer",
                totalScore = 18500L,
                bestScore = 3400L,
                totalLinesCleared = 54L,
                highestCombo = 4L
            )
        )
        val dailyEntries = listOf(
            DailyLeaderboardEntry(
                id = "uid_alice",
                userId = "uid_alice",
                displayName = "AlicePlayer",
                date = "2026-11-13",
                score = 3400L,
                linesCleared = 18L,
                maxCombo = 4L
            )
        )

        composeTestRule.setContent {
            DailyLeaderboardDialog(
                entries = dailyEntries,
                isLoading = false,
                errorMessage = null,
                dailyFormattedDate = "Nov 13, 2026",
                dailyPuzzleTitle = "Prism Matrix",
                currentUserId = "uid_alice",
                playerBestScore = 3400,
                gameMode = GameMode.CLASSIC,
                onPlayDaily = {},
                onDismiss = {},
                globalEntries = globalEntries,
                isGlobalLoading = false,
                globalErrorMessage = null,
                playerTotalScore = 18500L,
                initialTab = LeaderboardTab.ALL_TIME
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("daily_leaderboard_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("GLOBAL LEADERBOARD").assertIsDisplayed()
        composeTestRule.onNodeWithText("LIVE").assertIsDisplayed()
        composeTestRule.onNodeWithTag("leaderboard_player_summary_score").assertTextEquals("18,500")
        composeTestRule.onNodeWithText("#2").assertIsDisplayed()
        composeTestRule.onNodeWithText("BobMaster").assertIsDisplayed()
        composeTestRule.onNodeWithText("45,200").assertIsDisplayed()
        composeTestRule.onNodeWithText("AlicePlayer").assertIsDisplayed()
        composeTestRule.onNodeWithText("YOU").assertIsDisplayed()

        // Switch to Daily Leaderboard tab
        composeTestRule.onNodeWithTag("leaderboard_tab_daily").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("DAILY LEADERBOARD").assertIsDisplayed()
        composeTestRule.onNodeWithTag("leaderboard_player_summary_score").assertTextEquals("3,400")
        composeTestRule.onNodeWithText("#1").assertIsDisplayed()
    }
}
