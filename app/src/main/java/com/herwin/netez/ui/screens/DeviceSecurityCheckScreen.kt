package com.herwin.netez.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.herwin.netez.device.DeviceCheckAction
import com.herwin.netez.device.DeviceCheckItem
import com.herwin.netez.device.DeviceCheckStatus
import com.herwin.netez.device.DeviceSecurityAuditResult
import com.herwin.netez.entitlement.FeatureAccessManager
import com.herwin.netez.entitlement.FeatureId
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.components.ContextualPaywallDialog
import com.herwin.netez.ui.theme.SoftDarkBg
import com.herwin.netez.ui.theme.TextGray
import com.herwin.netez.ui.viewmodels.DeviceSecurityViewModel

@Composable
fun DeviceSecurityCheckScreen(
    viewModel: DeviceSecurityViewModel = viewModel()
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
            limitText = "Professional Device Security Audit PDF Report export is available in NeTez PRO.",
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
        uiState.auditResult?.let { result ->
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Device Info Card
                item {
                    DeviceInfoCard(result)
                }

                // 2. Score Card & Report Actions
                item {
                    DeviceSecurityScoreCard(
                        result = result,
                        context = context,
                        viewModel = viewModel,
                        onExportPdf = {
                            if (!accessManager.isPro()) {
                                showPaywallDialog = true
                            } else {
                                val cleanModel = "${result.manufacturer}_${result.model}".replace(Regex("[^a-zA-Z0-9]"), "_")
                                pdfExportLauncher.launch("Device_Security_${cleanModel}.pdf")
                            }
                        }
                    )
                }

                // 3. Scan Controls
                item {
                    Button(
                        onClick = { viewModel.startScan() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        enabled = !uiState.isScanning
                    ) {
                        if (uiState.isScanning) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        } else {
                            Icon(AppIcons.scan(), null)
                            Spacer(Modifier.width(8.dp))
                            Text("RE-SCAN DEVICE", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }

                // 4. Audit Details
                items(result.checks) { check ->
                    DeviceCheckDetailCard(check)
                }
            }
        } ?: run {
            // Not Scanned Header
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(AppIcons.shieldCheck(), null, tint = Color(0xFF6C8CFF).copy(alpha = 0.3f), modifier = Modifier.size(100.dp))
                    Spacer(Modifier.height(24.dp))
                    Text(stringResource(R.string.device_check_not_scanned), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(Modifier.height(32.dp))
                    Button(
                        onClick = { viewModel.startScan() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C8CFF)),
                        enabled = !uiState.isScanning
                    ) {
                        if (uiState.isScanning) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        } else {
                            Text(stringResource(R.string.scan_now).uppercase(), fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceInfoCard(result: DeviceSecurityAuditResult) {
    Surface(
        color = SoftDarkBg,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("${result.manufacturer} ${result.model}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Spacer(Modifier.height(4.dp))
            Text("Android ${result.androidVersion} • API ${result.sdkVersion}", color = TextGray, fontSize = 14.sp)
            if (result.securityPatch != null) {
                Text("Security Patch: ${result.securityPatch}", color = TextGray, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun DeviceSecurityScoreCard(
    result: DeviceSecurityAuditResult,
    context: Context,
    viewModel: DeviceSecurityViewModel,
    onExportPdf: () -> Unit
) {
    val scoreColor = when {
        result.securityScore >= 90 -> Color(0xFF4CAF50)
        result.securityScore >= 75 -> Color(0xFF4CAF50)
        result.securityScore >= 60 -> Color(0xFFFBBF24)
        result.securityScore >= 40 -> Color(0xFFFFC107)
        else -> Color(0xFFFF5252)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SoftDarkBg,
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.device_score), color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { result.securityScore / 100f },
                    modifier = Modifier.size(120.dp),
                    color = scoreColor,
                    strokeWidth = 10.dp,
                    trackColor = Color.White.copy(alpha = 0.05f),
                    strokeCap = StrokeCap.Round
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${result.securityScore}", color = scoreColor, fontWeight = FontWeight.Black, fontSize = 32.sp)
                    Text(result.grade, color = scoreColor, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatusCounter(stringResource(R.string.status_critical), result.criticalCount, Color(0xFFFF5252))
                StatusCounter(stringResource(R.string.status_warning), result.warningCount, Color(0xFFFBBF24))
                StatusCounter(stringResource(R.string.status_safe), result.safeCount, Color(0xFF4CAF50))
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
                    onClick = { viewModel.shareReport() },
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
fun StatusCounter(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$count", color = color, fontWeight = FontWeight.Black, fontSize = 20.sp)
        Text(label, color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DeviceCheckDetailCard(check: DeviceCheckItem) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    val statusColor = when (check.status) {
        DeviceCheckStatus.SAFE -> Color(0xFF4CAF50)
        DeviceCheckStatus.WARNING -> Color(0xFFFBBF24)
        DeviceCheckStatus.CRITICAL -> Color(0xFFFF5252)
        DeviceCheckStatus.INFO -> Color(0xFF6C8CFF)
        DeviceCheckStatus.UNKNOWN -> Color(0xFF9CA3AF)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SoftDarkBg,
        shape = RoundedCornerShape(16.dp),
        border = if (expanded) BorderStroke(1.dp, statusColor.copy(alpha = 0.3f)) else null
    ) {
        Column(modifier = Modifier.clickable { expanded = !expanded }.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(statusColor))
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(check.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(check.detail, color = TextGray, fontSize = 13.sp)
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
                    
                    Text("RECOMMENDATION", color = statusColor, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, letterSpacing = 0.5.sp)
                    Text(check.recommendation, color = Color.White, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(vertical = 4.dp))
                    
                    if (check.evidence != null) {
                        Spacer(Modifier.height(8.dp))
                        Text("EVIDENCE", color = TextGray, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        Text(check.evidence, color = TextGray.copy(alpha = 0.7f), fontSize = 11.sp, lineHeight = 14.sp)
                    }

                    check.action?.let { action ->
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { handleDeviceAction(context, action) },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = statusColor.copy(alpha = 0.15f), contentColor = statusColor),
                            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
                        ) {
                            Text(getDeviceActionLabel(action), fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    Text("${check.latencyMs}ms", color = TextGray, fontSize = 10.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

fun handleDeviceAction(context: Context, action: DeviceCheckAction) {
    val intent = when (action) {
        DeviceCheckAction.OPEN_SECURITY_SETTINGS -> Intent(Settings.ACTION_SECURITY_SETTINGS)
        DeviceCheckAction.OPEN_SYSTEM_UPDATE_SETTINGS -> Intent("android.settings.SYSTEM_UPDATE_SETTINGS")
        DeviceCheckAction.OPEN_DEVELOPER_SETTINGS -> Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
        DeviceCheckAction.OPEN_UNKNOWN_APP_SOURCES -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
            } else {
                Intent(Settings.ACTION_SECURITY_SETTINGS)
            }
        }
        DeviceCheckAction.OPEN_APP_SETTINGS -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        DeviceCheckAction.OPEN_BIOMETRIC_SETTINGS -> Intent(Settings.ACTION_SECURITY_SETTINGS)
        DeviceCheckAction.OPEN_STORAGE_SETTINGS -> Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
        DeviceCheckAction.OPEN_ACCESSIBILITY_SETTINGS -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    }
    
    try {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback for custom intents
        try {
            context.startActivity(Intent(Settings.ACTION_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
        } catch (ex: Exception) {}
    }
}

fun getDeviceActionLabel(action: DeviceCheckAction): String {
    return when (action) {
        DeviceCheckAction.OPEN_SECURITY_SETTINGS -> "OPEN SECURITY SETTINGS"
        DeviceCheckAction.OPEN_SYSTEM_UPDATE_SETTINGS -> "CHECK FOR UPDATES"
        DeviceCheckAction.OPEN_DEVELOPER_SETTINGS -> "OPEN DEVELOPER OPTIONS"
        DeviceCheckAction.OPEN_UNKNOWN_APP_SOURCES -> "REVIEW APP SOURCES"
        DeviceCheckAction.OPEN_APP_SETTINGS -> "OPEN APP INFO"
        DeviceCheckAction.OPEN_BIOMETRIC_SETTINGS -> "CONIGURE BIOMETRICS"
        DeviceCheckAction.OPEN_STORAGE_SETTINGS -> "MANAGE STORAGE"
        DeviceCheckAction.OPEN_ACCESSIBILITY_SETTINGS -> "REVIEW ACCESSIBILITY"
    }
}
