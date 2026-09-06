package com.herwin.netez.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.herwin.netez.R
import com.herwin.netez.data.StabilityMode
import com.herwin.netez.data.StabilityResult
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.components.LegalConsentDialog
import com.herwin.netez.ui.theme.*
import com.herwin.netez.ui.viewmodels.StabilityState
import com.herwin.netez.ui.viewmodels.StabilityTestViewModel
import com.herwin.netez.utils.NetworkUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StabilityTestScreen(
    viewModel: StabilityTestViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showSettings by remember { mutableStateOf(false) }
    var showConsentDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(PremiumNavy)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                ModeSelector(
                    selectedMode = uiState.mode,
                    onModeSelected = { viewModel.setMode(it) },
                    isEnabled = !uiState.isRunning
                )
            }

            if (uiState.mode == StabilityMode.LOCAL_LOAD) {
                item {
                    TargetPill(
                        value = uiState.target,
                        onValueChange = { viewModel.updateTarget(it) },
                        isEnabled = !uiState.isRunning,
                        gateway = NetworkUtils.getGateway(context)
                    )
                }
            } else {
                item {
                    BufferbloatSettings(
                        url = uiState.customUrl,
                        onUrlChange = { viewModel.updateCustomUrl(it) },
                        isEnabled = !uiState.isRunning
                    )
                }
            }

            if (showSettings) {
                item {
                    SettingsCard(
                        threads = uiState.threads,
                        duration = uiState.duration,
                        onThreadsChange = { viewModel.updateThreads(it) },
                        onDurationChange = { viewModel.updateDuration(it) },
                        isEnabled = !uiState.isRunning
                    )
                }
            }

            item {
                MetricsSection(uiState)
            }

            item {
                StabilityChart(
                    history = uiState.latencyHistory,
                    successHistory = uiState.successHistory,
                    isRunning = uiState.isRunning
                )
            }

            if (uiState.isRunning) {
                item {
                    LinearProgressIndicator(
                        progress = { uiState.progress },
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)),
                        color = CardGradientStart,
                        trackColor = SoftDarkBg
                    )
                }
            }

            if (uiState.showResult && uiState.lastResult != null) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.stability_test_summary),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Row {
                            IconButton(onClick = {
                                viewModel.copyToClipboard()
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(AppIcons.contentCopy(), null, tint = Color(0xFF6C8CFF))
                            }
                            IconButton(onClick = {
                                viewModel.shareResults(context)
                            }) {
                                Icon(AppIcons.share(), null, tint = Color(0xFF6C8CFF))
                            }
                        }
                    }
                }
                item {
                    ResultSummaryCard(uiState.lastResult!!)
                }
            }

            item {
                Button(
                    onClick = { 
                        if (uiState.isRunning) {
                            viewModel.stopTest()
                        } else {
                            if (uiState.hasConsent) viewModel.startTest()
                            else showConsentDialog = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isRunning) SignalPoor else Color(0xFF4F46E5)
                    )
                ) {
                    Icon(
                        if (uiState.isRunning) AppIcons.stop() else AppIcons.playArrow(),
                        contentDescription = null
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        if (uiState.isRunning) stringResource(R.string.stability_stop) else stringResource(R.string.stability_start),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }

    if (showConsentDialog) {
        LegalConsentDialog(
            isExternalTarget = uiState.isExternalTarget,
            onDismiss = { showConsentDialog = false },
            onConfirm = {
                viewModel.saveConsent()
                showConsentDialog = false
                viewModel.startTest()
            }
        )
    }
}

@Composable
fun PulseIcon(isRunning: Boolean) {
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(if (isRunning) SignalExcellent else Color.Gray)
    )
}

