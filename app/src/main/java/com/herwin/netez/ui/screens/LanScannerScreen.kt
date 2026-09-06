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
import com.herwin.netez.ui.theme.PremiumNavy
import com.herwin.netez.ui.theme.SoftDarkBg
import com.herwin.netez.ui.theme.TextGray
import com.herwin.netez.ui.viewmodels.LanFilter
import com.herwin.netez.ui.viewmodels.LanScannerViewModel
import com.herwin.netez.ui.viewmodels.ScanMode
import java.text.SimpleDateFormat
import java.util.*

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
    
    var showIdentifyDialog by remember { mutableStateOf(false) }
    var selectedMacToIdentify by remember { mutableStateOf("") }

    if (showIdentifyDialog) {
        var customLabel by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showIdentifyDialog = false },
            containerColor = SoftDarkBg,
            title = { Text("Identify Device", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Manually label this device (MAC: $selectedMacToIdentify)", color = TextGray, fontSize = 12.sp)
                    OutlinedTextField(
                        value = customLabel,
                        onValueChange = { customLabel = it },
                        placeholder = { Text("e.g. Living Room TV", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedTextColor = Color.White,
                            focusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF6C8CFF)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveVendorOverride(selectedMacToIdentify, customLabel)
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
            .background(PremiumNavy)
    ) {
        // 1. Top Input Section
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SoftDarkBg,
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Segmented Control
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = uiState.mode == ScanMode.SINGLE,
                        onClick = { viewModel.updateMode(ScanMode.SINGLE) },
                        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                        label = { Text("Single", fontSize = 12.sp) }
                    )
                    SegmentedButton(
                        selected = uiState.mode == ScanMode.RANGE,
                        onClick = { viewModel.updateMode(ScanMode.RANGE) },
                        shape = RoundedCornerShape(0.dp),
                        label = { Text("Range", fontSize = 12.sp) }
                    )
                    SegmentedButton(
                        selected = uiState.mode == ScanMode.CIDR,
                        onClick = { viewModel.updateMode(ScanMode.CIDR) },
                        shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                        label = { Text("CIDR", fontSize = 12.sp) }
                    )
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
                            containerColor = if (uiState.isScanning) Color(0xFFEF4444) else Color(0xFF6C8CFF)
                        )
                    ) {
                        if (uiState.isScanning) {
                            Icon(AppIcons.stop(), null)
                            Spacer(Modifier.width(8.dp))
                            Text("STOP", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(AppIcons.scan(), null)
                            Spacer(Modifier.width(8.dp))
                            Text("SCAN", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (uiState.isScanning || uiState.progress > 0) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LinearProgressIndicator(
                            progress = { uiState.progress },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            color = Color(0xFF6C8CFF),
                            trackColor = Color.White.copy(alpha = 0.05f)
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                uiState.progressText.ifEmpty { "Scanning... ${(uiState.progress * 100).toInt()}%" },
                                style = MaterialTheme.typography.labelSmall,
                                color = TextGray
                            )
                            uiState.ispInfo?.let { 
                                Text(it, style = MaterialTheme.typography.labelSmall, color = Color(0xFF6C8CFF))
                            }
                        }
                    }
                } else {
                    // Show ISP Info when not scanning
                    uiState.ispInfo?.let {
                        Surface(
                            color = Color(0xFF6C8CFF).copy(alpha = 0.05f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(AppIcons.public(), null, tint = Color(0xFF6C8CFF), modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(it, color = TextGray, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // 2. Filter & Results
        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
        ) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { onNavigateToWifiAudit() },
                    color = Color(0xFFEF4444).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.2f))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(AppIcons.warning(), null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("⚠️ WiFi tidak aman - Tap untuk audit keamanan", color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            if (uiState.devices.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Action Icons (Copy & Share)
                        if (uiState.devices.isNotEmpty()) {
                            IconButton(onClick = { 
                                viewModel.copyResultsToClipboard(context)
                                Toast.makeText(context, "Results copied", Toast.LENGTH_SHORT).show()
                            }, modifier = Modifier.size(32.dp)) {
                                Icon(AppIcons.contentCopy(), null, tint = Color(0xFF6C8CFF), modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { viewModel.shareResults(context) }, modifier = Modifier.size(32.dp)) {
                                Icon(AppIcons.share(), null, tint = Color(0xFF6C8CFF), modifier = Modifier.size(20.dp))
                            }
                            VerticalDivider(modifier = Modifier.height(20.dp).padding(horizontal = 4.dp), color = Color.White.copy(alpha = 0.1f))
                        }

                        val listedCount = uiState.devices.count { it.riskScore > 0 }
                        
                        LanFilterChip("All (${uiState.devices.size})", uiState.filter == LanFilter.ALL) { viewModel.updateFilter(LanFilter.ALL) }
                        
                        SummaryMiniChip("${uiState.devices.count { it.isOnline }} Clean", Color(0xFF10B981))
                        SummaryMiniChip("${uiState.devices.count { it.riskScore > 0 }} Listed", if (listedCount > 0) Color(0xFFEF4444) else TextGray)
                        
                        LanFilterChip("Phone", uiState.filter == LanFilter.PHONE) { viewModel.updateFilter(LanFilter.PHONE) }
                        LanFilterChip("PC", uiState.filter == LanFilter.PC) { viewModel.updateFilter(LanFilter.PC) }
                        LanFilterChip("Router", uiState.filter == LanFilter.ROUTER) { viewModel.updateFilter(LanFilter.ROUTER) }
                        LanFilterChip("IoT", uiState.filter == LanFilter.IOT) { viewModel.updateFilter(LanFilter.IOT) }
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
                        selectedMacToIdentify = it
                        showIdentifyDialog = true
                    }
                )
            }
        }
    }
}

@Composable
fun InputField(value: String, placeholder: String, onValueChange: (String) -> Unit) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = TextGray) },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White.copy(alpha = 0.05f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
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
    onIdentify: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = SoftDarkBg,
        border = if (expanded) BorderStroke(1.dp, Color(0xFF6C8CFF).copy(alpha = 0.3f)) else null
    ) {
        Column(modifier = Modifier.clickable { expanded = !expanded }.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Left: Icon + Status
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(getProDeviceIcon(device.deviceType)),
                        contentDescription = null,
                        tint = if (device.isOnline) Color.White else TextGray,
                        modifier = Modifier.size(28.dp)
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (device.isOnline) Color(0xFF10B981) else Color.Gray)
                            .border(2.dp, SoftDarkBg, CircleShape)
                    )
                }

                Spacer(Modifier.width(16.dp))

                // Center: Details
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(device.ip, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        if (device.isThisDevice) {
                            Spacer(Modifier.width(8.dp))
                            Surface(color = Color(0xFF10B981).copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                Text("THIS DEVICE", color = Color(0xFF10B981), fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }
                        if (device.isGateway) {
                            Spacer(Modifier.width(8.dp))
                            Surface(color = Color(0xFF6C8CFF).copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                Text("GW", color = Color(0xFF6C8CFF), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                            }
                        }
                        if (device.isRandomMac) {
                            Spacer(Modifier.width(8.dp))
                            Surface(color = Color(0xFFFBBF24).copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                Text("PRV", color = Color(0xFFFBBF24), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
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
                            color = if (displayVendor.contains("Unknown")) TextGray else Color(0xFF6C8CFF),
                            fontWeight = if (displayVendor.contains("Unknown")) FontWeight.Normal else FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (vendorMap.containsKey(device.mac)) {
                            Spacer(Modifier.width(4.dp))
                            Surface(color = Color(0xFF6C8CFF).copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                                Text("CUSTOM", color = Color(0xFF6C8CFF), fontSize = 8.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }
                        if (device.hostnameSource == "mDNS") {
                            Spacer(Modifier.width(6.dp))
                            Icon(AppIcons.link(), null, tint = Color(0xFF6C8CFF), modifier = Modifier.size(10.dp))
                        }
                    }
                    
                    val displayDetails = listOfNotNull(
                        if (device.deviceCategory != "Unknown") device.deviceCategory else null,
                        if (device.deviceType != "Unknown") device.deviceType else null
                    ).joinToString(" • ")

                    val displayHostname = when {
                        device.isThisDevice -> device.hostname
                        device.hostname != "Unknown" && displayDetails.isNotEmpty() -> "${device.hostname} • $displayDetails"
                        device.hostname != "Unknown" -> device.hostname
                        displayDetails.isNotEmpty() -> displayDetails
                        else -> "Unknown Host"
                    }
                    
                    Text(
                        text = displayHostname, 
                        style = MaterialTheme.typography.labelSmall, 
                        color = TextGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Right: Latency + Connection Icon
                Column(horizontalAlignment = Alignment.End) {
                    Icon(
                        painter = painterResource(if (device.connectionType == "WiFi") R.drawable.ic_wifi else R.drawable.ic_settings_ethernet),
                        contentDescription = null,
                        tint = TextGray.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    
                    val latencyColor = when {
                        !device.isOnline -> Color.Gray
                        device.isThisDevice || device.latencyMs == 0L || device.latencyMs > 800 -> Color(0xFF10B981)
                        device.latencyMs < 300 -> Color(0xFF10B981)
                        device.latencyMs <= 800 -> Color(0xFFFBBF24)
                        else -> Color(0xFFEF4444)
                    }
                    
                    val latencyText = when {
                        !device.isOnline -> "Offline"
                        device.isThisDevice || device.latencyMs == 0L || device.latencyMs > 800 -> "Cached"
                        device.hostnameSource == "NetBIOS" -> "NB: ${device.latencyMs}ms"
                        else -> "${device.latencyMs}ms"
                    }

                    Surface(
                        color = latencyColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, latencyColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = latencyText,
                            color = latencyColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                    ResultRow("MAC Address", device.mac)
                    if (device.isRandomMac) {
                        Text("Locally Administered Address (Randomized)", color = Color(0xFFFBBF24), fontSize = 10.sp, modifier = Modifier.padding(start = 16.dp))
                    }
                    if (!device.isOnline) {
                        val diff = System.currentTimeMillis() - device.lastSeen
                        val timeAgo = when {
                            diff < 60000 -> "Just now"
                            diff < 3600000 -> "${diff / 60000}m ago"
                            else -> dateFormat.format(Date(device.lastSeen))
                        }
                        ResultRow("Last Seen", timeAgo)
                    } else {
                        ResultRow("Last Active", timeFormat.format(Date(device.lastSeen)))
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ActionButton(AppIcons.networkCheck(), "PING", Modifier.weight(1f)) { onPing(device.ip) }
                        
                        Button(
                            onClick = { onPorts(device.ip) },
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isScanningPorts) Color(0xFF6C8CFF).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(0.dp),
                            enabled = !isScanningPorts
                        ) {
                            if (isScanningPorts) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                            } else {
                                Icon(AppIcons.search(), null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("PORTS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        ActionButton(AppIcons.person(), "IDENTIFY", Modifier.weight(1f)) { onIdentify(device.mac) }
                    }

                    if (device.hasPerformedPortScan || device.openPorts.isNotEmpty()) {
                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                        Text("Open Ports", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        
                        if (device.openPorts.isEmpty()) {
                            Text("No open ports detected", color = Color(0xFFEF4444).copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        } else {
                            @OptIn(ExperimentalLayoutApi::class)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                device.openPorts.forEach { port ->
                                    Surface(
                                        color = Color(0xFF6C8CFF).copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFF6C8CFF).copy(alpha = 0.2f))
                                    ) {
                                        Text(
                                            text = port.toString(),
                                            color = Color(0xFF6C8CFF),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
}

@Composable
fun ActionButton(painter: Painter, label: String, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f), contentColor = Color.White),
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
            selectedContainerColor = Color(0xFF6C8CFF),
            selectedLabelColor = Color.White,
            containerColor = Color.White.copy(alpha = 0.05f),
            labelColor = TextGray
        ),
        border = null
    )
}

fun getProDeviceIcon(type: String): Int {
    return when (type) {
        "Router" -> R.drawable.ic_router
        "iPhone" -> R.drawable.ic_phone_iphone
        "Android" -> R.drawable.ic_phone_android
        "PC" -> R.drawable.ic_laptop
        "Mac" -> R.drawable.ic_desktop_mac
        "Printer" -> R.drawable.ic_print
        "Smart TV" -> R.drawable.ic_tv
        "IoT" -> R.drawable.ic_sensors
        else -> R.drawable.ic_devices
    }
}
