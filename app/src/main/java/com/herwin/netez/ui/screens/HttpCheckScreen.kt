package com.herwin.netez.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.herwin.netez.ui.theme.TextGray
import com.herwin.netez.ui.viewmodels.HttpCheckViewModel

@Composable
fun HttpCheckScreen(viewModel: HttpCheckViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        // Search Input
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.url,
                        onValueChange = { viewModel.updateUrl(it) },
                        placeholder = { Text("google.com", color = TextGray) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )
                    Button(
                        onClick = { viewModel.runCheck() },
                        modifier = Modifier.height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Text("CHECK", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }

        // Result Premium Card
        uiState.result?.let { res ->
            item {
                PremiumResultCard(res)
            }
        }

        // History Chart (Response Time)
        if (uiState.history.isNotEmpty()) {
            item {
                Text("Response History (Last 7)", style = MaterialTheme.typography.labelLarge, color = TextGray)
                Spacer(Modifier.height(8.dp))
                ResponseHistoryChart(uiState.history)
            }
        }

        // Recent Checks List
        item {
            Text("Quick Suggestions", style = MaterialTheme.typography.labelLarge, color = TextGray)
            Spacer(Modifier.height(8.dp))
            FlowRowComponent(
                modifier = Modifier.fillMaxWidth()
            ) {
                uiState.recentHosts.forEach { host ->
                    SuggestionChip(
                        onClick = { viewModel.updateUrl(host); viewModel.runCheck() },
                        label = { Text(host) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumResultCard(res: com.herwin.netez.network.HttpCheckResult) {
    val statusColor = if (res.isAvailable) Color(0xFF10B981) else Color(0xFFEF4444)
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Column {
            // Top: Status Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(statusColor.copy(alpha = 0.1f))
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (res.isAvailable) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (res.isAvailable) "ONLINE" else "OFFLINE",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor,
                        letterSpacing = 2.sp
                    )
                }
            }

            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                // Center: Host with Favicon
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = "https://www.google.com/s2/favicons?sz=64&domain=${res.url}",
                        contentDescription = null,
                        modifier = Modifier.size(32.dp).clip(CircleShape)
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = res.url,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Bottom: Grid Metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MetricItem("Status", if (res.statusCode == -1) "ERROR" else res.statusCode.toString(), statusColor)
                    Box(modifier = Modifier.width(1.dp).height(40.dp).background(TextGray.copy(alpha = 0.2f)))
                    MetricItem("Response", "${res.responseTime} ms", MaterialTheme.colorScheme.primary)
                    Box(modifier = Modifier.width(1.dp).height(40.dp).background(TextGray.copy(alpha = 0.2f)))
                    MetricItem("Server IP", res.ipAddress.ifEmpty { "Unknown" }, TextGray)
                }
            }

            // Progress bar indicator for response time
            val progress = (res.responseTime.toFloat() / 1000f).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = statusColor,
                trackColor = Color.Transparent
            )
        }
    }
}

@Composable
fun MetricItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextGray)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.ExtraBold, color = color)
    }
}

@Composable
fun ResponseHistoryChart(history: List<com.herwin.netez.network.HttpCheckResult>) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(100.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            val maxTime = (history.maxOfOrNull { it.responseTime } ?: 100).coerceAtLeast(1)
            history.reversed().forEach { res ->
                val heightFactor = (res.responseTime.toFloat() / maxTime.toFloat()).coerceIn(0.1f, 1f)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(heightFactor)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(if (res.isAvailable) Color(0xFF10B981) else Color(0xFFEF4444))
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRowComponent(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = { content() }
    )
}
