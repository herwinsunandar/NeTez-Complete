package com.herwin.netez.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.tooling.preview.Preview
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.theme.NeTezTheme
import com.herwin.netez.ui.theme.PremiumNavy
import com.herwin.netez.ui.theme.SoftDarkBg
import com.herwin.netez.ui.theme.TextGray
import com.herwin.netez.ui.viewmodels.IpCalculatorViewModel
import com.herwin.netez.utils.IpCalculator
import com.herwin.netez.utils.VlsmSubnet

@Preview(showBackground = true)
@Composable
fun IpCalculatorScreenPreview() {
    NeTezTheme(darkTheme = true) {
        IpCalculatorScreen()
    }
}

@Composable
fun IpCalculatorScreen(
    viewModel: IpCalculatorViewModel = viewModel(),
    onNavigateToPing: (String) -> Unit = {},
    onNavigateToScan: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PremiumNavy)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            // IP Input Section
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SoftDarkBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        TextField(
                            value = uiState.fullInput,
                            onValueChange = { viewModel.updateIp(it) },
                            placeholder = { Text("IP Address/CIDR", color = TextGray) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }

            uiState.result?.let { result ->
                // Share & Copy Row (Like Ping Test)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Network Information",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Row {
                            IconButton(onClick = {
                                viewModel.copyToClipboard()
                                Toast.makeText(context, "Results copied to clipboard", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(AppIcons.contentCopy(), contentDescription = "Copy Results", tint = Color(0xFF6C8CFF))
                            }
                            IconButton(onClick = {
                                viewModel.shareResults(context)
                            }) {
                                Icon(AppIcons.share(), contentDescription = "Share Results", tint = Color(0xFF6C8CFF))
                            }
                        }
                    }
                }
                // Chips Row
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IpInfoChip(result.ipClass)
                        IpInfoChip(if (result.isPrivate) "Private" else "Public")
                        IpInfoChip("Usable: ${result.totalHosts}")
                    }
                }

                // Main Info Card
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = SoftDarkBg
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            InfoRow("Network ID", result.networkId)
                            InfoRow("Broadcast", result.broadcast)
                            InfoRow("First Host", result.firstHost)
                            InfoRow("Last Host", result.lastHost)
                            InfoRow("Subnet Mask", result.mask)
                            InfoRow("Wildcard", result.wildcard)
                        }
                    }
                }

                // Binary & Hex Section
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = SoftDarkBg
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Advanced Info", style = MaterialTheme.typography.titleSmall, color = Color.White)
                            InfoRow("Binary IP", result.binaryIp.chunked(8).joinToString("."))
                            InfoRow("Hexadecimal", result.hexIp)
                            Spacer(Modifier.height(8.dp))
                            Text("Network/Host Bits", style = MaterialTheme.typography.labelSmall, color = TextGray)
                            BinaryBar(result.cidr)
                            Spacer(Modifier.height(8.dp))
                            Text("Supernet Summary", style = MaterialTheme.typography.labelSmall, color = TextGray)
                            val supernetCidr = (result.cidr - 1).coerceAtLeast(0)
                            val supernet = IpCalculator.calculate(result.networkId, supernetCidr)
                            Text("Parent Block: ${supernet?.networkId}/$supernetCidr", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // VLSM Splitter Section
                item {
                    Column {
                        Text("Subnet Breakdown (VLSM)", style = MaterialTheme.typography.titleSmall, color = Color.White)
                        Text("Dividing current block into /${result.cidr + 1} subnets", style = MaterialTheme.typography.labelSmall, color = TextGray)
                    }
                }

                val subnets = IpCalculator.splitSubnet(result.networkId, result.cidr)
                items(subnets) { sub ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = SoftDarkBg.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(sub.subnet, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(sub.range, style = MaterialTheme.typography.labelSmall, color = TextGray)
                            }
                            Text("${sub.usable} hosts", style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }
                    }
                }
            }
            
            item { Spacer(Modifier.height(16.dp)) }
        }

        // Bottom Control Panel
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SoftDarkBg,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                // CIDR Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Subnet CIDR", style = MaterialTheme.typography.titleSmall, color = Color.White)
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                            shape = CircleShape
                        ) {
                            Text(
                                "/${uiState.cidr}",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Slider(
                        value = uiState.cidr.toFloatOrNull() ?: 24f,
                        onValueChange = { viewModel.updateCidr(it.toInt().toString()) },
                        valueRange = 0f..32f,
                        steps = 31
                    )
                }

                // Action Buttons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = { onNavigateToPing(uiState.ip) },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.NetworkCheck, null)
                        Spacer(Modifier.width(8.dp))
                        Text("PING")
                    }
                    Button(
                        onClick = { onNavigateToScan(uiState.ip) },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Radar, null)
                        Spacer(Modifier.width(8.dp))
                        Text("SCAN")
                    }
                }
            }
        }
    }
}

@Composable
fun IpInfoChip(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextGray)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
fun BinaryBar(cidr: Int) {
    Row(modifier = Modifier.fillMaxWidth().height(12.dp).clip(CircleShape)) {
        for (i in 0 until 32) {
            val color = if (i < cidr) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f)
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(color).border(0.5.dp, SoftDarkBg))
        }
    }
}


