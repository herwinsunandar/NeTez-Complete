package com.herwin.netez.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.network.CertificateChainNode
import com.herwin.netez.network.SslCertificateInfo
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.components.ResultRow
import com.herwin.netez.ui.theme.PremiumNavy
import com.herwin.netez.ui.theme.SoftDarkBg
import com.herwin.netez.ui.theme.TextGray
import com.herwin.netez.ui.viewmodels.SslCheckerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SslCheckerScreen(
    viewModel: SslCheckerViewModel = viewModel(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PremiumNavy)
            .padding(16.dp)
    ) {
        // Input Section
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SoftDarkBg,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = viewModel.target,
                    onValueChange = { viewModel.target = it },
                    placeholder = { Text("google.com:443", color = TextGray) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White,
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        focusedBorderColor = Color(0xFF6C8CFF)
                    ),
                    singleLine = true
                )
                Button(
                    onClick = { viewModel.checkSsl() },
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C8CFF)),
                    enabled = !viewModel.isLoading
                ) {
                    if (viewModel.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(AppIcons.search(), null)
                        Spacer(Modifier.width(8.dp))
                        Text("CHECK", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (viewModel.error != null) {
            Surface(
                color = Color.Red.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(viewModel.error!!, color = Color.Red, modifier = Modifier.padding(16.dp), fontSize = 14.sp)
            }
        } else if (viewModel.result != null) {
            SslResultHeader(viewModel, context)
            Spacer(modifier = Modifier.height(16.dp))
            SslResultContent(viewModel.result!!)
        } else if (!viewModel.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Enter a host to check SSL/TLS certificate", color = TextGray)
            }
        }
        
        if (viewModel.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF6C8CFF))
            }
        }
    }
}

@Composable
fun SslResultHeader(viewModel: SslCheckerViewModel, context: Context) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Certificate Result",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Row {
            IconButton(onClick = {
                viewModel.copyToClipboard()
                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            }) {
                Icon(AppIcons.contentCopy(), null, tint = Color(0xFF6C8CFF))
            }
            IconButton(onClick = {
                viewModel.shareResults(context)
            }) {
                Icon(AppIcons.share(), null, tint = Color(0xFF6C8CFF))
            }
        }
    }
}

@Composable
fun SslResultContent(result: SslCertificateInfo) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            HeaderSection(result)
        }
        item {
            TimelineCard(result)
        }
        item {
            SecurityCard(result)
        }
        item {
            DetailsGrid(result)
        }
        item {
            ChainCard(result.chain)
        }
    }
}

@Composable
private fun HeaderSection(result: SslCertificateInfo) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SoftDarkBg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val statusColor = if (result.isValid) Color(0xFF10B981) else Color(0xFFEF4444)
            Surface(
                color = statusColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (result.isValid) "VALID" else "EXPIRED/INVALID",
                    color = statusColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(result.host, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("${result.expiryDays} days remaining", color = TextGray, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun TimelineCard(result: SslCertificateInfo) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SoftDarkBg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Validity Timeline", color = Color.White, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TimelineItem("Issued", result.issuedDate, AppIcons.checkCircle(), Color(0xFF10B981))
                TimelineItem("Expires", result.expiryDate, AppIcons.event(), Color(0xFFF59E0B))
            }
        }
    }
}

@Composable
private fun TimelineItem(label: String, date: String, icon: Painter, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = TextGray, modifier = Modifier.width(60.dp), fontSize = 14.sp)
        Text(date, color = Color.White, fontSize = 14.sp)
    }
}

@Composable
private fun SecurityCard(result: SslCertificateInfo) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SoftDarkBg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Connection Security", color = Color.White, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecurityChip(result.protocol)
                SecurityChip(result.cipherSuite)
                if (result.hstsEnabled == true) {
                    SecurityChip("HSTS Enabled", Color(0xFF10B981))
                }
            }
        }
    }
}

@Composable
private fun SecurityChip(text: String, color: Color = Color(0xFF4F46E5)) {
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = text,
            color = color,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun DetailsGrid(result: SslCertificateInfo) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SoftDarkBg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Certificate Details", color = Color.White, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))
            ResultRow("Subject", result.subject)
            ResultRow("Issuer", result.issuer)
            ResultRow("Algorithm", result.signatureAlgorithm)
            ResultRow("Public Key", result.publicKeyInfo)
            ResultRow("Serial No", result.serialNumber)
            if (result.san.isNotEmpty()) {
                ResultRow("SAN", result.san.joinToString(", "))
            }
        }
    }
}

@Composable
private fun ChainCard(chain: List<CertificateChainNode>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SoftDarkBg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Trust Chain", color = Color.White, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))
            chain.forEachIndexed { index, node ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (index == 0) AppIcons.lock() else AppIcons.shield(),
                        contentDescription = null,
                        tint = if (node.isValid) Color(0xFF10B981) else Color(0xFFEF4444),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        val cn = node.subject.split(",").find { it.startsWith("CN=") }?.removePrefix("CN=") ?: node.subject
                        Text(cn, color = Color.White, fontSize = 14.sp, maxLines = 1)
                        Text("Issued by: ${node.issuer.split(",").find { it.startsWith("CN=") }?.removePrefix("CN=") ?: "Unknown"}", color = TextGray, fontSize = 12.sp, maxLines = 1)
                    }
                }
                if (index < (chain.size - 1)) {
                    Box(modifier = Modifier.padding(start = 9.dp).width(2.dp).height(16.dp).background(Color(0xFF334155)))
                }
            }
        }
    }
}
