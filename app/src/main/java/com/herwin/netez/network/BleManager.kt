package com.herwin.netez.network

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    val nativeDevice: BluetoothDevice? = null
)

enum class BleDeviceType {
    GENERIC, HEADPHONES, SMARTWATCH, PHONE, BEACON
}

class BleScannerManager(private val context: Context) {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val scanner = bluetoothAdapter?.bluetoothLeScanner
    
    private val _foundDevices = MutableStateFlow<List<BleDevice>>(emptyList())
    val foundDevices: StateFlow<List<BleDevice>> = _foundDevices

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val name = device.name ?: "Unknown Device"
            val mac = device.address
            val rssi = result.rssi
            
            val vendor = detectVendor(mac)
            
            val bleDevice = BleDevice(
                name = name,
                mac = mac,
                vendor = vendor,
                rssi = rssi,
                distance = calculateDistance(rssi),
                isPaired = device.bondState == BluetoothDevice.BOND_BONDED,
                deviceType = detectType(name),
                uuids = result.scanRecord?.serviceUuids?.map { it.toString() } ?: emptyList(),
                nativeDevice = device
            )

            val currentList = _foundDevices.value.toMutableList()
            val index = currentList.indexOfFirst { it.mac == mac }
            if (index != -1) {
                currentList[index] = bleDevice
            } else {
                currentList.add(bleDevice)
            }
            _foundDevices.value = currentList
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        _foundDevices.value = emptyList()
        scanner?.startScan(scanCallback)
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        scanner?.stopScan(scanCallback)
    }

    @SuppressLint("MissingPermission")
    fun pairDevice(device: BluetoothDevice): Boolean {
        return if (device.bondState == BluetoothDevice.BOND_NONE) {
            device.createBond()
        } else {
            false
        }
    }

    private fun calculateDistance(rssi: Int): Double {
        val txPower = -59 
        if (rssi == 0) return -1.0
        val ratio = rssi.toDouble() / txPower
        return if (ratio < 1.0) {
            ratio.pow(10.0)
        } else {
            (0.89976) * ratio.pow(7.7095) + 0.111
        }
    }

    private fun detectType(name: String): BleDeviceType {
        val lowName = name.lowercase()
        return when {
            lowName.contains("buds") || lowName.contains("headset") || lowName.contains("audio") -> BleDeviceType.HEADPHONES
            lowName.contains("watch") || lowName.contains("band") || lowName.contains("wear") -> BleDeviceType.SMARTWATCH
            lowName.contains("phone") || lowName.contains("android") || lowName.contains("iphone") -> BleDeviceType.PHONE
            else -> BleDeviceType.GENERIC
        }
    }

    private fun detectVendor(mac: String): String {
        val prefix = mac.uppercase().replace(":", "").take(6)
        return when (prefix) {
            "C34A12", "C47C8D" -> "Xiaomi"
            "0017F2", "00236C", "F0D1A9" -> "Apple"
            "001A7D" -> "Sony"
            "44650D" -> "Amazon"
            "000000" -> "Generic"
            else -> "Unknown"
        }
    }
}
