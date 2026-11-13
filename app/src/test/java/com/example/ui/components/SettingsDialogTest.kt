package com.example.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `score header settings button opens settings dialog and toggles music and sound effects`() {
        var showSettings by mutableStateOf(false)
        var musicEnabled by mutableStateOf(true)
        var soundEnabled by mutableStateOf(true)
        var hapticsEnabled by mutableStateOf(true)

        composeTestRule.setContent {
            ScoreHeader(
                score = 120,
                highScore = 500,
                comboStreak = 0,
                isSoundEnabled = soundEnabled,
                isHapticsEnabled = hapticsEnabled,
                onShowSettings = { showSettings = true },
                onShowStats = {},
                onRestart = {}
            )

            if (showSettings) {
                SettingsDialog(
                    isMusicEnabled = musicEnabled,
                    isSoundEnabled = soundEnabled,
                    isHapticsEnabled = hapticsEnabled,
                    onToggleMusic = { musicEnabled = !musicEnabled },
                    onToggleSound = { soundEnabled = !soundEnabled },
                    onToggleHaptics = { hapticsEnabled = !hapticsEnabled },
                    onDismiss = { showSettings = false }
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_dialog").assertDoesNotExist()

        // Open Settings Menu
        composeTestRule.onNodeWithTag("settings_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("settings_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("SETTINGS").assertIsDisplayed()
        composeTestRule.onNodeWithText("Background Music").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sound Effects").assertIsDisplayed()
        composeTestRule.onNodeWithText("Haptic Feedback").assertIsDisplayed()

        composeTestRule.onNodeWithTag("settings_music_switch").assertIsOn()
        composeTestRule.onNodeWithTag("settings_sfx_switch").assertIsOn()
        composeTestRule.onNodeWithTag("settings_haptics_switch").assertIsOn()

        // Toggle Background Music off
        composeTestRule.onNodeWithTag("settings_music_switch").performClick()
        composeTestRule.waitForIdle()
        assertFalse(musicEnabled)
        composeTestRule.onNodeWithTag("settings_music_switch").assertIsOff()

        // Toggle Sound Effects off via row click
        composeTestRule.onNodeWithTag("settings_sfx_row").performClick()
        composeTestRule.waitForIdle()
        assertFalse(soundEnabled)
        composeTestRule.onNodeWithTag("settings_sfx_switch").assertIsOff()

        // Toggle Haptics off and back on
        composeTestRule.onNodeWithTag("settings_haptics_switch").performClick()
        composeTestRule.waitForIdle()
        assertFalse(hapticsEnabled)
        composeTestRule.onNodeWithTag("settings_haptics_switch").assertIsOff()

        // Re-enable Music
        composeTestRule.onNodeWithTag("settings_music_switch").performClick()
        composeTestRule.waitForIdle()
        assertTrue(musicEnabled)
        composeTestRule.onNodeWithTag("settings_music_switch").assertIsOn()

        // Dismiss via Done button
        composeTestRule.onNodeWithTag("settings_done_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_dialog").assertDoesNotExist()
    }
}
