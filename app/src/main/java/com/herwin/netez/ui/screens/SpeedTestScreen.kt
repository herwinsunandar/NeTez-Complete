package com.herwin.netez.ui.screens

import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.theme.PremiumNavy
import com.herwin.netez.ui.theme.SoftDarkBg
import com.herwin.netez.ui.theme.TextGray
import com.herwin.netez.ui.viewmodels.SpeedTestStage
import com.herwin.netez.ui.viewmodels.SpeedTestViewModel
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedTestScreen(viewModel: SpeedTestViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var showServerDialog by remember { mutableStateOf(false) }

    val currentSpeed = if (uiState.stage == SpeedTestStage.UPLOAD) uiState.uploadSpeed else uiState.downloadSpeed
    
    val animatedSpeed by animateFloatAsState(
        targetValue = currentSpeed.toFloat(),
        animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing),
        label = "SpeedAnimation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PremiumNavy)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 16.dp, bottom = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top Metrics Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard("PING", "${uiState.pingMs.toInt()}ms", AppIcons.timer(), Color(0xFF8B5CF6), uiState.stage == SpeedTestStage.PING, Modifier.weight(1f))
                MetricCard("JITTER", "${uiState.jitterMs.toInt()}ms", AppIcons.graphicEq(), Color(0xFF8B5CF6), uiState.stage == SpeedTestStage.PING, Modifier.weight(1f))
                MetricCard("DOWN", String.format(Locale.US, "%.1f", uiState.downloadSpeed), AppIcons.arrowDownward(), Color(0xFF6C8CFF), uiState.stage == SpeedTestStage.DOWNLOAD, Modifier.weight(1f))
                MetricCard("UP", String.format(Locale.US, "%.1f", uiState.uploadSpeed), AppIcons.arrowUpward(), Color(0xFF8B5CF6), uiState.stage == SpeedTestStage.UPLOAD, Modifier.weight(1f))
            }

            Spacer(Modifier.height(24.dp))

            // 2. Center Gauge
            SpeedometerView(
                speed = animatedSpeed,
                status = when(uiState.stage) {
                    SpeedTestStage.IDLE -> "READY"
                    SpeedTestStage.PING -> "PINGING"
                    SpeedTestStage.DOWNLOAD -> "DOWNLOADING"
                    SpeedTestStage.UPLOAD -> "UPLOADING"
                    SpeedTestStage.FINISHED -> "FINISHED"
                },
                isRunning = uiState.isRunning
            )

            Spacer(Modifier.height(16.dp))

            // 3. Live Informative Chart
            DualSpeedChart(
                downloadHistory = uiState.downloadHistory,
                uploadHistory = uiState.uploadHistory,
                isRunning = uiState.isRunning,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(horizontal = 24.dp)
            )
            
            if (uiState.isRunning || uiState.stage == SpeedTestStage.FINISHED) {
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem("Download", Color(0xFF6C8CFF))
                    LegendItem("Upload", Color(0xFF8B5CF6))
                }
            }

            Spacer(Modifier.height(16.dp))

            // 4. Result Summary Card
            if (uiState.stage == SpeedTestStage.FINISHED) {
                Surface(
                    modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
                    color = SoftDarkBg.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Text(
                        text = "Download ${String.format(Locale.US, "%.1f", uiState.downloadSpeed)} Mbps • Upload ${String.format(Locale.US, "%.1f", uiState.uploadSpeed)} Mbps\nPing ${uiState.pingMs.toInt()}ms • Jitter ${uiState.jitterMs.toInt()}ms • ${uiState.selectedServer}",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            // 5. Server Selector
            Surface(
                onClick = { if (!uiState.isRunning) showServerDialog = true },
                color = SoftDarkBg,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(AppIcons.dns(), null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(uiState.selectedServer, color = Color.White, fontSize = 12.sp)
                    if (!uiState.isRunning) {
                        Spacer(Modifier.width(6.6.dp))
                        Icon(AppIcons.edit(), null, tint = TextGray, modifier = Modifier.size(12.dp))
                    }
                }
            }
        }

        // Fixed Bottom Buttons
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = PremiumNavy,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (uiState.stage == SpeedTestStage.FINISHED) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.startTest() },
                            modifier = Modifier.weight(1.2f).height(56.dp),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C8CFF))
                        ) {
                            Text("TEST AGAIN", fontWeight = FontWeight.Bold, color = Color(0xFF1A1D3D))
                        }
                        OutlinedButton(
                            onClick = {
                                val resultText = "Speed Test Result: ${uiState.downloadSpeed} Mbps Down / ${uiState.uploadSpeed} Mbps Up"
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, resultText)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Result"))
                            },
                            modifier = Modifier.weight(0.8f).height(56.dp),
                            shape = RoundedCornerShape(28.dp),
                            border = BorderStroke(1.dp, Color(0xFF2A2E45))
                        ) {
                            Icon(AppIcons.share(), null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("SHARE", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                } else {
                    Button(
                        onClick = { if (uiState.isRunning) viewModel.stopTest() else viewModel.startTest() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.isRunning) Color(0xFFEF4444) else Color(0xFF6C8CFF)
                        )
                    ) {
                        Text(if (uiState.isRunning) "STOP TEST" else "START TEST", fontWeight = FontWeight.Bold, color = if (uiState.isRunning) Color.White else Color(0xFF1A1D3D))
                    }
                }
            }
        }
    }

    if (showServerDialog) {
        ServerSelectionDialog(
            currentServer = uiState.selectedServer,
            onDismiss = { showServerDialog = false },
            onSelect = { 
                viewModel.updateServer(it)
                showServerDialog = false
            }
        )
    }
}

