package com.herwin.netez.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.ui.theme.PremiumNavy
import com.herwin.netez.ui.theme.SoftDarkBg
import com.herwin.netez.ui.theme.TextGray
import com.herwin.netez.ui.viewmodels.WifiFilter
import com.herwin.netez.ui.viewmodels.WifiScannerViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiScannerScreen(
    viewModel: WifiScannerViewModel = viewModel(),
    onNavigateToDetail: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PremiumNavy)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(8.dp))

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
                    Icon(Icons.Default.Refresh, null, tint = Color.White)
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
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(uiState.filteredResults) { result ->
                @Suppress("DEPRECATION")
                WifiCard(
                    result = result,
                    isConnected = result.BSSID == uiState.currentBssid,
                    linkSpeed = if (result.BSSID == uiState.currentBssid) uiState.currentLinkSpeed else 0,
                    isp = if (result.BSSID == uiState.currentBssid) uiState.connectedIsp else null,
                    publicIp = if (result.BSSID == uiState.currentBssid) uiState.connectedPublicIp else null,
                    onClick = { onNavigateToDetail(result.BSSID) }
                )
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
    result: android.net.wifi.ScanResult, 
    isConnected: Boolean, 
    linkSpeed: Int,
    isp: String?,
    publicIp: String?,
    onClick: () -> Unit
) {
    val level = result.level
    val ssid = @Suppress("DEPRECATION") result.SSID
    val bssid = result.BSSID
    
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
        shape = RoundedCornerShape(24.dp),
        color = SoftDarkBg,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (ssid.isEmpty()) "[Hidden SSID]" else ssid,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = bssid.uppercase(),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
                Text(
                    text = com.herwin.netez.utils.NetworkUtils.getVendor(bssid),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
                
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
                        icon = Icons.Default.Lock
                    )
                    WifiTag(
                        text = com.herwin.netez.utils.NetworkUtils.getFrequencyBand(result.frequency),
                        color = TextGray,
                        icon = Icons.Default.SettingsInputAntenna
                    )
                    val bw = when(result.channelWidth) {
                        android.net.wifi.ScanResult.CHANNEL_WIDTH_20MHZ -> "20 MHz"
                        android.net.wifi.ScanResult.CHANNEL_WIDTH_40MHZ -> "40 MHz"
                        android.net.wifi.ScanResult.CHANNEL_WIDTH_80MHZ -> "80 MHz"
                        android.net.wifi.ScanResult.CHANNEL_WIDTH_160MHZ -> "160 MHz"
                        android.net.wifi.ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ -> "80+80 MHz"
                        else -> "20 MHz"
                    }
                    WifiTag(text = bw, color = TextGray, icon = Icons.Default.Waves)
                    WifiTag(
                        text = "CH ${com.herwin.netez.utils.NetworkUtils.getChannel(result.frequency)}",
                        color = TextGray,
                        icon = Icons.Default.ArrowUpward
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
                            Icon(Icons.Default.BarChart, null, tint = TextGray, modifier = Modifier.size(12.dp))
                        }
                    }

                    if (isConnected) {
                        Surface(color = Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp)) {
                            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "You", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                        }
                        if (linkSpeed > 0) {
                            Surface(color = Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp)) {
                                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "$linkSpeed Mbps", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(4.dp))
                                    Icon(Icons.Default.Wifi, null, tint = Color.White, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    } else {
                        // Distance for non-connected
                        val distance = com.herwin.netez.utils.NetworkUtils.estimateDistance(level, result.frequency)
                        Surface(color = Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(12.dp)) {
                            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "∼${String.format(Locale.US, "%.1f", distance)}m", color = TextGray, fontSize = 11.sp)
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.LocationOn, null, tint = TextGray, modifier = Modifier.size(12.dp))
                            }
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
                                Icon(Icons.Default.Language, null, tint = Color(0xFF6C8CFF), modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("ISP: $isp", color = Color(0xFF6C8CFF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            if (!publicIp.isNullOrEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Dns, null, tint = Color(0xFF6C8CFF), modifier = Modifier.size(14.dp))
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
fun WifiTag(text: String, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
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
