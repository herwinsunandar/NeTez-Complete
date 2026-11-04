package com.herwin.netez.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.herwin.netez.R
import com.herwin.netez.data.VaultEntry
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.theme.TechMonospace
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredentialDetailBottomSheet(
    entry: VaultEntry,
    decryptedPassword: () -> String,
    decryptedPrivateKey: () -> String,
    decryptedApiToken: () -> String,
    decryptedNotes: () -> String,
    onDismiss: () -> Unit,
    onCopyUsername: (String) -> Unit,
    onCopyPassword: (String) -> Unit,
    onConnectNeTezTool: (VaultEntry, String) -> Unit, // Opens SSH / SFTP / Ping
    onEdit: (VaultEntry) -> Unit,
    onDelete: (VaultEntry) -> Unit
) {
    val context = LocalContext.current
    var isSecretVisible by remember { mutableStateOf(false) }
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    val displayPassword = remember(isSecretVisible) {
        if (isSecretVisible) decryptedPassword() else "••••••••••••"
    }

    val displayToken = remember(isSecretVisible) {
        if (isSecretVisible) decryptedApiToken() else "••••••••••••"
    }

    val displayKey = remember(isSecretVisible) {
        if (isSecretVisible) decryptedPrivateKey() else "••••••••••••"
    }

    val displayNotes = remember(isSecretVisible) {
        if (isSecretVisible) decryptedNotes() else "••••••••••••"
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
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.title.ifBlank { entry.appName.ifBlank { "Credential Detail" } },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Updated: ${sdf.format(Date(entry.updatedAt))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = entry.credentialType,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = TechMonospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // Details Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (entry.websiteUrl?.isNotBlank() == true) {
                        ResultRow(
                            label = "Website URL",
                            value = entry.websiteUrl,
                            isTechnicalData = true
                        )
                    }

                    if (entry.hostnameOrIp?.isNotBlank() == true) {
                        ResultRow(
                            label = "Host / IP",
                            value = "${entry.hostnameOrIp}:${entry.port ?: 22}",
                            isTechnicalData = true
                        )
                    }

                    if (entry.username.isNotBlank()) {
                        ResultRow(
                            label = "Username",
                            value = entry.username,
                            isTechnicalData = true
                        )
                    }

                    if (entry.passwordEncrypted.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Password", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = displayPassword,
                                    fontFamily = TechMonospace,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(onClick = { isSecretVisible = !isSecretVisible }) {
                                Icon(
                                    painter = if (isSecretVisible) AppIcons.visibilityOff() else AppIcons.visibility(),
                                    contentDescription = "Toggle",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    if (entry.credentialType == "SSH" || entry.credentialType == "SFTP") {
                        if (entry.privateKeyEncrypted?.isNotBlank() == true) {
                            Column {
                                Text("Private Key", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = displayKey,
                                    fontFamily = TechMonospace,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    if (entry.credentialType == "API" && entry.apiEndpoint?.isNotBlank() == true) {
                        ResultRow("Endpoint", entry.apiEndpoint, isTechnicalData = true)
                    }

                    if (entry.credentialType == "API" && entry.apiTokenEncrypted?.isNotBlank() == true) {
                        Column {
                            Text("API Secret / Token", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = displayToken, fontFamily = TechMonospace, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    if (entry.credentialType == "DATABASE" && entry.databaseType?.isNotBlank() == true) {
                        ResultRow("Database Type", "${entry.databaseType} (${entry.databaseName ?: "Default"})", isTechnicalData = true)
                    }

                    if (entry.credentialType == "NETWORK" && entry.vendorName?.isNotBlank() == true) {
                        ResultRow("Vendor Name", entry.vendorName, isTechnicalData = true)
                    }

                    if (entry.notesEncrypted?.isNotBlank() == true) {
                        Column {
                            Text("Secure Notes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = displayNotes, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            // Open Website Button
            if (entry.websiteUrl?.isNotBlank() == true) {
                Button(
                    onClick = {
                        try {
                            val urlStr = if (!entry.websiteUrl.startsWith("http://") && !entry.websiteUrl.startsWith("https://")) {
                                "https://${entry.websiteUrl}"
                            } else entry.websiteUrl
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlStr))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open URL", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        painter = AppIcons.public(),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("OPEN WEBSITE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            // Quick Connect Button for SSH / SFTP / Server
            if (entry.credentialType == "SSH" || entry.credentialType == "SFTP" || entry.credentialType == "SERVER") {
                Button(
                    onClick = { onConnectNeTezTool(entry, decryptedPassword()) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        painter = AppIcons.terminal(),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.vault_btn_connect), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            // Copy & Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (entry.username.isNotBlank()) {
                    OutlinedButton(
                        onClick = { onCopyUsername(entry.username) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("COPY USER", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (entry.passwordEncrypted.isNotBlank() || entry.apiTokenEncrypted?.isNotBlank() == true) {
                    Button(
                        onClick = {
                            val secret = if (entry.passwordEncrypted.isNotBlank()) decryptedPassword() else decryptedApiToken()
                            onCopyPassword(secret)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("COPY PASS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onEdit(entry) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(painter = AppIcons.edit(), contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("EDIT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { onDelete(entry) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Icon(painter = AppIcons.delete(), contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("DELETE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