@Composable
fun MetricCard(label: String, value: String, icon: Painter, iconColor: Color, isActive: Boolean, modifier: Modifier) {
    Surface(
        modifier = modifier.border(
            width = if (isActive) 1.dp else 0.dp,
            color = if (isActive) Color(0xFF6C8CFF).copy(alpha = 0.5f) else Color.Transparent,
            shape = RoundedCornerShape(16.dp)
        ),
        color = SoftDarkBg,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(14.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 10.sp, color = Color(0xFF8B92B5))
            Text(value, fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SpeedometerView(speed: Float, status: String, isRunning: Boolean) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(280.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val size = size.minDimension - strokeWidth
            
            drawArc(
                color = Color(0xFF2A2E45),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = Size(size, size),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            val progress = (speed / 1000f).coerceIn(0f, 1f)
            drawArc(
                brush = Brush.sweepGradient(0.0f to Color(0xFF6C8CFF), 0.75f to Color(0xFF8B5CF6), center = center),
                startAngle = 135f,
                sweepAngle = 270f * progress,
                useCenter = false,
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = Size(size, size),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = String.format(Locale.US, "%.1f", speed), fontSize = 64.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Mbps", fontSize = 16.sp, color = TextGray)
            Spacer(Modifier.height(16.dp))
            Text(text = status, fontSize = 12.sp, color = Color(0xFF8B5CF6), letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DualSpeedChart(downloadHistory: List<Float>, uploadHistory: List<Float>, isRunning: Boolean, modifier: Modifier) {
    Canvas(modifier = modifier) {
        if (!isRunning && downloadHistory.isEmpty()) return@Canvas
        val maxVal = maxOf((downloadHistory + uploadHistory).maxOrNull() ?: 10f, 100f)
        
        if (downloadHistory.size >= 2) {
            val path = Path()
            val xStep = size.width / (downloadHistory.size - 1)
            downloadHistory.forEachIndexed { i, v ->
                val x = i * xStep
                val y = size.height - (v / maxVal * size.height)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, Color(0xFF6C8CFF), style = Stroke(2.dp.toPx()))
        }
        
        if (uploadHistory.size >= 2) {
            val path = Path()
            val xStep = size.width / (uploadHistory.size - 1)
            uploadHistory.forEachIndexed { i, v ->
                val x = i * xStep
                val y = size.height - (v / maxVal * size.height)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, Color(0xFF8B5CF6), style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)))
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(label, color = TextGray, fontSize = 10.sp)
    }
}

@Composable
fun ServerSelectionDialog(currentServer: String, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    var customServer by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SoftDarkBg,
        title = { Text("Select Test Server", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { onSelect("speed.cloudflare.com") }, modifier = Modifier.fillMaxWidth()) {
                    Text("Cloudflare Global")
                }
                OutlinedTextField(value = customServer, onValueChange = { customServer = it }, placeholder = { Text("Custom URL") })
            }
        },
        confirmButton = { TextButton(onClick = { if(customServer.isNotEmpty()) onSelect(customServer) }) { Text("OK") } }
    )
}
