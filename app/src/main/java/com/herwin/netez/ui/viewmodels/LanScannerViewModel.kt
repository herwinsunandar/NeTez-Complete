package com.herwin.netez.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.data.AppDatabase
import com.herwin.netez.data.ScannedDevice
import com.herwin.netez.data.TestResult
import com.herwin.netez.network.Device
import com.herwin.netez.network.LanScanner
import com.herwin.netez.utils.NetworkUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.system.measureTimeMillis

enum class ScanMode {
    SINGLE, RANGE, CIDR
}

enum class LanFilter {
    ALL, ONLINE, GATEWAY, PHONE, PC, ROUTER, IOT
}

data class LanScannerState(
    val devices: List<Device> = emptyList(),
    val isScanning: Boolean = false,
    val progress: Float = 0f,
    val subnet: String = "192.168.1",
    val rangeStart: String = "192.168.1.1",
    val rangeEnd: String = "192.168.1.100",
    val singleIp: String = "192.168.1.1",
    val cidr: String = "192.168.1.0/24",
    val scanTimeMs: Long = 0,
    val filter: LanFilter = LanFilter.ALL,
    val mode: ScanMode = ScanMode.CIDR
) {
    val filteredDevices: List<Device>
        get() = when (filter) {
            LanFilter.ALL -> devices
            LanFilter.ONLINE -> devices.filter { it.isOnline }
            LanFilter.GATEWAY -> devices.filter { it.isGateway }
            LanFilter.PHONE -> devices.filter { it.deviceType == "iPhone" || it.deviceType == "Android" }
            LanFilter.PC -> devices.filter { it.deviceType == "PC" || it.deviceType == "Mac" }
            LanFilter.ROUTER -> devices.filter { it.deviceType == "Router" }
            LanFilter.IOT -> devices.filter { it.deviceType == "IoT" || it.deviceType == "Printer" || it.deviceType == "Smart TV" }
        }
}

class LanScannerViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(LanScannerState())
    val uiState: StateFlow<LanScannerState> = _uiState
    private val db = AppDatabase.getDatabase(application)
    private val testDao = db.testResultDao()
    private val deviceDao = db.scannedDeviceDao()
    private var scanJob: Job? = null

    init {
        val ip = NetworkUtils.getLocalIpAddress()
        if (ip != null) {
            val parts = ip.split(".")
            if (parts.size == 4) {
                val subnet = "${parts[0]}.${parts[1]}.${parts[2]}"
                _uiState.value = _uiState.value.copy(
                    subnet = subnet,
                    cidr = "$subnet.0/24",
                    rangeStart = "$subnet.1",
                    rangeEnd = "$subnet.254",
                    singleIp = ip
                )
            }
        }
    }

    fun updateMode(mode: ScanMode) { _uiState.value = _uiState.value.copy(mode = mode) }
    fun updateCidr(cidr: String) { _uiState.value = _uiState.value.copy(cidr = cidr) }
    fun updateRangeStart(ip: String) { _uiState.value = _uiState.value.copy(rangeStart = ip) }
    fun updateRangeEnd(ip: String) { _uiState.value = _uiState.value.copy(rangeEnd = ip) }
    fun updateSingleIp(ip: String) { _uiState.value = _uiState.value.copy(singleIp = ip) }
    fun updateFilter(filter: LanFilter) { _uiState.value = _uiState.value.copy(filter = filter) }

    fun startScan() {
        val gateway = NetworkUtils.getGateway(getApplication())
        _uiState.update { it.copy(isScanning = true, devices = emptyList(), progress = 0f) }
        
        val currentDevices = ConcurrentHashMap<String, Device>()

        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            val time = measureTimeMillis {
                try {
                    // Safety net timer: force 100% after 6.5s
                    launch {
                        delay(6500)
                        if (_uiState.value.isScanning) {
                            _uiState.update { it.copy(isScanning = false, progress = 1f) }
                        }
                    }

                    when (_uiState.value.mode) {
                        ScanMode.SINGLE -> {
                            LanScanner.scanRange(_uiState.value.singleIp, _uiState.value.singleIp, gateway, onDeviceFound = { device ->
                                currentDevices[device.ip] = device
                                _uiState.update { it.copy(devices = currentDevices.values.sortedBy { NetworkUtils.ipToLong(it.ip) }) }
                            }) { current, total ->
                                _uiState.update { it.copy(progress = current.toFloat() / total) }
                            }
                        }
                        ScanMode.RANGE -> {
                            LanScanner.scanRange(_uiState.value.rangeStart, _uiState.value.rangeEnd, gateway, onDeviceFound = { device ->
                                currentDevices[device.ip] = device
                                _uiState.update { it.copy(devices = currentDevices.values.sortedBy { NetworkUtils.ipToLong(it.ip) }) }
                            }) { current, total ->
                                _uiState.update { it.copy(progress = current.toFloat() / total) }
                            }
                        }
                        ScanMode.CIDR -> {
                            val subnet = _uiState.value.cidr.substringBeforeLast(".")
                            LanScanner.scanSubnet(getApplication(), subnet, gateway, onDeviceFound = { device ->
                                currentDevices[device.ip] = device
                                _uiState.update { it.copy(devices = currentDevices.values.sortedBy { NetworkUtils.ipToLong(it.ip) }) }
                            }) { current, total ->
                                _uiState.update { it.copy(progress = current.toFloat() / total) }
                            }
                        }
                    }
                } catch (e: CancellationException) {
                    _uiState.update { it.copy(isScanning = false, progress = 0f) }
                    return@launch
                } finally {
                    _uiState.update { it.copy(isScanning = false, progress = 1f) }
                }
            }
            
            val finalDevices = _uiState.value.devices
            _uiState.update { it.copy(
                isScanning = false, 
                scanTimeMs = time,
                progress = 1f
            ) }
            
            saveToHistory(finalDevices.size, time)
            
            // Background persistence
            viewModelScope.launch(Dispatchers.IO) {
                finalDevices.forEach { device ->
                    val dbDevice = deviceDao.getDeviceByMac(device.mac)
                    if (dbDevice == null) {
                        deviceDao.insertDevice(ScannedDevice(
                            mac = device.mac,
                            ip = device.ip,
                            hostname = device.hostname,
                            vendor = device.vendor,
                            deviceType = device.deviceType,
                            os = device.os,
                            firstSeen = System.currentTimeMillis(),
                            lastSeen = System.currentTimeMillis()
                        ))
                    } else {
                        deviceDao.updateLastSeen(device.mac, device.ip, System.currentTimeMillis())
                    }
                }
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        _uiState.value = _uiState.value.copy(isScanning = false, progress = 0f)
    }

    fun togglePortScan(deviceIp: String) {
        viewModelScope.launch {
            val ports = LanScanner.scanPorts(deviceIp)
            _uiState.update { state ->
                val newDevices = state.devices.map { 
                    if (it.ip == deviceIp) it.copy(openPorts = ports) else it 
                }
                state.copy(devices = newDevices)
            }
        }
    }

    private fun saveToHistory(count: Int, time: Long) {
        viewModelScope.launch {
            testDao.insertResult(TestResult(
                type = "LanScan",
                target = when(_uiState.value.mode) {
                    ScanMode.SINGLE -> _uiState.value.singleIp
                    ScanMode.RANGE -> "${_uiState.value.rangeStart}-${_uiState.value.rangeEnd}"
                    ScanMode.CIDR -> _uiState.value.cidr
                },
                result = String.format(Locale.US, "%d Devices • %.1fs", count, time / 1000f)
            ))
        }
    }
}
