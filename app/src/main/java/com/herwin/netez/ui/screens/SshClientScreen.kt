package com.herwin.netez.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.data.AuthType
import com.herwin.netez.data.SshHost
import com.herwin.netez.entitlement.FeatureAccessManager
import com.herwin.netez.entitlement.FeatureAccessState
import com.herwin.netez.entitlement.FeatureId
import com.herwin.netez.network.SftpManager
import com.herwin.netez.network.SftpRemoteFile
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.components.ContextualPaywallDialog
import com.herwin.netez.ui.components.terminal.TerminalView
import com.herwin.netez.ui.viewmodels.SshViewModel
import com.herwin.netez.utils.EncryptionUtils
import com.herwin.netez.utils.TerminalUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val context = LocalContext.current
    val isConnected by viewModel.isConnected.collectAsState()
    val savedHosts by viewModel.savedHosts.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()

    var showAddDialog by remember { mutableStateOf(initialIp.isNotEmpty()) }
    var hostToEdit by remember { mutableStateOf<SshHost?>(null) }
    var hostToDelete by remember { mutableStateOf<SshHost?>(null) }
    var sftpHost by remember { mutableStateOf<SshHost?>(null) }
    var connectingHostId by remember { mutableLongStateOf(-1L) }
    var showPaywallDialog by remember { mutableStateOf(false) }

    if (showPaywallDialog) {
        ContextualPaywallDialog(
            featureId = FeatureId.SAVED_SSH_SERVERS_MAX,
            limitText = "Free plan supports up to 3 saved SSH/SFTP servers. Upgrade to NeTez PRO for unlimited saved server profiles.",
            onDismiss = { showPaywallDialog = false },
            onUpgradeClick = {}
        )
    }

    LaunchedEffect(connectionState) {
        if (connectionState != "Connecting..." && connectionState != "Authenticating...") {
            connectingHostId = -1L
        }
    }

    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.errorMessage = null
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
                    onDelete = { hostToDelete = it },
                    onOpenSftp = { sftpHost = it },
                    onAddClick = {
                        val accessManager = FeatureAccessManager.getInstance(context)
                        val state = accessManager.checkAccess(FeatureId.SAVED_SSH_SERVERS_MAX, savedHosts.size)
                        if (state == FeatureAccessState.LIMIT_REACHED) {
                            showPaywallDialog = true
                        } else {
                            showAddDialog = true
                        }
                    }
                )

                if (hostToDelete != null) {
                    val host = hostToDelete!!
                    AlertDialog(
                        onDismissRequest = { hostToDelete = null },
                        containerColor = MaterialTheme.colorScheme.surface,
                        title = {
                            Text(
                                text = "Delete Saved SSH Server?",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Text(
                                text = "Are you sure you want to delete '${host.name}' (${host.username}@${host.host}:${host.port})? Saved connection profile and credentials for this server will be permanently removed.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.deleteHost(host)
                                    hostToDelete = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Delete")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { hostToDelete = null }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                if (sftpHost != null) {
                    SftpExplorerBottomSheet(
                        host = sftpHost!!,
                        onDismiss = { sftpHost = null }
                    )
                }

                if (showAddDialog || hostToEdit != null) {
                    AddHostDialog(
                        editHost = hostToEdit,
                        initialIp = if (hostToEdit == null) initialIp else "",
                        onDismiss = {
                            showAddDialog = false
                            hostToEdit = null
                        },
                        onSave = { host ->
                            if (hostToEdit != null) {
                                viewModel.updateHost(host)
                            } else {
                                val accessManager = FeatureAccessManager.getInstance(context)
                                val state = accessManager.checkAccess(FeatureId.SAVED_SSH_SERVERS_MAX, savedHosts.size)
                                if (state == FeatureAccessState.LIMIT_REACHED) {
                                    showPaywallDialog = true
                                } else {
                                    viewModel.saveHost(host)
                                }
                            }
                            showAddDialog = false
                            hostToEdit = null
                        }
                    )
                }
            }
        } else {
            InteractiveTerminalScreen(
                viewModel = viewModel,
                connectionState = connectionState,
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
    onOpenSftp: (SshHost) -> Unit,
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
                        onDelete = onDelete,
                        onOpenSftp = onOpenSftp
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
    onDelete: (SshHost) -> Unit,
    onOpenSftp: (SshHost) -> Unit
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
                    DropdownMenuItem(text = { Text("Edit Profile") }, onClick = { onEdit(host); showMenu = false })
                    DropdownMenuItem(text = { Text("SFTP File Explorer 📁") }, onClick = { onOpenSftp(host); showMenu = false })
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text("Delete Host", color = Color.Red) }, onClick = { onDelete(host); showMenu = false })
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
                Text(if (editHost != null) "Edit SSH Host" else "Add New SSH Host", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
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
fun HostTextField(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }

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
        visualTransformation = if (keyboardType == KeyboardType.Password) {
            if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        trailingIcon = if (keyboardType == KeyboardType.Password) {
            {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        painter = if (passwordVisible) AppIcons.visibilityOff() else AppIcons.visibility(),
                        contentDescription = if (passwordVisible) "Hide Password" else "Show Password",
                        tint = Color.Gray
                    )
                }
            }
        } else null
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
fun InteractiveTerminalScreen(
    viewModel: SshViewModel,
    connectionState: String,
    onDisconnect: () -> Unit
) {
    var fontSizeSp by remember { mutableFloatStateOf(10f) } // Default font size 10sp
    var isCtrlActive by remember { mutableStateOf(false) }
    var isAltActive by remember { mutableStateOf(false) }
    var showSnippets by remember { mutableStateOf(false) }
    var isScrolledToBottom by remember { mutableStateOf(true) }

    var terminalViewRef by remember { mutableStateOf<TerminalView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.onDataReceived = null
        }
    }

    // Function to send bytes or VT100 escape sequences to SSH server
    fun sendToTerminal(bytes: ByteArray) {
        viewModel.sendBytes(bytes)
    }

    // Function to process character input considering sticky CTRL / ALT states
    fun processCharInput(char: Char) {
        val bytes = when {
            isCtrlActive -> {
                isCtrlActive = false
                val code = char.uppercaseChar().code - 'A'.code + 1
                if (code in 1..26) byteArrayOf(code.toByte()) else char.toString().toByteArray()
            }
            isAltActive -> {
                isAltActive = false
                byteArrayOf(27) + char.toString().toByteArray()
            }
            else -> char.toString().toByteArray()
        }
        sendToTerminal(bytes)
    }

    Column(modifier = Modifier.fillMaxSize().background(TermBg).imePadding()) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth().background(NocCard).padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(if (connectionState == "Connected") TermGreen else Color.Red, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(connectionState, color = Color.White, fontSize = 12.sp)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = {
                        if (fontSizeSp > 8f) {
                            fontSizeSp -= 1f
                            terminalViewRef?.setFontSize(fontSizeSp)
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Text("A-", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Text("${fontSizeSp.toInt()}sp", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                TextButton(
                    onClick = {
                        if (fontSizeSp < 16f) {
                            fontSizeSp += 1f
                            terminalViewRef?.setFontSize(fontSizeSp)
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Text("A+", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onDisconnect, modifier = Modifier.size(24.dp)) {
                    Icon(AppIcons.close(), contentDescription = "Disconnect", tint = Color.Gray)
                }
            }
        }

        // Terminal View Container
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            AndroidView(
                factory = { ctx ->
                    TerminalView(ctx).apply {
                        terminalViewRef = this
                        setFontSize(fontSizeSp)
                        onSendBytes = { bytes -> viewModel.sendBytes(bytes) }
                        onResize = { cols, rows, w, h -> viewModel.updateWindowSize(cols, rows, w, h) }
                        onScrollStateChanged = { isBottom -> isScrolledToBottom = isBottom }

                        viewModel.onDataReceived = { buffer, length ->
                            appendData(buffer, length)
                        }
                    }
                },
                update = { view ->
                    terminalViewRef = view
                },
                modifier = Modifier.fillMaxSize()
            )

            // Jump to Bottom Floating Chip
            if (!isScrolledToBottom) {
                Surface(
                    onClick = {
                        terminalViewRef?.jumpToBottom()
                        isScrolledToBottom = true
                    },
                    shape = RoundedCornerShape(20.dp),
                    color = NocAccent,
                    shadowElevation = 6.dp,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("▼ Jump to bottom", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Real-Time Sticky Extra Keys Bar (2 Compact Rows)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(NocCard)
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Row 1: Modifiers, Navigation & Snippets
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item { KeyChip(text = "ESC", onClick = { sendToTerminal(byteArrayOf(27)) }) }
                item { KeyChip(text = "TAB", onClick = { sendToTerminal(byteArrayOf(9)) }) }
                item { KeyChip(text = "CTRL", isActive = isCtrlActive, onClick = { isCtrlActive = !isCtrlActive }) }
                item { KeyChip(text = "ALT", isActive = isAltActive, onClick = { isAltActive = !isAltActive }) }

                // Font Size Adjuster Chips (Range 8sp - 16sp)
                item {
                    KeyChip(
                        text = "A-",
                        onClick = {
                            if (fontSizeSp > 8f) {
                                fontSizeSp -= 1f
                                terminalViewRef?.setFontSize(fontSizeSp)
                            }
                        }
                    )
                }
                item {
                    KeyChip(
                        text = "${fontSizeSp.toInt()}sp",
                        onClick = { }
                    )
                }
                item {
                    KeyChip(
                        text = "A+",
                        onClick = {
                            if (fontSizeSp < 16f) {
                                fontSizeSp += 1f
                                terminalViewRef?.setFontSize(fontSizeSp)
                            }
                        }
                    )
                }

                item { KeyChip(text = "▲", onClick = { sendToTerminal(byteArrayOf(27, 91, 65)) }) }
                item { KeyChip(text = "▼", onClick = { sendToTerminal(byteArrayOf(27, 91, 66)) }) }
                item { KeyChip(text = "◄", onClick = { sendToTerminal(byteArrayOf(27, 91, 68)) }) }
                item { KeyChip(text = "►", onClick = { sendToTerminal(byteArrayOf(27, 91, 67)) }) }
                item { KeyChip(text = "HOME", onClick = { sendToTerminal("\u001B[1~".toByteArray()) }) }
                item { KeyChip(text = "END", onClick = { sendToTerminal("\u001B[4~".toByteArray()) }) }
                item { KeyChip(text = "PGUP", onClick = { sendToTerminal("\u001B[5~".toByteArray()) }) }
                item { KeyChip(text = "PGDN", onClick = { sendToTerminal("\u001B[6~".toByteArray()) }) }
                item {
                    IconButton(onClick = { showSnippets = true }, modifier = Modifier.size(28.dp)) {
                        Icon(AppIcons.list(), contentDescription = "Snippets", tint = TermGreen)
                    }
                }
            }

            // Row 2: Control Shortcuts & Common Symbols
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item { KeyChip(text = "Ctrl+C", onClick = { sendToTerminal(byteArrayOf(3)) }) }
                item { KeyChip(text = "Ctrl+D", onClick = { sendToTerminal(byteArrayOf(4)) }) }
                item { KeyChip(text = "Ctrl+Z", onClick = { sendToTerminal(byteArrayOf(26)) }) }
                item { KeyChip(text = "Ctrl+L", onClick = { sendToTerminal(byteArrayOf(12)) }) }
                item { KeyChip(text = "Ctrl+A", onClick = { sendToTerminal(byteArrayOf(1)) }) }
                item { KeyChip(text = "Ctrl+E", onClick = { sendToTerminal(byteArrayOf(5)) }) }
                item { KeyChip(text = "Ctrl+R", onClick = { sendToTerminal(byteArrayOf(18)) }) }

                val symbols = listOf("|", "~", "/", "-", "_", "\\", "`", "{", "}", "[", "]", "$", "&")
                items(symbols) { sym ->
                    KeyChip(text = sym, onClick = { processCharInput(sym[0]) })
                }
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
                            viewModel.sendCommand(snippet.command)
                            showSnippets = false
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
    }
}

@Composable
fun KeyChip(
    text: String,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isActive) NocAccent else NocCard,
        border = BorderStroke(1.dp, if (isActive) TermGreen else Color.White.copy(alpha = 0.15f))
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SftpExplorerBottomSheet(
    host: SshHost,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sftpManager = remember { SftpManager(context) }
    val initialPath = if (host.username.equals("root", ignoreCase = true)) "/root" else "/home/${host.username}"
    var currentPath by remember { mutableStateOf(initialPath) }
    var files by remember { mutableStateOf<List<SftpRemoteFile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var selectedFileForAction by remember { mutableStateOf<SftpRemoteFile?>(null) }
    var fileToDelete by remember { mutableStateOf<SftpRemoteFile?>(null) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    fun loadPath(path: String) {
        isLoading = true
        errorMessage = null
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!sftpManager.isConnected()) {
                    sftpManager.connect(host)
                }
                val result = sftpManager.listFiles(path)
                withContext(Dispatchers.Main) {
                    files = result.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                    currentPath = path
                    isLoading = false
                }
            } catch (e: Exception) {
                // If initial path fails (e.g. /home/user doesn't exist), fallback to root /
                if (path != "/") {
                    loadPath("/")
                } else {
                    withContext(Dispatchers.Main) {
                        errorMessage = e.localizedMessage ?: "Failed to list files"
                        isLoading = false
                    }
                }
            }
        }
    }

    fun downloadFile(file: SftpRemoteFile) {
        Toast.makeText(context, "Downloading ${file.name} to Downloads folder...", Toast.LENGTH_SHORT).show()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val destFile = File(downloadsDir, file.name)
                sftpManager.downloadFile(file.path, destFile.absolutePath)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Downloaded ${file.name} to Downloads!", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Download failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun deleteRemoteItem(file: SftpRemoteFile) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (file.isDirectory) {
                    sftpManager.deleteDirectory(file.path)
                } else {
                    sftpManager.deleteFile(file.path)
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Deleted ${file.name}", Toast.LENGTH_SHORT).show()
                    loadPath(currentPath)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Delete failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun createFolder(folderName: String) {
        if (folderName.isBlank()) return
        val targetFolder = if (currentPath.endsWith("/")) "$currentPath$folderName" else "$currentPath/$folderName"
        CoroutineScope(Dispatchers.IO).launch {
            try {
                sftpManager.createDirectory(targetFolder)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Created folder $folderName", Toast.LENGTH_SHORT).show()
                    loadPath(currentPath)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to create folder: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    LaunchedEffect(host) {
        loadPath(currentPath)
    }

    DisposableEffect(Unit) {
        onDispose {
            CoroutineScope(Dispatchers.IO).launch {
                sftpManager.disconnect()
            }
        }
    }

    // Delete Confirmation Dialog
    if (fileToDelete != null) {
        val target = fileToDelete!!
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Delete Remote ${if (target.isDirectory) "Folder" else "File"}?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${target.name}' from server?") },
            confirmButton = {
                Button(
                    onClick = {
                        deleteRemoteItem(target)
                        fileToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) { Text("Cancel") }
            }
        )
    }

    // New Folder Dialog
    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Create New Folder", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    placeholder = { Text("Folder Name (e.g. backup)") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        createFolder(newFolderName.trim())
                        newFolderName = ""
                        showNewFolderDialog = false
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) { Text("Cancel") }
            }
        )
    }

    // File Action BottomSheet
    if (selectedFileForAction != null) {
        val selectedFile = selectedFileForAction!!
        ModalBottomSheet(
            onDismissRequest = { selectedFileForAction = null },
            containerColor = NocCard
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "${if (selectedFile.isDirectory) "📁" else "📄"} ${selectedFile.name}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                )
                Text("Path: ${selectedFile.path}", fontSize = 12.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                if (!selectedFile.isDirectory) {
                    ListItem(
                        headlineContent = { Text("Download to HP Downloads", color = Color.White) },
                        leadingContent = { Icon(AppIcons.download(), contentDescription = null, tint = TermGreen) },
                        modifier = Modifier.clickable {
                            downloadFile(selectedFile)
                            selectedFileForAction = null
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }

                ListItem(
                    headlineContent = { Text("Copy Remote Path", color = Color.White) },
                    leadingContent = { Icon(AppIcons.contentCopy(), contentDescription = null, tint = Color.White) },
                    modifier = Modifier.clickable {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Remote Path", selectedFile.path))
                        Toast.makeText(context, "Path copied: ${selectedFile.path}", Toast.LENGTH_SHORT).show()
                        selectedFileForAction = null
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )

                ListItem(
                    headlineContent = { Text("Delete Remote Item", color = MaterialTheme.colorScheme.error) },
                    leadingContent = { Icon(AppIcons.delete(), contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable {
                        fileToDelete = selectedFile
                        selectedFileForAction = null
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = NocCard,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .fillMaxHeight(0.85f)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("SFTP File Explorer", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Host: ${host.name} (${host.host})", fontSize = 12.sp, color = Color.Gray)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showNewFolderDialog = true }) {
                        Icon(AppIcons.add(), contentDescription = "New Folder", tint = Color.White)
                    }
                    IconButton(onClick = { loadPath(currentPath) }) {
                        Icon(AppIcons.refresh(), contentDescription = "Refresh", tint = Color.White)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(AppIcons.close(), contentDescription = "Close", tint = Color.Gray)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Current Directory Bar
            Surface(
                color = NocBg,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📁 $currentPath",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = TermGreen,
                        modifier = Modifier.weight(1f)
                    )
                    if (currentPath != "/") {
                        TextButton(
                            onClick = {
                                val parent = currentPath.substringBeforeLast("/").ifBlank { "/" }
                                loadPath(parent)
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text("⬆ Parent", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NocAccent)
                }
            } else if (errorMessage != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Failed to load path", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        Text(errorMessage!!, color = Color.Gray, fontSize = 12.sp)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { loadPath(currentPath) }) { Text("Retry") }
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(files) { file ->
                        val icon = if (file.isDirectory) "📁" else "📄"
                        val sizeStr = if (file.isDirectory) "DIR" else "${file.size / 1024} KB"

                        Surface(
                            onClick = {
                                if (file.isDirectory) {
                                    val nextPath = if (currentPath.endsWith("/")) "$currentPath${file.name}" else "$currentPath/${file.name}"
                                    loadPath(nextPath)
                                } else {
                                    selectedFileForAction = file
                                }
                            },
                            color = Color.Transparent,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(icon, fontSize = 16.sp)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(file.name, color = Color.White, fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                    Text("Size: $sizeStr", color = Color.Gray, fontSize = 11.sp)
                                }
                                IconButton(onClick = { selectedFileForAction = file }) {
                                    Icon(AppIcons.moreVert(), contentDescription = "Options", tint = Color.Gray)
                                }
                            }
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    }
                }
            }
        }
    }
}
