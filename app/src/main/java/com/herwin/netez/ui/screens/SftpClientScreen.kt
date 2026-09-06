package com.herwin.netez.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.data.AuthType
import com.herwin.netez.data.SshHost
import com.herwin.netez.network.SftpRemoteFile
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.theme.PremiumNavy
import com.herwin.netez.ui.theme.SoftDarkBg
import com.herwin.netez.ui.theme.TextGray
import com.herwin.netez.ui.viewmodels.SftpViewModel
import com.herwin.netez.utils.EncryptionUtils
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SftpClientScreen(viewModel: SftpViewModel = viewModel()) {
    val isConnected = viewModel.isConnected
    val hosts by viewModel.allHosts.collectAsState(initial = emptyList())
    
    var showAddDialog by remember { mutableStateOf(false) }
    var hostToEdit by remember { mutableStateOf<SshHost?>(null) }

    BackHandler(enabled = isConnected) {
        if (viewModel.currentPath != "/") {
            viewModel.navigateUp()
        } else {
            viewModel.disconnect()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PremiumNavy)
    ) {
        if (!isConnected) {
            SftpHostManagerView(
                hosts = hosts,
                onConnect = { viewModel.connect(it) },
                onAddClick = { showAddDialog = true },
                onEdit = { hostToEdit = it },
                onDelete = { viewModel.deleteHost(it) },
                isLoading = viewModel.isLoading,
                errorMessage = viewModel.errorMessage,
                onClearError = { viewModel.clearError() }
            )

            if (showAddDialog || hostToEdit != null) {
                SftpAddHostDialog(
                    editHost = hostToEdit,
                    onDismiss = {
                        showAddDialog = false
                        hostToEdit = null
                    },
                    onSave = { host ->
                        if (hostToEdit != null) viewModel.updateHost(host)
                        else viewModel.saveHost(host)
                        showAddDialog = false
                        hostToEdit = null
                    }
                )
            }
        } else {
            ExplorerView(
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun SftpHostManagerView(
    hosts: List<SshHost>,
    onConnect: (SshHost) -> Unit,
    onAddClick: () -> Unit,
    onEdit: (SshHost) -> Unit,
    onDelete: (SshHost) -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    onClearError: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "SFTP Hosts",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            IconButton(
                onClick = onAddClick,
                modifier = Modifier.background(Color(0xFF3B82F6), CircleShape)
            ) {
                Icon(AppIcons.add(), contentDescription = "Add", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (errorMessage != null) {
            ErrorMessageCard(message = errorMessage, onDismiss = onClearError)
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (hosts.isEmpty()) {
            EmptyStateView("No saved hosts. Tap + to add one.")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(hosts) { host ->
                    SftpHostCard(
                        host = host,
                        onConnect = { onConnect(host) },
                        onEdit = onEdit,
                        onDelete = onDelete,
                        isLoading = isLoading
                    )
                }
            }
        }
    }
}

@Composable
fun SftpHostCard(
    host: SshHost,
    onConnect: () -> Unit,
    onEdit: (SshHost) -> Unit,
    onDelete: (SshHost) -> Unit,
    isLoading: Boolean
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SoftDarkBg),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLoading) { onConnect() }
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF3B82F6).copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(AppIcons.dns(), contentDescription = null, tint = Color(0xFF3B82F6))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(host.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                Text("${host.username}@${host.host}:${host.port}", color = TextGray, fontSize = 14.sp)
            }

            IconButton(onClick = { showMenu = true }) {
                Icon(AppIcons.moreVert(), contentDescription = "Menu", tint = TextGray)
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(SoftDarkBg)
            ) {
                DropdownMenuItem(
                    text = { Text("Edit", color = Color.White) },
                    onClick = { onEdit(host); showMenu = false }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = Color.Red) },
                    onClick = { onDelete(host); showMenu = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerView(viewModel: SftpViewModel) {
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var selectedFileForMenu by remember { mutableStateOf<SftpRemoteFile?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        BreadcrumbBar(
            currentPath = viewModel.currentPath,
            onNavigate = { viewModel.navigateTo(it) },
            onRefresh = { viewModel.refreshFiles() },
            onDisconnect = { viewModel.disconnect() }
        )

        if (viewModel.isLoading && viewModel.files.isEmpty()) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Color(0xFF3B82F6))
        }

        Box(modifier = Modifier.weight(1f)) {
            if (viewModel.files.isEmpty() && !viewModel.isLoading) {
                EmptyStateView("This folder is empty")
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(viewModel.files) { file ->
                        FileItem(
                            file = file,
                            onClick = {
                                if (file.isDirectory) {
                                    viewModel.navigateTo(file.path)
                                }
                            },
                            onLongClick = { selectedFileForMenu = file }
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }

            if (viewModel.errorMessage != null) {
                Snackbar(
                    action = { TextButton(onClick = { viewModel.clearError() }) { Text("OK") } },
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    Text(viewModel.errorMessage!!)
                }
            }
        }

        Surface(
            color = SoftDarkBg,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                ActionButton(AppIcons.createNewFolder(), "New Folder") { showNewFolderDialog = true }
                ActionButton(AppIcons.upload(), "Upload") { /* Implement Upload */ }
                ActionButton(AppIcons.refresh(), "Refresh") { viewModel.refreshFiles() }
            }
        }
    }

    if (showNewFolderDialog) {
        NewFolderDialog(
            onDismiss = { showNewFolderDialog = false },
            onCreate = { name ->
                viewModel.createDirectory(name)
                showNewFolderDialog = false
            }
        )
    }

    if (selectedFileForMenu != null) {
        FileActionMenu(
            file = selectedFileForMenu!!,
            onDismiss = { selectedFileForMenu = null },
            onAction = { action ->
                when (action) {
                    "Delete" -> viewModel.deleteFile(selectedFileForMenu!!)
                    "Download" -> { /* Implement Download */ }
                }
                selectedFileForMenu = null
            }
        )
    }
}

@Composable
fun BreadcrumbBar(
    currentPath: String,
    onNavigate: (String) -> Unit,
    onRefresh: () -> Unit,
    onDisconnect: () -> Unit
) {
    val parts = currentPath.split("/").filter { it.isNotEmpty() }
    
    Surface(color = SoftDarkBg) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Explorer",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onRefresh) { Icon(AppIcons.refresh(), null, tint = Color.White) }
                IconButton(onClick = onDisconnect) { Icon(AppIcons.logout(), null, tint = Color.Red) }
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    Text(
                        "/",
                        color = if (currentPath == "/") Color.White else TextGray,
                        modifier = Modifier
                            .clickable { onNavigate("/") }
                            .padding(4.dp)
                    )
                }
                
                parts.forEachIndexed { index, part ->
                    item { Icon(AppIcons.chevronRight(), null, tint = TextGray, modifier = Modifier.size(16.dp)) }
                    item {
                        val path = "/" + parts.take(index + 1).joinToString("/")
                        Text(
                            part,
                            color = if (index == parts.size - 1) Color.White else TextGray,
                            fontWeight = if (index == parts.size - 1) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .clickable { onNavigate(path) }
                                .padding(4.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileItem(file: SftpRemoteFile, onClick: () -> Unit, onLongClick: () -> Unit) {
    ListItem(
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        ),
        headlineContent = { 
            Text(file.name, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis) 
        },
        supportingContent = {
            val date = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                .format(Date(file.lastModified * 1000))
            Text("${formatSize(file.size)} • $date", color = TextGray, fontSize = 12.sp)
        },
        leadingContent = {
            val icon = if (file.isDirectory) AppIcons.folder() else AppIcons.description()
            val tint = if (file.isDirectory) Color(0xFFFBBF24) else Color(0xFF3B82F6)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(tint.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint)
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
fun ActionButton(icon: Painter, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        Text(label, color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun NewFolderDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Folder") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Folder Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { if (name.isNotEmpty()) onCreate(name) }) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        containerColor = SoftDarkBg,
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}

@Composable
fun FileActionMenu(file: SftpRemoteFile, onDismiss: () -> Unit, onAction: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column {
                ActionMenuItem(AppIcons.download(), "Download") { onAction("Download") }
                ActionMenuItem(AppIcons.edit(), "Rename") { onAction("Rename") }
                ActionMenuItem(AppIcons.delete(), "Delete", Color.Red) { onAction("Delete") }
                ActionMenuItem(AppIcons.contentCopy(), "Copy Path") { onAction("CopyPath") }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        containerColor = SoftDarkBg,
        titleContentColor = Color.White
    )
}

@Composable
fun ActionMenuItem(icon: Painter, label: String, tint: Color = Color.White, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, color = tint)
    }
}

@Composable
fun ErrorMessageCard(message: String, onDismiss: () -> Unit) {
    Surface(
        color = Color(0xFFEF4444).copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(AppIcons.error(), null, tint = Color(0xFFEF4444))
            Spacer(modifier = Modifier.width(12.dp))
            Text(message, color = Color(0xFFEF4444), modifier = Modifier.weight(1f), fontSize = 14.sp)
            IconButton(onClick = onDismiss) { Icon(AppIcons.close(), null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp)) }
        }
    }
}

@Composable
fun EmptyStateView(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(AppIcons.folder(), null, tint = TextGray.copy(alpha = 0.5f), modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(message, color = TextGray, textAlign = TextAlign.Center)
    }
}

fun formatSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(Locale.getDefault(), "%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

@Composable
fun SftpAddHostDialog(editHost: SshHost? = null, onDismiss: () -> Unit, onSave: (SshHost) -> Unit) {
    var name by remember { mutableStateOf(editHost?.name ?: "") }
    var host by remember { mutableStateOf(editHost?.host ?: "") }
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
            color = PremiumNavy,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Text(if (editHost == null) "Add New Host" else "Edit Host", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))

                SftpHostTextField("Display Name", name) { name = it }
                SftpHostTextField("Host / IP Address", host) { host = it }
                SftpHostTextField("Port", port, KeyboardType.Number) { port = it }
                SftpHostTextField("Username", user) { user = it }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Authentication", color = TextGray, fontSize = 14.sp)
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    SftpAuthTab("Password", authType == AuthType.PASSWORD, Modifier.weight(1f)) { authType = AuthType.PASSWORD }
                    SftpAuthTab("Private Key", authType == AuthType.KEY, Modifier.weight(1f)) { authType = AuthType.KEY }
                }

                if (authType == AuthType.PASSWORD) {
                    SftpHostTextField("Password / Passphrase", password, KeyboardType.Password) { password = it }
                } else {
                    Button(
                        onClick = { fileLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SoftDarkBg)
                    ) {
                        Icon(AppIcons.uploadFile(), contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (privateKeyPath != null) "Key Selected" else "Select Private Key")
                    }
                    if (privateKeyPath != null) {
                        Text("File: ${privateKeyPath?.substringAfterLast("/")}", color = Color(0xFF10B981), fontSize = 12.sp)
                    }
                    SftpHostTextField("Passphrase (Optional)", password, KeyboardType.Password) { password = it }
                }

                SftpHostTextField("Tag (Optional)", tag) { tag = it }

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
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun SftpHostTextField(label: String, value: String, keyboardType: KeyboardType = KeyboardType.Text, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextGray) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedTextColor = Color.White,
            focusedTextColor = Color.White,
            cursorColor = Color(0xFF3B82F6),
            focusedBorderColor = Color(0xFF3B82F6),
            unfocusedBorderColor = Color.DarkGray
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (keyboardType == KeyboardType.Password) PasswordVisualTransformation() else VisualTransformation.None
    )
}

@Composable
fun SftpAuthTab(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .padding(4.dp)
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color(0xFF3B82F6) else SoftDarkBg)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) Color.White else TextGray, fontWeight = FontWeight.Bold)
    }
}
