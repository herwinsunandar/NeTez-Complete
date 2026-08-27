package com.herwin.netez.utils

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LocaleUtils {
    fun setAppLocale(languageCode: String) {
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        val targetLocales = if (languageCode == "system") {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(languageCode)
        }
        
        // Only set if different to avoid recreation loops
        if (currentLocales != targetLocales) {
            AppCompatDelegate.setApplicationLocales(targetLocales)
        }
    }
}
