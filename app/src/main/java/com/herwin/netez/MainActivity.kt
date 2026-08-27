package com.herwin.netez

import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val themeMode by settingsViewModel.themeMode.collectAsState()
            val language by settingsViewModel.language.collectAsState()

            val darkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            LaunchedEffect(language) {
                LocaleUtils.setAppLocale(language)
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val launcher = rememberLauncherForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
                ) {}
                LaunchedEffect(Unit) {
                    launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            NeTezTheme(darkTheme = darkTheme) {
                NeTezApp()
            }
        }
    }
}
