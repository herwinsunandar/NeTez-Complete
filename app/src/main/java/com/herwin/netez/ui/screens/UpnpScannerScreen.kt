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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.herwin.netez.network.UpnpDevice
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.components.ResultFooter
import com.herwin.netez.ui.components.ResultRow
import com.herwin.netez.ui.theme.PremiumNavy
import com.herwin.netez.ui.theme.SoftDarkBg
import com.herwin.netez.ui.theme.TextGray
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
            .background(PremiumNavy)
    ) {
        // Top Section
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SoftDarkBg,
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "UPnP Discovery",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Scan for UPnP enabled devices",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )
                }

                Button(
                    onClick = { viewModel.startScan() },
                    enabled = !uiState.isScanning,
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C8CFF))
                ) {
                    if (uiState.isScanning) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(AppIcons.radar(), null)
                        Spacer(Modifier.width(8.dp))
                        Text("SCAN", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Results
        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
        ) {
            if (uiState.devices.isEmpty() && !uiState.isScanning) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No UPnP devices found", color = TextGray)
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
        shape = RoundedCornerShape(20.dp),
        color = SoftDarkBg,
        border = if (expanded) BorderStroke(1.dp, Color(0xFF6C8CFF).copy(alpha = 0.3f)) else null
    ) {
        Column(modifier = Modifier.clickable { expanded = !expanded }.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = AppIcons.router(),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(device.friendlyName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text(device.ip, style = MaterialTheme.typography.bodySmall, color = TextGray)
                    if (device.modelName.isNotEmpty()) {
                        Text(device.modelName, style = MaterialTheme.typography.labelSmall, color = Color(0xFF6C8CFF), fontSize = 10.sp)
                    }
                }

                Icon(
                    painter = if (expanded) AppIcons.keyboardArrowUp() else AppIcons.keyboardArrowDown(),
                    contentDescription = null,
                    tint = TextGray
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                    ResultRow("Manufacturer", device.manufacturer.ifEmpty { "Unknown" })
                    ResultRow("Location", device.location)
                    ResultRow("Server", device.server)
                    ResultRow("Search Target", device.searchTarget)
                    ResultRow("USN", device.usn)
                }
            }
        }
    }
}