@Composable
fun ModeSelector(
    selectedMode: StabilityMode,
    onModeSelected: (StabilityMode) -> Unit,
    isEnabled: Boolean
) {
    Surface(
        color = SoftDarkBg,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            StabilityMode.entries.forEach { mode ->
                val selected = mode == selectedMode
                Surface(
                    color = if (selected) Color(0xFF4F46E5) else Color.Transparent,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = isEnabled) { onModeSelected(mode) }
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (mode == StabilityMode.LOCAL_LOAD) "LOCAL LOAD" else "BUFFERBLOAT",
                            color = if (selected) Color.White else TextGray,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TargetPill(
    value: String,
    onValueChange: (String) -> Unit,
    isEnabled: Boolean,
    gateway: String
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.stability_target_ip), color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Box {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = isEnabled,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. 192.168.1.1", color = TextGray) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF4F46E5),
                    unfocusedBorderColor = SoftDarkBg,
                    focusedContainerColor = SoftDarkBg,
                    unfocusedContainerColor = SoftDarkBg
                ),
                trailingIcon = {
                    IconButton(onClick = { expanded = true }, enabled = isEnabled) {
                        Icon(AppIcons.arrowDropDown(), contentDescription = null, tint = TextGray)
                    }
                }
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(SoftDarkBg)
            ) {
                listOf(gateway, "192.168.1.1", "192.168.0.1", "10.0.0.1").distinct().forEach { ip ->
                    DropdownMenuItem(
                        text = { Text(ip, color = Color.White) },
                        onClick = {
                            onValueChange(ip)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BufferbloatSettings(
    url: String,
    onUrlChange: (String) -> Unit,
    isEnabled: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    val presets = mapOf(
        "Cloudflare" to "https://speed.cloudflare.com/__down?bytes=100000000",
        "DigitalOcean (SFO)" to "http://speedtest-sfo1.digitalocean.com/100mb.test",
        "Fast.com" to "https://fast.com"
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.stability_download_url), color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Box {
            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                enabled = isEnabled,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF4F46E5),
                    unfocusedBorderColor = SoftDarkBg,
                    focusedContainerColor = SoftDarkBg,
                    unfocusedContainerColor = SoftDarkBg
                ),
                trailingIcon = {
                    IconButton(onClick = { expanded = true }, enabled = isEnabled) {
                        Icon(AppIcons.link(), contentDescription = null, tint = TextGray)
                    }
                }
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(SoftDarkBg)
            ) {
                presets.forEach { (name, link) ->
                    DropdownMenuItem(
                        text = { Text(name, color = Color.White) },
                        onClick = {
                            onUrlChange(link)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsCard(
    threads: Int,
    duration: Int,
    onThreadsChange: (Int) -> Unit,
    onDurationChange: (Int) -> Unit,
    isEnabled: Boolean
) {
    Surface(color = SoftDarkBg, shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.stability_params), color = Color.White, fontWeight = FontWeight.Bold)
            
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.stability_threads), color = TextGray, fontSize = 13.sp)
                    Text("$threads", color = Color(0xFF4F46E5), fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = threads.toFloat(),
                    onValueChange = { onThreadsChange(it.toInt()) },
                    valueRange = 10f..50f,
                    enabled = isEnabled,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF4F46E5), activeTrackColor = Color(0xFF4F46E5))
                )
            }

            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.stability_duration), color = TextGray, fontSize = 13.sp)
                    Text("${duration}s", color = Color(0xFF4F46E5), fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = duration.toFloat(),
                    onValueChange = { onDurationChange(it.toInt()) },
                    valueRange = 10f..120f,
                    enabled = isEnabled,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF4F46E5), activeTrackColor = Color(0xFF4F46E5))
                )
            }
        }
    }
}

@Composable
fun MetricsSection(state: StabilityState) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        MetricCard(
            title = stringResource(R.string.stability_avg_latency),
            value = String.format("%.1f ms", state.avgLatency),
            modifier = Modifier.weight(1f),
            icon = AppIcons.timeline()
        )
        MetricCard(
            title = "Packet Loss",
            value = "${state.packetLoss}",
            modifier = Modifier.weight(1f),
            icon = AppIcons.warning(),
            valueColor = if (state.packetLoss > 0) SignalPoor else SignalExcellent
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: Painter,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.White
) {
    Surface(color = SoftDarkBg, shape = RoundedCornerShape(20.dp), modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, color = TextGray, fontSize = 12.sp)
            Text(value, color = valueColor, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        }
    }
}

@Composable
fun StabilityChart(
    history: List<Double>,
    successHistory: List<Boolean>,
    isRunning: Boolean
) {
    Surface(
        color = SoftDarkBg,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth().height(180.dp)
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (history.isEmpty()) return@Canvas
                
                val maxVal = (history.maxOrNull() ?: 1.0).coerceAtLeast(10.0)
                val width = size.width
                val height = size.height
                
                val path = Path()
                history.forEachIndexed { i, latency ->
                    val x = i * (width / (history.size - 1).coerceAtLeast(1))
                    val y = height - (latency.toFloat() / maxVal.toFloat() * height)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, color = Color(0xFF4F46E5), style = Stroke(width = 4f))
                
                history.forEachIndexed { i, latency ->
                    val isSuccess = successHistory.getOrNull(i) ?: true
                    val x = i * (width / (history.size - 1).coerceAtLeast(1))
                    val y = height - (latency.toFloat() / maxVal.toFloat() * height)
                    drawCircle(
                        color = if (isSuccess) SignalExcellent else SignalPoor,
                        radius = 6f,
                        center = Offset(x, y)
                    )
                }
            }
            Text(stringResource(R.string.stability_realtime_graph), color = TextGray, fontSize = 10.sp)
        }
    }
}

@Composable
fun ResultSummaryCard(result: StabilityResult) {
    val isStable = result.packetLoss == 0 && result.bufferbloat < 50.0
    val statusColor = if (isStable) SignalExcellent else if (result.packetLoss > 5) SignalPoor else SignalGood
    
    Surface(
        color = SoftDarkBg,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = statusColor.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                    Text(
                        if (isStable) "STABLE" else if (result.packetLoss > 5) "UNSTABLE" else "CONGESTED",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ResultLine(stringResource(R.string.stability_avg_latency), String.format("%.2f ms", result.avgLatency))
                ResultLine(stringResource(R.string.stability_max_latency), String.format("%.2f ms", result.maxLatency))
                if (result.mode == StabilityMode.BUFFERBLOAT) {
                    ResultLine(stringResource(R.string.stability_idle_latency), String.format("%.2f ms", result.idleLatency))
                    ResultLine(stringResource(R.string.stability_loaded_latency), String.format("%.2f ms", result.loadedLatency))
                    ResultLine(stringResource(R.string.stability_bufferbloat), String.format("%.2f ms", result.bufferbloat), valueColor = if (result.bufferbloat > 100) SignalPoor else Color.White)
                }
                ResultLine(stringResource(R.string.stability_success_rate), String.format("%.1f %%", result.successRate), valueColor = if (result.successRate < 95) SignalPoor else SignalExcellent)
            }
        }
    }
}

@Composable
fun ResultLine(label: String, value: String, valueColor: Color = Color.White) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextGray, fontSize = 13.sp)
        Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}
