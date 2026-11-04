package com.herwin.netez.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.R
import com.herwin.netez.network.Device
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.components.ResultRow
import com.herwin.netez.ui.theme.SoftDarkBg
import com.herwin.netez.ui.theme.SoftTechSecondary
import com.herwin.netez.ui.theme.TechMonospace
import com.herwin.netez.ui.theme.TextGray
import com.herwin.netez.ui.viewmodels.LanFilter
import com.herwin.netez.ui.viewmodels.LanScannerViewModel
import com.herwin.netez.ui.viewmodels.LanSortOption
import com.herwin.netez.ui.viewmodels.NetworkSummaryInfo
import com.herwin.netez.ui.viewmodels.ScanMode
import java.text.SimpleDateFormat
import java.util.*

import com.herwin.netez.entitlement.FeatureId
import com.herwin.netez.ui.components.ContextualPaywallDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanScannerScreen(
    viewModel: LanScannerViewModel = viewModel(),
    onNavigateToPing: (String) -> Unit = {},
    onNavigateToPortScan: (String) -> Unit = {},
    onNavigateToWifiAudit: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    if (viewModel.showGlobalSupportDialog) {
        ContextualPaywallDialog(
            featureId = FeatureId.GLOBAL_TOOL_USAGE,
            limitText = "You've performed 3 network & security scans! If NeTez is helpful, consider watching a short video to support development or upgrade to PRO (Skippable).",
            onDismiss = { viewModel.showGlobalSupportDialog = false },
            onUpgradeClick = {}
        )
    }
    
    var showIdentifyDialog by remember { mutableStateOf(false) }
    var selectedDeviceToIdentify by remember { mutableStateOf<Device?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }

    if (showIdentifyDialog && selectedDeviceToIdentify != null) {
        val targetDevice = selectedDeviceToIdentify!!
        var customLabel by remember { 
            mutableStateOf(
                targetDevice.vendor.takeIf { it != "Unknown" && it != "Unknown Vendor" && it != "Resolving..." } ?: ""
            ) 
        }
        AlertDialog(
            onDismissRequest = { showIdentifyDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Rename / Label Device", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Enter custom name for ${targetDevice.ip} (${if (targetDevice.mac != "00:00:00:00:00:00") targetDevice.mac else "MAC Hidden"})", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    OutlinedTextField(
                        value = customLabel,
                        onValueChange = { customLabel = it },
                        placeholder = { Text("e.g. Living Room Smart TV", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (customLabel.isNotBlank()) {
                        viewModel.saveVendorOverride(targetDevice.ip, targetDevice.mac, customLabel)
                    }
                    showIdentifyDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showIdentifyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Top Control & Scan Input Section
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Toolbar Row (Sort, Export, Clear)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
                        SegmentedButton(
                            selected = uiState.mode == ScanMode.SINGLE,
                            onClick = { viewModel.updateMode(ScanMode.SINGLE) },
                            shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                            label = { Text("Single", fontSize = 11.sp) }
                        )
                        SegmentedButton(
                            selected = uiState.mode == ScanMode.RANGE,
                            onClick = { viewModel.updateMode(ScanMode.RANGE) },
                            shape = RoundedCornerShape(0.dp),
                            label = { Text("Range", fontSize = 11.sp) }
                        )
                        SegmentedButton(
                            selected = uiState.mode == ScanMode.CIDR,
                            onClick = { viewModel.updateMode(ScanMode.CIDR) },
                            shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                            label = { Text("CIDR", fontSize = 11.sp) }
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    // Sort Dropdown
                    Box {
                        IconButton(onClick = { showSortMenu = true }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort", tint = MaterialTheme.colorScheme.primary)
                        }
                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                            DropdownMenuItem(text = { Text("Sort by IP") }, onClick = { viewModel.updateSortOption(LanSortOption.IP); showSortMenu = false })
                            DropdownMenuItem(text = { Text("Sort by Latency") }, onClick = { viewModel.updateSortOption(LanSortOption.LATENCY); showSortMenu = false })
                            DropdownMenuItem(text = { Text("Sort by Vendor") }, onClick = { viewModel.updateSortOption(LanSortOption.VENDOR); showSortMenu = false })
                            DropdownMenuItem(text = { Text("Sort by Name") }, onClick = { viewModel.updateSortOption(LanSortOption.NAME); showSortMenu = false })
                        }
                    }

                    // Export Dropdown
                    Box {
                        IconButton(onClick = { showExportMenu = true }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Export Options", tint = MaterialTheme.colorScheme.primary)
                        }
                        DropdownMenu(expanded = showExportMenu, onDismissRequest = { showExportMenu = false }) {
                            DropdownMenuItem(text = { Text("Export CSV") }, onClick = { viewModel.exportCsv(context); showExportMenu = false })
                            DropdownMenuItem(text = { Text("Export JSON") }, onClick = { viewModel.exportJson(context); showExportMenu = false })
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        when (uiState.mode) {
                            ScanMode.SINGLE -> InputField(uiState.singleIp, "192.168.1.1") { viewModel.updateSingleIp(it) }
                            ScanMode.RANGE -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.weight(1f)) { InputField(uiState.rangeStart, "Start") { viewModel.updateRangeStart(it) } }
                                Box(Modifier.weight(1f)) { InputField(uiState.rangeEnd, "End") { viewModel.updateRangeEnd(it) } }
                            }
                            ScanMode.CIDR -> InputField(uiState.cidr, "192.168.1.0/24") { viewModel.updateCidr(it) }
                        }
                    }

                    Button(
                        onClick = { 
                            if (uiState.isScanning) viewModel.stopScan() 
                            else viewModel.startScan() 
                        },
                        modifier = Modifier.height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.isScanning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (uiState.isScanning) {
                            Icon(AppIcons.stop(), null)
                            Spacer(Modifier.width(6.dp))
                            Text("STOP", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(AppIcons.scan(), null)
                            Spacer(Modifier.width(6.dp))
                            Text("SCAN", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 2. Scan Header & Progress
                if (uiState.isScanning) {
                    LinearProgressIndicator(
                        progress = { uiState.progress },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                // 2-Line Header Hierarchy
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = uiState.progressText.ifEmpty { "Found ${uiState.devices.count { it.isOnline }} devices" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!uiState.ispInfo.isNullOrEmpty()) {
                        Text(
                            text = uiState.ispInfo!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // 2. Scrollable Device List with Network Summary & Padding
        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp) // Ensure last card not covered by bottom nav
        ) {
            // AP Isolation Warning
            if (uiState.isApIsolationDetected) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFBBF24).copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, Color(0xFFFBBF24).copy(alpha = 0.3f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(AppIcons.warning(), contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Router AP Isolation may be active. Only Gateway and local device were found. Other Wi-Fi devices may be isolated by router security policy.",
                                color = Color(0xFFFBBF24),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Network Summary Card
            item {
                LanNetworkSummaryCard(uiState.networkSummary)
            }

            // Filter Chips Row
            if (uiState.devices.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (uiState.devices.isNotEmpty()) {
                            IconButton(onClick = { 
                                viewModel.copyResultsToClipboard(context)
                                Toast.makeText(context, "Results copied", Toast.LENGTH_SHORT).show()
                            }, modifier = Modifier.size(32.dp)) {
                                Icon(AppIcons.contentCopy(), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { viewModel.shareResults(context) }, modifier = Modifier.size(32.dp)) {
                                Icon(AppIcons.share(), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                            if (uiState.devices.any { !it.isOnline }) {
                                IconButton(onClick = { 
                                    viewModel.clearHistory()
                                    Toast.makeText(context, "LAN history cleared", Toast.LENGTH_SHORT).show()
                                }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.DeleteSweep, "Clear History", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                            VerticalDivider(modifier = Modifier.height(20.dp).padding(horizontal = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        }

                        val listedCount = uiState.devices.count { it.riskScore > 0 }
                        
                        LanFilterChip("All (${uiState.devices.size})", uiState.filter == LanFilter.ALL) { viewModel.updateFilter(LanFilter.ALL) }
                        SummaryMiniChip("${uiState.devices.count { it.isOnline }} Clean", Color(0xFF10B981))
                        SummaryMiniChip("${uiState.devices.count { it.riskScore > 0 }} Listed", if (listedCount > 0) MaterialTheme.colorScheme.error else TextGray)
                        
                        LanFilterChip("Phone", uiState.filter == LanFilter.PHONE) { viewModel.updateFilter(LanFilter.PHONE) }
                        LanFilterChip("PC", uiState.filter == LanFilter.PC) { viewModel.updateFilter(LanFilter.PC) }
                        LanFilterChip("Router", uiState.filter == LanFilter.ROUTER) { viewModel.updateFilter(LanFilter.ROUTER) }
                        LanFilterChip("IoT", uiState.filter == LanFilter.IOT) { viewModel.updateFilter(LanFilter.IOT) }
                        LanFilterChip("Bluetooth", uiState.filter == LanFilter.BLUETOOTH) { viewModel.updateFilter(LanFilter.BLUETOOTH) }
                        LanFilterChip("Online", uiState.filter == LanFilter.ONLINE) { viewModel.updateFilter(LanFilter.ONLINE) }
                    }
                }
            }

            items(uiState.filteredDevices) { device ->
                DeviceProCard(
                    device = device,
                    vendorMap = uiState.vendorMap,
                    isScanningPorts = uiState.portScanningDevices.contains(device.ip),
                    onPing = { onNavigateToPing(it) },
                    onPorts = { viewModel.togglePortScan(it) },
                    onInfo = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://${device.ip}"))
                        context.startActivity(intent)
                    },
                    onIdentify = { 
                        selectedDeviceToIdentify = it
                        showIdentifyDialog = true
                    },
                    onDelete = { viewModel.deleteDevice(it.mac) }
                )
            }
        }
    }
}

@Composable
fun LanNetworkSummaryCard(summary: NetworkSummaryInfo) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons.lan(), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${summary.ssid} • ${summary.aliveCount} Alive / ${summary.scannedCount} Scanned",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryItem("Gateway", summary.gateway)
                SummaryItem("Subnet Mask", summary.subnetMask)
                SummaryItem("DNS Server", summary.dnsServer)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryItem("My IP", summary.myIp)
                SummaryItem("My MAC", summary.myMac)
                SummaryItem("BSSID", summary.bssid)
            }
        }
    }
}

@Composable
fun SummaryItem(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        Text(value, style = MaterialTheme.typography.bodySmall, fontFamily = TechMonospace, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
    }
}

@Composable
fun InputField(value: String, placeholder: String, onValueChange: (String) -> Unit) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        singleLine = true
    )
}

@Composable
fun SummaryMiniChip(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f)),
        modifier = Modifier.height(32.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(6.dp))
            Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DeviceProCard(
    device: Device,
    vendorMap: Map<String, String>,
    isScanningPorts: Boolean,
    onPing: (String) -> Unit,
    onPorts: (String) -> Unit,
    onInfo: (String) -> Unit,
    onIdentify: (Device) -> Unit,
    onDelete: (Device) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    val hostnameDisplay = if (device.hostname.isBlank() || device.hostname == "Unknown" || device.hostname == "Unknown Host" || device.hostname == "Probing...") "—" else device.hostname

    val osGuess = when {
        device.os != "Unknown" -> device.os
        device.openPorts.contains(445) || device.openPorts.contains(139) -> "Windows"
        device.openPorts.contains(62078) || device.vendor.contains("Apple", true) -> "iOS / macOS"
        device.vendor.contains("Samsung", true) || device.vendor.contains("Xiaomi", true) || device.vendor.contains("Android", true) -> "Android OS"
        device.vendor.contains("Cisco", true) || device.vendor.contains("MikroTik", true) || device.vendor.contains("TP-Link", true) -> "RouterOS / Embedded"
        else -> "Linux / Embedded"
    }

    val ipStatus = if (device.isGateway || device.ip.endsWith(".1") || device.ip.endsWith(".254")) "Static IP" else "DHCP"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (expanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.clickable { expanded = !expanded }.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Left: Device Icon
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(getProDeviceIcon(device.deviceType)),
                        contentDescription = null,
                        tint = if (device.isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (device.isOnline) Color(0xFF10B981) else Color.Gray)
                    )
                }

                Spacer(Modifier.width(16.dp))

                // Center: Details
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(device.ip, style = MaterialTheme.typography.titleMedium, fontFamily = TechMonospace, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                        if (device.isThisDevice) {
                            Spacer(Modifier.width(6.dp))
                            Surface(color = Color(0xFF10B981).copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                Text("THIS DEVICE", color = Color(0xFF10B981), fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }
                        if (device.isGateway) {
                            Spacer(Modifier.width(6.dp))
                            Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                Text("GW", color = MaterialTheme.colorScheme.primary, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }
                        if (device.isRandomMac) {
                            Spacer(Modifier.width(6.dp))
                            Surface(color = Color(0xFFFBBF24).copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                Text("PRV", color = Color(0xFFFBBF24), fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }
                        if (device.isBluetoothNearby || device.bluetoothName != null) {
                            Spacer(Modifier.width(6.dp))
                            Surface(color = SoftTechSecondary.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
                                    Icon(AppIcons.bluetooth(), contentDescription = null, tint = SoftTechSecondary, modifier = Modifier.size(10.dp))
                                    Spacer(Modifier.width(2.dp))
                                    Text("BT NEARBY", color = SoftTechSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        if (!device.isOnline) {
                            Spacer(Modifier.width(6.dp))
                            Surface(color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                Text("SAVED", color = MaterialTheme.colorScheme.tertiary, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }
                    }
                    
                    val displayVendor = when {
                        device.isThisDevice -> device.vendor
                        device.vendor != "Unknown" && device.vendor != "Unknown Vendor" -> device.vendor
                        else -> "Unknown Vendor"
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = displayVendor, 
                            style = MaterialTheme.typography.bodySmall, 
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (vendorMap.containsKey(device.mac)) {
                            Spacer(Modifier.width(4.dp))
                            Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                                Text("CUSTOM", color = MaterialTheme.colorScheme.primary, fontSize = 8.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }
                    }

                    Text(
                        text = hostnameDisplay, 
                        style = MaterialTheme.typography.labelSmall, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Right: Latency Badge
                Column(horizontalAlignment = Alignment.End) {
                    val (latencyColor, latencyText) = when {
                        !device.isOnline -> Pair(MaterialTheme.colorScheme.error, "Offline")
                        device.isThisDevice -> Pair(Color(0xFF10B981), "Local")
                        device.latencyMs < 100L -> Pair(Color(0xFF10B981), "${device.latencyMs}ms")
                        device.latencyMs in 100L..300L -> Pair(Color(0xFFFBBF24), "${device.latencyMs}ms")
                        else -> Pair(Color(0xFFFF8C42), "${device.latencyMs}ms")
                    }

                    Surface(
                        color = latencyColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, latencyColor.copy(alpha = 0.3f)),
                        modifier = Modifier.clickable {
                            if (!device.isOnline || device.latencyMs == 0L) {
                                Toast.makeText(context, "Stored device info from recent scan", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text(
                            text = if (!device.isOnline) "Last Active (Cached)" else latencyText,
                            color = latencyColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    ResultRow("Hostname", hostnameDisplay, isTechnicalData = true)
                    ResultRow("MAC Address", device.mac, isTechnicalData = true)
                    ResultRow("Device Type", "${device.deviceType} (${device.deviceCategory})")
                    ResultRow("OS Guess", osGuess)
                    ResultRow("IP Status", ipStatus)
                    ResultRow("First Seen", dateFormat.format(Date(device.firstSeen)))
                    ResultRow("Last Seen", dateFormat.format(Date(device.lastSeen)))

                    if (device.isBluetoothNearby || device.bluetoothName != null) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Text("Bluetooth Profile", style = MaterialTheme.typography.labelSmall, color = SoftTechSecondary, fontWeight = FontWeight.Bold)
                        ResultRow("Bluetooth Name", device.bluetoothName ?: "—", isTechnicalData = true)
                        if (!device.bluetoothMac.isNullOrBlank()) {
                            ResultRow("Bluetooth MAC", device.bluetoothMac, isTechnicalData = true)
                        }
                        if (device.bluetoothRssi != null) {
                            ResultRow("Signal Strength", "${device.bluetoothRssi} dBm", isTechnicalData = true)
                        }
                    }

                    // Open Ports Section
                    if (device.hasPerformedPortScan || device.openPorts.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Text("Open Ports", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        
                        if (device.openPorts.isEmpty()) {
                            Text("No open ports detected", color = MaterialTheme.colorScheme.error, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        } else {
                            val maxVisiblePorts = 5
                            val visiblePorts = device.openPorts.take(maxVisiblePorts)
                            val remainingPorts = device.openPorts.size - maxVisiblePorts

                            @OptIn(ExperimentalLayoutApi::class)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                visiblePorts.forEach { port ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                    ) {
                                        Text(
                                            text = port.toString(),
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 11.sp,
                                            fontFamily = TechMonospace,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                if (remainingPorts > 0) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "+$remainingPorts more",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionButton(AppIcons.networkCheck(), "PING", Modifier.weight(1f)) { onPing(device.ip) }
                        
                        Button(
                            onClick = { onPorts(device.ip) },
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isScanningPorts) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            contentPadding = PaddingValues(0.dp),
                            enabled = !isScanningPorts
                        ) {
                            if (isScanningPorts) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                            } else {
                                Icon(AppIcons.search(), null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("PORTS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        ActionButton(AppIcons.edit(), "RENAME", Modifier.weight(1f)) { onIdentify(device) }
                        
                        IconButton(
                            onClick = { onDelete(device) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete device", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionButton(painter: Painter, label: String, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface),
        contentPadding = PaddingValues(0.dp)
    ) {
        Icon(painter, null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun LanFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 12.sp) },
        shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = null
    )
}

fun getProDeviceIcon(deviceType: String): Int {
    val t = deviceType.lowercase()
    return when {
        t.contains("router") || t.contains("gateway") -> R.drawable.ic_router
        t.contains("iphone") -> R.drawable.ic_phone_iphone
        t.contains("android") || t.contains("phone") || t.contains("mobile") -> R.drawable.ic_phone_android
        t.contains("mac") -> R.drawable.ic_desktop_mac
        t.contains("pc") || t.contains("computer") || t.contains("windows") -> R.drawable.ic_desktop_windows
        t.contains("laptop") -> R.drawable.ic_laptop
        t.contains("printer") -> R.drawable.ic_print
        t.contains("tv") -> R.drawable.ic_tv
        t.contains("camera") || t.contains("cctv") || t.contains("nvr") || t.contains("dvr") -> R.drawable.ic_security
        t.contains("iot") || t.contains("smart") || t.contains("embedded") -> R.drawable.ic_memory
        t.contains("watch") || t.contains("wearable") -> R.drawable.ic_watch
        else -> R.drawable.ic_devices
    }
}
