package com.herwin.netez.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.data.AppDatabase
import com.herwin.netez.data.TestResult
import com.herwin.netez.network.MdnsManager
import com.herwin.netez.network.MdnsService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

data class MdnsScannerState(
    val services: List<MdnsService> = emptyList(),
    val isScanning: Boolean = false,
    val scanTimeMs: Long = 0
)

class MdnsScannerViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(MdnsScannerState())
    val uiState: StateFlow<MdnsScannerState> = _uiState
    private val db = AppDatabase.getDatabase(application)
    private val testDao = db.testResultDao()
    private val mdnsManager = MdnsManager(application)

    fun startScan() {
        if (_uiState.value.isScanning) return
        
        _uiState.value = _uiState.value.copy(isScanning = true, services = emptyList())
        
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            
            mdnsManager.startDiscovery(object : MdnsManager.MdnsCallback {
                override fun onServiceFound(service: MdnsService) {
                    val currentList = _uiState.value.services.toMutableList()
                    if (currentList.none { it.name == service.name && it.type == service.type }) {
                        currentList.add(service)
                        _uiState.value = _uiState.value.copy(services = currentList)
                    }
                }

                override fun onScanFinished() {
                    val time = System.currentTimeMillis() - startTime
                    _uiState.value = _uiState.value.copy(
                        isScanning = false,
                        scanTimeMs = time
                    )
                    saveToHistory(_uiState.value.services.size, time)
                }
            })
        }
    }

    private fun saveToHistory(count: Int, time: Long) {
        viewModelScope.launch {
            testDao.insertResult(TestResult(
                type = "mDNS Scan",
                target = "Local Network",
                result = String.format(Locale.US, "%d Services • %.1fs", count, time / 1000f)
            ))
        }
    }

    override fun onCleared() {
        super.onCleared()
        mdnsManager.stopDiscovery()
    }
}
