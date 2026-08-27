package com.herwin.netez.ui.viewmodels

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.network.PingManager
import com.herwin.netez.network.PingResult
import com.herwin.netez.service.PingService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PingState(
    val host: String = "8.8.8.8",
    val results: List<PingResult> = emptyList(),
    val isRunning: Boolean = false,
    val avgLatency: Double = 0.0,
    val packetLoss: Int = 0,
    val jitter: Double = 0.0,
    val minLatency: Double = 0.0,
    val maxLatency: Double = 0.0,
    // Settings
    val intervalMs: Long = 1000,
    val packetSize: Int = 56,
    val count: Int = 0, // 0 for infinite
    val hostHistory: List<String> = listOf("8.8.8.8", "1.1.1.1", "google.com")
)

class PingViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(PingState())
    val uiState: StateFlow<PingState> = _uiState

    private var pingJob: Job? = null

    fun updateHost(newHost: String) {
        _uiState.value = _uiState.value.copy(host = newHost)
    }

    fun updateSettings(interval: Long, size: Int, count: Int) {
        _uiState.value = _uiState.value.copy(
            intervalMs = interval,
            packetSize = size,
            count = count
        )
    }

    fun togglePing() {
        if (_uiState.value.isRunning) {
            stopPing()
        } else {
            startPing()
        }
    }

    private fun startPing() {
        val currentHost = _uiState.value.host
        if (currentHost.isBlank()) return
        
        if (!_uiState.value.hostHistory.contains(currentHost)) {
            _uiState.value = _uiState.value.copy(
                hostHistory = (listOf(currentHost) + _uiState.value.hostHistory).take(10)
            )
        }

        _uiState.value = _uiState.value.copy(isRunning = true, results = emptyList())
        
        // Start background service
        PingService.start(getApplication(), currentHost)

        pingJob = viewModelScope.launch {
            var currentCount = 0
            val targetCount = _uiState.value.count
            
            while (_uiState.value.isRunning) {
                val result = PingManager.ping(_uiState.value.host, _uiState.value.packetSize)
                val newResults = (_uiState.value.results + result).takeLast(100)
                
                val successful = newResults.filter { it.isSuccessful }
                val avg = if (successful.isNotEmpty()) successful.map { it.time }.average() else 0.0
                val loss = if (newResults.isNotEmpty()) ((newResults.size - successful.size).toDouble() / newResults.size * 100).toInt() else 0
                
                val min = if (successful.isNotEmpty()) successful.minOf { it.time } else 0.0
                val max = if (successful.isNotEmpty()) successful.maxOf { it.time } else 0.0
                
                val jitter = if (successful.size > 1) {
                    successful.zipWithNext { a, b -> Math.abs(a.time - b.time) }.average()
                } else 0.0

                _uiState.value = _uiState.value.copy(
                    results = newResults,
                    avgLatency = avg,
                    packetLoss = loss,
                    minLatency = min,
                    maxLatency = max,
                    jitter = jitter
                )
                
                currentCount++
                if (targetCount > 0 && currentCount >= targetCount) {
                    stopPing()
                    break
                }
                
                delay(_uiState.value.intervalMs)
            }
        }
    }

    private fun stopPing() {
        _uiState.value = _uiState.value.copy(isRunning = false)
        PingService.stop(getApplication())
        pingJob?.cancel()
    }

    fun copyResultsToClipboard() {
        val context = getApplication<Application>()
        val state = _uiState.value
        val log = StringBuilder()
        log.append("Ping Results for ${state.host}\n")
        log.append("----------------------------\n")
        log.append("Avg Latency: ${String.format(Locale.US, "%.1f", state.avgLatency)} ms\n")
        log.append("Min/Max: ${state.minLatency.toInt()}/${state.maxLatency.toInt()} ms\n")
        log.append("Packet Loss: ${state.packetLoss}%\n")
        log.append("Jitter: ${String.format(Locale.US, "%.1f", state.jitter)} ms\n")
        log.append("Total Packets: ${state.results.size}\n")
        log.append("----------------------------\n\n")
        
        log.append("Detailed Log:\n")
        state.results.forEachIndexed { index, res ->
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(res.timestamp))
            if (res.isSuccessful) {
                log.append("[$time] Seq #${index + 1}: time=${res.time}ms, ttl=${res.ttl}\n")
            } else {
                log.append("[$time] Seq #${index + 1}: Request Timeout\n")
            }
        }
        
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Ping Results", log.toString())
        clipboard.setPrimaryClip(clip)
    }

    override fun onCleared() {
        stopPing()
    }
}
