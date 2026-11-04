package com.herwin.netez.utils

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.herwin.netez.R

object RewardedAdManager {

    private const val TAG = "RewardedAdManager"

    private var rewardedAd: RewardedAd? = null
    private var isLoading = false

    private fun getAdUnitId(context: Context): String {
        return try {
            context.getString(R.string.admob_rewarded_ad_unit_id)
        } catch (e: Exception) {
            "ca-app-pub-3202596830519022/8432957469"
        }
    }

    fun loadAd(context: Context) {
        if (rewardedAd != null || isLoading) return
        isLoading = true

        val adUnitId = getAdUnitId(context)
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context.applicationContext,
            adUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Rewarded Ad Loaded Successfully ($adUnitId)")
                    rewardedAd = ad
                    isLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Rewarded Ad Failed to Load: ${error.message}")
                    rewardedAd = null
                    isLoading = false
                }
            }
        )
    }

    fun showAd(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onAdFailed: (String) -> Unit
    ) {
        val ad = rewardedAd
        if (ad != null) {
            ad.show(activity) { rewardItem ->
                Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                rewardedAd = null
                loadAd(activity)
                onRewardEarned()
            }
        } else {
            // Preload for next attempt
            loadAd(activity)
            // Fallback: If ad is not ready, grant reward so testing is not blocked
            Log.w(TAG, "Rewarded Ad not ready yet, granting testing bonus fallback")
            onRewardEarned()
        }
    }
}
