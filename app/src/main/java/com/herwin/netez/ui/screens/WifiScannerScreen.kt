package com.herwin.netez.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.R
import com.herwin.netez.data.wifi.VendorDatabase
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.theme.PremiumNavy
import com.herwin.netez.ui.theme.SoftDarkBg
import com.herwin.netez.ui.theme.TextGray
import com.herwin.netez.ui.viewmodels.WifiFilter
import com.herwin.netez.ui.viewmodels.WifiScannerState
import com.herwin.netez.ui.viewmodels.WifiScannerViewModel
import com.herwin.netez.utils.NetworkUtils
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiScannerScreen(
    viewModel: WifiScannerViewModel = viewModel(),
    onNavigateToDetail: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    // Permission State
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    var hasNearbyPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasLocationPermission = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNearbyPermission = perms[Manifest.permission.NEARBY_WIFI_DEVICES] == true
        }
        if (hasLocationPermission && hasNearbyPermission) {
            viewModel.refresh()
        }
    }

    // WiFi & Location Enabled State
    val wifiManager = remember { context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager }
    val locationManager = remember { context.getSystemService(Context.LOCATION_SERVICE) as LocationManager }
    var isWifiEnabled by remember { mutableStateOf(wifiManager.isWifiEnabled) }
    var isLocationEnabled by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                locationManager.isLocationEnabled
            } else {
                @Suppress("DEPRECATION")
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            }
        )
    }
    
    // Monitor system settings
    LaunchedEffect(Unit) {
        while(true) {
            isWifiEnabled = wifiManager.isWifiEnabled
            isLocationEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                locationManager.isLocationEnabled
            } else {
                @Suppress("DEPRECATION")
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            }
            delay(2000)
        }
    }

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
                    Text("Manually label this network (BSSID: $selectedMacToIdentify)", color = TextGray, fontSize = 12.sp)
                    OutlinedTextField(
                        value = customLabel,
                        onValueChange = { customLabel = it },
                        placeholder = { Text("e.g. My Home Router", color = TextGray) },
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

    LaunchedEffect(hasLocationPermission, hasNearbyPermission, isWifiEnabled, isLocationEnabled) {
        if (hasLocationPermission && hasNearbyPermission && isWifiEnabled && isLocationEnabled) {
            viewModel.refresh()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PremiumNavy)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        if (!hasLocationPermission || !hasNearbyPermission) {
            PermissionRequiredUI {
                val perms = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    perms.add(Manifest.permission.NEARBY_WIFI_DEVICES)
                }
                permissionLauncher.launch(perms.toTypedArray())
            }
        } else if (!isWifiEnabled) {
            WifiDisabledUI {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.startActivity(Intent(Settings.Panel.ACTION_WIFI))
                } else {
                    @Suppress("DEPRECATION")
                    wifiManager.isWifiEnabled = true
                }
            }
        } else if (!isLocationEnabled) {
            LocationDisabledUI {
                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
        } else {
            // 1. Refresh & Filters Area
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val allCount = uiState.scanResults.size
                    val freq5Count = uiState.scanResults.count { it.frequency in 4900..5900 }
                    val freq24Count = uiState.scanResults.count { it.frequency in 2400..2500 }

                    WifiFilterChip("All $allCount", uiState.filter == WifiFilter.ALL) { viewModel.updateFilter(WifiFilter.ALL) }
                    WifiFilterChip("5 GHz $freq5Count", uiState.filter == WifiFilter.FREQ_5) { viewModel.updateFilter(WifiFilter.FREQ_5) }
                    WifiFilterChip("2.4 GHz $freq24Count", uiState.filter == WifiFilter.FREQ_24) { viewModel.updateFilter(WifiFilter.FREQ_24) }
                }
                
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = { viewModel.refresh() },
                    enabled = !uiState.isScanning,
                    modifier = Modifier.background(Color.White.copy(alpha = 0.05f), CircleShape)
                ) {
                    if (uiState.isScanning) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Icon(AppIcons.refresh(), null, tint = Color.White)
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

            // 3. Results List
            if (uiState.scanResults.isEmpty() && !uiState.isScanning) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(AppIcons.wifiOff(), null, tint = TextGray, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("No WiFi networks found", color = TextGray)
                        Text("Make sure Location is ON", color = TextGray, fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(uiState.filteredResults) { result ->
                        @Suppress("DEPRECATION")
                        WifiCard(
                            result = result,
                            uiState = uiState,
                            isConnected = result.BSSID == uiState.currentBssid,
                            linkSpeed = if (result.BSSID == uiState.currentBssid) uiState.currentLinkSpeed else 0,
                            isp = if (result.BSSID == uiState.currentBssid) uiState.connectedIsp else null,
                            publicIp = if (result.BSSID == uiState.currentBssid) uiState.connectedPublicIp else null,
                            onClick = { onNavigateToDetail(result.BSSID) },
                            onIdentify = { mac ->
                                selectedMacToIdentify = mac
                                showIdentifyDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionRequiredUI(onRequest: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        color = SoftDarkBg,
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(AppIcons.locationOn(), null, tint = Color(0xFF6C8CFF), modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(16.dp))
            Text("Location Permission Required", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "Android requires Location permission to perform WiFi scanning. Your location will not be shared.",
                color = TextGray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onRequest,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C8CFF))
            ) {
                Text("GRANT PERMISSION", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun WifiDisabledUI(onEnable: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        color = SoftDarkBg,
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(AppIcons.wifiOff(), null, tint = Color(0xFFEF4444), modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(16.dp))
            Text("WiFi is Disabled", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "Please enable WiFi to see nearby networks and analyzer signals.",
                color = TextGray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onEnable,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C8CFF))
            ) {
                Text("ENABLE WIFI", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun WifiFilterChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFF6C8CFF) else Color.White.copy(alpha = 0.05f),
        modifier = Modifier.height(32.dp)
    ) {
        Box(modifier = Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
            Text(label, color = if (isSelected) Color.White else TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun WifiCard(
    result: ScanResult,
    uiState: WifiScannerState,
    isConnected: Boolean, 
    linkSpeed: Int,
    isp: String?,
    publicIp: String?,
    onClick: () -> Unit,
    onIdentify: (String) -> Unit
) {
    val level = result.level
    val ssid = @Suppress("DEPRECATION") result.SSID
    val bssid = result.BSSID
    val context = LocalContext.current
    val isRandomized = VendorDatabase.isRandomized(bssid)
    
    val vendor = remember(bssid, uiState.vendorMap) {
        uiState.vendorMap[bssid] ?: if (isRandomized) "Randomized MAC" else VendorDatabase.getVendor(context, bssid)
    }
    
    val color = when {
        level > -60 -> Color(0xFF10B981)
        level >= -75 -> Color(0xFFFBBF24)
        level > -85 -> Color(0xFFF97316)
        else -> Color(0xFFEF4444)
    }

    val security = remember(result.capabilities) {
        val caps = result.capabilities
        when {
            caps.contains("WPA3") -> "WPA3"
            caps.contains("WPA2") -> "WPA2"
            caps.contains("WPA") -> "WPA"
            caps.contains("WEP") -> "WEP"
            else -> "Open"
        } + if (caps.contains("WPS")) "/WPS" else ""
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SoftDarkBg,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Vendor Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = when {
                        vendor.contains("Samsung", true) -> AppIcons.phoneAndroid()
                        vendor.contains("Apple", true) -> AppIcons.phoneIphone()
                        vendor.contains("FiberHome", true) || vendor.contains("Router", true) || vendor.contains("Tenda", true) || vendor.contains("ZTE", true) || vendor.contains("Mercusys", true) -> AppIcons.router()
                        else -> AppIcons.wifi()
                    },
                    contentDescription = null,
                    tint = if (vendor == "Unknown" || isRandomized) TextGray else Color(0xFF6C8CFF),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (ssid.isEmpty()) "[Hidden SSID]" else ssid,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    fontSize = 15.sp
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = bssid.uppercase(),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                    Text(" • ", color = TextGray, fontSize = 11.sp)
                    Text(
                        text = vendor,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (vendor == "Unknown" || isRandomized) TextGray else Color(0xFF6C8CFF),
                        fontWeight = if (vendor == "Unknown" || isRandomized) FontWeight.Normal else FontWeight.Bold,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    
                    if (uiState.vendorMap.containsKey(bssid)) {
                        Spacer(Modifier.width(4.dp))
                        Surface(color = Color(0xFF6C8CFF).copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                            Text("CUSTOM", color = Color(0xFF6C8CFF), fontSize = 8.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    }
                }
                
                Spacer(Modifier.height(12.dp))
                
                // Chips Row
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WifiTag(
                        text = security,
                        color = if (security.contains("Open")) Color(0xFF10B981) else Color(0xFFF97316),
                        icon = AppIcons.lock()
                    )
                    
                    val bw = when(result.channelWidth) {
                        ScanResult.CHANNEL_WIDTH_20MHZ -> "20 MHz"
                        ScanResult.CHANNEL_WIDTH_40MHZ -> "40 MHz"
                        ScanResult.CHANNEL_WIDTH_80MHZ -> "80 MHz"
                        ScanResult.CHANNEL_WIDTH_160MHZ -> "160 MHz"
                        ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ -> "80+80 MHz"
                        else -> "20 MHz"
                    }
                    WifiTag(text = bw, color = TextGray, icon = AppIcons.waves())
                    WifiTag(
                        text = "CH ${NetworkUtils.getChannel(result.frequency)}",
                        color = TextGray,
                        icon = AppIcons.arrowUpward()
                    )
                    WifiTag(
                        text = NetworkUtils.getFrequencyBand(result.frequency),
                        color = TextGray,
                        icon = AppIcons.wifi()
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(color = Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(12.dp)) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "$level dBm", color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(4.dp))
                            Icon(AppIcons.barChart(), null, tint = TextGray, modifier = Modifier.size(12.dp))
                        }
                    }

                    if (isConnected) {
                        Surface(color = Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp)) {
                            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "You", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(4.dp))
                                Icon(AppIcons.person(), null, tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                        }
                        if (linkSpeed > 0) {
                            Surface(color = Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp)) {
                                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "$linkSpeed Mbps", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(4.dp))
                                    Icon(AppIcons.wifi(), null, tint = Color.White, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    } else {
                        // Distance logic fix
                        val distance = NetworkUtils.estimateDistance(level, result.frequency)
                        if (level >= -85 && distance > 0 && distance <= 100.0) {
                            Surface(color = Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(12.dp)) {
                                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "∼${String.format(Locale.US, "%.1f", distance)}m", color = TextGray, fontSize = 11.sp)
                                    Spacer(Modifier.width(4.dp))
                                    Icon(AppIcons.locationOn(), null, tint = TextGray, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }

                    if (vendor == "Unknown" && !isRandomized) {
                        TextButton(
                            onClick = { onIdentify(bssid) },
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Identify", fontSize = 11.sp, color = Color(0xFF6C8CFF))
                        }
                    }
                }
                
                // Extra Row for Connected ISP
                if (isConnected && !isp.isNullOrEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF6C8CFF).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF6C8CFF).copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(AppIcons.language(), null, tint = Color(0xFF6C8CFF), modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("ISP: $isp", color = Color(0xFF6C8CFF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            if (!publicIp.isNullOrEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(AppIcons.dns(), null, tint = Color(0xFF6C8CFF), modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Public IP: $publicIp", color = Color(0xFF6C8CFF), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }

            // Right side: Vertical Strength Bar
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(100.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                val heightPercent = ((level + 100).coerceIn(0, 100) / 100f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(heightPercent)
                        .align(Alignment.BottomCenter)
                        .background(color)
                )
            }
        }
    }
}

@Composable
fun LocationDisabledUI(onEnable: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        color = SoftDarkBg,
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(AppIcons.locationOff(), null, tint = Color(0xFFFBBF24), modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(16.dp))
            Text("Location is Disabled", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "WiFi scanning requires Location Services to be enabled on Android. Please turn on GPS.",
                color = TextGray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onEnable,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C8CFF))
            ) {
                Text("ENABLE LOCATION", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun WifiTag(text: String, color: Color, icon: Painter) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(4.dp))
            Icon(icon, null, tint = color, modifier = Modifier.size(12.dp))
        }
    }
}
