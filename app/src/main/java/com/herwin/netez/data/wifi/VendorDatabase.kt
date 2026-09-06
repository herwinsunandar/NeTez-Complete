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
    private val pendingFetches = ConcurrentHashMap.newKeySet<String>()
    private var isInitialized = false
    
    // Using a simple property with a getter instead of 'by lazy' to ensure thread-safety
    // and provide better error handling for TLS initialization failures.
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
        if (isInitialized) return@withContext
        
        ouiMap["A0:E1:6D"] = VendorData("ZTE FiberHome", "Infrastructure", "Router")
        ouiMap["10:08:1D"] = VendorData("ZTE FiberHome", "Infrastructure", "Router")
        ouiMap["FC:C8:97"] = VendorData("ZTE FiberHome", "Infrastructure", "Router")
        ouiMap["D8:47:32"] = VendorData("ZTE", "Infrastructure", "Router")
        ouiMap["00:22:93"] = VendorData("ZTE", "Infrastructure", "Router")
        ouiMap["B4:A9:4F"] = VendorData("ZTE", "Infrastructure", "Router")
        ouiMap["BC:9B:68"] = VendorData("TP-Link", "Infrastructure", "Router")
        ouiMap["8C:FD:F0"] = VendorData("TP-Link", "Infrastructure", "Router")
        ouiMap["50:5B:1D"] = VendorData("Mercusys (TP-Link)", "Infrastructure", "Router")
        ouiMap["D8:0D:17"] = VendorData("TP-Link", "Infrastructure", "Router")
        ouiMap["E8:4E:06"] = VendorData("Huawei", "Infrastructure", "Router")
        ouiMap["00:1D:7E"] = VendorData("Huawei", "Infrastructure", "Router")
        ouiMap["24:DF:6A"] = VendorData("Huawei", "Infrastructure", "Router")
        ouiMap["CC:CC:81"] = VendorData("Huawei", "Infrastructure", "Router")
        ouiMap["A4:4B:D5"] = VendorData("Huawei", "Infrastructure", "Router")
        ouiMap["28:6F:B9"] = VendorData("Nokia Shanghai Bell", "Infrastructure", "Router")
        ouiMap["C4:CD:50"] = VendorData("Tenda", "Infrastructure", "Router")
        ouiMap["D0:14:71"] = VendorData("Tenda", "Infrastructure", "Router")
        ouiMap["00:9E:6B"] = VendorData("Xiaomi", "Mobile", "Phone")
        ouiMap["18:59:36"] = VendorData("Xiaomi", "Mobile", "Phone")
        ouiMap["F4:8E:92"] = VendorData("Xiaomi", "Mobile", "Phone")
        ouiMap["28:D2:44"] = VendorData("Xiaomi", "Mobile", "Phone")
        ouiMap["D0:C5:F3"] = VendorData("Apple", "Mobile", "Phone")
        ouiMap["00:03:93"] = VendorData("Apple", "Mobile", "Phone")
        ouiMap["20:34:FB"] = VendorData("Samsung", "Mobile", "Phone")
        ouiMap["00:00:F0"] = VendorData("Samsung", "Mobile", "Phone")

        loadFromAssets(context)
        loadFromCache(context)

        isInitialized = true
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

        ouiMap[prefix]?.let { return it }

        if (pendingFetches.add(prefix)) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = try {
                        AppDatabase.getDatabase(context)
                    } catch (e: Exception) {
                        null
                    } ?: return@launch
                    
                    val override = db.vendorOverrideDao().getOverride(prefix)
                    if (override != null) {
                        val vData = VendorData(override.customVendor, type = override.customType ?: "Unknown")
                        ouiMap[prefix] = vData
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
        val prefix = mac.uppercase().replace("-", ":").take(8)
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
}
