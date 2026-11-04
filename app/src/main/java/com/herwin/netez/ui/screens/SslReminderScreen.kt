package com.herwin.netez.ui.screens

import android.content.Context
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.R
import com.herwin.netez.data.entities.SslDomain
import com.herwin.netez.engine.SslTlsEngine
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.components.ResultRow
import com.herwin.netez.ui.components.SslDomainDetailBottomSheet
import com.herwin.netez.ui.theme.TechMonospace
import com.herwin.netez.ui.viewmodels.SslReminderViewModel

import com.herwin.netez.data.SecurityPreferences
import com.herwin.netez.entitlement.FeatureAccessManager
import com.herwin.netez.entitlement.FeatureId
import com.herwin.netez.ui.components.ContextualPaywallDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SslReminderScreen(
    onBack: () -> Unit,
    viewModel: SslReminderViewModel = viewModel()
) {
    val context = LocalContext.current
    val domains by viewModel.domains.collectAsState()
    var showBulkDialog by remember { mutableStateOf(false) }
    var selectedDomain by remember { mutableStateOf<SslDomain?>(null) }
    var domainToDelete by remember { mutableStateOf<SslDomain?>(null) }

    if (viewModel.showPaywallDialog) {
        ContextualPaywallDialog(
            featureId = FeatureId.SSL_REMINDER_DOMAINS_MAX,
            limitText = "Free plan supports up to 4 SSL expiration reminders. Upgrade to NeTez PRO for unlimited SSL domain monitoring.",
            onDismiss = { viewModel.showPaywallDialog = false },
            onUpgradeClick = {}
        )
    }

    LaunchedEffect(viewModel.actionMessage) {
        viewModel.actionMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.actionMessage = null
        }
    }

    val securityPrefs = remember { SecurityPreferences(context) }
    val maxReminderDays = remember(securityPrefs.defaultReminderDays) {
        val configDays = securityPrefs.defaultReminderDays.split(',').mapNotNull { it.trim().toIntOrNull() }
        configDays.maxOrNull() ?: 14
    }

    val filteredDomains = when (viewModel.filterState) {
        "Expiring" -> domains.filter { it.realDaysLeft in 1..maxReminderDays }
        "Expired" -> domains.filter { it.realDaysLeft <= 0 }
        "Grade F" -> domains.filter { it.grade == "F" }
        "Valid" -> domains.filter { it.realDaysLeft > maxReminderDays && it.grade != "F" }
        else -> domains
    }

    val expiringOrGradeFCount = domains.count { (it.realDaysLeft in 1..maxReminderDays) || it.grade == "F" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Tabs Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            TabRow(
                selectedTabIndex = viewModel.selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)) }
            ) {
                Tab(
                    selected = viewModel.selectedTab == 0,
                    onClick = { viewModel.selectedTab = 0 },
                    text = {
                        Text(
                            text = stringResource(R.string.ssltls_tab_my_domains),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Tab(
                    selected = viewModel.selectedTab == 1,
                    onClick = { viewModel.selectedTab = 1 },
                    text = {
                        Text(
                            text = stringResource(R.string.ssltls_tab_instant_check),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Tab Contents
        when (viewModel.selectedTab) {
            0 -> MyDomainsTabContent(
                domains = domains,
                filteredDomains = filteredDomains,
                expiringCount = expiringOrGradeFCount,
                viewModel = viewModel,
                onSelectDomain = { selectedDomain = it },
                onDeleteDomain = { domainToDelete = it },
                onShowBulkAdd = {
                    val accessManager = FeatureAccessManager.getInstance(context)
                    if (!accessManager.isPro()) {
                        viewModel.showPaywallDialog = true
                    } else {
                        showBulkDialog = true
                    }
                }
            )
            1 -> InstantCheckTabContent(viewModel = viewModel)
        }
    }

    selectedDomain?.let { domain ->
        SslDomainDetailBottomSheet(
            domain = domain,
            onDismiss = { selectedDomain = null },
            onRecheck = {
                viewModel.recheck(domain)
                selectedDomain = null
            },
            onDelete = {
                domainToDelete = domain
                selectedDomain = null
            }
        )
    }

    if (domainToDelete != null) {
        val domain = domainToDelete!!
        AlertDialog(
            onDismissRequest = { domainToDelete = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = stringResource(R.string.ssl_delete_dialog_title),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.ssl_delete_dialog_desc, domain.domain),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDomain(domain)
                        domainToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.btn_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { domainToDelete = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    if (showBulkDialog) {
        BulkAddDialog(
            onDismiss = { showBulkDialog = false },
            onConfirm = {
                viewModel.bulkAdd(it)
                showBulkDialog = false
            }
        )
    }
}

@Composable
private fun MyDomainsTabContent(
    domains: List<SslDomain>,
    filteredDomains: List<SslDomain>,
    expiringCount: Int,
    viewModel: SslReminderViewModel,
    onSelectDomain: (SslDomain) -> Unit,
    onDeleteDomain: (SslDomain) -> Unit,
    onShowBulkAdd: () -> Unit
) {
    val context = LocalContext.current
    val isProUser = remember(context) { FeatureAccessManager.getInstance(context).isPro() }
    val overflowCount = domains.size

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Expiring / Grade F Alert Banner
        if (expiringCount > 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = AppIcons.warning(),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "⚠️ $expiringCount SSL certificate issue(s) detected",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Expiring soon or Grade F security issues in monitored domains",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Soft Downgrade Overflow Banner (if Free tier and domain count > 4)
        if (!isProUser && overflowCount > 4) {
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
                            text = "You currently have $overflowCount monitored domains (Free limit is 4). All your existing domains remain active and safe. Re-subscribe to NeTez PRO to add new domains.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // Quick Input Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = viewModel.inputQuery,
                            onValueChange = { viewModel.onQueryChange(it) },
                            placeholder = { Text(stringResource(R.string.ssl_domain_placeholder), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Button(
                            onClick = { viewModel.addDomain(viewModel.inputQuery) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !viewModel.isScanning,
                            modifier = Modifier.height(56.dp)
                        ) {
                            if (viewModel.isScanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(painter = AppIcons.add(), contentDescription = "Add")
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onShowBulkAdd,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(painter = AppIcons.download(), contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.ssl_btn_bulk_add), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        TextButton(
                            onClick = { viewModel.recheckAll() },
                            enabled = !viewModel.isScanning && domains.isNotEmpty(),
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(painter = AppIcons.refresh(), contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Recheck All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (viewModel.isScanning && viewModel.scanProgress.second > 0) {
                        LinearProgressIndicator(
                            progress = { viewModel.scanProgress.first.toFloat() / viewModel.scanProgress.second },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                        Text(
                            text = stringResource(R.string.ssl_check_progress, viewModel.scanProgress.first, viewModel.scanProgress.second),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Empty State or List
        if (domains.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            painter = AppIcons.shield(),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = stringResource(R.string.ssltls_empty_my_domains),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            }
        } else {
            // Filter Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChipItem("All", domains.size, viewModel.filterState == "All") { viewModel.filterState = "All" }
                    FilterChipItem("Expiring", domains.count { it.realDaysLeft in 1..14 }, viewModel.filterState == "Expiring", MaterialTheme.colorScheme.tertiary) { viewModel.filterState = "Expiring" }
                    FilterChipItem("Expired", domains.count { it.realDaysLeft <= 0 }, viewModel.filterState == "Expired", MaterialTheme.colorScheme.error) { viewModel.filterState = "Expired" }
                    FilterChipItem("Grade F", domains.count { it.grade == "F" }, viewModel.filterState == "Grade F", MaterialTheme.colorScheme.error) { viewModel.filterState = "Grade F" }
                    FilterChipItem("Valid", domains.count { it.realDaysLeft > 14 && it.grade != "F" }, viewModel.filterState == "Valid", MaterialTheme.colorScheme.secondary) { viewModel.filterState = "Valid" }
                }
            }

            // Monitored Domains Items
            items(filteredDomains, key = { it.domain }) { domain ->
                SslDomainLabCard(
                    domain = domain,
                    onClick = { onSelectDomain(domain) },
                    onDelete = { onDeleteDomain(domain) }
                )
            }
        }
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    count: Int,
    selected: Boolean,
    dotColor: Color? = null,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (dotColor != null) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(dotColor))
                    Spacer(Modifier.width(6.dp))
                }
                Text("$label ($count)", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
private fun SslDomainLabCard(
    domain: SslDomain,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val realDays = domain.realDaysLeft

    val gradeColor = when (domain.grade) {
        "A+", "A" -> MaterialTheme.colorScheme.secondary
        "B" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }

    val expiryColor = when {
        realDays <= 0 -> MaterialTheme.colorScheme.error
        realDays <= 14 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.secondary
    }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Grade Badge
                Surface(
                    color = gradeColor.copy(alpha = 0.15f),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, gradeColor)
                ) {
                    Box(
                        modifier = Modifier.size(42.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = domain.grade.ifEmpty { "A+" },
                            color = gradeColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = domain.domain,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = TechMonospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Issuer: ${domain.issuer}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (realDays <= 0) "Expired" else "$realDays Days",
                        color = expiryColor,
                        fontFamily = TechMonospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = domain.tlsVersion,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun InstantCheckTabContent(viewModel: SslReminderViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Instant SSL/TLS Analysis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = viewModel.instantInput,
                            onValueChange = { viewModel.onInstantInputChange(it) },
                            placeholder = { Text("e.g. google.com or github.com", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Button(
                            onClick = { viewModel.performInstantCheck() },
                            enabled = !viewModel.isInstantChecking && viewModel.instantInput.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.height(56.dp)
                        ) {
                            if (viewModel.isInstantChecking) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                            } else {
                                Text("ANALYZE", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        viewModel.instantError?.let { err ->
            item {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Error: $err",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(14.dp),
                        fontSize = 13.sp
                    )
                }
            }
        }

        viewModel.instantResult?.let { res ->
            item {
                InstantResultCard(
                    result = res,
                    onAddToMyDomains = { viewModel.addInstantResultToMyDomains() },
                    onSaveToQuickNotes = { viewModel.saveToQuickNotes() },
                    onShare = { context, text -> viewModel.shareReport(context, text) },
                    onCopy = { context, text -> viewModel.copyToClipboard(context, text) },
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
private fun InstantResultCard(
    result: SslTlsEngine.SslTlsResult,
    onAddToMyDomains: () -> Unit,
    onSaveToQuickNotes: () -> Unit,
    onShare: (Context, String) -> Unit,
    onCopy: (Context, String) -> Unit,
    viewModel: SslReminderViewModel
) {
    val context = LocalContext.current
    val gradeColor = when (result.grade.letter) {
        "A+", "A" -> MaterialTheme.colorScheme.secondary
        "B" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(result.domain, fontSize = 20.sp, fontFamily = TechMonospace, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("Issuer: ${result.certificate.issuer}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    color = gradeColor.copy(alpha = 0.15f),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, gradeColor)
                ) {
                    Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                        Text(result.grade.letter, color = gradeColor, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ResultRow(label = "Certificate Status", value = if (result.certificate.valid) "Valid 🟢" else "Expired/Invalid 🔴", isTechnicalData = true)
                ResultRow(label = "Days Remaining", value = "${result.certificate.expiresInDays} Days", isTechnicalData = true)
                ResultRow(label = "TLS Version", value = if (result.tlsVersions.tls13) "TLS 1.3" else "TLS 1.2", isTechnicalData = true)
                ResultRow(label = "Cipher Suite", value = result.cipherSuites.firstOrNull()?.name ?: "N/A", isTechnicalData = true)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAddToMyDomains,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("MONITOR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { onCopy(context, viewModel.getShareText(result)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("COPY", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { onShare(context, viewModel.getShareText(result)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("SHARE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun BulkAddDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Bulk Add Domains", color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Enter domain names separated by commas or new lines:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("google.com\ncloudflare.com\ngithub.com") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = { if (text.isNotBlank()) onConfirm(text) }) {
                Text(stringResource(R.string.btn_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) }
        }
    )
}
