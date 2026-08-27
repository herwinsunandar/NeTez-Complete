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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.network.CertificateChainNode
import com.herwin.netez.network.SslCertificateInfo
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
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SSL/TLS Checker", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    if (viewModel.result != null) {
                        IconButton(onClick = { copySslToClipboard(context, viewModel.result!!) }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.White)
                        }
                        IconButton(onClick = { shareSslResult(context, viewModel.result!!) }) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PremiumNavy)
            )
        },
        containerColor = PremiumNavy
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = viewModel.target,
                onValueChange = { viewModel.target = it },
                label = { Text("Host (e.g. google.com:443)", color = TextGray) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedTextColor = Color.White,
                    focusedTextColor = Color.White,
                    cursorColor = Color.White,
                    focusedBorderColor = Color(0xFF4F46E5),
                    unfocusedBorderColor = SoftDarkBg
                ),
                trailingIcon = {
                    IconButton(onClick = { viewModel.checkSsl() }) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (viewModel.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF4F46E5))
                }
            } else if (viewModel.error != null) {
                Text(viewModel.error!!, color = Color.Red, modifier = Modifier.padding(8.dp))
            } else if (viewModel.result != null) {
                SslResultContent(viewModel.result!!)
            }
        }
    }
}

@Composable
fun SslResultContent(result: SslCertificateInfo) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
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
        item {
            val context = LocalContext.current
            com.herwin.netez.ui.components.ResultFooter(
                fullTextToCopy = formatSslResult(result),
                onCopyAll = { copySslToClipboard(context, result) }
            )
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
            
            // Simplified timeline view
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TimelineItem("Issued", result.issuedDate, Icons.Default.CheckCircle, Color(0xFF10B981))
                TimelineItem("Expires", result.expiryDate, Icons.Default.Event, Color(0xFFF59E0B))
            }
        }
    }
}

@Composable
private fun TimelineItem(label: String, date: String, icon: ImageVector, color: Color) {
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
            com.herwin.netez.ui.components.ResultRow("Subject", result.subject)
            com.herwin.netez.ui.components.ResultRow("Issuer", result.issuer)
            com.herwin.netez.ui.components.ResultRow("Algorithm", result.signatureAlgorithm)
            com.herwin.netez.ui.components.ResultRow("Public Key", result.publicKeyInfo)
            com.herwin.netez.ui.components.ResultRow("Serial No", result.serialNumber)
            if (result.san.isNotEmpty()) {
                com.herwin.netez.ui.components.ResultRow("SAN", result.san.joinToString(", "))
            }
        }
    }
}

private fun copySslToClipboard(context: Context, result: SslCertificateInfo) {
    val text = formatSslResult(result)
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("SSL Certificate Info", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
}

private fun shareSslResult(context: Context, result: SslCertificateInfo) {
    val text = formatSslResult(result)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "SSL Certificate Info - ${result.host}")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share Result"))
}

private fun formatSslResult(result: SslCertificateInfo): String {
    return """
        SSL/TLS Certificate Checker Result
        ----------------------------------
        Domain: ${result.host}
        Status: ${if (result.isValid) "VALID" else "EXPIRED/INVALID"}
        Days Remaining: ${result.expiryDays}
        
        Timeline:
        - Issued: ${result.issuedDate}
        - Expires: ${result.expiryDate}
        
        Security:
        - Protocol: ${result.protocol}
        - Cipher: ${result.cipherSuite}
        - HSTS: ${if (result.hstsEnabled == true) "Enabled" else "Disabled"}
        
        Details:
        - Subject: ${result.subject}
        - Issuer: ${result.issuer}
        - Algorithm: ${result.signatureAlgorithm}
        - Public Key: ${result.publicKeyInfo}
        - Serial No: ${result.serialNumber}
        ${if (result.san.isNotEmpty()) "- SAN: ${result.san.joinToString(", ")}" else ""}
        
        Generated by NeTez App
    """.trimIndent()
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
                        if (index == 0) Icons.Default.Lock else Icons.Default.Shield,
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
