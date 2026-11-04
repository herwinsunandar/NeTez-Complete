package com.herwin.netez.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.network.UpnpDevice
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.components.ResultFooter
import com.herwin.netez.ui.components.ResultRow
import com.herwin.netez.ui.theme.TechMonospace
import com.herwin.netez.ui.viewmodels.UpnpScannerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpnpScannerScreen(
    viewModel: UpnpScannerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.startScan()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Section Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
            tonalElevation = 2.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "UPnP Discovery",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Scan for UPnP enabled devices",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { viewModel.startScan() },
                    enabled = !uiState.isScanning,
                    modifier = Modifier.height(52.dp),
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
                        Text("SCAN", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Results
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
        ) {
            if (uiState.devices.isEmpty() && !uiState.isScanning) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No UPnP devices found",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(uiState.devices) { device ->
                UpnpDeviceCard(device = device)
            }

            if (uiState.devices.isNotEmpty() && !uiState.isScanning) {
                item {
                    val context = LocalContext.current
                    ResultFooter(
                        fullTextToCopy = uiState.devices.joinToString("\n") { "${it.friendlyName} (${it.ip})" },
                        onCopyAll = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("UPnP Devices", uiState.devices.joinToString("\n") { it.friendlyName }))
                            Toast.makeText(context, "Copied list", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun UpnpDeviceCard(device: UpnpDevice) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (expanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .clickable { expanded = !expanded }
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = AppIcons.router(),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.friendlyName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = device.ip,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = TechMonospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (device.modelName.isNotEmpty()) {
                        Text(
                            text = device.modelName,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = TechMonospace,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp
                        )
                    }
                }

                Icon(
                    painter = if (expanded) AppIcons.keyboardArrowUp() else AppIcons.keyboardArrowDown(),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(top = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    ResultRow("Manufacturer", device.manufacturer.ifEmpty { "Unknown" })
                    ResultRow("Location", device.location, isTechnicalData = true)
                    ResultRow("Server", device.server, isTechnicalData = true)
                    ResultRow("Search Target", device.searchTarget, isTechnicalData = true)
                    ResultRow("USN", device.usn, isTechnicalData = true)
                }
            }
        }
    }
}
