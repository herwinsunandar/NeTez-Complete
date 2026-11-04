package com.herwin.netez.ui.screens

import com.herwin.netez.entitlement.FeatureAccessManager

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.R
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.theme.TechMonospace
import com.herwin.netez.ui.viewmodels.LeakCheckResult
import com.herwin.netez.ui.viewmodels.LeakCheckViewModel

import com.herwin.netez.entitlement.FeatureId
import com.herwin.netez.ui.components.ContextualPaywallDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeakCheckScreen(
    viewModel: LeakCheckViewModel = viewModel()
) {
    val context = LocalContext.current
    val detectedCount = viewModel.getDetectedEmailCount()

    if (viewModel.showPaywallDialog) {
        ContextualPaywallDialog(
            featureId = FeatureId.LEAK_CHECK_DAILY,
            limitText = viewModel.paywallText.ifBlank { "Free plan supports up to 3 email leak checks per day. Upgrade to NeTez PRO or watch an ad to get +1 bonus check." },
            onDismiss = { viewModel.showPaywallDialog = false },
            onUpgradeClick = {}
        )
    }

    if (viewModel.showGlobalSupportDialog) {
        ContextualPaywallDialog(
            featureId = FeatureId.GLOBAL_TOOL_USAGE,
            limitText = "You've performed 3 network & security scans! If NeTez is helpful, consider watching a short video to support development or upgrade to PRO (Skippable).",
            onDismiss = { viewModel.showGlobalSupportDialog = false },
            onUpgradeClick = {}
        )
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.parseCsvFile(it) }
    }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { os ->
                os.write(viewModel.getExportCsvContent().toByteArray())
                Toast.makeText(context, "Results exported successfully", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Input Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = viewModel.inputEmails,
                            onValueChange = { viewModel.onEmailChange(it) },
                            placeholder = {
                                Text(
                                    text = "e.g. user@example.com or paste list of emails",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    fontSize = 13.sp
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 1,
                            maxLines = 6,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            ),
                            trailingIcon = {
                                if (viewModel.inputEmails.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onEmailChange("") }) {
                                        Icon(
                                            painter = AppIcons.close(),
                                            contentDescription = "Clear",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (detectedCount > 0) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.leak_emails_detected, detectedCount),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        fontFamily = TechMonospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Spacer(Modifier.width(1.dp))
                            }

                            TextButton(
                                onClick = { viewModel.checkMyEmail() },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(
                                    text = stringResource(R.string.leak_btn_check_my_email),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val accessManager = FeatureAccessManager.getInstance(context)
                                    if (!accessManager.isPro()) {
                                        viewModel.paywallText = "Bulk CSV Email Leak Check is available in NeTez PRO."
                                        viewModel.showPaywallDialog = true
                                    } else {
                                        filePickerLauncher.launch("*/*")
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(painter = AppIcons.folder(), contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.leak_btn_upload_csv), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { viewModel.startManualCheck() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !viewModel.isScanning && detectedCount > 0
                            ) {
                                if (viewModel.isScanning) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(stringResource(R.string.blacklist_btn_check), fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Text(
                            text = stringResource(R.string.leak_privacy_disclaimer),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            // Progress Bar
            if (viewModel.isScanning) {
                item {
                    val current = viewModel.scanProgress.first
                    val total = viewModel.scanProgress.second
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.leak_checking_progress, current, total, viewModel.breachedSoFar),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${(current.toFloat() / total * 100).toInt()}%",
                                    fontSize = 12.sp,
                                    fontFamily = TechMonospace,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { if (total > 0) current.toFloat() / total else 0f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }

            // 2. Results Toolbar
            if (viewModel.batchResults.isNotEmpty() && !viewModel.isScanning) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Scan Results",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row {
                            IconButton(onClick = { csvExportLauncher.launch("leak_results_${System.currentTimeMillis()}.csv") }) {
                                Icon(
                                    painter = AppIcons.download(),
                                    contentDescription = "Export CSV",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // 3. Summary Card (Batch)
                item {
                    val total = viewModel.batchResults.size
                    val breachedCount = viewModel.batchResults.count { it.isBreached }
                    val isBreached = breachedCount > 0

                    val statusColor = if (isBreached) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                    val statusIcon = if (isBreached) AppIcons.error() else AppIcons.checkCircle()
                    val statusText = if (isBreached) "BREACHED" else "CLEAN"
                    val statusDesc = if (isBreached) stringResource(R.string.leak_batch_breached_desc, breachedCount, total) else stringResource(R.string.leak_batch_clean_desc, total, total)

                    val avgRisk = if (total > 0) viewModel.batchResults.sumOf { it.riskScore } / total else 0

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        tonalElevation = 2.dp
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Surface(
                                    color = statusColor.copy(alpha = 0.15f),
                                    shape = CircleShape,
                                    border = BorderStroke(1.dp, statusColor)
                                ) {
                                    Box(
                                        modifier = Modifier.size(44.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = statusIcon,
                                            contentDescription = null,
                                            tint = statusColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = statusText,
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                    Text(
                                        text = statusDesc,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }

                            // Average Risk Score
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Average Risk Score", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("$avgRisk/100", fontSize = 11.sp, fontFamily = TechMonospace, color = statusColor, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { avgRisk / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape),
                                    color = statusColor,
                                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                    }
                }

                // 4. Summary Chips
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val total = viewModel.batchResults.size
                        val breachedCount = viewModel.batchResults.count { it.isBreached }
                        val cleanCount = total - breachedCount

                        LeakSummaryStatusChip("All ($total)", MaterialTheme.colorScheme.primary, true)
                        LeakSummaryStatusChip("$breachedCount Breached", if (breachedCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant, breachedCount > 0)
                        LeakSummaryStatusChip("$cleanCount Clean", MaterialTheme.colorScheme.secondary, cleanCount > 0)
                    }
                }

                // 5. Result List
                items(viewModel.batchResults) { res ->
                    BatchResultItem(res)
                }
            }

            item { Spacer(Modifier.height(40.dp)) }
        }
    }

    // CSV Preview BottomSheet
    if (viewModel.showCsvPreview) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.showCsvPreview = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.leak_csv_preview_title, viewModel.csvPreviewList.size),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.leak_csv_preview_desc, viewModel.csvInvalidCount, 5.coerceAtMost(viewModel.csvPreviewList.size)),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    viewModel.csvPreviewList.take(5).forEach { csv ->
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = AppIcons.person(),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = csv.email,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontFamily = TechMonospace,
                                fontSize = 13.sp
                            )
                            if (csv.name != null) {
                                Text(
                                    text = " • ${csv.name}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.leak_offline_disclaimer),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.showCsvPreview = false },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.btn_cancel))
                    }
                    Button(
                        onClick = {
                            viewModel.startBatchCheck(viewModel.csvPreviewList)
                            viewModel.showCsvPreview = false
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("CHECK ALL", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun BatchResultItem(res: LeakCheckResult) {
    var expanded by remember { mutableStateOf(false) }
    val statusColor = if (res.isBreached) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (res.isBreached) statusColor.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(statusColor))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = res.email,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = TechMonospace,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    val detail = if (res.isBreached) "BREACHED (${res.breaches.size} breaches)" else "CLEAN"
                    val deptInfo = if (res.dept != null) " • Dept: ${res.dept}" else ""
                    Text(
                        text = "$detail$deptInfo",
                        fontSize = 11.sp,
                        color = if (res.isBreached) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    painter = if (expanded) AppIcons.keyboardArrowUp() else AppIcons.keyboardArrowDown(),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (expanded && res.isBreached) {
                Spacer(Modifier.height(12.dp))
                res.breaches.forEach { breach ->
                    Column(modifier = Modifier.padding(start = 22.dp, bottom = 8.dp)) {
                        Text(
                            text = "• ${breach.name} (${breach.year})",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "  Data: ${breach.data}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.leak_advice),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 22.dp)
                )
            }
        }
    }
}

@Composable
private fun LeakSummaryStatusChip(label: String, color: Color, isHighlighted: Boolean) {
    Surface(
        color = if (isHighlighted) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp),
        border = if (isHighlighted) BorderStroke(1.dp, color.copy(alpha = 0.3f)) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                color = if (isHighlighted) color else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
