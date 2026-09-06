package com.herwin.netez.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.data.AuthType
import com.herwin.netez.data.SshHost
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.viewmodels.SshViewModel
import com.herwin.netez.utils.EncryptionUtils
import com.herwin.netez.utils.TerminalUtils
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

val NocBg = Color(0xFF0F1220)
val NocCard = Color(0xFF1E2235)
val NocAccent = Color(0xFF3D5AFE)
val TermBg = Color(0xFF0A0C14)
val TermGreen = Color(0xFF00FF00)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SshClientScreen(
    viewModel: SshViewModel = viewModel(),
    initialIp: String = ""
) {
    val isConnected by viewModel.isConnected.collectAsState()
    val savedHosts by viewModel.savedHosts.collectAsState()
    val terminalOutput by viewModel.terminalOutput.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(initialIp.isNotEmpty()) }
    var hostToEdit by remember { mutableStateOf<SshHost?>(null) }
    var connectingHostId by remember { mutableLongStateOf(-1L) }

    LaunchedEffect(connectionState) {
        if (connectionState == "Connected" || connectionState.startsWith("Error")) {
            connectingHostId = -1L
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = NocBg) {
        if (!isConnected) {
            Box(modifier = Modifier.fillMaxSize()) {
                HostListView(
                    hosts = savedHosts,
                    connectingHostId = connectingHostId,
                    onConnect = { 
                        connectingHostId = it.id
                        viewModel.connect(it) 
                    },
                    onEdit = { hostToEdit = it },
                    onDelete = { viewModel.deleteHost(it) },
                    onAddClick = { showAddDialog = true }
                )
                
                if (showAddDialog || hostToEdit != null) {
                    AddHostDialog(
                        editHost = hostToEdit,
                        initialIp = if (hostToEdit == null) initialIp else "",
                        onDismiss = { 
                            showAddDialog = false
                            hostToEdit = null
                        },
                        onSave = { 
                            if (hostToEdit != null) viewModel.updateHost(it)
                            else viewModel.saveHost(it)
                            showAddDialog = false
                            hostToEdit = null
                        }
                    )
                }
            }
        } else {
            TerminalView(
                output = terminalOutput,
                connectionState = connectionState,
                onSendCommand = { viewModel.sendCommand(it) },
                onSpecialKey = { viewModel.sendSpecialKey(it) },
                onDisconnect = { viewModel.disconnect() }
            )
        }
    }
}

@Composable
fun HostListView(
    hosts: List<SshHost>,
    connectingHostId: Long,
    onConnect: (SshHost) -> Unit,
    onEdit: (SshHost) -> Unit,
    onDelete: (SshHost) -> Unit,
    onAddClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("SSH Connections", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
            IconButton(onClick = onAddClick, modifier = Modifier.background(NocAccent, CircleShape)) {
                Icon(AppIcons.add(), contentDescription = "Add", tint = Color.White)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (hosts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hosts saved. Tap + to add one.", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(hosts) { host ->
                    HostCard(
                        host = host,
                        isConnecting = host.id == connectingHostId,
                        onConnect = onConnect,
                        onEdit = onEdit,
                        onDelete = onDelete
                    )
                }
            }
        }
    }
}

