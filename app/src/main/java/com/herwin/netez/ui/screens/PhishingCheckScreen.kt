package com.herwin.netez.ui.screens

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
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.theme.TechMonospace
import com.herwin.netez.ui.viewmodels.PhishingCheckItem
import com.herwin.netez.ui.viewmodels.PhishingCheckViewModel
import com.herwin.netez.ui.viewmodels.PhishingStatus
import com.herwin.netez.workers.UrlHausFeedWorker

import com.herwin.netez.entitlement.FeatureId
import com.herwin.netez.ui.components.ContextualPaywallDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhishingCheckScreen(
    viewModel: PhishingCheckViewModel = viewModel()
) {
    val context = LocalContext.current

    if (viewModel.showPaywallDialog) {
        ContextualPaywallDialog(
            featureId = FeatureId.PHISHING_CHECK_DAILY,
            limitText = "Free plan supports up to 3 Phishing checks per day. Upgrade to NeTez PRO or watch a quick video for +1 bonus check.",
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 0. Feed Status Chip
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable {
                        UrlHausFeedWorker.schedule(context, true)
                        Toast.makeText(context, "Updating feed...", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(
                        text = "URLhaus: Updated ${viewModel.lastUpdateText} • ${viewModel.urlHausCount / 1000}k URLs",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = TechMonospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

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
                            value = viewModel.inputUrl,
                            onValueChange = { viewModel.onUrlChange(it) },
                            placeholder = {
                                Text(
                                    text = "e.g. https://example.com or suspicious-link.com",
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
                                onGo = { viewModel.checkPhishing(viewModel.inputUrl) }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            ),
                            trailingIcon = {
                                if (viewModel.inputUrl.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onUrlChange("") }) {
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
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { viewModel.pasteFromClipboard() },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    painter = AppIcons.contentCopy(),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.phishing_btn_paste),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.checkPhishing(viewModel.inputUrl) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
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

                        Text(
                            text = stringResource(R.string.phishing_privacy_note),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            // 2. Results Header
            if (viewModel.result != null && !viewModel.isScanning) {
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
                                Toast.makeText(context, "Results copied to clipboard", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(
                                    painter = AppIcons.contentCopy(),
                                    contentDescription = "Copy",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { viewModel.shareResults(context) }) {
                                Icon(
                                    painter = AppIcons.share(),
                                    contentDescription = "Share",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // 3. Summary Card
                item {
                    val res = viewModel.result!!
                    val statusColor = when (res.status) {
                        PhishingStatus.PHISHING -> MaterialTheme.colorScheme.error
                        PhishingStatus.SUSPICIOUS -> MaterialTheme.colorScheme.tertiary
                        PhishingStatus.SAFE -> MaterialTheme.colorScheme.secondary
                    }
                    val statusIcon = when (res.status) {
                        PhishingStatus.PHISHING -> AppIcons.error()
                        PhishingStatus.SUSPICIOUS -> AppIcons.warning()
                        PhishingStatus.SAFE -> AppIcons.checkCircle()
                    }
                    val statusText = when (res.status) {
                        PhishingStatus.PHISHING -> stringResource(R.string.phishing_status_phishing)
                        PhishingStatus.SUSPICIOUS -> stringResource(R.string.phishing_status_suspicious)
                        PhishingStatus.SAFE -> stringResource(R.string.phishing_status_safe)
                    }
                    val statusDesc = when (res.status) {
                        PhishingStatus.PHISHING -> stringResource(R.string.phishing_phishing_desc)
                        PhishingStatus.SUSPICIOUS -> stringResource(R.string.phishing_suspicious_desc)
                        PhishingStatus.SAFE -> stringResource(R.string.phishing_safe_desc)
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
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
                                        text = res.domain,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontFamily = TechMonospace,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (res.status == PhishingStatus.PHISHING) {
                                        val threatItem = res.checks.find { it.status == "PHISHING" }
                                        Text(
                                            text = "Verified threat! ${threatItem?.description ?: ""}",
                                            color = statusColor,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        Text(
                                            text = statusDesc,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            // Risk Score
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = stringResource(R.string.phishing_risk_score),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${res.riskScore}/100",
                                        fontSize = 11.sp,
                                        fontFamily = TechMonospace,
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { res.riskScore / 100f },
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

                // 4. Detail Layers
                items(viewModel.result?.checks ?: emptyList()) { check ->
                    PhishingCheckItemCard(
                        item = check,
                        onCopy = {
                            viewModel.copyItemInfo(context, check)
                            Toast.makeText(context, "Item info copied", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // Education Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.phishing_education_title),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.phishing_education_content),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
fun PhishingCheckItemCard(
    item: PhishingCheckItem,
    onCopy: () -> Unit
) {
    val context = LocalContext.current
    val statusColor = when (item.status) {
        "LISTED", "PHISHING", "NO SSL" -> MaterialTheme.colorScheme.error
        "SUSPICIOUS" -> MaterialTheme.colorScheme.tertiary
        "CLEAN", "SAFE", "VALID" -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(statusColor))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.name,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (item.isVerified) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "VERIFIED",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 9.sp,
                                    fontFamily = TechMonospace,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "${item.status} (${item.latency}ms)",
                        color = statusColor,
                        fontFamily = TechMonospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.description,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }

                IconButton(
                    onClick = onCopy,
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

            if (item.detailUrl != null) {
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.detailUrl))
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("View on URLhaus", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
