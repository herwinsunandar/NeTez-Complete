package com.herwin.netez.ui.viewmodels

import android.app.Application
import android.content.Context
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.network.WifiScanner
import com.herwin.netez.utils.NetworkUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class WifiFilter {
    ALL, FREQ_24, FREQ_5
}

data class WifiScannerState(
    val scanResults: List<ScanResult> = emptyList(),
    val currentBssid: String? = null,
    val currentLinkSpeed: Int = 0,
    val isScanning: Boolean = false,
    val filter: WifiFilter = WifiFilter.ALL,
    val connectedIsp: String = "",
    val connectedPublicIp: String = ""
) {
    val filteredResults: List<ScanResult>
        get() = when (filter) {
            WifiFilter.ALL -> scanResults
            WifiFilter.FREQ_24 -> scanResults.filter { it.frequency in 2400..2500 }
            WifiFilter.FREQ_5 -> scanResults.filter { it.frequency in 4900..5900 }
        }
}

class WifiScannerViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(WifiScannerState())
    val uiState: StateFlow<WifiScannerState> = _uiState

    fun updateFilter(filter: WifiFilter) {
        _uiState.value = _uiState.value.copy(filter = filter)
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isScanning = true)
        
        viewModelScope.launch {
            val results = WifiScanner.getWifiScanResults(getApplication())
            val wifiManager = getApplication<Application>().applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val currentInfo = @Suppress("DEPRECATION") wifiManager.connectionInfo
            val bssid = if (currentInfo?.networkId != -1) currentInfo?.bssid else null
            val speed = if (bssid != null) currentInfo.linkSpeed else 0
            
            var isp = ""
            var pubIp = ""
            
            if (bssid != null) {
                pubIp = NetworkUtils.getPublicIp()
                isp = NetworkUtils.getIspInfo()
            }

            _uiState.value = _uiState.value.copy(
                scanResults = results.sortedByDescending { it.level },
                currentBssid = bssid,
                currentLinkSpeed = speed,
                isScanning = false,
                connectedIsp = isp,
                connectedPublicIp = pubIp
            )
        }
    }
}
