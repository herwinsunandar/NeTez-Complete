package com.herwin.netez.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.network.SnmpDevice
import com.herwin.netez.ui.theme.PremiumNavy
import com.herwin.netez.ui.theme.SoftDarkBg
import com.herwin.netez.ui.theme.TextGray
import com.herwin.netez.ui.viewmodels.SnmpScannerViewModel

@Composable
fun SnmpScannerScreen(
    viewModel: SnmpScannerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PremiumNavy)
    ) {
        // 1. Top Input Section
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SoftDarkBg,
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "SNMP Scanner",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextField(
                        value = uiState.subnet,
                        onValueChange = { viewModel.updateSubnet(it) },
                        placeholder = { Text("192.168.1", color = TextGray) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(alpha = 0.05f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        label = { Text("Subnet (x.x.x)", color = TextGray, fontSize = 12.sp) }
                    )

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
                            Icon(Icons.Default.Radar, null)
                            Spacer(Modifier.width(8.dp))
                            Text("SCAN", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (uiState.isScanning || uiState.progress > 0) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LinearProgressIndicator(
                            progress = { uiState.progress },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            color = Color(0xFF6C8CFF),
                            trackColor = Color.White.copy(alpha = 0.05f)
                        )
                        Text(
                            "Scanning... ${(uiState.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextGray
                        )
                    }
                }
            }
        }

        // 2. Results
        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
        ) {
            if (uiState.devices.isEmpty() && !uiState.isScanning && uiState.progress == 1f) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                        Text("No SNMP devices found", color = TextGray)
                    }
                }
            }

            items(uiState.devices) { device ->
                SnmpDeviceCard(device)
            }

            if (uiState.devices.isNotEmpty() && !uiState.isScanning) {
                item {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    com.herwin.netez.ui.components.ResultFooter(
                        fullTextToCopy = uiState.devices.joinToString("\n") { "${it.ip} - ${it.sysDescr}" },
                        onCopyAll = {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("SNMP Devices", uiState.devices.joinToString("\n") { it.ip }))
                            android.widget.Toast.makeText(context, "Copied list", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SnmpDeviceCard(device: SnmpDevice) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = SoftDarkBg
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            com.herwin.netez.ui.components.ResultRow(label = device.ip, value = device.sysDescr)
            Text(
                "Response time: ${device.responseTime}ms",
                color = Color(0xFF10B981),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
