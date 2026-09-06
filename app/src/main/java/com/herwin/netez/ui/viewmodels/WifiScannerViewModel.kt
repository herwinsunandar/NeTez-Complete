package com.herwin.netez.ui.viewmodels

import android.app.Application
import android.content.Context
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.data.AppDatabase
import com.herwin.netez.data.VendorOverrideEntity
import com.herwin.netez.data.wifi.VendorDatabase
import com.herwin.netez.network.WifiScanner
import com.herwin.netez.utils.NetworkUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
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
    val connectedPublicIp: String = "",
    val vendorMap: Map<String, String> = emptyMap()
) {
    val filteredResults: List<ScanResult>
        get() {
            val base = when (filter) {
                WifiFilter.ALL -> scanResults
                WifiFilter.FREQ_24 -> scanResults.filter { it.frequency in 2400..2500 }
                WifiFilter.FREQ_5 -> scanResults.filter { it.frequency in 4900..5900 }
            }
            return base.sortedByDescending { it.level }
        }
}

class WifiScannerViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(WifiScannerState())
    val uiState: StateFlow<WifiScannerState> = _uiState

    init {
        viewModelScope.launch {
            VendorDatabase.vendorUpdates.collectLatest { mac ->
                val vendor = VendorDatabase.getVendorSync(mac)
                _uiState.update { state ->
                    val newMap = state.vendorMap.toMutableMap()
                    newMap[mac] = vendor
                    state.copy(vendorMap = newMap)
                }
            }
        }
    }

    fun updateFilter(filter: WifiFilter) {
        _uiState.value = _uiState.value.copy(filter = filter)
    }

    fun saveVendorOverride(mac: String, label: String) {
        viewModelScope.launch {
            val prefix = mac.take(8).uppercase().replace("-", ":")
            val db = AppDatabase.getDatabase(getApplication())
            db.vendorOverrideDao().insertOverride(
                VendorOverrideEntity(mac = prefix, customVendor = label, source = "USER")
            )
            _uiState.update { state ->
                val newMap = state.vendorMap.toMutableMap()
                newMap[mac] = label
                state.copy(vendorMap = newMap)
            }
        }
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
