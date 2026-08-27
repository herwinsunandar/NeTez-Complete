package com.herwin.netez.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.network.DomainInfo
import com.herwin.netez.network.IpInfo
import com.herwin.netez.network.WhoisResult
import com.herwin.netez.ui.theme.PremiumNavy
import com.herwin.netez.ui.theme.SoftDarkBg
import com.herwin.netez.ui.theme.TextGray
import com.herwin.netez.ui.viewmodels.WhoisViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhoisScreen(viewModel: WhoisViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PremiumNavy)
    ) {
        // 1. Top Input Section
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SoftDarkBg,
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = { viewModel.updateQuery(it) },
                    placeholder = { Text("IP or Domain", color = Color(0xFF9CA3AF)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                        focusedContainerColor = Color.White.copy(alpha = 0.05f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        focusedBorderColor = Color(0xFF6C8CFF)
                    ),
                    singleLine = true
                )
                Button(
                    onClick = { viewModel.fetchInfo() },
                    enabled = !uiState.isLoading,
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C8CFF))
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("FETCH", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        // 2. Result Content
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            when (val res = uiState.result) {
                is WhoisResult.Ip -> {
                    item { IpResultLayout(res.info) }
                    item {
                        com.herwin.netez.ui.components.ResultFooter(
                            fullTextToCopy = res.info.toString(),
                            onCopyAll = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Whois Data", res.info.toString()))
                                Toast.makeText(context, "All data copied", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
                is WhoisResult.Domain -> {
                    item { DomainResultLayout(res.info) }
                    item {
                        com.herwin.netez.ui.components.ResultFooter(
                            fullTextToCopy = res.info.raw,
                            onCopyAll = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Whois Data", res.info.raw))
                                Toast.makeText(context, "All data copied", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
                is WhoisResult.Error -> {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFEF4444).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f))
                        ) {
                            Text(res.message, color = Color(0xFFEF4444), modifier = Modifier.padding(16.dp), fontSize = 14.sp)
                        }
                    }
                }
                null -> {
                    if (!uiState.isLoading) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(top = 100.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Search, null, tint = TextGray.copy(alpha = 0.2f), modifier = Modifier.size(80.dp))
                                    Spacer(Modifier.height(16.dp))
                                    Text("Enter an IP or Domain to lookup", color = TextGray)
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
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        // Location Card
        Surface(shape = RoundedCornerShape(20.dp), color = SoftDarkBg) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                WhoisSectionHeader("LOCATION", Icons.Default.LocationOn)
                com.herwin.netez.ui.components.ResultRow("Country", "${info.country} ${getFlagEmoji(info.countryCode)}")
                com.herwin.netez.ui.components.ResultRow("Region / City", "${info.regionName}, ${info.city}")
                com.herwin.netez.ui.components.ResultRow("Zip Code", info.zip)
                com.herwin.netez.ui.components.ResultRow("Timezone", info.timezone)
                com.herwin.netez.ui.components.ResultRow("Coordinates", "${info.lat}, ${info.lon}")
                
                Button(
                    onClick = {
                        val gmmIntentUri = Uri.parse("geo:${info.lat},${info.lon}?q=${info.lat},${info.lon}")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                        context.startActivity(mapIntent)
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6C8CFF).copy(alpha = 0.15f),
                        contentColor = Color(0xFF6C8CFF)
                    )
                ) {
                    Icon(Icons.Default.Map, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("OPEN MAPS", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Network Card
        Surface(shape = RoundedCornerShape(20.dp), color = SoftDarkBg) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                WhoisSectionHeader("NETWORK", Icons.Default.Dns)
                com.herwin.netez.ui.components.ResultRow("ISP", info.isp)
                com.herwin.netez.ui.components.ResultRow("Organization", info.org)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("ASN", style = MaterialTheme.typography.labelSmall, color = TextGray)
                    Surface(color = Color(0xFF6C8CFF).copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                        Text(info.asn, color = Color(0xFF6C8CFF), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
                com.herwin.netez.ui.components.ResultRow("Reverse DNS", info.reverse)
            }
        }

        // Security / Technical Chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WhoisStatusChip("Mobile", info.mobile)
            WhoisStatusChip("Proxy", info.proxy)
            WhoisStatusChip("Hosting", info.hosting, positiveColor = Color(0xFF10B981))
        }
    }
}

@Composable
fun DomainResultLayout(info: DomainInfo) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Surface(shape = RoundedCornerShape(20.dp), color = SoftDarkBg) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(info.domain.uppercase(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color.White)
                    Surface(color = Color(0xFF10B981).copy(alpha = 0.1f), shape = CircleShape) {
                        Text("ACTIVE", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
                
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                WhoisSectionHeader("REGISTRATION", Icons.Default.Description)
                com.herwin.netez.ui.components.ResultRow("Registrar", info.registrar)
                com.herwin.netez.ui.components.ResultRow("Handle", info.handle)
                com.herwin.netez.ui.components.ResultRow("Created", info.creationDate.substringBefore("T"))
                com.herwin.netez.ui.components.ResultRow("Expires", info.expirationDate.substringBefore("T"))
                
                WhoisSectionHeader("NAMESERVERS", Icons.Default.Dns)
                info.nameservers.forEach { ns ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SubdirectoryArrowRight, null, tint = TextGray, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(ns, color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun WhoisSectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color(0xFF6C8CFF), modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.labelSmall, color = Color(0xFF6C8CFF), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
fun WhoisStatusChip(label: String, active: Boolean, positiveColor: Color = Color(0xFF6C8CFF)) {
    Surface(
        color = if (active) positiveColor.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (active) positiveColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f))
    ) {
        Text(
            text = "$label: ${if (active) "Yes" else "No"}",
            color = if (active) positiveColor else TextGray,
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
