package com.herwin.netez.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.herwin.netez.network.BleDevice
import com.herwin.netez.network.BleDeviceType
import com.herwin.netez.ui.theme.PremiumNavy
import com.herwin.netez.ui.theme.TextGray
import com.herwin.netez.ui.viewmodels.BleFilter
import com.herwin.netez.ui.viewmodels.BleScannerViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BleScannerScreen(viewModel: BleScannerViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var selectedDevice by remember { mutableStateOf<BleDevice?>(null) }
    val sheetState = rememberModalBottomSheetState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (!granted) {
            Toast.makeText(context, "Bluetooth permissions required", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        // 1. Filter Field & Scan Button
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White.copy(alpha = 0.05f),
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = { Text("Device Name", color = TextGray) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White,
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        focusedBorderColor = Color(0xFF6C8CFF)
                    ),
                    singleLine = true
                )
                Button(
                    onClick = { viewModel.toggleScan() },
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6C8CFF)
                    )
                ) {
                    if (uiState.isScanning) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("SCAN", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        // 2. Summary Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary))
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Found ${uiState.devices.size} devices • Scanning ${String.format(Locale.US, "%.1fs", uiState.scanTimeMs / 1000f)} • BLE 5.0",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (uiState.devices.any { it.isCached }) {
                    IconButton(
                        onClick = {
                            viewModel.clearHistory()
                            Toast.makeText(context, "History cleared", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear History", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // 3. Filter Chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BleFilterChip("All ${uiState.devices.size}", uiState.filter == BleFilter.ALL) { viewModel.updateFilter(BleFilter.ALL) }
            BleFilterChip("Paired", uiState.filter == BleFilter.PAIRED) { viewModel.updateFilter(BleFilter.PAIRED) }
            BleFilterChip("Nearby", uiState.filter == BleFilter.NEARBY) { viewModel.updateFilter(BleFilter.NEARBY) }
        }

        // 4. Result Cards
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(uiState.filteredDevices) { device ->
                BleDeviceCard(device) { selectedDevice = device }
            }
        }
    }

    if (selectedDevice != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedDevice = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            BleDetailSheet(
                device = selectedDevice!!,
                context = context,
                onPair = { 
                    viewModel.pairDevice(selectedDevice?.nativeDevice)
                    selectedDevice = null
                },
                onDelete = {
                    val mac = selectedDevice!!.mac
                    viewModel.deleteDevice(mac)
                    selectedDevice = null
                    Toast.makeText(context, "Removed from history", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
fun BleFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
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

@Composable
fun BleDeviceCard(device: BleDevice, onClick: () -> Unit) {
    val cardBg = MaterialTheme.colorScheme.surface
    val accentColor = if (device.deviceType == BleDeviceType.SMARTWATCH) Color(0xFFA855F7) else MaterialTheme.colorScheme.primary

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = cardBg,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getBleIcon(device.deviceType),
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(26.dp)
                )
                if (device.rssi > -100 && !device.isCached) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary)
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (device.isPaired || device.isEverConnected) {
                        Spacer(Modifier.width(6.dp))
                        Surface(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                            Text("PAIRED", color = MaterialTheme.colorScheme.secondary, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    } else if (device.isCached) {
                        Spacer(Modifier.width(6.dp))
                        Surface(color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                            Text("SAVED", color = MaterialTheme.colorScheme.tertiary, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    }
                }
                Text(
                    text = "${device.mac} • ${device.vendor}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (device.isCached) "Saved in History" else "${device.rssi} dBm • ${String.format(Locale.US, "%.1fm", device.distance)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                if (!device.isCached) {
                    SignalBars(device.rssi)
                } else {
                    Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onClick,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (device.isCached) "VIEW" else "CONNECT", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun SignalBars(rssi: Int) {
    val level = when {
        rssi > -60 -> 4
        rssi > -70 -> 3
        rssi > -80 -> 2
        else -> 1
    }
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
        for (i in 1..4) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((i * 4).dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (i <= level) Color(0xFF10B981) else Color.White.copy(alpha = 0.1f))
            )
        }
    }
}

@Composable
fun BleDetailSheet(device: BleDevice, context: Context, onPair: () -> Unit, onDelete: () -> Unit) {
    Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                Icon(getBleIcon(device.deviceType), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(device.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(device.mac, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }

        if (device.batteryLevel != null) {
            DetailItem("Battery Level", "${device.batteryLevel}%", Icons.Default.BatteryChargingFull)
        }
        
        if (device.uuids.isNotEmpty()) {
            Text("SERVICES UUID", style = MaterialTheme.typography.labelSmall, color = TextGray, letterSpacing = 1.sp)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                device.uuids.take(3).forEach { uuid ->
                    Text(uuid, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f), fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onPair, 
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                enabled = !device.isPaired
            ) {
                Text(if (device.isPaired) "PAIRED" else "PAIR")
            }
            OutlinedButton(onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("MAC", device.mac))
                Toast.makeText(context, "MAC Copied", Toast.LENGTH_SHORT).show()
            }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                Text("COPY MAC")
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun DetailItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, modifier = Modifier.weight(1f), color = TextGray)
        Text(value, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

fun getBleIcon(type: BleDeviceType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (type) {
        BleDeviceType.HEADPHONES -> Icons.Default.Headset
        BleDeviceType.SMARTWATCH -> Icons.Default.Watch
        BleDeviceType.PHONE -> Icons.Default.PhoneIphone
        else -> Icons.Default.Bluetooth
    }
}
