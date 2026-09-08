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
import com.herwin.netez.ui.theme.PremiumNavy
import com.herwin.netez.ui.theme.SoftDarkBg
import com.herwin.netez.ui.theme.TextGray
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
            containerColor = SoftDarkBg
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
            .background(PremiumNavy)
    ) {
        // Top Section: Header & Progress
        if (uiState.hasScanned || uiState.isScanning) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SoftDarkBg,
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                tonalElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (uiState.isScanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF6C8CFF)
                                )
                            } else {
                                IconButton(onClick = { viewModel.startScan() }) {
                                    Icon(AppIcons.refresh(), null, tint = Color(0xFF6C8CFF), modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }

                    if (uiState.isScanning) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                            color = Color(0xFF6C8CFF),
                            trackColor = Color.White.copy(alpha = 0.05f)
                        )
                    }
                }
            }
        }

        // Filter & Actions Row (Match LAN Scanner)
        if (uiState.hasScanned) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (uiState.apps.isNotEmpty()) {
                    IconButton(onClick = { 
                        viewModel.copyResultsToClipboard(context)
                        Toast.makeText(context, R.string.app_scan_results_copied, Toast.LENGTH_SHORT).show()
                    }, modifier = Modifier.size(32.dp)) {
                        Icon(AppIcons.contentCopy(), null, tint = Color(0xFF6C8CFF), modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { viewModel.shareResults(context) }, modifier = Modifier.size(32.dp)) {
                        Icon(AppIcons.share(), null, tint = Color(0xFF6C8CFF), modifier = Modifier.size(20.dp))
                    }
                    VerticalDivider(modifier = Modifier.height(20.dp).padding(horizontal = 4.dp), color = Color.White.copy(alpha = 0.1f))
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
                    color = if (dangerousCount > 0) Color(0xFFEF4444) else TextGray
                )

                val suspiciousCount = uiState.apps.count { it.riskCategory == RiskCategory.SUSPICIOUS }
                AppScanFilterChip(
                    label = stringResource(R.string.app_scan_filter_suspicious, suspiciousCount),
                    selected = uiState.filter == AppPermissionFilter.SUSPICIOUS,
                    onClick = { viewModel.updateFilter(AppPermissionFilter.SUSPICIOUS) },
                    color = if (suspiciousCount > 0) Color(0xFFFBBF24) else TextGray
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
            modifier = Modifier.weight(1f).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
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
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                painter = AppIcons.shield(),
                                contentDescription = null,
                                tint = Color(0xFF6C8CFF).copy(alpha = 0.3f),
                                modifier = Modifier.size(100.dp)
                            )
                            Spacer(Modifier.height(24.dp))
                            Text(
                                text = stringResource(R.string.security_app_scanner_title),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.app_scan_start_desc),
                                color = TextGray,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                            Spacer(Modifier.height(32.dp))
                            Button(
                                onClick = { viewModel.startScan() },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C8CFF))
                            ) {
                                Text(stringResource(R.string.app_scan_btn_start), fontWeight = FontWeight.ExtraBold)
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
                                modifier = Modifier.fillMaxWidth().clickable { viewModel.toggleSystemApps() },
                                color = Color(0xFF6C8CFF).copy(alpha = 0.05f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.app_scan_hidden_banner, systemCount),
                                        color = Color(0xFF6C8CFF),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                if (uiState.filteredApps.isEmpty() && !uiState.isScanning) {
                    item {
                        Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.app_scan_empty), color = TextGray)
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
        shape = RoundedCornerShape(20.dp),
        color = SoftDarkBg
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // App Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    app.icon?.let {
                        Image(
                            bitmap = it.toBitmap().asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.size(36.dp)
                        )
                    } ?: Icon(AppIcons.info(), null, tint = TextGray)
                }

                Spacer(Modifier.width(16.dp))

                // App Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF8A8FA8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    // Chips: [SYSTEM/USER], [TRUSTED/UNKNOWN]
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (app.isSystem) {
                            TinyChip("SYSTEM", Color(0xFF2A2E45), Color.White.copy(alpha = 0.6f))
                        } else {
                            TinyChip("USER APP", Color(0xFF6C8CFF).copy(alpha = 0.1f), Color(0xFF6C8CFF))
                        }
                        
                        if (app.isTrusted) {
                            TinyChip("TRUSTED", Color(0xFF6C8CFF).copy(alpha = 0.1f), Color(0xFF6C8CFF))
                        } else {
                            TinyChip("UNKNOWN", Color(0xFFFBBF24).copy(alpha = 0.1f), Color(0xFFFBBF24))
                        }
                    }
                }

                // Risk Score Chip
                val riskColor = when (app.riskCategory) {
                    RiskCategory.DANGEROUS -> Color(0xFFEF4444)
                    RiskCategory.SUSPICIOUS -> Color(0xFFFBBF24)
                    RiskCategory.CLEAN -> Color(0xFF10B981)
                }

                Surface(
                    color = riskColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, riskColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "${app.riskScore}",
                        color = riskColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            if (app.riskReasonRes != 0) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(app.riskReasonRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (app.riskCategory == RiskCategory.DANGEROUS) Color(0xFFEF4444) else TextGray,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(12.dp))

            // Permissions Tags
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                app.permissions.take(5).forEach { permission ->
                    val cleanName = permission.substringAfterLast(".")
                    Surface(
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Text(
                            text = cleanName,
                            color = TextGray,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                if (app.permissions.size > 5) {
                    Text(
                        text = "+${app.permissions.size - 5}",
                        color = TextGray,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Action Button
            Button(
                onClick = { onAppInfo(app.packageName) },
                modifier = Modifier.fillMaxWidth().height(40.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.05f),
                    contentColor = Color.White
                )
            ) {
                Text(stringResource(R.string.app_scan_app_info), fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun AppScanFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    color: Color = Color(0xFF6C8CFF)
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 12.sp) },
        shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = color,
            selectedLabelColor = Color.White,
            containerColor = Color.White.copy(alpha = 0.05f),
            labelColor = TextGray
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
        modifier = Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            app.icon?.let {
                Image(bitmap = it.toBitmap().asImageBitmap(), contentDescription = null, modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(app.appName, color = Color.White, fontWeight = FontWeight.Bold)
                Text(app.packageName, color = TextGray, fontSize = 12.sp)
            }
        }
        
        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
        
        ListItem(
            headlineContent = { Text(stringResource(R.string.set_about).substringBefore(" ") + " Info", color = Color.White) },
            leadingContent = { Icon(AppIcons.info(), null, tint = Color(0xFF6C8CFF)) },
            modifier = Modifier.clickable { 
                onAppInfo(app.packageName)
                onDismiss()
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )

        if (app.isSystem) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.app_scan_disable_system), color = TextGray) },
                leadingContent = { Icon(AppIcons.visibilityOff(), null, tint = TextGray) },
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
