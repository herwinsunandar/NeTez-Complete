package com.herwin.netez.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.network.SnmpDevice
import com.herwin.netez.network.SnmpManager
import com.herwin.netez.utils.NetworkUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.system.measureTimeMillis

data class SnmpScannerState(
    val devices: List<SnmpDevice> = emptyList(),
    val isScanning: Boolean = false,
    val progress: Float = 0f,
    val subnet: String = "192.168.1",
    val scanTimeMs: Long = 0
)

class SnmpScannerViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(SnmpScannerState())
    val uiState: StateFlow<SnmpScannerState> = _uiState

    init {
        val ip = NetworkUtils.getLocalIpAddress()
        if (ip != null) {
            val parts = ip.split(".")
            if (parts.size == 4) {
                val subnet = "${parts[0]}.${parts[1]}.${parts[2]}"
                _uiState.value = _uiState.value.copy(subnet = subnet)
            }
        }
    }

    fun updateSubnet(subnet: String) {
        _uiState.value = _uiState.value.copy(subnet = subnet)
    }

    fun startScan() {
        val subnet = _uiState.value.subnet
        _uiState.value = _uiState.value.copy(
            isScanning = true,
            devices = emptyList(),
            progress = 0f
        )

        viewModelScope.launch {
            var foundDevices: List<SnmpDevice> = emptyList()
            val time = measureTimeMillis {
                foundDevices = SnmpManager.scanSubnet(subnet) { completed ->
                    _uiState.value = _uiState.value.copy(progress = completed.toFloat() / 254f)
                }
            }

            _uiState.value = _uiState.value.copy(
                isScanning = false,
                devices = foundDevices.sortedBy { it.ip },
                scanTimeMs = time,
                progress = 1f
            )
        }
    }
}
