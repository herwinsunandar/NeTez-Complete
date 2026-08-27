package com.herwin.netez.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.R
import com.herwin.netez.ui.theme.PremiumNavy
import com.herwin.netez.ui.theme.SoftDarkBg
import com.herwin.netez.ui.theme.TextGray
import com.herwin.netez.ui.viewmodels.HistoryViewModel
import com.herwin.netez.ui.viewmodels.PingViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PingScreen(
    viewModel: PingViewModel = viewModel(),
    historyViewModel: HistoryViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSettings by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HostInputSection(
            host = uiState.host,
            history = uiState.hostHistory,
            isRunning = uiState.isRunning,
            onHostChange = { viewModel.updateHost(it) },
            onTogglePing = {
                if (uiState.isRunning) {
                    historyViewModel.addResult("Ping", uiState.host, "Avg: ${String.format(Locale.US, "%.1f", uiState.avgLatency)}ms")
                }
                viewModel.togglePing()
            }
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Statistics", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            IconButton(onClick = { 
                viewModel.copyResultsToClipboard()
                Toast.makeText(context, "Results copied to clipboard", Toast.LENGTH_SHORT).show()
            }) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Results", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PingStatCard(Modifier.weight(1f), "Avg Latency", "${String.format(Locale.US, "%.1f", uiState.avgLatency)} ms", MaterialTheme.colorScheme.primary)
            val lossColor = when {
                uiState.packetLoss == 0 -> Color(0xFF10B981)
                uiState.packetLoss < 5 -> Color(0xFFFBBF24)
                else -> Color(0xFFEF4444)
            }
            PingStatCard(Modifier.weight(1f), "Packet Loss", "${uiState.packetLoss}%", lossColor, isHighlighted = true)
        }
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PingStatCard(Modifier.weight(1f), "Jitter", "${String.format(Locale.US, "%.1f", uiState.jitter)} ms", MaterialTheme.colorScheme.secondary)
            PingStatCard(Modifier.weight(1f), "Min/Max", "${uiState.minLatency.toInt()}/${uiState.maxLatency.toInt()} ms", MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Latency Graph (ms)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Surface(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                LatencyLineChart(uiState.results.map { if (it.isSuccessful) it.time.toFloat() else 0f })
            }
        }

        Surface(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column {
                LazyColumn(
                    modifier = Modifier.weight(1f).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(uiState.results.reversed()) { index, result ->
                        PingDetailRow(uiState.results.size - index, result, isRunning = uiState.isRunning)
                        if (index < uiState.results.size - 1) HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    }
                }
                
                if (uiState.results.isNotEmpty() && !uiState.isRunning) {
                    com.herwin.netez.ui.components.ResultFooter(
                        fullTextToCopy = "Ping Results for ${uiState.host}\nAvg: ${uiState.avgLatency}ms, Loss: ${uiState.packetLoss}%",
                        onCopyAll = { viewModel.copyResultsToClipboard() }
                    )
                }
            }
        }
    }

    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            PingSettingsContent(
                interval = uiState.intervalMs,
                packetSize = uiState.packetSize,
                count = uiState.count,
                onSave = { i, s, c ->
                    viewModel.updateSettings(i, s, c)
                    showSettings = false
                }
            )
        }
    }
}

