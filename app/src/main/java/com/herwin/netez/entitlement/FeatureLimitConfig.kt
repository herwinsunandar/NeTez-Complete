package com.herwin.netez.entitlement

object FeatureLimitConfig {

    // GLOBAL ROLLBACK KILL-SWITCH: Set to false if rollback is triggered
    var IS_ENTITLEMENT_ENABLED: Boolean = true

    fun getLimit(featureId: FeatureId, tier: UserTier): Int {
        if (!IS_ENTITLEMENT_ENABLED) return Int.MAX_VALUE
        return when (featureId) {
            FeatureId.MULTI_PING_NODES -> if (tier == UserTier.PRO) 50 else 3
            FeatureId.STABILITY_TEST_DAILY -> if (tier == UserTier.PRO) 999 else 3
            FeatureId.SECURITY_INTELLIGENCE_DAILY -> if (tier == UserTier.PRO) 999 else 3
            FeatureId.LEAK_CHECK_DAILY -> if (tier == UserTier.PRO) 999 else 3
            FeatureId.PHISHING_CHECK_DAILY -> if (tier == UserTier.PRO) 999 else 3
            FeatureId.BLACKLIST_CHECK_DAILY -> if (tier == UserTier.PRO) 999 else 3
            FeatureId.CREDENTIAL_VAULT_MAX -> if (tier == UserTier.PRO) 9999 else 10
            FeatureId.SAVED_SSH_SERVERS_MAX -> if (tier == UserTier.PRO) 999 else 3
            FeatureId.MONITORING_TARGETS_MAX -> if (tier == UserTier.PRO) 50 else 3
            FeatureId.MONITORING_MIN_INTERVAL -> if (tier == UserTier.PRO) 1 else 5
            FeatureId.SSL_REMINDER_DOMAINS_MAX -> if (tier == UserTier.PRO) 999 else 4
            FeatureId.REWARDED_AD_BONUS -> 3 // Max 3 rewarded bonus grants per day
            else -> Int.MAX_VALUE
        }
    }

    fun isProOnly(featureId: FeatureId): Boolean {
        if (!IS_ENTITLEMENT_ENABLED) return false
        return when (featureId) {
            FeatureId.REPORT_EXPORT,
            FeatureId.CREDENTIAL_TYPE_ADVANCED,
            FeatureId.BULK_LEAK_CHECK_CSV,
            FeatureId.ADVANCED_SECURITY_ANALYSIS -> true
            else -> false
        }
    }
}
