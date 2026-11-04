package com.herwin.netez.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.network.PortResult
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.components.ResultFooter
import com.herwin.netez.ui.components.ResultRow
import com.herwin.netez.ui.theme.TechMonospace
import com.herwin.netez.ui.viewmodels.PortFilter
import com.herwin.netez.ui.viewmodels.PortRangeOption
import com.herwin.netez.ui.viewmodels.PortScannerViewModel
import java.util.Locale

import com.herwin.netez.entitlement.FeatureId
import com.herwin.netez.ui.components.ContextualPaywallDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortScannerScreen(viewModel: PortScannerViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var selectedPort by remember { mutableStateOf<PortResult?>(null) }
    val sheetState = rememberModalBottomSheetState()

    if (viewModel.showGlobalSupportDialog) {
        ContextualPaywallDialog(
            featureId = FeatureId.GLOBAL_TOOL_USAGE,
            limitText = "You've performed 3 network & security scans! If NeTez is helpful, consider watching a short video to support development or upgrade to PRO (Skippable).",
            onDismiss = { viewModel.showGlobalSupportDialog = false },
            onUpgradeClick = {}
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(4.dp))

        // 1. Target IP & Protocol Toggle Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // IP Input Field
                    OutlinedTextField(
                        value = uiState.ip,
                        onValueChange = { viewModel.updateIp(it) },
                        placeholder = {
                            Text(
                                text = "e.g. 192.168.1.1 or example.com",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontSize = 13.sp
                            )
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isScanning,
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = { viewModel.startScan() }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )

                    // Protocol Toggle Switch
                    Surface(
                        onClick = { if (!uiState.isScanning) viewModel.toggleProtocol() },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .height(56.dp)
                            .width(68.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = uiState.protocol,
                                color = MaterialTheme.colorScheme.primary,
                                fontFamily = TechMonospace,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // Range Options Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RangeChip("Common", uiState.rangeOption == PortRangeOption.COMMON) { viewModel.updateRange(PortRangeOption.COMMON) }
                    RangeChip("Top 100", uiState.rangeOption == PortRangeOption.TOP_100) { viewModel.updateRange(PortRangeOption.TOP_100) }
                    RangeChip("1-1024", uiState.rangeOption == PortRangeOption.SYSTEM) { viewModel.updateRange(PortRangeOption.SYSTEM) }
                    RangeChip("Custom", uiState.rangeOption == PortRangeOption.CUSTOM) { viewModel.updateRange(PortRangeOption.CUSTOM) }
                }

                if (uiState.rangeOption == PortRangeOption.CUSTOM) {
                    OutlinedTextField(
                        value = uiState.customRange,
                        onValueChange = { viewModel.updateRange(PortRangeOption.CUSTOM, it) },
                        label = { Text("Range (e.g. 80,443 or 1-100)", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                Button(
                    onClick = { viewModel.startScan() },
                    enabled = !uiState.isScanning,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (uiState.isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            painter = AppIcons.search(),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "START PORT SCAN",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        if (uiState.isScanning) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        }

        // Summary Bar
        if (uiState.results.isNotEmpty() || uiState.isScanning) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${uiState.openCount} open / ${uiState.results.size} ports",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = TechMonospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• ${String.format(Locale.US, "%.1fs", uiState.scanTimeMs / 1000f)} • ${uiState.deviceType}",
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChipItem("All", uiState.filter == PortFilter.ALL) { viewModel.updateFilter(PortFilter.ALL) }
            FilterChipItem("Open", uiState.filter == PortFilter.OPEN) { viewModel.updateFilter(PortFilter.OPEN) }
            FilterChipItem("Closed", uiState.filter == PortFilter.CLOSED) { viewModel.updateFilter(PortFilter.CLOSED) }
        }

        // Results List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
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
            containerColor = MaterialTheme.colorScheme.surface
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
        label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
        shape = RoundedCornerShape(10.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.primary
        ),
        border = null
    )
}

@Composable
fun RangeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(
            text = label,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PortResultCard(result: PortResult, onClick: () -> Unit) {
    val statusColor = if (result.isOpen) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (result.isOpen) statusColor.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (result.isOpen) statusColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = getServiceIcon(result.port),
                    contentDescription = null,
                    tint = if (result.isOpen) statusColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Port ${result.port}",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = TechMonospace,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (result.isOpen) "OPEN" else "CLOSED",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
                Text(
                    text = "${result.protocol} • ${result.service}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (result.banner != null) {
                    Text(
                        text = result.banner,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = TechMonospace,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
            }

            if (result.isOpen) {
                Text(
                    text = "${result.latencyMs} ms",
                    style = MaterialTheme.typography.labelLarge,
                    fontFamily = TechMonospace,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun PortDetailContent(port: PortResult, ip: String, context: Context) {
    Column(
        modifier = Modifier
            .padding(20.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Port ${port.port} Details",
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = TechMonospace,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(14.dp).fillMaxWidth()) {
                ResultRow("Service", port.service)
                ResultRow("Protocol", port.protocol)
                ResultRow("Latency", "${port.latencyMs} ms", isTechnicalData = true)
                if (port.banner != null) ResultRow("Banner", port.banner, isTechnicalData = true)
            }
        }

        if (port.port == 80 || port.port == 443 || port.port == 8080) {
            Button(
                onClick = {
                    val protocol = if (port.port == 443) "https" else "http"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("$protocol://$ip:${port.port}"))
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Open in Browser", fontWeight = FontWeight.Bold)
            }
        }

        ResultFooter(
            fullTextToCopy = "IP: $ip, Port: ${port.port}, Service: ${port.service}, Banner: ${port.banner ?: "None"}",
            onCopyAll = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Port Info", "IP: $ip, Port: ${port.port}, Service: ${port.service}"))
                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            }
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun getServiceIcon(port: Int): Painter {
    return when (port) {
        80, 443, 8080 -> AppIcons.language()
        22 -> AppIcons.terminal()
        53 -> AppIcons.dns()
        21, 445 -> AppIcons.folder()
        3306, 5432 -> AppIcons.storage()
        8291 -> AppIcons.router()
        else -> AppIcons.settingsInputComponent()
    }
}
