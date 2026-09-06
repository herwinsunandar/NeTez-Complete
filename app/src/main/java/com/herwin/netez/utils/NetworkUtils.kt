package com.herwin.netez.utils

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.*
import com.herwin.netez.data.wifi.VendorDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URL
import java.util.Locale
import java.util.Scanner
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow

object NetworkUtils {

    data class SubnetInfo(
        val ip: String,
        val netmask: String,
        val networkAddress: String,
        val broadcastAddress: String,
        val startIp: String,
        val endIp: String,
        val prefixLength: Int
    )

    fun getSubnetInfo(context: Context): SubnetInfo? {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val dhcp = wifiManager.dhcpInfo
            
            val ipInt = dhcp?.ipAddress ?: 0
            val netmaskInt = dhcp?.netmask ?: 0
            
            // Fallback to NetworkInterface if DHCP info is missing or invalid
            if (ipInt == 0 || netmaskInt == 0) {
                val interfaces = NetworkInterface.getNetworkInterfaces()
                for (intf in interfaces) {
                    if (intf.isLoopback || !intf.isUp) continue
                    val interfaceAddresses = intf.interfaceAddresses
                    for (addr in interfaceAddresses) {
                        val inetAddr = addr.address
                        val hostAddr = inetAddr.hostAddress
                        if (inetAddr is Inet4Address && hostAddr != null) {
                            val ipLong = ipToLong(hostAddr)
                            
                            val prefix = addr.networkPrefixLength
                            val prefixLength = prefix.toInt().coerceIn(0, 32)
                            val maskLong = if (prefixLength == 0) 0xFFFFFF00L else (0xFFFFFFFFL shl (32 - prefixLength)) and 0xFFFFFFFFL
                            
                            val networkLong = ipLong and maskLong
                            val broadcastLong = networkLong or (maskLong.inv() and 0xFFFFFFFFL)
                            
                            return SubnetInfo(
                                ip = hostAddr,
                                netmask = longToIp(maskLong),
                                networkAddress = longToIp(networkLong),
                                broadcastAddress = longToIp(broadcastLong),
                                startIp = longToIp(networkLong + 1),
                                endIp = longToIp(broadcastLong - 1),
                                prefixLength = prefixLength
                            )
                        }
                    }
                }
            }
            
            if (ipInt == 0) return null
            
            // Handle dhcpInfo's format (usually little-endian)
            val ip = formatIp(ipInt)
            val finalNetmaskInt = if (netmaskInt == 0) 0x00FFFFFF else netmaskInt
            val netmask = formatIp(finalNetmaskInt)
            
            val ipLong = ipToLong(ip)
            val netmaskLong = ipToLong(netmask)
            
            val networkLong = ipLong and netmaskLong
            val broadcastLong = networkLong or (netmaskLong.inv() and 0xFFFFFFFFL)
            
            val prefixLength = java.lang.Long.bitCount(netmaskLong)
            
            return SubnetInfo(
                ip = ip,
                netmask = netmask,
                networkAddress = longToIp(networkLong),
                broadcastAddress = longToIp(broadcastLong),
                startIp = longToIp(networkLong + 1),
                endIp = longToIp(broadcastLong - 1),
                prefixLength = prefixLength
            )
        } catch (e: Exception) {
            return null
        }
    }

    fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            for (intf in interfaces) {
                val addrs = intf.inetAddresses ?: continue
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return null
    }

    fun getConnectionType(context: Context): String {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val nw = cm.activeNetwork ?: return "None"
            val actNw = cm.getNetworkCapabilities(nw) ?: return "None"
            return when {
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                else -> "Other"
            }
        } catch (e: Exception) {
            return "None"
        }
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    fun getWifiDetails(context: Context): Map<String, String> {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            if (!wifiManager.isWifiEnabled) return emptyMap()
            
            val info = wifiManager.connectionInfo ?: return emptyMap()
            if (info.networkId == -1 || info.bssid == null || info.bssid == "00:00:00:00:00:00") return emptyMap()
            
            val ssid = if (info.ssid == WifiManager.UNKNOWN_SSID) "Unknown" else info.ssid.replace("\"", "")
            
            val security = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSecurityTypeModern(info)
            } else {
                getSecurityTypeLegacy(context, info.bssid)
            }

            return mapOf(
                "SSID" to ssid,
                "BSSID" to (info.bssid ?: "N/A"),
                "Signal Strength" to "${info.rssi} dBm",
                "Link Speed" to "${info.linkSpeed} Mbps",
                "Frequency" to "${info.frequency} MHz",
                "Security" to security
            )
        } catch (e: Exception) {
            return emptyMap()
        }
    }

    @SuppressLint("MissingPermission")
    fun getCellularDetails(context: Context): Map<String, String> {
        try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val operatorName = tm.networkOperatorName ?: "Unknown"
            val networkType = getMobileNetworkType(tm)
            
            var dbm = -100
            val cellInfoList = tm.allCellInfo
            if (cellInfoList != null) {
                for (info in cellInfoList) {
                    if (info.isRegistered) {
                        @Suppress("DEPRECATION")
                        when (info) {
                            is CellInfoLte -> dbm = info.cellSignalStrength.dbm
                            is CellInfoGsm -> dbm = info.cellSignalStrength.dbm
                            is CellInfoWcdma -> dbm = info.cellSignalStrength.dbm
                            is CellInfoCdma -> dbm = info.cellSignalStrength.dbm
                            else -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && info is CellInfoNr) {
                                    dbm = info.cellSignalStrength.dbm
                                }
                            }
                        }
                        if (dbm != 0 && dbm != Int.MAX_VALUE && dbm != -1) break
                    }
                }
            }

            val dataState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try { if (tm.isDataEnabled) "Enabled" else "Disabled" } catch (e: Exception) { "Unknown" }
            } else "Unknown"

            return mapOf(
                "Operator" to operatorName,
                "Network Type" to networkType,
                "Data State" to dataState,
                "Signal Strength" to if (dbm <= -120 || dbm >= 0) "N/A" else "$dbm dBm"
            )
        } catch (e: Exception) {
            return emptyMap()
        }
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun getMobileNetworkType(tm: TelephonyManager): String {
        try {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) tm.dataNetworkType else tm.networkType
            return when (type) {
                TelephonyManager.NETWORK_TYPE_GPRS, TelephonyManager.NETWORK_TYPE_EDGE, TelephonyManager.NETWORK_TYPE_CDMA, 
                TelephonyManager.NETWORK_TYPE_1xRTT, TelephonyManager.NETWORK_TYPE_IDEN -> "2G"
                
                TelephonyManager.NETWORK_TYPE_UMTS, TelephonyManager.NETWORK_TYPE_HSDPA, TelephonyManager.NETWORK_TYPE_HSUPA, 
                TelephonyManager.NETWORK_TYPE_HSPA, TelephonyManager.NETWORK_TYPE_EVDO_0, TelephonyManager.NETWORK_TYPE_EVDO_A, 
                TelephonyManager.NETWORK_TYPE_EVDO_B, TelephonyManager.NETWORK_TYPE_EHRPD, TelephonyManager.NETWORK_TYPE_HSPAP -> "3G"
                
                TelephonyManager.NETWORK_TYPE_LTE -> "4G"
                TelephonyManager.NETWORK_TYPE_NR -> "5G"
                else -> "Connected"
            }
        } catch (e: Exception) { return "Unknown" }
    }

    @Suppress("DEPRECATION")
    private fun getSecurityTypeModern(info: WifiInfo): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            when (info.currentSecurityType) {
                0 -> "Open"; 1 -> "WEP"; 2 -> "WPA2"; 4 -> "WPA3"; else -> "Protected"
            }
        } else "Protected"
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun getSecurityTypeLegacy(context: Context, bssid: String?): String {
        if (bssid == null) return "Unknown"
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifiManager.scanResults?.find { it.BSSID == bssid }?.let { result ->
                return when {
                    result.capabilities.contains("WPA3") -> "WPA3"
                    result.capabilities.contains("WPA2") -> "WPA2"
                    result.capabilities.contains("WPA") -> "WPA"
                    result.capabilities.contains("WEP") -> "WEP"
                    else -> "Open"
                }
            }
        } catch (e: Exception) {}
        return "Protected"
    }

    @Suppress("DEPRECATION")
    fun getGateway(context: Context): String {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val dhcp = wifiManager.dhcpInfo ?: return "0.0.0.0"
            return formatIp(dhcp.gateway)
        } catch (e: Exception) { return "0.0.0.0" }
    }

    fun getMacAddress(): String? {
        try {
            val all = NetworkInterface.getNetworkInterfaces() ?: return null
            for (nif in all) {
                if (!nif.name.equals("wlan0", ignoreCase = true)) continue
                val res = nif.hardwareAddress ?: return null
                val sb = StringBuilder()
                for (b in res) {
                    sb.append(String.format("%02X:", b))
                }
                if (sb.isNotEmpty()) {
                    sb.deleteCharAt(sb.length - 1)
                }
                return sb.toString()
            }
        } catch (e: Exception) {}
        return "02:00:00:00:00:00"
    }

    fun ipToLong(ip: String): Long {
        return try {
            val parts = ip.split(".")
            if (parts.size != 4) return 0
            (parts[0].toLong() shl 24) or (parts[1].toLong() shl 16) or (parts[2].toLong() shl 8) or parts[3].toLong()
        } catch (e: Exception) { 0 }
    }

    fun longToIp(ip: Long): String {
        return "${(ip shr 24) and 0xFF}.${(ip shr 16) and 0xFF}.${(ip shr 8) and 0xFF}.${ip and 0xFF}"
    }

    fun formatIp(ip: Int): String {
        return String.format(Locale.US, "%d.%d.%d.%d", ip and 0xff, ip shr 8 and 0xff, ip shr 16 and 0xff, ip shr 24 and 0xff)
    }

    suspend fun getPublicIp(): String = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.ipify.org")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            val scanner = Scanner(conn.inputStream)
            if (scanner.hasNext()) scanner.next() else "Unknown"
        } catch (e: Exception) { "Unknown" }
    }

    fun isRandomizedMac(mac: String): Boolean {
        if (mac.isBlank() || !mac.contains(":")) return false
        return try {
            val firstByteStr = mac.split(":")[0]
            val firstByte = firstByteStr.toInt(16)
            (firstByte and 0x02) != 0
        } catch (e: Exception) { false }
    }

    suspend fun getIspData(): Map<String, String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("http://ip-api.com/json/")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            val response = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            if (json.getString("status") == "success") {
                mapOf(
                    "isp" to json.optString("isp"),
                    "city" to json.optString("city"),
                    "country" to json.optString("country"),
                    "org" to json.optString("org")
                )
            } else emptyMap()
        } catch (e: Exception) { emptyMap() }
    }

    suspend fun getIspInfo(): String = withContext(Dispatchers.IO) {
        try {
            val url = URL("http://ip-api.com/json/")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            val response = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            if (json.getString("status") == "success") {
                "${json.optString("isp")} • ${json.optString("city")}, ${json.optString("countryCode")}"
            } else "Unknown ISP"
        } catch (e: Exception) { "Unknown ISP" }
    }

    suspend fun fetchVendorOnline(mac: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.macvendors.com/$mac")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 3000
            if (conn.responseCode == 200) {
                return@withContext conn.inputStream.bufferedReader().use { it.readText() }.trim()
            }
        } catch (e: Exception) {}
        null
    }

    fun getVendor(context: Context, mac: String): String {
        return VendorDatabase.getVendor(context, mac)
    }

    fun getVendorSync(mac: String): String {
        if (mac.isEmpty() || mac == "00:00:00:00:00:00") return "Resolving..."
        return VendorDatabase.getVendorSync(mac)
    }

    fun getFrequencyBand(freq: Int): String {
        return when {
            freq in 2412..2484 -> "2.4 GHz"
            freq in 5170..5825 -> "5 GHz"
            freq in 5945..7125 -> "6 GHz"
            else -> "Unknown"
        }
    }

    fun getWifiStandard(result: ScanResult): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return when (result.wifiStandard) {
                ScanResult.WIFI_STANDARD_11AX -> "Wi-Fi 6"
                ScanResult.WIFI_STANDARD_11AC -> "Wi-Fi 5"
                ScanResult.WIFI_STANDARD_11N -> "Wi-Fi 4"
                ScanResult.WIFI_STANDARD_LEGACY -> "Legacy"
                else -> "Unknown"
            }
        }
        return "Unknown"
    }

    fun getChannel(freq: Int): Int {
        return when {
            freq == 2484 -> 14
            freq in 2412..2472 -> (freq - 2412) / 5 + 1
            freq in 5170..5825 -> (freq - 5170) / 5 + 34
            freq in 5945..7105 -> (freq - 5945) / 5 + 1
            else -> -1
        }
    }

    fun estimateDistance(rssi: Int, freq: Int): Double {
        val exp = (27.55 - (20 * log10(freq.toDouble())) + abs(rssi)) / 20.0
        return 10.0.pow(exp)
    }

    fun getSignalQuality(rssi: Int): String {
        return when {
            rssi >= -50 -> "Excellent"
            rssi >= -60 -> "Good"
            rssi >= -70 -> "Fair"
            else -> "Poor"
        }
    }

    suspend fun httpGet(urlStr: String, timeout: Int = 1000): String = withContext(Dispatchers.IO) {
        try {
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = timeout
            conn.readTimeout = timeout
            conn.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) { "" }
    }
}
