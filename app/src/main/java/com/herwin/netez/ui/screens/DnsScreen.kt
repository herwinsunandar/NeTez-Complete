package com.herwin.netez.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.herwin.netez.network.DnsRecord
import com.herwin.netez.network.DnsRecordType
import com.herwin.netez.network.DnsServer
import com.herwin.netez.network.PublicDnsServers
import com.herwin.netez.ui.theme.TextGray
import com.herwin.netez.ui.viewmodels.DnsViewModel

private val LightBg = Color(0xFFF6F7FB)
private val BrandBlue = Color(0xFF2563EB)
private val TextDark = Color(0xFF111827)
private val TextHint = Color(0xFF9CA3AF)
private val BorderColor = Color(0xFFE6E8F0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnsScreen(
    viewModel: DnsViewModel = viewModel(),
    onNavigateToPing: (String) -> Unit = {},
    onNavigateToTraceroute: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showServerMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBg)
    ) {
        // 1. Top Tabs
        PrimaryScrollableTabRow(
            selectedTabIndex = uiState.recordType.ordinal,
            containerColor = Color.White,
            contentColor = BrandBlue,
            edgePadding = 16.dp,
            divider = {},
            indicator = {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(uiState.recordType.ordinal),
                    color = BrandBlue,
                    height = 3.dp
                )
            }
        ) {
            DnsRecordType.entries.forEach { type ->
                Tab(
                    selected = uiState.recordType == type,
                    onClick = { viewModel.updateRecordType(type) },
                    text = {
                        Text(
                            text = type.name,
                            fontWeight = if (uiState.recordType == type) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 2. DNS Server Selector Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PublicDnsServers.forEach { server ->
                        DnsServerChip(
                            server = server,
                            isSelected = uiState.selectedServer == server,
                            onClick = { viewModel.updateServer(server) }
                        )
                    }
                    // Custom/More Button
                    Surface(
                        onClick = { showServerMenu = true },
                        shape = CircleShape,
                        color = Color.White,
                        border = BorderStroke(1.dp, BorderColor),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ExpandMore, null, modifier = Modifier.size(16.dp), tint = TextGray)
                            
                            DropdownMenu(
                                expanded = showServerMenu,
                                onDismissRequest = { showServerMenu = false },
                                containerColor = Color.White
                            ) {
                                PublicDnsServers.forEach { server ->
                                    DropdownMenuItem(
                                        text = { Text("${server.name} (${server.ip})", fontSize = 14.sp) },
                                        onClick = {
                                            viewModel.updateServer(server)
                                            showServerMenu = false
                                        },
                                        leadingIcon = {
                                            if (server.countryCode.isNotEmpty()) {
                                                AsyncImage(
                                                    model = "https://flagcdn.com/w40/${server.countryCode.lowercase()}.png",
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp, 14.dp).clip(RoundedCornerShape(2.dp))
                                                )
                                            } else {
                                                Icon(Icons.Default.Dns, null, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Input Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextField(
                            value = uiState.domain,
                            onValueChange = { viewModel.updateDomain(it) },
                            placeholder = { Text("Enter domain (e.g. google.com)", color = TextHint, fontSize = 15.sp) },
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = TextDark,
                                unfocusedTextColor = TextDark
                            ),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        )
                        Button(
                            onClick = { viewModel.lookup() },
                            modifier = Modifier.height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                            enabled = !uiState.isLoading,
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                            } else {
                                Text("LOOKUP", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // 4. Status Bar
            uiState.response?.let { res ->
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = BrandBlue.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                if (uiState.selectedServer.countryCode.isNotEmpty()) {
                                    AsyncImage(
                                        model = "https://flagcdn.com/w40/${uiState.selectedServer.countryCode.lowercase()}.png",
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp, 14.dp).clip(RoundedCornerShape(2.dp))
                                    )
                                    Spacer(Modifier.width(10.dp))
                                } else {
                                    Icon(Icons.Default.Dns, null, tint = BrandBlue, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(
                                    text = "Resolved via ${res.resolver} ${if (res.resolverIp == "System") "" else res.resolverIp}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = BrandBlue,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "${res.latency}ms",
                                style = MaterialTheme.typography.labelMedium,
                                color = BrandBlue,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                if (res.records.isEmpty()) {
                    item {
                        EmptyStateSuggestion { viewModel.updateServer(PublicDnsServers[1]) }
                    }
                } else {
                    items(res.records) { record ->
                        DnsProResultCard(
                            record = record,
                            onPing = { onNavigateToPing(record.data) },
                            onTrace = { onNavigateToTraceroute(record.data) }
                        )
                    }
                    item {
                        val localContext = LocalContext.current
                        com.herwin.netez.ui.components.ResultFooter(
                            fullTextToCopy = res.records.joinToString("\n") { it.data },
                            onCopyAll = {
                                val clipboard = localContext.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("DNS Records", res.records.joinToString("\n") { it.data }))
                                android.widget.Toast.makeText(localContext, "All records copied", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }

            if (uiState.error != null && !uiState.isLoading && uiState.response == null) {
                item {
                    EmptyStateSuggestion { viewModel.updateServer(PublicDnsServers[0]) }
                }
            }
            
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun DnsServerChip(server: DnsServer, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) BrandBlue else Color.White,
        border = if (isSelected) null else BorderStroke(1.dp, BorderColor),
        modifier = Modifier.height(36.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = server.name,
                color = if (isSelected) Color.White else TextDark,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = if (server.ip == "System") "" else server.ip,
                color = if (isSelected) Color.White.copy(alpha = 0.7f) else TextHint,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun DnsProResultCard(
    record: DnsRecord,
    onPing: () -> Unit,
    onTrace: () -> Unit
) {
    val context = LocalContext.current
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                com.herwin.netez.ui.components.ResultRow("IP Address", record.data)
            }
            
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = LightBg)
            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ResultMetric("TTL", "${record.ttl}s")
                ResultMetric("TYPE", when(record.type) {
                    1 -> "A"
                    28 -> "AAAA"
                    15 -> "MX"
                    16 -> "TXT"
                    5 -> "CNAME"
                    2 -> "NS"
                    else -> "Type ${record.type}"
                })
                ResultMetric("LOC", record.countryName.ifEmpty { "Global" })
            }

            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionChip(Icons.Default.NetworkCheck, "Ping", onPing)
                ActionChip(Icons.Default.Route, "Trace", onTrace)
            }
        }
    }
}

@Composable
fun ResultMetric(label: String, value: String) {
    Column {
        Text(label, fontSize = 9.sp, color = TextHint, fontWeight = FontWeight.Bold)
        Text(value, fontSize = 12.sp, color = TextDark, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ActionChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = LightBg,
        modifier = Modifier.height(32.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, modifier = Modifier.size(14.dp), tint = BrandBlue)
            Spacer(Modifier.width(6.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextDark)
        }
    }
}

@Composable
fun EmptyStateSuggestion(onTryOther: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Default.SearchOff, null, modifier = Modifier.size(48.dp), tint = TextHint.copy(alpha = 0.5f))
        Text("No records found", color = TextDark, fontWeight = FontWeight.Bold)
        Text(
            "Try switching to a different DNS server or check the domain spelling.",
            color = TextHint,
            fontSize = 12.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        TextButton(onClick = onTryOther) {
            Text("Switch DNS Server", color = BrandBlue, fontWeight = FontWeight.Bold)
        }
    }
}
