package com.herwin.netez.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.R
import com.herwin.netez.ui.viewmodels.SettingsViewModel

@Composable
fun SettingsScreen(
    onNavigateToHistory: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val language by viewModel.language.collectAsState()
    
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            SettingsCategory(stringResource(R.string.nav_report)) {
                SettingsItem(
                    title = stringResource(R.string.nav_report),
                    subtitle = stringResource(R.string.set_history_desc),
                    icon = Icons.Default.History,
                    onClick = onNavigateToHistory
                )
            }
        }

        item {
            SettingsCategory(stringResource(R.string.set_appearance)) {
                SettingsItem(
                    title = stringResource(R.string.set_theme),
                    subtitle = themeMode.replaceFirstChar { it.uppercase() },
                    icon = Icons.Default.Palette,
                    onClick = { showThemeDialog = true }
                )
                SettingsItem(
                    title = stringResource(R.string.set_language),
                    subtitle = when(language) {
                        "en" -> "English"
                        "in" -> "Indonesia"
                        else -> "System Default"
                    },
                    icon = Icons.Default.Language,
                    onClick = { showLanguageDialog = true }
                )
            }
        }

        item {
            SettingsCategory(stringResource(R.string.set_about)) {
                SettingsItem(
                    title = "Version",
                    subtitle = "1.0.0 (Release Candidate)",
                    icon = Icons.Default.Info,
                    onClick = { /* TODO */ }
                )
            }
        }
        
        item {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Made with ❤️ for Engineers",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.set_theme)) },
            text = {
                Column {
                    ThemeOption("System Default", "system", themeMode) { viewModel.setThemeMode(it); showThemeDialog = false }
                    ThemeOption("Light", "light", themeMode) { viewModel.setThemeMode(it); showThemeDialog = false }
                    ThemeOption("Dark", "dark", themeMode) { viewModel.setThemeMode(it); showThemeDialog = false }
                }
            },
            confirmButton = { TextButton(onClick = { showThemeDialog = false }) { Text("CANCEL") } }
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.set_language)) },
            text = {
                Column {
                    LanguageOption("System Default", "system", language) { viewModel.setLanguage(it); showLanguageDialog = false }
                    LanguageOption("English", "en", language) { viewModel.setLanguage(it); showLanguageDialog = false }
                    LanguageOption("Indonesia", "in", language) { viewModel.setLanguage(it); showLanguageDialog = false }
                }
            },
            confirmButton = { TextButton(onClick = { showLanguageDialog = false }) { Text("CANCEL") } }
        )
    }
}

@Composable
fun ThemeOption(label: String, value: String, current: String, onClick: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick(value) }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = current == value, onClick = { onClick(value) })
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

@Composable
fun LanguageOption(label: String, value: String, current: String, onClick: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick(value) }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = current == value, onClick = { onClick(value) })
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

@Composable
fun SettingsCategory(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp)
        )
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingsItem(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.size(20.dp)
        )
    }
}
