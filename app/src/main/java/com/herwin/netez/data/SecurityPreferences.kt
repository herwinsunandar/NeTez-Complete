package com.herwin.netez.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecurityPreferences(context: Context) {

    private val prefs: SharedPreferences = createEncryptedPrefs(context)

    private fun createEncryptedPrefs(context: Context): SharedPreferences {
        val appContext = context.applicationContext
        return try {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                appContext,
                "netez_security_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e("SecurityPreferences", "EncryptedSharedPreferences initialization failed, resetting file", e)
            try {
                appContext.deleteSharedPreferences("netez_security_prefs")
                val masterKey = MasterKey.Builder(appContext)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                EncryptedSharedPreferences.create(
                    appContext,
                    "netez_security_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e2: Exception) {
                Log.e("SecurityPreferences", "Fallback to standard SharedPreferences", e2)
                appContext.getSharedPreferences("netez_security_prefs_fallback", Context.MODE_PRIVATE)
            }
        }
    }

    var autoLockDelay: Long
        get() = try { prefs.getLong("auto_lock_delay", 30000L) } catch (_: Exception) { 30000L }
        set(value) { try { prefs.edit().putLong("auto_lock_delay", value).apply() } catch (_: Exception) {} }

    var clipboardClearDelay: Long
        get() = try { prefs.getLong("clipboard_clear_delay", 30000L) } catch (_: Exception) { 30000L }
        set(value) { try { prefs.edit().putLong("clipboard_clear_delay", value).apply() } catch (_: Exception) {} }
        
    var lastAuthTime: Long
        get() = try { prefs.getLong("last_auth_time", 0L) } catch (_: Exception) { 0L }
        set(value) { try { prefs.edit().putLong("last_auth_time", value).apply() } catch (_: Exception) {} }

    var isSslReminderEnabled: Boolean
        get() = try { prefs.getBoolean("ssl_reminder_enabled", true) } catch (_: Exception) { true }
        set(value) { try { prefs.edit().putBoolean("ssl_reminder_enabled", value).apply() } catch (_: Exception) {} }

    var sslCheckTime: String
        get() = try { prefs.getString("ssl_check_time", "08:00") ?: "08:00" } catch (_: Exception) { "08:00" }
        set(value) { try { prefs.edit().putString("ssl_check_time", value).apply() } catch (_: Exception) {} }

    var defaultReminderDays: String
        get() = try { prefs.getString("default_reminder_days", "1,3,7,14,30") ?: "1,3,7,14,30" } catch (_: Exception) { "1,3,7,14,30" }
        set(value) { try { prefs.edit().putString("default_reminder_days", value).apply() } catch (_: Exception) {} }

    var urlHausLastUpdate: Long
        get() = try { prefs.getLong("urlhaus_last_update", 0L) } catch (_: Exception) { 0L }
        set(value) { try { prefs.edit().putLong("urlhaus_last_update", value).apply() } catch (_: Exception) {} }

    var isOnlineCheckEnabled: Boolean
        get() = try { prefs.getBoolean("online_check_enabled", true) } catch (_: Exception) { true }
        set(value) { try { prefs.edit().putBoolean("online_check_enabled", value).apply() } catch (_: Exception) {} }

    fun isAutoLockNeeded(): Boolean {
        val currentTime = System.currentTimeMillis()
        return currentTime - lastAuthTime > autoLockDelay
    }
}
