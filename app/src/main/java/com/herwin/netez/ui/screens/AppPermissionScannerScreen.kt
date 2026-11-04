package com.herwin.netez.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.R
import com.herwin.netez.data.AppPermissionInfo
import com.herwin.netez.data.RiskCategory
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.theme.TechMonospace
import com.herwin.netez.ui.viewmodels.AppPermissionFilter
import com.herwin.netez.ui.viewmodels.AppPermissionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPermissionScannerScreen(
    viewModel: AppPermissionViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var selectedAppForSheet by remember { mutableStateOf<AppPermissionInfo?>(null) }
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    if (showBottomSheet && selectedAppForSheet != null) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            AppActionBottomSheet(
                app = selectedAppForSheet!!,
                onDismiss = { showBottomSheet = false },
                onAppInfo = { openAppInfo(context, it) }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Section: Header & Progress
        if (uiState.hasScanned || uiState.isScanning) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
                tonalElevation = 2.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Permission Risk Audit",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (uiState.isScanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                IconButton(onClick = { viewModel.startScan() }) {
                                    Icon(
                                        painter = AppIcons.refresh(),
                                        contentDescription = "Rescan",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (uiState.isScanning) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }

        // Filter & Actions Row
        if (uiState.hasScanned) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (uiState.apps.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            viewModel.copyResultsToClipboard(context)
                            Toast.makeText(context, R.string.app_scan_results_copied, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            painter = AppIcons.contentCopy(),
                            contentDescription = "Copy",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.shareResults(context) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            painter = AppIcons.share(),
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    VerticalDivider(
                        modifier = Modifier
                            .height(20.dp)
                            .padding(horizontal = 4.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                }

                AppScanFilterChip(
                    label = stringResource(R.string.app_scan_filter_all, uiState.apps.size),
                    selected = uiState.filter == AppPermissionFilter.ALL,
                    onClick = { viewModel.updateFilter(AppPermissionFilter.ALL) }
                )

                val dangerousCount = uiState.apps.count { it.riskCategory == RiskCategory.DANGEROUS }
                AppScanFilterChip(
                    label = stringResource(R.string.app_scan_filter_dangerous, dangerousCount),
                    selected = uiState.filter == AppPermissionFilter.DANGEROUS,
                    onClick = { viewModel.updateFilter(AppPermissionFilter.DANGEROUS) },
                    color = if (dangerousCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )

                val suspiciousCount = uiState.apps.count { it.riskCategory == RiskCategory.SUSPICIOUS }
                AppScanFilterChip(
                    label = stringResource(R.string.app_scan_filter_suspicious, suspiciousCount),
                    selected = uiState.filter == AppPermissionFilter.SUSPICIOUS,
                    onClick = { viewModel.updateFilter(AppPermissionFilter.SUSPICIOUS) },
                    color = if (suspiciousCount > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                )

                AppScanFilterChip(
                    label = "User (${uiState.apps.count { !it.isSystem }})",
                    selected = uiState.filter == AppPermissionFilter.USER,
                    onClick = { viewModel.updateFilter(AppPermissionFilter.USER) }
                )

                AppScanFilterChip(
                    label = "System (${uiState.apps.count { it.isSystem }})",
                    selected = uiState.filter == AppPermissionFilter.SYSTEM,
                    onClick = { viewModel.updateFilter(AppPermissionFilter.SYSTEM) }
                )
            }
        }

        // Results List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            if (!uiState.hasScanned && !uiState.isScanning) {
                item {
                    Box(
                        modifier = Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                painter = AppIcons.shield(),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                modifier = Modifier.size(80.dp)
                            )
                            Spacer(Modifier.height(20.dp))
                            Text(
                                text = stringResource(R.string.security_app_scanner_title),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.app_scan_start_desc),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                            Spacer(Modifier.height(24.dp))
                            Button(
                                onClick = { viewModel.startScan() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text(
                                    text = stringResource(R.string.app_scan_btn_start),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            } else {
                // System Apps Hidden Banner
                if (!uiState.showSystemApps && uiState.filter != AppPermissionFilter.SYSTEM) {
                    val systemCount = uiState.apps.count { it.isSystem }
                    if (systemCount > 0) {
                        item {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleSystemApps() },
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.app_scan_hidden_banner, systemCount),
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                if (uiState.filteredApps.isEmpty() && !uiState.isScanning) {
                    item {
                        Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(R.string.app_scan_empty),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                items(uiState.filteredApps) { app ->
                    AppPermissionCard(
                        app = app,
                        onAppInfo = { openAppInfo(context, it) },
                        onLongPress = {
                            selectedAppForSheet = app
                            showBottomSheet = true
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppPermissionCard(
    app: AppPermissionInfo,
    onAppInfo: (String) -> Unit,
    onLongPress: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = onLongPress
            ),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // App Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    app.icon?.let {
                        Image(
                            bitmap = it.toBitmap().asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                    } ?: Icon(
                        painter = AppIcons.info(),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.width(14.dp))

                // App Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = TechMonospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(6.dp))

                    // Chips: [SYSTEM/USER], [TRUSTED/UNKNOWN]
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (app.isSystem) {
                            TinyChip("SYSTEM", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            TinyChip("USER APP", MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), MaterialTheme.colorScheme.primary)
                        }

                        if (app.isTrusted) {
                            TinyChip("TRUSTED", MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), MaterialTheme.colorScheme.primary)
                        } else {
                            TinyChip("UNKNOWN", MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f), MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }

                // Risk Score Badge
                val riskColor = when (app.riskCategory) {
                    RiskCategory.DANGEROUS -> MaterialTheme.colorScheme.error
                    RiskCategory.SUSPICIOUS -> MaterialTheme.colorScheme.tertiary
                    RiskCategory.CLEAN -> MaterialTheme.colorScheme.secondary
                }

                Surface(
                    color = riskColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, riskColor)
                ) {
                    Text(
                        text = "${app.riskScore}",
                        color = riskColor,
                        fontFamily = TechMonospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            if (app.riskReasonRes != 0) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(app.riskReasonRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (app.riskCategory == RiskCategory.DANGEROUS) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(10.dp))

            // Permissions Tags
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                app.permissions.take(5).forEach { permission ->
                    val cleanName = permission.substringAfterLast(".")
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = cleanName,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = TechMonospace,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
                if (app.permissions.size > 5) {
                    Text(
                        text = "+${app.permissions.size - 5}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Action Button
            OutlinedButton(
                onClick = { onAppInfo(app.packageName) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            ) {
                Text(
                    text = stringResource(R.string.app_scan_app_info),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun TinyChip(label: String, bgColor: Color, textColor: Color) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun AppScanFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    color: Color = MaterialTheme.colorScheme.primary
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
        shape = RoundedCornerShape(10.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = color.copy(alpha = 0.2f),
            selectedLabelColor = color
        ),
        border = null
    )
}

@Composable
fun AppActionBottomSheet(
    app: AppPermissionInfo,
    onDismiss: () -> Unit,
    onAppInfo: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            app.icon?.let {
                Image(
                    bitmap = it.toBitmap().asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(44.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = app.appName,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = app.packageName,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = TechMonospace,
                    fontSize = 11.sp
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        ListItem(
            headlineContent = {
                Text(
                    text = stringResource(R.string.set_about).substringBefore(" ") + " Info",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            leadingContent = {
                Icon(
                    painter = AppIcons.info(),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            modifier = Modifier.clickable {
                onAppInfo(app.packageName)
                onDismiss()
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )

        if (app.isSystem) {
            ListItem(
                headlineContent = {
                    Text(
                        text = stringResource(R.string.app_scan_disable_system),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingContent = {
                    Icon(
                        painter = AppIcons.visibilityOff(),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.clickable {
                    onAppInfo(app.packageName)
                    onDismiss()
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
        }
    }
}

fun openAppInfo(context: Context, packageName: String) {
    if (packageName.isEmpty()) return
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:$packageName")
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, context.getString(R.string.app_scan_info_error), Toast.LENGTH_SHORT).show()
    }
}
