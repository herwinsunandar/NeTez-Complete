package com.herwin.netez.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.network.TftpMode
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.theme.PremiumNavy
import com.herwin.netez.ui.theme.SoftDarkBg
import com.herwin.netez.ui.theme.TextGray
import com.herwin.netez.ui.viewmodels.TftpHistoryItem
import com.herwin.netez.ui.viewmodels.TftpViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TftpClientScreen(viewModel: TftpViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showHistory by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val tempFile = File(context.cacheDir, "tftp_upload")
            context.contentResolver.openInputStream(it)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            }
            viewModel.updateLocalPath(tempFile.absolutePath)
        }
    }

    Scaffold(
        containerColor = PremiumNavy
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Surface(shape = RoundedCornerShape(24.dp), color = SoftDarkBg, tonalElevation = 2.dp) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = uiState.serverIp,
                            onValueChange = { viewModel.updateServerIp(it) },
                            label = { Text("Server IP") },
                            modifier = Modifier.weight(0.7f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        OutlinedTextField(
                            value = uiState.port,
                            onValueChange = { viewModel.updatePort(it) },
                            label = { Text("Port") },
                            modifier = Modifier.weight(0.3f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }

                    OutlinedTextField(
                        value = uiState.remoteFileName,
                        onValueChange = { viewModel.updateRemoteFileName(it) },
                        label = { Text("Remote File Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = uiState.localPath,
                        onValueChange = { viewModel.updateLocalPath(it) },
                        label = { Text("Local Path") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        trailingIcon = {
                            IconButton(onClick = { filePicker.launch("*/*") }) {
                                Icon(AppIcons.folderOpen(), contentDescription = "Browse", tint = Color(0xFF6C8CFF))
                            }
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().background(Color(0xFF2A2E45), RoundedCornerShape(12.dp)).padding(4.dp)
                    ) {
                        TftpModeTab("OCTET", uiState.mode == TftpMode.OCTET, Modifier.weight(1f)) { viewModel.updateMode(TftpMode.OCTET) }
                        TftpModeTab("NETASCII", uiState.mode == TftpMode.NETASCII, Modifier.weight(1f)) { viewModel.updateMode(TftpMode.NETASCII) }
                    }

                    if (uiState.isTransferring) {
                        LinearProgressIndicator(
                            progress = { uiState.progress },
                            modifier = Modifier.fillMaxWidth().clip(CircleShape),
                            color = Color(0xFF6C8CFF),
                            trackColor = Color.White.copy(alpha = 0.05f)
                        )
                    }

                    Surface(
                        color = Color(0xFF0A0E1A),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(80.dp)
                    ) {
                        Text(
                            text = uiState.log,
                            modifier = Modifier.padding(8.dp),
                            color = Color(0xFF8A8E9A),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 14.sp
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { viewModel.startTransfer(isGet = true) },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5B8DEF)),
                            enabled = !uiState.isTransferring
                        ) {
                            Icon(AppIcons.download(), contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("GET", fontWeight = FontWeight.ExtraBold)
                        }
                        Button(
                            onClick = { viewModel.startTransfer(isGet = false) },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5B8DEF)),
                            enabled = !uiState.isTransferring
                        ) {
                            Icon(AppIcons.upload(), contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("PUT", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
    }

    if (showHistory) {
        ModalBottomSheet(onDismissRequest = { showHistory = false }, containerColor = SoftDarkBg) {
            TftpHistorySheet(uiState.history)
        }
    }

    if (uiState.error != null) {
        LaunchedEffect(uiState.error) {
            Toast.makeText(context, uiState.error, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }
}

@Composable
fun TftpModeTab(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color(0xFF6C8CFF) else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) Color.White else TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TftpHistorySheet(history: List<TftpHistoryItem>) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 32.dp)) {
        Text("Recent Transfers", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        if (history.isEmpty()) {
            Text("No history yet", color = TextGray, modifier = Modifier.padding(vertical = 16.dp))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(history) { item ->
                    TftpHistoryCard(item)
                }
            }
        }
    }
}

@Composable
fun TftpHistoryCard(item: TftpHistoryItem) {
    Surface(color = Color.White.copy(alpha = 0.03f), shape = RoundedCornerShape(12.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (item.type == "GET") AppIcons.download() else AppIcons.upload(),
                contentDescription = null,
                tint = if (item.status == "SUCCESS") Color(0xFF10B981) else Color(0xFFEF4444),
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.file, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(item.host, color = TextGray, fontSize = 12.sp)
            }
            Text(
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(item.timestamp)),
                color = TextGray,
                fontSize = 11.sp
            )
        }
    }
}
