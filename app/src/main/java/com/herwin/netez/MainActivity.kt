package com.herwin.netez

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.ui.NeTezApp
import com.herwin.netez.ui.theme.NeTezTheme
import com.herwin.netez.ui.viewmodels.SettingsViewModel
import com.herwin.netez.utils.LocaleUtils
import com.herwin.netez.workers.SslReminderWorker
import com.herwin.netez.workers.LeakCheckWorker
import com.herwin.netez.workers.PhishingFeedWorker

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Schedule Workers safely
        try {
            SslReminderWorker.schedule(this)
            LeakCheckWorker.schedule(this)
            PhishingFeedWorker.schedule(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val themeMode by settingsViewModel.themeMode.collectAsState()
            val language by settingsViewModel.language.collectAsState()

            val isSystemInDark = isSystemInDarkTheme()
            val (darkTheme, dynamicColor) = when (themeMode) {
                "dynamic", "light" -> Pair(isSystemInDark, true)
                "dark" -> Pair(true, false)
                else -> Pair(isSystemInDark, false)
            }

            LaunchedEffect(language) {
                LocaleUtils.setAppLocale(language)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val launcher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) {}
                LaunchedEffect(Unit) {
                    launcher.launch(arrayOf(
                        Manifest.permission.POST_NOTIFICATIONS,
                        Manifest.permission.NEARBY_WIFI_DEVICES
                    ))
                }
            }

            NeTezTheme(darkTheme = darkTheme, dynamicColor = dynamicColor) {
                NeTezApp()
            }
        }
    }
}
