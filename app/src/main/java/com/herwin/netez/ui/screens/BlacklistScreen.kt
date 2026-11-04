package com.herwin.netez.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.R
import com.herwin.netez.network.BlacklistCategory
import com.herwin.netez.network.BlacklistResult
import com.herwin.netez.network.BlacklistStatus
import com.herwin.netez.network.api.AbuseData
import com.herwin.netez.network.api.ShodanResponse
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.theme.TechMonospace
import com.herwin.netez.ui.viewmodels.BlacklistViewModel

import com.herwin.netez.entitlement.FeatureId
import com.herwin.netez.ui.components.ContextualPaywallDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlacklistScreen(
    viewModel: BlacklistViewModel = viewModel()
) {
    val context = LocalContext.current

    if (viewModel.showPaywallDialog) {
        ContextualPaywallDialog(
            featureId = FeatureId.BLACKLIST_CHECK_DAILY,
            limitText = "Free plan supports up to 3 Blacklist checks per day. Upgrade to NeTez PRO or watch a video for +1 bonus check.",
            onDismiss = { viewModel.showPaywallDialog = false },
            onUpgradeClick = {}
        )
    }

    if (viewModel.showGlobalSupportDialog) {
        ContextualPaywallDialog(
            featureId = FeatureId.GLOBAL_TOOL_USAGE,
            limitText = "You've performed 3 network & security scans! If NeTez is helpful, consider watching a short video to support development or upgrade to PRO (Skippable).",
            onDismiss = { viewModel.showGlobalSupportDialog = false },
            onUpgradeClick = {}
        )
    }

    LaunchedEffect(Unit) {
        viewModel.fetchMyIp()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Input Card
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
                        OutlinedTextField(
                            value = viewModel.inputQuery,
                            onValueChange = { viewModel.onQueryChange(it) },
                            placeholder = {
                                Text(
                                    text = "e.g. 8.8.8.8 or google.com",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    fontSize = 13.sp
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !viewModel.isScanning,
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Go
                            ),
                            keyboardActions = KeyboardActions(
                                onGo = { viewModel.checkIp(viewModel.inputQuery) }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            ),
                            trailingIcon = {
                                if (viewModel.inputQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onQueryChange("") }) {
                                        Icon(
                                            painter = AppIcons.close(),
                                            contentDescription = "Clear",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Detected: ${viewModel.detectedType}",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontFamily = TechMonospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            TextButton(
                                onClick = { viewModel.fetchMyIp() },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    painter = AppIcons.locationOn(),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.blacklist_btn_check_my_ip),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.checkIp(viewModel.inputQuery) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            enabled = !viewModel.isScanning
                        ) {
                            if (viewModel.isScanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    painter = AppIcons.search(),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.blacklist_btn_check),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // Error Message
            if (viewModel.error != null && !viewModel.isScanning) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = AppIcons.error(),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = viewModel.error!!,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Results Toolbar (Copy & Share)
            if (viewModel.results.isNotEmpty() && !viewModel.isScanning) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Scan Results",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row {
                            IconButton(onClick = {
                                viewModel.copyToClipboard()
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
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
            }

            // AbuseIPDB Card
            if (viewModel.abuseIpDbResponse != null && !viewModel.isScanning) {
                item {
                    viewModel.abuseIpDbResponse?.let {
                        AbuseIpDbCard(it.data)
                    }
                }
            }

            // Shodan Card
            if (viewModel.shodanResponse != null && !viewModel.isScanning) {
                item {
                    viewModel.shodanResponse?.let {
                        ShodanCard(it)
                    }
                }
            }

            // Status Summary Card
            if (viewModel.results.isNotEmpty() || viewModel.isScanning) {
                item {
                    val isListed = viewModel.results.any { it.status == BlacklistStatus.LISTED }
                    val statusText = if (isListed) "LISTED" else "CLEAN"
                    val statusColor = when {
                        viewModel.riskScore > 50 -> MaterialTheme.colorScheme.error
                        viewModel.riskScore > 20 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.secondary
                    }
                    val statusIcon = if (isListed) AppIcons.warning() else AppIcons.checkCircle()

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        tonalElevation = 2.dp
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Surface(
                                    color = statusColor.copy(alpha = 0.15f),
                                    shape = CircleShape,
                                    border = BorderStroke(1.dp, statusColor)
                                ) {
                                    Box(
                                        modifier = Modifier.size(44.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = statusIcon,
                                            contentDescription = null,
                                            tint = statusColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = statusText,
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                    Text(
                                        text = viewModel.inputQuery,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontFamily = TechMonospace,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )

                                    if (viewModel.detectedType == "Domain" && viewModel.resolvedIp != null) {
                                        Text(
                                            text = "Resolved IP: ${viewModel.resolvedIp}",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontFamily = TechMonospace,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    viewModel.ipInfo?.let { info ->
                                        Text(
                                            text = "ISP: ${info.isp} • ${info.city}, ${info.countryCode}",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            // Risk Score Bar
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Aggregated Risk Score",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${viewModel.riskScore.toInt()}/100",
                                        fontSize = 11.sp,
                                        fontFamily = TechMonospace,
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { viewModel.riskScore / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape),
                                    color = statusColor,
                                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }

            // Summary Chip Row
            if (viewModel.results.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val listedCount = viewModel.results.count { it.status == BlacklistStatus.LISTED }
                        val cleanCount = viewModel.results.count { it.status == BlacklistStatus.CLEAN }
                        val timeoutCount = viewModel.results.count { it.status == BlacklistStatus.TIMEOUT }

                        SummaryStatusChip("$cleanCount Clean", MaterialTheme.colorScheme.secondary, true)
                        SummaryStatusChip("$listedCount Listed", if (listedCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant, listedCount > 0)
                        if (timeoutCount > 0) {
                            SummaryStatusChip("$timeoutCount Rate Limit", MaterialTheme.colorScheme.tertiary, true)
                        }
                    }
                }
            }

            // Detailed RBL Results
            val domainResults = viewModel.results.filter { it.category == BlacklistCategory.DOMAIN }
            val ipResults = viewModel.results.filter { it.category == BlacklistCategory.IP }

            if (domainResults.isNotEmpty()) {
                item { BlacklistSectionHeader("Domain Blacklists") }
                items(domainResults) { ProRblResultItem(it) }
            }

            if (ipResults.isNotEmpty()) {
                item { BlacklistSectionHeader(if (viewModel.detectedType == "Domain") "IP Blacklists (Resolved)" else "IP Blacklists") }
                items(ipResults) { ProRblResultItem(it) }
            }

            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
fun BlacklistSectionHeader(title: String) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.primary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun AbuseIpDbCard(data: AbuseData) {
    val statusColor = when {
        data.abuseConfidenceScore > 50 -> MaterialTheme.colorScheme.error
        data.abuseConfidenceScore > 20 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.secondary
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = AppIcons.security(),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "AbuseIPDB Intelligence",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp
                )
                Spacer(Modifier.weight(1f))
                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "${data.abuseConfidenceScore}% Confidence",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        color = statusColor,
                        fontFamily = TechMonospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AbuseInfoItem("Reports", data.totalReports.toString())
                AbuseInfoItem("Country", data.countryCode)
                AbuseInfoItem("Usage", data.usageType ?: "Unknown")
            }

            if (!data.reports.isNullOrEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Latest Reports:",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                data.reports.take(3).forEach { report ->
                    Text(
                        text = "• ${report.reportedAt.substringBefore("T")}: ${report.comment}",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AbuseInfoItem(label: String, value: String) {
    Column {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        Text(text = value, color = MaterialTheme.colorScheme.onSurface, fontFamily = TechMonospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ShodanCard(data: ShodanResponse) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = AppIcons.cloud(),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Shodan Open Ports",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            if (data.ports.isEmpty()) {
                Text(
                    text = "No open ports found in Shodan database.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            } else {
                Text(
                    text = "Detected Ports: ${data.ports.joinToString(", ")}",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = TechMonospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(Modifier.height(8.dp))

                data.data?.take(5)?.forEach { service ->
                    Text(
                        text = "• Port ${service.port}/${service.transport}: ${service.product ?: "Unknown"} ${service.version ?: ""}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = TechMonospace,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

            if (data.os != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "OS: ${data.os}",
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SummaryStatusChip(label: String, color: Color, isHighlighted: Boolean) {
    Surface(
        color = if (isHighlighted) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp),
        border = if (isHighlighted) BorderStroke(1.dp, color.copy(alpha = 0.3f)) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                color = if (isHighlighted) color else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ProRblResultItem(result: BlacklistResult) {
    val isListed = result.status == BlacklistStatus.LISTED
    val isTimeout = result.status == BlacklistStatus.TIMEOUT
    val statusColor = when (result.status) {
        BlacklistStatus.LISTED -> MaterialTheme.colorScheme.error
        BlacklistStatus.CLEAN -> MaterialTheme.colorScheme.secondary
        BlacklistStatus.TIMEOUT -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (isListed) statusColor.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = result.name,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = when (result.status) {
                            BlacklistStatus.LISTED -> "LISTED - ${result.reason ?: "Unknown"}"
                            BlacklistStatus.TIMEOUT -> "TIMEOUT (Rate Limit)"
                            BlacklistStatus.CLEAN -> "CLEAN (${result.responseTime}ms)"
                            else -> "ERROR"
                        },
                        color = if (isListed) MaterialTheme.colorScheme.error else if (isTimeout) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontFamily = TechMonospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("RBL Result", "${result.name}: ${result.status}"))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        painter = AppIcons.contentCopy(),
                        contentDescription = "Copy",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (result.resultIp != null) {
                Row(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Result: ${result.resultIp}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = TechMonospace,
                        fontSize = 11.sp
                    )

                    if (isListed) {
                        Surface(
                            modifier = Modifier.clickable {
                                val url = when {
                                    result.domain.contains("spamhaus") -> "https://check.spamhaus.org/"
                                    result.domain.contains("spamcop") -> "https://www.spamcop.net/bl.shtml"
                                    else -> "https://www.google.com/search?q=delist+${result.domain}"
                                }
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            },
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "Delist →",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
