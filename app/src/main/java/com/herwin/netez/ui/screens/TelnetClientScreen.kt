package com.herwin.netez.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.theme.PremiumNavy
import com.herwin.netez.ui.theme.SoftDarkBg
import com.herwin.netez.ui.theme.TextGray

@Composable
fun TelnetClientScreen() {
    var isConnected by remember { mutableStateOf(false) }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("23") }
    
    val terminalOutput = remember { mutableStateListOf<String>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PremiumNavy)
    ) {
        if (!isConnected) {
            TelnetConnectionForm(
                host = host, onHostChange = { host = it },
                port = port, onPortChange = { port = it },
                onConnect = { 
                    isConnected = true
                    terminalOutput.add("Connected to $host:$port")
                    terminalOutput.add("Simulation: Welcome to Telnet Server")
                }
            )
        } else {
            TelnetTerminalView(
                output = terminalOutput,
                onSendCommand = { cmd -> 
                    terminalOutput.add("> $cmd")
                    terminalOutput.add("Server response: command accepted.")
                },
                onDisconnect = { isConnected = false }
            )
        }
    }
}

@Composable
fun TelnetConnectionForm(
    host: String, onHostChange: (String) -> Unit,
    port: String, onPortChange: (String) -> Unit,
    onConnect: () -> Unit
) {
    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(shape = RoundedCornerShape(24.dp), color = SoftDarkBg) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Telnet Session", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                
                OutlinedTextField(
                    value = host, onValueChange = onHostChange,
                    label = { Text("Host / IP", color = TextGray) }, 
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF6C8CFF),
                        unfocusedBorderColor = TextGray
                    )
                )
                
                OutlinedTextField(
                    value = port, onValueChange = onPortChange,
                    label = { Text("Port", color = TextGray) }, 
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF6C8CFF),
                        unfocusedBorderColor = TextGray
                    )
                )
                
                Button(
                    onClick = onConnect,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C8CFF))
                ) {
                    Text("CONNECT", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
fun TelnetTerminalView(output: List<String>, onSendCommand: (String) -> Unit, onDisconnect: () -> Unit) {
    var command by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(output) { line ->
                Text(line, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().background(SoftDarkBg).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = command, onValueChange = { command = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type command...", color = TextGray) },
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedTextColor = Color.White,
                    focusedTextColor = Color.White,
                    cursorColor = Color.Cyan
                )
            )
            IconButton(onClick = { if (command.isNotBlank()) { onSendCommand(command); command = "" } }) {
                Icon(AppIcons.send(), contentDescription = "Send", tint = Color.Cyan)
            }
            IconButton(onClick = onDisconnect) {
                Icon(AppIcons.powerSettingsNew(), contentDescription = "Disconnect", tint = Color.Red)
            }
        }
    }
}
