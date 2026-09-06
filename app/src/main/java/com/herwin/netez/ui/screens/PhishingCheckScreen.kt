package com.herwin.netez.ui.screens

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
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.theme.PremiumNavy
import com.herwin.netez.ui.theme.SoftDarkBg
import com.herwin.netez.ui.theme.TextGray
import com.herwin.netez.ui.viewmodels.PhishingCheckViewModel
import com.herwin.netez.ui.viewmodels.PhishingResult
import com.herwin.netez.ui.viewmodels.PhishingStatus
import com.herwin.netez.ui.viewmodels.PhishingCheckItem
import com.herwin.netez.workers.UrlHausFeedWorker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhishingCheckScreen(
    viewModel: PhishingCheckViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.history.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PremiumNavy)
    ) {
        // 0. Feed Status Chip
        Surface(
            color = SoftDarkBg,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFF6C8CFF).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable { 
                        UrlHausFeedWorker.schedule(context, true)
                        Toast.makeText(context, "Updating feed...", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(
                        text = "URLhaus: Updated ${viewModel.lastUpdateText} • ${viewModel.urlHausCount / 1000}k URLs",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        color = Color(0xFF6C8CFF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

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
                            value = viewModel.inputUrl,
                            onValueChange = { viewModel.onUrlChange(it) },
                            label = { Text(stringResource(R.string.phishing_field_url), color = TextGray) },
                            placeholder = { Text(stringResource(R.string.phishing_placeholder_url), color = TextGray.copy(alpha = 0.5f)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF6C8CFF),
                                unfocusedBorderColor = TextGray
                            ),
                            singleLine = true,
                            trailingIcon = {
                                if (viewModel.inputUrl.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onUrlChange("") }) {
                                        Icon(AppIcons.close(), contentDescription = null, tint = TextGray)
                                    }
                                }
                            }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { viewModel.pasteFromClipboard() },
                                colors = ButtonDefaults.buttonColors(contentColor = Color(0xFF6C8CFF), containerColor = Color.Transparent)
                            ) {
                                Text(stringResource(R.string.phishing_btn_paste), fontSize = 12.sp)
                            }
                        }

                        Button(
                            onClick = { viewModel.checkPhishing(viewModel.inputUrl) },
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
                        
                        Text(
                            text = "🔒 Offline feed dicek dulu (privacy). Online check hanya kirim URL tanpa token ke Abuse.ch Swiss non-profit",
                            color = TextGray,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // 2. Results Header
            if (viewModel.result != null && !viewModel.isScanning) {
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
                                Toast.makeText(context, "Results copied to clipboard", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(AppIcons.contentCopy(), null, tint = Color(0xFF6C8CFF))
                            }
                            IconButton(onClick = { viewModel.shareResults(context) }) {
                                Icon(AppIcons.share(), null, tint = Color(0xFF6C8CFF))
                            }
                        }
                    }
                }

                // 3. Summary Card
                item {
                    val res = viewModel.result!!
                    val statusColor = when(res.status) {
                        PhishingStatus.PHISHING -> Color(0xFFEF4444)
                        PhishingStatus.SUSPICIOUS -> Color(0xFFF59E0B)
                        PhishingStatus.SAFE -> Color(0xFF10B981)
                    }
                    val statusIcon = when(res.status) {
                        PhishingStatus.PHISHING -> AppIcons.error()
                        PhishingStatus.SUSPICIOUS -> AppIcons.warning()
                        PhishingStatus.SAFE -> AppIcons.checkCircle()
                    }
                    val statusText = when(res.status) {
                        PhishingStatus.PHISHING -> stringResource(R.string.phishing_status_phishing)
                        PhishingStatus.SUSPICIOUS -> stringResource(R.string.phishing_status_suspicious)
                        PhishingStatus.SAFE -> stringResource(R.string.phishing_status_safe)
                    }
                    val statusDesc = when(res.status) {
                        PhishingStatus.PHISHING -> stringResource(R.string.phishing_phishing_desc)
                        PhishingStatus.SUSPICIOUS -> stringResource(R.string.phishing_suspicious_desc)
                        PhishingStatus.SAFE -> stringResource(R.string.phishing_safe_desc)
                    }

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
                                    color = statusColor.copy(alpha = 0.1f),
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
                                    Text(text = res.domain, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                                    if (res.status == PhishingStatus.PHISHING) {
                                        val threatItem = res.checks.find { it.status == "PHISHING" }
                                        Text(text = "Terverifikasi! ${threatItem?.description ?: ""}", color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    } else {
                                        Text(text = statusDesc, color = TextGray, fontSize = 11.sp)
                                    }
                                }
                            }
                            
                            // Risk Score
                            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(stringResource(R.string.phishing_risk_score), fontSize = 10.sp, color = TextGray)
                                    Text("${res.riskScore}/100", fontSize = 10.sp, color = statusColor, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { res.riskScore / 100f },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                                    color = statusColor,
                                    trackColor = Color.White.copy(alpha = 0.05f)
                                )
                                if (res.status == PhishingStatus.PHISHING) {
                                    Spacer(Modifier.height(12.dp))
                                    Button(
                                        onClick = { /* Report & Block */ },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Blokir & Laporkan", fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                    }
                }
                
                // 4. Detail Layers
                items(viewModel.result?.checks ?: emptyList()) { check ->
                    PhishingCheckItemCard(
                        item = check,
                        onCopy = {
                            viewModel.copyItemInfo(context, check)
                            Toast.makeText(context, "Item info copied", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // Education Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF6C8CFF).copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF6C8CFF).copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.phishing_education_title), color = Color(0xFF6C8CFF), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.phishing_education_content), color = TextGray, fontSize = 12.sp, lineHeight = 20.sp)
                    }
                }
            }
            
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
fun PhishingCheckItemCard(
    item: PhishingCheckItem,
    onCopy: () -> Unit
) {
    val context = LocalContext.current
    val statusColor = when(item.status) {
        "LISTED", "PHISHING", "NO SSL" -> Color(0xFFEF4444)
        "SUSPICIOUS" -> Color(0xFFF59E0B)
        "CLEAN", "SAFE", "VALID" -> Color(0xFF10B981)
        else -> TextGray
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SoftDarkBg),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(statusColor))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.name, fontSize = 11.sp, color = TextGray)
                        if (item.isVerified) {
                            Spacer(Modifier.width(8.dp))
                            Surface(color = Color(0xFF6C8CFF).copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                Text("VERIFIED", color = Color(0xFF6C8CFF), fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }
                    }
                    Text(
                        text = "${item.status} (${item.latency}ms)",
                        color = if (item.status == "CLEAN" || item.status == "SAFE") Color.White else statusColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(item.description, color = TextGray, fontSize = 10.sp)
                }
                
                IconButton(
                    onClick = onCopy, 
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(AppIcons.contentCopy(), null, tint = TextGray.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                }
            }

            if (item.detailUrl != null) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { 
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.detailUrl))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C8CFF).copy(alpha = 0.1f), contentColor = Color(0xFF6C8CFF)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Lihat di URLhaus", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
