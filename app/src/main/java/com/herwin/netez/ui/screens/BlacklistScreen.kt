package com.herwin.netez.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.R
import com.herwin.netez.network.BlacklistResult
import com.herwin.netez.network.BlacklistStatus
import com.herwin.netez.network.BlacklistCategory
import com.herwin.netez.network.api.AbuseData
import com.herwin.netez.network.api.ShodanResponse
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.theme.PremiumNavy
import com.herwin.netez.ui.theme.SoftDarkBg
import com.herwin.netez.ui.theme.TextGray
import com.herwin.netez.ui.viewmodels.BlacklistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlacklistScreen(
    viewModel: BlacklistViewModel = viewModel()
) {
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        viewModel.fetchMyIp()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PremiumNavy)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Input Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SoftDarkBg),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = viewModel.inputQuery,
                            onValueChange = { viewModel.onQueryChange(it) },
                            label = { Text("IP / Domain", color = TextGray) },
                            placeholder = { Text("8.8.8.8 or google.com", color = TextGray.copy(alpha = 0.5f)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF6C8CFF),
                                unfocusedBorderColor = TextGray
                            ),
                            singleLine = true,
                            trailingIcon = {
                                if (viewModel.inputQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onQueryChange("") }) {
                                        Icon(AppIcons.close(), contentDescription = null, tint = TextGray)
                                    }
                                }
                            }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color(0xFF6C8CFF).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Detected: ${viewModel.detectedType}",
                                    color = Color(0xFF6C8CFF),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            
                            TextButton(
                                onClick = { viewModel.fetchMyIp() },
                                colors = ButtonDefaults.buttonColors(contentColor = Color(0xFF6C8CFF))
                            ) {
                                Text(stringResource(R.string.blacklist_btn_check_my_ip), fontSize = 12.sp)
                            }
                        }

                        Button(
                            onClick = { viewModel.checkIp(viewModel.inputQuery) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C8CFF)),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !viewModel.isScanning
                        ) {
                            if (viewModel.isScanning) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text(stringResource(R.string.blacklist_btn_check), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 1.1 Error Message
            if (viewModel.error != null && !viewModel.isScanning) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(AppIcons.error(), null, tint = Color(0xFFEF4444))
                            Spacer(Modifier.width(12.dp))
                            Text(viewModel.error!!, color = Color(0xFFEF4444), fontSize = 14.sp)
                        }
                    }
                }
            }

            // 2. Results Toolbar (Copy & Share)
            if (viewModel.results.isNotEmpty() && !viewModel.isScanning) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Scan Results", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                        Row {
                            IconButton(onClick = {
                                viewModel.copyToClipboard()
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(AppIcons.contentCopy(), null, tint = Color(0xFF6C8CFF))
                            }
                            IconButton(onClick = {
                                viewModel.shareResults(context)
                            }) {
                                Icon(AppIcons.share(), null, tint = Color(0xFF6C8CFF))
                            }
                        }
                    }
                }
            }

            // 3. AbuseIPDB Card
            if (viewModel.abuseIpDbResponse != null && !viewModel.isScanning) {
                item {
                    viewModel.abuseIpDbResponse?.let {
                        AbuseIpDbCard(it.data)
                    }
                }
            }

            // 3.1 Shodan Card
            if (viewModel.shodanResponse != null && !viewModel.isScanning) {
                item {
                    viewModel.shodanResponse?.let {
                        ShodanCard(it)
                    }
                }
            }

            // 4. Pro Status Summary Card
            if (viewModel.results.isNotEmpty() || viewModel.isScanning) {
                item {
                    val isListed = viewModel.results.any { it.status == BlacklistStatus.LISTED }
                    val statusText = if (isListed) "LISTED" else "CLEAN"
                    val statusColor = when {
                        viewModel.riskScore > 50 -> Color(0xFFEF4444)
                        viewModel.riskScore > 20 -> Color(0xFFF59E0B)
                        else -> Color(0xFF10B981)
                    }
                    val statusIcon = if (isListed) AppIcons.warning() else AppIcons.check()
                    val bandColor = statusColor.copy(alpha = 0.1f)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SoftDarkBg),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Surface(
                                    color = bandColor,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        painter = statusIcon,
                                        contentDescription = null,
                                        tint = statusColor,
                                        modifier = Modifier.padding(12.dp).fillMaxSize()
                                    )
                                }
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = statusText, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                    Text(text = viewModel.inputQuery, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                                    
                                    if (viewModel.detectedType == "Domain" && viewModel.resolvedIp != null) {
                                        Text(
                                            text = "Resolved IP: ${viewModel.resolvedIp}",
                                            color = Color(0xFF6C8CFF),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    viewModel.ipInfo?.let { info ->
                                        Text(
                                            text = "ISP: ${info.isp} • ${info.city}, ${info.countryCode}",
                                            color = TextGray,
                                            fontSize = 11.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                            
                            // Risk Score
                            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Aggregated Risk Score", fontSize = 10.sp, color = TextGray)
                                    Text("${viewModel.riskScore.toInt()}/100", fontSize = 10.sp, color = statusColor, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(6.6.dp))
                                LinearProgressIndicator(
                                    progress = { viewModel.riskScore / 100f },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                                    color = statusColor,
                                    trackColor = Color.White.copy(alpha = 0.05f)
                                )
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }

            // 5. Summary Chip Row
            if (viewModel.results.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val listedCount = viewModel.results.count { it.status == BlacklistStatus.LISTED }
                        val cleanCount = viewModel.results.count { it.status == BlacklistStatus.CLEAN }
                        val timeoutCount = viewModel.results.count { it.status == BlacklistStatus.TIMEOUT }
                        
                        SummaryStatusChip("$cleanCount Clean", Color(0xFF10B981), true)
                        SummaryStatusChip("$listedCount Listed", if(listedCount > 0) Color(0xFFEF4444) else TextGray, listedCount > 0)
                        if (timeoutCount > 0) {
                            SummaryStatusChip("$timeoutCount Rate Limit", Color(0xFFF59E0B), true)
                        }
                    }
                }
            }

            // 6. Detailed RBL Results
            val domainResults = viewModel.results.filter { it.category == BlacklistCategory.DOMAIN }
            val ipResults = viewModel.results.filter { it.category == BlacklistCategory.IP }

            if (domainResults.isNotEmpty()) {
                item { BlacklistSectionHeader("Domain Blacklists") }
                items(domainResults) { ProRblResultItem(it) }
            }
            
            if (ipResults.isNotEmpty()) {
                item { BlacklistSectionHeader(if (viewModel.detectedType == "Domain") "IP Blacklists (Resolved)" else "IP Blacklists") }
                items(ipResults) { ProRblResultItem(it) }
            }
            
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
fun BlacklistSectionHeader(title: String) {
    Text(
        text = title,
        color = Color(0xFF6C8CFF),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun AbuseIpDbCard(data: AbuseData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2235)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF6C8CFF).copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons.security(), contentDescription = null, tint = Color(0xFF6C8CFF), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("AbuseIPDB Intelligence", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                Spacer(Modifier.weight(1f))
                Surface(
                    color = when {
                        data.abuseConfidenceScore > 50 -> Color(0xFFEF4444).copy(alpha = 0.1f)
                        data.abuseConfidenceScore > 20 -> Color(0xFFF59E0B).copy(alpha = 0.1f)
                        else -> Color(0xFF10B981).copy(alpha = 0.1f)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "${data.abuseConfidenceScore}% Confidence",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = when {
                            data.abuseConfidenceScore > 50 -> Color(0xFFEF4444)
                            data.abuseConfidenceScore > 20 -> Color(0xFFF59E0B)
                            else -> Color(0xFF10B981)
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                AbuseInfoItem("Reports", data.totalReports.toString())
                AbuseInfoItem("Country", data.countryCode)
                AbuseInfoItem("Usage", data.usageType ?: "Unknown")
            }
            
            Spacer(Modifier.height(12.dp))
            
            Text("Latest Reports:", color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            data.reports?.take(3)?.forEach { report ->
                Text(
                    "• ${report.reportedAt.substringBefore("T")}: ${report.comment}",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun AbuseInfoItem(label: String, value: String) {
    Column {
        Text(label, color = TextGray, fontSize = 10.sp)
        Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ShodanCard(data: ShodanResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SoftDarkBg),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF6C8CFF).copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons.cloud(), contentDescription = null, tint = Color(0xFF6C8CFF), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Shodan Open Ports", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
            }
            
            Spacer(Modifier.height(12.dp))
            
            if (data.ports.isEmpty()) {
                Text("No open ports found in Shodan database.", color = TextGray, fontSize = 11.sp)
            } else {
                Text("Detected Ports: ${data.ports.joinToString(", ")}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                
                Spacer(Modifier.height(8.dp))
                
                data.data?.take(5)?.forEach { service ->
                    Text(
                        "• Port ${service.port}/${service.transport}: ${service.product ?: "Unknown"} ${service.version ?: ""}",
                        color = TextGray,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
            }
            
            if (data.os != null) {
                Spacer(Modifier.height(8.dp))
                Text("OS: ${data.os}", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SummaryStatusChip(label: String, color: Color, isHighlighted: Boolean) {
    Surface(
        color = if (isHighlighted) color.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(8.dp),
        border = if (isHighlighted) BorderStroke(1.dp, color.copy(alpha = 0.2f)) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(8.dp))
            Text(label, color = if (isHighlighted) color else TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ProRblResultItem(result: BlacklistResult) {
    val isListed = result.status == BlacklistStatus.LISTED
    val isTimeout = result.status == BlacklistStatus.TIMEOUT
    val statusColor = when(result.status) {
        BlacklistStatus.LISTED -> Color(0xFFEF4444)
        BlacklistStatus.CLEAN -> Color(0xFF10B981)
        BlacklistStatus.TIMEOUT -> Color(0xFFF59E0B)
        else -> TextGray
    }
    val context = LocalContext.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SoftDarkBg),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(statusColor))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(result.name, fontSize = 11.sp, color = TextGray)
                    Text(
                        text = when(result.status) {
                            BlacklistStatus.LISTED -> "LISTED - ${result.reason ?: "Unknown"}"
                            BlacklistStatus.TIMEOUT -> "TIMEOUT (Rate Limit)"
                            BlacklistStatus.CLEAN -> "CLEAN (${result.responseTime}ms)"
                            else -> "ERROR"
                        },
                        color = if (isListed) Color(0xFFEF4444) else if (isTimeout) Color(0xFFF59E0B) else Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                IconButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("RBL Result", "${result.name}: ${result.status}"))
                    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                }, modifier = Modifier.size(24.dp)) {
                    Icon(AppIcons.contentCopy(), null, tint = TextGray.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                }
            }
            
            if (result.resultIp != null) {
                Row(
                    modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Result: ${result.resultIp}",
                        color = TextGray,
                        fontSize = 11.sp
                    )
                    
                    if (isListed) {
                        Surface(
                            modifier = Modifier.clickable {
                                val url = when {
                                    result.domain.contains("spamhaus") -> "https://check.spamhaus.org/"
                                    result.domain.contains("spamcop") -> "https://www.spamcop.net/bl.shtml"
                                    else -> "https://www.google.com/search?q=delist+${result.domain}"
                                }
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            },
                            color = Color(0xFFEF4444).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "Delist →",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = Color(0xFFEF4444),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
