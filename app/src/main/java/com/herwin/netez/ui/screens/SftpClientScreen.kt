package com.herwin.netez.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.herwin.netez.ui.theme.PremiumNavy
import com.herwin.netez.ui.theme.SoftDarkBg
import com.herwin.netez.ui.theme.TextGray

@Composable
fun SftpClientScreen() {
    var isConnected by remember { mutableStateOf(false) }
    var host by remember { mutableStateOf("") }
    
    val files = remember { mutableStateListOf<SftpFile>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PremiumNavy)
    ) {
        if (!isConnected) {
            SftpConnectionForm(
                host = host,
                onHostChange = { host = it },
                onConnect = { 
                    isConnected = true
                    files.clear()
                    files.addAll(listOf(
                        SftpFile("config.cfg", "2 KB", false),
                        SftpFile("backup/", "0 B", true),
                        SftpFile("logs.txt", "45 KB", false)
                    ))
                }
            )
        } else {
            SftpExplorerView(
                host = host,
                files = files,
                onDisconnect = { isConnected = false }
            )
        }
    }
}

data class SftpFile(val name: String, val size: String, val isDirectory: Boolean)

@Composable
fun SftpConnectionForm(host: String, onHostChange: (String) -> Unit, onConnect: () -> Unit) {
    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(shape = RoundedCornerShape(24.dp), color = SoftDarkBg) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("SFTP Explorer", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                OutlinedTextField(value = host, onValueChange = onHostChange, label = { Text("SFTP Host") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = onConnect, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp)) {
                    Text("OPEN EXPLORER", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SftpExplorerView(host: String, files: List<SftpFile>, onDisconnect: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(
            title = { Text(host, fontSize = 14.sp) },
            actions = {
                IconButton(onClick = onDisconnect) { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color.Red) }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = SoftDarkBg, titleContentColor = Color.White)
        )
        
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(files) { file ->
                ListItem(
                    headlineContent = { Text(file.name, color = Color.White) },
                    supportingContent = { Text(file.size, color = TextGray) },
                    leadingContent = { 
                        Icon(
                            if (file.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                            contentDescription = null,
                            tint = if (file.isDirectory) Color(0xFFFBBF24) else Color(0xFF3B82F6)
                        )
                    },
                    trailingContent = {
                        IconButton(onClick = {}) { Icon(Icons.Default.Download, contentDescription = null, tint = TextGray) }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}
