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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.herwin.netez.R
import com.herwin.netez.engine.DotStatus
import com.herwin.netez.engine.SecurityPostureResult
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.components.CircularScoreView
import com.herwin.netez.ui.theme.TechMonospace
import com.herwin.netez.ui.viewmodels.DashboardState
import com.herwin.netez.ui.viewmodels.DashboardViewModel
import kotlinx.coroutines.delay
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
    onNavigateToSecurityPostureDetail: () -> Unit = {},
    onNavigateToSecurityIntelligence: () -> Unit = {},
    onNavigateToEngTools: () -> Unit = {},
    onNavigateToSecurityToolsList: () -> Unit = {}
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
        while (true) {
            viewModel.refresh()
            delay(5000)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Network/Security Summary Hero Card
        item {
            SecurityPostureHeroCard(
                postureResult = uiState.securityPostureResult,
                onRefreshPosture = { viewModel.refreshSecurityPosture() },
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

        // 2. GPS Warning Card (if GPS is disabled)
        if (!isGpsEnabled(context)) {
            item {
                GpsWarningCard(context)
            }
        }

        // 3. Strength & Link Speed Metric Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // 4. Current Network Usage Card
        item {
            NetworkUsageCard(uiState)
        }

        // 5 & 6. SSL Expiry Alert & Password Manager Group
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (uiState.expiringSslCount > 0) {
                    SslExpiryAlertCard(
                        count = uiState.expiringSslCount,
                        onClick = onNavigateToSslReminder
                    )
                }

                PasswordManagerCard(onClick = onNavigateToPasswordManager)
            }
        }

        // 7. Monitoring Dashboard Card
        item {
            MonitoringDashboardCard(
                activeCount = uiState.activeMonitors,
                onClick = onNavigateToMonitoring
            )
        }

        // 8. Quick Actions Grid
        item {
            QuickActionsGrid(
                onNavigateToSecurityIntelligence = onNavigateToSecurityIntelligence,
                onNavigateToSslReminder = onNavigateToSslReminder,
                onNavigateToEngTools = onNavigateToEngTools,
                onNavigateToSecurityToolsList = onNavigateToSecurityToolsList,
                onNavigateToSpeed = onNavigateToSpeed,
                onNavigateToWhois = onNavigateToWhois
            )
        }
    }
}

@Composable
fun SecurityPostureHeroCard(
    postureResult: SecurityPostureResult?,
    onRefreshPosture: () -> Unit,
    onHeroClick: () -> Unit,
    onWifiIconClick: () -> Unit
) {
    LaunchedEffect(Unit) {
        if (postureResult == null) {
            onRefreshPosture()
        }
    }

    AndroidView<View>(
        factory = { ctx ->
            LayoutInflater.from(ctx).inflate(R.layout.dashboard_hero_merged, null, false).apply {
                findViewById<View>(R.id.hero_card).setOnClickListener { onHeroClick() }
                findViewById<View>(R.id.iv_wifi).setOnClickListener { onWifiIconClick() }
            }
        },
        modifier = Modifier.fillMaxWidth(),
        update = { view ->
            if (postureResult != null) {
                val net = postureResult.netInfo

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

                view.findViewById<CircularScoreView>(R.id.score_circle).setScore(postureResult.overallScore, postureResult.overallStatus)

                val chips = view.findViewById<ChipGroup>(R.id.category_chips)
                chips.removeAllViews()
                postureResult.categories.forEach { category ->
                    val chip = Chip(view.context).apply {
                        val dot = when (category.status) {
                            DotStatus.SAFE -> "🟢"
                            DotStatus.WARNING -> "🟡"
                            DotStatus.CRITICAL -> "🔴"
                        }
                        text = "$dot ${category.name.split(" ")[0]} ${category.score}"
                        setTextColor(ColorStateList.valueOf(android.graphics.Color.WHITE))
                        chipBackgroundColor = ColorStateList.valueOf(android.graphics.Color.parseColor("#33000000"))
                        chipStrokeColor = ColorStateList.valueOf(android.graphics.Color.parseColor("#4DFFFFFF"))
                        chipStrokeWidth = 1f * view.resources.displayMetrics.density
                        textSize = 11f
                        chipCornerRadius = 12f * view.resources.displayMetrics.density
                        setEnsureMinTouchTargetSize(false)
                    }
                    chips.addView(chip)
                }

                val issueCount = postureResult.issues.size
                val issueSummary = when (issueCount) {
                    0 -> "System is secure"
                    1 -> "1 security issue detected"
                    else -> "$issueCount security issues detected"
                }
                view.findViewById<TextView>(R.id.tv_issues).text = issueSummary
            }
        }
    )
}

