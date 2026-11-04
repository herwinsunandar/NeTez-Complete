package com.herwin.netez.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.herwin.netez.entitlement.FeatureAccessManager

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.data.AppDatabase
import com.herwin.netez.data.TestResult
import com.herwin.netez.network.PortResult
import com.herwin.netez.network.PortScanner
import com.herwin.netez.utils.NetworkUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.system.measureTimeMillis

enum class PortFilter {
    ALL, OPEN, CLOSED
}

enum class PortRangeOption {
    COMMON, TOP_100, SYSTEM, CUSTOM
}

data class PortScannerState(
    val ip: String = "",
    val results: List<PortResult> = emptyList(),
    val isScanning: Boolean = false,
    val filter: PortFilter = PortFilter.ALL,
    val rangeOption: PortRangeOption = PortRangeOption.COMMON,
    val customRange: String = "1-1024",
    val protocol: String = "TCP",
    val scanTimeMs: Long = 0,
    val deviceType: String = "Unknown Device"
) {
    val filteredResults: List<PortResult>
        get() = when (filter) {
            PortFilter.ALL -> results
            PortFilter.OPEN -> results.filter { it.isOpen }
            PortFilter.CLOSED -> results.filter { !it.isOpen }
        }

    val openCount: Int get() = results.count { it.isOpen }
}

class PortScannerViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(PortScannerState())
    val uiState: StateFlow<PortScannerState> = _uiState
    private val dao = AppDatabase.getDatabase(application).testResultDao()

    var showGlobalSupportDialog by mutableStateOf(false)

    fun updateIp(newIp: String) {
        _uiState.value = _uiState.value.copy(ip = newIp)
    }

    fun updateFilter(newFilter: PortFilter) {
        _uiState.value = _uiState.value.copy(filter = newFilter)
    }

    fun updateRange(option: PortRangeOption, custom: String = "") {
        _uiState.value = _uiState.value.copy(
            rangeOption = option,
            customRange = if (custom.isNotEmpty()) custom else _uiState.value.customRange
        )
    }

    fun toggleProtocol() {
        val newProto = if (_uiState.value.protocol == "TCP") "UDP" else "TCP"
        _uiState.value = _uiState.value.copy(protocol = newProto)
    }

    fun startScan() {
        val state = _uiState.value
        val targetIp = state.ip.trim().ifBlank {
            NetworkUtils.getLocalIpAddress() ?: "192.168.1.1"
        }

        val portsToScan = when (state.rangeOption) {
            PortRangeOption.COMMON -> PortScanner.commonPorts
            PortRangeOption.TOP_100 -> PortScanner.top100Ports
            PortRangeOption.SYSTEM -> (1..1024).toList()
            PortRangeOption.CUSTOM -> parseRange(state.customRange)
        }

        _uiState.value = _uiState.value.copy(
            isScanning = true,
            results = emptyList(),
            scanTimeMs = 0,
            ip = state.ip.ifBlank { targetIp }
        )

        viewModelScope.launch {
            var results: List<PortResult>
            val time = measureTimeMillis {
                results = PortScanner.scanPorts(targetIp, portsToScan, state.protocol)
            }

            val detectedDevice = detectDevice(results)

            _uiState.value = _uiState.value.copy(
                isScanning = false,
                results = results,
                scanTimeMs = time,
                deviceType = detectedDevice
            )

            saveToHistory(results.count { it.isOpen }, portsToScan.size, time, detectedDevice)

            val accessManager = FeatureAccessManager.getInstance(getApplication())
            if (accessManager.trackAndCheckGlobalToolUsage()) {
                showGlobalSupportDialog = true
            }
        }
    }

    private fun parseRange(range: String): List<Int> {
        return try {
            if (range.contains("-")) {
                val parts = range.split("-")
                (parts[0].trim().toInt()..parts[1].trim().toInt()).toList()
            } else {
                range.split(",").map { it.trim().toInt() }
            }
        } catch (e: Exception) {
            PortScanner.commonPorts
        }
    }

    private fun detectDevice(results: List<PortResult>): String {
        return when {
            results.any { it.port == 8291 && it.isOpen } -> "MikroTik"
            results.any { it.port == 22 && it.isOpen } -> "Linux/Server"
            results.any { it.port == 80 && it.isOpen } -> "Web Server"
            results.any { it.port == 53 && it.isOpen } -> "DNS Server"
            else -> "Generic Device"
        }
    }

    private fun saveToHistory(open: Int, total: Int, time: Long, device: String) {
        viewModelScope.launch {
            dao.insertResult(
                TestResult(
                    type = "PortScan",
                    target = _uiState.value.ip,
                    result = String.format(Locale.US, "%d Open / %d Ports • %.1fs • %s", open, total, time / 1000f, device)
                )
            )
        }
    }
}
