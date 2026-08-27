package com.herwin.netez.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.data.AppDatabase
import com.herwin.netez.data.TestResult
import com.herwin.netez.network.UpnpDevice
import com.herwin.netez.network.UpnpManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.system.measureTimeMillis

data class UpnpScannerState(
    val devices: List<UpnpDevice> = emptyList(),
    val isScanning: Boolean = false,
    val scanTimeMs: Long = 0
)

class UpnpScannerViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(UpnpScannerState())
    val uiState: StateFlow<UpnpScannerState> = _uiState
    private val db = AppDatabase.getDatabase(application)
    private val testDao = db.testResultDao()

    fun startScan() {
        _uiState.value = _uiState.value.copy(isScanning = true, devices = emptyList())
        
        viewModelScope.launch {
            var foundDevices: List<UpnpDevice> = emptyList()
            val time = measureTimeMillis {
                foundDevices = UpnpManager.scan(getApplication())
            }
            
            _uiState.value = _uiState.value.copy(
                isScanning = false, 
                devices = foundDevices,
                scanTimeMs = time
            )
            
            saveToHistory(foundDevices.size, time)
        }
    }

    private fun saveToHistory(count: Int, time: Long) {
        viewModelScope.launch {
            testDao.insertResult(TestResult(
                type = "UPnP Scan",
                target = "Local Network",
                result = String.format(Locale.US, "%d Devices • %.1fs", count, time / 1000f)
            ))
        }
    }
}
