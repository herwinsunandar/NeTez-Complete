package com.herwin.netez.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.theme.TechMonospace
import com.herwin.netez.ui.viewmodels.IpCalculatorViewModel
import com.herwin.netez.utils.IpCalculator

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
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // IP Input Section Card
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = uiState.fullInput,
                            onValueChange = { viewModel.updateIp(it) },
                            placeholder = {
                                Text(
                                    text = "e.g. 192.168.1.1/24 or 10.0.0.1",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    fontSize = 13.sp
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Done
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            ),
                            trailingIcon = {
                                if (uiState.fullInput.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.updateIp("") }) {
                                        Icon(
                                            painter = AppIcons.close(),
                                            contentDescription = "Clear",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }

            uiState.result?.let { result ->
                // Share & Copy Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Network Information",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Row {
                            IconButton(onClick = {
                                viewModel.copyToClipboard()
                                Toast.makeText(context, "Results copied to clipboard", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(
                                    painter = AppIcons.contentCopy(),
                                    contentDescription = "Copy Results",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = {
                                viewModel.shareResults(context)
                            }) {
                                Icon(
                                    painter = AppIcons.share(),
                                    contentDescription = "Share Results",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                // Chips Row
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IpInfoChip("Class ${result.ipClass}")
                        IpInfoChip(if (result.isPrivate) "Private" else "Public")
                        IpInfoChip("Usable: ${result.totalHosts}")
                    }
                }

                // Main Info Card
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        tonalElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
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
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        tonalElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Advanced Binary & Hex Info",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            InfoRow("Binary IP", result.binaryIp.chunked(8).joinToString("."))
                            InfoRow("Hexadecimal", result.hexIp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Network / Host Bit Division",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            BinaryBar(result.cidr)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Supernet Block Summary",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val supernetCidr = (result.cidr - 1).coerceAtLeast(0)
                            val supernet = IpCalculator.calculate(result.networkId, supernetCidr)
                            Text(
                                text = "Parent Block: ${supernet?.networkId}/$supernetCidr",
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = TechMonospace,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // VLSM Splitter Section
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Subnet Breakdown (VLSM)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Dividing current block into /${result.cidr + 1} subnets",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                val subnets = IpCalculator.splitSubnet(result.networkId, result.cidr)
                items(subnets) { sub ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = sub.subnet,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontFamily = TechMonospace,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = sub.range,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = TechMonospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${sub.usable} hosts",
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = TechMonospace,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }

        // Bottom Control Panel
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // CIDR Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Subnet CIDR Mask",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            shape = CircleShape
                        ) {
                            Text(
                                text = "/${uiState.cidr}",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.primary,
                                fontFamily = TechMonospace,
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { onNavigateToPing(uiState.ip.ifBlank { "192.168.1.1" }) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            painter = AppIcons.networkCheck(),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("PING", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { onNavigateToScan(uiState.ip.ifBlank { "192.168.1.1" }) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            painter = AppIcons.wifi(),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("SCAN", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun IpInfoChip(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontFamily = TechMonospace,
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
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = TechMonospace,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun BinaryBar(cidr: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(CircleShape)
    ) {
        for (i in 0 until 32) {
            val color = if (i < cidr) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(color)
                    .border(0.5.dp, MaterialTheme.colorScheme.surface)
            )
        }
    }
}
