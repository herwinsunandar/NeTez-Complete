package com.herwin.netez.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.R
import com.herwin.netez.network.WifiAuditResult
import com.herwin.netez.network.WifiCheckAction
import com.herwin.netez.network.WifiCheckItem
import com.herwin.netez.network.WifiCheckStatus
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.theme.PremiumNavy
import com.herwin.netez.ui.theme.SoftDarkBg
import com.herwin.netez.ui.theme.TextGray
import com.herwin.netez.ui.viewmodels.WifiSecurityViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WifiSecurityScreen(
    viewModel: WifiSecurityViewModel = viewModel(),
    onNavigateToLanScanner: () -> Unit,
    onNavigateToDnsLeakTest: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PremiumNavy)
    ) {
        // 1. Header with Connected WiFi
        uiState.auditResult?.let { result ->
            ConnectedWifiCard(result, viewModel, context)
        } ?: run {
            WelcomeWifiHeader()
        }

        // 2. Control Buttons
        ScanControlRow(uiState.isScanning, onStartScan = { viewModel.startAudit() }, onNavigateToLanScanner)

        uiState.auditResult?.let { result ->
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // 3. Score Card
                item {
                    SecurityScoreCard(result)
                }

                // 4. Findings Summary
                item {
                    FindingsSummaryRow(result, viewModel, context, onNavigateToDnsLeakTest)
                }

                // 5. Category Breakdown
                item {
                    CategoryBreakdownList(result.categoryScores)
                }

                // 6. Audit Details List
                item {
                    Text(
                        stringResource(R.string.wifi_audit_details_header),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        color = TextGray,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                }
                items(result.checks) { check ->
                    AuditDetailCard(check, result.gateway ?: "192.168.1.1", onNavigateToLanScanner)
                }

                // 7. Secure WiFi Checklist
                item {
                    SecureWiFiChecklist(result)
                }
            }
        } ?: run {
            Box(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun ConnectedWifiCard(result: WifiAuditResult, viewModel: WifiSecurityViewModel, context: Context) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        color = SoftDarkBg,
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF6C8CFF).copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(AppIcons.wifi(), null, tint = Color(0xFF6C8CFF), modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(result.ssid ?: stringResource(R.string.wifi_audit_unknown_wifi), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(stringResource(R.string.wifi_audit_bssid_label, result.bssid ?: stringResource(R.string.wifi_audit_not_verifiable)), style = MaterialTheme.typography.bodySmall, color = TextGray)
                }
                IconButton(onClick = {
                    result.bssid?.let { viewModel.copyBssidToClipboard(context, it) }
                    Toast.makeText(context, "BSSID Copied", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(AppIcons.contentCopy(), null, tint = Color(0xFF6C8CFF), modifier = Modifier.size(20.dp))
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip(result.security?.substringBefore(" ") ?: stringResource(R.string.wifi_audit_not_verifiable), Color(0xFF6C8CFF))
                InfoChip("${result.frequency ?: 0} MHz", Color(0xFF6C8CFF))
                InfoChip(stringResource(R.string.wifi_audit_ch_label, result.channel ?: 0), Color(0xFF6C8CFF))
                InfoChip("${result.rssi ?: 0} dBm", if ((result.rssi ?: 0) > -67) Color(0xFF10B981) else Color(0xFFFBBF24))
                InfoChip(stringResource(R.string.wifi_audit_gtw_label, result.gateway ?: stringResource(R.string.wifi_audit_not_verifiable)), Color(0xFF6C8CFF))
            }
        }
    }
}

@Composable
fun WelcomeWifiHeader() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(16.dp).height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.size(140.dp).background(Brush.radialGradient(colors = listOf(Color(0xFF6C8CFF).copy(alpha = 0.15f), Color.Transparent)))
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(modifier = Modifier.size(100.dp), color = SoftDarkBg, shape = CircleShape, border = BorderStroke(2.dp, Color(0xFF6C8CFF).copy(alpha = 0.2f))) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(AppIcons.wifiShield(), null, tint = Color(0xFF6C8CFF), modifier = Modifier.size(56.dp))
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.security_wifi_audit_desc), color = TextGray, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun ScanControlRow(isScanning: Boolean, onStartScan: () -> Unit, onNavigateToLanScanner: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onStartScan,
            modifier = Modifier.weight(1f).height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C8CFF)),
            enabled = !isScanning
        ) {
            if (isScanning) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 3.dp)
            } else {
                Icon(AppIcons.scan(), null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.wifi_audit_scan), fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
            }
        }
        
        OutlinedButton(
            onClick = onNavigateToLanScanner,
            modifier = Modifier.weight(1f).height(56.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF6C8CFF).copy(alpha = 0.5f))
        ) {
            Icon(AppIcons.lan(), null, tint = Color(0xFF6C8CFF))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.wifi_audit_scan_lan), color = Color(0xFF6C8CFF), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SecurityScoreCard(result: WifiAuditResult) {
    val scoreColor = when {
        result.securityScore >= 90 -> Color(0xFF10B981)
        result.securityScore >= 75 -> Color(0xFF10B981)
        result.securityScore >= 60 -> Color(0xFFFBBF24)
        result.securityScore >= 40 -> Color(0xFFF97316)
        else -> Color(0xFFEF4444)
    }

    Surface(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        color = SoftDarkBg,
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(stringResource(R.string.wifi_audit_score_title), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(result.securityScore.toString(), color = scoreColor, fontWeight = FontWeight.Black, fontSize = 36.sp)
                        Text(" / 100", color = TextGray, fontSize = 16.sp, modifier = Modifier.padding(bottom = 6.dp))
                    }
                    val gradeRes = when(result.securityGrade) {
                        "EXCELLENT" -> R.string.wifi_audit_grade_excellent
                        "GOOD" -> R.string.wifi_audit_grade_good
                        "FAIR" -> R.string.wifi_audit_grade_fair
                        "WEAK" -> R.string.wifi_audit_grade_weak
                        "CRITICAL" -> R.string.wifi_audit_grade_critical
                        else -> R.string.wifi_audit_grade_fair
                    }
                    Text(stringResource(gradeRes), color = scoreColor, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                }
                
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { result.securityScore / 100f },
                        modifier = Modifier.size(80.dp),
                        color = scoreColor,
                        strokeWidth = 8.dp,
                        trackColor = Color.White.copy(alpha = 0.05f),
                        strokeCap = StrokeCap.Round
                    )
                    Icon(
                        painter = if (result.securityScore >= 75) AppIcons.checkCircle() else AppIcons.warning(),
                        contentDescription = null,
                        tint = scoreColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
            Spacer(Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ScoreIndicator(stringResource(R.string.wifi_audit_safe), "${result.safeCount}", Color(0xFF10B981))
                ScoreIndicator(stringResource(R.string.wifi_audit_suspicious), "${result.warningCount}", Color(0xFFFBBF24))
                ScoreIndicator(stringResource(R.string.wifi_audit_critical_label), "${result.criticalCount}", Color(0xFFEF4444))
            }
        }
    }
}

