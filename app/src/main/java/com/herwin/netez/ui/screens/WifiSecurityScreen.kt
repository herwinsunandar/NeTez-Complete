package com.herwin.netez.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.herwin.netez.entitlement.FeatureAccessManager
import com.herwin.netez.entitlement.FeatureId
import com.herwin.netez.network.WifiAuditResult
import com.herwin.netez.network.WifiCheckAction
import com.herwin.netez.network.WifiCheckItem
import com.herwin.netez.network.WifiCheckStatus
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.components.ContextualPaywallDialog
import com.herwin.netez.ui.theme.SoftDarkBg
import com.herwin.netez.ui.theme.SoftTechSecondary
import com.herwin.netez.ui.theme.TechMonospace
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
    val accessManager = remember { FeatureAccessManager.getInstance(context) }
    var showPaywallDialog by remember { mutableStateOf(false) }

    val pdfExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportPdfReport(context, it) }
    }

    if (showPaywallDialog) {
        ContextualPaywallDialog(
            featureId = FeatureId.REPORT_EXPORT,
            limitText = "Professional Wi-Fi Security Audit PDF Report export is available in NeTez PRO.",
            onDismiss = { showPaywallDialog = false },
            onUpgradeClick = {}
        )
    }

    LaunchedEffect(viewModel.actionMessage) {
        viewModel.actionMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.actionMessage = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                // 3. Score Card & Report Actions
                item {
                    SecurityScoreCard(
                        result = result,
                        context = context,
                        viewModel = viewModel,
                        onExportPdf = {
                            if (!accessManager.isPro()) {
                                showPaywallDialog = true
                            } else {
                                val cleanSsid = (result.ssid ?: "Wifi").replace(Regex("[^a-zA-Z0-9]"), "_")
                                pdfExportLauncher.launch("WiFi_Audit_${cleanSsid}.pdf")
                            }
                        }
                    )
                }

                // 4. Category Breakdown
                item {
                    CategoryBreakdownList(result.categoryScores)
                }

                // 5. Checklist Items
                items(result.checks) { check ->
                    AuditCheckCard(check, onNavigateToLanScanner, onNavigateToDnsLeakTest)
                }
            }
        } ?: run {
            if (!uiState.isScanning) {
                EmptyAuditPlaceholder()
            }
        }
    }
}

@Composable
fun WelcomeWifiHeader() {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        color = SoftDarkBg,
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(stringResource(R.string.security_wifi_audit_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
            Text(stringResource(R.string.security_wifi_audit_desc), style = MaterialTheme.typography.bodySmall, color = TextGray)
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
fun InfoChip(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
    }
}

@Composable
fun ScanControlRow(
    isScanning: Boolean,
    onStartScan: () -> Unit,
    onNavigateToLanScanner: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onStartScan,
            enabled = !isScanning,
            modifier = Modifier.weight(1f).height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C8CFF))
        ) {
            if (isScanning) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Icon(AppIcons.shieldCheck(), null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.wifi_audit_scan), fontWeight = FontWeight.Bold)
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
fun SecurityScoreCard(
    result: WifiAuditResult,
    context: Context,
    viewModel: WifiSecurityViewModel,
    onExportPdf: () -> Unit
) {
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

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
            Spacer(Modifier.height(16.dp))

            // Export Actions Row (1 Row Side-By-Side)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { viewModel.copyReportToClipboard(context) },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    Icon(painter = AppIcons.contentCopy(), contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                    Spacer(Modifier.width(4.dp))
                    Text("COPY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                }

                OutlinedButton(
                    onClick = { viewModel.shareReport(context) },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    Icon(painter = AppIcons.share(), contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                    Spacer(Modifier.width(4.dp))
                    Text("SHARE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                }

                Button(
                    onClick = onExportPdf,
                    modifier = Modifier
                        .weight(1.2f)
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    Icon(painter = AppIcons.download(), contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("PDF REPORT", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                }
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
    
    val pct = (score.toFloat() / maxWeight).coerceIn(0f, 1f)
    val color = when {
        pct >= 0.8f -> Color(0xFF10B981)
        pct >= 0.5f -> Color(0xFFFBBF24)
        else -> Color(0xFFEF4444)
    }

    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(labelRes), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("$score / $maxWeight", color = color, fontSize = 12.sp, fontFamily = TechMonospace, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { pct },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
            color = color,
            trackColor = Color.White.copy(alpha = 0.05f)
        )
    }
}

@Composable
fun AuditCheckCard(
    check: WifiCheckItem,
    onNavigateToLanScanner: () -> Unit,
    onNavigateToDnsLeakTest: () -> Unit
) {
    val (statusColor, icon) = when(check.status) {
        WifiCheckStatus.CRITICAL -> Color(0xFFEF4444) to AppIcons.warning()
        WifiCheckStatus.WARNING -> Color(0xFFFBBF24) to AppIcons.warning()
        WifiCheckStatus.SAFE -> Color(0xFF10B981) to AppIcons.checkCircle()
        else -> TextGray to AppIcons.info()
    }

    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        color = SoftDarkBg,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painter = icon, contentDescription = null, tint = statusColor, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(check.nameRes),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                check.score?.let { score ->
                    Text("+$score", color = statusColor, fontFamily = TechMonospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(check.detail, color = TextGray, fontSize = 12.sp)

            check.recommendation?.let { rec ->
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = Color.White.copy(alpha = 0.03f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("💡 $rec", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, modifier = Modifier.padding(8.dp))
                }
            }

            check.action?.let { action ->
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        when (action) {
                            WifiCheckAction.OPEN_LAN_SCANNER -> onNavigateToLanScanner()
                            WifiCheckAction.OPEN_PRIVATE_DNS_SETTINGS -> onNavigateToDnsLeakTest()
                            else -> {}
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = SoftTechSecondary)
                ) {
                    Text("FIX ISSUE →", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun EmptyAuditPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(AppIcons.wifiShield(), null, tint = TextGray.copy(alpha = 0.2f), modifier = Modifier.size(80.dp))
            Spacer(Modifier.height(16.dp))
            Text("No Audit Run Yet", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(4.dp))
            Text("Tap 'Start Audit' above to run a comprehensive security analysis on your connected Wi-Fi network.", color = TextGray, fontSize = 12.sp, textAlign = TextAlign.Center)
        }
    }
}
