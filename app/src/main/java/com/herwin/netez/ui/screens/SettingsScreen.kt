package com.herwin.netez.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.herwin.netez.R
import com.herwin.netez.data.SecurityPreferences
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.theme.SoftDarkBg
import com.herwin.netez.ui.theme.TextGray
import com.herwin.netez.ui.viewmodels.SettingsViewModel
import com.herwin.netez.ui.viewmodels.StabilityTestViewModel
import com.herwin.netez.worker.OUIUpdateWorker
import com.herwin.netez.workers.SslReminderWorker
import com.herwin.netez.workers.UrlHausFeedWorker
import java.util.Locale

@Composable
fun SettingsScreen(
    onNavigateToHistory: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val language by viewModel.language.collectAsState()
    val abuseIpDbKey by viewModel.abuseIpDbKey.collectAsState()
    val shodanKey by viewModel.shodanKey.collectAsState()
    val context = LocalContext.current
    val securityPrefs = remember { SecurityPreferences(context) }
    val updateStartedMsg = stringResource(R.string.set_vendor_update_started)
    
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showLegalRevokeDialog by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var selectedApiKeyType by remember { mutableStateOf("") } // "AbuseIPDB" or "Shodan"
    var showPinDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var showReminderDaysDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            SettingsCategory(stringResource(R.string.set_security)) {
                var bioEnabled by remember { mutableStateOf(securityPrefs.isBiometricEnabled) }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.set_bio_auth), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(stringResource(R.string.set_bio_auth_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = bioEnabled,
                        onCheckedChange = {
                            bioEnabled = it
                            securityPrefs.isBiometricEnabled = it
                        },
                        modifier = Modifier.scale(0.8f)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                SettingsItem(
                    title = stringResource(R.string.set_pin_title),
                    subtitle = if (securityPrefs.pin == null) stringResource(R.string.set_pin_not_set) else stringResource(R.string.set_pin_active),
                    icon = AppIcons.lock(),
                    onClick = { showPinDialog = true }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                var autoLock by remember { mutableStateOf(securityPrefs.autoLockDelay == 30000L) }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.set_autolock), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(stringResource(R.string.set_autolock_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = autoLock,
                        onCheckedChange = {
                            autoLock = it
                            securityPrefs.autoLockDelay = if (it) 30000L else 3600000L // 30s or 1h
                        },
                        modifier = Modifier.scale(0.8f)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                var clipClear by remember { mutableStateOf(securityPrefs.clipboardClearDelay == 30000L) }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.set_clip_clear), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(stringResource(R.string.set_clip_clear_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = clipClear,
                        onCheckedChange = {
                            clipClear = it
                            securityPrefs.clipboardClearDelay = if (it) 30000L else 0L // 30s or Never
                        },
                        modifier = Modifier.scale(0.8f)
                    )
                }
            }
        }
        item {
            SettingsCategory(stringResource(R.string.set_integrations)) {
                SettingsItem(
                    title = stringResource(R.string.set_abuseipdb_key),
                    subtitle = if (abuseIpDbKey.isEmpty()) stringResource(R.string.set_pin_not_set) else "••••••••••••••••",
                    icon = AppIcons.info(),
                    onClick = { 
                        selectedApiKeyType = "AbuseIPDB"
                        showApiKeyDialog = true 
                    }
                )
                SettingsItem(
                    title = stringResource(R.string.set_shodan_key),
                    subtitle = if (shodanKey.isEmpty()) stringResource(R.string.set_pin_not_set) else "••••••••••••••••",
                    icon = AppIcons.info(),
                    onClick = { 
                        selectedApiKeyType = "Shodan"
                        showApiKeyDialog = true 
                    }
                )
            }
        }

        item {
            SettingsCategory("Security Discovery Feeds") {
                var onlineCheck by remember { mutableStateOf(securityPrefs.isOnlineCheckEnabled) }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Allow Online Check", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Send URLs to URLhaus & PhishStats (Privacy: URL only)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = onlineCheck,
                        onCheckedChange = {
                            onlineCheck = it
                            securityPrefs.isOnlineCheckEnabled = it
                        },
                        modifier = Modifier.scale(0.8f)
                    )
                }
                
                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                val lastUpdateText = if (securityPrefs.urlHausLastUpdate == 0L) "Never" else {
                    val diff = System.currentTimeMillis() - securityPrefs.urlHausLastUpdate
                    when {
                        diff < 3600000 -> "${diff / 60000}m ago"
                        diff < 86400000 -> "${diff / 3600000}h ago"
                        else -> "${diff / 86400000}d ago"
                    }
                }
                
                SettingsItem(
                    title = "URLhaus Feed",
                    subtitle = "Last update: $lastUpdateText • 30k URLs\nNext update auto in 12h",
                    icon = AppIcons.refresh(),
                    onClick = {
                        UrlHausFeedWorker.schedule(context, true)
                        Toast.makeText(context, "Update started", Toast.LENGTH_SHORT).show()
                    }
                )
                
                Text(
                    text = "URLhaus & OpenPhish & PhishStats free no key",
                    modifier = Modifier.padding(20.dp),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        item {
            SettingsCategory(stringResource(R.string.ssl_reminder_settings)) {
                var sslEnabled by remember { mutableStateOf(securityPrefs.isSslReminderEnabled) }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Notification ON/OFF", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Receive alerts for expiring SSL", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = sslEnabled,
                        onCheckedChange = {
                            sslEnabled = it
                            securityPrefs.isSslReminderEnabled = it
                        },
                        modifier = Modifier.scale(0.8f)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                SettingsItem(
                    title = "Check Time",
                    subtitle = securityPrefs.sslCheckTime,
                    icon = AppIcons.timer(),
                    onClick = { showTimePickerDialog = true }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                SettingsItem(
                    title = "Reminder Days",
                    subtitle = securityPrefs.defaultReminderDays,
                    icon = AppIcons.event(),
                    onClick = { showReminderDaysDialog = true }
                )
            }
        }
        item {
            SettingsCategory(stringResource(R.string.set_database)) {
                SettingsItem(
                    title = stringResource(R.string.set_update_vendor),
                    subtitle = stringResource(R.string.set_update_vendor_desc),
                    icon = AppIcons.palette(),
                    onClick = {
                        val request = OneTimeWorkRequestBuilder<OUIUpdateWorker>().build()
                        WorkManager.getInstance(context).enqueue(request)
                        Toast.makeText(context, updateStartedMsg, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        item {
            SettingsCategory(stringResource(R.string.nav_report)) {
                SettingsItem(
                    title = stringResource(R.string.nav_report),
                    subtitle = stringResource(R.string.set_history_desc),
                    icon = AppIcons.history(),
                    onClick = onNavigateToHistory
                )
            }
        }

        item {
            SettingsCategory(stringResource(R.string.set_appearance)) {
                SettingsItem(
                    title = stringResource(R.string.set_theme),
                    subtitle = when(themeMode) {
                        "light" -> stringResource(R.string.set_theme_light)
                        "dark" -> stringResource(R.string.set_theme_dark)
                        else -> stringResource(R.string.set_theme_system)
                    },
                    icon = AppIcons.palette(),
                    onClick = { showThemeDialog = true }
                )
                SettingsItem(
                    title = stringResource(R.string.set_language),
                    subtitle = when(language) {
                        "en" -> "English"
                        "in" -> "Indonesia"
                        else -> "System Default"
                    },
                    icon = AppIcons.language(),
                    onClick = { showLanguageDialog = true }
                )
            }
        }

        item {
            SettingsCategory(stringResource(R.string.set_about)) {
                SettingsItem(
                    title = stringResource(R.string.set_legal_notice),
                    subtitle = stringResource(R.string.set_legal_notice_desc),
                    icon = AppIcons.info(),
                    onClick = { showLegalRevokeDialog = true }
                )
                SettingsItem(
                    title = "Version",
                    subtitle = "1.0.0 (Release Candidate)",
                    icon = AppIcons.info(),
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
                    ThemeOption(stringResource(R.string.set_theme_system), "system", themeMode) { viewModel.setThemeMode(it); showThemeDialog = false }
                    ThemeOption(stringResource(R.string.set_theme_light), "light", themeMode) { viewModel.setThemeMode(it); showThemeDialog = false }
                    ThemeOption(stringResource(R.string.set_theme_dark), "dark", themeMode) { viewModel.setThemeMode(it); showThemeDialog = false }
                }
            },
            confirmButton = { TextButton(onClick = { showThemeDialog = false }) { Text(stringResource(R.string.btn_cancel)) } }
        )
    }

    if (showPinDialog) {
        var tempPin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            containerColor = SoftDarkBg,
            title = { Text(stringResource(R.string.set_pin_dialog_title), color = Color.White) },
            text = {
                Column {
                    Text(stringResource(R.string.set_pin_dialog_desc), color = TextGray, fontSize = 12.sp)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = tempPin,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) tempPin = it },
                        label = { Text(stringResource(R.string.set_pin_field)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF6C8CFF),
                            unfocusedBorderColor = TextGray
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        securityPrefs.pin = tempPin
                        showPinDialog = false
                    },
                    enabled = tempPin.length == 6,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C8CFF))
                ) {
                    Text(stringResource(R.string.btn_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text(stringResource(R.string.btn_cancel), color = Color.White)
                }
            }
        )
    }

    if (showTimePickerDialog) {
        val currentTime = securityPrefs.sslCheckTime.split(":")
        var hour by remember { mutableStateOf(currentTime.getOrNull(0)?.toIntOrNull() ?: 8) }
        var minute by remember { mutableStateOf(currentTime.getOrNull(1)?.toIntOrNull() ?: 0) }

        AlertDialog(
            onDismissRequest = { showTimePickerDialog = false },
            containerColor = SoftDarkBg,
            title = { Text("SSL Check Time", color = Color.White) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Select when the daily SSL check should run:", color = TextGray, fontSize = 12.sp)
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NumberPicker(
                            value = hour,
                            onValueChange = { hour = it },
                            range = 0..23,
                            label = "Hour"
                        )
                        Text(" : ", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        NumberPicker(
                            value = minute,
                            onValueChange = { minute = it },
                            range = 0..59,
                            label = "Min"
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val time = String.format(Locale.US, "%02d:%02d", hour, minute)
                    securityPrefs.sslCheckTime = time
                    SslReminderWorker.schedule(context) // Reschedule with new time
                    showTimePickerDialog = false
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C8CFF))) {
                    Text(stringResource(R.string.btn_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePickerDialog = false }) {
                    Text(stringResource(R.string.btn_cancel), color = Color.White)
                }
            }
        )
    }

    if (showReminderDaysDialog) {
        var daysText by remember { mutableStateOf(securityPrefs.defaultReminderDays) }
        AlertDialog(
            onDismissRequest = { showReminderDaysDialog = false },
            containerColor = SoftDarkBg,
            title = { Text("Reminder Days", color = Color.White) },
            text = {
                Column {
                    Text("Enter comma-separated days before expiry to receive alerts (e.g. 1,3,7,14,30):", color = TextGray, fontSize = 12.sp)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = daysText,
                        onValueChange = { daysText = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF6C8CFF),
                            unfocusedBorderColor = TextGray
                        ),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    securityPrefs.defaultReminderDays = daysText
                    showReminderDaysDialog = false
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C8CFF))) {
                    Text(stringResource(R.string.btn_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showReminderDaysDialog = false }) {
                    Text(stringResource(R.string.btn_cancel), color = Color.White)
                }
            }
        )
    }

    if (showApiKeyDialog) {
        var tempKey by remember { mutableStateOf(if (selectedApiKeyType == "AbuseIPDB") abuseIpDbKey else shodanKey) }
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = { Text("$selectedApiKeyType API Key") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tempKey,
                        onValueChange = { tempKey = it },
                        label = { Text(stringResource(R.string.set_api_key_field)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (selectedApiKeyType == "AbuseIPDB") {
                        Text(
                            text = stringResource(R.string.set_get_api_key),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.abuseipdb.com/account/api")))
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (selectedApiKeyType == "AbuseIPDB") viewModel.setAbuseIpDbKey(tempKey)
                    else viewModel.setShodanKey(tempKey)
                    showApiKeyDialog = false
                }) { Text(stringResource(R.string.btn_save)) }
            },
            dismissButton = { TextButton(onClick = { showApiKeyDialog = false }) { Text(stringResource(R.string.btn_cancel)) } }
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
            confirmButton = { TextButton(onClick = { showLanguageDialog = false }) { Text(stringResource(R.string.btn_cancel)) } }
        )
    }

    if (showLegalRevokeDialog) {
        val stabilityViewModel: StabilityTestViewModel = viewModel()
        AlertDialog(
            onDismissRequest = { showLegalRevokeDialog = false },
            containerColor = SoftDarkBg,
            title = { Text(stringResource(R.string.set_legal_revoke_title), color = Color.White) },
            text = {
                Text(
                    text = stringResource(R.string.set_legal_revoke_desc),
                    color = TextGray
                )
            },
            confirmButton = {
                Button(
                    onClick = { 
                        stabilityViewModel.revokeConsent()
                        showLegalRevokeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text(stringResource(R.string.set_btn_revoke))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLegalRevokeDialog = false }) {
                    Text(stringResource(R.string.btn_close), color = Color.White)
                }
            }
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
fun SettingsItem(title: String, subtitle: String, icon: Painter, onClick: () -> Unit) {
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
            AppIcons.chevronRight(),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun NumberPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = TextGray, fontSize = 10.sp)
        IconButton(onClick = { if (value < range.last) onValueChange(value + 1) else onValueChange(range.first) }) {
            Icon(AppIcons.keyboardArrowUp(), null, tint = Color.White)
        }
        Text(
            text = String.format("%02d", value),
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = { if (value > range.first) onValueChange(value - 1) else onValueChange(range.last) }) {
            Icon(AppIcons.keyboardArrowDown(), null, tint = Color.White)
        }
    }
}
