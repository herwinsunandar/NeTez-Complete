package com.herwin.netez.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import android.net.wifi.ScanResult
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.theme.PremiumNavy
import com.herwin.netez.ui.theme.SoftDarkBg
import com.herwin.netez.ui.theme.TextGray
import com.herwin.netez.ui.viewmodels.ChannelGraphViewModel
import com.herwin.netez.ui.viewmodels.WifiBand
import com.herwin.netez.ui.viewmodels.Recommendation
import com.herwin.netez.utils.NetworkUtils
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun ChannelGraphScreen(viewModel: ChannelGraphViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    var selectedBssid by remember { mutableStateOf<String?>(null) }
    
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx() }

    // Auto-scroll logic to focus on crowded channel
    LaunchedEffect(uiState.selectedBand, uiState.scanResults, scrollState.maxValue) {
        if (uiState.selectedBand != WifiBand.BAND_2_4GHZ && uiState.scanResults.isNotEmpty() && scrollState.maxValue > 0) {
            val targetChannel = uiState.recommendation?.crowdedChannel ?: uiState.recommendation?.bestChannel ?: -1
            if (targetChannel != -1) {
                val channels = uiState.scanResults.map { NetworkUtils.getChannel(it.frequency) }
                val minChanFound = channels.minOrNull() ?: 1
                val maxChanFound = channels.maxOrNull() ?: 1
                
                val start = (minChanFound - 8).coerceAtLeast(if (uiState.selectedBand == WifiBand.BAND_5GHZ) 36 else 1)
                val end = (maxChanFound + 8).coerceAtMost(if (uiState.selectedBand == WifiBand.BAND_5GHZ) 165 else 233)
                
                val span = (end - start).toFloat()
                if (span > 0) {
                    val progress = (targetChannel - start) / span
                    val graphWidthPx = with(density) { 1200.dp.toPx() }
                    val targetX = graphWidthPx * progress
                    val scrollPos = (targetX - screenWidthPx / 2).coerceIn(0f, scrollState.maxValue.toFloat())
                    scrollState.animateScrollTo(scrollPos.toInt())
                }
            }
        }
    }

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
                Icon(AppIcons.refresh(), contentDescription = "Refresh", tint = Color.White)
            }
        }

        // 2. Summary Info Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val rec = uiState.recommendation
            if (rec?.crowdedChannel != null) {
                StatusChip(
                    icon = AppIcons.warning(),
                    text = "Crowded Ch ${rec.crowdedChannel} (${rec.crowdedApCount} APs)",
                    containerColor = Color(0xFFEF4444).copy(alpha = 0.15f),
                    contentColor = Color(0xFFEF4444),
                    modifier = Modifier.weight(1f)
                )
            }
            if (rec != null) {
                StatusChip(
                    icon = AppIcons.checkCircle(),
                    text = "Rec: Ch ${rec.bestChannel}",
                    containerColor = Color(0xFF22C55E).copy(alpha = 0.15f),
                    contentColor = Color(0xFF22C55E),
                    actionText = "Switch",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3. Main Graph
        Box(modifier = Modifier.fillMaxWidth()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                shape = RoundedCornerShape(24.dp),
                color = SoftDarkBg,
                tonalElevation = 2.dp
            ) {
                Box(modifier = Modifier.horizontalScroll(scrollState)) {
                    WiFiChannelCanvas(
                        results = uiState.scanResults,
                        band = uiState.selectedBand,
                        mySsid = uiState.mySsid,
                        recommendation = uiState.recommendation,
                        modifier = Modifier.width(if (uiState.selectedBand == WifiBand.BAND_2_4GHZ) 400.dp else 1200.dp),
                        onApClick = { bssid ->
                            selectedBssid = bssid
                            val index = uiState.scanResults.indexOfFirst { it.BSSID == bssid }
                            if (index != -1) {
                                coroutineScope.launch {
                                    listState.animateScrollToItem(index)
                                }
                            }
                        }
                    )
                }
            }
            
            // Swipe indicator for 5/6GHz
            if (uiState.selectedBand != WifiBand.BAND_2_4GHZ) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                        .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(AppIcons.arrowBack(), null, tint = TextGray, modifier = Modifier.size(10.dp))
                    Text(" Swipe to see more channels ", color = TextGray, fontSize = 9.sp)
                    Icon(AppIcons.arrowForward(), null, tint = TextGray, modifier = Modifier.size(10.dp))
                }
            }
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
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(uiState.scanResults, key = { it.BSSID }) { result ->
                @Suppress("DEPRECATION")
                val ssid = result.SSID
                ApItemCard(
                    ap = result, 
                    isHighlighted = ssid == uiState.mySsid || ssid == "ZTE_5G" || result.BSSID == selectedBssid
                )
            }
        }
    }
}

