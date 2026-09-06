package com.herwin.netez.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecurityPreferences(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "netez_security_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var pin: String?
        get() = prefs.getString("security_pin", null)
        set(value) = prefs.edit().putString("security_pin", value).apply()

    var isBiometricEnabled: Boolean
        get() = prefs.getBoolean("biometric_enabled", true)
        set(value) = prefs.edit().putBoolean("biometric_enabled", value).apply()

    var autoLockDelay: Long
        get() = prefs.getLong("auto_lock_delay", 30000L) // Default 30s
        set(value) = prefs.edit().putLong("auto_lock_delay", value).apply()

    var clipboardClearDelay: Long
        get() = prefs.getLong("clipboard_clear_delay", 30000L) // Default 30s
        set(value) = prefs.edit().putLong("clipboard_clear_delay", value).apply()
        
    var lastAuthTime: Long
        get() = prefs.getLong("last_auth_time", 0L)
        set(value) = prefs.edit().putLong("last_auth_time", value).apply()

    var isSslReminderEnabled: Boolean
        get() = prefs.getBoolean("ssl_reminder_enabled", true)
        set(value) = prefs.edit().putBoolean("ssl_reminder_enabled", value).apply()

    var sslCheckTime: String
        get() = prefs.getString("ssl_check_time", "08:00") ?: "08:00"
        set(value) = prefs.edit().putString("ssl_check_time", value).apply()

    var defaultReminderDays: String
        get() = prefs.getString("default_reminder_days", "1,3,7,14,30") ?: "1,3,7,14,30"
        set(value) = prefs.edit().putString("default_reminder_days", value).apply()

    var urlHausLastUpdate: Long
        get() = prefs.getLong("urlhaus_last_update", 0L)
        set(value) = prefs.edit().putLong("urlhaus_last_update", value).apply()

    var isOnlineCheckEnabled: Boolean
        get() = prefs.getBoolean("online_check_enabled", true)
        set(value) = prefs.edit().putBoolean("online_check_enabled", value).apply()

    fun isAutoLockNeeded(): Boolean {
        val currentTime = System.currentTimeMillis()
        return currentTime - lastAuthTime > autoLockDelay
    }
}
