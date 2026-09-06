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
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.R
import com.herwin.netez.network.PingResult
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.theme.PremiumNavy
import com.herwin.netez.ui.theme.SoftDarkBg
import com.herwin.netez.ui.theme.TextGray
import com.herwin.netez.ui.viewmodels.HistoryViewModel
import com.herwin.netez.ui.viewmodels.MultiPingHost
import com.herwin.netez.ui.viewmodels.PingMode
import com.herwin.netez.ui.viewmodels.PingState
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
    var showAddHostDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PremiumNavy)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Mode Selector
        TabRow(
            selectedTabIndex = uiState.mode.ordinal,
            containerColor = SoftDarkBg,
            contentColor = Color(0xFF6C8CFF),
            divider = {},
            indicator = { 
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(it[uiState.mode.ordinal]), 
                    color = Color(0xFF6C8CFF)
                ) 
            },
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            PingMode.entries.forEach { mode ->
                Tab(
                    selected = uiState.mode == mode,
                    onClick = { viewModel.setMode(mode) },
                    text = { Text(mode.name, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                )
            }
        }

        if (uiState.mode == PingMode.SINGLE) {
            SinglePingContent(uiState, viewModel, historyViewModel)
        } else {
            MultiPingContent(uiState, viewModel, onAddClick = { showAddHostDialog = true })
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
fun SinglePingContent(uiState: PingState, viewModel: PingViewModel, historyViewModel: HistoryViewModel) {
    val context = LocalContext.current
    
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
            Text("Statistics", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
            Row {
                IconButton(onClick = { 
                    viewModel.copyResultsToClipboard()
                    Toast.makeText(context, "Results copied to clipboard", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(AppIcons.contentCopy(), contentDescription = "Copy Results", tint = Color(0xFF6C8CFF))
                }
                IconButton(onClick = { 
                    viewModel.shareResults(context)
                }) {
                    Icon(AppIcons.share(), contentDescription = "Share Results", tint = Color(0xFF6C8CFF))
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PingStatCard(Modifier.weight(1f), "Avg Latency", "${String.format(Locale.US, "%.1f", uiState.avgLatency)} ms", Color(0xFF6C8CFF))
            val lossColor = when {
                uiState.packetLoss == 0 -> Color(0xFF10B981)
                uiState.packetLoss < 5 -> Color(0xFFFBBF24)
                else -> Color(0xFFEF4444)
            }
            PingStatCard(Modifier.weight(1f), "Packet Loss", "${uiState.packetLoss}%", lossColor, isHighlighted = true)
        }
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PingStatCard(Modifier.weight(1f), "Jitter", "${String.format(Locale.US, "%.1f", uiState.jitter)} ms", Color(0xFF8B5CF6))
            PingStatCard(Modifier.weight(1f), "Min/Max", "${uiState.minLatency.toInt()}/${uiState.maxLatency.toInt()} ms", TextGray)
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Latency Graph (ms)", style = MaterialTheme.typography.labelSmall, color = TextGray)
            Surface(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                shape = RoundedCornerShape(24.dp),
                color = SoftDarkBg,
                tonalElevation = 2.dp
            ) {
                LatencyLineChart(uiState.results.map { if (it.isSuccessful) it.time.toFloat() else 0f })
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = SoftDarkBg,
            tonalElevation = 2.dp
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(uiState.results.reversed()) { index, result ->
                    PingDetailRow(uiState.results.size - index, result, isRunning = uiState.isRunning)
                    if (index < uiState.results.size - 1) HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                }
            }
        }
    }
}

@Composable
fun MultiPingContent(uiState: PingState, viewModel: PingViewModel, onAddClick: () -> Unit) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Hosts to Ping", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { 
                    viewModel.copyResultsToClipboard()
                    Toast.makeText(context, "Results copied", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(AppIcons.contentCopy(), null, tint = Color(0xFF6C8CFF), modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { viewModel.shareResults(context) }) {
                    Icon(AppIcons.share(), null, tint = Color(0xFF6C8CFF), modifier = Modifier.size(18.dp))
                }
                TextButton(onClick = onAddClick, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF6C8CFF))) {
                    Icon(AppIcons.add(), contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Host")
                }
            }
        }

        LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(uiState.multiHosts) { host ->
                MultiHostCard(host, isRunning = uiState.isRunning, 
                    onToggle = { viewModel.toggleMultiHost(host) },
                    onDelete = { viewModel.removeMultiHost(host) }
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth().height(200.dp),
            shape = RoundedCornerShape(20.dp),
            color = SoftDarkBg,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            MultiLatencyChart(uiState.multiHosts.filter { it.isEnabled })
        }

        // Multi Stats Grid
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val enabledHosts = uiState.multiHosts.filter { it.isEnabled }
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Host", modifier = Modifier.weight(1.5f), color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("Avg", modifier = Modifier.weight(1f), color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                    Text("Loss", modifier = Modifier.weight(1f), color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
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
                            "Best: ${best.host} (${String.format("%.1f", best.avgLatency)}ms), Worst: ${worst?.host} (${String.format("%.1f", worst?.avgLatency)}ms)",
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            color = Color(0xFF6C8CFF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Button(
            onClick = { viewModel.togglePing() },
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(bottom = 8.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (uiState.isRunning) Color(0xFFEF4444) else Color(0xFF6C8CFF)
            )
        ) {
            Text(if (uiState.isRunning) "STOP MULTI PING" else "START MULTI PING", fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun MultiHostCard(host: MultiPingHost, isRunning: Boolean, onToggle: () -> Unit, onDelete: () -> Unit) {
    Surface(
        modifier = Modifier.width(160.dp),
        shape = RoundedCornerShape(16.dp),
        color = SoftDarkBg,
        border = BorderStroke(1.dp, if (host.isEnabled) Color(host.color).copy(alpha = 0.3f) else Color.Transparent)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.size(10.dp).background(Color(host.color), CircleShape))
                IconButton(onClick = onDelete, modifier = Modifier.size(20.dp), enabled = !isRunning) {
                    Icon(AppIcons.close(), null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(host.host, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(host.label.ifEmpty { "Host" }, color = TextGray, fontSize = 10.sp)
            
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "${host.lastLatency.toInt()}ms", 
                    color = if (host.lastLatency < 50) Color(0xFF10B981) else if (host.lastLatency < 100) Color(0xFFFBBF24) else Color(0xFFEF4444),
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
                Switch(
                    checked = host.isEnabled, 
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.scale(0.6f),
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
        color = Color.White.copy(alpha = 0.03f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).background(Color(host.color), CircleShape))
            Spacer(Modifier.width(12.dp))
            Text(host.host, modifier = Modifier.weight(1.5f), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text("${String.format("%.1f", host.avgLatency)} ms", modifier = Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End)
            Text("${host.packetLoss}%", modifier = Modifier.weight(1f), color = if (host.packetLoss > 0) Color(0xFFEF4444) else Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End)
        }
    }
}

@Composable
fun MultiLatencyChart(hosts: List<MultiPingHost>) {
    Canvas(modifier = Modifier.fillMaxSize().padding(start = 40.dp, end = 16.dp, top = 30.dp, bottom = 20.dp)) {
        val width = size.width
        val height = size.height
        
        // Grid & Scale
        val maxLatency = hosts.flatMap { it.results }.map { it.time.toFloat() }.maxOrNull()?.coerceAtLeast(100f) ?: 150f
        val maxVal = maxLatency * 1.2f
        
        // Draw Grid
        val axisColor = Color.White.copy(alpha = 0.1f)
        for (i in 1..4) {
            val y = height - (i * height / 4)
            drawLine(color = axisColor, start = Offset(0f, y), end = Offset(width, y), strokeWidth = 1.dp.toPx())
            drawContext.canvas.nativeCanvas.drawText(
                (maxVal * i / 4).toInt().toString(),
                -8.dp.toPx(),
                y + 4.dp.toPx(),
                Paint().apply { color = android.graphics.Color.GRAY; textSize = 8.sp.toPx(); textAlign = Paint.Align.RIGHT }
            )
        }

        hosts.forEach { host ->
            if (host.results.size < 2) return@forEach
            val data = host.results.takeLast(20)
            val stepX = width / 19 // Fixed slots for 20 points
            val path = Path()
            
            data.forEachIndexed { index, result ->
                val x = index * stepX
                val y = if (result.isSuccessful) {
                    height - (result.time.toFloat() / maxVal * height).coerceIn(0f, height)
                } else {
                    height // Packet loss at bottom
                }
                
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                
                // Draw Packet Loss X
                if (!result.isSuccessful) {
                    val xSize = 4.dp.toPx()
                    drawLine(Color.Red, Offset(x - xSize, height - xSize), Offset(x + xSize, height), strokeWidth = 2.dp.toPx())
                    drawLine(Color.Red, Offset(x + xSize, height - xSize), Offset(x - xSize, height), strokeWidth = 2.dp.toPx())
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
        containerColor = SoftDarkBg,
        title = { Text("Add Host to Multi Ping", color = Color.White) },
        text = {
            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                placeholder = { Text("e.g. cloudflare.com", color = TextGray) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedTextColor = Color.White,
                    focusedTextColor = Color.White
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
fun PingStatCard(modifier: Modifier, label: String, value: String, color: Color, isHighlighted: Boolean = false) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = if (isHighlighted) color.copy(alpha = 0.1f) else SoftDarkBg,
        tonalElevation = if (isHighlighted) 0.dp else 2.dp,
        border = if (isHighlighted) BorderStroke(1.dp, color.copy(alpha = 0.3f)) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextGray)
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
        color = SoftDarkBg,
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
                        placeholder = { Text("8.8.8.8", color = TextGray) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        enabled = !isRunning,
                        shape = RoundedCornerShape(16.dp),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedTextColor = Color.White,
                            focusedTextColor = Color.White,
                            unfocusedContainerColor = PremiumNavy.copy(alpha = 0.5f),
                            focusedContainerColor = PremiumNavy.copy(alpha = 0.5f)
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expanded && !isRunning,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(SoftDarkBg)
                    ) {
                        history.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item, color = Color.White) },
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
                    containerColor = if (isRunning) Color(0xFFEF4444) else Color(0xFF6C8CFF)
                )
            ) {
                Text(if (isRunning) "STOP" else "START", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun LatencyLineChart(data: List<Float>) {
    val primaryColor = Color(0xFF6C8CFF)
    val axisColor = TextGray.copy(alpha = 0.5f)
    
    Canvas(modifier = Modifier.fillMaxSize().padding(start = 40.dp, end = 16.dp, top = 20.dp, bottom = 20.dp)) {
        val width = size.width
        val height = size.height
        
        val displayData = data.takeLast(30)
        val maxDataVal = displayData.maxOrNull() ?: 0f
        val maxVal = if (maxDataVal > 0f) maxDataVal * 1.1f else 100f
        
        val paint = Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 9.dp.toPx()
            textAlign = Paint.Align.RIGHT
        }

        for (i in 1..5) {
            val labelVal = (maxVal * i / 5).toInt()
            val y = height - (labelVal / maxVal * height)
            val pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
            val linePaint = Paint().apply {
                color = android.graphics.Color.argb(50, 255, 255, 255)
                strokeWidth = 1.dp.toPx()
                style = Paint.Style.STROKE
                setPathEffect(pathEffect)
            }
            drawContext.canvas.nativeCanvas.drawLine(0f, y, width, y, linePaint)
            drawContext.canvas.nativeCanvas.drawText(labelVal.toString(), -8.dp.toPx(), y + 4.dp.toPx(), paint)
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
fun PingDetailRow(seq: Int, result: PingResult, isRunning: Boolean) {
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
                Text("Seq #$seq", style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Text(time, style = MaterialTheme.typography.labelSmall, color = TextGray)
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = if (result.isSuccessful) "${String.format(Locale.US, "%.1f", result.time)} ms" else "Timeout",
                style = MaterialTheme.typography.bodyLarge,
                color = if (result.isSuccessful) Color(0xFF6C8CFF) else Color(0xFFEF4444),
                fontWeight = FontWeight.ExtraBold
            )
            if (result.isSuccessful) {
                Text("ttl=${result.ttl}", style = MaterialTheme.typography.labelSmall, color = TextGray)
            }
        }
    }
}
