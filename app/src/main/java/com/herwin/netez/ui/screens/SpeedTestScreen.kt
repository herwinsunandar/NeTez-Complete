package com.herwin.netez.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.ui.theme.PremiumNavy
import com.herwin.netez.ui.theme.TextGray
import com.herwin.netez.ui.viewmodels.SpeedTestStage
import com.herwin.netez.ui.viewmodels.SpeedTestViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedTestScreen(viewModel: SpeedTestViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showServerDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PremiumNavy)
            .padding(horizontal = 24.dp)
    ) {
        // 1. Top Metrics Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MetricHeaderItem("PING", "${uiState.pingMs.toInt()}ms", Icons.Default.Timer, uiState.stage == SpeedTestStage.PING)
            MetricHeaderItem("JITTER", "${uiState.jitterMs.toInt()}ms", Icons.Default.NetworkCheck, uiState.stage == SpeedTestStage.PING)
            MetricHeaderItem("DOWN", String.format(Locale.US, "%.1f", uiState.downloadSpeed), Icons.Default.Download, uiState.stage == SpeedTestStage.DOWNLOAD)
            MetricHeaderItem("UP", String.format(Locale.US, "%.1f", uiState.uploadSpeed), Icons.Default.Upload, uiState.stage == SpeedTestStage.UPLOAD)
        }

        // 2. Main Large Display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isRunning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(260.dp),
                    color = Color(0xFF6366F1),
                    strokeWidth = 3.dp,
                    trackColor = Color.White.copy(alpha = 0.05f),
                    strokeCap = StrokeCap.Round
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val currentVal = if (uiState.stage == SpeedTestStage.UPLOAD) uiState.uploadSpeed else uiState.downloadSpeed
                Text(
                    text = String.format(Locale.US, "%.1f", currentVal),
                    style = MaterialTheme.typography.displayLarge,
                    fontSize = if (currentVal >= 1000) 64.sp else 84.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "Mbps",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextGray.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = when(uiState.stage) {
                        SpeedTestStage.PING -> "LATENCY TEST"
                        SpeedTestStage.DOWNLOAD -> "DOWNLOAD TEST"
                        SpeedTestStage.UPLOAD -> "UPLOAD TEST"
                        else -> "READY"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (uiState.isRunning) Color(0xFF6366F1) else TextGray,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // 3. Server Info
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                onClick = { if (!uiState.isRunning) showServerDialog = true },
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(16.dp),
                enabled = !uiState.isRunning
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Dns, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(uiState.selectedServer, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    if (!uiState.isRunning) {
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.Edit, contentDescription = null, tint = TextGray, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // 4. ISP Footer / Advanced Metrics (Moved Up)
        if (uiState.stage == SpeedTestStage.FINISHED) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White.copy(alpha = 0.03f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("ADVANCED METRICS", style = MaterialTheme.typography.labelSmall, color = TextGray, letterSpacing = 1.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MetricFooterItem("Loss", "0%", Icons.Default.GppBad)
                        MetricFooterItem("ISP", uiState.isp, Icons.Default.Language)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        } else {
            Spacer(Modifier.height(16.dp))
        }

        // 5. Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { viewModel.startTest() },
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6366F1),
                    disabledContainerColor = Color(0xFF6366F1).copy(alpha = 0.3f)
                ),
                enabled = !uiState.isRunning
            ) {
                if (uiState.isRunning) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("START TEST", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                }
            }

            if (uiState.stage == SpeedTestStage.FINISHED) {
                IconButton(
                    onClick = {
                        val resultText = String.format(
                            Locale.US,
                            "NeTez Speed Test Result:\nServer: %s\nDownload: %.1f Mbps\nUpload: %.1f Mbps\nPing: %d ms\nJitter: %d ms",
                            uiState.selectedServer, uiState.downloadSpeed, uiState.uploadSpeed, uiState.pingMs.toInt(), uiState.jitterMs.toInt()
                        )
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "NeTez Speed Test")
                            putExtra(Intent.EXTRA_TEXT, resultText)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Result"))
                    },
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                }
            }
        }

        Spacer(Modifier.weight(1f))
    }

    if (showServerDialog) {
        ServerSelectionDialog(
            currentServer = uiState.selectedServer,
            onDismiss = { showServerDialog = false },
            onSelect = { 
                viewModel.updateServer(it)
                showServerDialog = false
            }
        )
    }
}

@Composable
fun MetricHeaderItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isActive: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = if (isActive) Color(0xFF6366F1) else TextGray, modifier = Modifier.size(10.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, color = if (isActive) Color(0xFF6366F1) else TextGray, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
        Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun MetricFooterItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color(0xFF6366F1).copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, color = TextGray, fontSize = 10.sp)
            Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ServerSelectionDialog(currentServer: String, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    var customServer by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Test Server") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { onSelect("speed.cloudflare.com") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = if(currentServer == "speed.cloudflare.com") Color(0xFF6366F1) else Color.Gray)
                ) {
                    Text("Cloudflare Global (Auto)")
                }
                Text("Custom Server URL", style = MaterialTheme.typography.labelMedium)
                OutlinedTextField(
                    value = customServer,
                    onValueChange = { customServer = it },
                    placeholder = { Text("e.g. speed.hetzner.de") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if(customServer.isNotEmpty()) onSelect(customServer) }) {
                Text("CONFIRM")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL")
            }
        }
    )
}
