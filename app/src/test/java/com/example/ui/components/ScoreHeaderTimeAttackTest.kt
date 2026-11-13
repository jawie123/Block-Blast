package com.example.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.model.GameMode
import com.example.model.GameOverReason
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScoreHeaderTimeAttackTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `score header renders time attack tab timer banner and bonus badge`() {
        var mode by mutableStateOf(GameMode.CLASSIC)
        var remainingMs by mutableLongStateOf(60_000L)
        var bonusSeconds by mutableIntStateOf(0)
        var switchedTo: GameMode? = null

        composeTestRule.setContent {
            ScoreHeader(
                score = 450,
                highScore = 1200,
                comboStreak = 1,
                isSoundEnabled = true,
                isHapticsEnabled = true,
                gameMode = mode,
                timeRemainingMs = remainingMs,
                timeMaxMs = 99_000L,
                lastTimeBonusSeconds = bonusSeconds,
                onToggleSound = {},
                onToggleHaptics = {},
                onShowStats = {},
                onSwitchMode = { target ->
                    switchedTo = target
                    mode = target
                },
                onRestart = {}
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("mode_tab_time_attack").assertIsDisplayed()
        composeTestRule.onNodeWithTag("time_attack_timer_banner").assertDoesNotExist()

        // Switch to Time Attack mode
        composeTestRule.onNodeWithTag("mode_tab_time_attack").performClick()
        composeTestRule.waitForIdle()
        assertEquals(GameMode.TIME_ATTACK, switchedTo)

        composeTestRule.onNodeWithTag("time_attack_timer_banner").assertIsDisplayed()
        composeTestRule.onNodeWithTag("time_attack_timer_text").assertTextEquals("01:00")

        // Update remaining time to urgent 8s + show bonus badge
        composeTestRule.runOnUiThread {
            remainingMs = 8_000L
            bonusSeconds = 4
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("time_attack_timer_text").assertTextEquals("00:08")
        composeTestRule.onNodeWithTag("time_attack_bonus_badge").assertIsDisplayed()
        composeTestRule.onNodeWithTag("time_attack_bonus_badge").assertTextEquals("+4s!")
    }

    @Test
    fun `game over dialog shows TIME UP header and second chance time revive`() {
        composeTestRule.setContent {
            GameOverDialog(
                score = 1850,
                highScore = 2400,
                isNewHighScore = false,
                linesCleared = 12,
                maxCombo = 3,
                secondChanceAvailable = true,
                canUndo = true,
                gameMode = GameMode.TIME_ATTACK,
                gameOverReason = GameOverReason.TIME_UP,
                hammerCount = 2,
                onSecondChance = {},
                onRestart = {}
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("TIME'S UP!").assertIsDisplayed()
        composeTestRule.onNodeWithText("SECOND CHANCE (+15s TIME)").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dialog_undo_button").assertDoesNotExist()
        composeTestRule.onNodeWithTag("dialog_use_hammer_button").assertDoesNotExist()
    }
}
