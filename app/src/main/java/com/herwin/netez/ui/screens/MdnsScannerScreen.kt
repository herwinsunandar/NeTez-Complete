package com.herwin.netez.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.network.MdnsService
import com.herwin.netez.ui.theme.PremiumNavy
import com.herwin.netez.ui.theme.SoftDarkBg
import com.herwin.netez.ui.theme.TextGray
import com.herwin.netez.ui.viewmodels.MdnsScannerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MdnsScannerScreen(
    viewModel: MdnsScannerViewModel = viewModel()
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
                        "mDNS Discovery",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Scan for Zeroconf/Bonjour services",
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
                        Icon(Icons.Default.WifiTethering, null)
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
            if (uiState.services.isEmpty() && !uiState.isScanning) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No mDNS services found", color = TextGray)
                    }
                }
            }

            items(uiState.services) { service ->
                MdnsServiceCard(service = service)
            }

            if (uiState.services.isNotEmpty() && !uiState.isScanning) {
                item {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    com.herwin.netez.ui.components.ResultFooter(
                        fullTextToCopy = uiState.services.joinToString("\n") { "${it.name} (${it.ipAddress})" },
                        onCopyAll = {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("mDNS Services", uiState.services.joinToString("\n") { it.name }))
                            android.widget.Toast.makeText(context, "Copied list", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MdnsServiceCard(service: MdnsService) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = SoftDarkBg,
        border = if (expanded) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6C8CFF).copy(alpha = 0.3f)) else null
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
                    val icon = when {
                        service.type.contains("printer") -> Icons.Default.Print
                        service.type.contains("http") -> Icons.Default.Http
                        service.type.contains("ssh") -> Icons.Default.Terminal
                        service.type.contains("googlecast") -> Icons.Default.Cast
                        service.type.contains("spotify") -> Icons.Default.MusicNote
                        service.type.contains("airplay") -> Icons.Default.Airplay
                        else -> Icons.Default.Dns
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(service.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text(service.ipAddress ?: "Unknown IP", style = MaterialTheme.typography.bodySmall, color = TextGray)
                    Text(service.type, style = MaterialTheme.typography.labelSmall, color = Color(0xFF6C8CFF), fontSize = 10.sp)
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
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
                    
                    com.herwin.netez.ui.components.ResultRow("Service Name", service.name)
                    com.herwin.netez.ui.components.ResultRow("Service Type", service.type)
                    com.herwin.netez.ui.components.ResultRow("IP Address", service.ipAddress ?: "Unknown")
                    com.herwin.netez.ui.components.ResultRow("Port", service.port.toString())
                    com.herwin.netez.ui.components.ResultRow("Host", service.host ?: "Unknown")
                }
            }
        }
    }
}

