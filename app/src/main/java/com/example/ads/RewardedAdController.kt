package com.example.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import kotlinx.coroutines.flow.StateFlow

enum class RewardedAdRewardType {
    SECOND_CHANCE,
    FREE_HAMMER
}

data class AdUiState(
    val isRewardedAdReady: Boolean = false,
    val isRewardedAdLoading: Boolean = false,
    val isAdShowing: Boolean = false,
    val isPrivacyOptionsRequired: Boolean = false
)

object AdMobConfig {
    /**
     * Official Google AdMob Test App ID (registered in AndroidManifest.xml until release).
     */
    const val TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"

    /**
     * Official Google AdMob Test Rewarded Ad Unit ID for development and pre-release QA.
     */
    const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
}

interface RewardedAdController {
    val adState: StateFlow<AdUiState>

    fun initializeAndGatherConsent(activity: Activity)

    fun preloadRewardedAd()

    fun showRewardedAd(
        activity: Activity,
        rewardType: RewardedAdRewardType,
        onAdStarted: () -> Unit,
        onRewardEarned: (RewardedAdRewardType) -> Unit,
        onAdClosedOrFailed: (String?) -> Unit
    )

    fun showPrivacyOptionsForm(
        activity: Activity,
        onComplete: (String?) -> Unit = {}
    )
}

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