@Composable
fun PingStatCard(modifier: Modifier, label: String, value: String, color: Color, isHighlighted: Boolean = false) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = if (isHighlighted) color.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface,
        tonalElevation = if (isHighlighted) 0.dp else 2.dp,
        border = if (isHighlighted) BorderStroke(1.dp, color.copy(alpha = 0.3f)) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = color)
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

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                ExposedDropdownMenuBox(
                    expanded = expanded && !isRunning,
                    onExpandedChange = { if (!isRunning) expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = host,
                        onValueChange = onHostChange,
                        placeholder = { Text("8.8.8.8", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        enabled = !isRunning,
                        shape = RoundedCornerShape(16.dp),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                            focusedContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.5f)
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expanded && !isRunning,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        history.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item, color = MaterialTheme.colorScheme.onSurface) },
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
                modifier = Modifier.height(56.dp).width(90.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (isRunning) "STOP" else "START", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun LatencyLineChart(data: List<Float>) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    
    Canvas(modifier = Modifier.fillMaxSize().padding(start = 40.dp, end = 16.dp, top = 20.dp, bottom = 20.dp)) {
        val width = size.width
        val height = size.height
        
        val displayData = data.takeLast(30)
        val maxDataVal = displayData.maxOrNull() ?: 0f
        val maxVal = if (maxDataVal > 0f) maxDataVal * 1.1f else 100f // Give 10% headroom
        
        val paint = android.graphics.Paint().apply {
            color = axisColor.toArgb()
            textSize = 9.dp.toPx()
            textAlign = android.graphics.Paint.Align.RIGHT
        }

        // Draw 5 dynamic labels and grid lines
        for (i in 1..5) {
            val labelVal = (maxVal * i / 5).toInt()
            val y = height - (labelVal / maxVal * height)
            
            // Draw dotted line
            val pathEffect = android.graphics.DashPathEffect(floatArrayOf(10f, 10f), 0f)
            val linePaint = android.graphics.Paint().apply {
                color = axisColor.toArgb()
                strokeWidth = 1.dp.toPx()
                style = android.graphics.Paint.Style.STROKE
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
        
        drawPath(
            path = path, 
            color = primaryColor, 
            style = Stroke(width = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
        )
        
        // Fill area under path
        val fillPath = android.graphics.Path()
        displayData.forEachIndexed { index, value ->
            val x = index * stepX
            val y = height - (value.coerceAtMost(maxVal) / maxVal * height)
            if (index == 0) fillPath.moveTo(x, y) else fillPath.lineTo(x, y)
        }
        fillPath.lineTo((displayData.size - 1) * stepX, height)
        fillPath.lineTo(0f, height)
        fillPath.close()
        
        val fillPaint = android.graphics.Paint().apply {
            shader = android.graphics.LinearGradient(
                0f, 0f, 0f, height,
                primaryColor.copy(alpha = 0.3f).toArgb(),
                Color.Transparent.toArgb(),
                android.graphics.Shader.TileMode.CLAMP
            )
        }
        drawContext.canvas.nativeCanvas.drawPath(fillPath, fillPaint)
    }
}

private fun Color.toArgb(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt(),
    (red * 255).toInt(),
    (green * 255).toInt(),
    (blue * 255).toInt()
)

@Composable
fun PingDetailRow(seq: Int, result: com.herwin.netez.network.PingResult, isRunning: Boolean) {
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
                    modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp))
                        .background(if (result.isSuccessful) Color(0xFF10B981) else Color(0xFFEF4444))
                )
                Spacer(Modifier.width(12.dp))
                Text("Seq #$seq", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            }
            Text(time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = if (result.isSuccessful) "${String.format(Locale.US, "%.1f", result.time)} ms" else "Timeout",
                style = MaterialTheme.typography.bodyLarge,
                color = if (result.isSuccessful) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.ExtraBold
            )
            if (result.isSuccessful) {
                Text("ttl=${result.ttl}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun PingSettingsContent(
    interval: Long,
    packetSize: Int,
    count: Int,
    onSave: (Long, Int, Int) -> Unit
) {
    var intVal by remember { mutableStateOf(interval.toString()) }
    var sizeVal by remember { mutableStateOf(packetSize.toString()) }
    var countVal by remember { mutableStateOf(count.toString()) }

    Column(
        modifier = Modifier.padding(24.dp).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Ping Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        
        OutlinedTextField(
            value = intVal,
            onValueChange = { intVal = it },
            label = { Text("Interval (ms)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
        )
        
        OutlinedTextField(
            value = sizeVal,
            onValueChange = { sizeVal = it },
            label = { Text("Packet Size (bytes)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
        )
        
        OutlinedTextField(
            value = countVal,
            onValueChange = { countVal = it },
            label = { Text("Count (0 = infinite)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
        )
        
        Button(
            onClick = {
                val i = intVal.toLongOrNull() ?: 1000L
                val s = sizeVal.toIntOrNull() ?: 56
                val c = countVal.toIntOrNull() ?: 0
                onSave(i, s, c)
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("APPLY SETTINGS", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(32.dp))
    }
}
