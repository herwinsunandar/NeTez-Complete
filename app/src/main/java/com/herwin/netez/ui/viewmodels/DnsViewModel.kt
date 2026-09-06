package com.herwin.netez.ui.viewmodels

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.network.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class DnsState(
    val domain: String = "google.com",
    val recordType: DnsRecordType = DnsRecordType.A,
    val selectedServer: DnsServer = PublicDnsServers[0],
    val response: DnsResponse? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val counts: Map<DnsRecordType, Int> = emptyMap()
)

class DnsViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(DnsState())
    val uiState: StateFlow<DnsState> = _uiState

    fun updateDomain(newDomain: String) {
        _uiState.value = _uiState.value.copy(domain = newDomain)
    }

    fun updateRecordType(type: DnsRecordType) {
        _uiState.value = _uiState.value.copy(recordType = type)
        if (_uiState.value.domain.isNotEmpty()) {
            lookup()
        }
    }

    fun updateServer(server: DnsServer) {
        _uiState.value = _uiState.value.copy(selectedServer = server)
        if (_uiState.value.domain.isNotEmpty()) {
            lookup()
        }
    }

    fun lookup() {
        if (_uiState.value.domain.isEmpty()) return
        
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val response = DnsManager.lookup(
                domain = _uiState.value.domain, 
                type = _uiState.value.recordType,
                server = _uiState.value.selectedServer
            )
            
            val newCounts = _uiState.value.counts.toMutableMap()
            newCounts[_uiState.value.recordType] = response.records.size
            
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                response = response,
                counts = newCounts,
                error = if (response.records.isEmpty()) "No records found" else null
            )
        }
    }

    fun getShareText(): String {
        val state = _uiState.value
        val res = state.response ?: return "No DNS results available."
        
        val sb = StringBuilder("⚡ NeTez DNS Lookup Report\n")
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        sb.append("📅 Date: ${sdf.format(Date())}\n")
        sb.append("─────────────────────\n")
        sb.append("🎯 Domain: ${state.domain}\n")
        sb.append("🔍 Record Type: ${state.recordType}\n")
        sb.append("📡 Resolver: ${res.resolver} (${res.resolverIp})\n")
        sb.append("⏱️ Latency: ${res.latency}ms\n\n")
        
        if (res.records.isNotEmpty()) {
            sb.append("📍 Records Found (${res.records.size}):\n")
            res.records.forEachIndexed { i, record ->
                val typeStr = when(record.type) {
                    1 -> "A"
                    28 -> "AAAA"
                    15 -> "MX"
                    16 -> "TXT"
                    5 -> "CNAME"
                    2 -> "NS"
                    else -> "Type ${record.type}"
                }
                sb.append("   [${i + 1}] $typeStr: ${record.data} (TTL: ${record.ttl}s)\n")
            }
        } else {
            sb.append("No records found.\n")
        }
        
        sb.append("─────────────────────\n")
        sb.append("Sent from NeTez Network Tools")
        return sb.toString()
    }

    fun copyResultsToClipboard() {
        val context = getApplication<Application>()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("DNS Lookup Results", getShareText())
        clipboard.setPrimaryClip(clip)
    }

    fun shareResults(context: Context) {
        val text = getShareText()
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(intent, "Share DNS Results"))
    }
}
