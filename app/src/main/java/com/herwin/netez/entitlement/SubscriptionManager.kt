package com.herwin.netez.entitlement

import android.content.Context
import android.content.SharedPreferences
import com.herwin.netez.data.SecurityPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SubscriptionManager(context: Context) {

    private val securityPrefs = SecurityPreferences(context)
    private val prefs: SharedPreferences = context.getSharedPreferences("netez_sub_prefs", Context.MODE_PRIVATE)

    private val _userTier = MutableStateFlow(getStoredTier())
    val userTier: StateFlow<UserTier> = _userTier.asStateFlow()

    private fun getStoredTier(): UserTier {
        val tierName = prefs.getString("user_tier", UserTier.FREE.name) ?: UserTier.FREE.name
        return try {
            UserTier.valueOf(tierName)
        } catch (e: Exception) {
            UserTier.FREE
        }
    }

    fun isPro(): Boolean = _userTier.value == UserTier.PRO

    fun setProTier(isPro: Boolean) {
        val tier = if (isPro) UserTier.PRO else UserTier.FREE
        _userTier.value = tier
        prefs.edit().putString("user_tier", tier.name).apply()
    }

    fun restorePurchases(): Boolean {
        // Cached or Play Billing purchase restore handler
        val current = getStoredTier()
        _userTier.value = current
        return current == UserTier.PRO
    }

    companion object {
        @Volatile
        private var instance: SubscriptionManager? = null

        fun getInstance(context: Context): SubscriptionManager {
            return instance ?: synchronized(this) {
                instance ?: SubscriptionManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
