package com.herwin.netez.ui.viewmodels

import android.app.Application
import android.net.wifi.ScanResult
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.network.WifiScanner
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class WifiDetailState(
    val scanResult: ScanResult? = null,
    val signalHistory: List<Int> = emptyList()
)

class WifiDetailViewModel(application: Application, private val bssid: String) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(WifiDetailState())
    val uiState: StateFlow<WifiDetailState> = _uiState

    init {
        startMonitoring()
    }

    private fun startMonitoring() {
        viewModelScope.launch {
            while (true) {
                val results = WifiScanner.getWifiScanResults(getApplication())
                val current = results.find { it.BSSID == bssid }
                
                current?.let {
                    val newHistory = (_uiState.value.signalHistory + it.level).takeLast(30)
                    _uiState.value = _uiState.value.copy(
                        scanResult = it,
                        signalHistory = newHistory
                    )
                }
                delay(2000)
            }
        }
    }
}
