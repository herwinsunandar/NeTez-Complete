package com.herwin.netez.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.herwin.netez.entitlement.FeatureAccessManager
import com.herwin.netez.entitlement.FeatureId

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
import com.herwin.netez.network.BleScannerManager
import com.herwin.netez.network.Device
import com.herwin.netez.network.LanScanner
import com.herwin.netez.utils.NetworkUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.system.measureTimeMillis

enum class ScanMode {
    SINGLE, RANGE, CIDR
}

enum class LanFilter {
    ALL, ONLINE, GATEWAY, PHONE, PC, ROUTER, IOT, BLUETOOTH
}

enum class LanSortOption {
    IP, LATENCY, VENDOR, NAME
}

data class NetworkSummaryInfo(
    val gateway: String = "-",
    val subnetMask: String = "255.255.255.0",
    val dnsServer: String = "-",
    val dhcpServer: String = "-",
    val myIp: String = "-",
    val myMac: String = "-",
    val ssid: String = "-",
    val bssid: String = "-",
    val aliveCount: Int = 0,
    val scannedCount: Int = 0
)

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
    val sortOption: LanSortOption = LanSortOption.IP,
    val mode: ScanMode = ScanMode.CIDR,
    val ispInfo: String? = null,
    val networkSummary: NetworkSummaryInfo = NetworkSummaryInfo(),
    val vendorMap: Map<String, String> = emptyMap(),
    val portScanningDevices: Set<String> = emptySet(),
    val isApIsolationDetected: Boolean = false
) {
    val filteredDevices: List<Device>
        get() {
            val filtered = when (filter) {
                LanFilter.ALL -> devices
                LanFilter.ONLINE -> devices.filter { it.isOnline }
                LanFilter.GATEWAY -> devices.filter { it.isGateway }
                LanFilter.PHONE -> devices.filter { it.deviceType == "iPhone" || it.deviceType == "Android" }
                LanFilter.PC -> devices.filter { it.deviceType == "PC" || it.deviceType == "Mac" }
                LanFilter.ROUTER -> devices.filter { it.deviceType == "Router" }
                LanFilter.IOT -> devices.filter { it.deviceType == "IoT" || it.deviceType == "Printer" || it.deviceType == "Smart TV" }
                LanFilter.BLUETOOTH -> devices.filter { it.isBluetoothNearby || it.bluetoothName != null }
            }

            return when (sortOption) {
                LanSortOption.IP -> filtered.sortedBy { NetworkUtils.ipToLong(it.ip) }
                LanSortOption.LATENCY -> filtered.sortedWith(compareBy<Device> { !it.isOnline }.thenBy { it.latencyMs })
                LanSortOption.VENDOR -> filtered.sortedBy { it.vendor.lowercase() }
                LanSortOption.NAME -> filtered.sortedBy { it.hostname.lowercase() }
            }
        }
}

class LanScannerViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(LanScannerState())
    val uiState: StateFlow<LanScannerState> = _uiState
    private val db = AppDatabase.getDatabase(application)
    private val testDao = db.testResultDao()
    private val deviceDao = db.scannedDeviceDao()
    private val bleScannerManager = BleScannerManager(application)
    private var scanJob: Job? = null

    var showGlobalSupportDialog by mutableStateOf(false)

    companion object {
        const val TCP_PING_ENABLED = true
        const val DEEP_SCAN_ENABLED = true
        const val TIMEOUT_MS = 500
        const val THREADS = 100
    }

    init {
        val subnetInfo = NetworkUtils.getSubnetInfo(application)
        val ip = NetworkUtils.getLocalIpAddress()
        if (subnetInfo != null) {
            _uiState.value = _uiState.value.copy(
                subnet = subnetInfo.networkAddress.substringBeforeLast("."),
                cidr = "${subnetInfo.networkAddress}/${subnetInfo.prefixLength}",
                rangeStart = subnetInfo.startIp,
                rangeEnd = subnetInfo.endIp,
                singleIp = ip ?: subnetInfo.ip
            )
        } else if (ip != null) {
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

        loadCachedHistory()
        updateNetworkSummary()

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

    private fun loadCachedHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            val allHistory = deviceDao.getAllDevices()
            if (allHistory.isNotEmpty()) {
                val cachedDevices = allHistory.map { dbDev ->
                    Device(
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
                }.sortedBy { NetworkUtils.ipToLong(it.ip) }

                if (_uiState.value.devices.isEmpty()) {
                    _uiState.update { it.copy(devices = cachedDevices) }
                }
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            deviceDao.clearAllDevices()
            _uiState.update { it.copy(devices = emptyList()) }
        }
    }

    fun deleteDevice(mac: String) {
        viewModelScope.launch(Dispatchers.IO) {
            deviceDao.deleteDeviceByMac(mac)
            _uiState.update { state ->
                state.copy(devices = state.devices.filter { it.mac != mac })
            }
            updateNetworkSummary()
        }
    }

    fun updateSortOption(sortOption: LanSortOption) {
        _uiState.update { it.copy(sortOption = sortOption) }
    }

    fun updateNetworkSummary() {
        val context = getApplication<Application>()
        val subnetInfo = NetworkUtils.getSubnetInfo(context)
        val gateway = NetworkUtils.getGateway(context)
        val wifiDetails = NetworkUtils.getWifiDetails(context)
        val myIp = NetworkUtils.getLocalIpAddress() ?: "-"
        val myMac = NetworkUtils.getMacAddress() ?: "-"

        _uiState.update { state ->
            state.copy(
                networkSummary = NetworkSummaryInfo(
                    gateway = if (gateway.isNotEmpty()) gateway else "-",
                    subnetMask = subnetInfo?.netmask ?: "255.255.255.0",
                    dnsServer = if (gateway.isNotEmpty()) gateway else "1.1.1.1",
                    dhcpServer = if (gateway.isNotEmpty()) gateway else "-",
                    myIp = myIp,
                    myMac = myMac,
                    ssid = wifiDetails["SSID"] ?: "WiFi Network",
                    bssid = wifiDetails["BSSID"] ?: "-",
                    aliveCount = state.devices.count { it.isOnline },
                    scannedCount = if (state.devices.isNotEmpty()) state.devices.size else 254
                )
            )
        }
    }

    fun exportCsv(context: Context) {
        val state = _uiState.value
        val sb = StringBuilder("IP,Hostname,MAC,Vendor,DeviceType,OS,Status,LatencyMs,OpenPorts\n")
        state.devices.forEach { dev ->
            sb.append("\"${dev.ip}\",\"${dev.hostname}\",\"${dev.mac}\",\"${dev.vendor}\",\"${dev.deviceType}\",\"${dev.os}\",\"${if (dev.isOnline) "Online" else "Offline"}\",${dev.latencyMs},\"${dev.openPorts.joinToString(";")}\"\n")
        }
        shareExportText(context, sb.toString(), "text/csv")
    }

    fun exportJson(context: Context) {
        val state = _uiState.value
        val jsonArray = JSONArray()
        state.devices.forEach { dev ->
            val obj = JSONObject()
            obj.put("ip", dev.ip)
            obj.put("hostname", dev.hostname)
            obj.put("mac", dev.mac)
            obj.put("vendor", dev.vendor)
            obj.put("deviceType", dev.deviceType)
            obj.put("os", dev.os)
            obj.put("isOnline", dev.isOnline)
            obj.put("latencyMs", dev.latencyMs)
            obj.put("openPorts", JSONArray(dev.openPorts))
            jsonArray.put(obj)
        }
        shareExportText(context, jsonArray.toString(2), "text/plain")
    }

    private fun shareExportText(context: Context, content: String, mimeType: String) {
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, content)
            type = mimeType
        }
        context.startActivity(Intent.createChooser(intent, "Export LAN Scan Report"))
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
            // Check connection type
            val connectionType = NetworkUtils.getConnectionType(context)
            if (connectionType == "Cellular") {
                _uiState.update { it.copy(
                    isScanning = false, 
                    progressText = "LAN Scan not supported on Cellular network",
                    progress = 0f,
                    devices = emptyList()
                ) }
                return@launch
            }

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
            val onlineCount = finalDevices.count { it.isOnline }
            val isApIsolation = (onlineCount <= 2 && finalDevices.size >= 10)
            _uiState.update { it.copy(isScanning = false, progress = 1f, isApIsolationDetected = isApIsolation) }
            
            saveToHistory(finalDevices.size, time)
            correlateBluetoothProximity()
            updateNetworkSummary()

            val accessManager = FeatureAccessManager.getInstance(getApplication())
            if (accessManager.trackAndCheckGlobalToolUsage()) {
                showGlobalSupportDialog = true
            }
            
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

    fun correlateBluetoothProximity() {
        viewModelScope.launch {
            try {
                bleScannerManager.startScan()
                delay(2500)
                bleScannerManager.stopScan()

                val bleDevices = bleScannerManager.foundDevices.value
                if (bleDevices.isNotEmpty()) {
                    _uiState.update { state ->
                        val updated = state.devices.map { lanDev ->
                            val matchedBle = bleDevices.firstOrNull { ble ->
                                val bleName = ble.name.trim()
                                val lanHost = lanDev.hostname.trim()
                                val netbios = lanDev.netbiosName?.trim() ?: ""

                                (bleName.isNotBlank() && bleName != "Unknown Device" && (
                                    lanHost.contains(bleName, ignoreCase = true) ||
                                    bleName.contains(lanHost, ignoreCase = true) ||
                                    (netbios.isNotBlank() && netbios.contains(bleName, ignoreCase = true))
                                )) ||
                                (lanDev.vendor != "Unknown" && ble.vendor != "Unknown" && 
                                 lanDev.vendor.contains(ble.vendor, ignoreCase = true))
                            }

                            if (matchedBle != null) {
                                lanDev.copy(
                                    bluetoothName = matchedBle.name,
                                    bluetoothMac = matchedBle.mac,
                                    bluetoothRssi = matchedBle.rssi,
                                    bluetoothDeviceType = matchedBle.deviceType.name,
                                    isBluetoothNearby = true
                                )
                            } else {
                                lanDev
                            }
                        }
                        state.copy(devices = updated)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun manuallyLinkBluetooth(deviceIp: String, bleName: String, bleMac: String, rssi: Int, deviceType: String) {
        _uiState.update { state ->
            val updated = state.devices.map { 
                if (it.ip == deviceIp) {
                    it.copy(
                        bluetoothName = bleName,
                        bluetoothMac = bleMac,
                        bluetoothRssi = rssi,
                        bluetoothDeviceType = deviceType,
                        isBluetoothNearby = true
                    )
                } else it
            }
            state.copy(devices = updated)
        }
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

    fun saveVendorOverride(ip: String, mac: String, vendor: String) {
        val cleanMac = mac.uppercase().replace("-", ":")
        val effectiveKey = if (cleanMac != "00:00:00:00:00:00" && cleanMac.isNotBlank()) cleanMac else ip

        viewModelScope.launch(Dispatchers.IO) {
            db.vendorOverrideDao().insertOverride(VendorOverrideEntity(effectiveKey, vendor))
            VendorDatabase.setCustomOverride(effectiveKey, vendor)
            if (cleanMac.isNotBlank() && cleanMac != "00:00:00:00:00:00") {
                VendorDatabase.setCustomOverride(cleanMac, vendor)
            }

            val dbDev = if (cleanMac.isNotBlank() && cleanMac != "00:00:00:00:00:00") {
                deviceDao.getDeviceByMac(cleanMac)
            } else null
            if (dbDev != null) {
                deviceDao.insertDevice(dbDev.copy(vendor = vendor))
            }

            _uiState.update { state ->
                val newDevices = state.devices.map { 
                    if ((cleanMac != "00:00:00:00:00:00" && it.mac.equals(cleanMac, ignoreCase = true)) || it.ip == ip) {
                        it.copy(vendor = vendor)
                    } else it 
                }
                val newMap = state.vendorMap.toMutableMap()
                newMap[effectiveKey] = vendor
                if (cleanMac.isNotBlank()) newMap[cleanMac] = vendor
                state.copy(devices = newDevices, vendorMap = newMap)
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
