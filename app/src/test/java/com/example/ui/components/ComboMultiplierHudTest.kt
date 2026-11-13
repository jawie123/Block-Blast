package com.example.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ComboMultiplierHudTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `combo multiplier hud hides at zero streak and pops up in real time on line clears`() {
        var streak by mutableIntStateOf(0)
        var eventId by mutableLongStateOf(0L)
        var clearedLines by mutableIntStateOf(0)
        var prospectiveLines by mutableIntStateOf(0)

        composeTestRule.setContent {
            ComboMultiplierHud(
                comboStreak = streak,
                comboEventId = eventId,
                lastClearedLinesCount = clearedLines,
                prospectiveLinesCount = prospectiveLines
            )
        }

        // Hidden when streak is 0 and no prospective clear
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("combo_multiplier_hud").assertDoesNotExist()

        // First line clear triggers 1x combo HUD
        composeTestRule.runOnUiThread {
            streak = 1
            eventId = 1L
            clearedLines = 1
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("combo_multiplier_hud").assertIsDisplayed()
        composeTestRule.onNodeWithTag("combo_hud_multiplier_text").assertTextEquals("1x")
        composeTestRule.onNodeWithText("+1 LINE CLEARED!").assertIsDisplayed()

        // Consecutive multi-line clear advances real-time multiplier to 3x
        composeTestRule.runOnUiThread {
            streak = 3
            eventId = 2L
            clearedLines = 2
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("combo_hud_multiplier_text").assertTextEquals("3x")
        composeTestRule.onNodeWithText("MEGA COMBO").assertIsDisplayed()
        composeTestRule.onNodeWithText("+2 LINES CLEARED!").assertIsDisplayed()
    }
}
