package com.herwin.netez.ui.viewmodels

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.utils.NetworkUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class WifiBand {
    BAND_2_4GHZ, BAND_5GHZ, BAND_6GHZ
}

data class Recommendation(
    val bestChannel: Int,
    val reason: String
)

data class ChannelGraphState(
    val selectedBand: WifiBand = WifiBand.BAND_2_4GHZ,
    val scanResults: List<ScanResult> = emptyList(),
    val isScanning: Boolean = false,
    val mySsid: String = "",
    val recommendation: Recommendation? = null
)

class ChannelGraphViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(ChannelGraphState())
    val uiState: StateFlow<ChannelGraphState> = _uiState

    private val wifiManager = application.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private var scanJob: Job? = null

    init {
        startRealtimeScan()
    }

    fun updateBand(band: WifiBand) {
        _uiState.value = _uiState.value.copy(selectedBand = band)
        refresh()
    }

    private fun startRealtimeScan() {
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            while (true) {
                refresh()
                delay(3000) // Scan every 3 seconds
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun refresh() {
        viewModelScope.launch {
            try {
                val results = wifiManager.scanResults ?: emptyList()
                val filtered = when (_uiState.value.selectedBand) {
                    WifiBand.BAND_2_4GHZ -> results.filter { it.frequency in 2400..2500 }
                    WifiBand.BAND_5GHZ -> results.filter { it.frequency in 4900..5900 }
                    WifiBand.BAND_6GHZ -> results.filter { it.frequency in 5925..7125 }
                }
                
                @Suppress("DEPRECATION")
                val info = wifiManager.connectionInfo
                val mySsid = info?.ssid?.replace("\"", "") ?: ""

                val recommendation = calculateBestChannel(filtered, _uiState.value.selectedBand)

                _uiState.value = _uiState.value.copy(
                    scanResults = filtered.sortedByDescending { it.level },
                    mySsid = mySsid,
                    recommendation = recommendation
                )
            } catch (ignored: Exception) {}
        }
    }

    private fun calculateBestChannel(results: List<ScanResult>, band: WifiBand): Recommendation {
        val channelRange = when (band) {
            WifiBand.BAND_2_4GHZ -> listOf(1, 6, 11) // Standard non-overlapping
            WifiBand.BAND_5GHZ -> listOf(36, 40, 44, 48, 149, 153, 157, 161)
            WifiBand.BAND_6GHZ -> listOf(1, 5, 9, 13) // Simplified
        }

        if (results.isEmpty()) return Recommendation(channelRange.first(), "No interference detected")

        val channelScores = channelRange.associateWith { channel ->
            var overlapCount = 0
            var totalRssi = 0.0
            
            results.forEach { ap ->
                val apChannel = NetworkUtils.getChannel(ap.frequency)
                // Assume 20MHz spread for scoring
                if (Math.abs(apChannel - channel) <= 2) {
                    overlapCount++
                    totalRssi += ap.level + 100 // Normalize -100 to 0
                }
            }
            overlapCount * 100 + totalRssi
        }

        val best = channelScores.minByOrNull { it.value }?.key ?: channelRange.first()
        return Recommendation(best, "Lowest interference found")
    }

    override fun onCleared() {
        super.onCleared()
        scanJob?.cancel()
    }
}
