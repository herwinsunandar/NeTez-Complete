package com.herwin.netez.ui.screens

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.R
import com.herwin.netez.data.entities.SslDomain
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.components.SslDomainDetailBottomSheet
import com.herwin.netez.ui.theme.PremiumNavy
import com.herwin.netez.ui.theme.SoftDarkBg
import com.herwin.netez.ui.theme.TextGray
import com.herwin.netez.ui.viewmodels.SslReminderViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SslReminderScreen(
    onBack: () -> Unit,
    viewModel: SslReminderViewModel = viewModel()
) {
    val context = LocalContext.current
    val domains by viewModel.domains.collectAsState()
    var showBulkDialog by remember { mutableStateOf(false) }
    var selectedDomain by remember { mutableStateOf<SslDomain?>(null) }

    val filteredDomains = when (viewModel.filterState) {
        "Expiring" -> domains.filter { it.daysLeft in 1..14 }
        "Expired" -> domains.filter { it.daysLeft <= 0 }
        "Valid" -> domains.filter { it.daysLeft > 14 }
        else -> domains
    }

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
                            placeholder = { Text(stringResource(R.string.ssl_domain_placeholder), color = TextGray.copy(alpha = 0.5f)) },
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
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { showBulkDialog = true },
                                colors = ButtonDefaults.buttonColors(contentColor = Color(0xFF6C8CFF), containerColor = Color.Transparent)
                            ) {
                                Text(stringResource(R.string.ssl_btn_bulk_add), fontSize = 12.sp)
                            }
                            
                            Spacer(Modifier.width(8.dp))

                            Button(
                                onClick = { viewModel.addDomain(viewModel.inputQuery) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C8CFF)),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !viewModel.isScanning
                            ) {
                                if (viewModel.isScanning) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Text(stringResource(R.string.btn_add).uppercase(), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        
                        if (viewModel.isScanning && viewModel.scanProgress.second > 0) {
                            LinearProgressIndicator(
                                progress = { viewModel.scanProgress.first.toFloat() / viewModel.scanProgress.second },
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                color = Color(0xFF6C8CFF),
                                trackColor = Color.White.copy(alpha = 0.1f)
                            )
                            Text(
                                stringResource(R.string.ssl_check_progress, viewModel.scanProgress.first, viewModel.scanProgress.second),
                                color = TextGray,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // 2. Results Header
            if (domains.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.ssl_monitored_domains), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                        Row {
                            IconButton(onClick = { /* Copy all */ }) {
                                Icon(AppIcons.contentCopy(), null, tint = Color(0xFF6C8CFF), modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { /* Share all */ }) {
                                Icon(AppIcons.share(), null, tint = Color(0xFF6C8CFF), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                // 3. Filter Chips
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChipItem("All", domains.size, viewModel.filterState == "All") { viewModel.filterState = "All" }
                        FilterChipItem("Expiring", domains.count { it.daysLeft in 1..14 }, viewModel.filterState == "Expiring", Color(0xFFF59E0B)) { viewModel.filterState = "Expiring" }
                        FilterChipItem("Expired", domains.count { it.daysLeft <= 0 }, viewModel.filterState == "Expired", Color(0xFFEF4444)) { viewModel.filterState = "Expired" }
                        FilterChipItem("Valid", domains.count { it.daysLeft > 14 }, viewModel.filterState == "Valid", Color(0xFF10B981)) { viewModel.filterState = "Valid" }
                    }
                }

                // 4. Domain List
                items(filteredDomains) { domain ->
                    SslDomainCard(domain, onClick = { selectedDomain = domain })
                }
            }
        }
    }

    selectedDomain?.let { domain ->
        SslDomainDetailBottomSheet(
            domain = domain,
            onDismiss = { selectedDomain = null },
            onRecheck = { 
                viewModel.recheck(domain)
                selectedDomain = null
            },
            onDelete = {
                viewModel.deleteDomain(domain)
                selectedDomain = null
            }
        )
    }

    if (showBulkDialog) {
        BulkAddDialog(
            onDismiss = { showBulkDialog = false },
            onConfirm = { 
                viewModel.bulkAdd(it)
                showBulkDialog = false
            }
        )
    }
}

@Composable
fun FilterChipItem(
    label: String,
    count: Int,
    selected: Boolean,
    dotColor: Color? = null,
    onClick: () -> Unit
) {
    val containerColor = if (selected) Color(0xFF6C8CFF).copy(alpha = 0.2f) else SoftDarkBg
    val borderColor = if (selected) Color(0xFF6C8CFF) else Color.Transparent
    val textColor = if (selected) Color(0xFF6C8CFF) else TextGray

    Surface(
        onClick = onClick,
        color = containerColor,
        shape = RoundedCornerShape(20.dp),
        border = if (selected) BorderStroke(1.dp, borderColor) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (dotColor != null) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(dotColor))
                Spacer(Modifier.width(6.dp))
            }
            val labelText = when(label) {
                "All" -> stringResource(R.string.ssl_filter_all, count)
                "Expiring" -> stringResource(R.string.ssl_filter_expiring, count)
                "Expired" -> stringResource(R.string.ssl_filter_expired, count)
                "Valid" -> stringResource(R.string.ssl_filter_valid, count)
                else -> label
            }
            Text(labelText, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun SslDomainCard(
    domain: SslDomain,
    onClick: () -> Unit
) {
    val statusColor = when {
        domain.daysLeft <= 0 -> Color(0xFFEF4444)
        domain.daysLeft <= 14 -> Color(0xFFF59E0B)
        else -> Color(0xFF10B981)
    }
    
    val statusIcon = when {
        domain.daysLeft <= 0 -> AppIcons.close()
        domain.daysLeft <= 14 -> AppIcons.warning()
        else -> AppIcons.lock()
    }

    val chipText = when {
        domain.daysLeft <= 0 -> stringResource(R.string.ssl_chip_expired)
        domain.daysLeft <= 14 -> stringResource(R.string.ssl_chip_days_left, domain.daysLeft)
        else -> stringResource(R.string.ssl_chip_valid, domain.daysLeft)
    }

    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val lastCheckSdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = SoftDarkBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(statusColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(statusIcon, null, tint = statusColor, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(domain.domain, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Issuer: ${domain.issuer.substringBefore(',')} • Group: ${domain.groupName}", color = TextGray, fontSize = 11.sp)
                }
                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        chipText,
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
            Spacer(Modifier.height(12.dp))
            
            Text(
                stringResource(R.string.ssl_details_expires, sdf.format(Date(domain.expiryDate)), lastCheckSdf.format(Date(domain.lastChecked))),
                color = TextGray,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun BulkAddDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ssl_add_dialog_title), color = Color.White) },
        text = {
            Column {
                Text(stringResource(R.string.ssl_add_dialog_desc), color = TextGray, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF6C8CFF)
                    )
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(text) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C8CFF))) {
                Text(stringResource(R.string.btn_add).uppercase())
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel), color = TextGray)
            }
        },
        containerColor = SoftDarkBg
    )
}
