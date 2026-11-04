package com.herwin.netez.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.network.PingResult
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.theme.TechMonospace
import com.herwin.netez.ui.viewmodels.HistoryViewModel
import com.herwin.netez.ui.viewmodels.MultiPingHost
import com.herwin.netez.ui.viewmodels.PingMode
import com.herwin.netez.ui.viewmodels.PingState
import com.herwin.netez.ui.viewmodels.PingViewModel
import java.text.SimpleDateFormat
import java.util.*

import com.herwin.netez.entitlement.FeatureAccessManager
import com.herwin.netez.entitlement.FeatureAccessState
import com.herwin.netez.entitlement.FeatureId
import com.herwin.netez.ui.components.ContextualPaywallDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PingScreen(
    viewModel: PingViewModel = viewModel(),
    historyViewModel: HistoryViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var showAddHostDialog by remember { mutableStateOf(false) }

    if (viewModel.showPaywallDialog) {
        ContextualPaywallDialog(
            featureId = FeatureId.MULTI_PING_NODES,
            limitText = "Free plan supports up to 3 Multi-Ping targets in a session. Upgrade to NeTez PRO for up to 50 simultaneous ping targets.",
            onDismiss = { viewModel.showPaywallDialog = false },
            onUpgradeClick = {}
        )
    }

    if (viewModel.showGlobalSupportDialog) {
        ContextualPaywallDialog(
            featureId = FeatureId.GLOBAL_TOOL_USAGE,
            limitText = "You've performed 3 network & security scans! If NeTez is helpful, consider watching a short video to support development or upgrade to PRO (Skippable).",
            onDismiss = { viewModel.showGlobalSupportDialog = false },
            onUpgradeClick = {}
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Mode Selector Tab
        SecondaryTabRow(
            selectedTabIndex = uiState.mode.ordinal,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            PingMode.entries.forEach { mode ->
                Tab(
                    selected = uiState.mode == mode,
                    onClick = { viewModel.setMode(mode) },
                    text = {
                        Text(
                            text = mode.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (uiState.mode == PingMode.SINGLE) {
            SinglePingContent(uiState, viewModel, historyViewModel)
        } else {
            MultiPingContent(
                uiState = uiState,
                viewModel = viewModel,
                onAddClick = {
                    val accessManager = FeatureAccessManager.getInstance(context)
                    val state = accessManager.checkAccess(FeatureId.MULTI_PING_NODES, uiState.multiHosts.size)
                    if (state == FeatureAccessState.LIMIT_REACHED) {
                        viewModel.showPaywallDialog = true
                    } else {
                        showAddHostDialog = true
                    }
                }
            )
        }
    }

    if (showAddHostDialog) {
        AddMultiHostDialog(
            onDismiss = { showAddHostDialog = false },
            onAdd = {
                viewModel.addMultiHost(it)
                showAddHostDialog = false
            }
        )
    }
}

@Composable
fun SinglePingContent(
    uiState: PingState,
    viewModel: PingViewModel,
    historyViewModel: HistoryViewModel
) {
    val context = LocalContext.current

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Host Input & Control Bar
        HostInputSection(
            host = uiState.host,
            history = uiState.hostHistory,
            isRunning = uiState.isRunning,
            onHostChange = { viewModel.updateHost(it) },
            onTogglePing = {
                if (uiState.isRunning) {
                    historyViewModel.addResult(
                        "Ping",
                        uiState.host,
                        "Avg: ${String.format(Locale.US, "%.1f", uiState.avgLatency)}ms"
                    )
                }
                viewModel.togglePing()
            }
        )

        // Statistics Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Statistics",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            Row {
                IconButton(onClick = {
                    viewModel.copyResultsToClipboard()
                    Toast.makeText(context, "Results copied to clipboard", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(
                        painter = AppIcons.contentCopy(),
                        contentDescription = "Copy Results",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = {
                    viewModel.shareResults(context)
                }) {
                    Icon(
                        painter = AppIcons.share(),
                        contentDescription = "Share Results",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Metrics Grid (2x2)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PingStatCard(
                modifier = Modifier.weight(1f),
                label = "Avg Latency",
                value = "${String.format(Locale.US, "%.1f", uiState.avgLatency)} ms",
                color = MaterialTheme.colorScheme.primary
            )
            val lossColor = when {
                uiState.packetLoss == 0 -> MaterialTheme.colorScheme.secondary
                uiState.packetLoss < 5 -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.error
            }
            PingStatCard(
                modifier = Modifier.weight(1f),
                label = "Packet Loss",
                value = "${uiState.packetLoss}%",
                color = lossColor,
                isHighlighted = uiState.packetLoss > 0
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PingStatCard(
                modifier = Modifier.weight(1f),
                label = "Jitter",
                value = "${String.format(Locale.US, "%.1f", uiState.jitter)} ms",
                color = MaterialTheme.colorScheme.primary
            )
            PingStatCard(
                modifier = Modifier.weight(1f),
                label = "Min/Max Latency",
                value = "${uiState.minLatency.toInt()}/${uiState.maxLatency.toInt()} ms",
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Latency Chart Section
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Latency Real-Time Graph",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                tonalElevation = 2.dp
            ) {
                LatencyLineChart(uiState.results.map { if (it.isSuccessful) it.time.toFloat() else 0f })
            }
        }

        // Results Log Section
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            tonalElevation = 2.dp
        ) {
            if (uiState.results.isEmpty() && !uiState.isRunning) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = AppIcons.networkCheck(),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "No Ping Results Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Enter a host address above and press START to measure network response.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(uiState.results.reversed()) { index, result ->
                        PingDetailRow(
                            seq = uiState.results.size - index,
                            result = result,
                            isRunning = uiState.isRunning
                        )
                        if (index < uiState.results.size - 1) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MultiPingContent(
    uiState: PingState,
    viewModel: PingViewModel,
    onAddClick: () -> Unit
) {
    val context = LocalContext.current

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Hosts to Ping",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    viewModel.copyResultsToClipboard()
                    Toast.makeText(context, "Results copied", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(
                        painter = AppIcons.contentCopy(),
                        contentDescription = "Copy",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = { viewModel.shareResults(context) }) {
                    Icon(
                        painter = AppIcons.share(),
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                TextButton(
                    onClick = onAddClick,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        painter = AppIcons.add(),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Add Host", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Horizontal list of target hosts
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(uiState.multiHosts) { host ->
                MultiHostCard(
                    host = host,
                    isRunning = uiState.isRunning,
                    onToggle = { viewModel.toggleMultiHost(host) },
                    onDelete = { viewModel.removeMultiHost(host) }
                )
            }
        }

        // Multi Latency Chart
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            tonalElevation = 2.dp
        ) {
            MultiLatencyChart(hosts = uiState.multiHosts.filter { it.isEnabled })
        }

        // Multi Stats Table
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val enabledHosts = uiState.multiHosts.filter { it.isEnabled }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "HOST",
                        modifier = Modifier.weight(1.5f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "AVG",
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = "LOSS",
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End
                    )
                }
            }
            items(enabledHosts) { host ->
                MultiStatRow(host)
            }

            if (enabledHosts.isNotEmpty() && !uiState.isRunning) {
                item {
                    val best = enabledHosts.filter { it.avgLatency > 0 }.minByOrNull { it.avgLatency }
                    val worst = enabledHosts.maxByOrNull { it.avgLatency }
                    if (best != null) {
                        Text(
                            text = "Best: ${best.host} (${String.format(Locale.US, "%.1f", best.avgLatency)}ms) • Worst: ${worst?.host} (${String.format(Locale.US, "%.1f", worst?.avgLatency)}ms)",
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontFamily = TechMonospace,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Button(
            onClick = { viewModel.togglePing() },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(bottom = 4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (uiState.isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = if (uiState.isRunning) "STOP MULTI PING" else "START MULTI PING",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun MultiHostCard(
    host: MultiPingHost,
    isRunning: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.width(130.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (host.isEnabled) Color(host.color).copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.size(8.dp).background(Color(host.color), CircleShape))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(16.dp),
                    enabled = !isRunning
                ) {
                    Icon(
                        painter = AppIcons.close(),
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = host.host,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = TechMonospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = host.label.ifEmpty { "Host" },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${host.lastLatency.toInt()} ms",
                    fontFamily = TechMonospace,
                    color = if (host.lastLatency < 50) MaterialTheme.colorScheme.secondary else if (host.lastLatency < 100) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp
                )
                Switch(
                    checked = host.isEnabled,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.scale(0.55f),
                    enabled = !isRunning
                )
            }
        }
    }
}

@Composable
fun MultiStatRow(host: MultiPingHost) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(8.dp).background(Color(host.color), CircleShape))
            Spacer(Modifier.width(10.dp))
            Text(
                text = host.host,
                modifier = Modifier.weight(1.5f),
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = TechMonospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${String.format(Locale.US, "%.1f", host.avgLatency)} ms",
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = TechMonospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                textAlign = TextAlign.End
            )
            Text(
                text = "${host.packetLoss}%",
                modifier = Modifier.weight(1f),
                color = if (host.packetLoss > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                fontFamily = TechMonospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
fun MultiLatencyChart(hosts: List<MultiPingHost>) {
    val axisColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val textPaintColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 40.dp, end = 16.dp, top = 20.dp, bottom = 20.dp)
    ) {
        val width = size.width
        val height = size.height

        val maxLatency = hosts.flatMap { it.results }.map { it.time.toFloat() }.maxOrNull()?.coerceAtLeast(100f) ?: 150f
        val maxVal = maxLatency * 1.2f

        for (i in 1..4) {
            val y = height - (i * height / 4)
            drawLine(
                color = axisColor,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )
            drawContext.canvas.nativeCanvas.drawText(
                (maxVal * i / 4).toInt().toString(),
                -8.dp.toPx(),
                y + 4.dp.toPx(),
                Paint().apply {
                    color = textPaintColor
                    textSize = 9.sp.toPx()
                    textAlign = Paint.Align.RIGHT
                }
            )
        }

        hosts.forEach { host ->
            if (host.results.size < 2) return@forEach
            val data = host.results.takeLast(20)
            val stepX = width / 19
            val path = Path()

            data.forEachIndexed { index, result ->
                val x = index * stepX
                val y = if (result.isSuccessful) {
                    height - (result.time.toFloat() / maxVal * height).coerceIn(0f, height)
                } else {
                    height
                }

                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)

                if (!result.isSuccessful) {
                    val xSize = 4.dp.toPx()
                    drawLine(
                        color = Color.Red,
                        start = Offset(x - xSize, height - xSize),
                        end = Offset(x + xSize, height),
                        strokeWidth = 2.dp.toPx()
                    )
                    drawLine(
                        color = Color.Red,
                        start = Offset(x + xSize, height - xSize),
                        end = Offset(x - xSize, height),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }

            drawPath(path, Color(host.color), style = Stroke(2.dp.toPx()))
        }
    }
}

@Composable
fun AddMultiHostDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var host by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Add Host to Multi Ping", color = MaterialTheme.colorScheme.onSurface) },
        text = {
            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                placeholder = { Text("e.g. cloudflare.com", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        confirmButton = {
            Button(onClick = { if (host.isNotBlank()) onAdd(host) }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PingStatCard(
    modifier: Modifier,
    label: String,
    value: String,
    color: Color,
    isHighlighted: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (isHighlighted) color.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isHighlighted) color.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostInputSection(
    host: String,
    history: List<String>,
    isRunning: Boolean,
    onHostChange: (String) -> Unit,
    onTogglePing: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    ExposedDropdownMenuBox(
                        expanded = expanded && !isRunning,
                        onExpandedChange = { if (!isRunning) expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = host,
                            onValueChange = onHostChange,
                            placeholder = { Text("e.g. 8.8.8.8 or google.com", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            enabled = !isRunning,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Go
                            ),
                            keyboardActions = KeyboardActions(
                                onGo = { onTogglePing() }
                            ),
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = expanded && !isRunning,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            history.forEach { item ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = item,
                                            fontFamily = TechMonospace,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        onHostChange(item)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = onTogglePing,
                    modifier = Modifier
                        .height(52.dp)
                        .width(90.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = if (isRunning) "STOP" else "START",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        if (isRunning) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
fun LatencyLineChart(data: List<Float>) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val textPaintColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 40.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
    ) {
        val width = size.width
        val height = size.height

        val displayData = data.takeLast(30)
        val maxDataVal = displayData.maxOrNull() ?: 0f
        val maxVal = if (maxDataVal > 0f) maxDataVal * 1.1f else 100f

        val paint = Paint().apply {
            color = textPaintColor
            textSize = 9.sp.toPx()
            textAlign = Paint.Align.RIGHT
        }

        for (i in 1..4) {
            val labelVal = (maxVal * i / 4).toInt()
            val y = height - (labelVal / maxVal * height)
            val pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
            val linePaint = Paint().apply {
                color = textPaintColor
                alpha = 30
                strokeWidth = 1.dp.toPx()
                style = Paint.Style.STROKE
                setPathEffect(pathEffect)
            }
            drawContext.canvas.nativeCanvas.drawLine(0f, y, width, y, linePaint)
            drawContext.canvas.nativeCanvas.drawText(
                labelVal.toString(),
                -8.dp.toPx(),
                y + 4.dp.toPx(),
                paint
            )
        }

        if (displayData.isEmpty()) return@Canvas

        val stepX = if (displayData.size > 1) width / (displayData.size - 1) else width
        val path = Path()

        displayData.forEachIndexed { index, value ->
            val x = index * stepX
            val y = height - (value.coerceAtMost(maxVal) / maxVal * height)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(path, primaryColor, style = Stroke(width = 2.dp.toPx()))
    }
}

@Composable
fun PingDetailRow(
    seq: Int,
    result: PingResult,
    isRunning: Boolean
) {
    val context = LocalContext.current
    val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(result.timestamp))
    val copyText = "Seq #$seq: ${if (result.isSuccessful) "${result.time}ms" else "Timeout"} (ttl=${result.ttl})"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isRunning) {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Ping Result", copyText))
                Toast.makeText(context, "Copied Seq #$seq", Toast.LENGTH_SHORT).show()
            }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (result.isSuccessful) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                        )
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Seq #$seq",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = TechMonospace,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = time,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = if (result.isSuccessful) "${String.format(Locale.US, "%.1f", result.time)} ms" else "Timeout",
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = TechMonospace,
                color = if (result.isSuccessful) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
            if (result.isSuccessful) {
                Text(
                    text = "ttl=${result.ttl}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = TechMonospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
