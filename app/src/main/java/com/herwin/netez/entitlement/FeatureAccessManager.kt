package com.herwin.netez.entitlement

import android.content.Context

class FeatureAccessManager private constructor(context: Context) {

    private val subManager = SubscriptionManager.getInstance(context)
    private val quotaManager = FeatureQuotaManager(context)

    fun getCurrentTier(): UserTier {
        if (!FeatureLimitConfig.IS_ENTITLEMENT_ENABLED) return UserTier.PRO
        return subManager.userTier.value
    }

    fun isPro(): Boolean {
        if (!FeatureLimitConfig.IS_ENTITLEMENT_ENABLED) return true
        return subManager.isPro()
    }

    fun checkAccess(featureId: FeatureId, currentItemCount: Int = 0): FeatureAccessState {
        if (!FeatureLimitConfig.IS_ENTITLEMENT_ENABLED) return FeatureAccessState.AVAILABLE

        val tier = getCurrentTier()
        if (tier == UserTier.PRO) return FeatureAccessState.AVAILABLE

        // Check if feature is PRO_ONLY
        if (FeatureLimitConfig.isProOnly(featureId)) {
            return FeatureAccessState.PRO_ONLY
        }

        // Check Capacity/Item Limit (e.g. Credential Vault, Saved SSH, Monitoring, SSL Reminder, Multi-Ping)
        val maxLimit = FeatureLimitConfig.getLimit(featureId, tier)
        if (currentItemCount >= maxLimit) {
            return FeatureAccessState.LIMIT_REACHED
        }

        // Check Daily Usage Limit (e.g. Stability Test, Security Intelligence, Leak Check, Phishing, Blacklist)
        val dailyUsage = quotaManager.getUsageCount(featureId)
        val bonus = quotaManager.getRewardedBonusCount(featureId)
        val totalAllowed = maxLimit + bonus

        return if (dailyUsage >= totalAllowed) {
            FeatureAccessState.LIMIT_REACHED
        } else if (maxLimit < Int.MAX_VALUE) {
            FeatureAccessState.LIMITED
        } else {
            FeatureAccessState.AVAILABLE
        }
    }

    fun getRemainingQuota(featureId: FeatureId, currentItemCount: Int = 0): Int {
        if (!FeatureLimitConfig.IS_ENTITLEMENT_ENABLED) return Int.MAX_VALUE

        val tier = getCurrentTier()
        if (tier == UserTier.PRO) return Int.MAX_VALUE

        val maxLimit = FeatureLimitConfig.getLimit(featureId, tier)
        if (maxLimit == Int.MAX_VALUE) return Int.MAX_VALUE

        val usage = quotaManager.getUsageCount(featureId)
        val bonus = quotaManager.getRewardedBonusCount(featureId)
        val totalAllowed = maxLimit + bonus

        return (totalAllowed - usage).coerceAtLeast(0)
    }

    fun consumeQuota(featureId: FeatureId): Boolean {
        if (!FeatureLimitConfig.IS_ENTITLEMENT_ENABLED || isPro()) return true
        val state = checkAccess(featureId)
        if (state == FeatureAccessState.AVAILABLE || state == FeatureAccessState.LIMITED) {
            quotaManager.incrementUsage(featureId)
            return true
        }
        return false
    }

    fun grantRewardedBonus(featureId: FeatureId): Boolean {
        if (!FeatureLimitConfig.IS_ENTITLEMENT_ENABLED) return true
        return quotaManager.grantRewardedBonus(featureId)
    }

    fun trackAndCheckGlobalToolUsage(): Boolean {
        if (!FeatureLimitConfig.IS_ENTITLEMENT_ENABLED || isPro()) return false
        val globalCount = quotaManager.incrementUsage(FeatureId.GLOBAL_TOOL_USAGE)
        return globalCount > 0 && (globalCount % 3 == 0)
    }

    companion object {
        @Volatile
        private var instance: FeatureAccessManager? = null

        fun getInstance(context: Context): FeatureAccessManager {
            return instance ?: synchronized(this) {
                instance ?: FeatureAccessManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
