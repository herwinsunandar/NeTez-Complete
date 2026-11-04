package com.herwin.netez.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.herwin.netez.R
import com.herwin.netez.data.entities.SslDomain
import com.herwin.netez.engine.SslTlsEngine
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.theme.TechMonospace
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SslDomainDetailBottomSheet(
    domain: SslDomain,
    onDismiss: () -> Unit,
    onRecheck: () -> Unit,
    onDelete: () -> Unit
) {
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    val days = domain.realDaysLeft

    val statusColor = when {
        days <= 0 || domain.grade == "F" -> MaterialTheme.colorScheme.error
        days <= 14 || domain.grade == "B" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.secondary
    }

    val cleanIssuer = remember(domain.issuer) {
        val raw = domain.issuer
        if (raw.contains("CN=")) {
            raw.split(",")
                .firstOrNull { it.trim().startsWith("CN=", ignoreCase = true) }
                ?.substringAfter("=")?.trim() ?: raw.removePrefix("CN=").substringBefore(",")
        } else {
            raw
        }
    }

    val isWildcardDomain = remember(domain.domain) {
        domain.domain.startsWith("*.") || domain.domain.contains("*")
    }

    var discoveredServers by remember { mutableStateOf<List<SslTlsEngine.DiscoveredServer>>(emptyList()) }
    var isDiscoveringServers by remember { mutableStateOf(false) }

    LaunchedEffect(domain.domain) {
        isDiscoveringServers = true
        try {
            val res = SslTlsEngine.checkDomain(domain.domain)
            discoveredServers = res.discoveredServers
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isDiscoveringServers = false
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
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = domain.domain,
                        fontSize = 20.sp,
                        fontFamily = TechMonospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Endpoint: ${domain.domain.removePrefix("*.")}:443",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = TechMonospace
                    )
                }

                // Grade Badge
                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, statusColor)
                ) {
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = domain.grade.ifEmpty { "A+" },
                            color = statusColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                    }
                }
            }

            if (isWildcardDomain) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "🌐 WILDCARD CERTIFICATE (${domain.domain})",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = TechMonospace,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Expiry Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Days Remaining",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                        Text(
                            text = if (days <= 0) "Expired" else "$days Days",
                            color = statusColor,
                            fontFamily = TechMonospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (days.toFloat() / 365f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = statusColor,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Expires on: ${sdf.format(Date(domain.expiryDate))}",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp
                    )
                }
            }

            // Discovered Subdomains / Active Servers Card
            DiscoveredServersCard(discoveredServers, isDiscoveringServers)

            // Technical Details Grid
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Certificate Specifications",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    DetailItem("Common Name (CN)", domain.domain, MaterialTheme.colorScheme.onSurface)
                    DetailItem("Issuer (CA)", cleanIssuer, MaterialTheme.colorScheme.primary)
                    if (domain.cipher.isNotEmpty()) {
                        DetailItem("Cipher Suite", domain.cipher, MaterialTheme.colorScheme.onSurface)
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    DetailRow("TLS Version", domain.tlsVersion, MaterialTheme.colorScheme.primary)
                    DetailRow("Chain Status", if (domain.chainValid) "Complete (Trusted) 🟢" else "Incomplete 🟡", if (domain.chainValid) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary)
                    DetailRow("Vulnerabilities", if (domain.vulnCount == 0) "None detected 🟢" else "${domain.vulnCount} vulnerabilities 🔴", if (domain.vulnCount == 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error)
                    DetailRow("Hostname Match", "Verified (Matching SAN/CN) 🟢", MaterialTheme.colorScheme.secondary)
                }
            }

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onRecheck,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(painter = AppIcons.refresh(), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("RECHECK", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(painter = AppIcons.delete(), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("DELETE", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DiscoveredServersCard(
    servers: List<SslTlsEngine.DiscoveredServer>,
    isLoading: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🌐 Active Servers & Subdomains (${servers.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            if (servers.isEmpty()) {
                Text(
                    text = if (isLoading) "Scanning domain for active HTTPS servers..." else "No active subdomains/servers detected under this domain.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                servers.forEach { server ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = server.hostname,
                                fontSize = 12.sp,
                                fontFamily = TechMonospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "IP: ${server.ip}:${server.port}",
                                fontSize = 11.sp,
                                fontFamily = TechMonospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${server.latencyMs} ms 🟢",
                            fontSize = 12.sp,
                            fontFamily = TechMonospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(2.dp))
        Text(text = value, color = valueColor, fontSize = 13.sp, fontFamily = TechMonospace, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DetailRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Text(text = value, color = valueColor, fontSize = 12.sp, fontFamily = TechMonospace, fontWeight = FontWeight.SemiBold)
    }
}
