package com.herwin.netez.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.R
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.components.ToolItem
import com.herwin.netez.ui.theme.PremiumNavy
import com.herwin.netez.ui.theme.SoftDarkBg
import com.herwin.netez.ui.viewmodels.SecurityViewModel

@Composable
fun SecurityScreen(
    onNavigateToLeakCheck: () -> Unit,
    onNavigateToPhishingCheck: () -> Unit,
    onNavigateToPasswordStrength: () -> Unit,
    onNavigateToPasswordManager: () -> Unit,
    onNavigateToBlacklist: () -> Unit,
    onNavigateToSslReminder: () -> Unit,
    onNavigateToAppPermissionScanner: () -> Unit,
    onNavigateToWifiSecurityAudit: () -> Unit,
    onNavigateToDeviceSecurityCheck: () -> Unit,
    onNavigateToDnsLeakTest: () -> Unit,
    viewModel: SecurityViewModel = viewModel()
) {
    val stats by viewModel.securityStats.collectAsState()
    val totalPasswords by viewModel.totalPasswords.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PremiumNavy),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Summary Card
        item {
            Surface(
                color = SoftDarkBg,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Security Status", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(AppIcons.verifiedUser(), null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("System is protected", color = Color.White, fontSize = 14.sp)
                    }
                    if (stats.breaches > 0) {
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(AppIcons.error(), null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("${stats.breaches} breaches found!", color = Color(0xFFEF4444), fontSize = 13.sp)
                        }
                    }
                    // WiFi status in summary
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(AppIcons.wifi(), null, tint = Color(0xFF6C8CFF), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(12.dp))
                        // We don't have real-time wifi issues here yet, just placeholder or use last scan
                        Text("WiFi: Protected", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                    }
                    // Device status in summary
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val deviceIcon = if (stats.deviceScore >= 75 || stats.deviceScore == -1) AppIcons.shieldCheck() else AppIcons.error()
                        val deviceColor = if (stats.deviceScore >= 75) Color(0xFF10B981) else if (stats.deviceScore >= 40) Color(0xFFFBBF24) else if (stats.deviceScore == -1) Color(0xFF6C8CFF) else Color(0xFFEF4444)
                        val deviceText = when {
                            stats.deviceScore == -1 -> "Device: Analysis ready"
                            stats.deviceCritical > 0 -> "Device: Critical ${stats.deviceCritical} 🔴"
                            stats.deviceWarning > 0 -> "Device: ${stats.deviceWarning} Warnings 🟡"
                            else -> "Device: Secure ${stats.deviceScore}/100 🟢"
                        }
                        Icon(deviceIcon, null, tint = deviceColor, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(deviceText, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                    }
                    // DNS Leak status in summary
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(AppIcons.public(), null, tint = if (stats.dnsLeaks > 0) Color(0xFFFF5252) else if (stats.dnsLeaks == -1) Color(0xFF6C8CFF) else Color(0xFF10B981), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(12.dp))
                        val dnsText = when {
                            stats.dnsLeaks == -1 -> "DNS: Ready to test"
                            stats.dnsLeaks > 0 -> "DNS: Leaking 🔴"
                            else -> "DNS: No Leak 🟢"
                        }
                        Text(dnsText, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                    }
                }
            }
        }

        // 1. Security Tools
        item {
            Text(
                "Security Tools",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            val deviceSubtitle = when {
                stats.deviceScore == -1 -> stringResource(R.string.device_check_not_scanned)
                stats.deviceCritical > 0 -> "${stats.deviceCritical} Critical 🔴"
                stats.deviceWarning > 0 -> "${stats.deviceWarning} Warnings 🟡"
                else -> "Secure ${stats.deviceScore}/100 🟢"
            }
            ToolItem(
                title = stringResource(R.string.device_check_title),
                subtitle = deviceSubtitle,
                painter = AppIcons.shieldCheck(),
                onClick = onNavigateToDeviceSecurityCheck
            )
        }

        item {
            ToolItem(
                title = stringResource(R.string.security_wifi_audit_title),
                subtitle = stringResource(R.string.security_wifi_audit_desc),
                painter = AppIcons.wifiShield(),
                onClick = onNavigateToWifiSecurityAudit
            )
        }

        item {
            ToolItem(
                title = stringResource(R.string.dns_leak_title),
                subtitle = stringResource(R.string.dns_leak_subtitle),
                painter = AppIcons.dns(),
                onClick = onNavigateToDnsLeakTest
            )
        }

        item {
            ToolItem(
                title = stringResource(R.string.security_leak_check),
                subtitle = "Leak Check: ${stats.totalLeaks} checked • ${stats.breaches} breached",
                painter = AppIcons.shield(),
                onClick = onNavigateToLeakCheck
            )
        }

        item {
            ToolItem(
                title = stringResource(R.string.security_pass_strength_title),
                subtitle = stringResource(R.string.security_pass_strength_desc),
                painter = AppIcons.key(),
                onClick = onNavigateToPasswordStrength
            )
        }

        item {
            ToolItem(
                title = stringResource(R.string.security_pass_manager_title),
                subtitle = stringResource(R.string.pass_stat_saved, totalPasswords),
                painter = AppIcons.lock(),
                onClick = onNavigateToPasswordManager
            )
        }

        item {
            ToolItem(
                title = stringResource(R.string.diag_blacklist),
                subtitle = stringResource(R.string.blacklist_check_desc),
                painter = AppIcons.security(),
                onClick = onNavigateToBlacklist
            )
        }

        item {
            val sslSubtitle = if (stats.expiredSsl > 0) {
                stringResource(R.string.ssl_stat_expired_warning, stats.expiredSsl, 5)
            } else {
                stringResource(R.string.ssl_stat_monitoring, stats.totalSsl, stats.expiringSsl)
            }
            ToolItem(
                title = stringResource(R.string.security_ssl_reminder),
                subtitle = sslSubtitle,
                painter = AppIcons.timer(),
                onClick = onNavigateToSslReminder
            )
        }

        item {
            ToolItem(
                title = stringResource(R.string.security_phishing_check),
                subtitle = "Powered by URLhaus 200k+ • OpenPhish • PhishStats",
                painter = AppIcons.public(),
                onClick = onNavigateToPhishingCheck
            )
        }

        item {
            ToolItem(
                title = stringResource(R.string.security_app_scanner_title),
                subtitle = stringResource(R.string.security_app_scanner_desc),
                painter = AppIcons.security(),
                onClick = onNavigateToAppPermissionScanner
            )
        }
    }
}
