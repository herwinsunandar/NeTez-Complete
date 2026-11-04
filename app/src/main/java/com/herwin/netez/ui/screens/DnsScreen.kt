package com.herwin.netez.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.herwin.netez.R
import com.herwin.netez.entitlement.FeatureAccessManager
import com.herwin.netez.entitlement.FeatureId
import com.herwin.netez.network.DnsRecord
import com.herwin.netez.network.DnsRecordType
import com.herwin.netez.network.DnsServer
import com.herwin.netez.network.PublicDnsServers
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.components.ContextualPaywallDialog
import com.herwin.netez.ui.components.ResultRow
import com.herwin.netez.ui.theme.TechMonospace
import com.herwin.netez.ui.viewmodels.DnsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnsScreen(
    viewModel: DnsViewModel = viewModel(),
    onNavigateToPing: (String) -> Unit = {},
    onNavigateToTraceroute: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showServerMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val accessManager = remember { FeatureAccessManager.getInstance(context) }
    var showPaywallDialog by remember { mutableStateOf(false) }

    val pdfExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportPdfReport(context, it) }
    }

    if (showPaywallDialog) {
        ContextualPaywallDialog(
            featureId = FeatureId.REPORT_EXPORT,
            limitText = "Professional DNS Lookup PDF Report export is available in NeTez PRO.",
            onDismiss = { showPaywallDialog = false },
            onUpgradeClick = {}
        )
    }

    LaunchedEffect(viewModel.actionMessage) {
        viewModel.actionMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.actionMessage = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Top Record Type Tabs
        SecondaryScrollableTabRow(
            selectedTabIndex = uiState.recordType.ordinal,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 16.dp,
            divider = {},
            indicator = {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(uiState.recordType.ordinal),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            DnsRecordType.entries.forEach { type ->
                Tab(
                    selected = uiState.recordType == type,
                    onClick = { viewModel.updateRecordType(type) },
                    text = {
                        Text(
                            text = type.name,
                            fontWeight = if (uiState.recordType == type) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 2. DNS Server Selector Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PublicDnsServers.forEach { server ->
                        DnsServerChip(
                            server = server,
                            isSelected = uiState.selectedServer == server,
                            onClick = { viewModel.updateServer(server) }
                        )
                    }
                    // Custom/More Dropdown Button
                    Surface(
                        onClick = { showServerMenu = true },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = AppIcons.expandMore(),
                                contentDescription = "More Servers",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            DropdownMenu(
                                expanded = showServerMenu,
                                onDismissRequest = { showServerMenu = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                PublicDnsServers.forEach { server ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "${server.name} (${server.ip})",
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        onClick = {
                                            viewModel.updateServer(server)
                                            showServerMenu = false
                                        },
                                        leadingIcon = {
                                            if (server.countryCode.isNotEmpty()) {
                                                AsyncImage(
                                                    model = "https://flagcdn.com/w40/${server.countryCode.lowercase()}.png",
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .size(20.dp, 14.dp)
                                                        .clip(RoundedCornerShape(2.dp))
                                                )
                                            } else {
                                                Icon(
                                                    painter = AppIcons.dns(),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Input Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.domain,
                            onValueChange = { viewModel.updateDomain(it) },
                            placeholder = {
                                Text(
                                    text = "e.g. google.com or example.com",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    fontSize = 13.sp
                                )
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !uiState.isLoading,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Go
                            ),
                            keyboardActions = KeyboardActions(
                                onGo = { viewModel.lookup() }
                            ),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                        )

                        Button(
                            onClick = { viewModel.lookup() },
                            modifier = Modifier.height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            enabled = !uiState.isLoading,
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("LOOKUP", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // 4. Status Bar & Results
            uiState.response?.let { res ->
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                if (uiState.selectedServer.countryCode.isNotEmpty()) {
                                    AsyncImage(
                                        model = "https://flagcdn.com/w40/${uiState.selectedServer.countryCode.lowercase()}.png",
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(20.dp, 14.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                    )
                                    Spacer(Modifier.width(10.dp))
                                } else {
                                    Icon(
                                        painter = AppIcons.dns(),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(
                                    text = "Resolved via ${res.resolver} ${if (res.resolverIp == "System") "" else res.resolverIp}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontFamily = TechMonospace,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "${res.latency} ms",
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = TechMonospace,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (res.records.isEmpty()) {
                    item {
                        EmptyStateSuggestion { viewModel.updateServer(PublicDnsServers[1]) }
                    }
                } else {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Query Results",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row {
                                IconButton(onClick = {
                                    if (!accessManager.isPro()) {
                                        showPaywallDialog = true
                                    } else {
                                        val cleanDomain = (uiState.domain.ifBlank { "Domain" }).replace(Regex("[^a-zA-Z0-9]"), "_")
                                        pdfExportLauncher.launch("DNS_Audit_${cleanDomain}_All_Records.pdf")
                                    }
                                }) {
                                    Icon(
                                        painter = AppIcons.download(),
                                        contentDescription = "PDF Report",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = {
                                    viewModel.copyResultsToClipboard()
                                }) {
                                    Icon(
                                        painter = AppIcons.contentCopy(),
                                        contentDescription = "Copy",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = {
                                    viewModel.shareResults(context)
                                }) {
                                    Icon(
                                        painter = AppIcons.share(),
                                        contentDescription = "Share",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    items(res.records) { record ->
                        DnsProResultCard(
                            record = record,
                            onPing = { onNavigateToPing(record.data) },
                            onTrace = { onNavigateToTraceroute(record.data) }
                        )
                    }
                }
            }

            if (uiState.error != null && !uiState.isLoading && uiState.response == null) {
                item {
                    EmptyStateSuggestion { viewModel.updateServer(PublicDnsServers[0]) }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun DnsServerChip(server: DnsServer, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.height(34.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (server.countryCode.isNotEmpty()) {
                AsyncImage(
                    model = "https://flagcdn.com/w40/${server.countryCode.lowercase()}.png",
                    contentDescription = null,
                    modifier = Modifier
                        .size(18.dp, 12.dp)
                        .clip(RoundedCornerShape(2.dp))
                )
            }
            Text(
                text = server.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun DnsProResultCard(
    record: DnsRecord,
    onPing: () -> Unit,
    onTrace: () -> Unit
) {
    val recordTypeStr = when (record.type) {
        1 -> "A"
        28 -> "AAAA"
        15 -> "MX"
        16 -> "TXT"
        5 -> "CNAME"
        2 -> "NS"
        12 -> "PTR"
        else -> "Type ${record.type}"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = recordTypeStr,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = TechMonospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Text(
                    text = "TTL: ${record.ttl}s",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = TechMonospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = record.data,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = TechMonospace,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (record.countryName.isNotBlank() || record.provider.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (record.countryCode.isNotEmpty()) {
                        AsyncImage(
                            model = "https://flagcdn.com/w40/${record.countryCode.lowercase()}.png",
                            contentDescription = null,
                            modifier = Modifier
                                .size(18.dp, 12.dp)
                                .clip(RoundedCornerShape(2.dp))
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = "${record.provider} ${if (record.countryName.isNotEmpty()) "(${record.countryName})" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (record.type == 1 || record.type == 28) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onPing,
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(painter = AppIcons.networkCheck(), contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("PING", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = onTrace,
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(painter = AppIcons.route(), contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("TRACE", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateSuggestion(onSwitchServer: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = AppIcons.searchOff(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "No DNS Records Found",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Try switching your DNS resolver server or check record type.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onSwitchServer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("TRY GOOGLE DNS (8.8.8.8)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
