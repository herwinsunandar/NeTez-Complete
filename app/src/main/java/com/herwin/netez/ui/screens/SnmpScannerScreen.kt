package com.herwin.netez.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.herwin.netez.network.SnmpDevice
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.components.ResultFooter
import com.herwin.netez.ui.components.ResultRow
import com.herwin.netez.ui.theme.PremiumNavy
import com.herwin.netez.ui.theme.SoftDarkBg
import com.herwin.netez.ui.theme.TextGray
import com.herwin.netez.ui.viewmodels.SnmpScannerViewModel

@Composable
fun SnmpScannerScreen(
    viewModel: SnmpScannerViewModel = viewModel(),
    onNavigateToPing: (String) -> Unit = {},
    onNavigateToSsh: (String) -> Unit = {},
    onNavigateToWol: (String) -> Unit = {}
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
                            Icon(AppIcons.radar(), null)
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
                SnmpDeviceCard(device, onNavigateToPing, onNavigateToSsh, onNavigateToWol)
            }

            if (uiState.devices.isNotEmpty() && !uiState.isScanning) {
                item {
                    val context = LocalContext.current
                    ResultFooter(
                        fullTextToCopy = uiState.devices.joinToString("\n") { "${it.ip} - ${it.sysDescr}" },
                        onCopyAll = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("SNMP Devices", uiState.devices.joinToString("\n") { it.ip }))
                            Toast.makeText(context, "Copied list", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SnmpDeviceCard(
    device: SnmpDevice,
    onPing: (String) -> Unit,
    onSsh: (String) -> Unit,
    onWol: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = SoftDarkBg
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ResultRow(label = device.ip, value = device.sysDescr)
            Text(
                "Response time: ${device.responseTime}ms",
                color = Color(0xFF10B981),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
            
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = { onPing(device.ip) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF6C8CFF))
                ) {
                    Text("PING", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                TextButton(
                    onClick = { onSsh(device.ip) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF6C8CFF))
                ) {
                    Text("SSH", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = { onWol(device.ip) }) {
                    Icon(AppIcons.powerSettingsNew(), null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
