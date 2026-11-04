package com.herwin.netez.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.R
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.components.ToolItem
import com.herwin.netez.ui.viewmodels.SecurityViewModel

@Composable
fun SecurityScreen(
    onNavigateToSecurityIntelligence: () -> Unit = {},
    onNavigateToLeakCheck: () -> Unit = {},
    onNavigateToPhishingCheck: () -> Unit = {},
    onNavigateToPasswordStrength: () -> Unit = {},
    onNavigateToPasswordManager: () -> Unit = {},
    onNavigateToBlacklist: () -> Unit = {},
    onNavigateToSslReminder: () -> Unit = {},
    onNavigateToAppPermissionScanner: () -> Unit = {},
    onNavigateToWifiSecurityAudit: () -> Unit = {},
    onNavigateToDeviceSecurityCheck: () -> Unit = {},
    onNavigateToDnsLeakTest: () -> Unit = {},
    onNavigateToSecurityToolsList: () -> Unit = {},
    viewModel: SecurityViewModel = viewModel()
) {
    val stats by viewModel.securityStats.collectAsState()
    val totalPasswords by viewModel.totalPasswords.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Summary Card
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Security Status Overview",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = AppIcons.verifiedUser(),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "System Protection Active",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (stats.breaches > 0) {
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = AppIcons.error(),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "${stats.breaches} breaches found!",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    // WiFi status
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = AppIcons.wifi(),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "WiFi: Protected Network",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                    // Device status
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val deviceIcon = if (stats.deviceScore >= 75 || stats.deviceScore == -1) AppIcons.shieldCheck() else AppIcons.error()
                        val deviceColor = if (stats.deviceScore >= 75) MaterialTheme.colorScheme.secondary else if (stats.deviceScore >= 40) MaterialTheme.colorScheme.tertiary else if (stats.deviceScore == -1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        val deviceText = when {
                            stats.deviceScore == -1 -> "Device: Analysis ready"
                            stats.deviceCritical > 0 -> "Device: Critical ${stats.deviceCritical} 🔴"
                            stats.deviceWarning > 0 -> "Device: ${stats.deviceWarning} Warnings 🟡"
                            else -> "Device: Secure ${stats.deviceScore}/100 🟢"
                        }
                        Icon(
                            painter = deviceIcon,
                            contentDescription = null,
                            tint = deviceColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = deviceText,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                    // DNS Leak status
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val dnsColor = if (stats.dnsLeaks > 0) MaterialTheme.colorScheme.error else if (stats.dnsLeaks == -1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        val dnsText = when {
                            stats.dnsLeaks == -1 -> "DNS: Ready to test"
                            stats.dnsLeaks > 0 -> "DNS: Leaking 🔴"
                            else -> "DNS: No Leak 🟢"
                        }
                        Icon(
                            painter = AppIcons.public(),
                            contentDescription = null,
                            tint = dnsColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = dnsText,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Featured Item: Security Intelligence (Unified Assessment)
        item {
            ToolItem(
                title = stringResource(R.string.security_intelligence_title),
                subtitle = stringResource(R.string.security_intelligence_subtitle),
                painter = AppIcons.security(),
                onClick = onNavigateToSecurityIntelligence
            )
        }

        // Section Header
        item {
            Text(
                text = "Security Tools & Scanners",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // 1. App Permission Scanner
        item {
            ToolItem(
                title = stringResource(R.string.security_app_scanner_title),
                subtitle = stringResource(R.string.security_app_scanner_desc),
                painter = AppIcons.security(),
                onClick = onNavigateToAppPermissionScanner
            )
        }

        // 2. Blacklist Check
        item {
            ToolItem(
                title = stringResource(R.string.diag_blacklist),
                subtitle = stringResource(R.string.blacklist_check_desc),
                painter = AppIcons.security(),
                onClick = onNavigateToBlacklist
            )
        }

        // 3. Device Security Check
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

        // 4. DNS Leak Test
        item {
            ToolItem(
                title = stringResource(R.string.dns_leak_title),
                subtitle = stringResource(R.string.dns_leak_subtitle),
                painter = AppIcons.dns(),
                onClick = onNavigateToDnsLeakTest
            )
        }

        // 5. Leak Check
        item {
            ToolItem(
                title = stringResource(R.string.security_leak_check),
                subtitle = "Leak Check: ${stats.totalLeaks} checked • ${stats.breaches} breached",
                painter = AppIcons.shield(),
                onClick = onNavigateToLeakCheck
            )
        }

        // 6. Password Manager
        item {
            ToolItem(
                title = stringResource(R.string.security_pass_manager_title),
                subtitle = stringResource(R.string.pass_stat_saved, totalPasswords),
                painter = AppIcons.lock(),
                onClick = onNavigateToPasswordManager
            )
        }

        // 7. Password Strength
        item {
            ToolItem(
                title = stringResource(R.string.security_pass_strength_title),
                subtitle = stringResource(R.string.security_pass_strength_desc),
                painter = AppIcons.key(),
                onClick = onNavigateToPasswordStrength
            )
        }

        // 8. Phishing Check
        item {
            ToolItem(
                title = stringResource(R.string.security_phishing_check),
                subtitle = "Powered by URLhaus 200k+ • OpenPhish • PhishStats",
                painter = AppIcons.public(),
                onClick = onNavigateToPhishingCheck
            )
        }

        // 9. Security Tools
        item {
            ToolItem(
                title = stringResource(R.string.security_tools_section),
                subtitle = stringResource(R.string.security_tools_desc),
                painter = AppIcons.build(),
                onClick = onNavigateToSecurityToolsList
            )
        }

        // 10. SSL/TLS Monitor
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

        // 11. WiFi Security Audit
        item {
            ToolItem(
                title = stringResource(R.string.security_wifi_audit_title),
                subtitle = stringResource(R.string.security_wifi_audit_desc),
                painter = AppIcons.wifiShield(),
                onClick = onNavigateToWifiSecurityAudit
            )
        }
    }
}
