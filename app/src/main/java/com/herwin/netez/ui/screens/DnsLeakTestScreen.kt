package com.herwin.netez.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.R
import com.herwin.netez.network.DnsLeakItem
import com.herwin.netez.network.DnsLeakResult
import com.herwin.netez.network.DnsLeakStatus
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.theme.PremiumNavy
import com.herwin.netez.ui.theme.SoftDarkBg
import com.herwin.netez.ui.theme.TextGray
import com.herwin.netez.ui.viewmodels.DnsLeakViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnsLeakTestScreen(
    viewModel: DnsLeakViewModel = viewModel()
) {
    val result = viewModel.result
    val isTesting = viewModel.isTesting
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PremiumNavy)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Status Card
            item {
                DnsLeakStatusCard(result, isTesting, onTestClick = { viewModel.startTest() })
            }

            if (result != null && !isTesting) {
                // Toolbar Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { 
                            viewModel.copyToClipboard(context)
                            Toast.makeText(context, "Results copied to clipboard", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(AppIcons.contentCopy(), contentDescription = "Copy", tint = Color(0xFF6C8CFF))
                        }
                        IconButton(onClick = { viewModel.shareResults(context) }) {
                            Icon(AppIcons.share(), contentDescription = "Share", tint = Color(0xFF6C8CFF))
                        }
                    }
                }

                // 2. Score Card
                item {
                    DnsLeakScoreCard(result)
                }

                // 3. Summary Chips
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SummaryChip(stringResource(R.string.dns_leak_summary_all, result.dnsServers.size), Color(0xFF6C8CFF))
                        SummaryChip(stringResource(R.string.dns_leak_summary_leaked, result.leakCount), if (result.leakCount > 0) Color(0xFFFF5252) else TextGray)
                        SummaryChip(stringResource(R.string.dns_leak_summary_safe, result.dnsServers.size - result.leakCount), Color(0xFF4CAF50))
                    }
                }

                // 4. DNS Servers List
                items(result.dnsServers) { item ->
                    DnsServerCard(item)
                }
            } else if (!isTesting) {
                item {
                    Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.dns_leak_not_scanned), color = TextGray)
                    }
                }
            }
            
            if (isTesting) {
                item {
                    Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF6C8CFF))
                    }
                }
            }
        }
    }
}

@Composable
fun DnsLeakStatusCard(result: DnsLeakResult?, isTesting: Boolean, onTestClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SoftDarkBg),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color(0xFF6C8CFF).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        painter = AppIcons.shield(),
                        contentDescription = null,
                        tint = Color(0xFF6C8CFF),
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(stringResource(R.string.dns_leak_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    if (result != null) {
                        Text("Your IP: ${result.publicIp}", color = TextGray, fontSize = 12.sp)
                    }
                }
            }

            if (result != null) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("ISP: ${result.isp}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    val dnsText = result.dnsServers.firstOrNull()?.let { "${it.ip} (${it.isp})" } ?: "Unknown"
                    Text("DNS: $dnsText", color = TextGray, fontSize = 12.sp)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusChip(if (result.vpnActive) "VPN ON" else "VPN OFF", if (result.vpnActive) Color(0xFF4CAF50) else Color(0xFFFF5252))
                    val leakStatus = if (result.leakCount > 0) stringResource(R.string.dns_leak_status_leaking, result.leakCount) else stringResource(R.string.dns_leak_status_safe)
                    StatusChip(leakStatus, if (result.leakCount > 0) Color(0xFFFF5252) else Color(0xFF4CAF50))
                }
            }

            Button(
                onClick = onTestClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C8CFF)),
                enabled = !isTesting
            ) {
                Text(stringResource(R.string.dns_leak_btn_test), fontWeight = FontWeight.ExtraBold)
            }
            
            var showExplanation by remember { mutableStateOf(false) }
            OutlinedButton(
                onClick = { showExplanation = true },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF6C8CFF).copy(alpha = 0.3f))
            ) {
                Text(stringResource(R.string.dns_leak_btn_what_is), color = Color(0xFF6C8CFF))
            }
            
            if (showExplanation) {
                AlertDialog(
                    onDismissRequest = { showExplanation = false },
                    containerColor = SoftDarkBg,
                    title = { Text(stringResource(R.string.dns_leak_explanation_title), color = Color.White) },
                    text = { Text(stringResource(R.string.dns_leak_explanation_content), color = TextGray) },
                    confirmButton = {
                        TextButton(onClick = { showExplanation = false }) {
                            Text("OK", color = Color(0xFF6C8CFF))
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun DnsLeakScoreCard(result: DnsLeakResult) {
    val statusColor = when (result.status) {
        "SAFE" -> Color(0xFF4CAF50)
        "WARNING" -> Color(0xFFFFC107)
        else -> Color(0xFFFF5252)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SoftDarkBg),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.dns_leak_score_title), style = MaterialTheme.typography.labelMedium, color = TextGray)
                Text(stringResource(R.string.dns_leak_tested_time, String.format(Locale.US, "%.1fs", result.testDurationMs / 1000f)), style = MaterialTheme.typography.labelSmall, color = TextGray)
            }
            
            Spacer(Modifier.height(8.dp))
            
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${result.score}/100", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = statusColor)
                Spacer(Modifier.width(8.dp))
                Text(result.status, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = statusColor, modifier = Modifier.padding(bottom = 4.dp))
            }
            
            Spacer(Modifier.height(12.dp))
            
            LinearProgressIndicator(
                progress = { result.score / 100f },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = statusColor,
                trackColor = Color.White.copy(alpha = 0.05f)
            )
            
            Spacer(Modifier.height(16.dp))
            
            val descRes = when(result.status) {
                "SAFE" -> R.string.dns_leak_status_safe_desc
                "WARNING" -> R.string.dns_leak_status_warning_desc
                else -> R.string.dns_leak_status_critical_desc
            }
            Text(stringResource(descRes), color = TextGray, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
fun DnsServerCard(item: DnsLeakItem) {
    val context = LocalContext.current
    val statusColor = when(item.status) {
        DnsLeakStatus.SAFE -> Color(0xFF4CAF50)
        DnsLeakStatus.LEAKED -> Color(0xFFFF5252)
        DnsLeakStatus.INFO -> Color(0xFFFFC107)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SoftDarkBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(statusColor))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("${item.ip} - ${item.isp}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        text = stringResource(R.string.dns_leak_detail_line, item.latency),
                        color = TextGray,
                        fontSize = 11.sp
                    )
                }
                IconButton(onClick = { 
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("DNS IP", item.ip)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "IP ${item.ip} copied", Toast.LENGTH_SHORT).show()
                }, modifier = Modifier.size(24.dp)) {
                    Icon(AppIcons.contentCopy(), null, tint = TextGray.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                }
            }
            
            if (item.status == DnsLeakStatus.LEAKED) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    color = Color(0xFFFF5252).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.2f))
                ) {
                    Text(
                        text = stringResource(R.string.dns_leak_card_leaked, item.isp),
                        color = Color(0xFFFF5252),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            } else if (item.status == DnsLeakStatus.INFO) {
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.dns_leak_card_info_no_vpn), color = Color(0xFFFFC107), fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun StatusChip(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

@Composable
fun SummaryChip(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
    }
}
