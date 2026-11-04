package com.herwin.netez.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.network.DomainInfo
import com.herwin.netez.network.IpInfo
import com.herwin.netez.network.WhoisResult
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.components.ResultRow
import com.herwin.netez.ui.theme.TechMonospace
import com.herwin.netez.ui.viewmodels.WhoisViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhoisScreen(viewModel: WhoisViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.error) {
        uiState.error?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Top Input Card Section
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
            tonalElevation = 2.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = { viewModel.updateQuery(it) },
                    placeholder = {
                        Text(
                            text = "e.g. 8.8.8.8 or google.com",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isLoading,
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = { viewModel.fetchInfo() }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    ),
                    trailingIcon = {
                        if (uiState.query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateQuery("") }) {
                                Icon(
                                    painter = AppIcons.close(),
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                )

                OutlinedButton(
                    onClick = { viewModel.fetchMyIpInfo() },
                    enabled = !uiState.isLoading,
                    modifier = Modifier.height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Text("MY IP", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Button(
                    onClick = { viewModel.fetchInfo() },
                    enabled = !uiState.isLoading,
                    modifier = Modifier.height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            painter = AppIcons.search(),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "LOOKUP",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // 2. Result Content
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.result != null && uiState.result !is WhoisResult.Error) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Whois & IP Information",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Row {
                            IconButton(onClick = {
                                viewModel.copyResultsToClipboard()
                                Toast.makeText(context, "Results copied to clipboard", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(
                                    painter = AppIcons.contentCopy(),
                                    contentDescription = "Copy Results",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = {
                                viewModel.shareResults(context)
                            }) {
                                Icon(
                                    painter = AppIcons.share(),
                                    contentDescription = "Share Results",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            when (val res = uiState.result) {
                is WhoisResult.Ip -> {
                    item { IpResultLayout(res.info) }
                }
                is WhoisResult.Domain -> {
                    item { DomainResultLayout(res.info) }
                }
                is WhoisResult.Error -> {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = res.message,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(14.dp),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
                null -> {
                    if (!uiState.isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 80.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        painter = AppIcons.search(),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        text = "Enter an IP or Domain above to lookup",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IpResultLayout(info: IpInfo) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Location Card
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
                WhoisSectionHeader("LOCATION", AppIcons.public())
                ResultRow("Country", "${info.country} ${getFlagEmoji(info.countryCode)}", showCopyIcon = false)
                ResultRow("Region / City", "${info.regionName}, ${info.city}", showCopyIcon = false)
                ResultRow("Zip Code", info.zip, showCopyIcon = false, isTechnicalData = true)
                ResultRow("Timezone", info.timezone, showCopyIcon = false)
                ResultRow("Coordinates", "${info.lat}, ${info.lon}", showCopyIcon = false, isTechnicalData = true)

                Button(
                    onClick = {
                        val gmmIntentUri = Uri.parse("geo:${info.lat},${info.lon}?q=${info.lat},${info.lon}")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                        context.startActivity(mapIntent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(painter = AppIcons.map(), contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("OPEN IN MAPS", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // Network Card
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
                WhoisSectionHeader("NETWORK", AppIcons.dns())
                ResultRow("ISP", info.isp, showCopyIcon = false)
                ResultRow("Organization", info.org, showCopyIcon = false)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ASN", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = info.asn,
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = TechMonospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                ResultRow("Reverse DNS", info.reverse, showCopyIcon = false, isTechnicalData = true)
            }
        }

        // Security / Technical Chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WhoisStatusChip("Mobile", info.mobile)
            WhoisStatusChip("Proxy", info.proxy)
            WhoisStatusChip("Hosting", info.hosting, positiveColor = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
fun DomainResultLayout(info: DomainInfo) {
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
                Text(
                    text = info.domain.uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = TechMonospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                    shape = CircleShape
                ) {
                    Text(
                        text = "ACTIVE",
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            WhoisSectionHeader("REGISTRATION", AppIcons.description())
            ResultRow("Registrar", info.registrar, showCopyIcon = false)
            ResultRow("Handle", info.handle, showCopyIcon = false, isTechnicalData = true)
            ResultRow("Created", info.creationDate.substringBefore("T"), showCopyIcon = false, isTechnicalData = true)
            ResultRow("Expires", info.expirationDate.substringBefore("T"), showCopyIcon = false, isTechnicalData = true)

            WhoisSectionHeader("NAMESERVERS", AppIcons.dns())
            info.nameservers.forEach { ns ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = AppIcons.dns(),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = ns,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = TechMonospace,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun WhoisSectionHeader(title: String, icon: Painter) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun WhoisStatusChip(label: String, active: Boolean, positiveColor: Color = MaterialTheme.colorScheme.primary) {
    Surface(
        color = if (active) positiveColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (active) positiveColor.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Text(
            text = "$label: ${if (active) "Yes" else "No"}",
            color = if (active) positiveColor else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

fun getFlagEmoji(countryCode: String): String {
    if (countryCode.length != 2) return ""
    val firstLetter = Character.codePointAt(countryCode, 0) - 0x41 + 0x1F1E6
    val secondLetter = Character.codePointAt(countryCode, 1) - 0x41 + 0x1F1E6
    return String(Character.toChars(firstLetter)) + String(Character.toChars(secondLetter))
}
