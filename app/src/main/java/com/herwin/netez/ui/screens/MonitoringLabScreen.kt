package com.herwin.netez.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.R
import com.herwin.netez.data.MonitoringLog
import com.herwin.netez.data.MonitoringTarget
import com.herwin.netez.data.MonitoringType
import com.herwin.netez.entitlement.FeatureAccessManager
import com.herwin.netez.entitlement.FeatureId
import com.herwin.netez.ui.components.ContextualPaywallDialog
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.theme.TechMonospace
import com.herwin.netez.ui.viewmodels.MonitoringLabViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitoringLabScreen(
    viewModel: MonitoringLabViewModel = viewModel(),
    onBack: () -> Unit = {},
    onNavigateToDetail: (Long) -> Unit
) {
    val targets by viewModel.targets.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var targetToDelete by remember { mutableStateOf<MonitoringTarget?>(null) }

    if (viewModel.showPaywallDialog) {
        ContextualPaywallDialog(
            featureId = FeatureId.MONITORING_TARGETS_MAX,
            limitText = "Free plan supports up to 3 monitoring targets. Upgrade to NeTez PRO for up to 50 targets with faster 1-minute intervals.",
            onDismiss = { viewModel.showPaywallDialog = false },
            onUpgradeClick = {}
        )
    }

    val context = LocalContext.current
    val isProUser = remember(context) { FeatureAccessManager.getInstance(context).isPro() }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (!isProUser && targets.size >= 3) {
                        viewModel.showPaywallDialog = true
                    } else {
                        showAddDialog = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(
                    painter = AppIcons.add(),
                    contentDescription = "Add Monitor"
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            MonitoringSummary(targets)

            val overflowMonitoringCount = targets.size
            if (!isProUser && overflowMonitoringCount > 3) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = AppIcons.info(),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "You currently have $overflowMonitoringCount monitoring targets (Free limit is 3). All your existing targets remain active. Re-subscribe to NeTez PRO to add new targets.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            if (targets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            painter = AppIcons.monitor(),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No Monitored Targets Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tap the + button below to start monitoring a server, IP, or URL.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(targets, key = { it.id }) { target ->
                        MonitoringTargetCard(
                            target = target,
                            onToggle = { viewModel.toggleTarget(target) },
                            onDelete = { targetToDelete = target },
                            onClick = { onNavigateToDetail(target.id) },
                            viewModel = viewModel
                        )
                    }
                }
            }
        }

        if (targetToDelete != null) {
            val target = targetToDelete!!
            val targetName = target.host ?: target.url ?: "Target #${target.id}"
            AlertDialog(
                onDismissRequest = { targetToDelete = null },
                containerColor = MaterialTheme.colorScheme.surface,
                title = {
                    Text(
                        text = "Delete Monitor Target?",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to delete '$targetName'? All associated uptime history logs will be permanently deleted.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteTarget(target)
                            targetToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.btn_delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { targetToDelete = null }) {
                        Text(stringResource(R.string.btn_cancel))
                    }
                }
            )
        }

        if (showAddDialog) {
            AddMonitorDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { type, host, port, url, keyword, interval ->
                    viewModel.addTarget(type, host, port, url, null, keyword, interval)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun MonitoringSummary(targets: List<MonitoringTarget>) {
    val upCount = targets.count { it.isActive && it.lastStatus == "UP" }
    val downCount = targets.count { it.isActive && it.lastStatus == "DOWN" }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryCard(
            modifier = Modifier.weight(1f),
            label = "UP",
            value = upCount.toString(),
            color = MaterialTheme.colorScheme.secondary,
            icon = AppIcons.checkCircle()
        )
        SummaryCard(
            modifier = Modifier.weight(1f),
            label = "DOWN",
            value = downCount.toString(),
            color = MaterialTheme.colorScheme.error,
            icon = AppIcons.error()
        )
        SummaryCard(
            modifier = Modifier.weight(1f),
            label = "TOTAL",
            value = targets.size.toString(),
            color = MaterialTheme.colorScheme.primary,
            icon = AppIcons.monitor()
        )
    }
}

@Composable
fun SummaryCard(
    modifier: Modifier,
    label: String,
    value: String,
    color: Color,
    icon: Painter
) {
    Surface(
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = TechMonospace,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun MonitoringTargetCard(
    target: MonitoringTarget,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    viewModel: MonitoringLabViewModel
) {
    val logs by viewModel.getLogs(target.id).collectAsState(initial = emptyList())
    val sparklineLogs = remember(logs) { logs.take(15).reversed() }

    val (statusColor, statusText) = when (target.lastStatus) {
        "UP" -> Pair(MaterialTheme.colorScheme.secondary, "ONLINE")
        "DOWN" -> Pair(MaterialTheme.colorScheme.error, "OFFLINE")
        else -> Pair(MaterialTheme.colorScheme.onSurfaceVariant, "PENDING")
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Status Badge + Latency/Sparkline + Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Status Badge Chip
                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = statusText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }

                // Sparkline Graph & Latency Value
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (sparklineLogs.isNotEmpty()) {
                        Sparkline(
                            logs = sparklineLogs,
                            modifier = Modifier.size(60.dp, 24.dp)
                        )
                    }

                    if (target.lastLatency != null) {
                        Text(
                            text = "${target.lastLatency} ms",
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = TechMonospace,
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                // Switch Toggle & Delete Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Switch(
                        checked = target.isActive,
                        onCheckedChange = { onToggle() },
                        modifier = Modifier.scale(0.75f)
                    )
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            painter = AppIcons.delete(),
                            contentDescription = "Delete Target",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Target Name & Info Section (Full Width, No Truncation)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = getTargetDisplayName(target),
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = TechMonospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${target.type.name} Monitor",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.monitor_every, target.intervalMillis / 60000),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun Sparkline(logs: List<MonitoringLog>, modifier: Modifier) {
    if (logs.isEmpty()) return

    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error

    val maxLatency = (logs.maxOfOrNull { it.latency } ?: 100L).coerceAtLeast(10L).toFloat()
    val minLatency = (logs.minOfOrNull { it.latency } ?: 0L).toFloat()
    val range = (maxLatency - minLatency).coerceAtLeast(1f)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        if (logs.size == 1) {
            val normalizedY = ((logs[0].latency - minLatency) / range * (height * 0.8f)) + (height * 0.1f)
            val y = height - normalizedY
            drawCircle(color = primaryColor, radius = 3.dp.toPx(), center = Offset(width / 2, y))
        } else {
            val path = Path()
            val step = width / (logs.size - 1)

            logs.forEachIndexed { index, log ->
                val x = index * step
                val normalizedY = ((log.latency - minLatency) / range * (height * 0.8f)) + (height * 0.1f)
                val y = height - normalizedY

                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)

                drawCircle(
                    color = if (log.status == "UP") primaryColor else errorColor,
                    radius = 1.5.dp.toPx(),
                    center = Offset(x, y)
                )
            }

            drawPath(
                path = path,
                color = primaryColor.copy(alpha = 0.7f),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

private fun getTargetDisplayName(target: MonitoringTarget): String {
    return when (target.type) {
        MonitoringType.PING -> target.host ?: "Unspecified Host"
        MonitoringType.PORT -> "${target.host ?: ""}:${target.port ?: ""}"
        MonitoringType.HTTP -> target.url ?: "Unspecified URL"
        MonitoringType.DNS -> target.host ?: "Unspecified Host"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMonitorDialog(
    onDismiss: () -> Unit,
    onAdd: (MonitoringType, String?, Int?, String?, String?, Int) -> Unit
) {
    var type by remember { mutableStateOf(MonitoringType.PING) }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("https://") }
    var keyword by remember { mutableStateOf("") }
    var interval by remember { mutableFloatStateOf(1f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = stringResource(R.string.monitor_add_title),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // Type Choice Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MonitoringType.entries.forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(text = t.name, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                when (type) {
                    MonitoringType.PING, MonitoringType.DNS -> {
                        OutlinedTextField(
                            value = host,
                            onValueChange = { host = it },
                            label = { Text(stringResource(R.string.monitor_field_host)) },
                            placeholder = { Text("e.g. 8.8.8.8 or google.com", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    MonitoringType.PORT -> {
                        OutlinedTextField(
                            value = host,
                            onValueChange = { host = it },
                            label = { Text(stringResource(R.string.monitor_field_host)) },
                            placeholder = { Text("e.g. 192.168.1.1", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        OutlinedTextField(
                            value = port,
                            onValueChange = { port = it },
                            label = { Text(stringResource(R.string.monitor_field_port)) },
                            placeholder = { Text("e.g. 80 or 443", fontSize = 12.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    MonitoringType.HTTP -> {
                        OutlinedTextField(
                            value = url,
                            onValueChange = { url = it },
                            label = { Text(stringResource(R.string.monitor_field_url)) },
                            placeholder = { Text("https://example.com/health", fontSize = 12.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        OutlinedTextField(
                            value = keyword,
                            onValueChange = { keyword = it },
                            label = { Text(stringResource(R.string.monitor_field_keyword)) },
                            placeholder = { Text("e.g. OK (Optional)", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.monitor_interval, interval.toInt()),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Slider(
                    value = interval,
                    onValueChange = { interval = it },
                    valueRange = 1f..60f,
                    steps = 59
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(type, host, port.toIntOrNull(), url, keyword, interval.toInt()) },
                enabled = when (type) {
                    MonitoringType.PING, MonitoringType.DNS -> host.isNotBlank()
                    MonitoringType.PORT -> host.isNotBlank() && port.toIntOrNull() != null
                    MonitoringType.HTTP -> url.isNotBlank()
                }
            ) {
                Text(stringResource(R.string.btn_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitoringLogsBottomSheet(
    target: MonitoringTarget,
    logs: List<MonitoringLog>,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxHeight(0.6f)
        ) {
            Text(
                text = stringResource(R.string.monitor_logs_title, getTargetDisplayName(target)),
                style = MaterialTheme.typography.titleMedium,
                fontFamily = TechMonospace,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(logs) { log ->
                    LogItem(log)
                }
            }
        }
    }
}

@Composable
fun LogItem(log: MonitoringLog) {
    val sdf = SimpleDateFormat("HH:mm:ss dd/MM", Locale.getDefault())
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val color = if (log.status == "UP") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.message ?: log.status,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp
                )
                Text(
                    text = sdf.format(Date(log.timestamp)),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
            Text(
                text = "${log.latency} ms",
                fontFamily = TechMonospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