@Composable
fun StatusChip(
    icon: Painter,
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    actionText: String? = null
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = contentColor, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                text = text, 
                color = Color.White, 
                fontSize = 9.sp, 
                fontWeight = FontWeight.Bold, 
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (actionText != null) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = actionText, 
                    color = contentColor, 
                    fontSize = 9.sp, 
                    fontWeight = FontWeight.Black, 
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { /* Action */ }
                )
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
    recommendation: Recommendation?,
    modifier: Modifier = Modifier,
    onApClick: (String) -> Unit
) {
    val gridColor = Color(0xFF2A2E45)
    
    // Dynamic X Range Logic
    val channels = results.map { NetworkUtils.getChannel(it.frequency) }
    val minChanFound = channels.minOrNull() ?: when(band) {
        WifiBand.BAND_2_4GHZ -> 1
        WifiBand.BAND_5GHZ -> 149
        WifiBand.BAND_6GHZ -> 1
    }
    val maxChanFound = channels.maxOrNull() ?: when(band) {
        WifiBand.BAND_2_4GHZ -> 13
        WifiBand.BAND_5GHZ -> 161
        WifiBand.BAND_6GHZ -> 97
    }

    val channelRange = when (band) {
        WifiBand.BAND_2_4GHZ -> 1..13
        WifiBand.BAND_5GHZ -> (minChanFound - 8).coerceAtLeast(36)..(maxChanFound + 8).coerceAtMost(165)
        WifiBand.BAND_6GHZ -> (minChanFound - 8).coerceAtLeast(1)..(maxChanFound + 8).coerceAtMost(233)
    }

    val minLevel = (results.minOfOrNull { it.level } ?: -90).toFloat()
    val maxLevel = (results.maxOfOrNull { it.level } ?: -40).toFloat()
    
    val yMax = (maxLevel + 10f).coerceAtMost(-20f)
    val yMin = (minLevel - 10f).coerceAtLeast(-100f)
    val yRange = yMax - yMin

    Canvas(modifier = modifier
        .fillMaxHeight()
        .padding(start = 48.dp, end = 24.dp, top = 32.dp, bottom = 40.dp)
        .pointerInput(results, yMin, yMax) {
            detectTapGestures { offset ->
                val width = size.width.toFloat()
                val height = size.height.toFloat()
                val minChan = channelRange.first.toFloat()
                val maxChan = channelRange.last.toFloat()
                val chanSpan = maxChan - minChan
                
                results.forEach { ap ->
                    val chan = NetworkUtils.getChannel(ap.frequency).toFloat()
                    val x = (chan - minChan) / chanSpan * width
                    val sy = height - ((ap.level.toFloat() - yMin) / yRange * height).coerceIn(0f, height)
                    
                    if (abs(offset.x - x) < 30.dp.toPx() && abs(offset.y - sy) < 30.dp.toPx()) {
                        onApClick(ap.BSSID)
                        return@detectTapGestures
                    }
                }
            }
        }
    ) {
        val width = size.width
        val height = size.height
        
        val minChan = channelRange.first.toFloat()
        val maxChan = channelRange.last.toFloat()
        val chanSpan = maxChan - minChan

        // 1. Draw Connected Channel Highlight (Vertical #1E2235)
        results.find { 
            @Suppress("DEPRECATION")
            it.SSID == mySsid 
        }?.let { conn ->
            val connChan = NetworkUtils.getChannel(conn.frequency).toFloat()
            if (connChan in minChan..maxChan) {
                val connX = (connChan - minChan) / chanSpan * width
                val chanWidth = width / chanSpan
                drawRect(
                    color = Color(0xFF1E2235),
                    topLeft = Offset(connX - chanWidth * 0.25f, 0f),
                    size = Size(chanWidth * 0.5f, height)
                )
            }
        }

        // 2. Best & Crowded Zones
        recommendation?.let { rec ->
            val chanWidth = width / chanSpan
            // Best Zone (Green 15% alpha)
            if (rec.bestChannel.toFloat() in minChan..maxChan) {
                val bestX = (rec.bestChannel.toFloat() - minChan) / chanSpan * width
                drawRect(
                    color = Color(0xFF22C55E).copy(alpha = 0.15f),
                    topLeft = Offset(bestX - chanWidth * 0.5f, 0f),
                    size = Size(chanWidth, height)
                )
            }
            
            // Crowded Zone (Red 15% alpha)
            rec.crowdedChannel?.let { crd ->
                if (crd.toFloat() in minChan..maxChan) {
                    val crdX = (crd.toFloat() - minChan) / chanSpan * width
                    drawRect(
                        color = Color(0xFFEF4444).copy(alpha = 0.15f),
                        topLeft = Offset(crdX - chanWidth * 0.5f, 0f),
                        size = Size(chanWidth, height)
                    )
                }
            }
        }

        // 3. Grid Lines & Y Labels
        val step = 10f
        var currentYVal = (Math.ceil(yMin.toDouble() / step) * step).toFloat()
        while (currentYVal <= yMax) {
            val y = height - ((currentYVal - yMin) / yRange * height)
            if (y in 0f..height) {
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
                drawContext.canvas.nativeCanvas.drawText(
                    "${currentYVal.toInt()}", 
                    -12.dp.toPx(), 
                    y + 4.dp.toPx(), 
                    Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 9.sp.toPx()
                        textAlign = Paint.Align.RIGHT
                    }
                )
            }
            currentYVal += step
        }

        // 4. Draw AP Curves (Bell curve, width=4 channels)
        results.forEachIndexed { index, ap ->
            val channel = NetworkUtils.getChannel(ap.frequency).toFloat()
            if (channel < minChan || channel > maxChan) return@forEachIndexed
            
            val x = (channel - minChan) / chanSpan * width
            val signalY = height - ((ap.level.toFloat() - yMin) / yRange * height).coerceIn(0f, height)
            
            // 4 channels width for 20MHz
            val curveWidth = 4f * (width / chanSpan) 

            val path = Path()
            @Suppress("DEPRECATION")
            val ssid = ap.SSID
            val isTarget = ssid == mySsid || ssid == "ZTE_5G"
            val baseColor = when {
                ssid == "Daffa" -> Color(0xFFFF6B9D)
                ssid == "Apotek" -> Color(0xFF22D3EE)
                isTarget -> Color(0xFF6C8CFF)
                else -> getColorForIndex(index)
            }
            
            path.moveTo(x - curveWidth / 2, height)
            path.quadraticTo(x, signalY, x + curveWidth / 2, height)
            
            // Gradient Fill 40% alpha
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(baseColor.copy(alpha = 0.4f), Color.Transparent),
                    startY = signalY,
                    endY = height
                )
            )

            // Stroke 2dp
            drawPath(
                path = path,
                color = baseColor,
                style = Stroke(width = 2.dp.toPx())
            )

            // Label on peak
            val labelText = if (ssid.isEmpty()) "[Hidden]" else ssid
            val labelPaint = Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 8.sp.toPx()
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            val textWidth = labelPaint.measureText(labelText)
            
            drawRoundRect(
                color = baseColor.copy(alpha = 0.6f),
                topLeft = Offset(x - textWidth / 2 - 4.dp.toPx(), signalY - 14.dp.toPx()),
                size = Size(textWidth + 8.dp.toPx(), 12.dp.toPx()),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
            drawContext.canvas.nativeCanvas.drawText(
                labelText,
                x,
                signalY - 5.dp.toPx(),
                labelPaint
            )
        }
        
        // 5. X-axis Labels (Fix overlapping for 5/6GHz)
        val totalChannelsInRange = channelRange.last - channelRange.first
        val labelStep = if (band == WifiBand.BAND_2_4GHZ) 1 else if (totalChannelsInRange > 30) 8 else 4
        
        for (i in channelRange step labelStep) {
            val x = (i.toFloat() - minChan) / chanSpan * width
            drawContext.canvas.nativeCanvas.drawText(
                "$i",
                x,
                height + 22.dp.toPx(),
                Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 9.sp.toPx()
                    textAlign = Paint.Align.CENTER
                }
            )
            drawLine(
                color = Color.White.copy(alpha = 0.3f),
                start = Offset(x, height),
                end = Offset(x, height + 4.dp.toPx()),
                strokeWidth = 1.dp.toPx()
            )
        }

        // BEST Pill Label
        recommendation?.let { rec ->
            if (rec.bestChannel.toFloat() in minChan..maxChan) {
                val bestX = (rec.bestChannel.toFloat() - minChan) / chanSpan * width
                drawRoundRect(
                    color = Color(0xFF22C55E),
                    topLeft = Offset(bestX - 16.dp.toPx(), -24.dp.toPx()),
                    size = Size(32.dp.toPx(), 14.dp.toPx()),
                    cornerRadius = CornerRadius(12.dp.toPx())
                )
                drawContext.canvas.nativeCanvas.drawText(
                    "BEST",
                    bestX,
                    -13.dp.toPx(),
                    Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 7.sp.toPx()
                        textAlign = Paint.Align.CENTER
                        typeface = Typeface.DEFAULT_BOLD
                    }
                )
            }
        }
    }
}

@Composable
fun ApItemCard(ap: ScanResult, isHighlighted: Boolean) {
    @Suppress("DEPRECATION")
    val ssid = ap.SSID
    val baseColor = when {
        ssid == "Daffa" -> Color(0xFFFF6B9D)
        ssid == "Apotek" -> Color(0xFF22D3EE)
        isHighlighted -> Color(0xFF3B82F6)
        else -> getColorForIndex(ap.BSSID.hashCode())
    }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = if (isHighlighted) Color(0xFF1E2235).copy(alpha = 0.8f) else Color(0xFF1E2235),
        tonalElevation = 1.dp,
        border = if (isHighlighted) BorderStroke(1.dp, baseColor.copy(alpha = 0.5f)) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(baseColor)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (ssid.isEmpty()) "[Hidden]" else ssid,
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
