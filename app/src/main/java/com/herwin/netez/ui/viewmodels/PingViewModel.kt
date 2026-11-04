package com.herwin.netez.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.herwin.netez.entitlement.FeatureAccessManager
import com.herwin.netez.entitlement.FeatureAccessState
import com.herwin.netez.entitlement.FeatureId

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.network.PingManager
import com.herwin.netez.network.PingResult
import com.herwin.netez.service.PingService
import com.herwin.netez.utils.NetworkUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class PingMode { SINGLE, MULTI }

data class MultiPingHost(
    val host: String,
    val label: String = "",
    val isEnabled: Boolean = true,
    val color: Int = 0xFF6C8CFF.toInt(), // Default blue
    val lastLatency: Double = 0.0,
    val avgLatency: Double = 0.0,
    val packetLoss: Int = 0,
    val results: List<PingResult> = emptyList()
)

data class PingState(
    val mode: PingMode = PingMode.SINGLE,
    val host: String = "",
    val results: List<PingResult> = emptyList(),
    val isRunning: Boolean = false,
    val avgLatency: Double = 0.0,
    val packetLoss: Int = 0,
    val jitter: Double = 0.0,
    val minLatency: Double = 0.0,
    val maxLatency: Double = 0.0,
    // Multi Ping State
    val multiHosts: List<MultiPingHost> = emptyList(),
    // Settings
    val intervalMs: Long = 1000,
    val packetSize: Int = 56,
    val count: Int = 0, // 0 for infinite
    val hostHistory: List<String> = listOf("8.8.8.8", "1.1.1.1", "google.com")
)

class PingViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(PingState())
    val uiState: StateFlow<PingState> = _uiState

    var showPaywallDialog by mutableStateOf(false)
    var showGlobalSupportDialog by mutableStateOf(false)

    private var pingJob: Job? = null

    init {
        // Pre-fill multi hosts
        val gateway = NetworkUtils.getGateway(application)
        _uiState.update { it.copy(
            multiHosts = listOf(
                MultiPingHost("8.8.8.8", "Google DNS", color = 0xFF6C8CFF.toInt()),
                MultiPingHost("1.1.1.1", "Cloudflare", color = 0xFF8B5CF6.toInt()),
                MultiPingHost(if (gateway != "0.0.0.0") gateway else "192.168.1.1", "Gateway", color = 0xFF22C55E.toInt())
            )
        ) }
    }

    fun setMode(mode: PingMode) {
        if (_uiState.value.isRunning) stopPing()
        _uiState.update { it.copy(mode = mode) }
    }

    fun updateHost(newHost: String) {
        _uiState.update { it.copy(host = newHost) }
    }

    fun addMultiHost(host: String) {
        val accessManager = FeatureAccessManager.getInstance(getApplication())
        val currentCount = _uiState.value.multiHosts.size
        val state = accessManager.checkAccess(FeatureId.MULTI_PING_NODES, currentCount)
        if (state == FeatureAccessState.LIMIT_REACHED) {
            showPaywallDialog = true
            return
        }

        val colors = listOf(0xFF6C8CFF, 0xFF8B5CF6, 0xFF22C55E, 0xFFFBBF24, 0xFFEF4444)
        val color = colors[_uiState.value.multiHosts.size % colors.size].toInt()
        _uiState.update { it.copy(
            multiHosts = it.multiHosts + MultiPingHost(host, color = color)
        ) }
    }

    fun removeMultiHost(host: MultiPingHost) {
        _uiState.update { it.copy(
            multiHosts = it.multiHosts.filter { h -> h.host != host.host }
        ) }
    }

    fun toggleMultiHost(host: MultiPingHost) {
        _uiState.update { state ->
            state.copy(multiHosts = state.multiHosts.map {
                if (it.host == host.host) it.copy(isEnabled = !it.isEnabled) else it
            })
        }
    }

    fun updateSettings(interval: Long, size: Int, count: Int) {
        _uiState.update { it.copy(
            intervalMs = interval,
            packetSize = size,
            count = count
        ) }
    }

    fun togglePing() {
        if (_uiState.value.isRunning) {
            stopPing()
        } else {
            val accessManager = FeatureAccessManager.getInstance(getApplication())
            if (accessManager.trackAndCheckGlobalToolUsage()) {
                showGlobalSupportDialog = true
            }
            if (_uiState.value.mode == PingMode.SINGLE) startSinglePing() else startMultiPing()
        }
    }

    private fun startSinglePing() {
        val currentHost = _uiState.value.host.trim().ifBlank {
            _uiState.value.hostHistory.firstOrNull() ?: "8.8.8.8"
        }

        if (!_uiState.value.hostHistory.contains(currentHost)) {
            _uiState.update { it.copy(
                hostHistory = (listOf(currentHost) + it.hostHistory).take(10)
            ) }
        }

        _uiState.update { it.copy(
            isRunning = true,
            results = emptyList(),
            host = _uiState.value.host.ifBlank { currentHost }
        ) }

        PingService.start(getApplication(), currentHost)

        pingJob = viewModelScope.launch {
            var currentCount = 0
            val targetCount = _uiState.value.count

            while (_uiState.value.isRunning) {
                val result = PingManager.ping(currentHost, _uiState.value.packetSize)
                _uiState.update { state ->
                    val newResults = (state.results + result).takeLast(100)
                    val successful = newResults.filter { it.isSuccessful }
                    state.copy(
                        results = newResults,
                        avgLatency = if (successful.isNotEmpty()) successful.map { it.time }.average() else 0.0,
                        packetLoss = if (newResults.isNotEmpty()) ((newResults.size - successful.size).toDouble() / newResults.size * 100).toInt() else 0,
                        minLatency = if (successful.isNotEmpty()) successful.minOf { it.time } else 0.0,
                        maxLatency = if (successful.isNotEmpty()) successful.maxOf { it.time } else 0.0,
                        jitter = if (successful.size > 1) successful.zipWithNext { a, b -> Math.abs(a.time - b.time) }.average() else 0.0
                    )
                }

                currentCount++
                if (targetCount > 0 && currentCount >= targetCount) {
                    stopPing()
                    break
                }
                delay(_uiState.value.intervalMs)
            }
        }
    }

    private fun startMultiPing() {
        val hosts = _uiState.value.multiHosts.filter { it.isEnabled }
        if (hosts.isEmpty()) return

        _uiState.update { it.copy(
            isRunning = true,
            multiHosts = it.multiHosts.map { h -> h.copy(results = emptyList(), avgLatency = 0.0, packetLoss = 0) }
        ) }

        pingJob = viewModelScope.launch(Dispatchers.IO) {
            val jobs = hosts.map { host ->
                launch {
                    var currentCount = 0
                    val targetCount = if (_uiState.value.count > 0) _uiState.value.count else 30

                    while (isActive && _uiState.value.isRunning) {
                        val result = PingManager.ping(host.host, _uiState.value.packetSize)

                        _uiState.update { state ->
                            val updatedHosts = state.multiHosts.map { h ->
                                if (h.host == host.host) {
                                    val newResults = (h.results + result).takeLast(100)
                                    val successful = newResults.filter { it.isSuccessful }
                                    h.copy(
                                        results = newResults,
                                        lastLatency = if (result.isSuccessful) result.time else 0.0,
                                        avgLatency = if (successful.isNotEmpty()) successful.map { it.time }.average() else 0.0,
                                        packetLoss = if (newResults.isNotEmpty()) ((newResults.size - successful.size).toDouble() / newResults.size * 100).toInt() else 0
                                    )
                                } else h
                            }
                            state.copy(multiHosts = updatedHosts)
                        }

                        currentCount++
                        if (targetCount > 0 && currentCount >= targetCount) break
                        delay(_uiState.value.intervalMs)
                    }
                }
            }
            jobs.joinAll()
            withContext(Dispatchers.Main) { stopPing() }
        }
    }

    private fun stopPing() {
        _uiState.update { it.copy(isRunning = false) }
        PingService.stop(getApplication())
        pingJob?.cancel()
    }

    fun getShareText(): String {
        val state = _uiState.value
        val sb = StringBuilder("⚡ NeTez Ping Test Report\n")
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        sb.append("📅 Date: ${sdf.format(Date())}\n")
        sb.append("─────────────────────\n")

        if (state.mode == PingMode.SINGLE) {
            sb.append("🎯 Target: ${state.host}\n")
            sb.append("📊 Stats:\n")
            sb.append("   • Avg Latency: ${String.format("%.1f", state.avgLatency)} ms\n")
            sb.append("   • Packet Loss: ${state.packetLoss}%\n")
            sb.append("   • Jitter: ${String.format("%.1f", state.jitter)} ms\n")
            sb.append("   • Min/Max: ${state.minLatency.toInt()}/${state.maxLatency.toInt()} ms\n")

            if (state.results.isNotEmpty()) {
                sb.append("\n📝 Recent Logs (Last 20):\n")
                state.results.takeLast(20).forEachIndexed { i, res ->
                    val status = if (res.isSuccessful) "OK - ${res.time}ms" else "TIMEOUT"
                    sb.append("   [${i + 1}] $status\n")
                }
            }
        } else {
            sb.append("🌐 Multi-Target Results:\n")
            state.multiHosts.filter { it.isEnabled }.forEach { host ->
                val status = if (host.packetLoss == 100) "🔴 DOWN" else if (host.packetLoss > 0) "🟡 UNSTABLE" else "🟢 UP"
                sb.append("   • ${host.host} ($status)\n")
                sb.append("     Avg: ${String.format("%.1f", host.avgLatency)}ms | Loss: ${host.packetLoss}%\n")
            }
        }
        sb.append("─────────────────────\n")
        sb.append("Sent from NeTez Network Tools")
        return sb.toString()
    }

    fun copyResultsToClipboard() {
        val context = getApplication<Application>()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Ping Results", getShareText())
        clipboard.setPrimaryClip(clip)
    }

    fun shareResults(context: Context) {
        val text = getShareText()
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(intent, "Share Ping Results"))
    }

    override fun onCleared() {
        stopPing()
    }
}
