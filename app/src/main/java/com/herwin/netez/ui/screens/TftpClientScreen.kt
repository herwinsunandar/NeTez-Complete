package com.herwin.netez.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.herwin.netez.ui.theme.PremiumNavy
import com.herwin.netez.ui.theme.SoftDarkBg
import com.herwin.netez.ui.theme.TextGray

@Composable
fun TftpClientScreen() {
    var host by remember { mutableStateOf("") }
    var filename by remember { mutableStateOf("") }
    var isTransferring by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PremiumNavy)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Surface(shape = RoundedCornerShape(24.dp), color = SoftDarkBg) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("TFTP Transfer", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                
                OutlinedTextField(
                    value = host, onValueChange = { host = it },
                    label = { Text("Server IP") }, modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = filename, onValueChange = { filename = it },
                    label = { Text("File Name") }, modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { isTransferring = true },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !isTransferring
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("GET")
                    }
                    Button(
                        onClick = { isTransferring = true },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !isTransferring
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("PUT")
                    }
                }
            }
        }

        if (isTransferring) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Transferring $filename...", color = Color.White)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = Color(0xFF3B82F6)
                )
                TextButton(onClick = { isTransferring = false; progress = 0f }) {
                    Text("CANCEL", color = Color.Red)
                }
            }
            
            // Simulating progress
            LaunchedEffect(isTransferring) {
                if (isTransferring) {
                    for (i in 1..10) {
                        kotlinx.coroutines.delay(500)
                        progress = i / 10f
                    }
                    isTransferring = false
                    progress = 0f
                }
            }
        }
    }
}
