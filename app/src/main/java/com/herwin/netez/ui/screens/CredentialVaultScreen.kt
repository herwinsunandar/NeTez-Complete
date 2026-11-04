package com.herwin.netez.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.R
import com.herwin.netez.data.VaultEntry
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.components.AddCredentialBottomSheet
import com.herwin.netez.ui.components.CredentialDetailBottomSheet
import com.herwin.netez.ui.theme.TechMonospace
import com.herwin.netez.ui.viewmodels.PasswordManagerViewModel
import com.herwin.netez.ui.viewmodels.VaultTypeFilter
import com.herwin.netez.utils.BiometricHelper
import java.text.SimpleDateFormat
import java.util.*

import com.herwin.netez.entitlement.FeatureAccessManager
import com.herwin.netez.entitlement.FeatureId
import com.herwin.netez.ui.components.ContextualPaywallDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredentialVaultScreen(
    onBack: () -> Unit = {},
    onNavigateToSsh: (String) -> Unit = {},
    onNavigateToSftp: (String) -> Unit = {},
    viewModel: PasswordManagerViewModel = viewModel()
) {
    val vaultEntries by viewModel.vaultEntries.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    val context = LocalContext.current

    if (viewModel.showPaywallDialog) {
        ContextualPaywallDialog(
            featureId = FeatureId.CREDENTIAL_VAULT_MAX,
            limitText = viewModel.paywallLimitText.ifBlank { "Free plan supports up to 10 credentials in vault. Upgrade to NeTez PRO for unlimited vault storage." },
            onDismiss = { viewModel.showPaywallDialog = false },
            onUpgradeClick = {}
        )
    }

    val authTitle = stringResource(R.string.pass_auth_title)
    val authSubtitle = stringResource(R.string.pass_auth_subtitle)

    var isAuthorized by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedEntryToDetail by remember { mutableStateOf<VaultEntry?>(null) }
    var selectedEntryToEdit by remember { mutableStateOf<VaultEntry?>(null) }
    var entryToDelete by remember { mutableStateOf<VaultEntry?>(null) }

    LaunchedEffect(Unit) {
        if (viewModel.isLocked()) {
            BiometricHelper.authenticate(
                context as FragmentActivity,
                authTitle,
                authSubtitle,
                onSuccess = {
                    viewModel.markAuthenticated()
                    isAuthorized = true
                },
                onError = { onBack() }
            )
        } else {
            isAuthorized = true
        }
    }

    LaunchedEffect(viewModel.actionMessage) {
        viewModel.actionMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.actionMessage = null
        }
    }

    if (!isAuthorized) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    // Add / Edit BottomSheet
    if (showAddDialog || selectedEntryToEdit != null) {
        AddCredentialBottomSheet(
            initialEntry = selectedEntryToEdit,
            onDismiss = {
                showAddDialog = false
                selectedEntryToEdit = null
            },
            onSave = { entry ->
                viewModel.saveEntry(entry)
                showAddDialog = false
                selectedEntryToEdit = null
            },
            onReadPrivateKey = { uri -> viewModel.readTextFromUri(context, uri) }
        )
    }

    // Detail BottomSheet
    selectedEntryToDetail?.let { entry ->
        CredentialDetailBottomSheet(
            entry = entry,
            decryptedPassword = { viewModel.decryptSecret(entry.passwordEncrypted) },
            decryptedPrivateKey = { viewModel.decryptSecret(entry.privateKeyEncrypted) },
            decryptedApiToken = { viewModel.decryptSecret(entry.apiTokenEncrypted) },
            decryptedNotes = { viewModel.decryptSecret(entry.notesEncrypted) },
            onDismiss = { selectedEntryToDetail = null },
            onCopyUsername = { viewModel.copyToClipboard(context, it, "username") },
            onCopyPassword = { viewModel.copyToClipboard(context, it, "password") },
            onConnectNeTezTool = { vaultEntry, _ ->
                if (vaultEntry.credentialType == "SSH") {
                    onNavigateToSsh(vaultEntry.hostnameOrIp ?: "")
                } else if (vaultEntry.credentialType == "SFTP") {
                    onNavigateToSftp(vaultEntry.hostnameOrIp ?: "")
                } else {
                    onNavigateToSsh(vaultEntry.hostnameOrIp ?: "")
                }
                selectedEntryToDetail = null
            },
            onEdit = {
                selectedEntryToDetail = null
                selectedEntryToEdit = it
            },
            onDelete = {
                selectedEntryToDetail = null
                entryToDelete = it
            }
        )
    }

    // Delete Dialog
    if (entryToDelete != null) {
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(R.string.pass_delete_confirm_title), color = MaterialTheme.colorScheme.onSurface) },
            text = { Text(stringResource(R.string.pass_delete_confirm_desc), color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteEntry(entryToDelete!!)
                        entryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.btn_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    val isProUser = remember(context) { FeatureAccessManager.getInstance(context).isPro() }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (!isProUser && vaultEntries.size >= 10) {
                        viewModel.showPaywallDialog = true
                    } else {
                        showAddDialog = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(painter = AppIcons.add(), contentDescription = "Add Credential")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Security Banner (Transparent Privacy Info)
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = AppIcons.shieldCheck(),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "100% Local & Encrypted Storage",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(R.string.vault_security_info),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }

                // 2. Stats Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VaultStatChip("${stats.total} Total", MaterialTheme.colorScheme.primary)
                        VaultStatChip("${stats.servers} Servers", MaterialTheme.colorScheme.secondary)
                        VaultStatChip("${stats.weak} Weak", if (stats.weak > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Soft Downgrade Overflow Banner (if Free tier and vault count > 10)
                val overflowVaultCount = vaultEntries.size
                if (!isProUser && overflowVaultCount > 10) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = AppIcons.info(),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "You currently have $overflowVaultCount saved credentials (Free limit is 10). All your existing saved secrets remain encrypted and safe. Re-subscribe to NeTez PRO to add new credentials.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                // 3. Search Field
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onSearchQueryChange(it) },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.vault_search_hint),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontSize = 13.sp
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(
                                painter = AppIcons.search(),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )
                }

                // 4. Type Filter Chips
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            VaultTypeFilter.ALL to "ALL",
                            VaultTypeFilter.WEBSITE to "WEBSITE",
                            VaultTypeFilter.SERVER to "SERVER",
                            VaultTypeFilter.SSH to "SSH",
                            VaultTypeFilter.SFTP to "SFTP",
                            VaultTypeFilter.API to "API",
                            VaultTypeFilter.DATABASE to "DATABASE",
                            VaultTypeFilter.NETWORK to "NETWORK",
                            VaultTypeFilter.NOTES to "NOTES"
                        ).forEach { (filterType, label) ->
                            FilterChip(
                                selected = selectedType == filterType,
                                onClick = { viewModel.onTypeFilterChange(filterType) },
                                label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }

                // 5. Entries List
                if (vaultEntries.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    painter = AppIcons.lock(),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                    modifier = Modifier.size(56.dp)
                                )
                                Text(
                                    text = "No credentials found in vault",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                } else {
                    items(vaultEntries, key = { it.id }) { entry ->
                        VaultEntryCard(
                            entry = entry,
                            onClick = { selectedEntryToDetail = entry },
                            onToggleFavorite = { viewModel.toggleFavorite(entry) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VaultEntryCard(
    entry: VaultEntry,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val displayTitle = entry.title.ifBlank { entry.appName.ifBlank { entry.hostnameOrIp ?: "Untitled" } }
    val displaySubtitle = when {
        entry.websiteUrl?.isNotBlank() == true -> entry.websiteUrl
        entry.hostnameOrIp?.isNotBlank() == true -> "${entry.hostnameOrIp}:${entry.port ?: 22}"
        entry.username.isNotBlank() -> entry.username
        entry.apiEndpoint?.isNotBlank() == true -> entry.apiEndpoint
        else -> entry.credentialType
    }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayTitle.take(1).uppercase(),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = displayTitle,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (entry.isFavorite) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            painter = AppIcons.star(),
                            contentDescription = "Favorite",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Text(
                    text = displaySubtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = TechMonospace,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = entry.credentialType,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = TechMonospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            IconButton(onClick = onToggleFavorite) {
                Icon(
                    painter = if (entry.isFavorite) AppIcons.star() else AppIcons.starBorder(),
                    contentDescription = "Favorite",
                    tint = if (entry.isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun VaultStatChip(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontFamily = TechMonospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}
