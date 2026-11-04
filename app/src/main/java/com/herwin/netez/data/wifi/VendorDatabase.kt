package com.herwin.netez.data.wifi

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.herwin.netez.data.AppDatabase
import com.herwin.netez.data.VendorOverrideEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class VendorData(
    val vendor: String,
    val category: String = "Unknown",
    val type: String = "Unknown"
)

object VendorDatabase {
    private val ouiMap = ConcurrentHashMap<String, VendorData>()
    private val overrideMap = ConcurrentHashMap<String, VendorData>()
    private val pendingFetches = ConcurrentHashMap.newKeySet<String>()
    private var isInitialized = false
    
    private var _client: OkHttpClient? = null
    
    private val _vendorUpdates = MutableSharedFlow<String>()
    val vendorUpdates = _vendorUpdates.asSharedFlow()

    private fun getClient(): OkHttpClient? {
        if (_client == null) {
            synchronized(this) {
                if (_client == null) {
                    _client = try {
                        OkHttpClient.Builder()
                            .connectTimeout(10, TimeUnit.SECONDS)
                            .readTimeout(20, TimeUnit.SECONDS)
                            .build()
                    } catch (e: Throwable) {
                        e.printStackTrace()
                        null
                    }
                }
            }
        }
        return _client
    }

    suspend fun load(context: Context) = withContext(Dispatchers.IO) {
        if (!isInitialized) {
            // Comprehensive OUI Map for popular vendors in ID/SEA
            // ZTE / FiberHome
            ouiMap["A0:E1:6D"] = VendorData("ZTE FiberHome", "Infrastructure", "Router")
            ouiMap["10:08:1D"] = VendorData("ZTE FiberHome", "Infrastructure", "Router")
            ouiMap["FC:C8:97"] = VendorData("ZTE FiberHome", "Infrastructure", "Router")
            ouiMap["D8:47:32"] = VendorData("ZTE", "Infrastructure", "Router")
            ouiMap["00:22:93"] = VendorData("ZTE", "Infrastructure", "Router")
            ouiMap["B4:A9:4F"] = VendorData("ZTE", "Infrastructure", "Router")
            ouiMap["34:4B:50"] = VendorData("ZTE", "Infrastructure", "Router")
            ouiMap["E0:18:77"] = VendorData("ZTE", "Infrastructure", "Router")
            ouiMap["2C:26:C5"] = VendorData("ZTE", "Infrastructure", "Router")
            
            // Huawei
            ouiMap["E8:4E:06"] = VendorData("Huawei", "Infrastructure", "Router")
            ouiMap["00:1D:7E"] = VendorData("Huawei", "Infrastructure", "Router")
            ouiMap["24:DF:6A"] = VendorData("Huawei", "Infrastructure", "Router")
            ouiMap["CC:CC:81"] = VendorData("Huawei", "Infrastructure", "Router")
            ouiMap["A4:4B:D5"] = VendorData("Huawei", "Infrastructure", "Router")
            ouiMap["BC:76:70"] = VendorData("Huawei", "Infrastructure", "Router")
            ouiMap["4C:B1:6C"] = VendorData("Huawei", "Infrastructure", "Router")
            ouiMap["28:5F:DB"] = VendorData("Huawei", "Infrastructure", "Router")
            ouiMap["AC:85:3D"] = VendorData("Huawei", "Infrastructure", "Router")

            // TP-Link / Mercusys
            ouiMap["BC:9B:68"] = VendorData("TP-Link", "Infrastructure", "Router")
            ouiMap["8C:FD:F0"] = VendorData("TP-Link", "Infrastructure", "Router")
            ouiMap["50:5B:1D"] = VendorData("Mercusys (TP-Link)", "Infrastructure", "Router")
            ouiMap["D8:0D:17"] = VendorData("TP-Link", "Infrastructure", "Router")
            ouiMap["60:E3:27"] = VendorData("TP-Link", "Infrastructure", "Router")
            ouiMap["00:0A:EB"] = VendorData("TP-Link", "Infrastructure", "Router")
            ouiMap["98:DE:D0"] = VendorData("TP-Link", "Infrastructure", "Router")

            // D-Link
            ouiMap["00:0F:3D"] = VendorData("D-Link", "Infrastructure", "Router")
            ouiMap["00:12:17"] = VendorData("D-Link", "Infrastructure", "Router")
            ouiMap["1C:7E:E5"] = VendorData("D-Link", "Infrastructure", "Router")

            // ASUS / Tenda
            ouiMap["C4:CD:50"] = VendorData("Tenda", "Infrastructure", "Router")
            ouiMap["D0:14:71"] = VendorData("Tenda", "Infrastructure", "Router")
            ouiMap["AC:92:32"] = VendorData("ASUS", "Infrastructure", "Router")
            ouiMap["BC:EE:7B"] = VendorData("ASUS", "Infrastructure", "Router")

            // Apple
            ouiMap["D0:C5:F3"] = VendorData("Apple", "Mobile", "Phone")
            ouiMap["00:03:93"] = VendorData("Apple", "Mobile", "Phone")
            ouiMap["28:CF:E9"] = VendorData("Apple", "Mobile", "Phone")
            ouiMap["F0:99:B6"] = VendorData("Apple", "Mobile", "Phone")
            ouiMap["F4:0F:24"] = VendorData("Apple", "Mobile", "Phone")

            // Samsung
            ouiMap["20:34:FB"] = VendorData("Samsung", "Mobile", "Phone")
            ouiMap["00:00:F0"] = VendorData("Samsung", "Mobile", "Phone")
            ouiMap["AC:5A:14"] = VendorData("Samsung", "Mobile", "Phone")
            ouiMap["24:FC:E5"] = VendorData("Samsung", "Mobile", "Phone")

            // Xiaomi
            ouiMap["00:9E:6B"] = VendorData("Xiaomi", "Mobile", "Phone")
            ouiMap["18:59:36"] = VendorData("Xiaomi", "Mobile", "Phone")
            ouiMap["F4:8E:92"] = VendorData("Xiaomi", "Mobile", "Phone")
            ouiMap["28:D2:44"] = VendorData("Xiaomi", "Mobile", "Phone")
            ouiMap["64:90:C1"] = VendorData("Xiaomi", "Mobile", "Phone")
            ouiMap["34:2E:B7"] = VendorData("Xiaomi", "Mobile", "Phone")

            // Google / Nest
            ouiMap["00:1A:11"] = VendorData("Google", "Multimedia", "Cast Device")
            ouiMap["3C:5C:A1"] = VendorData("Google", "Multimedia", "Cast Device")
            ouiMap["D8:EB:97"] = VendorData("Google", "Multimedia", "Cast Device")

            // Amazon
            ouiMap["00:BB:3A"] = VendorData("Amazon", "Smart Device", "Echo")
            ouiMap["F0:27:2D"] = VendorData("Amazon", "Smart Device", "Kindle")

            // Espressif (IoT)
            ouiMap["24:0A:C4"] = VendorData("Espressif", "IoT", "Smart Device")
            ouiMap["30:AE:A4"] = VendorData("Espressif", "IoT", "Smart Device")
            ouiMap["C8:2B:96"] = VendorData("Espressif", "IoT", "Smart Device")

            loadFromAssets(context)
            loadFromCache(context)
            isInitialized = true
        }
        loadOverrides(context)
    }

