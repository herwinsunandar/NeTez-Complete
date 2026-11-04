package com.herwin.netez.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.herwin.netez.R
import com.herwin.netez.data.VaultEntry
import com.herwin.netez.ui.AppIcons
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCredentialBottomSheet(
    initialEntry: VaultEntry? = null,
    prefilledPassword: String = "",
    prefilledStrength: Int = 80,
    onDismiss: () -> Unit,
    onSave: (VaultEntry) -> Unit,
    onReadPrivateKey: (suspend (Uri) -> String)? = null
) {
    val scope = rememberCoroutineScope()

    var selectedType by remember { mutableStateOf(initialEntry?.credentialType ?: "WEBSITE") }
    var title by remember { mutableStateOf(initialEntry?.title ?: initialEntry?.appName ?: "") }
    var username by remember { mutableStateOf(initialEntry?.username ?: "") }
    var password by remember { mutableStateOf(prefilledPassword.ifBlank { "" }) }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var websiteUrl by remember { mutableStateOf(initialEntry?.websiteUrl ?: "") }
    var notes by remember { mutableStateOf(initialEntry?.notesEncrypted ?: "") }

    // Server / SSH / SFTP / Network fields
    var hostnameOrIp by remember { mutableStateOf(initialEntry?.hostnameOrIp ?: "") }
    var portStr by remember { mutableStateOf(initialEntry?.port?.toString() ?: "22") }
    var protocol by remember { mutableStateOf(initialEntry?.protocol ?: "SSH") }
    var authType by remember { mutableStateOf(initialEntry?.authType ?: "PASSWORD") }
    var privateKeyContent by remember { mutableStateOf(initialEntry?.privateKeyEncrypted ?: "") }

    // API / DB / Network Vendor fields
    var apiEndpoint by remember { mutableStateOf(initialEntry?.apiEndpoint ?: "") }
    var apiToken by remember { mutableStateOf(initialEntry?.apiTokenEncrypted ?: "") }
    var databaseType by remember { mutableStateOf(initialEntry?.databaseType ?: "MySQL") }
    var databaseName by remember { mutableStateOf(initialEntry?.databaseName ?: "") }
    var vendorName by remember { mutableStateOf(initialEntry?.vendorName ?: "MikroTik") }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            if (onReadPrivateKey != null) {
                scope.launch {
                    val keyText = onReadPrivateKey(it)
                    if (keyText.isNotBlank()) {
                        privateKeyContent = keyText
                    }
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (initialEntry == null) stringResource(R.string.vault_add_title) else stringResource(R.string.vault_edit_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Credential Type Selector Scrollable Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "WEBSITE" to stringResource(R.string.vault_type_website),
                    "SERVER" to stringResource(R.string.vault_type_server),
                    "SSH" to stringResource(R.string.vault_type_ssh),
                    "SFTP" to stringResource(R.string.vault_type_sftp),
                    "API" to stringResource(R.string.vault_type_api),
                    "DATABASE" to stringResource(R.string.vault_type_database),
                    "NETWORK" to stringResource(R.string.vault_type_network),
                    "NOTES" to stringResource(R.string.vault_type_notes)
                ).forEach { (typeKey, label) ->
                    FilterChip(
                        selected = selectedType == typeKey,
                        onClick = { selectedType = typeKey },
                        label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            // Title / Label Input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title / Label", fontSize = 12.sp) },
                placeholder = { Text("e.g. Production Server, Main Google Account", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            // Dynamic Form per Type
            when (selectedType) {
                "WEBSITE" -> {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username / Email", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        shape = RoundedCornerShape(10.dp),
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    painter = if (isPasswordVisible) AppIcons.visibilityOff() else AppIcons.visibility(),
                                    contentDescription = "Toggle"
                                )
                            }
                        }
                    )

                    OutlinedTextField(
                        value = websiteUrl,
                        onValueChange = { websiteUrl = it },
                        label = { Text("Website URL (Optional)", fontSize = 12.sp) },
                        placeholder = { Text("e.g. https://example.com", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                "SERVER", "SSH", "SFTP", "NETWORK" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = hostnameOrIp,
                            onValueChange = { hostnameOrIp = it },
                            label = { Text("Hostname / IP", fontSize = 12.sp) },
                            placeholder = { Text("192.168.1.1", fontSize = 12.sp) },
                            modifier = Modifier.weight(2f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = portStr,
                            onValueChange = { portStr = it },
                            label = { Text("Port", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username", fontSize = 12.sp) },
                        placeholder = { Text("root or admin", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    if (selectedType == "SSH" || selectedType == "SFTP") {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilterChip(
                                selected = authType == "PASSWORD",
                                onClick = { authType = "PASSWORD" },
                                label = { Text("Password Auth") }
                            )
                            FilterChip(
                                selected = authType == "PRIVATE_KEY",
                                onClick = { authType = "PRIVATE_KEY" },
                                label = { Text("Private Key Auth") }
                            )
                        }
                    }

                    if (authType == "PASSWORD" || selectedType == "SERVER" || selectedType == "NETWORK") {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            shape = RoundedCornerShape(10.dp),
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        painter = if (isPasswordVisible) AppIcons.visibilityOff() else AppIcons.visibility(),
                                        contentDescription = "Toggle"
                                    )
                                }
                            }
                        )
                    }

                    if (authType == "PRIVATE_KEY" && (selectedType == "SSH" || selectedType == "SFTP")) {
                        OutlinedTextField(
                            value = privateKeyContent,
                            onValueChange = { privateKeyContent = it },
                            label = { Text("Private Key (PEM/OpenSSH)", fontSize = 12.sp) },
                            placeholder = { Text("-----BEGIN OPENSSH PRIVATE KEY-----", fontSize = 11.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 90.dp),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedButton(
                            onClick = { filePicker.launch("*/*") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(painter = AppIcons.folder(), contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.vault_import_private_key), fontSize = 12.sp)
                        }
                    }

                    if (selectedType == "NETWORK") {
                        OutlinedTextField(
                            value = vendorName,
                            onValueChange = { vendorName = it },
                            label = { Text("Vendor Name", fontSize = 12.sp) },
                            placeholder = { Text("MikroTik, Cisco, Fortinet", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                "API" -> {
                    OutlinedTextField(
                        value = apiEndpoint,
                        onValueChange = { apiEndpoint = it },
                        label = { Text("Endpoint URL", fontSize = 12.sp) },
                        placeholder = { Text("https://api.example.com/v1", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = apiToken,
                        onValueChange = { apiToken = it },
                        label = { Text("API Token / Key / Secret", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                "DATABASE" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = hostnameOrIp,
                            onValueChange = { hostnameOrIp = it },
                            label = { Text("Hostname / IP", fontSize = 12.sp) },
                            modifier = Modifier.weight(2f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = databaseType,
                            onValueChange = { databaseType = it },
                            label = { Text("Type (MySQL/Postgres)", fontSize = 12.sp) },
                            modifier = Modifier.weight(1.5f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = databaseName,
                            onValueChange = { databaseName = it },
                            label = { Text("Database Name", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("DB User", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("DB Password", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                "NOTES" -> {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Encrypted Secure Content", fontSize = 12.sp) },
                        placeholder = { Text("Enter confidential notes or secrets here...", fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            if (selectedType != "NOTES") {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Remarks (Optional)", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.btn_cancel))
                }

                Button(
                    onClick = {
                        val isWebsite = selectedType == "WEBSITE"
                        val isServerLike = selectedType == "SERVER" || selectedType == "SSH" || selectedType == "SFTP" || selectedType == "NETWORK"
                        val isApi = selectedType == "API"
                        val isDb = selectedType == "DATABASE"
                        val isNet = selectedType == "NETWORK"

                        val portInt = portStr.toIntOrNull() ?: if (selectedType == "SSH" || selectedType == "SFTP") 22 else 80
                        val entry = VaultEntry(
                            id = initialEntry?.id ?: UUID.randomUUID().toString(),
                            title = title,
                            credentialType = selectedType,
                            appName = title,
                            username = username,
                            passwordEncrypted = password,
                            websiteUrl = if (isWebsite) websiteUrl else null,
                            notesEncrypted = notes,
                            hostnameOrIp = if (isServerLike || isDb) hostnameOrIp else null,
                            port = if (isServerLike || isDb) portInt else null,
                            protocol = if (selectedType == "SSH") "SSH" else if (selectedType == "SFTP") "SFTP" else if (isServerLike) protocol else null,
                            authType = if (selectedType == "SSH" || selectedType == "SFTP") authType else null,
                            privateKeyEncrypted = if ((selectedType == "SSH" || selectedType == "SFTP") && authType == "PRIVATE_KEY") privateKeyContent else null,
                            apiEndpoint = if (isApi) apiEndpoint else null,
                            apiTokenEncrypted = if (isApi) apiToken else null,
                            databaseType = if (isDb) databaseType else null,
                            databaseName = if (isDb) databaseName else null,
                            vendorName = if (isNet) vendorName else null,
                            strengthScore = prefilledStrength,
                            isFavorite = initialEntry?.isFavorite ?: false
                        )
                        onSave(entry)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    enabled = title.isNotBlank() || username.isNotBlank() || hostnameOrIp.isNotBlank() || websiteUrl.isNotBlank()
                ) {
                    Text(stringResource(R.string.btn_save), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
