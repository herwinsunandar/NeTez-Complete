package com.herwin.netez.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.data.SettingsManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsManager = SettingsManager(application)

    val themeMode: StateFlow<String> = settingsManager.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    val language: StateFlow<String> = settingsManager.language
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    val abuseIpDbKey: StateFlow<String> = settingsManager.abuseIpDbKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val shodanKey: StateFlow<String> = settingsManager.shodanKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            settingsManager.setThemeMode(mode)
        }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            settingsManager.setLanguage(lang)
        }
    }

    fun setAbuseIpDbKey(key: String) {
        viewModelScope.launch {
            settingsManager.setAbuseIpDbKey(key)
        }
    }

    fun setShodanKey(key: String) {
        viewModelScope.launch {
            settingsManager.setShodanKey(key)
        }
    }
}
