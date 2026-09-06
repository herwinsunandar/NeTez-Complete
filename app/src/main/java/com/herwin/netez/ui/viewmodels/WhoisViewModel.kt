package com.herwin.netez.ui.viewmodels

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.network.WhoisManager
import com.herwin.netez.network.WhoisResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class WhoisState(
    val query: String = "8.8.8.8",
    val result: WhoisResult? = null,
    val isLoading: Boolean = false
)

class WhoisViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(WhoisState())
    val uiState: StateFlow<WhoisState> = _uiState

    fun updateQuery(newQuery: String) {
        _uiState.value = _uiState.value.copy(query = newQuery)
    }

    fun fetchInfo() {
        if (_uiState.value.query.isBlank()) return
        
        _uiState.value = _uiState.value.copy(isLoading = true, result = null)
        viewModelScope.launch {
            val result = WhoisManager.getInfo(_uiState.value.query)
            _uiState.value = _uiState.value.copy(isLoading = false, result = result)
        }
    }

    fun getShareText(): String {
        val res = _uiState.value.result ?: return ""
        val sb = StringBuilder("⚡ NeTez Whois & IP Report\n")
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        sb.append("📅 Date: ${sdf.format(Date())}\n")
        sb.append("─────────────────────\n")

        when (res) {
            is WhoisResult.Ip -> {
                val info = res.info
                sb.append("🎯 Target: ${info.query}\n")
                sb.append("📊 Stats:\n")
                sb.append("   • Country: ${info.country} (${info.countryCode})\n")
                sb.append("   • City/Region: ${info.city}, ${info.regionName}\n")
                sb.append("   • ISP: ${info.isp}\n")
                sb.append("   • Org: ${info.org}\n")
                sb.append("   • ASN: ${info.asn}\n")
                sb.append("   • Timezone: ${info.timezone}\n")
                sb.append("   • Coordinates: ${info.lat}, ${info.lon}\n")
                sb.append("   • Reverse DNS: ${info.reverse}\n")
                sb.append("   • Mobile: ${if (info.mobile) "Yes" else "No"}\n")
                sb.append("   • Proxy: ${if (info.proxy) "Yes" else "No"}\n")
                sb.append("   • Hosting: ${if (info.hosting) "Yes" else "No"}\n")
            }
            is WhoisResult.Domain -> {
                val info = res.info
                sb.append("🌐 Domain: ${info.domain}\n")
                sb.append("📊 Registration:\n")
                sb.append("   • Registrar: ${info.registrar}\n")
                sb.append("   • Created: ${info.creationDate}\n")
                sb.append("   • Expires: ${info.expirationDate}\n")
                sb.append("   • Nameservers: ${info.nameservers.joinToString(", ")}\n")
            }
            is WhoisResult.Error -> {
                sb.append("❌ Error: ${res.message}\n")
            }
        }

        sb.append("─────────────────────\n")
        sb.append("Sent from NeTez Network Tools")
        return sb.toString()
    }

    fun copyResultsToClipboard() {
        val context = getApplication<Application>()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Whois Results", getShareText())
        clipboard.setPrimaryClip(clip)
    }

    fun shareResults(context: Context) {
        val text = getShareText()
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(intent, "Share Whois Results"))
    }
}
