package com.herwin.netez.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.R
import com.herwin.netez.ui.theme.*
import com.herwin.netez.ui.viewmodels.DashboardState
import com.herwin.netez.ui.viewmodels.DashboardViewModel
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: DashboardViewModel = viewModel(),
    onNavigateToPing: () -> Unit,
    onNavigateToSpeed: () -> Unit,
    onNavigateToWhois: () -> Unit,
    onNavigateToIpCalc: () -> Unit,
    onNavigateToWifiDetail: (String) -> Unit,
    onNavigateToCellularDetail: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) viewModel.refresh()
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.READ_PHONE_STATE
            )
        )
    }

    LaunchedEffect(Unit) {
        while(true) {
            viewModel.refresh()
            kotlinx.coroutines.delay(5000)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PremiumNavy),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!isGpsEnabled(context)) {
            item {
                GpsWarningCard(context)
            }
        }

        item {
            PremiumWifiCard(uiState, onClick = {
                if (uiState.connectionType == "WiFi" && uiState.bssid.isNotEmpty()) {
                    onNavigateToWifiDetail(uiState.bssid)
                } else if (uiState.connectionType == "Cellular") {
                    onNavigateToCellularDetail()
                }
            })
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SignalCard(
                    modifier = Modifier.weight(1f),
                    label = "Strength",
                    value = uiState.signalStrength,
                    icon = Icons.Default.SignalCellularAlt,
                    color = getSignalColor(uiState.signalStrength)
                )
                SignalCard(
                    modifier = Modifier.weight(1f),
                    label = if (uiState.connectionType == "WiFi") "Link Speed" else "Net Type",
                    value = uiState.linkSpeed,
                    icon = if (uiState.connectionType == "WiFi") Icons.Default.Speed else Icons.Default.NetworkCheck,
                    color = Color.White
                )
            }
        }

        item {
            NetworkUsageCard(uiState)
        }

        item {
            QuickActionsGrid(
                onNavigateToPing,
                onNavigateToSpeed,
                onNavigateToWhois,
                onNavigateToIpCalc
            )
        }
        
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
fun PremiumWifiCard(state: DashboardState, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF6C8CFF), Color(0xFF4F46E5))
                )
            )
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (state.connectionType == "WiFi") state.ssid else state.operatorName,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (state.connectionType == "WiFi") "WiFi Network" else "Cellular Network",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                Icon(
                    imageVector = if (state.connectionType == "WiFi") Icons.Default.Wifi else Icons.Default.SignalCellularAlt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    WifiInfoItem("Local IP", state.localIp)
                    WifiInfoItem("Gateway", state.gateway)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    WifiInfoItem("Public IP", state.publicIp)
                    WifiInfoItem("ISP", state.isp)
                }
            }
        }
    }
}

@Composable
fun WifiInfoItem(label: String, value: String) {
    Column {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
            color = Color(0xFFC7D2FE)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SignalCard(modifier: Modifier, label: String, value: String, icon: ImageVector, color: Color) {
    Surface(
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(24.dp),
        color = SoftDarkBg,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = TextGray)
                Icon(icon, null, tint = TextGray.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = color)
        }
    }
}

@Composable
fun NetworkUsageCard(state: DashboardState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = SoftDarkBg,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("CURRENT NETWORK USAGE", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 1.sp)
                Text(
                    text = "Today: ↓ ${formatBytes(state.todayDownBytes)} ↑ ${formatBytes(state.todayUpBytes)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextGray,
                    fontSize = 9.sp
                )
            }
            
            Spacer(Modifier.height(16.dp))
            
            Box(modifier = Modifier.fillMaxWidth().height(100.dp)) {
                UsageGraph(state.downloadHistory, state.uploadHistory)
            }
            
            Spacer(Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                LegendItem(Color(0xFF6C8CFF), "Download ${formatRate(state.currentDownRate)}")
                Spacer(Modifier.width(16.dp))
                LegendItem(Color(0xFFFF8C42), "Upload ${formatRate(state.currentUpRate)}")
            }
        }
    }
}

@Composable
fun UsageGraph(downHistory: List<Float>, upHistory: List<Float>) {
    val downColor = Color(0xFF6C8CFF)
    val upColor = Color(0xFFFF8C42)
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        val maxVal = (downHistory.maxOrNull() ?: 1024f).coerceAtLeast(upHistory.maxOrNull() ?: 1024f).coerceAtLeast(1024f)
        
        // Draw grid lines
        for (i in 1..3) {
            val y = height * (i / 4f)
            drawLine(Color.White.copy(alpha = 0.05f), start = androidx.compose.ui.geometry.Offset(0f, y), end = androidx.compose.ui.geometry.Offset(width, y))
        }

        if (downHistory.size < 2) return@Canvas
        
        val stepX = width / (downHistory.size - 1)
        
        // Path for Down
        val downPath = Path()
        downHistory.forEachIndexed { index, value ->
            val x = index * stepX
            val y = height - (value / maxVal * height)
            if (index == 0) downPath.moveTo(x, y) else downPath.lineTo(x, y)
        }
        drawPath(downPath, downColor, style = Stroke(2.dp.toPx()))
        
        // Path for Up
        val upPath = Path()
        upHistory.forEachIndexed { index, value ->
            val x = index * stepX
            val y = height - (value / maxVal * height)
            if (index == 0) upPath.moveTo(x, y) else upPath.lineTo(x, y)
        }
        drawPath(upPath, upColor, style = Stroke(2.dp.toPx()))
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun QuickActionsGrid(
    onNavigateToPing: () -> Unit,
    onNavigateToSpeed: () -> Unit,
    onNavigateToWhois: () -> Unit,
    onNavigateToIpCalc: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            QuickActionNocButton(Modifier.weight(1f), stringResource(R.string.action_quick_ping), Icons.Default.NetworkCheck, onNavigateToPing)
            QuickActionNocButton(Modifier.weight(1f), stringResource(R.string.action_speedtest), Icons.Default.Speed, onNavigateToSpeed)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            QuickActionNocButton(Modifier.weight(1f), stringResource(R.string.action_whois), Icons.Default.Info, onNavigateToWhois)
            QuickActionNocButton(Modifier.weight(1f), stringResource(R.string.action_ip_info), Icons.Default.Calculate, onNavigateToIpCalc)
        }
    }
}

@Composable
fun QuickActionNocButton(modifier: Modifier, label: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(24.dp),
        color = SoftDarkBg,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 1.sp)
        }
    }
}



private fun getSignalColor(strength: String): Color {
    val dbm = strength.split(" ")[0].toIntOrNull() ?: -100
    return when {
        dbm > -60 -> SignalExcellent
        dbm >= -80 -> SignalGood
        else -> SignalPoor
    }
}

private fun formatRate(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB/s", bytes / (1024f * 1024f))
        bytes >= 1024 -> String.format(Locale.US, "%.1f KB/s", bytes / 1024f)
        else -> "$bytes B/s"
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 * 1024 -> String.format(Locale.US, "%.1f GB", bytes / (1024f * 1024f * 1024f))
        bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024f * 1024f))
        bytes >= 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024f)
        else -> "$bytes B"
    }
}

private fun isGpsEnabled(context: Context): Boolean {
    return try {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    } catch (e: Exception) {
        false
    }
}

@Composable
fun GpsWarningCard(context: Context) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.LocationOff, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("GPS Required", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Enable GPS to detect WiFi SSID.", style = MaterialTheme.typography.bodySmall, color = TextGray)
            }
            TextButton(onClick = {
                try {
                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                } catch (e: Exception) {}
            }) {
                Text("FIX NOW", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
