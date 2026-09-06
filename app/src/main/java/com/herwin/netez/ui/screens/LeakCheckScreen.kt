package com.herwin.netez.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.herwin.netez.ui.viewmodels.LeakCheckViewModel
import com.herwin.netez.ui.viewmodels.LeakCheckResult
import com.herwin.netez.ui.viewmodels.BreachInfo
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeakCheckScreen(
    viewModel: LeakCheckViewModel = viewModel()
) {
    val context = LocalContext.current
    val detectedCount = viewModel.getDetectedEmailCount()
    
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.parseCsvFile(it) }
    }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { os ->
                os.write(viewModel.getExportCsvContent().toByteArray())
                Toast.makeText(context, "Results exported successfully", Toast.LENGTH_SHORT).show()
            }
        }
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
                            value = viewModel.inputEmails,
                            onValueChange = { viewModel.onEmailChange(it) },
                            label = { Text(stringResource(R.string.leak_field_multi_email), color = TextGray) },
                            placeholder = { Text(stringResource(R.string.leak_placeholder_multi_email), color = TextGray.copy(alpha = 0.5f)) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 1,
                            maxLines = 6,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF6C8CFF),
                                unfocusedBorderColor = TextGray
                            ),
                            trailingIcon = {
                                if (viewModel.inputEmails.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onEmailChange("") }) {
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
                            if (detectedCount > 0) {
                                Surface(
                                    color = Color(0xFF6C8CFF).copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        stringResource(R.string.leak_emails_detected, detectedCount),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        color = Color(0xFF6C8CFF),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Spacer(Modifier.width(1.dp))
                            }
                            
                            TextButton(
                                onClick = { viewModel.checkMyEmail() },
                                colors = ButtonDefaults.buttonColors(contentColor = Color(0xFF6C8CFF), containerColor = Color.Transparent)
                            ) {
                                Text(stringResource(R.string.leak_btn_check_my_email), fontSize = 12.sp)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { filePickerLauncher.launch("*/*") }, // CSV/TXT
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, Color(0xFF6C8CFF)),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF6C8CFF))
                            ) {
                                Icon(AppIcons.folder(), null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.leak_btn_upload_csv))
                            }

                            Button(
                                onClick = { viewModel.startManualCheck() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C8CFF)),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !viewModel.isScanning && detectedCount > 0
                            ) {
                                if (viewModel.isScanning) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Text(stringResource(R.string.blacklist_btn_check), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        
                        Text(
                            text = stringResource(R.string.leak_privacy_disclaimer),
                            color = TextGray,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // Progress Bar
            if (viewModel.isScanning) {
                item {
                    val current = viewModel.scanProgress.first
                    val total = viewModel.scanProgress.second
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SoftDarkBg),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    stringResource(R.string.leak_checking_progress, current, total, viewModel.breachedSoFar),
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                                Text("${(current.toFloat() / total * 100).toInt()}%", fontSize = 12.sp, color = Color(0xFF6C8CFF), fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { if (total > 0) current.toFloat() / total else 0f },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                                color = Color(0xFF6C8CFF),
                                trackColor = Color.White.copy(alpha = 0.05f)
                            )
                        }
                    }
                }
            }

            // 2. Results Toolbar
            if (viewModel.batchResults.isNotEmpty() && !viewModel.isScanning) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Scan Results", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                        Row {
                            IconButton(onClick = { csvExportLauncher.launch("leak_results_${System.currentTimeMillis()}.csv") }) {
                                Icon(AppIcons.download(), null, tint = Color(0xFF6C8CFF))
                            }
                            IconButton(onClick = { /* Share summary */ }) {
                                Icon(AppIcons.share(), null, tint = Color(0xFF6C8CFF))
                            }
                        }
                    }
                }

                // 3. Summary Card (Batch)
                item {
                    val total = viewModel.batchResults.size
                    val breachedCount = viewModel.batchResults.count { it.isBreached }
                    val cleanCount = total - breachedCount
                    val isBreached = breachedCount > 0
                    
                    val statusColor = if (isBreached) Color(0xFFEF4444) else Color(0xFF10B981)
                    val statusIcon = if (isBreached) AppIcons.error() else AppIcons.checkCircle()
                    val statusText = if (isBreached) "BREACHED" else "CLEAN"
                    val statusDesc = if (isBreached) stringResource(R.string.leak_batch_breached_desc, breachedCount, total) else stringResource(R.string.leak_batch_clean_desc, total, total)
                    
                    val avgRisk = if (total > 0) viewModel.batchResults.sumOf { it.riskScore } / total else 0

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
                                    Text(text = statusDesc, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            
                            // Average Risk Score
                            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Average Risk Score", fontSize = 10.sp, color = TextGray)
                                    Text("$avgRisk/100", fontSize = 10.sp, color = statusColor, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { avgRisk / 100f },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                                    color = statusColor,
                                    trackColor = Color.White.copy(alpha = 0.05f)
                                )
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                    }
                }
                
                // 4. Summary Chips
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val total = viewModel.batchResults.size
                        val breachedCount = viewModel.batchResults.count { it.isBreached }
                        val cleanCount = total - breachedCount
                        
                        LeakSummaryStatusChip("All($total)", Color(0xFF6C8CFF), true)
                        LeakSummaryStatusChip("$breachedCount Breached", if(breachedCount > 0) Color(0xFFEF4444) else TextGray, breachedCount > 0)
                        LeakSummaryStatusChip("$cleanCount Clean", Color(0xFF10B981), cleanCount > 0)
                    }
                }
                
                // 5. Result List
                items(viewModel.batchResults) { res ->
                    BatchResultItem(res)
                }
            }
            
            item { Spacer(Modifier.height(40.dp)) }
        }
    }

    // CSV Preview BottomSheet
    if (viewModel.showCsvPreview) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.showCsvPreview = false },
            containerColor = SoftDarkBg,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.leak_csv_preview_title, viewModel.csvPreviewList.size), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(stringResource(R.string.leak_csv_preview_desc, viewModel.csvInvalidCount, 5.coerceAtMost(viewModel.csvPreviewList.size)), color = TextGray, fontSize = 12.sp)
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    viewModel.csvPreviewList.take(5).forEach { csv ->
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(AppIcons.person(), null, tint = Color(0xFF6C8CFF), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(csv.email, color = Color.White, fontSize = 13.sp)
                            if (csv.name != null) {
                                Text(" • ${csv.name}", color = TextGray, fontSize = 13.sp)
                            }
                        }
                    }
                }
                
                Text(stringResource(R.string.leak_offline_disclaimer), color = TextGray, fontSize = 11.sp)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.showCsvPreview = false },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.btn_cancel))
                    }
                    Button(
                        onClick = { 
                            viewModel.startBatchCheck(viewModel.csvPreviewList)
                            viewModel.showCsvPreview = false 
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C8CFF))
                    ) {
                        Text("CHECK ALL")
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun BatchResultItem(res: LeakCheckResult) {
    var expanded by remember { mutableStateOf(false) }
    val statusColor = if (res.isBreached) Color(0xFFEF4444) else Color(0xFF10B981)
    
    Card(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = SoftDarkBg),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(statusColor))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(res.email, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    val detail = if (res.isBreached) "BREACHED (${res.breaches.size} breaches)" else "CLEAN"
                    val deptInfo = if (res.dept != null) " • Dept: ${res.dept}" else ""
                    Text("$detail$deptInfo", fontSize = 11.sp, color = TextGray)
                }
                
                Icon(
                    painter = if (expanded) AppIcons.keyboardArrowUp() else AppIcons.keyboardArrowDown(),
                    contentDescription = null,
                    tint = TextGray,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            if (expanded && res.isBreached) {
                Spacer(Modifier.height(12.dp))
                res.breaches.forEach { breach ->
                    Column(modifier = Modifier.padding(start = 22.dp, bottom = 8.dp)) {
                        Text("• ${breach.name} (${breach.year})", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text("  Data: ${breach.data}", color = TextGray, fontSize = 10.sp)
                    }
                }
                Text(
                    text = stringResource(R.string.leak_advice),
                    color = Color(0xFF6C8CFF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 22.dp)
                )
            }
        }
    }
}

@Composable
private fun LeakSummaryStatusChip(label: String, color: Color, isHighlighted: Boolean) {
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