@Composable
fun SslExpiryAlertCard(
    count: Int,
    onClick: () -> Unit
) {
    if (count <= 0) return

    val titleText = if (count == 1) "1 SSL Certificate Expiring" else "$count SSL Certificates Expiring"
    val subtitleText = "Review certificates that require attention"
    val alertColor = MaterialTheme.colorScheme.error

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = alertColor.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, alertColor.copy(alpha = 0.4f)),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = AppIcons.warning(),
                contentDescription = null,
                tint = alertColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = alertColor
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                painter = AppIcons.chevronRight(),
                contentDescription = null,
                tint = alertColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun PasswordManagerCard(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = AppIcons.lock(),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.security_pass_manager_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.security_pass_manager_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                painter = AppIcons.chevronRight(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SignalCard(
    modifier: Modifier,
    label: String,
    value: String,
    painter: Painter,
    color: Color
) {
    Surface(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    painter = painter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontFamily = TechMonospace,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun NetworkUsageCard(state: DashboardState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Current Network Usage",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Today: ↓ ${formatBytes(state.todayDownBytes)} ↑ ${formatBytes(state.todayUpBytes)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = TechMonospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
            ) {
                UsageGraph(state.downloadHistory, state.uploadHistory)
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(MaterialTheme.colorScheme.primary, "Download ${formatRate(state.currentDownRate)}")
                Spacer(Modifier.width(16.dp))
                LegendItem(MaterialTheme.colorScheme.tertiary, "Upload ${formatRate(state.currentUpRate)}")
            }
        }
    }
}

@Composable
fun UsageGraph(downHistory: List<Float>, upHistory: List<Float>) {
    val downColor = MaterialTheme.colorScheme.primary
    val upColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        val maxVal = (downHistory.maxOrNull() ?: 1024f).coerceAtLeast(upHistory.maxOrNull() ?: 1024f).coerceAtLeast(1024f)

        for (i in 1..3) {
            val y = height * (i / 4f)
            drawLine(gridColor, start = Offset(0f, y), end = Offset(width, y))
        }

        if (downHistory.size < 2) return@Canvas

        val stepX = width / (downHistory.size - 1)

        val downPath = Path()
        downHistory.forEachIndexed { index, value ->
            val x = index * stepX
            val y = height - (value / maxVal * height)
            if (index == 0) downPath.moveTo(x, y) else downPath.lineTo(x, y)
        }
        drawPath(downPath, downColor, style = Stroke(2.dp.toPx()))

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
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = TechMonospace,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun QuickActionsGrid(
    onNavigateToSecurityIntelligence: () -> Unit,
    onNavigateToSslReminder: () -> Unit,
    onNavigateToEngTools: () -> Unit,
    onNavigateToSecurityToolsList: () -> Unit,
    onNavigateToSpeed: () -> Unit,
    onNavigateToWhois: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Row 1: Security Intel & SSL/TLS Monitor
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            QuickActionNocButton(
                modifier = Modifier.weight(1f),
                label = "Security Intel",
                painter = AppIcons.shieldCheck(),
                onClick = onNavigateToSecurityIntelligence
            )
            QuickActionNocButton(
                modifier = Modifier.weight(1f),
                label = "SSL/TLS Monitor",
                painter = AppIcons.lock(),
                onClick = onNavigateToSslReminder
            )
        }

        // Row 2: Engineer Tools & Security Tools
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            QuickActionNocButton(
                modifier = Modifier.weight(1f),
                label = "Engineer Tools",
                painter = AppIcons.terminal(),
                onClick = onNavigateToEngTools
            )
            QuickActionNocButton(
                modifier = Modifier.weight(1f),
                label = "Security Tools",
                painter = AppIcons.build(),
                onClick = onNavigateToSecurityToolsList
            )
        }

        // Row 3: Speed Test & WHOIS
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            QuickActionNocButton(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.action_speedtest),
                painter = AppIcons.speed(),
                onClick = onNavigateToSpeed
            )
            QuickActionNocButton(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.action_whois),
                painter = AppIcons.info(),
                onClick = onNavigateToWhois
            )
        }
    }
}

@Composable
fun QuickActionNocButton(
    modifier: Modifier,
    label: String,
    painter: Painter,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(90.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painter,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun MonitoringDashboardCard(
    activeCount: Int,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = AppIcons.monitor(),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Monitoring Lab",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "$activeCount active monitors",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                painter = AppIcons.chevronRight(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun GpsWarningCard(context: Context) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = AppIcons.locationOff(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "GPS Required",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Enable GPS to detect WiFi SSID.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            TextButton(onClick = {
                try {
                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                } catch (e: Exception) {
                }
            }) {
                Text(
                    text = "FIX NOW",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun getSignalColor(strength: String): Color {
    val dbm = strength.split(" ")[0].toIntOrNull() ?: -100
    return when {
        dbm > -60 -> MaterialTheme.colorScheme.secondary
        dbm >= -80 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
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
