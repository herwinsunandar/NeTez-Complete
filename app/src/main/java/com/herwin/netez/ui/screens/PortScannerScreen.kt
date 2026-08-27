package com.herwin.netez.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.network.PortResult
import com.herwin.netez.ui.theme.PremiumNavy
import com.herwin.netez.ui.theme.TextGray
import com.herwin.netez.ui.viewmodels.PortFilter
import com.herwin.netez.ui.viewmodels.PortRangeOption
import com.herwin.netez.ui.viewmodels.PortScannerViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortScannerScreen(viewModel: PortScannerViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var selectedPort by remember { mutableStateOf<PortResult?>(null) }
    val sheetState = rememberModalBottomSheetState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PremiumNavy)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        // 1. Target IP & Protocol Toggle
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White.copy(alpha = 0.05f),
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.ip,
                        onValueChange = { viewModel.updateIp(it) },
                        placeholder = { Text("192.168.1.1", color = TextGray) },
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isScanning,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedTextColor = Color.White,
                            focusedTextColor = Color.White,
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                            focusedBorderColor = Color(0xFF3B82F6)
                        ),
                        singleLine = true
                    )
                    
                    Surface(
                        onClick = { if (!uiState.isScanning) viewModel.toggleProtocol() },
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF3B82F6).copy(alpha = 0.1f),
                        modifier = Modifier.height(56.dp).width(70.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(uiState.protocol, color = Color(0xFF3B82F6), fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RangeChip("Common", uiState.rangeOption == PortRangeOption.COMMON) { viewModel.updateRange(PortRangeOption.COMMON) }
                    RangeChip("Top 100", uiState.rangeOption == PortRangeOption.TOP_100) { viewModel.updateRange(PortRangeOption.TOP_100) }
                    RangeChip("1-1024", uiState.rangeOption == PortRangeOption.SYSTEM) { viewModel.updateRange(PortRangeOption.SYSTEM) }
                    RangeChip("Custom", uiState.rangeOption == PortRangeOption.CUSTOM) { viewModel.updateRange(PortRangeOption.CUSTOM) }
                }
                
                if (uiState.rangeOption == PortRangeOption.CUSTOM) {
                    OutlinedTextField(
                        value = uiState.customRange,
                        onValueChange = { viewModel.updateRange(PortRangeOption.CUSTOM, it) },
                        label = { Text("Range (e.g. 80,443 or 1-100)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Button(
                    onClick = { viewModel.startScan() },
                    enabled = !uiState.isScanning,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                ) {
                    if (uiState.isScanning) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Text("START SCAN", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        if (uiState.isScanning) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                color = Color(0xFF3B82F6),
                trackColor = Color.White.copy(alpha = 0.05f)
            )
        }

        if (uiState.results.isNotEmpty() || uiState.isScanning) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF10B981).copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${uiState.openCount} open / ${uiState.results.size} ports",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• ${String.format(Locale.US, "%.1fs", uiState.scanTimeMs / 1000f)} • ${uiState.deviceType}",
                        color = Color(0xFF10B981),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChipItem("All", uiState.filter == PortFilter.ALL) { viewModel.updateFilter(PortFilter.ALL) }
            FilterChipItem("Open", uiState.filter == PortFilter.OPEN) { viewModel.updateFilter(PortFilter.OPEN) }
            FilterChipItem("Closed", uiState.filter == PortFilter.CLOSED) { viewModel.updateFilter(PortFilter.CLOSED) }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(uiState.filteredResults) { result ->
                PortResultCard(result) { selectedPort = result }
            }
        }
    }

    if (selectedPort != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedPort = null },
            sheetState = sheetState,
            containerColor = Color(0xFF1E293B)
        ) {
            PortDetailContent(selectedPort!!, uiState.ip, context)
        }
    }
}

@Composable
fun FilterChipItem(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 12.sp) },
        shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFF3B82F6),
            selectedLabelColor = Color.White,
            containerColor = Color.White.copy(alpha = 0.05f),
            labelColor = TextGray
        ),
        border = null
    )
}

@Composable
fun RangeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) Color(0xFF3B82F6) else Color.White.copy(alpha = 0.05f),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            label, 
            color = if (selected) Color.White else TextGray,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PortResultCard(result: PortResult, onClick: () -> Unit) {
    val statusColor = if (result.isOpen) Color(0xFF10B981) else Color(0xFFEF4444)
    
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.04f),
        border = if (result.isOpen) androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.2f)) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (result.isOpen) statusColor.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getServiceIcon(result.port),
                    contentDescription = null,
                    tint = if (result.isOpen) statusColor else TextGray,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Port ${result.port}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text(if (result.isOpen) "OPEN" else "CLOSED", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = statusColor)
                }
                Text("${result.protocol} • ${result.service}", style = MaterialTheme.typography.bodySmall, color = TextGray)
                if (result.banner != null) {
                    Text(result.banner, style = MaterialTheme.typography.labelSmall, color = Color(0xFF3B82F6), maxLines = 1)
                }
            }

            if (result.isOpen) {
                Text("${result.latencyMs}ms", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, color = Color(0xFF3B82F6))
            }
        }
    }
}

@Composable
fun PortDetailContent(port: PortResult, ip: String, context: Context) {
    Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Port ${port.port} Details", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
        
        Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                com.herwin.netez.ui.components.ResultRow("Service", port.service)
                com.herwin.netez.ui.components.ResultRow("Protocol", port.protocol)
                com.herwin.netez.ui.components.ResultRow("Latency", "${port.latencyMs} ms")
                if (port.banner != null) com.herwin.netez.ui.components.ResultRow("Banner", port.banner)
            }
        }

        if (port.port == 80 || port.port == 443 || port.port == 8080) {
            Button(
                onClick = {
                    val protocol = if (port.port == 443) "https" else "http"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("$protocol://$ip:${port.port}"))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Open Browser")
            }
        }

        com.herwin.netez.ui.components.ResultFooter(
            fullTextToCopy = "IP: $ip, Port: ${port.port}, Service: ${port.service}, Banner: ${port.banner ?: "None"}",
            onCopyAll = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Port Info", "IP: $ip, Port: ${port.port}, Service: ${port.service}"))
                android.widget.Toast.makeText(context, "Copied", android.widget.Toast.LENGTH_SHORT).show()
            }
        )
        Spacer(Modifier.height(32.dp))
    }
}

fun getServiceIcon(port: Int): androidx.compose.ui.graphics.vector.ImageVector {
    return when (port) {
        80, 443, 8080 -> Icons.Default.Language
        22 -> Icons.Default.Terminal
        53 -> Icons.Default.Dns
        21, 445 -> Icons.Default.Folder
        3306, 5432 -> Icons.Default.Storage
        8291 -> Icons.Default.Router
        else -> Icons.Default.SettingsInputComponent
    }
}

