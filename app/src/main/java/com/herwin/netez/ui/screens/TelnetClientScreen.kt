package com.herwin.netez.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.theme.TechMonospace
import com.herwin.netez.ui.viewmodels.TelnetViewModel

val TelnetBg = Color(0xFF0F1220)
val TelnetCard = Color(0xFF1E2235)
val TelnetAccent = Color(0xFF3D5AFE)
val TelnetTermBg = Color(0xFF0A0C14)
val TelnetTermGreen = Color(0xFF00FF00)

@Composable
fun TelnetClientScreen(
    viewModel: TelnetViewModel = viewModel()
) {
    val context = LocalContext.current
    val isConnected by viewModel.isConnected.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val terminalOutput by viewModel.terminalOutput.collectAsState()

    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("23") }

    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.errorMessage = null
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = TelnetBg) {
        if (!isConnected) {
            TelnetConnectionForm(
                host = host,
                onHostChange = { host = it },
                port = port,
                onPortChange = { port = it },
                connectionState = connectionState,
                onConnect = { viewModel.connect(host, port) }
            )
        } else {
            TelnetTerminalView(
                output = terminalOutput,
                connectionState = connectionState,
                onSendCommand = { viewModel.sendCommand(it) },
                onSpecialKey = { key, typedText -> viewModel.sendSpecialKey(key, typedText) },
                onDisconnect = { viewModel.disconnect() }
            )
        }
    }
}

@Composable
fun TelnetConnectionForm(
    host: String,
    onHostChange: (String) -> Unit,
    port: String,
    onPortChange: (String) -> Unit,
    connectionState: String,
    onConnect: () -> Unit
) {
    val isConnecting = connectionState == "Connecting..."

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = TelnetCard,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(TelnetAccent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(AppIcons.terminal(), contentDescription = null, tint = TelnetAccent)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Telnet TCP Session",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Native 100% TCP Socket Telnet Engine",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                OutlinedTextField(
                    value = host,
                    onValueChange = onHostChange,
                    label = { Text("Host / IP Address", color = Color.Gray) },
                    placeholder = { Text("e.g. 192.168.1.1 or mail.domain.com", color = Color.DarkGray) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = TelnetAccent,
                        unfocusedBorderColor = Color.DarkGray
                    )
                )

                OutlinedTextField(
                    value = port,
                    onValueChange = onPortChange,
                    label = { Text("Port (Default 23)", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = TelnetAccent,
                        unfocusedBorderColor = Color.DarkGray
                    )
                )

                Button(
                    onClick = onConnect,
                    enabled = !isConnecting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TelnetAccent)
                ) {
                    if (isConnecting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("CONNECT TO TELNET SOCKET", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun TelnetTerminalView(
    output: AnnotatedString,
    connectionState: String,
    onSendCommand: (String) -> Unit,
    onSpecialKey: (String, String) -> Unit,
    onDisconnect: () -> Unit
) {
    var command by remember { mutableStateOf("") }
    var fontSizeSp by remember { mutableIntStateOf(13) }
    val scrollState = rememberScrollState()

    LaunchedEffect(output) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(modifier = Modifier.fillMaxSize().background(TelnetTermBg).imePadding()) {
        // Top Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(TelnetCard)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(if (connectionState == "Connected") TelnetTermGreen else Color.Red, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(connectionState, color = Color.White, fontSize = 12.sp)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = { if (fontSizeSp > 9) fontSizeSp-- },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Text("A-", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Text("${fontSizeSp}sp", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                TextButton(
                    onClick = { if (fontSizeSp < 22) fontSizeSp++ },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Text("A+", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onDisconnect, modifier = Modifier.size(24.dp)) {
                    Icon(AppIcons.powerSettingsNew(), contentDescription = "Disconnect", tint = Color.Red)
                }
            }
        }

        // Terminal Output Screen
        SelectionContainer(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(8.dp)
                .verticalScroll(scrollState)
        ) {
            Text(
                text = output,
                fontFamily = FontFamily.Monospace,
                fontSize = fontSizeSp.sp,
                lineHeight = (fontSizeSp + 3).sp,
                color = Color.White
            )
        }

        // Special Keys Toolbar (Pill Chips)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(TelnetCard)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val keys = listOf("▲ UP", "▼ DOWN", "ENTER ↵", "TAB", "PIPE", "ESC", "CTRL-C", "CTRL-D", "CTRL-Z", "CTRL-L")
            items(keys) { key ->
                Surface(
                    onClick = {
                        onSpecialKey(key, command)
                        if (key == "TAB" || key == "▲ UP" || key == "▼ DOWN" || key == "ENTER ↵") {
                            command = ""
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    color = if (key == "ENTER ↵") TelnetAccent else TelnetCard,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Text(
                        text = key,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Bottom Command Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(TelnetTermBg)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = command,
                onValueChange = { command = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type Telnet command...", color = Color.DarkGray) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = TelnetTermGreen,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    onSendCommand(command)
                    command = ""
                })
            )
            IconButton(onClick = {
                onSendCommand(command)
                command = ""
            }) {
                Icon(AppIcons.arrowForward(), contentDescription = "Execute Command", tint = TelnetTermGreen)
            }
        }
    }
}