@Composable
fun ScoreIndicator(label: String, count: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count, color = color, fontWeight = FontWeight.Black, fontSize = 18.sp)
        Text(label, color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun CategoryBreakdownList(categoryScores: Map<String, Int>) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        color = SoftDarkBg,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            categoryScores.forEach { (cat, score) ->
                CategoryRow(cat, score)
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun CategoryRow(category: String, score: Int) {
    val (labelRes, maxWeight) = when(category) {
        "Encryption & Authentication" -> R.string.wifi_audit_cat_encryption to 35
        "WiFi Configuration" -> R.string.wifi_audit_cat_config to 20
        "Access Point Trust" -> R.string.wifi_audit_cat_trust to 15
        "Network Security" -> R.string.wifi_audit_cat_network to 15
        "Privacy" -> R.string.wifi_audit_cat_privacy to 10
        "Network Exposure" -> R.string.wifi_audit_cat_exposure to 5
        else -> R.string.nav_scanner to 10
    }
    
    val percentage = score.toFloat() / maxWeight
    val color = when {
        percentage >= 0.9f -> Color(0xFF10B981)
        percentage >= 0.7f -> Color(0xFFFBBF24)
        else -> Color(0xFFEF4444)
    }

    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(labelRes), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text("$score / $maxWeight", color = color, fontSize = 12.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { percentage },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
            color = color,
            trackColor = Color.White.copy(alpha = 0.05f)
        )
    }
}

@Composable
fun FindingsSummaryRow(result: WifiAuditResult, viewModel: WifiSecurityViewModel, context: Context, onNavigateToDnsLeakTest: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val findings = mutableListOf<String>()
        if (result.criticalCount > 0) findings.add("${result.criticalCount} Critical")
        if (result.warningCount > 0) findings.add("${result.warningCount} Warning")
        
        val summaryText = if (findings.isEmpty()) "No Security Issues Found" else findings.joinToString(" • ")
        
        Column(modifier = Modifier.weight(1f)) {
            Text(summaryText, color = if (result.criticalCount > 0) Color(0xFFEF4444) else if (result.warningCount > 0) Color(0xFFFBBF24) else Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            
            val hasPublicDns = result.dnsServers.any { it == "8.8.8.8" || it == "8.8.4.4" || it == "1.1.1.1" || it == "9.9.9.9" }
            if (hasPublicDns) {
                Text(
                    text = "Check if leaking →",
                    color = Color(0xFF6C8CFF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateToDnsLeakTest() }.padding(top = 4.dp)
                )
            }
        }
        
        Row {
            IconButton(onClick = { 
                viewModel.copyReportToClipboard(context)
                Toast.makeText(context, context.getString(R.string.wifi_audit_report_copied), Toast.LENGTH_SHORT).show()
            }, modifier = Modifier.size(32.dp)) {
                Icon(AppIcons.contentCopy(), null, tint = Color(0xFF6C8CFF), modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = { viewModel.shareReport(context) }, modifier = Modifier.size(32.dp)) {
                Icon(AppIcons.share(), null, tint = Color(0xFF6C8CFF), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun AuditDetailCard(check: WifiCheckItem, gateway: String, onNavigateToLanScanner: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    val statusColor = when (check.status) {
        WifiCheckStatus.SAFE -> Color(0xFF10B981)
        WifiCheckStatus.WARNING -> Color(0xFFFBBF24)
        WifiCheckStatus.CRITICAL -> Color(0xFFEF4444)
        WifiCheckStatus.INFO -> Color(0xFF6C8CFF)
        WifiCheckStatus.UNKNOWN -> Color(0xFF9CA3AF)
    }

    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        color = SoftDarkBg,
        shape = RoundedCornerShape(16.dp),
        border = if (expanded) BorderStroke(1.dp, statusColor.copy(alpha = 0.3f)) else null
    ) {
        Column(modifier = Modifier.clickable { expanded = !expanded }.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(statusColor))
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    val nameText = if (check.nameRes == R.string.wifi_audit_gateway) {
                        stringResource(check.nameRes, gateway)
                    } else {
                        stringResource(check.nameRes)
                    }
                    Text(nameText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(check.detail, color = TextGray, fontSize = 12.sp)
                }
                Icon(
                    painter = if (expanded) AppIcons.expandLess() else AppIcons.expandMore(),
                    contentDescription = null,
                    tint = TextGray,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    Spacer(Modifier.height(12.dp))
                    
                    Text(stringResource(R.string.wifi_audit_why_it_matters), color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, letterSpacing = 0.5.sp)
                    Text(
                        if (check.status == WifiCheckStatus.UNKNOWN) stringResource(R.string.wifi_audit_unknown_desc) 
                        else stringResource(R.string.wifi_audit_why_it_matters_desc),
                        color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(vertical = 4.dp)
                    )
                    
                    check.recommendation?.let { rec ->
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.wifi_audit_rec), color = statusColor, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, letterSpacing = 0.5.sp)
                        Text(rec, color = Color.White, fontSize = 13.sp, lineHeight = 18.sp)
                    }
                    
                    check.action?.let { action ->
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { 
                                if (action == WifiCheckAction.OPEN_LAN_SCANNER) {
                                    onNavigateToLanScanner()
                                } else {
                                    handleAuditAction(context, action, gateway) 
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = statusColor.copy(alpha = 0.15f), contentColor = statusColor),
                            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
                        ) {
                            Text(getActionLabel(action, context), fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    
                    if (check.durationMs != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.wifi_audit_duration_label, check.durationMs), color = TextGray, fontSize = 10.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
fun SecureWiFiChecklist(result: WifiAuditResult) {
    val items = listOf(
        R.string.wifi_audit_checklist_wpa23 to (result.checks.any { it.id == "encryption" && it.status == WifiCheckStatus.SAFE }),
        R.string.wifi_audit_checklist_wps_off to (result.checks.any { it.id == "wps" && it.status == WifiCheckStatus.SAFE }),
        R.string.wifi_audit_checklist_strong_pass to null, // Manual
        R.string.wifi_audit_checklist_admin_pass to null, // Manual
        R.string.wifi_audit_checklist_https_mgmt to (result.checks.any { it.id == "gateway_ports" && it.status == WifiCheckStatus.SAFE }),
        R.string.wifi_audit_checklist_telnet_off to (result.checks.any { it.id == "gateway_ports" && it.status != WifiCheckStatus.CRITICAL }),
        R.string.wifi_audit_checklist_firmware to null, // Manual
        R.string.wifi_audit_checklist_guest to null, // Manual
        R.string.wifi_audit_checklist_isolation to (result.checks.any { it.id == "isolation" && it.status == WifiCheckStatus.SAFE })
    )
    
    val completedCount = items.count { it.second == true }

    Surface(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        color = SoftDarkBg,
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(stringResource(R.string.wifi_audit_checklist_title), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(12.dp))
            
            items.forEach { (resId, status) ->
                ChecklistItem(stringResource(resId), status)
            }
            
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { completedCount / items.size.toFloat() },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = Color(0xFF6C8CFF),
                trackColor = Color.White.copy(alpha = 0.05f)
            )
            Text(stringResource(R.string.wifi_audit_verified_count, completedCount, items.size), color = TextGray, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun ChecklistItem(label: String, status: Boolean?) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(
            painter = when(status) {
                true -> AppIcons.checkCircle()
                false -> AppIcons.cancel()
                else -> AppIcons.info()
            },
            contentDescription = null,
            tint = when(status) {
                true -> Color(0xFF10B981)
                false -> Color(0xFFEF4444)
                else -> TextGray
            },
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(label, color = if (status == null) TextGray else Color.White, fontSize = 13.sp)
    }
}

@Composable
fun InfoChip(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

fun handleAuditAction(context: Context, action: WifiCheckAction, gateway: String) {
    when (action) {
        WifiCheckAction.OPEN_ROUTER -> {
            try {
                val url = if (!gateway.startsWith("http")) "http://$gateway" else gateway
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, context.getString(R.string.wifi_audit_router_fail), Toast.LENGTH_SHORT).show()
            }
        }
        WifiCheckAction.OPEN_WIFI_SETTINGS -> {
            try {
                val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (e: Exception) {}
        }
        WifiCheckAction.OPEN_PRIVATE_DNS_SETTINGS -> {
            try {
                val intent = Intent("android.settings.PRIVATE_DNS_SETTINGS")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (e: Exception) {
                try {
                    val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } catch (ex: Exception) {}
            }
        }
        WifiCheckAction.OPEN_VPN_SETTINGS -> {
            try {
                val intent = Intent("android.net.vpn.SETTINGS")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (e: Exception) {
                try {
                    val intent = Intent(Settings.ACTION_VPN_SETTINGS)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } catch (ex: Exception) {}
            }
        }
        WifiCheckAction.OPEN_LAN_SCANNER -> {} // Handled in UI callback
    }
}

fun getActionLabel(action: WifiCheckAction, context: Context): String {
    return when (action) {
        WifiCheckAction.OPEN_ROUTER -> context.getString(R.string.wifi_audit_how_to_fix)
        WifiCheckAction.OPEN_WIFI_SETTINGS -> context.getString(R.string.nav_settings)
        WifiCheckAction.OPEN_PRIVATE_DNS_SETTINGS -> context.getString(R.string.diag_dns)
        WifiCheckAction.OPEN_VPN_SETTINGS -> "VPN"
        WifiCheckAction.OPEN_LAN_SCANNER -> context.getString(R.string.scan_lan)
    }
}