@Composable
fun HostCard(
    host: SshHost,
    isConnecting: Boolean,
    onConnect: (SshHost) -> Unit,
    onEdit: (SshHost) -> Unit,
    onDelete: (SshHost) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { if (!isConnecting) onConnect(host) },
        colors = CardDefaults.cardColors(containerColor = NocCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).background(NocAccent.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isConnecting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = NocAccent, strokeWidth = 2.dp)
                } else {
                    Icon(AppIcons.terminal(), contentDescription = null, tint = NocAccent)
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(host.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                Text("${host.username}@${host.host}:${host.port}", color = Color.Gray, fontSize = 14.sp)
                if (host.tag != null) {
                    Text(host.tag, color = NocAccent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { if (!isConnecting) onConnect(host) },
                        enabled = !isConnecting
                    ) {
                        Icon(
                            painter = AppIcons.playArrow(),
                            contentDescription = "Connect",
                            tint = if (isConnecting) Color.Gray else TermGreen
                        )
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(AppIcons.moreVert(), contentDescription = "Menu", tint = Color.Gray)
                    }
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("Edit") }, onClick = { onEdit(host); showMenu = false })
                    DropdownMenuItem(text = { Text("[SFTP] (Soon)") }, onClick = { showMenu = false })
                    DropdownMenuItem(text = { Text("[Port Forward] (Soon)") }, onClick = { showMenu = false })
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text("Delete", color = Color.Red) }, onClick = { onDelete(host); showMenu = false })
                }
                
                if (host.lastConnected > 0) {
                    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                    Text(sdf.format(Date(host.lastConnected)), color = Color.Gray, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun AddHostDialog(
    editHost: SshHost? = null,
    initialIp: String = "",
    onDismiss: () -> Unit,
    onSave: (SshHost) -> Unit
) {
    var name by remember { mutableStateOf(editHost?.name ?: "") }
    var host by remember { mutableStateOf(editHost?.host ?: initialIp) }
    var port by remember { mutableStateOf(editHost?.port?.toString() ?: "22") }
    var user by remember { mutableStateOf(editHost?.username ?: "") }
    var authType by remember { mutableStateOf(editHost?.authType ?: AuthType.PASSWORD) }
    var password by remember { mutableStateOf("") } 
    var privateKeyPath by remember { mutableStateOf(editHost?.privateKeyPath) }
    var tag by remember { mutableStateOf(editHost?.tag ?: "") }
    
    LaunchedEffect(editHost) {
        if (editHost?.passwordEncrypted != null) {
            try {
                password = EncryptionUtils.decrypt(editHost.passwordEncrypted)
            } catch (e: Exception) {}
        }
    }
    
    val context = LocalContext.current
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val file = File(context.filesDir, "ssh_keys")
            if (!file.exists()) file.mkdirs()
            val destFile = File(file, "id_rsa_${System.currentTimeMillis()}")
            context.contentResolver.openInputStream(it)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            privateKeyPath = destFile.absolutePath
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            color = NocBg,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Text("Add New Host", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))
                
                HostTextField("Display Name (e.g. My Server)", name) { name = it }
                HostTextField("Host / IP Address", host) { host = it }
                HostTextField("Port", port, KeyboardType.Number) { port = it }
                HostTextField("Username", user) { user = it }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("Authentication", color = Color.Gray, fontSize = 14.sp)
                Row(modifier = Modifier.fillMaxWidth()) {
                    AuthTab("Password", authType == AuthType.PASSWORD, Modifier.weight(1f)) { authType = AuthType.PASSWORD }
                    AuthTab("Private Key", authType == AuthType.KEY, Modifier.weight(1f)) { authType = AuthType.KEY }
                }
                
                if (authType == AuthType.PASSWORD) {
                    HostTextField("Password / Passphrase", password, KeyboardType.Password) { password = it }
                } else {
                    Button(
                        onClick = { fileLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NocCard)
                    ) {
                        Icon(AppIcons.uploadFile(), contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (privateKeyPath != null) "Key Selected" else "Select Private Key File")
                    }
                    if (privateKeyPath != null) {
                        Text("File: ${privateKeyPath?.substringAfterLast("/")}", color = TermGreen, fontSize = 12.sp)
                    }
                    HostTextField("Passphrase (Optional)", password, KeyboardType.Password) { password = it }
                }
                
                HostTextField("Tag (e.g. Production, Home)", tag) { tag = it }
                
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(
                        onClick = {
                            if (name.isNotBlank() && host.isNotBlank() && user.isNotBlank()) {
                                onSave(SshHost(
                                    id = editHost?.id ?: 0,
                                    name = name,
                                    host = host,
                                    port = port.toIntOrNull() ?: 22,
                                    username = user,
                                    authType = authType,
                                    passwordEncrypted = if (password.isNotEmpty()) EncryptionUtils.encrypt(password) else null,
                                    privateKeyPath = privateKeyPath,
                                    tag = if (tag.isEmpty()) null else tag
                                ))
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = NocAccent)
                    ) {
                        Text(if (editHost != null) "Update Host" else "Save Host")
                    }
                }
            }
        }
    }
}

@Composable
fun HostTextField(label: String, value: String, keyboardType: KeyboardType = KeyboardType.Text, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color.Gray) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedTextColor = Color.White,
            focusedTextColor = Color.White,
            cursorColor = NocAccent,
            focusedBorderColor = NocAccent,
            unfocusedBorderColor = Color.DarkGray
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (keyboardType == KeyboardType.Password) PasswordVisualTransformation() else VisualTransformation.None
    )
}

@Composable
fun AuthTab(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .padding(4.dp)
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) NocAccent else NocCard)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) Color.White else Color.Gray, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalView(
    output: AnnotatedString,
    connectionState: String,
    onSendCommand: (String) -> Unit,
    onSpecialKey: (String) -> Unit,
    onDisconnect: () -> Unit
) {
    var command by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()
    var showSnippets by remember { mutableStateOf(false) }

    LaunchedEffect(output) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(modifier = Modifier.fillMaxSize().background(TermBg).imePadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().background(NocCard).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(if (connectionState == "Connected") TermGreen else Color.Red, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(connectionState, color = Color.White, fontSize = 12.sp)
            }
            IconButton(onClick = onDisconnect, modifier = Modifier.size(24.dp)) {
                Icon(AppIcons.close(), contentDescription = "Close", tint = Color.Gray)
            }
        }

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
                fontSize = 13.sp,
                lineHeight = 16.sp,
                color = Color.White
            )
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth().background(NocCard).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val keys = listOf("ESC", "TAB", "CTRL-C", "CTRL-D", "CTRL-Z")
            items(keys) { key ->
                TextButton(onClick = { onSpecialKey(key) }) {
                    Text(key, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            item {
                IconButton(onClick = { showSnippets = true }) {
                    Icon(AppIcons.list(), contentDescription = "Snippets", tint = TermGreen)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().background(TermBg).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = command,
                onValueChange = { command = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Enter command...", color = Color.DarkGray) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = TermGreen,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (command.isNotBlank()) { onSendCommand(command); command = "" } 
                })
            )
            IconButton(onClick = { if (command.isNotBlank()) { onSendCommand(command); command = "" } }) {
                Icon(AppIcons.send(), contentDescription = "Send", tint = TermGreen)
            }
        }
    }

    if (showSnippets) {
        ModalBottomSheet(onDismissRequest = { showSnippets = false }, containerColor = NocCard) {
            LazyColumn(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                item { Text("Common Snippets", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold) }
                items(TerminalUtils.commonSnippets) { snippet ->
                    ListItem(
                        headlineContent = { Text(snippet.command, color = TermGreen, fontFamily = FontFamily.Monospace) },
                        supportingContent = { Text(snippet.category, color = Color.Gray) },
                        modifier = Modifier.clickable { 
                            onSendCommand(snippet.command)
                            showSnippets = false
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
    }
}
