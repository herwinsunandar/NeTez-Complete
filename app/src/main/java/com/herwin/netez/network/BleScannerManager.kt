package com.herwin.netez.network

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.SparseArray
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*
import kotlin.math.pow

data class BleDevice(
    val name: String,
    val mac: String,
    val vendor: String = "Unknown",
    val rssi: Int,
    val distance: Double = 0.0,
    val isPaired: Boolean = false,
    val deviceType: BleDeviceType = BleDeviceType.GENERIC,
    val batteryLevel: Int? = null,
    val uuids: List<String> = emptyList(),
    val nativeDevice: BluetoothDevice? = null,
    val isBle: Boolean = true,
    val manufacturerData: String? = null,
    val scanRecord: ScanRecord? = null,
    val lastSeen: Long = System.currentTimeMillis(),
    val reason: String? = null
)

enum class BleDeviceType {
    GENERIC, HEADPHONES, SMARTWATCH, PHONE, BEACON, COMPUTER
}

class BleScannerManager(private val context: Context) {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val scanner = bluetoothAdapter?.bluetoothLeScanner
    
    private val _foundDevices = MutableStateFlow<List<BleDevice>>(emptyList())
    val foundDevices: StateFlow<List<BleDevice>> = _foundDevices

    private val deviceMap = mutableMapOf<String, BleDevice>()
    private var scope = CoroutineScope(Dispatchers.Main + Job())

    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    val rssi: Short = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)
                    device?.let { addOrUpdateDevice(it, rssi.toInt(), null, isBle = false) }
                }
                BluetoothDevice.ACTION_NAME_CHANGED -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    device?.let { addOrUpdateDevice(it, null, null, isBle = false) }
                }
            }
        }
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            addOrUpdateDevice(result.device, result.rssi, result.scanRecord, isBle = true)
        }
    }

    @SuppressLint("MissingPermission")
    private fun addOrUpdateDevice(
        device: BluetoothDevice,
        rssi: Int?,
        scanRecord: ScanRecord? = null,
        isBle: Boolean = true,
        manualName: String? = null
    ) {
        val mac = device.address
        val currentRssi = rssi ?: deviceMap[mac]?.rssi ?: -100
        
        // Filter weak/spam devices
        if (currentRssi < -95 && (device.name == null && scanRecord?.deviceName == null && manualName == null)) return

        val name = manualName ?: getDeviceNameCompat(device, scanRecord)
        val vendor = detectVendor(mac)
        
        val manufacturerInfo = scanRecord?.let { parseManufacturerData(it.manufacturerSpecificData) }
        val reason = determineReason(name, scanRecord)

        val existing = deviceMap[mac]
        val finalName = if (name == "Unknown Device" && existing != null && existing.name != "Unknown Device") {
            existing.name
        } else {
            name
        }

        val bleDevice = BleDevice(
            name = finalName,
            mac = mac,
            vendor = vendor,
            rssi = currentRssi,
            distance = calculateDistance(currentRssi),
            isPaired = device.bondState == BluetoothDevice.BOND_BONDED,
            deviceType = detectType(finalName, device.bluetoothClass?.majorDeviceClass),
            uuids = scanRecord?.serviceUuids?.map { it.toString() } ?: existing?.uuids ?: emptyList(),
            nativeDevice = device,
            isBle = existing?.isBle ?: isBle,
            manufacturerData = manufacturerInfo ?: existing?.manufacturerData,
            scanRecord = scanRecord ?: existing?.scanRecord,
            lastSeen = System.currentTimeMillis(),
            reason = reason
        )

        deviceMap[mac] = bleDevice
        updateList()
    }

    private fun updateList() {
        val now = System.currentTimeMillis()
        // Remove devices not seen for 10 seconds if they are weak and unnamed
        val filtered = deviceMap.values.filter { 
            val isOld = now - it.lastSeen > 10000
            val isWeak = it.rssi < -90
            val isUnknown = it.name == "Unknown Device"
            !(isOld && (isWeak || isUnknown))
        }.sortedByDescending { it.rssi }
        
        _foundDevices.value = filtered
    }

    private fun getDeviceNameCompat(device: BluetoothDevice, scanRecord: ScanRecord?): String {
        @SuppressLint("MissingPermission")
        var name = scanRecord?.deviceName
        if (name.isNullOrEmpty()) {
            name = try { device.name } catch (e: SecurityException) { null }
        }
        
        if (name.isNullOrEmpty()) {
            val manuData = scanRecord?.manufacturerSpecificData
            if (manuData != null && manuData.size() > 0) {
                for (i in 0 until manuData.size()) {
                    val id = manuData.keyAt(i)
                    val mName = getManufacturerName(id)
                    if (mName != null) return "$mName Device"
                }
            }
        }

        if (name.isNullOrEmpty()) {
            val vendor = detectVendor(device.address)
            if (vendor != "Unknown" && vendor != "Random Address") {
                return "$vendor Device"
            }
        }

        return name ?: "Unknown Device"
    }

    private fun determineReason(name: String, scanRecord: ScanRecord?): String? {
        if (name != "Unknown Device") return null
        if (scanRecord == null) return "No name in advertisement (Classic/Random MAC)"
        if (scanRecord.manufacturerSpecificData.size() > 0) return "Beacon • No broadcast name"
        return "Hidden name (Encrypted/Private)"
    }

    private fun parseManufacturerData(data: SparseArray<ByteArray>?): String? {
        if (data == null || data.size() == 0) return null
        val id = data.keyAt(0)
        val name = getManufacturerName(id)
        return name ?: "ID: 0x${Integer.toHexString(id).uppercase()}"
    }

    private fun getManufacturerName(companyId: Int): String? {
        return when (companyId) {
            0x004C -> "Apple"
            0x0006 -> "Microsoft"
            0x0075 -> "Samsung"
            0x038F -> "Xiaomi"
            0x0008 -> "Motorola"
            0x00E0 -> "Google"
            0x000F -> "Broadcom"
            0x0059 -> "Nordic Semi"
            0x000A -> "Qualcomm"
            0x0057 -> "Intel"
            0x0000 -> "Ericsson"
            0x0001 -> "Nokia"
            0x04CC -> "Nothing"
            0x001D -> "Sony"
            else -> null
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        deviceMap.clear()
        _foundDevices.value = emptyList()
        
        // 1. BLE Scan
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        scanner?.startScan(null, settings, scanCallback)
        
        // 2. Classic Scan
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothDevice.ACTION_NAME_CHANGED)
        }
        context.registerReceiver(receiver, filter)
        bluetoothAdapter?.startDiscovery()
        
        // Periodic list cleanup
        scope.launch {
            while (isActive) {
                delay(5000)
                updateList()
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        scanner?.stopScan(scanCallback)
        try {
            context.unregisterReceiver(receiver)
        } catch (e: Exception) { }
        bluetoothAdapter?.cancelDiscovery()
        scope.coroutineContext.cancelChildren()
    }

    @SuppressLint("MissingPermission")
    fun pairDevice(device: BluetoothDevice): Boolean {
        return if (device.bondState == BluetoothDevice.BOND_NONE) {
            device.createBond()
        } else {
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun fetchNameGatt(mac: String, onResult: (String?) -> Unit) {
        val device = bluetoothAdapter?.getRemoteDevice(mac) ?: return
        
        device.connectGatt(context, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    gatt.close()
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                val service = gatt.getService(UUID.fromString("00001800-0000-1000-8000-00805f9b34fb"))
                val char = service?.getCharacteristic(UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb"))
                if (char != null) {
                    gatt.readCharacteristic(char)
                } else {
                    gatt.disconnect()
                }
            }

            @Suppress("DEPRECATION")
            override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val name = characteristic.getStringValue(0)
                    if (!name.isNullOrEmpty()) {
                        addOrUpdateDevice(gatt.device, null, null, isBle = true, manualName = name)
                        onResult(name)
                    }
                }
                gatt.disconnect()
            }
        })
        
        // Timeout
        scope.launch {
            delay(5000)
            onResult(null)
        }
    }

    private fun calculateDistance(rssi: Int): Double {
        val txPower = -59 
        if (rssi == 0 || rssi == Short.MIN_VALUE.toInt()) return -1.0
        val ratio = rssi.toDouble() / txPower
        return if (ratio < 1.0) {
            ratio.pow(10.0)
        } else {
            (0.89976) * ratio.pow(7.7095) + 0.111
        }
    }

    private fun detectType(name: String, majorClass: Int?): BleDeviceType {
        val lowName = name.lowercase()
        if (majorClass != null) {
            when (majorClass) {
                BluetoothClass.Device.Major.AUDIO_VIDEO -> return BleDeviceType.HEADPHONES
                BluetoothClass.Device.Major.COMPUTER -> return BleDeviceType.COMPUTER
                BluetoothClass.Device.Major.PHONE -> return BleDeviceType.PHONE
                BluetoothClass.Device.Major.WEARABLE -> return BleDeviceType.SMARTWATCH
            }
        }
        return when {
            lowName.contains("buds") || lowName.contains("headset") || lowName.contains("audio") || lowName.contains("ear") || lowName.contains("tune") || lowName.contains("beat") -> BleDeviceType.HEADPHONES
            lowName.contains("watch") || lowName.contains("band") || lowName.contains("wear") || lowName.contains("fit") || lowName.contains("m-pencil") -> BleDeviceType.SMARTWATCH
            lowName.contains("phone") || lowName.contains("android") || lowName.contains("iphone") || lowName.contains("mobile") || lowName.contains("realme") || lowName.contains("s2") || lowName.contains("ultra") -> BleDeviceType.PHONE
            lowName.contains("desktop") || lowName.contains("pc") || lowName.contains("laptop") || lowName.contains("macbook") || lowName.contains("mac") -> BleDeviceType.COMPUTER
            else -> BleDeviceType.GENERIC
        }
    }

    private fun detectVendor(mac: String): String {
        val cleanMac = mac.uppercase().replace(":", "")
        val prefix = cleanMac.take(6)
        
        val firstByte = cleanMac.take(2).toIntOrNull(16) ?: 0
        val isRandom = (firstByte and 0x02) != 0 
        
        return when (prefix) {
            "C34A12", "C47C8D", "48D6D5", "D4F513", "2C8DB1", "A4C138", "34B1F7" -> "Xiaomi"
            "0017F2", "00236C", "F0D1A9", "002376", "E425E7", "ACF23C", "4021EA", "A07817", "10B588" -> "Apple"
            "001A7D", "F8D0BD", "7E0D6C", "4939AE", "4B0A3F" -> "Sony / Edifier"
            "44650D" -> "Amazon"
            "001B66", "AC5AF0", "7CD30A", "F8E61A", "28CDC4", "72607D", "C40F08", "74190A", "081093" -> "Samsung"
            "F44EFD" -> "Google"
            "7061E7", "EC3CBB" -> "Huawei"
            "45AE1F", "6C74F8" -> "Nothing (CMF)"
            "D1F9C1", "55EC63" -> "JBL"
            "A4403D", "A4405B", "38FBC6", "41BB00" -> "Baseus / Bass"
            "D028BA" -> "Realme"
            "7A581F", "437907" -> "Intel / PC"
            "5D7E6B", "53DBE3" -> "Personal Device"
            else -> if (isRandom) "Random Address" else "Unknown"
        }
    }
}
