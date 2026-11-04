package com.herwin.netez.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.R
import com.herwin.netez.entitlement.FeatureId
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.components.ContextualPaywallDialog
import com.herwin.netez.ui.theme.TechMonospace
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

    if (viewModel.showGlobalSupportDialog) {
        ContextualPaywallDialog(
            featureId = FeatureId.GLOBAL_TOOL_USAGE,
            limitText = "You've performed 3 network & security scans! If NeTez is helpful, consider watching a short video to support development or upgrade to PRO (Skippable).",
            onDismiss = { viewModel.showGlobalSupportDialog = false },
            onUpgradeClick = {}
        )
    }

    val currentSpeed = if (uiState.stage == SpeedTestStage.UPLOAD) uiState.uploadSpeed else uiState.downloadSpeed

    val animatedSpeed by animateFloatAsState(
        targetValue = currentSpeed.toFloat(),
        animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing),
        label = "SpeedAnimation"
    )

    val formattedResult = remember(uiState) {
        """
        🚀 NeTez Speed Test Result
        • Provider: ${uiState.isp}
        • Download: ${String.format(Locale.US, "%.1f", uiState.downloadSpeed)} Mbps
        • Upload: ${String.format(Locale.US, "%.1f", uiState.uploadSpeed)} Mbps
        • Ping: ${uiState.pingMs.toInt()} ms
        • Jitter: ${uiState.jitterMs.toInt()} ms
        • Server: ${uiState.selectedServer}
        """.trimIndent()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 16.dp, bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top Metrics Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard(
                    label = "PING",
                    value = "${uiState.pingMs.toInt()} ms",
                    icon = AppIcons.timer(),
                    iconColor = MaterialTheme.colorScheme.primary,
                    isActive = uiState.stage == SpeedTestStage.PING,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = "JITTER",
                    value = "${uiState.jitterMs.toInt()} ms",
                    icon = AppIcons.graphicEq(),
                    iconColor = MaterialTheme.colorScheme.primary,
                    isActive = uiState.stage == SpeedTestStage.PING,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = "DOWN",
                    value = String.format(Locale.US, "%.1f", uiState.downloadSpeed),
                    icon = AppIcons.arrowDownward(),
                    iconColor = MaterialTheme.colorScheme.primary,
                    isActive = uiState.stage == SpeedTestStage.DOWNLOAD,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = "UP",
                    value = String.format(Locale.US, "%.1f", uiState.uploadSpeed),
                    icon = AppIcons.arrowUpward(),
                    iconColor = MaterialTheme.colorScheme.secondary,
                    isActive = uiState.stage == SpeedTestStage.UPLOAD,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(20.dp))

            // 2. Center Gauge Speedometer
            SpeedometerView(
                speed = animatedSpeed,
                status = when (uiState.stage) {
                    SpeedTestStage.IDLE -> "READY"
                    SpeedTestStage.PING -> "PINGING"
                    SpeedTestStage.DOWNLOAD -> "DOWNLOADING"
                    SpeedTestStage.UPLOAD -> "UPLOADING"
                    SpeedTestStage.FINISHED -> "FINISHED"
                },
                isRunning = uiState.isRunning
            )

            Spacer(Modifier.height(16.dp))

            // 3. Live Dual Speed Chart
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
                    modifier = Modifier.padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem("Download", MaterialTheme.colorScheme.primary)
                    LegendItem("Upload", MaterialTheme.colorScheme.secondary)
                }
            }

            Spacer(Modifier.height(16.dp))

            // 4. Result Summary Card
            if (uiState.stage == SpeedTestStage.FINISHED) {
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Provider: ${uiState.isp}\nDownload ${String.format(Locale.US, "%.1f", uiState.downloadSpeed)} Mbps • Upload ${String.format(Locale.US, "%.1f", uiState.uploadSpeed)} Mbps\nPing ${uiState.pingMs.toInt()} ms • Jitter ${uiState.jitterMs.toInt()} ms • ${uiState.selectedServer}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = TechMonospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            // ISP / Provider Badge
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = AppIcons.public(),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Network Provider: ${uiState.isp}",
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // 5. Server Selector Bar
            Surface(
                onClick = { if (!uiState.isRunning) showServerDialog = true },
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = AppIcons.dns(),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = uiState.selectedServer,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = TechMonospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (!uiState.isRunning) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            painter = AppIcons.edit(),
                            contentDescription = "Edit Server",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // Fixed Bottom Action Control Bar
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (uiState.stage == SpeedTestStage.FINISHED) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // TEST AGAIN
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.startTest()
                            },
                            modifier = Modifier
                                .weight(1.3f)
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                painter = AppIcons.refresh(),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "TEST AGAIN",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        // COPY
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Speed Test Result", formattedResult))
                                Toast.makeText(context, "Result copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                painter = AppIcons.contentCopy(),
                                contentDescription = "Copy",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "COPY",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // SHARE
                        OutlinedButton(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, formattedResult)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Speed Test Result"))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                painter = AppIcons.share(),
                                contentDescription = "Share",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "SHARE",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            if (uiState.isRunning) {
                                viewModel.stopTest()
                            } else {
                                viewModel.startTest()
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
                            painter = if (uiState.isRunning) AppIcons.stop() else AppIcons.speed(),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (uiState.isRunning) "STOP TEST" else "START TEST",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
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
fun MetricCard(
    label: String,
    value: String,
    icon: Painter,
    iconColor: Color,
    isActive: Boolean,
    modifier: Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = 15.sp,
                fontFamily = TechMonospace,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun SpeedometerView(speed: Float, status: String, isRunning: Boolean) {
    val targetProgress = remember(speed) {
        if (speed <= 0f) 0f
        else if (speed <= 100f) (speed / 100f) * 0.65f
        else 0.65f + ((speed - 100f) / 900f) * 0.35f
    }.coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "GaugeArcProgress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "PulseGlow")
    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AuraAlpha"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val activeGlowColor = MaterialTheme.colorScheme.secondary
    val hotPinkColor = MaterialTheme.colorScheme.tertiary
    val trackBgColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(280.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val strokeWidth = 16.dp.toPx()
            val radius = (size.minDimension - strokeWidth - 28.dp.toPx()) / 2f
            val arcSize = Size(radius * 2f, radius * 2f)
            val arcTopLeft = Offset(center.x - radius, center.y - radius)

            val startAngle = 135f
            val maxSweepAngle = 270f
            val currentSweepAngle = maxSweepAngle * animatedProgress

            // 1. Tick Marks (Scale Lines)
            val totalTicks = 51
            val tickRadiusOuter = radius + strokeWidth / 2f + 8.dp.toPx()
            for (i in 0 until totalTicks) {
                val tickProgress = i.toFloat() / (totalTicks - 1)
                val tickAngleDeg = startAngle + maxSweepAngle * tickProgress
                val tickAngleRad = Math.toRadians(tickAngleDeg.toDouble())

                val isMajor = i % 10 == 0
                val tickLength = if (isMajor) 12.dp.toPx() else 6.dp.toPx()
                val tickThick = if (isMajor) 2.5.dp.toPx() else 1.2.dp.toPx()

                val isTickActive = tickProgress <= animatedProgress && isRunning

                val outerX = center.x + tickRadiusOuter * cos(tickAngleRad).toFloat()
                val outerY = center.y + tickRadiusOuter * sin(tickAngleRad).toFloat()
                val innerX = center.x + (tickRadiusOuter - tickLength) * cos(tickAngleRad).toFloat()
                val innerY = center.y + (tickRadiusOuter - tickLength) * sin(tickAngleRad).toFloat()

                val tickColor = if (isTickActive) {
                    if (tickProgress > 0.7f) hotPinkColor else if (tickProgress > 0.35f) activeGlowColor else primaryColor
                } else {
                    trackBgColor
                }

                drawLine(
                    color = tickColor,
                    start = Offset(innerX, innerY),
                    end = Offset(outerX, outerY),
                    strokeWidth = tickThick,
                    cap = StrokeCap.Round
                )
            }

            // 2. Background Track Arc
            drawArc(
                color = trackBgColor,
                startAngle = startAngle,
                sweepAngle = maxSweepAngle,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // 3. Active Multi-Color Arc (Sweep Gradient)
            if (currentSweepAngle > 0.5f) {
                // Glow Underlayer Stroke
                drawArc(
                    brush = Brush.sweepGradient(
                        0.00f to primaryColor.copy(alpha = if (isRunning) auraAlpha else 0.3f),
                        0.50f to activeGlowColor.copy(alpha = if (isRunning) auraAlpha else 0.3f),
                        0.85f to hotPinkColor.copy(alpha = if (isRunning) auraAlpha else 0.3f),
                        center = center
                    ),
                    startAngle = startAngle,
                    sweepAngle = currentSweepAngle,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth + 8.dp.toPx(), cap = StrokeCap.Round)
                )

                // Main Bright Stroke
                drawArc(
                    brush = Brush.sweepGradient(
                        0.00f to primaryColor,
                        0.45f to activeGlowColor,
                        0.85f to hotPinkColor,
                        center = center
                    ),
                    startAngle = startAngle,
                    sweepAngle = currentSweepAngle,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // 4. Glowing Indicator Dot (Needle Head) at Arc Tip
                val tipAngleDeg = startAngle + currentSweepAngle
                val tipAngleRad = Math.toRadians(tipAngleDeg.toDouble())
                val tipX = center.x + radius * cos(tipAngleRad).toFloat()
                val tipY = center.y + radius * sin(tipAngleRad).toFloat()

                val tipColor = if (animatedProgress > 0.7f) hotPinkColor else if (animatedProgress > 0.35f) activeGlowColor else primaryColor

                // Outer Glowing Aura Circle
                drawCircle(
                    color = tipColor.copy(alpha = if (isRunning) auraAlpha else 0.4f),
                    radius = 16.dp.toPx(),
                    center = Offset(tipX, tipY)
                )

                // Middle Circle
                drawCircle(
                    color = tipColor,
                    radius = 9.dp.toPx(),
                    center = Offset(tipX, tipY)
                )

                // Inner Bright White Core
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = Offset(tipX, tipY)
                )
            }
        }

        // Center Speed Display & Status Badge
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = String.format(Locale.US, "%.1f", speed),
                fontSize = 54.sp,
                fontFamily = TechMonospace,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Mbps",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(10.dp))

            // Status Pill Badge
            Surface(
                color = if (isRunning) activeGlowColor.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    if (isRunning) activeGlowColor.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isRunning) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(activeGlowColor)
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = status,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isRunning) activeGlowColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.5.sp
                    )
                }
            }
        }
    }
}

