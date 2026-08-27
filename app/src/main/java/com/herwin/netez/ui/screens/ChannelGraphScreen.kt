package com.herwin.netez.ui.screens

import android.net.wifi.ScanResult
import android.os.Build
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.R
import com.herwin.netez.ui.theme.PremiumNavy
import com.herwin.netez.ui.theme.SoftDarkBg
import com.herwin.netez.ui.theme.TextGray
import com.herwin.netez.ui.viewmodels.ChannelGraphViewModel
import com.herwin.netez.ui.viewmodels.WifiBand
import com.herwin.netez.utils.NetworkUtils
import kotlin.math.abs

@Composable
fun ChannelGraphScreen(viewModel: ChannelGraphViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PremiumNavy)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        // 1. Band Selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BandToggleChip("2.4 GHz", uiState.selectedBand == WifiBand.BAND_2_4GHZ) { viewModel.updateBand(WifiBand.BAND_2_4GHZ) }
            BandToggleChip("5 GHz", uiState.selectedBand == WifiBand.BAND_5GHZ) { viewModel.updateBand(WifiBand.BAND_5GHZ) }
            BandToggleChip("6 GHz", uiState.selectedBand == WifiBand.BAND_6GHZ) { viewModel.updateBand(WifiBand.BAND_6GHZ) }
            
            Spacer(Modifier.weight(1f))
            
            IconButton(onClick = { viewModel.refresh() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
            }
        }

        // 2. Summary Info
        val crowdedChannel = uiState.scanResults.groupBy { NetworkUtils.getChannel(it.frequency) }
            .maxByOrNull { it.value.size }
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.05f)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = buildString {
                        append("Crowded: Ch ${crowdedChannel?.key ?: "-"} (${crowdedChannel?.value?.size ?: 0} APs)")
                        append(" • ")
                        append("Recommended: Ch ${uiState.recommendation?.bestChannel ?: "-"}")
                    },
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 3. Main Graph
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            shape = RoundedCornerShape(24.dp),
            color = SoftDarkBg,
            tonalElevation = 2.dp
        ) {
            WiFiChannelCanvas(
                results = uiState.scanResults,
                band = uiState.selectedBand,
                mySsid = uiState.mySsid,
                bestChannel = uiState.recommendation?.bestChannel ?: -1
            )
        }

        // 4. Access Points List
        Text(
            text = "ACCESS POINTS",
            style = MaterialTheme.typography.labelSmall,
            color = TextGray,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(uiState.scanResults, key = { it.BSSID }) { result ->
                @Suppress("DEPRECATION")
                val ssid = result.SSID
                ApItemCard(result, ssid == uiState.mySsid || ssid == "ZTE_5G")
            }
        }
    }
}

