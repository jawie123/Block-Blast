package com.example.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
class ScoreHeaderDifficultyTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `score header difficulty badge updates as tier advances`() {
        var level by mutableIntStateOf(1)
        var title by mutableStateOf("STARTER")
        var nextScore by mutableStateOf<Int?>(500)
        var unlockedVariety by mutableIntStateOf(com.example.model.ShapeDefinitions.getUnlockedVarietyCount(0))
        val totalVariety = com.example.model.ShapeDefinitions.totalShapeVarietyCount

        composeTestRule.setContent {
            ScoreHeader(
                score = 250,
                highScore = 1000,
                comboStreak = 0,
                isSoundEnabled = true,
                isHapticsEnabled = true,
                difficultyLevel = level,
                difficultyTitle = title,
                difficultyProgress = 0.5f,
                nextDifficultyScore = nextScore,
                unlockedShapeVariety = unlockedVariety,
                totalShapeVariety = totalVariety,
                onToggleSound = {},
                onToggleHaptics = {},
                onShowStats = {},
                onRestart = {}
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("difficulty_badge").assertIsDisplayed()
        composeTestRule.onNodeWithTag("difficulty_badge").assertTextEquals("LV 1 STARTER")
        composeTestRule.onNodeWithTag("difficulty_shape_variety_badge", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("${unlockedVariety}/${totalVariety}", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("NEXT 500").assertIsDisplayed()

        composeTestRule.runOnUiThread {
            level = 5
            title = "GRANDMASTER"
            nextScore = null
            unlockedVariety = totalVariety
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("difficulty_badge").assertTextEquals("LV 5 GRANDMASTER")
        composeTestRule.onNodeWithText("${totalVariety}/${totalVariety}", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("MAX TIER").assertIsDisplayed()
    }
}