@Composable
fun DualSpeedChart(
    downloadHistory: List<Float>,
    uploadHistory: List<Float>,
    isRunning: Boolean,
    modifier: Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ChartPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val downColor = MaterialTheme.colorScheme.primary
    val upColor = MaterialTheme.colorScheme.secondary
    val gridLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        val gridLines = 3
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx()), 0f)
        for (i in 1..gridLines) {
            val y = height * (i.toFloat() / (gridLines + 1))
            drawLine(
                color = gridLineColor,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = dashEffect
            )
        }

        if (!isRunning && downloadHistory.isEmpty() && uploadHistory.isEmpty()) return@Canvas

        val maxVal = maxOf((downloadHistory + uploadHistory).maxOrNull() ?: 10f, 100f)

        fun buildOffsets(history: List<Float>): List<Offset> {
            if (history.isEmpty()) return emptyList()
            val stepX = if (history.size > 1) width / (history.size - 1) else width
            return history.mapIndexed { i, v ->
                val x = i * stepX
                val y = height - ((v / maxVal) * height).coerceIn(0f, height)
                Offset(x, y)
            }
        }

        fun drawSmoothSpeedCurve(offsets: List<Offset>, color: Color, isDashed: Boolean = false) {
            if (offsets.size < 2) return

            val path = Path()
            path.moveTo(offsets[0].x, offsets[0].y)

            for (i in 0 until offsets.size - 1) {
                val p0 = offsets[i]
                val p1 = offsets[i + 1]
                val controlX1 = p0.x + (p1.x - p0.x) / 2f
                val controlY1 = p0.y
                val controlX2 = p0.x + (p1.x - p0.x) / 2f
                val controlY2 = p1.y
                path.cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
            }

            val fillPath = Path().apply {
                addPath(path)
                lineTo(offsets.last().x, height)
                lineTo(offsets.first().x, height)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(color.copy(alpha = 0.30f), Color.Transparent),
                    startY = 0f,
                    endY = height
                )
            )

            drawPath(
                path = path,
                color = color.copy(alpha = 0.25f),
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
            )

            drawPath(
                path = path,
                color = color,
                style = Stroke(
                    width = 2.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = if (isDashed) PathEffect.dashPathEffect(floatArrayOf(12.dp.toPx(), 8.dp.toPx()), 0f) else null
                )
            )

            val lastPt = offsets.last()
            if (isRunning) {
                drawCircle(
                    color = color.copy(alpha = 0.3f),
                    radius = 8.dp.toPx() * pulseScale,
                    center = lastPt
                )
            }
            drawCircle(
                color = Color.White,
                radius = 3.5.dp.toPx(),
                center = lastPt
            )
            drawCircle(
                color = color,
                radius = 2.dp.toPx(),
                center = lastPt
            )
        }

        if (downloadHistory.size >= 2) {
            drawSmoothSpeedCurve(buildOffsets(downloadHistory), downColor, isDashed = false)
        }

        if (uploadHistory.size >= 2) {
            drawSmoothSpeedCurve(buildOffsets(uploadHistory), upColor, isDashed = true)
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ServerSelectionDialog(
    currentServer: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    var customServer by remember { mutableStateOf("") }
    val presetServers = listOf(
        Pair("Cloudflare Global CDN", "speed.cloudflare.com"),
        Pair("Google Edge CDN", "speedtest.tele2.net"),
        Pair("Singapore APAC Node", "sg-speedtest.fast.com")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Select SpeedTest Server", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                presetServers.forEach { (name, server) ->
                    val isSelected = currentServer == server
                    Surface(
                        onClick = { onSelect(server) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text(server, fontSize = 11.sp, fontFamily = TechMonospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (isSelected) {
                                Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                Text("Custom SpeedTest Server", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                OutlinedTextField(
                    value = customServer,
                    onValueChange = { customServer = it },
                    placeholder = { Text("e.g. speed.cloudflare.com", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (customServer.isNotBlank()) onSelect(customServer.trim()) },
                enabled = customServer.isNotBlank()
            ) {
                Text("APPLY CUSTOM")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
