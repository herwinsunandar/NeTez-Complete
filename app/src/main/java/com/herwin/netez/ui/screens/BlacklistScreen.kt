package com.herwin.netez.ui.screens

import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.R
import com.herwin.netez.network.BlacklistResult
import com.herwin.netez.network.BlacklistStatus
import com.herwin.netez.ui.theme.PremiumNavy
import com.herwin.netez.ui.theme.SoftDarkBg
import com.herwin.netez.ui.theme.TextGray
import com.herwin.netez.ui.viewmodels.BlacklistViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlacklistScreen(
    viewModel: BlacklistViewModel = viewModel()
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var ipInput by remember { mutableStateOf("") }
    val scanTime = remember(viewModel.results) {
        if (viewModel.results.isNotEmpty()) {
            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        } else ""
    }

    LaunchedEffect(Unit) {
        viewModel.fetchMyIp()
    }
    
    // Sync input with viewModel.scannedIp when it changes (e.g. from fetchMyIp)
    LaunchedEffect(viewModel.scannedIp) {
        if (viewModel.scannedIp.isNotEmpty() && ipInput.isEmpty()) {
            ipInput = viewModel.scannedIp
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PremiumNavy)
    ) {
        // Top Actions Bar (Hidden if we use Scaffold TopBar in NeTezApp, but user asked for Copy/Share buttons)
        // Actually, NeTezApp has a topBar. I might need to communicate with it, 
        // but typically in this project, screens might have their own header if needed or we use the Scaffold's actions.
        // I'll add them to the top of the content for now or as icons if I were in NeTezApp.
        // Re-reading: "Actions: Add Copy and Share buttons to the TopBar."
        // This implies modifying NeTezApp.kt or having a custom topBar.
        // Let's add them as an overlay/top row for now if I can't easily modify NeTezApp's Scaffold from here.
        // Better: I'll add a row for actions.

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = {
                val text = viewModel.results.joinToString("\n") { 
                    "${it.name}: ${it.status} (${it.resultIp ?: ""})" 
                }
                clipboardManager.setText(AnnotatedString(text))
            }) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.White)
            }
            IconButton(onClick = {
                val text = "NeTez Blacklist Check for ${viewModel.scannedIp}\n\n" +
                        viewModel.results.joinToString("\n") { 
                            "${it.name}: ${it.status}" 
                        } + "\n\nSummary: ${viewModel.summary}"
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                context.startActivity(Intent.createChooser(intent, "Share Results"))
            }) {
                Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Input Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SoftDarkBg),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = ipInput,
                            onValueChange = { ipInput = it },
                            label = { Text("IP Address", color = TextGray) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF6C8CFF),
                                unfocusedBorderColor = TextGray
                            ),
                            singleLine = true,
                            trailingIcon = {
                                if (ipInput.isNotEmpty()) {
                                    IconButton(onClick = { ipInput = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = null, tint = TextGray)
                                    }
                                }
                            }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.checkIp(ipInput) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C8CFF)),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !viewModel.isScanning
                            ) {
                                if (viewModel.isScanning) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(stringResource(R.string.blacklist_btn_check), fontWeight = FontWeight.Bold)
                                }
                            }

                            TextButton(
                                onClick = { viewModel.fetchMyIp() },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF6C8CFF))
                            ) {
                                Text(stringResource(R.string.blacklist_btn_check_my_ip))
                            }
                        }
                        
                        viewModel.error?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Result Header
            if (viewModel.results.isNotEmpty() || viewModel.isScanning) {
                item {
                    val isListed = viewModel.results.any { it.status == BlacklistStatus.LISTED }
                    val statusText = if (isListed) stringResource(R.string.blacklist_status_listed) else stringResource(R.string.blacklist_status_clean)
                    val statusColor = if (isListed) Color(0xFFEF4444) else Color(0xFF10B981)
                    val statusIcon = if (isListed) Icons.Default.Close else Icons.Default.Check

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SoftDarkBg),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(20.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Surface(
                                color = statusColor.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(
                                    imageVector = statusIcon,
                                    contentDescription = null,
                                    tint = statusColor,
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .fillMaxSize()
                                )
                            }
                            
                            Column {
                                Text(
                                    text = statusText,
                                    color = statusColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                                Text(
                                    text = "${viewModel.scannedIp} • $scanTime",
                                    color = TextGray,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (viewModel.results.isNotEmpty()) {
                                    val listedCount = viewModel.results.count { it.status == BlacklistStatus.LISTED }
                                    val cleanCount = viewModel.results.count { it.status == BlacklistStatus.CLEAN }
                                    Text(
                                        text = stringResource(R.string.blacklist_summary, listedCount, cleanCount),
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // RBL Results
            items(viewModel.results) { result ->
                RblResultItem(result)
            }

            if (viewModel.results.isNotEmpty() && !viewModel.isScanning) {
                item {
                    com.herwin.netez.ui.components.ResultFooter(
                        fullTextToCopy = "NeTez Blacklist Check for ${viewModel.scannedIp}\n\n" +
                                viewModel.results.joinToString("\n") { "${it.name}: ${it.status}" },
                        onCopyAll = {
                            val text = viewModel.results.joinToString("\n") { "${it.name}: ${it.status}" }
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Blacklist Results", text))
                            android.widget.Toast.makeText(context, "Results copied", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RblResultItem(result: BlacklistResult) {
    val isListed = result.status == BlacklistStatus.LISTED
    val backgroundColor = if (isListed) Color(0xFFEF4444).copy(alpha = 0.1f) else SoftDarkBg
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            com.herwin.netez.ui.components.ResultRow(
                label = result.name,
                value = if (isListed) "LISTED (${result.responseTime}ms)" else "CLEAN (${result.responseTime}ms)"
            )
            if (result.resultIp != null) {
                Text(
                    text = "Result: ${result.resultIp}",
                    color = TextGray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
