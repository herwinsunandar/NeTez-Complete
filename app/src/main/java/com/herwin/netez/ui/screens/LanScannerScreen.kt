package com.herwin.netez.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.network.Device
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
    onNavigateToPortScan: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

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
                            Icon(Icons.Default.Stop, null)
                            Spacer(Modifier.width(8.dp))
                            Text("STOP", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Radar, null)
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
                        Text(
                            "Scanning... ${(uiState.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextGray
                        )
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
            if (uiState.devices.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LanFilterChip("All (${uiState.devices.size})", uiState.filter == LanFilter.ALL) { viewModel.updateFilter(LanFilter.ALL) }
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
                    onPing = { onNavigateToPing(it) },
                    onPorts = { viewModel.togglePortScan(it) },
                    onInfo = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://${device.ip}"))
                        context.startActivity(intent)
                    }
                )
            }

            if (uiState.devices.isNotEmpty() && !uiState.isScanning) {
                item {
                    com.herwin.netez.ui.components.ResultFooter(
                        fullTextToCopy = uiState.devices.joinToString("\n") { "${it.ip} - ${it.hostname} (${it.mac})" },
                        onCopyAll = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("LAN Devices", uiState.devices.joinToString("\n") { "${it.ip} - ${it.hostname}" }))
                            android.widget.Toast.makeText(context, "Device list copied", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                }
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
fun DeviceProCard(
    device: Device,
    onPing: (String) -> Unit,
    onPorts: (String) -> Unit,
    onInfo: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = SoftDarkBg,
        border = if (expanded) borderStroke(1.dp, Color(0xFF6C8CFF).copy(alpha = 0.3f)) else null
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
                        imageVector = getProDeviceIcon(device.deviceType),
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
                        if (device.isGateway) {
                            Spacer(Modifier.width(8.dp))
                            Surface(color = Color(0xFF6C8CFF).copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                Text("GW", color = Color(0xFF6C8CFF), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                            }
                        }
                    }
                    Text(device.hostname, style = MaterialTheme.typography.bodySmall, color = TextGray)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(getOsIcon(device.os, device.deviceType), null, tint = TextGray, modifier = Modifier.size(12.dp))
                        Text(
                            text = "${device.os} • ${device.connectionType}${device.signalStrength?.let { " • $it" } ?: ""}", 
                            style = MaterialTheme.typography.labelSmall, 
                            color = TextGray, 
                            fontSize = 9.sp
                        )
                    }
                }

                // Right: Latency + Connection Icon
                Column(horizontalAlignment = Alignment.End) {
                    Icon(
                        imageVector = if (device.connectionType == "WiFi") Icons.Default.Wifi else Icons.Default.SettingsEthernet,
                        contentDescription = null,
                        tint = TextGray.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${device.latencyMs}ms",
                        color = when {
                            !device.isOnline -> Color.Gray
                            device.latencyMs < 50 -> Color(0xFF10B981)
                            device.latencyMs < 150 -> Color(0xFFF59E0B)
                            else -> Color(0xFFEF4444)
                        },
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                    if (device.openPorts.isNotEmpty()) {
                        Surface(
                            color = Color(0xFF10B981).copy(alpha = 0.1f),
                            shape = CircleShape,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                "${device.openPorts.size} ports",
                                color = Color(0xFF10B981),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
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
                    
                    com.herwin.netez.ui.components.ResultRow("First Discovered", dateFormat.format(Date(device.firstSeen)))
                    com.herwin.netez.ui.components.ResultRow("Last Seen", timeFormat.format(Date(device.lastSeen)))
                    com.herwin.netez.ui.components.ResultRow("TTL", device.ttl.toString())

                    if (device.openPorts.isNotEmpty()) {
                        Column {
                            Text("Open Ports", style = MaterialTheme.typography.labelSmall, color = TextGray)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                                device.openPorts.forEach { port ->
                                    Surface(color = Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(4.dp)) {
                                        Text(port.toString(), color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ActionButton(Icons.Default.NetworkCheck, "PING", Modifier.weight(1f)) { onPing(device.ip) }
                        ActionButton(Icons.Default.Search, "PORTS", Modifier.weight(1f)) { onPorts(device.ip) }
                        ActionButton(Icons.Default.Info, "INFO", Modifier.weight(1f)) { onInfo(device.ip) }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f), contentColor = Color.White),
        contentPadding = PaddingValues(0.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(16.dp))
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

fun getProDeviceIcon(type: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (type) {
        "Router" -> Icons.Default.Router
        "iPhone" -> Icons.Default.PhoneIphone
        "Android" -> Icons.Default.PhoneAndroid
        "PC" -> Icons.Default.Laptop
        "Mac" -> Icons.Default.DesktopMac
        "Printer" -> Icons.Default.Print
        "Smart TV" -> Icons.Default.Tv
        "IoT" -> Icons.Default.Sensors
        else -> Icons.Default.Devices
    }
}

fun getOsIcon(os: String, type: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when {
        type == "iPhone" || type == "Mac" -> Icons.Default.PhoneIphone
        os.contains("Windows") -> Icons.Default.DesktopWindows
        os.contains("Linux") || os.contains("Android") -> Icons.Default.Terminal
        os.contains("Cisco") -> Icons.Default.SettingsInputComponent
        else -> Icons.Default.Info
    }
}

fun borderStroke(width: androidx.compose.ui.unit.Dp, color: Color) = androidx.compose.foundation.BorderStroke(width, color)