    private fun loadOverrides(context: Context) {
        try {
            val db = AppDatabase.getDatabase(context)
            runBlocking(Dispatchers.IO) {
                val overrides = db.vendorOverrideDao().getAllOverrides()
                overrides.forEach { override ->
                    val key = override.mac.uppercase().replace("-", ":")
                    val vData = VendorData(override.customVendor, type = override.customType ?: "Unknown")
                    overrideMap[key] = vData
                    if (key.length >= 8) {
                        overrideMap[key.take(8)] = vData
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setCustomOverride(mac: String, vendor: String) {
        val cleanMac = mac.uppercase().replace("-", ":")
        val vData = VendorData(vendor)
        overrideMap[cleanMac] = vData
        if (cleanMac.length >= 8) {
            overrideMap[cleanMac.take(8)] = vData
        }
        _vendorUpdates.tryEmit(cleanMac)
    }

    private fun loadFromAssets(context: Context) {
        try {
            val assets = context.assets.list("") ?: emptyArray()
            
            if (assets.contains("manuf.txt")) {
                context.assets.open("manuf.txt").bufferedReader().use { reader ->
                    reader.forEachLine { line ->
                        if (line.isNotBlank() && !line.startsWith("#")) {
                            parseLine(line)
                        }
                    }
                }
            }

            val ouiFile = if (assets.contains("oui.txt")) "oui.txt" else "oui_mini.txt"
            context.assets.open(ouiFile).bufferedReader().use { reader ->
                reader.forEachLine { line ->
                    if (line.isNotBlank() && !line.startsWith("#")) {
                        parseLine(line)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadFromCache(context: Context) {
        val cacheFile = File(context.filesDir, "oui_cached.txt")
        if (cacheFile.exists()) {
            try {
                cacheFile.bufferedReader().use { reader ->
                    reader.forEachLine { line ->
                        if (line.isNotBlank()) parseLine(line)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun parseLine(line: String) {
        try {
            if (line.contains("|")) {
                val parts = line.split("|")
                if (parts.size >= 2) {
                    val prefix = parts[0].uppercase().trim().replace("-", ":").take(8)
                    val vendor = shortenVendorName(parts[1].trim())
                    val category = if (parts.size >= 3) parts[2].trim() else "Unknown"
                    val type = if (parts.size >= 4) parts[3].trim() else "Unknown"
                    ouiMap[prefix] = VendorData(vendor, category, type)
                }
            } else if (line.contains("(hex)")) {
                val parts = line.split("(hex)")
                if (parts.size >= 2) {
                    val prefix = parts[0].trim().replace("-", ":").uppercase()
                    val vendor = shortenVendorName(parts[1].trim())
                    if (!ouiMap.containsKey(prefix)) {
                        ouiMap[prefix] = VendorData(vendor)
                    }
                }
            } else {
                val parts = line.split(Regex("\\s+"), 2)
                if (parts.size >= 2) {
                    val prefix = parts[0].uppercase().trim().replace("-", ":").take(8)
                    val vendor = shortenVendorName(parts[1].trim())
                    if (!ouiMap.containsKey(prefix)) {
                        ouiMap[prefix] = VendorData(vendor)
                    }
                }
            }
        } catch (e: Exception) {}
    }

    fun shortenVendorName(name: String): String {
        var shortened = name.trim().split("#").first().trim()
        val suffixes = listOf("Co., Ltd.", "Co., Ltd", "Ltd.", "Ltd", "Inc.", "Inc", "Corporation", "Corp.", "Corp", "S.A.", "S.A")
        for (suffix in suffixes) {
            if (shortened.endsWith(suffix, ignoreCase = true)) {
                shortened = shortened.substring(0, shortened.length - suffix.length).trim().removeSuffix(",")
            }
        }
        return shortened.trim().removeSuffix(",")
    }

    fun getVendorData(context: Context, mac: String?): VendorData {
        if (mac == null || mac.length < 8) return VendorData("Unknown")
        val fullMac = mac.uppercase().replace("-", ":")
        val prefix = fullMac.take(8)

        // 1. Check Full MAC in custom overrides
        overrideMap[fullMac]?.let { return it }
        // 2. Check Prefix MAC in custom overrides
        overrideMap[prefix]?.let { return it }
        // 3. Check OUI map in memory
        ouiMap[prefix]?.let { return it }

        if (pendingFetches.add(prefix)) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = try {
                        AppDatabase.getDatabase(context)
                    } catch (e: Exception) {
                        null
                    } ?: return@launch
                    
                    val override = db.vendorOverrideDao().getOverrideByMacOrPrefix(fullMac, prefix)
                    if (override != null) {
                        val vData = VendorData(override.customVendor, type = override.customType ?: "Unknown")
                        overrideMap[fullMac] = vData
                        overrideMap[prefix] = vData
                        _vendorUpdates.emit(fullMac)
                        return@launch
                    }

                    if (isWiFiConnected(context)) {
                        fetchVendorOnline(context, fullMac)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingFetches.remove(prefix)
                }
            }
        }

        return VendorData("Unknown")
    }

    fun getVendor(context: Context, mac: String?): String {
        return getVendorData(context, mac).vendor
    }

    private fun isWiFiConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) && 
               caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private suspend fun fetchVendorOnline(context: Context, mac: String) {
        val httpClient = getClient() ?: return
        val prefix = mac.take(8)
        val apis = listOf(
            "https://api.macvendors.com/$prefix",
            "https://api.maclookup.app/v2/macs/$prefix"
        )

        for (apiUrl in apis) {
            try {
                val request = Request.Builder().url(apiUrl).build()
                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val rawBody = response.body?.string()?.trim() ?: return@use
                        
                        var vendorName = ""
                        if (rawBody.startsWith("{")) {
                            try {
                                val json = JSONObject(rawBody)
                                if (json.optBoolean("success", false)) {
                                    vendorName = json.optString("company", "")
                                    if (vendorName.isEmpty()) {
                                        vendorName = json.optJSONObject("mac_info")?.optString("company", "") ?: ""
                                    }
                                }
                            } catch (e: Exception) {}
                        } else {
                            vendorName = rawBody
                        }

                        if (vendorName.isNotEmpty() && !vendorName.contains("Not Found", true)) {
                            val shortName = shortenVendorName(vendorName)
                            
                            try {
                                val db = AppDatabase.getDatabase(context)
                                db.vendorOverrideDao().insertOverride(
                                    VendorOverrideEntity(
                                        mac = prefix,
                                        customVendor = shortName,
                                        source = "CLOUD"
                                    )
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            
                            val vData = VendorData(shortName)
                            ouiMap[prefix] = vData
                            
                            _vendorUpdates.emit(mac)
                            return
                        }
                    }
                }
            } catch (e: Exception) {
            }
        }
    }

    fun getVendorDataSync(mac: String?): VendorData {
        if (mac == null || mac.length < 8) return VendorData("Unknown")
        val fullMac = mac.uppercase().replace("-", ":")
        val prefix = fullMac.take(8)

        overrideMap[fullMac]?.let { return it }
        overrideMap[prefix]?.let { return it }
        return ouiMap[prefix] ?: VendorData("Unknown")
    }

    fun getVendorSync(mac: String?): String {
        return getVendorDataSync(mac).vendor
    }

    fun isRandomized(mac: String?): Boolean {
        if (mac == null || mac.length < 2) return false
        return try {
            val firstByteStr = mac.substring(0, 2)
            val firstByte = firstByteStr.toInt(16)
            (firstByte and 0x02) != 0
        } catch (e: Exception) {
            false
        }
    }

    fun reloadCache(context: Context) {
        loadFromCache(context)
        loadOverrides(context)
    }

    fun getLastUpdateTimestamp(context: Context): Long {
        val prefs = context.getSharedPreferences("netez_prefs", Context.MODE_PRIVATE)
        return prefs.getLong("oui_last_update", 0L)
    }

    fun getCacheRecordCount(context: Context): Int {
        val prefs = context.getSharedPreferences("netez_prefs", Context.MODE_PRIVATE)
        return prefs.getInt("oui_record_count", 0)
    }
}
