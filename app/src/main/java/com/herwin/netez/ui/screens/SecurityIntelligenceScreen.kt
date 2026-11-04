package com.herwin.netez.ui.screens

import com.herwin.netez.entitlement.FeatureAccessManager

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.engine.intelligence.*
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.theme.TechMonospace
import com.herwin.netez.ui.viewmodels.SecurityIntelligenceViewModel
import java.text.SimpleDateFormat
import java.util.*

import com.herwin.netez.entitlement.FeatureId
import com.herwin.netez.ui.components.ContextualPaywallDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityIntelligenceScreen(
    onBack: () -> Unit = {},
    onNavigateToUpgradeToPro: () -> Unit = {},
    viewModel: SecurityIntelligenceViewModel = viewModel()
) {
    val context = LocalContext.current
    val scanResult = viewModel.scanResult
    val recentHistory by viewModel.recentHistory.collectAsState()
    var isTechDetailsExpanded by remember { mutableStateOf(false) }
    var showPaywallDialog by remember { mutableStateOf(false) }

    if (viewModel.showPaywallDialog) {
        ContextualPaywallDialog(
            featureId = FeatureId.SECURITY_INTELLIGENCE_DAILY,
            limitText = "Today's free assessments limit (3/day) reached. Upgrade to NeTez PRO for higher limits or watch a quick video for +1 bonus scan.",
            onDismiss = { viewModel.showPaywallDialog = false },
            onUpgradeClick = onNavigateToUpgradeToPro
        )
    }

    if (viewModel.showGlobalSupportDialog) {
        ContextualPaywallDialog(
            featureId = FeatureId.GLOBAL_TOOL_USAGE,
            limitText = "You've performed 3 network & security scans! If NeTez is helpful, consider watching a short video to support development or upgrade to PRO (Skippable).",
            onDismiss = { viewModel.showGlobalSupportDialog = false },
            onUpgradeClick = onNavigateToUpgradeToPro
        )
    }

    val pdfExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportPdfReport(context, it) }
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
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Input Section Card
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = viewModel.inputQuery,
                                onValueChange = { viewModel.onQueryChange(it) },
                                placeholder = {
                                    Text(
                                        text = "example.com or user@example.com",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        fontSize = 13.sp
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !viewModel.isScanning,
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Uri,
                                    imeAction = ImeAction.Go
                                ),
                                keyboardActions = KeyboardActions(
                                    onGo = { viewModel.startAssessment() }
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                ),
                                trailingIcon = {
                                    if (viewModel.inputQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                                            Icon(
                                                painter = AppIcons.close(),
                                                contentDescription = "Clear",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            )

                            Button(
                                onClick = { viewModel.startAssessment() },
                                enabled = !viewModel.isScanning && viewModel.inputQuery.isNotBlank(),
                                modifier = Modifier.height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                if (viewModel.isScanning) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        painter = AppIcons.security(),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("SCAN", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Auto Input Detection Pills
                        val detectedType = viewModel.parsedTarget?.type
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Analyze:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )

                            TargetTypeChip(
                                label = "Domain",
                                isSelected = detectedType == TargetType.DOMAIN,
                                onClick = {
                                    if (viewModel.inputQuery.isBlank()) viewModel.onQueryChange("example.com")
                                }
                            )

                            TargetTypeChip(
                                label = "URL",
                                isSelected = detectedType == TargetType.URL,
                                onClick = {
                                    if (viewModel.inputQuery.isBlank()) viewModel.onQueryChange("https://example.com/login")
                                }
                            )

                            TargetTypeChip(
                                label = "Email",
                                isSelected = detectedType == TargetType.EMAIL,
                                onClick = {
                                    if (viewModel.inputQuery.isBlank()) viewModel.onQueryChange("user@example.com")
                                }
                            )
                        }
                    }
                }
            }

            // Error Message Banner
            viewModel.errorMessage?.let { err ->
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = AppIcons.error(),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = err,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Live Progress Checklist (While scanning)
            if (viewModel.isScanning && scanResult != null) {
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
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Analyzing ${scanResult.target.rawInput}...",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp
                            )

                            val completed = scanResult.categoryStatuses.count { it.status != CheckExecutionStatus.WAITING && it.status != CheckExecutionStatus.RUNNING }
                            val total = scanResult.categoryStatuses.size
                            val progressFloat = if (total > 0) completed.toFloat() / total else 0f

                            LinearProgressIndicator(
                                progress = { progressFloat },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                scanResult.categoryStatuses.forEach { cat ->
                                    LiveCheckStatusRow(cat)
                                }
                            }
                        }
                    }
                }
            }

            // 2. Results Dashboard
            if (scanResult != null && !viewModel.isScanning) {
                val res = scanResult

                // Summary Header Bar
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Assessment Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row {
                            IconButton(onClick = {
                                val accessManager = FeatureAccessManager.getInstance(context)
                                if (!accessManager.isPro()) {
                                    viewModel.showPaywallDialog = true
                                } else {
                                    val cleanTarget = res.target.rawInput.replace(Regex("[^a-zA-Z0-9]"), "_")
                                    pdfExportLauncher.launch("Security_Report_${cleanTarget}.pdf")
                                }
                            }) {
                                Icon(
                                    painter = AppIcons.download(),
                                    contentDescription = "Export PDF Report",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = {
                                viewModel.copySummaryToClipboard(context)
                                Toast.makeText(context, "Summary copied to clipboard", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(
                                    painter = AppIcons.contentCopy(),
                                    contentDescription = "Copy Summary",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = {
                                viewModel.shareSummary(context)
                            }) {
                                Icon(
                                    painter = AppIcons.share(),
                                    contentDescription = "Share Summary",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Overall Status Banner
                item {
                    val statusColor = when (res.overallStatus) {
                        "CRITICAL RISK" -> MaterialTheme.colorScheme.error
                        "HIGH RISK" -> MaterialTheme.colorScheme.error
                        "MODERATE RISK" -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.secondary
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)),
                        tonalElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = res.target.rawInput,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontFamily = TechMonospace,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${res.target.type.name} ASSESSMENT",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = TechMonospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Surface(
                                    color = statusColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, statusColor)
                                ) {
                                    Text(
                                        text = res.overallStatus,
                                        color = statusColor,
                                        fontFamily = TechMonospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                            Text(
                                text = "${res.checksCompleted} checks completed · ${res.warningCount} warning(s) · ${res.criticalCount} critical",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Category Check Cards
                items(res.categoryStatuses) { categoryStatus ->
                    CategoryStatusCard(categoryStatus)
                }

                // Optional Active Scan Trigger
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
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Active Inspection (Optional)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Passive assessment complete. Run active port probing only if you own or have permission to inspect this target.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (viewModel.activeScanRunning) {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(CircleShape),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = viewModel.activeScanProgress,
                                    fontSize = 11.sp,
                                    fontFamily = TechMonospace,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                OutlinedButton(
                                    onClick = { viewModel.runActivePortScan() },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(
                                        painter = AppIcons.radar(),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "RUN ACTIVE SCAN",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                // Security Findings Section
                if (res.findings.isNotEmpty()) {
                    item {
                        Text(
                            text = "Security Findings (${res.findings.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(res.findings) { finding ->
                        FindingCard(finding)
                    }
                }

                // Passed Checks Section
                if (res.passedChecks.isNotEmpty()) {
                    item {
                        Text(
                            text = "Passed Checks (${res.passedChecks.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
                            tonalElevation = 2.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                res.passedChecks.forEach { passed ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            painter = AppIcons.checkCircle(),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column {
                                            Text(
                                                text = passed.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (passed.detail.isNotBlank()) {
                                                Text(
                                                    text = passed.detail,
                                                    fontSize = 11.sp,
                                                    fontFamily = TechMonospace,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (passed.sourceProvider.isNotBlank()) {
                                                Text(
                                                    text = "Source: ${passed.sourceProvider}",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Technical Details Accordion
                if (res.technicalDetails.isNotEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            onClick = { isTechDetailsExpanded = !isTechDetailsExpanded }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Technical Details",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp
                                    )
                                    Icon(
                                        painter = if (isTechDetailsExpanded) AppIcons.expandLess() else AppIcons.expandMore(),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                AnimatedVisibility(visible = isTechDetailsExpanded) {
                                    Column(
                                        modifier = Modifier.padding(top = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                        res.technicalDetails.forEach { (key, value) ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = key,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = value.toString(),
                                                    fontSize = 11.sp,
                                                    fontFamily = TechMonospace,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Recent Assessments History Section
            if (recentHistory.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent Assessments",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(recentHistory.take(5)) { item ->
                    RecentAssessmentCard(
                        result = item,
                        onClick = { viewModel.selectFromHistory(item) },
                        onRescan = { viewModel.rescan(item.target.rawInput) },
                        onDelete = { viewModel.deleteHistoryItem(item.target.rawInput) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TargetTypeChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun LiveCheckStatusRow(cat: CategoryCheckStatus) {
    val (statusText, statusColor) = when (cat.status) {
        CheckExecutionStatus.SUCCESS -> Pair("✓ Done", MaterialTheme.colorScheme.secondary)
        CheckExecutionStatus.WARNING -> Pair("! Warning", MaterialTheme.colorScheme.tertiary)
        CheckExecutionStatus.CRITICAL -> Pair("🔴 Critical", MaterialTheme.colorScheme.error)
        CheckExecutionStatus.RUNNING -> Pair("Running...", MaterialTheme.colorScheme.primary)
        CheckExecutionStatus.UNAVAILABLE -> Pair("Unavailable", MaterialTheme.colorScheme.onSurfaceVariant)
        CheckExecutionStatus.WAITING -> Pair("Waiting...", MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = cat.title,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = statusText,
            fontSize = 11.sp,
            fontFamily = TechMonospace,
            fontWeight = FontWeight.Bold,
            color = statusColor
        )
    }
}

@Composable
private fun CategoryStatusCard(cat: CategoryCheckStatus) {
    val statusColor = when (cat.status) {
        CheckExecutionStatus.CRITICAL -> MaterialTheme.colorScheme.error
        CheckExecutionStatus.WARNING -> MaterialTheme.colorScheme.tertiary
        CheckExecutionStatus.SUCCESS -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = statusColor.copy(alpha = 0.15f),
                shape = CircleShape,
                border = BorderStroke(1.dp, statusColor)
            ) {
                Box(
                    modifier = Modifier.size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = if (cat.status == CheckExecutionStatus.SUCCESS) AppIcons.checkCircle() else AppIcons.info(),
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cat.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = cat.summary,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (cat.sourceProvider.isNotBlank()) {
                    Text(
                        text = "Source: ${cat.sourceProvider}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun FindingCard(finding: SecurityFinding) {
    val severityColor = when (finding.severity) {
        FindingSeverity.CRITICAL -> MaterialTheme.colorScheme.error
        FindingSeverity.HIGH -> MaterialTheme.colorScheme.error
        FindingSeverity.MEDIUM -> MaterialTheme.colorScheme.tertiary
        FindingSeverity.LOW -> MaterialTheme.colorScheme.tertiary
        FindingSeverity.INFO -> MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, severityColor.copy(alpha = 0.4f)),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = severityColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, severityColor)
                ) {
                    Text(
                        text = finding.severity.name,
                        color = severityColor,
                        fontFamily = TechMonospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                if (finding.sourceProvider.isNotBlank()) {
                    Text(
                        text = "Source: ${finding.sourceProvider}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = finding.title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (finding.currentState.isNotBlank()) {
                Text(
                    text = "Current State: ${finding.currentState}",
                    fontSize = 12.sp,
                    fontFamily = TechMonospace,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = "Impact: ${finding.riskImpact}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            Text(
                text = "Recommendation: ${finding.recommendation}",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun RecentAssessmentCard(
    result: SecurityIntelligenceResult,
    onClick: () -> Unit,
    onRescan: () -> Unit,
    onDelete: () -> Unit
) {
    val sdf = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())
    val statusColor = when (result.overallStatus) {
        "CRITICAL RISK" -> MaterialTheme.colorScheme.error
        "HIGH RISK" -> MaterialTheme.colorScheme.error
        "MODERATE RISK" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.secondary
    }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.target.rawInput,
                    fontWeight = FontWeight.Bold,
                    fontFamily = TechMonospace,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${result.overallStatus} · ${sdf.format(Date(result.timestamp))}",
                    fontSize = 11.sp,
                    color = statusColor,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onRescan) {
                    Icon(
                        painter = AppIcons.refresh(),
                        contentDescription = "Re-scan",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        painter = AppIcons.close(),
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