@Composable
fun BandToggleChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) Color(0xFF6366F1) else Color.White.copy(alpha = 0.05f),
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else TextGray,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun WiFiChannelCanvas(
    results: List<ScanResult>, 
    band: WifiBand, 
    mySsid: String,
    bestChannel: Int
) {
    val gridColor = Color(0xFF2A2F45)
    val myWifiColor = Color(0xFF3B82F6)
    
    val channelRange = when (band) {
        WifiBand.BAND_2_4GHZ -> 1..13
        WifiBand.BAND_5GHZ -> 36..165
        WifiBand.BAND_6GHZ -> 1..233
    }

    Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 36.dp, vertical = 32.dp)) {
        val width = size.width
        val height = size.height
        
        // Y-axis Grid (-30 to -90 dBm)
        val yLevels = listOf(-30, -50, -70, -90)
        yLevels.forEach { level ->
            val mappedY = ((level + 100).toFloat() / 75f) * height
            val finalY = height - mappedY
            
            // Dashed Grid Line
            drawLine(
                color = gridColor,
                start = Offset(0f, finalY),
                end = Offset(width, finalY),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
            
            drawContext.canvas.nativeCanvas.drawText(
                "$level", 
                -12.dp.toPx(), 
                finalY + 4.dp.toPx(), 
                android.graphics.Paint().apply { 
                    color = android.graphics.Color.GRAY
                    textSize = 9.sp.toPx()
                    textAlign = android.graphics.Paint.Align.RIGHT
                }
            )
        }

        val minChan = channelRange.first.toFloat()
        val maxChan = channelRange.last.toFloat()
        val chanSpan = maxChan - minChan

        // BEST Badge
        if (bestChannel != -1) {
            val bestX = (bestChannel.toFloat() - minChan) / chanSpan * width
            drawCircle(
                color = Color(0xFF10B981).copy(alpha = 0.2f),
                radius = 20.dp.toPx(),
                center = Offset(bestX, 0f)
            )
            drawContext.canvas.nativeCanvas.drawText(
                "BEST",
                bestX,
                -10.dp.toPx(),
                android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#10B981")
                    textSize = 8.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
            )
        }

        results.forEachIndexed { index, ap ->
            val channel = NetworkUtils.getChannel(ap.frequency).toFloat()
            if (channel < channelRange.first || channel > channelRange.last) return@forEachIndexed
            
            val x = (channel - minChan) / chanSpan * width
            val signalY = height - (((ap.level + 100).toFloat() / 75f) * height).coerceIn(0f, height)
            
            // Width based on 20/40/80/160
            val widthMhz = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                when(ap.channelWidth) {
                    ScanResult.CHANNEL_WIDTH_40MHZ -> 40f
                    ScanResult.CHANNEL_WIDTH_80MHZ -> 80f
                    ScanResult.CHANNEL_WIDTH_160MHZ -> 160f
                    else -> 20f
                }
            } else 20f
            
            val visualWidth = (widthMhz / 5f) * (width / chanSpan) 

            val path = Path()
            @Suppress("DEPRECATION")
            val ssid = ap.SSID
            val isTarget = ssid == mySsid || ssid == "ZTE_5G"
            val color = if (isTarget) myWifiColor else getColorForIndex(index)
            
            path.moveTo(x - visualWidth / 2, height)
            path.quadraticTo(x, signalY, x + visualWidth / 2, height)
            
            // Gradient Fill
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(color.copy(alpha = if (isTarget) 0.4f else 0.15f), Color.Transparent),
                    startY = signalY,
                    endY = height
                )
            )

            // Outline
            drawPath(
                path = path,
                color = if (isTarget) Color.White else color.copy(alpha = 0.8f),
                style = Stroke(width = if (isTarget) 3.dp.toPx() else 1.dp.toPx())
            )

            if (isTarget) {
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }
        
        // X-axis Channels
        val labelStep = if (band == WifiBand.BAND_2_4GHZ) 1 else 20
        for (i in channelRange step labelStep) {
            val x = (i.toFloat() - minChan) / chanSpan * width
            drawContext.canvas.nativeCanvas.drawText(
                "$i",
                x,
                height + 18.dp.toPx(),
                android.graphics.Paint().apply { 
                    color = android.graphics.Color.GRAY
                    textSize = 10.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER 
                }
            )
        }
    }
}

@Composable
fun ApItemCard(ap: ScanResult, isHighlighted: Boolean) {
    val color = if (isHighlighted) Color(0xFF3B82F6) else getColorForIndex(ap.BSSID.hashCode())
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF1E2235),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                @Suppress("DEPRECATION")
                Text(
                    text = if (ap.SSID.isEmpty()) "[Hidden]" else ap.SSID,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isHighlighted) Color(0xFF3B82F6) else Color.White,
                    maxLines = 1
                )
                Text(
                    text = "BSSID: ${ap.BSSID.uppercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextGray
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Ch ${NetworkUtils.getChannel(ap.frequency)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "${ap.level} dBm",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (ap.level > -60) Color(0xFF10B981) else TextGray
                )
            }
        }
    }
}

fun getColorForIndex(index: Int): Color {
    val colors = listOf(
        Color(0xFF3B82F6), // Blue
        Color(0xFFA855F7), // Purple
        Color(0xFF06B6D4), // Cyan
        Color(0xFFF97316), // Orange
        Color(0xFF10B981), // Green
        Color(0xFFEC4899)  // Pink
    )
    return colors[abs(index) % colors.size]
}
