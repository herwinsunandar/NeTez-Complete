package com.herwin.netez.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.herwin.netez.ui.theme.TechMonospace
import com.herwin.netez.ui.viewmodels.TftpHistoryItem
import com.herwin.netez.ui.viewmodels.TftpViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

val TftpBg = Color(0xFF0F1220)
val TftpCard = Color(0xFF1E2235)
val TftpAccent = Color(0xFF3D5AFE)
val TextGray = Color(0xFF94A3B8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TftpClientScreen(
    onBack: () -> Unit = {},
    viewModel: TftpViewModel = viewModel()
) {
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
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "TFTP Client",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(AppIcons.arrowBack(), contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showHistory = true }) {
                        Icon(AppIcons.history(), contentDescription = "Transfer History", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TftpBg)
            )
        },
        containerColor = TftpBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = TftpCard,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = uiState.serverIp,
                            onValueChange = { viewModel.updateServerIp(it) },
                            label = { Text("Server IP", color = Color.Gray) },
                            placeholder = { Text("192.168.1.1", color = Color.DarkGray) },
                            modifier = Modifier.weight(0.7f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = TftpAccent,
                                unfocusedBorderColor = Color.DarkGray
                            )
                        )
                        OutlinedTextField(
                            value = uiState.port,
                            onValueChange = { viewModel.updatePort(it) },
                            label = { Text("Port", color = Color.Gray) },
                            modifier = Modifier.weight(0.3f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = TftpAccent,
                                unfocusedBorderColor = Color.DarkGray
                            )
                        )
                    }

                    OutlinedTextField(
                        value = uiState.remoteFileName,
                        onValueChange = { viewModel.updateRemoteFileName(it) },
                        label = { Text("Remote File Name", color = Color.Gray) },
                        placeholder = { Text("e.g. router_config.bin", color = Color.DarkGray) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = TftpAccent,
                            unfocusedBorderColor = Color.DarkGray
                        )
                    )

                    OutlinedTextField(
                        value = uiState.localPath,
                        onValueChange = { viewModel.updateLocalPath(it) },
                        label = { Text("Local File Path", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = TftpAccent,
                            unfocusedBorderColor = Color.DarkGray
                        ),
                        trailingIcon = {
                            IconButton(onClick = { filePicker.launch("*/*") }) {
                                Icon(AppIcons.folderOpen(), contentDescription = "Browse", tint = TftpAccent)
                            }
                        }
                    )

                    Text("Transfer Mode", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF2A2E45), RoundedCornerShape(12.dp))
                            .padding(4.dp)
                    ) {
                        TftpModeTab("OCTET (Binary)", uiState.mode == TftpMode.OCTET, Modifier.weight(1f)) { viewModel.updateMode(TftpMode.OCTET) }
                        TftpModeTab("NETASCII (Text)", uiState.mode == TftpMode.NETASCII, Modifier.weight(1f)) { viewModel.updateMode(TftpMode.NETASCII) }
                    }

                    if (uiState.isTransferring) {
                        LinearProgressIndicator(
                            progress = { uiState.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(CircleShape),
                            color = TftpAccent,
                            trackColor = Color.White.copy(alpha = 0.05f)
                        )
                    }

                    Surface(
                        color = Color(0xFF0A0E1A),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                    ) {
                        Text(
                            text = uiState.log,
                            modifier = Modifier.padding(10.dp),
                            color = Color(0xFF8A8E9A),
                            fontSize = 11.sp,
                            fontFamily = TechMonospace,
                            lineHeight = 15.sp
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { viewModel.startTransfer(isGet = true) },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TftpAccent),
                            enabled = !uiState.isTransferring
                        ) {
                            Icon(AppIcons.download(), contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("GET (DOWNLOAD)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Button(
                            onClick = { viewModel.startTransfer(isGet = false) },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            enabled = !uiState.isTransferring
                        ) {
                            Icon(AppIcons.upload(), contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("PUT (UPLOAD)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showHistory) {
        ModalBottomSheet(onDismissRequest = { showHistory = false }, containerColor = TftpCard) {
            TftpHistorySheet(
                history = uiState.history,
                onSelectHistory = { item ->
                    viewModel.selectHistoryItem(item)
                    showHistory = false
                }
            )
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
            .background(if (selected) TftpAccent else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) Color.White else TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TftpHistorySheet(
    history: List<TftpHistoryItem>,
    onSelectHistory: (TftpHistoryItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(20.dp).padding(bottom = 32.dp)) {
        Text("Recent Transfers History", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
        Text("Tap any history item to auto-fill transfer parameters", color = TextGray, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))
        if (history.isEmpty()) {
            Text("No transfer history recorded yet", color = TextGray, modifier = Modifier.padding(vertical = 16.dp))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(history) { item ->
                    TftpHistoryCard(item, onClick = { onSelectHistory(item) })
                }
            }
        }
    }
}

@Composable
fun TftpHistoryCard(
    item: TftpHistoryItem,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.White.copy(alpha = 0.04f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (item.type == "GET") AppIcons.download() else AppIcons.upload(),
                contentDescription = null,
                tint = if (item.status == "SUCCESS") Color(0xFF10B981) else Color(0xFFEF4444),
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.file, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = TechMonospace)
                Text("${item.type} • Server: ${item.host}", color = TextGray, fontSize = 12.sp)
            }
            Text(
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(item.timestamp)),
                color = TextGray,
                fontSize = 11.sp,
                fontFamily = TechMonospace
            )
        }
    }
}
