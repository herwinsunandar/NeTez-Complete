package com.herwin.netez.ui.viewmodels

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.network.BleDevice
import com.herwin.netez.network.BleScannerManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class BleFilter {
    ALL, PAIRED, NEARBY
}

data class BleScannerState(
    val devices: List<BleDevice> = emptyList(),
    val isScanning: Boolean = false,
    val filter: BleFilter = BleFilter.ALL,
    val scanTimeMs: Long = 0,
    val searchQuery: String = ""
) {
    val filteredDevices: List<BleDevice>
        get() = devices.filter { 
            val matchesFilter = when (filter) {
                BleFilter.ALL -> true
                BleFilter.PAIRED -> it.isPaired
                BleFilter.NEARBY -> it.rssi > -70
            }
            val matchesSearch = it.name.lowercase().contains(searchQuery.lowercase()) || it.mac.contains(searchQuery)
            matchesFilter && matchesSearch
        }
}

class BleScannerViewModel(application: Application) : AndroidViewModel(application) {
    private val manager = BleScannerManager(application)
    private val _uiState = MutableStateFlow(BleScannerState())
    val uiState: StateFlow<BleScannerState> = _uiState

    private var scanJob: Job? = null

    init {
        viewModelScope.launch {
            manager.foundDevices.collect {
                _uiState.value = _uiState.value.copy(devices = it)
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun updateFilter(filter: BleFilter) {
        _uiState.value = _uiState.value.copy(filter = filter)
    }

    fun toggleScan() {
        if (_uiState.value.isScanning) {
            stopScan()
        } else {
            startScan()
        }
    }

    fun pairDevice(nativeDevice: BluetoothDevice?) {
        nativeDevice?.let {
            manager.pairDevice(it)
        }
    }

    private fun startScan() {
        _uiState.value = _uiState.value.copy(isScanning = true, scanTimeMs = 0)
        manager.startScan()
        scanJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            while (_uiState.value.isScanning) {
                delay(100)
                _uiState.value = _uiState.value.copy(scanTimeMs = System.currentTimeMillis() - startTime)
                if (_uiState.value.scanTimeMs > 15000) {
                    stopScan()
                }
            }
        }
    }

    private fun stopScan() {
        _uiState.value = _uiState.value.copy(isScanning = false)
        manager.stopScan()
        scanJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        stopScan()
    }
}
