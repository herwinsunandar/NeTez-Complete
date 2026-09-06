package com.herwin.netez.ui.viewmodels

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.data.AppDatabase
import com.herwin.netez.data.ScannedDevice
import com.herwin.netez.data.TestResult
import com.herwin.netez.data.VendorOverrideEntity
import com.herwin.netez.data.wifi.VendorDatabase
import com.herwin.netez.network.Device
import com.herwin.netez.network.LanScanner
import com.herwin.netez.utils.NetworkUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
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
    val progressText: String = "",
    val subnet: String = "192.168.1",
    val rangeStart: String = "192.168.1.1",
    val rangeEnd: String = "192.168.1.254",
    val singleIp: String = "192.168.1.1",
    val cidr: String = "192.168.1.0/24",
    val scanTimeMs: Long = 0,
    val filter: LanFilter = LanFilter.ALL,
    val mode: ScanMode = ScanMode.CIDR,
    val ispInfo: String? = null,
    val vendorMap: Map<String, String> = emptyMap(),
    val portScanningDevices: Set<String> = emptySet() // IPs currently scanning ports
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

    companion object {
        const val TCP_PING_ENABLED = true
        const val DEEP_SCAN_ENABLED = true
        const val TIMEOUT_MS = 500
        const val THREADS = 100
    }

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

        viewModelScope.launch {
            VendorDatabase.vendorUpdates.collectLatest { mac ->
                val vendor = VendorDatabase.getVendorSync(mac)
                _uiState.update { state ->
                    val newDevices = state.devices.map { 
                        if (it.mac == mac) it.copy(vendor = vendor) else it 
                    }
                    val newMap = state.vendorMap.toMutableMap()
                    newMap[mac] = vendor
                    state.copy(devices = newDevices, vendorMap = newMap)
                }
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
        val context = getApplication<Application>()
        _uiState.update { it.copy(isScanning = true, devices = emptyList(), progress = 0f, progressText = "Preparing scan...") }
        
        val currentDevices = ConcurrentHashMap<String, Device>()
        val scanStartTime = System.currentTimeMillis()

        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            // Ensure Vendor Database is loaded
            VendorDatabase.load(context)

            // Fetch ISP Info in background
            launch {
                val isp = NetworkUtils.getIspInfo()
                _uiState.update { it.copy(ispInfo = isp) }
            }

            val time = measureTimeMillis {
                try {
                    when (_uiState.value.mode) {
                        ScanMode.SINGLE -> {
                            LanScanner.scanRange(context, _uiState.value.singleIp, _uiState.value.singleIp, gateway, onDeviceFound = { device ->
                                currentDevices[device.ip] = device
                                _uiState.update { it.copy(devices = currentDevices.values.sortedBy { NetworkUtils.ipToLong(it.ip) }) }
                            }) { current, total ->
                                val text = "Scanning $current/$total • Found ${currentDevices.size}"
                                _uiState.update { it.copy(progress = current.toFloat() / total, progressText = text) }
                            }
                        }
                        ScanMode.RANGE -> {
                            LanScanner.scanRange(context, _uiState.value.rangeStart, _uiState.value.rangeEnd, gateway, onDeviceFound = { device ->
                                currentDevices[device.ip] = device
                                _uiState.update { it.copy(devices = currentDevices.values.sortedBy { NetworkUtils.ipToLong(it.ip) }) }
                            }) { current, total ->
                                val text = "Scanning $current/$total • Found ${currentDevices.size}"
                                _uiState.update { it.copy(progress = current.toFloat() / total, progressText = text) }
                            }
                        }
                        ScanMode.CIDR -> {
                            val subnet = _uiState.value.cidr.substringBeforeLast(".")
                            LanScanner.scanSubnet(context, subnet, gateway, onDeviceFound = { device ->
                                currentDevices[device.ip] = device
                                _uiState.update { it.copy(devices = currentDevices.values.sortedBy { NetworkUtils.ipToLong(it.ip) }) }
                            }) { current, total ->
                                val text = "Found ${currentDevices.size} devices • Scanning $current/$total"
                                _uiState.update { it.copy(progress = current.toFloat() / total, progressText = text) }
                            }
                        }
                    }
                } catch (e: CancellationException) {
                    _uiState.update { it.copy(isScanning = false, progress = 0f, progressText = "Scan cancelled") }
                    return@launch
                } finally {
                    val finalElapsed = (System.currentTimeMillis() - scanStartTime) / 1000f
                    
                    // Merge with offline devices from DB
                    val allHistory = deviceDao.getAllDevices()
                    allHistory.forEach { dbDev ->
                        if (!currentDevices.containsKey(dbDev.ip) && !currentDevices.values.any { it.mac == dbDev.mac }) {
                            val isInRange = when (_uiState.value.mode) {
                                ScanMode.SINGLE -> dbDev.ip == _uiState.value.singleIp
                                ScanMode.RANGE -> {
                                    val ipL = NetworkUtils.ipToLong(dbDev.ip)
                                    ipL >= NetworkUtils.ipToLong(_uiState.value.rangeStart) && 
                                    ipL <= NetworkUtils.ipToLong(_uiState.value.rangeEnd)
                                }
                                ScanMode.CIDR -> dbDev.ip.startsWith(_uiState.value.cidr.substringBeforeLast("."))
                            }
                            
                            if (isInRange) {
                                currentDevices[dbDev.ip] = Device(
                                    ip = dbDev.ip,
                                    hostname = dbDev.hostname,
                                    mac = dbDev.mac,
                                    vendor = dbDev.vendor,
                                    deviceCategory = dbDev.deviceCategory,
                                    deviceType = dbDev.deviceType,
                                    os = dbDev.os,
                                    isOnline = false,
                                    lastSeen = dbDev.lastSeen,
                                    firstSeen = dbDev.firstSeen
                                )
                            }
                        }
                    }
                    
                    val sortedList = currentDevices.values.sortedBy { NetworkUtils.ipToLong(it.ip) }
                    val finalOnlineCount = currentDevices.values.count { it.isOnline }
                    val finalText = "Found $finalOnlineCount devices • Finished in ${String.format(Locale.US, "%.1fs", finalElapsed)}"
                    _uiState.update { it.copy(isScanning = false, progress = 1f, progressText = finalText, devices = sortedList) }
                }
            }
            
            val finalDevices = _uiState.value.devices
            _uiState.update { it.copy(isScanning = false, progress = 1f) }
            
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
                            deviceCategory = device.deviceCategory,
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

        viewModelScope.launch {
            VendorDatabase.vendorUpdates.collectLatest { mac ->
                val vendor = VendorDatabase.getVendorSync(mac)
                _uiState.update { state ->
                    val newDevices = state.devices.map { 
                        if (it.mac == mac) it.copy(vendor = vendor) else it 
                    }
                    val newMap = state.vendorMap.toMutableMap()
                    newMap[mac] = vendor
                    state.copy(devices = newDevices, vendorMap = newMap)
                }
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        _uiState.value = _uiState.value.copy(isScanning = false, progress = 0f)
    }

    fun togglePortScan(deviceIp: String) {
        if (_uiState.value.portScanningDevices.contains(deviceIp)) return

        _uiState.update { it.copy(portScanningDevices = it.portScanningDevices + deviceIp) }
        
        viewModelScope.launch {
            val ports = LanScanner.scanPorts(deviceIp)
            _uiState.update { state ->
                val newDevices = state.devices.map { 
                    if (it.ip == deviceIp) it.copy(openPorts = ports, hasPerformedPortScan = true) else it 
                }
                state.copy(
                    devices = newDevices,
                    portScanningDevices = state.portScanningDevices - deviceIp
                )
            }
        }
    }

    fun saveVendorOverride(mac: String, vendor: String) {
        viewModelScope.launch(Dispatchers.IO) {
            db.vendorOverrideDao().insertOverride(VendorOverrideEntity(mac, vendor))
            // Refresh current list if possible
            _uiState.update { state ->
                state.copy(devices = state.devices.map { 
                    if (it.mac == mac) it.copy(vendor = vendor) else it 
                })
            }
        }

        viewModelScope.launch {
            VendorDatabase.vendorUpdates.collectLatest { mac ->
                val vendor = VendorDatabase.getVendorSync(mac)
                _uiState.update { state ->
                    val newDevices = state.devices.map { 
                        if (it.mac == mac) it.copy(vendor = vendor) else it 
                    }
                    val newMap = state.vendorMap.toMutableMap()
                    newMap[mac] = vendor
                    state.copy(devices = newDevices, vendorMap = newMap)
                }
            }
        }
    }

    fun getShareText(): String {
        val state = _uiState.value
        val sb = StringBuilder("📋 NeTez LAN Scan Report\n")
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        sb.append("📅 Date: ${sdf.format(Date())}\n")
        sb.append("🌐 Target: ${when(state.mode) {
            ScanMode.SINGLE -> state.singleIp
            ScanMode.RANGE -> "${state.rangeStart} - ${state.rangeEnd}"
            ScanMode.CIDR -> state.cidr
        }}\n")
        sb.append("📊 Summary: ${state.devices.count { it.isOnline }} Online • ${state.devices.size} Total\n")
        sb.append("─────────────────────\n")
        
        state.devices.filter { it.isOnline }.forEach { device ->
            sb.append("• ${device.ip}\n")
            if (device.hostname != "Unknown" && device.hostname != device.ip) {
                sb.append("  Name: ${device.hostname}\n")
            }
            sb.append("  MAC: ${device.mac}\n")
            sb.append("  Vendor: ${device.vendor}\n")
            if (device.openPorts.isNotEmpty()) {
                sb.append("  Ports: ${device.openPorts.joinToString(", ")}\n")
            }
            sb.append("\n")
        }
        
        sb.append("─────────────────────\n")
        sb.append("Sent from NeTez Network Tools")
        return sb.toString()
    }

    fun copyResultsToClipboard(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("LAN Scan Results", getShareText())
        clipboard.setPrimaryClip(clip)
    }

    fun shareResults(context: Context) {
        val text = getShareText()
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(intent, "Share LAN Scan Results"))
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

        viewModelScope.launch {
            VendorDatabase.vendorUpdates.collectLatest { mac ->
                val vendor = VendorDatabase.getVendorSync(mac)
                _uiState.update { state ->
                    val newDevices = state.devices.map { 
                        if (it.mac == mac) it.copy(vendor = vendor) else it 
                    }
                    val newMap = state.vendorMap.toMutableMap()
                    newMap[mac] = vendor
                    state.copy(devices = newDevices, vendorMap = newMap)
                }
            }
        }
    }
}
