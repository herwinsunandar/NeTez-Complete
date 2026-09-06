package com.herwin.netez.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {
    companion object {
        val THEME_KEY = stringPreferencesKey("theme_mode") // "light", "dark", "system"
        val LANGUAGE_KEY = stringPreferencesKey("language") // "en", "in", "system"
        val ABUSE_IPDB_KEY = stringPreferencesKey("abuse_ipdb_key")
        val SHODAN_KEY = stringPreferencesKey("shodan_key")
    }

    val themeMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[THEME_KEY] ?: "system"
    }

    val language: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[LANGUAGE_KEY] ?: "system"
    }

    val abuseIpDbKey: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[ABUSE_IPDB_KEY] ?: ""
    }

    val shodanKey: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SHODAN_KEY] ?: ""
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = mode
        }
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = lang
        }
    }

    suspend fun setAbuseIpDbKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[ABUSE_IPDB_KEY] = key
        }
    }

    suspend fun setShodanKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[SHODAN_KEY] = key
        }
    }
}
