package com.herwin.netez.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.R
import com.herwin.netez.data.MonitoringLog
import com.herwin.netez.data.MonitoringTarget
import com.herwin.netez.data.MonitoringType
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.theme.PremiumNavy
import com.herwin.netez.ui.theme.SoftDarkBg
import com.herwin.netez.ui.theme.TextGray
import com.herwin.netez.ui.viewmodels.MonitoringLabViewModel
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitoringLabScreen(
    viewModel: MonitoringLabViewModel = viewModel(),
    onBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit
) {
    val targets by viewModel.targets.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF0F1220),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF6C8CFF),
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(AppIcons.add(), contentDescription = "Add Monitor")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            MonitoringSummary(targets)

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (targets.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No targets monitored yet. Tap + to add one.", color = TextGray)
                        }
                    }
                }
                
                items(targets, key = { it.id }) { target ->
                    MonitoringTargetCard(
                        target = target,
                        onToggle = { viewModel.toggleTarget(target) },
                        onDelete = { viewModel.deleteTarget(target) },
                        onClick = { onNavigateToDetail(target.id) },
                        viewModel = viewModel
                    )
                }
            }
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryCard(Modifier.weight(1f), "UP", upCount.toString(), Color(0xFF10B981), AppIcons.checkCircle())
        SummaryCard(Modifier.weight(1f), "DOWN", downCount.toString(), Color(0xFFEF4444), AppIcons.error())
        SummaryCard(Modifier.weight(1f), "TOTAL", targets.size.toString(), Color(0xFF6C8CFF), AppIcons.monitor())
    }
}

@Composable
fun SummaryCard(modifier: Modifier, label: String, value: String, color: Color, icon: Painter) {
    Surface(
        modifier = modifier.height(70.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1E2235)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = color, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text(label, fontSize = 10.sp, color = Color(0xFF8A8E9A))
            }
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
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

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1E2235)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val statusColor = when (target.lastStatus) {
                "UP" -> Color(0xFF10B981)
                "DOWN" -> Color(0xFFEF4444)
                else -> Color.Gray
            }

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            
            Spacer(Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = getTargetDisplayName(target),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = stringResource(R.string.monitor_every, target.intervalMillis / 60000),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8A8E9A)
                )
            }

            if (sparklineLogs.isNotEmpty()) {
                Sparkline(
                    logs = sparklineLogs,
                    modifier = Modifier.size(70.dp, 30.dp).padding(horizontal = 4.dp)
                )
            }

            if (target.lastLatency != null) {
                Text(
                    text = "${target.lastLatency}ms",
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    fontWeight = FontWeight.Bold
                )
            }

            Switch(
                checked = target.isActive,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF8B5CF6),
                    checkedTrackColor = Color(0xFF8B5CF6).copy(alpha = 0.5f),
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f)
                ),
                modifier = Modifier.scale(0.8f)
            )
            
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(AppIcons.delete(), null, tint = Color(0xFFEF4444).copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun Sparkline(logs: List<MonitoringLog>, modifier: Modifier) {
    if (logs.isEmpty()) return
    
    val maxLatency = (logs.maxOfOrNull { it.latency } ?: 100L).coerceAtLeast(10L).toFloat()
    val minLatency = (logs.minOfOrNull { it.latency } ?: 0L).toFloat()
    val range = (maxLatency - minLatency).coerceAtLeast(1f)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        
        if (logs.size == 1) {
            val normalizedY = ((logs[0].latency - minLatency) / range * (height * 0.8f)) + (height * 0.1f)
            val y = height - normalizedY
            drawCircle(color = Color(0xFF6C8CFF), radius = 3.dp.toPx(), center = Offset(width / 2, y))
        } else {
            val path = Path()
            val step = width / (logs.size - 1)
            
            logs.forEachIndexed { index, log ->
                val x = index * step
                val normalizedY = ((log.latency - minLatency) / range * (height * 0.8f)) + (height * 0.1f)
                val y = height - normalizedY
                
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                
                // Draw nodes
                drawCircle(
                    color = if (log.status == "UP") Color(0xFF6C8CFF) else Color(0xFFEF4444),
                    radius = 1.5.dp.toPx(),
                    center = Offset(x, y)
                )
            }
            
            drawPath(
                path = path,
                color = Color(0xFF6C8CFF).copy(alpha = 0.7f),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

private fun getTargetDisplayName(target: MonitoringTarget): String {
    return when (target.type) {
        MonitoringType.PING -> target.host ?: ""
        MonitoringType.PORT -> "${target.host}:${target.port}"
        MonitoringType.HTTP -> target.url ?: ""
        MonitoringType.DNS -> target.host ?: ""
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMonitorDialog(onDismiss: () -> Unit, onAdd: (MonitoringType, String?, Int?, String?, String?, Int) -> Unit) {
    var type by remember { mutableStateOf(MonitoringType.PING) }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("https://") }
    var keyword by remember { mutableStateOf("") }
    var interval by remember { mutableFloatStateOf(1f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SoftDarkBg,
        title = { Text(stringResource(R.string.monitor_add_title), color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MonitoringType.entries.forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t.name, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White,
                                labelColor = TextGray
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
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }
                    MonitoringType.PORT -> {
                        OutlinedTextField(
                            value = host,
                            onValueChange = { host = it },
                            label = { Text(stringResource(R.string.monitor_field_host)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        OutlinedTextField(
                            value = port,
                            onValueChange = { port = it },
                            label = { Text(stringResource(R.string.monitor_field_port)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }
                    MonitoringType.HTTP -> {
                        OutlinedTextField(
                            value = url,
                            onValueChange = { url = it },
                            label = { Text(stringResource(R.string.monitor_field_url)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        OutlinedTextField(
                            value = keyword,
                            onValueChange = { keyword = it },
                            label = { Text(stringResource(R.string.monitor_field_keyword)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }
                }

                Text(stringResource(R.string.monitor_interval, interval.toInt()), color = Color.White, style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = interval,
                    onValueChange = { interval = it },
                    valueRange = 1f..60f,
                    steps = 59
                )
            }
        },
        confirmButton = {
            Button(onClick = { onAdd(type, host, port.toIntOrNull(), url, keyword, interval.toInt()) }) {
                Text(stringResource(R.string.btn_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) }
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
        containerColor = PremiumNavy
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxHeight(0.6f)) {
            Text(
                text = stringResource(R.string.monitor_logs_title, getTargetDisplayName(target)),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
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
        color = SoftDarkBg,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val color = if (log.status == "UP") Color(0xFF10B981) else Color(0xFFEF4444)
            Box(Modifier.size(6.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(log.message ?: log.status, color = Color.White, fontSize = 13.sp)
                Text(sdf.format(Date(log.timestamp)), color = TextGray, fontSize = 11.sp)
            }
            Text("${log.latency}ms", color = TextGray, fontSize = 12.sp)
        }
    }
}
