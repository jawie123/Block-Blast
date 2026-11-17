package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Coordinates Google User Messaging Platform (UMP) consent gathering and
 * Google Mobile Ads Rewarded Ad preloading/playback.
 */
class AdMobRewardedCoordinator(
    context: Context,
    private val rewardedAdUnitId: String = AdMobConfig.TEST_REWARDED_AD_UNIT_ID
) : RewardedAdController {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(appContext)
    private val isMobileAdsInitStarted = AtomicBoolean(false)

    private var currentRewardedAd: RewardedAd? = null

    private val _adState = MutableStateFlow(
        AdUiState(
            isPrivacyOptionsRequired = consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        )
    )
    override val adState: StateFlow<AdUiState> = _adState.asStateFlow()

    override fun initializeAndGatherConsent(activity: Activity) {
        val params = ConsentRequestParameters.Builder().build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                if (activity.isFinishing || activity.isDestroyed) return@requestConsentInfoUpdate
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { loadAndShowError ->
                    if (loadAndShowError != null) {
                        Log.w(TAG, "UMP consent form error: ${loadAndShowError.message}")
                    }
                    refreshPrivacyRequirement()
                    if (consentInformation.canRequestAds()) {
                        initializeMobileAdsSdk()
                    }
                }
            },
            { requestConsentError ->
                Log.w(TAG, "UMP consent update failed: ${requestConsentError.message}")
                refreshPrivacyRequirement()
                if (consentInformation.canRequestAds()) {
                    initializeMobileAdsSdk()
                }
            }
        )

        // If consent was already obtained in an earlier session, initialize immediately in background
        if (consentInformation.canRequestAds()) {
            initializeMobileAdsSdk()
        }
    }

    private fun refreshPrivacyRequirement() {
        val required = consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        _adState.update { it.copy(isPrivacyOptionsRequired = required) }
    }

    private fun initializeMobileAdsSdk() {
        if (isMobileAdsInitStarted.getAndSet(true)) {
            if (currentRewardedAd == null && !_adState.value.isRewardedAdLoading) {
                preloadRewardedAd()
            }
            return
        }

        scope.launch {
            withContext(Dispatchers.IO) {
                MobileAds.initialize(appContext) {
                    scope.launch {
                        preloadRewardedAd()
                    }
                }
            }
        }
    }

    override fun preloadRewardedAd() {
        if (!consentInformation.canRequestAds()) return
        if (currentRewardedAd != null || _adState.value.isRewardedAdLoading) return

        _adState.update { it.copy(isRewardedAdLoading = true) }

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            appContext,
            rewardedAdUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    currentRewardedAd = ad
                    _adState.update {
                        it.copy(
                            isRewardedAdReady = true,
                            isRewardedAdLoading = false
                        )
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.w(TAG, "RewardedAd failed to load: ${loadAdError.message}")
                    currentRewardedAd = null
                    _adState.update {
                        it.copy(
                            isRewardedAdReady = false,
                            isRewardedAdLoading = false
                        )
                    }
                }
            }
        )
    }

    override fun showRewardedAd(
        activity: Activity,
        rewardType: RewardedAdRewardType,
        onAdStarted: () -> Unit,
        onRewardEarned: (RewardedAdRewardType) -> Unit,
        onAdClosedOrFailed: (String?) -> Unit
    ) {
        if (_adState.value.isAdShowing) {
            onAdClosedOrFailed(null)
            return
        }

        val ad = currentRewardedAd
        if (ad == null) {
            if (consentInformation.canRequestAds()) {
                initializeMobileAdsSdk()
                preloadRewardedAd()
                onAdClosedOrFailed("Ad is loading — please try again in a moment.")
            } else {
                initializeAndGatherConsent(activity)
                onAdClosedOrFailed("Consent check required before loading ads.")
            }
            return
        }

        currentRewardedAd = null
        _adState.update {
            it.copy(
                isRewardedAdReady = false,
                isAdShowing = true
            )
        }
        onAdStarted()

        var finishedNotified = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                _adState.update { it.copy(isAdShowing = false) }
                if (!finishedNotified) {
                    finishedNotified = true
                    onAdClosedOrFailed(null)
                }
                preloadRewardedAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.w(TAG, "RewardedAd failed to show: ${adError.message}")
                _adState.update { it.copy(isAdShowing = false) }
                if (!finishedNotified) {
                    finishedNotified = true
                    onAdClosedOrFailed("Unable to display ad right now. Try again shortly.")
                }
                preloadRewardedAd()
            }
        }

        ad.show(activity) {
            onRewardEarned(rewardType)
        }
    }

    override fun showPrivacyOptionsForm(
        activity: Activity,
        onComplete: (String?) -> Unit
    ) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            refreshPrivacyRequirement()
            if (consentInformation.canRequestAds()) {
                initializeMobileAdsSdk()
            }
            onComplete(formError?.message)
        }
    }

    companion object {
        private const val TAG = "AdMobCoordinator"
    }
}
