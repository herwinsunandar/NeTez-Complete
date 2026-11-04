package com.herwin.netez.ui.viewmodels

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.network.Hop
import com.herwin.netez.network.TracerouteManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TracerouteState(
    val host: String = "",
    val hops: List<Hop> = emptyList(),
    val isRunning: Boolean = false
)

class TracerouteViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(TracerouteState())
    val uiState: StateFlow<TracerouteState> = _uiState

    private var traceJob: Job? = null

    fun updateHost(newHost: String) {
        _uiState.value = _uiState.value.copy(host = newHost)
    }

    fun toggleTrace() {
        if (_uiState.value.isRunning) {
            stopTrace()
        } else {
            startTrace()
        }
    }

    private fun startTrace() {
        val targetHost = _uiState.value.host.trim().ifBlank { "google.com" }
        _uiState.value = _uiState.value.copy(
            isRunning = true,
            hops = emptyList(),
            host = _uiState.value.host.ifBlank { targetHost }
        )
        traceJob = viewModelScope.launch {
            TracerouteManager.trace(targetHost).collect { hop ->
                _uiState.value = _uiState.value.copy(
                    hops = _uiState.value.hops + hop
                )
            }
            _uiState.value = _uiState.value.copy(isRunning = false)
        }
    }

    private fun stopTrace() {
        _uiState.value = _uiState.value.copy(isRunning = false)
        traceJob?.cancel()
    }

    fun getShareText(): String {
        val state = _uiState.value
        val sb = StringBuilder("⚡ NeTez Traceroute Report\n")
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        sb.append("📅 Date: ${sdf.format(Date())}\n")
        sb.append("─────────────────────\n")
        sb.append("🎯 Target: ${state.host}\n\n")

        if (state.hops.isNotEmpty()) {
            sb.append("📍 Route Hops:\n")
            state.hops.forEach { hop ->
                val latency = if (hop.latency == "*") "*" else "${hop.latency}ms"
                sb.append("   [${hop.number}] ${hop.ip} (${hop.hostname}) - $latency\n")
            }
        } else {
            sb.append("No results available.\n")
        }

        sb.append("─────────────────────\n")
        sb.append("Sent from NeTez Network Tools")
        return sb.toString()
    }

    fun copyResultsToClipboard() {
        val context = getApplication<Application>()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Traceroute Results", getShareText())
        clipboard.setPrimaryClip(clip)
    }

    fun shareResults(context: Context) {
        val text = getShareText()
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(intent, "Share Traceroute Results"))
    }
}
