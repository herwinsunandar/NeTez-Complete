package com.herwin.netez.ui.viewmodels

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.network.SslCertificateInfo
import com.herwin.netez.network.SslManager
import kotlinx.coroutines.launch

class SslCheckerViewModel(application: Application) : AndroidViewModel(application) {
    private val sslManager = SslManager()

    var target by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var result by mutableStateOf<SslCertificateInfo?>(null)
    var error by mutableStateOf<String?>(null)

    fun checkSsl() {
        if (target.isBlank()) {
            error = "Please enter a host"
            return
        }

        val parts = target.split(":")
        val host = parts[0]
        val port = if (parts.size > 1) parts[1].toIntOrNull() ?: 443 else 443

        viewModelScope.launch {
            isLoading = true
            error = null
            result = null
            try {
                result = sslManager.checkSsl(host, port)
            } catch (e: Exception) {
                error = e.localizedMessage ?: "Failed to connect"
            } finally {
                isLoading = false
            }
        }
    }

    fun getShareText(): String {
        val res = result ?: return ""
        return """
            ⚡ NeTez SSL/TLS Certificate Report
            ─────────────────────
            🎯 Domain: ${res.host}
            🛡️ Status: ${if (res.isValid) "🟢 VALID" else "🔴 EXPIRED/INVALID"}
            ⏳ Days Remaining: ${res.expiryDays}
            
            📅 Timeline:
            • Issued: ${res.issuedDate}
            • Expires: ${res.expiryDate}
            
            🔒 Security:
            • Protocol: ${res.protocol}
            • Cipher: ${res.cipherSuite}
            • HSTS: ${if (res.hstsEnabled == true) "Enabled" else "Disabled"}
            
            📜 Details:
            • Subject: ${res.subject}
            • Issuer: ${res.issuer}
            • Algorithm: ${res.signatureAlgorithm}
            • Serial No: ${res.serialNumber}
            ${if (res.san.isNotEmpty()) "• SAN: ${res.san.joinToString(", ")}" else ""}
            
            ─────────────────────
            Sent from NeTez Network Tools
        """.trimIndent()
    }

    fun copyToClipboard() {
        val context = getApplication<Application>()
        val text = getShareText()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("SSL Certificate Info", text)
        clipboard.setPrimaryClip(clip)
    }

    fun shareResults(context: Context) {
        val text = getShareText()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "SSL Certificate Info - ${result?.host}")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share SSL Results"))
    }
}
