package com.herwin.netez.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.ui.theme.SoftDarkBg
import com.herwin.netez.ui.theme.TextGray
import com.herwin.netez.ui.viewmodels.SshViewModel

@Composable
fun SshClientScreen(viewModel: SshViewModel = viewModel()) {
    val isConnected by viewModel.isConnected.collectAsState()
    var host by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("22") }

    if (!isConnected) {
        SshConnectionForm(
            host = host, onHostChange = { host = it },
            user = user, onUserChange = { user = it },
            password = password, onPasswordChange = { password = it },
            port = port, onPortChange = { port = it },
            onConnect = { 
                viewModel.connect(host, user, password, port.toIntOrNull() ?: 22)
            }
        )
    } else {
        SshTerminalView(
            output = viewModel.output,
            onSendCommand = { viewModel.sendCommand(it) },
            onDisconnect = { viewModel.disconnect() }
        )
    }
}

@Composable
fun SshConnectionForm(
    host: String, onHostChange: (String) -> Unit,
    user: String, onUserChange: (String) -> Unit,
    password: String, onPasswordChange: (String) -> Unit,
    port: String, onPortChange: (String) -> Unit,
    onConnect: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("New SSH Session", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                
                OutlinedTextField(
                    value = host, onValueChange = onHostChange,
                    label = { Text("Host / IP") }, modifier = Modifier.fillMaxWidth()
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = user, onValueChange = onUserChange,
                        label = { Text("Username") }, modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = port, onValueChange = onPortChange,
                        label = { Text("Port") }, modifier = Modifier.width(80.dp)
                    )
                }
                
                OutlinedTextField(
                    value = password, onValueChange = onPasswordChange,
                    label = { Text("Password") }, modifier = Modifier.fillMaxWidth(),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                )
                
                Spacer(Modifier.height(12.dp))
                
                Button(
                    onClick = onConnect,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("CONNECT", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
fun SshTerminalView(output: List<String>, onSendCommand: (String) -> Unit, onDisconnect: () -> Unit) {
    var command by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(output) { line ->
                Text(line, color = Color(0xFF00FF00), fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
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
                    focusedTextColor = Color.White
                )
            )
            IconButton(onClick = { if (command.isNotBlank()) { onSendCommand(command); command = "" } }) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
            }
            IconButton(onClick = onDisconnect) {
                Icon(Icons.Default.PowerSettingsNew, contentDescription = "Disconnect", tint = Color.Red)
            }
        }
    }
}
