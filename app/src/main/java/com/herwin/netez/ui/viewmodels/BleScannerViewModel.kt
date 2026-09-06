package com.herwin.netez.ui.viewmodels

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
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
    val filter: BleFilter = BleFilter.NEARBY, // Default to Nearby
    val scanTimeMs: Long = 0,
    val searchQuery: String = "",
    val error: String? = null,
    val showPermissionDialog: Boolean = false
) {
    val filteredDevices: List<BleDevice>
        get() = devices.filter { 
            val matchesFilter = when (filter) {
                BleFilter.ALL -> true
                BleFilter.PAIRED -> it.isPaired
                BleFilter.NEARBY -> it.rssi > -75 // User requested -75
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
            if (checkPrerequisites()) {
                startScan()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun checkPrerequisites(): Boolean {
        val context = getApplication<Application>()
        
        // 1. Check Bluetooth Enabled
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null || !adapter.isEnabled) {
            _uiState.value = _uiState.value.copy(error = "Aktifkan Bluetooth untuk scan")
            return false
        }

        // 2. Check Permissions
        val permissions = if (Build.VERSION.SDK_INT >= 31) {
            listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        
        val missing = permissions.any { 
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED 
        }
        
        if (missing) {
            _uiState.value = _uiState.value.copy(showPermissionDialog = true)
            return false
        }

        // 3. Check Location Enabled (needed for BLE on some versions)
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            _uiState.value = _uiState.value.copy(error = "Aktifkan Lokasi/GPS untuk scan BLE name")
            return false
        }

        _uiState.value = _uiState.value.copy(error = null)
        return true
    }

    fun dismissPermissionDialog() {
        _uiState.value = _uiState.value.copy(showPermissionDialog = false)
    }

    fun fetchName(mac: String) {
        manager.fetchNameGatt(mac) { name: String? ->
            // The manager updates via foundDevices flow. 
        }
    }

    fun pairDevice(nativeDevice: BluetoothDevice?) {
        nativeDevice?.let { device: BluetoothDevice ->
            manager.pairDevice(device)
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
