package com.herwin.netez.entitlement

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FeatureQuotaManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("netez_quota_prefs", Context.MODE_PRIVATE)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private fun getTodayDateString(): String {
        return dateFormat.format(Date())
    }

    private fun checkAndResetDailyQuota() {
        val today = getTodayDateString()
        val lastSavedDate = prefs.getString("last_quota_date", "") ?: ""
        if (today != lastSavedDate) {
            prefs.edit().apply {
                putString("last_quota_date", today)
                // Clear all daily usage counters
                FeatureId.entries.forEach { id ->
                    remove("usage_${id.name}")
                    remove("reward_bonus_${id.name}")
                }
                apply()
            }
        }
    }

    fun getUsageCount(featureId: FeatureId): Int {
        checkAndResetDailyQuota()
        return prefs.getInt("usage_${featureId.name}", 0)
    }

    fun incrementUsage(featureId: FeatureId): Int {
        checkAndResetDailyQuota()
        val current = getUsageCount(featureId)
        val newCount = current + 1
        prefs.edit().putInt("usage_${featureId.name}", newCount).apply()
        return newCount
    }

    fun getRewardedBonusCount(featureId: FeatureId): Int {
        checkAndResetDailyQuota()
        return prefs.getInt("reward_bonus_${featureId.name}", 0)
    }

    fun grantRewardedBonus(featureId: FeatureId): Boolean {
        checkAndResetDailyQuota()
        if (featureId == FeatureId.GLOBAL_TOOL_USAGE) {
            val currentBonus = getRewardedBonusCount(featureId)
            prefs.edit().putInt("reward_bonus_${featureId.name}", currentBonus + 1).apply()
            return true
        }
        val currentBonus = getRewardedBonusCount(featureId)
        val maxBonus = FeatureLimitConfig.getLimit(FeatureId.REWARDED_AD_BONUS, UserTier.FREE)
        if (currentBonus < maxBonus) {
            prefs.edit().putInt("reward_bonus_${featureId.name}", currentBonus + 1).apply()
            return true
        }
        return false
    }
}
