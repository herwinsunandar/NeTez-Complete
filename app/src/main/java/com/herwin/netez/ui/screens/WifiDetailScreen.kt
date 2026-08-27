package com.herwin.netez.ui.screens

import android.app.Application
import android.net.wifi.ScanResult
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.ui.viewmodels.WifiDetailViewModel
import com.herwin.netez.utils.NetworkUtils
import java.util.Locale

@Composable
fun WifiDetailScreen(bssid: String, onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: WifiDetailViewModel = viewModel(
        factory = WifiDetailViewModelFactory(context.applicationContext as Application, bssid)
    )
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        uiState.scanResult?.let { result ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { IdentitySection(result) }
                item { RadioSection(result) }
                item { SecuritySection(result) }
                item { AdditionalSection(result) }
                item { SignalGraphSection(uiState.signalHistory) }
            }
        } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun IdentitySection(result: ScanResult) {
    DetailCategory("Identity") {
        @Suppress("DEPRECATION")
        DetailRow("SSID", if (result.SSID.isNullOrEmpty()) "[Hidden]" else result.SSID, Icons.Default.Wifi)
        DetailRow("BSSID", result.BSSID, Icons.Default.Fingerprint)
        DetailRow("Vendor", NetworkUtils.getVendor(result.BSSID), Icons.Default.Business)
        @Suppress("DEPRECATION")
        DetailRow("Hidden", if (result.SSID.isNullOrEmpty()) "Yes" else "No", Icons.Default.VisibilityOff)
    }
}

@Composable
fun RadioSection(result: ScanResult) {
    val quality = NetworkUtils.getSignalQuality(result.level)
    DetailCategory("Radio & Signal") {
        DetailRow("Strength", "${result.level} dBm ($quality)", Icons.Default.SignalWifiStatusbar4Bar)
        DetailRow("Frequency", "${result.frequency} MHz", Icons.Default.Waves)
        DetailRow("Channel", NetworkUtils.getChannel(result.frequency).toString(), Icons.Default.Toll)
        DetailRow("Standard", NetworkUtils.getWifiStandard(result), Icons.Default.SettingsInputAntenna)
        DetailRow("Band", NetworkUtils.getFrequencyBand(result.frequency), Icons.Default.Layers)
    }
}

@Composable
fun SecuritySection(result: ScanResult) {
    val isSecure = !result.capabilities.contains("OPEN")
    val icon = if (isSecure) Icons.Default.Lock else Icons.Default.LockOpen
    DetailCategory("Security") {
        DetailRow("Type", result.capabilities, icon)
        DetailRow("WPS", if (result.capabilities.contains("WPS")) "Enabled" else "Disabled", Icons.Default.Security)
    }
}

@Composable
fun AdditionalSection(result: ScanResult) {
    val distance = NetworkUtils.estimateDistance(result.level, result.frequency)
    DetailCategory("Additional Info") {
        DetailRow("Est. Distance", "~${String.format(Locale.US, "%.1f", distance)} meters", Icons.Default.Straighten)
        DetailRow("Timestamp", result.timestamp.toString(), Icons.Default.Schedule)
    }
}

@Composable
fun SignalGraphSection(history: List<Int>) {
    Text("Real-time Signal (30s)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        if (history.isNotEmpty()) {
            val primaryColor = MaterialTheme.colorScheme.primary
            Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                val width = size.width
                val height = size.height
                val maxPoints = 30
                val spacing = width / (maxPoints - 1)
                
                val path = Path()
                history.forEachIndexed { index, level ->
                    val normalizedLevel = (level + 100).toFloat().coerceIn(0f, 70f)
                    val y = height - (normalizedLevel / 70f * height)
                    val x = index * spacing
                    
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                
                drawPath(
                    path = path,
                    color = primaryColor,
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }
    }
}

@Composable
fun DetailCategory(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

class WifiDetailViewModelFactory(
    private val application: Application,
    private val bssid: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WifiDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WifiDetailViewModel(application, bssid) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
