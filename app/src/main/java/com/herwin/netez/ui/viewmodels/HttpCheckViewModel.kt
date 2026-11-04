package com.herwin.netez.ui.viewmodels

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.data.AppDatabase
import com.herwin.netez.data.TestResult
import com.herwin.netez.network.HttpCheckManager
import com.herwin.netez.network.HttpCheckResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

data class HttpCheckState(
    val url: String = "",
    val method: String = "GET",
    val result: HttpCheckResult? = null,
    val history: List<HttpCheckResult> = emptyList(),
    val recentHosts: List<String> = listOf("google.com", "facebook.com", "github.com", "microsoft.com"),
    val isLoading: Boolean = false,
    val isHeadersExpanded: Boolean = false,
    val isSslExpanded: Boolean = false,
    val isRawExpanded: Boolean = false
)

class HttpCheckViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(HttpCheckState())
    val uiState: StateFlow<HttpCheckState> = _uiState
    private val dao = AppDatabase.getDatabase(application).testResultDao()

    fun updateUrl(newUrl: String) {
        _uiState.value = _uiState.value.copy(url = newUrl)
    }

    fun updateMethod(newMethod: String) {
        _uiState.value = _uiState.value.copy(method = newMethod)
    }

    fun toggleHeaders() {
        _uiState.value = _uiState.value.copy(isHeadersExpanded = !_uiState.value.isHeadersExpanded)
    }

    fun toggleSsl() {
        _uiState.value = _uiState.value.copy(isSslExpanded = !_uiState.value.isSslExpanded)
    }

    fun toggleRaw() {
        _uiState.value = _uiState.value.copy(isRawExpanded = !_uiState.value.isRawExpanded)
    }

    fun runCheck() {
        var targetUrl = _uiState.value.url.trim().ifBlank { "google.com" }

        if (!targetUrl.contains("http")) {
            targetUrl = "https://$targetUrl"
        }

        _uiState.value = _uiState.value.copy(isLoading = true, url = _uiState.value.url.ifBlank { targetUrl })
        viewModelScope.launch {
            val res = HttpCheckManager.checkWebsite(targetUrl, _uiState.value.method)
            val newHistory = (listOf(res) + _uiState.value.history).take(7)

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                result = res,
                history = newHistory
            )

            saveToDb(res)
        }
    }

    fun getShareText(): String {
        val res = _uiState.value.result ?: return "No HTTP check result available."
        val sb = StringBuilder("🌐 NeTez HTTP Check Report\n")
        sb.append("─────────────────────\n")
        sb.append("🔗 URL: ${res.url}\n")
        sb.append("📡 Status: ${if (res.isAvailable) "ONLINE" else "OFFLINE"} (${res.statusCode})\n")
        sb.append("⚡ Response: ${res.responseTime}ms\n")
        sb.append("🌐 IP: ${res.ipAddress}\n")

        sb.append("\n🕒 Timings:\n")
        sb.append("   • DNS: ${if (res.dnsTime > 0) "${res.dnsTime}ms" else "cached"}\n")
        sb.append("   • Connect: ${if (res.connectTime > 0) "${res.connectTime}ms" else "-"}\n")
        sb.append("   • TLS: ${if (res.tlsTime > 0) "${res.tlsTime}ms" else "-"}\n")
        sb.append("   • TTFB: ${if (res.ttfbTime > 0) "${res.ttfbTime}ms" else "-"}\n")

        if (res.hasSsl) {
            sb.append("\n🔒 SSL Info:\n")
            sb.append("   • Subject: ${res.sslSubject}\n")
            sb.append("   • Issuer: ${res.sslIssuer}\n")
            sb.append("   • Expiry: ${res.sslExpiryDays} days remaining\n")
        }

        sb.append("\n📄 Headers:\n")
        res.headers.forEach { (k, v) ->
            sb.append("   $k: $v\n")
        }

        sb.append("─────────────────────\n")
        sb.append("Sent from NeTez Network Tools")
        return sb.toString()
    }

    fun copyToClipboard() {
        val context = getApplication<Application>()
        val text = getShareText()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("HTTP Check Result", text)
        clipboard.setPrimaryClip(clip)
    }

    fun shareResults(context: Context) {
        val text = getShareText()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share HTTP Result"))
    }

    private fun saveToDb(res: HttpCheckResult) {
        viewModelScope.launch {
            dao.insertResult(TestResult(
                type = "HTTP",
                target = res.url,
                result = if (res.isAvailable) "ONLINE (${res.statusCode})" else "DOWN"
            ))
        }
    }
}
