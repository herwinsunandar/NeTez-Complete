package com.herwin.netez.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.herwin.netez.R
import com.herwin.netez.data.StabilityMode
import com.herwin.netez.data.StabilityResult
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.components.LegalConsentDialog
import com.herwin.netez.ui.theme.TechMonospace
import com.herwin.netez.ui.viewmodels.StabilityState
import com.herwin.netez.ui.viewmodels.StabilityTestViewModel
import com.herwin.netez.utils.NetworkUtils
import java.util.Locale

import com.herwin.netez.entitlement.FeatureId
import com.herwin.netez.ui.components.ContextualPaywallDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StabilityTestScreen(
    viewModel: StabilityTestViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showSettings by remember { mutableStateOf(true) }
    var showConsentDialog by remember { mutableStateOf(false) }

    if (viewModel.showPaywallDialog) {
        ContextualPaywallDialog(
            featureId = FeatureId.STABILITY_TEST_DAILY,
            limitText = "Free plan supports 3 Stability Tests per day. Upgrade to NeTez PRO or watch a video for +1 bonus test.",
            onDismiss = { viewModel.showPaywallDialog = false },
            onUpgradeClick = {}
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
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
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row {
                            IconButton(onClick = {
                                viewModel.copyToClipboard()
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(
                                    painter = AppIcons.contentCopy(),
                                    contentDescription = "Copy",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = {
                                viewModel.shareResults(context)
                            }) {
                                Icon(
                                    painter = AppIcons.share(),
                                    contentDescription = "Share",
                                    tint = MaterialTheme.colorScheme.primary
                                )
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
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        painter = if (uiState.isRunning) AppIcons.stop() else AppIcons.playArrow(),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (uiState.isRunning) stringResource(R.string.stability_stop) else stringResource(R.string.stability_start),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
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
fun ModeSelector(
    selectedMode: StabilityMode,
    onModeSelected: (StabilityMode) -> Unit,
    isEnabled: Boolean
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            StabilityMode.entries.forEach { mode ->
                val selected = mode == selectedMode
                Surface(
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
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
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
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
    val presetIps = remember(gateway) {
        listOf(gateway, "192.168.1.1", "192.168.0.1", "10.0.0.1").filter { it.isNotBlank() && it != "0.0.0.0" }.distinct()
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.stability_target_ip),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Box {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = isEnabled,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "e.g. 192.168.1.1 or example.com",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done
                ),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                trailingIcon = {
                    if (presetIps.isNotEmpty()) {
                        IconButton(onClick = { expanded = true }, enabled = isEnabled) {
                            Icon(
                                painter = AppIcons.arrowDropDown(),
                                contentDescription = "Select IP Preset",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )
            if (presetIps.isNotEmpty()) {
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    presetIps.forEach { ip ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = ip,
                                    fontFamily = TechMonospace,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
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
}

@Composable
fun BufferbloatSettings(
    url: String,
    onUrlChange: (String) -> Unit,
    isEnabled: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    val presets = mapOf(
        "Cloudflare Global" to "https://speed.cloudflare.com/__down?bytes=100000000",
        "DigitalOcean (SFO)" to "http://speedtest-sfo1.digitalocean.com/100mb.test",
        "Fast.com" to "https://fast.com"
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.stability_download_url),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Box {
            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                enabled = isEnabled,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = {
                    Text(
                        text = "e.g. https://speed.cloudflare.com/__down?bytes=100000000",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                trailingIcon = {
                    IconButton(onClick = { expanded = true }, enabled = isEnabled) {
                        Icon(
                            painter = AppIcons.link(),
                            contentDescription = "Presets",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                presets.forEach { (name, link) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = name,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
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
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.stability_params),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.stability_threads),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "$threads",
                        fontFamily = TechMonospace,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = threads.toFloat(),
                    onValueChange = { onThreadsChange(it.toInt()) },
                    valueRange = 10f..50f,
                    enabled = isEnabled
                )
            }

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.stability_duration),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "${duration}s",
                        fontFamily = TechMonospace,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = duration.toFloat(),
                    onValueChange = { onDurationChange(it.toInt()) },
                    valueRange = 10f..120f,
                    enabled = isEnabled
                )
            }
        }
    }
}

@Composable
fun MetricsSection(state: StabilityState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MetricCard(
            title = stringResource(R.string.stability_avg_latency),
            value = String.format(Locale.US, "%.1f ms", state.avgLatency),
            modifier = Modifier.weight(1f),
            icon = AppIcons.timeline()
        )
        MetricCard(
            title = "Packet Loss",
            value = "${state.packetLoss}%",
            modifier = Modifier.weight(1f),
            icon = AppIcons.warning(),
            valueColor = if (state.packetLoss > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: Painter,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        tonalElevation = 2.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                color = valueColor,
                fontFamily = TechMonospace,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun StabilityChart(
    history: List<Double>,
    successHistory: List<Boolean>,
    isRunning: Boolean
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val successColor = MaterialTheme.colorScheme.secondary
    val errorColor = MaterialTheme.colorScheme.error

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
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
                drawPath(path, color = primaryColor, style = Stroke(width = 3f))

                history.forEachIndexed { i, latency ->
                    val isSuccess = successHistory.getOrNull(i) ?: true
                    val x = i * (width / (history.size - 1).coerceAtLeast(1))
                    val y = height - (latency.toFloat() / maxVal.toFloat() * height)
                    drawCircle(
                        color = if (isSuccess) successColor else errorColor,
                        radius = 4f,
                        center = Offset(x, y)
                    )
                }
            }
            Text(
                text = stringResource(R.string.stability_realtime_graph),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun ResultSummaryCard(result: StabilityResult) {
    val isStable = result.packetLoss == 0 && result.bufferbloat < 50.0
    val statusColor = if (isStable) MaterialTheme.colorScheme.secondary else if (result.packetLoss > 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = if (isStable) "STABLE" else if (result.packetLoss > 5) "UNSTABLE" else "CONGESTED",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ResultLine(stringResource(R.string.stability_avg_latency), String.format(Locale.US, "%.2f ms", result.avgLatency))
                ResultLine(stringResource(R.string.stability_max_latency), String.format(Locale.US, "%.2f ms", result.maxLatency))
                if (result.mode == StabilityMode.BUFFERBLOAT) {
                    ResultLine(stringResource(R.string.stability_idle_latency), String.format(Locale.US, "%.2f ms", result.idleLatency))
                    ResultLine(stringResource(R.string.stability_loaded_latency), String.format(Locale.US, "%.2f ms", result.loadedLatency))
                    ResultLine(stringResource(R.string.stability_bufferbloat), String.format(Locale.US, "%.2f ms", result.bufferbloat), valueColor = if (result.bufferbloat > 100) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                }
                ResultLine(stringResource(R.string.stability_success_rate), String.format(Locale.US, "%.1f %%", result.successRate), valueColor = if (result.successRate < 95) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
fun ResultLine(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Text(text = value, color = valueColor, fontFamily = TechMonospace, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}
