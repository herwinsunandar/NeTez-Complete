package com.herwin.netez.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.location.LocationManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.R
import com.herwin.netez.engine.DotStatus
import com.herwin.netez.engine.SecurityPostureEngine
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.components.CircularScoreView
import com.herwin.netez.ui.theme.*
import com.herwin.netez.ui.viewmodels.DashboardState
import com.herwin.netez.ui.viewmodels.DashboardViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: DashboardViewModel = viewModel(),
    onNavigateToPing: () -> Unit,
    onNavigateToSpeed: () -> Unit,
    onNavigateToWhois: () -> Unit,
    onNavigateToIpCalc: () -> Unit,
    onNavigateToMonitoring: () -> Unit,
    onNavigateToWifiDetail: (String) -> Unit,
    onNavigateToCellularDetail: () -> Unit,
    onNavigateToPasswordManager: () -> Unit,
    onNavigateToSslReminder: () -> Unit = {},
    onNavigateToSecurity: () -> Unit = {},
    onNavigateToSecurityPostureDetail: () -> Unit = {}
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
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.GET_ACCOUNTS
            )
        )
    }

    LaunchedEffect(Unit) {
        while(true) {
            viewModel.refresh()
            delay(5000)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PremiumNavy),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SecurityPostureHeroCard(
                onHeroClick = onNavigateToSecurityPostureDetail,
                onWifiIconClick = {
                    if (uiState.connectionType == "WiFi" && uiState.bssid.isNotEmpty()) {
                        onNavigateToWifiDetail(uiState.bssid)
                    } else if (uiState.connectionType == "Cellular") {
                        onNavigateToCellularDetail()
                    }
                }
            )
        }

        if (!isGpsEnabled(context)) {
            item {
                GpsWarningCard(context)
            }
        }

        if (uiState.expiringSslCount > 0) {
            item {
                SecurityAlertBanner(
                    text = stringResource(R.string.ssl_banner_expiring, uiState.expiringSslCount),
                    onClick = onNavigateToSslReminder
                )
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { onNavigateToPasswordManager() },
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF6C8CFF).copy(alpha = 0.1f),
                border = BorderStroke(1.dp, Color(0xFF6C8CFF).copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(AppIcons.lock(), null, tint = Color(0xFF6C8CFF))
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.dash_pass_prompt), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SignalCard(
                    modifier = Modifier.weight(1f),
                    label = "Strength",
                    value = uiState.signalStrength,
                    painter = AppIcons.signalCellular(),
                    color = getSignalColor(uiState.signalStrength)
                )
                SignalCard(
                    modifier = Modifier.weight(1f),
                    label = if (uiState.connectionType == "WiFi") "Link Speed" else "Net Type",
                    value = uiState.linkSpeed,
                    painter = if (uiState.connectionType == "WiFi") AppIcons.speed() else AppIcons.networkCheck(),
                    color = Color.White
                )
            }
        }

        item {
            NetworkUsageCard(uiState)
        }

        item {
            MonitoringDashboardCard(
                activeCount = uiState.activeMonitors,
                onClick = onNavigateToMonitoring
            )
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
fun SecurityPostureHeroCard(
    onHeroClick: () -> Unit,
    onWifiIconClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    
    AndroidView<View>(
        factory = { ctx ->
            LayoutInflater.from(ctx).inflate(R.layout.dashboard_hero_merged, null, false).apply {
                findViewById<View>(R.id.hero_card).setOnClickListener { onHeroClick() }
                findViewById<View>(R.id.iv_wifi).setOnClickListener { onWifiIconClick() }
            }
        },
        modifier = Modifier.fillMaxWidth(),
        update = { view ->
            scope.launch {
                val result = SecurityPostureEngine.calculate(view.context)
                val net = result.netInfo
                
                view.findViewById<TextView>(R.id.tv_ssid).text = net.ssidOrOperator
                view.findViewById<TextView>(R.id.tv_subtitle).text = if (net.isWifi) "WiFi Network" else "Cellular Network • ${net.networkType}"
                view.findViewById<TextView>(R.id.tv_local_ip_value).text = net.localIp
                view.findViewById<TextView>(R.id.tv_public_ip_value).text = net.publicIp
                
                val gatewayValue = view.findViewById<TextView>(R.id.tv_gateway_value)
                val gatewayLabel = view.findViewById<TextView>(R.id.tv_gateway_label)
                if (net.isWifi) {
                    gatewayLabel.text = "GATEWAY"
                    gatewayValue.text = net.gateway
                } else {
                    gatewayLabel.text = "TYPE"
                    gatewayValue.text = net.networkType
                }
                
                view.findViewById<TextView>(R.id.tv_isp_value).text = net.isp
                
                val wifiIcon = view.findViewById<ImageView>(R.id.iv_wifi)
                wifiIcon.setImageResource(if (net.isWifi) R.drawable.ic_wifi else R.drawable.ic_signal_cellular)
                
                view.findViewById<CircularScoreView>(R.id.score_circle).setScore(result.overallScore, result.overallStatus)
                
                val chips = view.findViewById<ChipGroup>(R.id.category_chips)
                chips.removeAllViews()
                result.categories.forEach { category ->
                    val chip = Chip(view.context)
                    val dot = when (category.status) {
                        DotStatus.SAFE -> "🟢"
                        DotStatus.WARNING -> "🟡"
                        DotStatus.CRITICAL -> "🔴"
                    }
                    chip.text = "$dot ${category.name.split(" ")[0]} ${category.score}"
                    chip.chipBackgroundColor = ColorStateList.valueOf(android.graphics.Color.parseColor("#26FFFFFF"))
                    chip.setTextColor(android.graphics.Color.WHITE)
                    chip.textSize = 11f
                    chip.chipCornerRadius = 8f * view.resources.displayMetrics.density
                    chip.setEnsureMinTouchTargetSize(false)
                    chips.addView(chip)
                }
                
                val issueSummary = result.issues.joinToString(" • ") { it.title }
                view.findViewById<TextView>(R.id.tv_issues).text = view.context.getString(R.string.security_issues, result.issues.size, issueSummary)
            }
        }
    )
}

@Composable
fun SecurityAlertBanner(text: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                color = Color(0xFFEF4444),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Icon(
                painter = AppIcons.chevronRight(),
                contentDescription = null,
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun WifiInfoItem(label: String, value: String) {
    Column {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = Color(0xFFC7D2FE)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SignalCard(modifier: Modifier, label: String, value: String, painter: Painter, color: Color) {
    Surface(
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(24.dp),
        color = SoftDarkBg,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = TextGray)
                Icon(painter, null, tint = TextGray.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
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
            drawLine(Color.White.copy(alpha = 0.05f), start = Offset(0f, y), end = Offset(width, y))
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
            QuickActionNocButton(Modifier.weight(1f), stringResource(R.string.action_quick_ping), AppIcons.networkCheck(), onNavigateToPing)
            QuickActionNocButton(Modifier.weight(1f), stringResource(R.string.action_speedtest), AppIcons.speed(), onNavigateToSpeed)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            QuickActionNocButton(Modifier.weight(1f), stringResource(R.string.action_whois), AppIcons.info(), onNavigateToWhois)
            QuickActionNocButton(Modifier.weight(1f), stringResource(R.string.action_ip_info), AppIcons.calculate(), onNavigateToIpCalc)
        }
    }
}

@Composable
fun QuickActionNocButton(modifier: Modifier, label: String, painter: Painter, onClick: () -> Unit) {
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
            Icon(painter, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 1.sp)
        }
    }
}

@Composable
fun MonitoringDashboardCard(activeCount: Int, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = SoftDarkBg,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(AppIcons.monitor(), null, tint = Color(0xFF6C8CFF), modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("MONITORING LAB", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 1.sp)
                Text("$activeCount active monitors", style = MaterialTheme.typography.bodySmall, color = TextGray)
            }
            Icon(AppIcons.chevronRight(), null, tint = TextGray)
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(AppIcons.locationOff(), contentDescription = null, tint = MaterialTheme.colorScheme.error)
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
