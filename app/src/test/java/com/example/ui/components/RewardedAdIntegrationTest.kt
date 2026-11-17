package com.example.ui.components

import android.app.Activity
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.ads.AdUiState
import com.example.ads.RewardedAdController
import com.example.ads.RewardedAdRewardType
import com.example.audio.SoundManager
import com.example.data.GamePreferences
import com.example.model.GameMode
import com.example.model.GameOverReason
import com.example.ui.GameScreen
import com.example.viewmodel.GameViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
class RewardedAdIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private class FakeRewardedAdController(
        initialState: AdUiState = AdUiState(isRewardedAdReady = true),
        private val earnRewardOnShow: Boolean = true
    ) : RewardedAdController {
        private val _adState = MutableStateFlow(initialState)
        override val adState: StateFlow<AdUiState> = _adState.asStateFlow()

        var requestedTypes = mutableListOf<RewardedAdRewardType>()
        var privacyFormShownCount = 0

        override fun initializeAndGatherConsent(activity: Activity) = Unit

        override fun preloadRewardedAd() = Unit

        override fun showRewardedAd(
            activity: Activity,
            rewardType: RewardedAdRewardType,
            onAdStarted: () -> Unit,
            onRewardEarned: (RewardedAdRewardType) -> Unit,
            onAdClosedOrFailed: (String?) -> Unit
        ) {
            requestedTypes.add(rewardType)
            onAdStarted()
            if (earnRewardOnShow) {
                onRewardEarned(rewardType)
            }
            onAdClosedOrFailed(null)
        }

        override fun showPrivacyOptionsForm(activity: Activity, onComplete: (String?) -> Unit) {
            privacyFormShownCount += 1
            onComplete(null)
        }
    }

    @Test
    fun `empty hammer HUD tap opens rewarded ad dialog and grants free hammer on reward`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = GamePreferences(context).apply {
            hasSeenTutorial = true
            hammerCount = 0
        }
        val viewModel = GameViewModel(
            context = context,
            soundManager = SoundManager(context),
            preferences = prefs,
            localSessionStorage = null
        )
        val fakeAds = FakeRewardedAdController(earnRewardOnShow = true)

        composeTestRule.setContent {
            GameScreen(
                viewModel = viewModel,
                adCoordinator = fakeAds
            )
        }
        composeTestRule.waitForIdle()

        assertEquals(0, viewModel.gameState.value.hammerCount)

        composeTestRule.onNodeWithTag("hammer_powerup_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("free_hammer_ad_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("free_hammer_watch_ad_button").performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(RewardedAdRewardType.FREE_HAMMER), fakeAds.requestedTypes)
        assertEquals(1, viewModel.gameState.value.hammerCount)
        assertEquals(1, prefs.hammerCount)
    }

    @Test
    fun `second chance rewarded ad only revives when reward is earned`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = GamePreferences(context).apply {
            hasSeenTutorial = true
            hammerCount = 0
        }
        val viewModel = GameViewModel(
            context = context,
            soundManager = SoundManager(context),
            preferences = prefs,
            localSessionStorage = null
        )

        // Verify early dismissal without reward preserves Game Over & Second Chance availability
        val abortedAdController = FakeRewardedAdController(earnRewardOnShow = false)
        var gameOverRevived = false
        viewModel.switchGameMode(GameMode.TIME_ATTACK)
        viewModel.tickTimeAttack(60_000L)

        assertTrue(viewModel.gameState.value.isGameOver)
        assertTrue(viewModel.gameState.value.secondChanceAvailable)

        // Direct test of ViewModel reward callbacks
        abortedAdController.showRewardedAd(
            activity = Activity(),
            rewardType = RewardedAdRewardType.SECOND_CHANCE,
            onAdStarted = { viewModel.onRewardedAdStarted() },
            onRewardEarned = {
                gameOverRevived = true
                viewModel.grantSecondChanceAfterAd()
            },
            onAdClosedOrFailed = { viewModel.onRewardedAdEnded() }
        )

        assertFalse(gameOverRevived)
        assertTrue(viewModel.gameState.value.isGameOver)
        assertTrue(viewModel.gameState.value.secondChanceAvailable)

        // Now complete the rewarded ad cleanly
        val completedAdController = FakeRewardedAdController(earnRewardOnShow = true)
        completedAdController.showRewardedAd(
            activity = Activity(),
            rewardType = RewardedAdRewardType.SECOND_CHANCE,
            onAdStarted = { viewModel.onRewardedAdStarted() },
            onRewardEarned = {
                gameOverRevived = true
                viewModel.grantSecondChanceAfterAd()
            },
            onAdClosedOrFailed = { viewModel.onRewardedAdEnded() }
        )

        assertTrue(gameOverRevived)
        assertFalse(viewModel.gameState.value.isGameOver)
        assertFalse(viewModel.gameState.value.secondChanceAvailable)
    }

    @Test
    fun `game over dialog displays watch ad badge and free hammer ad CTA when out of hammers`() {
        var freeHammerAdClicked = false
        composeTestRule.setContent {
            GameOverDialog(
                score = 420,
                highScore = 900,
                isNewHighScore = false,
                linesCleared = 5,
                maxCombo = 2,
                secondChanceAvailable = true,
                gameMode = GameMode.CLASSIC,
                gameOverReason = GameOverReason.NO_MOVES,
                hammerCount = 0,
                isRewardedAdReady = true,
                onSecondChance = {},
                onWatchAdForHammer = { freeHammerAdClicked = true },
                onRestart = {}
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("second_chance_ad_badge", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("dialog_watch_ad_hammer_button")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        assertTrue(freeHammerAdClicked)
    }

    @Test
    fun `settings dialog displays privacy options entry point when UMP requires consent management`() {
        var privacyClicked = false
        composeTestRule.setContent {
            SettingsDialog(
                isMusicEnabled = true,
                isSoundEnabled = true,
                isHapticsEnabled = true,
                isPrivacyOptionsRequired = true,
                onToggleMusic = {},
                onToggleSound = {},
                onToggleHaptics = {},
                onShowPrivacyOptions = { privacyClicked = true },
                onDismiss = {}
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("settings_privacy_options_button")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        assertTrue(privacyClicked)
        composeTestRule.onNodeWithText("Privacy & Ad Consent").assertExists()
    }
}
