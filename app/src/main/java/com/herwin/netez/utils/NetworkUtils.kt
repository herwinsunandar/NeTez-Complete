package com.herwin.netez.utils

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URL
import java.util.Locale
import java.util.Scanner
import kotlin.math.pow

object NetworkUtils {

    private var ouiMap: MutableMap<String, String>? = null

    fun loadOui(context: Context) {
        if (ouiMap != null) return
        ouiMap = mutableMapOf()
        try {
            context.assets.open("oui.txt").bufferedReader().use { reader ->
                reader.forEachLine { line ->
                    if (line.contains("(base 16)")) {
                        val parts = line.split("(base 16)")
                        if (parts.size == 2) {
                            val prefix = parts[0].trim().replace("-", "").uppercase()
                            val vendor = parts[1].trim()
                            ouiMap?.put(prefix, vendor)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // File not found or error reading
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
            
            val ssid = if (info.ssid == WifiManager.UNKNOWN_SSID) "Unknown (Check Location/Perms)" else info.ssid.replace("\"", "")
            
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
                try {
                    if (tm.isDataEnabled) "Enabled" else "Disabled"
                } catch (e: Exception) {
                    "Unknown"
                }
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
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                tm.dataNetworkType
            } else {
                tm.networkType
            }
            return when (type) {
                TelephonyManager.NETWORK_TYPE_GPRS, TelephonyManager.NETWORK_TYPE_EDGE, TelephonyManager.NETWORK_TYPE_CDMA, 
                TelephonyManager.NETWORK_TYPE_1xRTT, TelephonyManager.NETWORK_TYPE_IDEN -> "2G"
                
                TelephonyManager.NETWORK_TYPE_UMTS, TelephonyManager.NETWORK_TYPE_HSDPA, TelephonyManager.NETWORK_TYPE_HSUPA, 
                TelephonyManager.NETWORK_TYPE_HSPA, TelephonyManager.NETWORK_TYPE_EVDO_0, TelephonyManager.NETWORK_TYPE_EVDO_A, 
                TelephonyManager.NETWORK_TYPE_EVDO_B, TelephonyManager.NETWORK_TYPE_EHRPD, TelephonyManager.NETWORK_TYPE_HSPAP -> "3G"
                
                TelephonyManager.NETWORK_TYPE_LTE -> "4G"
                20 -> "5G" // NETWORK_TYPE_NR
                else -> "Connected"
            }
        } catch (e: Exception) {
            return "Unknown"
        }
    }

    @Suppress("DEPRECATION")
    private fun getSecurityTypeModern(info: WifiInfo): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                when (info.currentSecurityType) {
                    0 -> "Open"
                    1 -> "WEP"
                    2 -> "WPA2"
                    4 -> "WPA3"
                    else -> "Protected"
                }
            } catch (e: Exception) { "Protected" }
        } else "Protected"
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun getSecurityTypeLegacy(context: Context, bssid: String?): String {
        if (bssid == null || bssid == "00:00:00:00:00:00") return "Unknown"
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val scanResults = wifiManager.scanResults
            if (scanResults != null) {
                for (result in scanResults) {
                    if (result.BSSID == bssid) {
                        val capabilities = result.capabilities
                        return when {
                            capabilities.contains("WPA3") -> "WPA3"
                            capabilities.contains("WPA2") -> "WPA2"
                            capabilities.contains("WPA") -> "WPA"
                            capabilities.contains("WEP") -> "WEP"
                            else -> "Open"
                        }
                    }
                }
            }
        } catch (e: Exception) {
            return "Protected"
        }
        return "Protected"
    }

    @Suppress("DEPRECATION")
    fun getGateway(context: Context): String {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            if (!wifiManager.isWifiEnabled) return "0.0.0.0"
            val dhcp = wifiManager.dhcpInfo ?: return "0.0.0.0"
            return formatIp(dhcp.gateway)
        } catch (e: Exception) {
            return "0.0.0.0"
        }
    }

    fun ipToLong(ip: String): Long {
        return try {
            val parts = ip.split(".")
            if (parts.size != 4) return 0
            (parts[0].toLong() shl 24) or (parts[1].toLong() shl 16) or (parts[2].toLong() shl 8) or parts[3].toLong()
        } catch (e: Exception) { 0 }
    }

    private fun formatIp(ip: Int): String {
        return String.format(
            Locale.US,
            "%d.%d.%d.%d",
            ip and 0xff,
            ip shr 8 and 0xff,
            ip shr 16 and 0xff,
            ip shr 24 and 0xff
        )
    }

    suspend fun getPublicIp(): String = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.ipify.org")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            val scanner = Scanner(conn.inputStream)
            if (scanner.hasNext()) scanner.next() else "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    suspend fun getIspInfo(): String = withContext(Dispatchers.IO) {
        try {
            val url = URL("http://ip-api.com/json/")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            val scanner = Scanner(conn.inputStream)
            val response = StringBuilder()
            while (scanner.hasNextLine()) {
                response.append(scanner.nextLine())
            }
            val json = org.json.JSONObject(response.toString())
            if (json.getString("status") == "success") {
                json.optString("isp", "Unknown ISP")
            } else {
                "Unknown ISP"
            }
        } catch (e: Exception) {
            "Unknown ISP"
        }
    }

    fun getVendor(mac: String): String {
        if (mac.isEmpty() || mac == "00:00:00:00:00:00") return "Resolving..."
        val prefix = mac.uppercase().replace(":", "").replace(" ", "").take(6)
        
        // Try dynamic map first
        ouiMap?.get(prefix)?.let { return it }
        
        return when (prefix) {
            // Apple OUIs
            "000393", "000502", "000A27", "000A95", "0010FA", "001124", "001451", "0016CB", "0017F2", "0019E3", "001B63", "001C42", "001CB3", "001D4F", "001E52", "001F5B", "001FF3", "0021E9", "002241", "002312", "002332", "00236C", "002436", "002500", "00254B", "0025BC", "002608", "00264A", "0026B0", "0026BB", "101C0C", "1040F3", "1044A1", "1093E9", "109ADD", "10ADE1", "14109F", "14205E", "145A05", "148FC6", "1499E2", "182032", "183451", "186590", "18AF61", "18E7F4", "1C1AC0", "1C5CF2", "1CAA07", "1CABA7", "1CB094", "1CE62B", "20C9D0", "24240E", "245BA7", "24A074", "24AB81", "24E314", "283737", "285AEB", "28CFDA", "28E347", "2C1F23", "2C200B", "2CBE08", "3035AD", "34159E", "34363B", "34C059", "38484C", "3871BF", "38C986", "38CA84", "3C0754", "3C15C2", "3C71BF", "3C7AD1", "3CAB8E", "3CD0F8", "3CE072", "403004", "40331A", "403C58", "404D7F", "406C8F", "4098AD", "40A6D9", "40B395", "444C0C", "444E1A", "44D832", "44FB42", "483B38", "48437C", "484BAA", "4860BC", "48746E", "48A195", "48D705", "4C3275", "4C57CA", "4C74BF", "4C8D79", "502E5C", "542696", "5433CB", "54724F", "54992F", "54AE27", "54E43A", "581FAA", "58403E", "5855CA", "58E6BA", "5C8D4E", "5C95AE", "5C9656", "5CF7E6", "5CF938", "600308", "60334B", "606944", "609217", "60A366", "60C547", "60F81D", "64200C", "6476BA", "649ABE", "64B9E8", "64C753", "64E682", "680927", "685B35", "68A86D", "68AE20", "68D93C", "68EF43", "6C3E6D", "6C4008", "6C709F", "6C8814", "6C94F8", "6CAAB3", "701124", "703EAC", "705681", "7073CB", "7081EB", "70A2B3", "70CD60", "70DEE2", "70E72C", "741BB2", "748114", "74E1B6", "7831C1", "784F43", "787B8A", "78886D", "789F70", "78A3E4", "78CA39", "7C04D0", "7C11BE", "7C5049", "7C6D62", "7CC3A1", "7CD1C3", "7CFA70", "804971", "808223", "80929F", "80B03D", "80E650", "842999", "843835", "84788B", "8489AD", "84FCAC", "881908", "8863DF", "886440", "8866A5", "88C663", "88CB87", "8C2937", "8C5877", "8C7B9D", "8C8590", "8C8EF2", "9027E4", "907240", "90840D", "90B0ED", "90B21F", "90FD61", "94103E", "9476B7", "949426", "94E979", "9801A7", "9810E8", "985AEB", "98D6BB", "98E0D9", "98FE94", "9C04EB", "9C207B", "9C35EB", "9C4FDA", "9C8B7E", "A01828", "A03BE3", "A04EA7", "A0999B", "A43135", "A45E60", "A46706", "A4B197", "A4C361", "A4D18C", "A82066", "A85B78", "A8667F", "A88808", "A8BBCF", "AC1F74", "AC293A", "AC3C0B", "AC7F3E", "AC87A3", "ACCF85", "B03495", "B0481A", "B065BD", "B0702D", "B418D1", "B48B19", "B4F0AB", "B8098A", "B817C2", "B844D9", "B853AC", "B8634D", "B8782E", "B88D12", "B8C75D", "B8E856", "B8F6B1", "BC3BAF", "BC52B7", "BC671C", "BC926B", "BCA920", "BCFED9", "C01ADA", "C0847A", "C0CCF8", "C42C03", "C461CD", "C47D4F", "C48466", "C49880", "C4B301", "C82A14", "C8334B", "C86F1D", "C8BCC8", "C8D019", "C8E7D8", "CC08E0", "CC20E8", "CC25EF", "CC2DB7", "CC785F", "D0034B", "D023DB", "D02598", "D03311", "D04F7E", "D0817A", "D0A637", "D0C5F3", "D0D2B0", "D43A35", "D4619D", "D4909C", "D49A20", "D4F46F", "D8004D", "D81D72", "D83062", "D88F76", "D89695", "D89E3F", "D8BB2C", "D8CF9C", "DC0C5C", "DC2B2A", "DC2B61", "DC3714", "DC415F", "DCA4CA", "E0338E", "E06678", "E0B52D", "E0C767", "E0F5C6", "E0F847", "E425E7", "E48B7F", "E4CE14", "E4E4AB", "E8040B", "E80688", "E88D28", "E8B227", "EC3586", "F01898", "F02475", "F0761C", "F099BF", "F0B479", "F0DBE2", "F0DCE2", "F40616", "F40F24", "F41BA1", "F431C3", "F437B7", "F45C89", "F4F15A", "F4F26D", "F4F5D8", "F81EDF", "F82793", "F83880", "F86214", "F86FCA", "F88FCA", "F8E079", "FC183C", "FC2181", "FCD848", "FCFBFB" -> "Apple"
            
            // Huawei OUIs
            "001D7E", "00247B", "00259E", "0025D4", "0026C1", "0027D0", "04F938", "0819A6", "086361", "0C37DC", "101B54", "104780", "143C60", "1C1D67", "200BC7", "240995", "244427", "283152", "283166", "285FDB", "286ED4", "308730", "3400A3", "342EBF", "346BD3", "3897D3", "3C15C2", "3C9C0F", "401266", "404D8E", "40CB76", "441EA1", "446747", "44821A", "4857DD", "48FD8E", "4C1FA1", "4C8B30", "501395", "504B62", "5439DF", "548998", "54B121", "5866BA", "5C4CA9", "5C7D3E", "5CB395", "601466", "6035C0", "60DE44", "6416F0", "643E8C", "681AB2", "688F84", "68A03E", "6C112E", "6C71D9", "7054F5", "705A0F", "70723C", "7079B3", "707BE8", "7488BB", "781DBA", "786256", "78D793", "7C6097", "801382", "8038BC", "80717A", "80D09B", "84183A", "8446FE", "845BA3", "84DBAC", "88108F", "8853D4", "888603", "88E3C5", "8C15C7", "8C34FD", "9017AC", "904E91", "90671C", "94049C", "94103E", "94772B", "981F11", "9C061B", "A416F0", "A4933F", "A49947", "A4BA76", "AC4E91", "AC853D", "B05B67", "B40CEF", "B41513", "B43052", "B4A330", "B83A08", "B89436", "BC25E0", "BC7670", "C00A2C", "C40528", "C49F4C", "C81451", "C88D83", "CC96A0", "D02D1D", "D440F0", "D46AA8", "D49E1E", "DC2A7E", "DC3B78", "E0247F", "E0553D", "E41263", "E435C8", "E468A3", "E8088B", "E8CD2D", "EC388F", "F0388E", "F43E07", "F48C50", "F4911E", "F80113", "F83DFF", "F84A73", "F898B9", "F8BC12", "FC48EF", "00E0FC", "001882", "24DF6A", "38378B", "5451D8", "600810", "78F5FD", "A8AB3A", "D07AB5", "E0CA94" -> "Huawei"

            // Xiaomi OUIs
            "009E6B", "185936", "286C07", "3480B3", "50642B", "640980", "742344", "8CBEBE", "98FAAB", "ACF7F3", "C46A2C", "E446DA", "F0B429" -> "Xiaomi"

            // OPPO OUIs
            "149D09", "24D13F", "64D4DA", "98D502", "A03BF3", "ACD8A0", "B0C559", "D4A528" -> "OPPO"

            // Vivo OUIs
            "18FE34", "5422F8", "70AF6A", "8035C1", "902107", "ACD618", "B40B44" -> "Vivo"

            // Samsung OUIs
            "0000F0", "00016C", "000219", "00023A", "0007AB", "0007D1", "000918", "000D3A", "000DBF", "00105C", "001096", "001247", "0012FB", "001370", "001435", "001599", "0015B9", "00166B", "00166C", "0017C9", "0017D4", "0018AF", "001901", "001A8A", "001B98", "001C62", "001D25", "001E7D", "001F6B", "002119", "0021D2", "002331", "0023D7", "002454", "002490", "0024E3", "002567", "00267E", "0026E2", "00273D", "003099", "00D059", "00E064", "04180F", "0452F3", "04B167", "04C5A4", "04F128", "0808C2", "08373D", "08D42B", "08EBED", "08FC88", "0C1420", "0C715D", "0C9160", "0C9741", "0CAFF7", "101DC0", "1027BE", "103047", "10683F", "1077B0", "1077B1", "10AE60", "10BF48", "10D07A", "10F96F", "1432D1", "1441E6", "144AFE", "1489FD", "14BB6E", "14F42A", "1816C9", "182195", "18227E", "18473D", "187A93", "1883BF", "18B430", "18BB26", "1C12B0", "1C5A3E", "1C62B8", "1C66AA", "1CB72C", "2013E0", "2047DA", "20D5BF", "243154", "244B03", "247189", "24AB81", "24C696", "24E314", "28A02B", "28CC01", "2C4401", "2C5A05", "2CAB00", "2CBE08", "30074D", "300D43", "301966", "305A3A", "307512", "3085A9", "3096FB", "30A9DE", "30CBF8", "3423BA", "34466F", "3481C4", "34AA99", "34D712", "380195", "382DD1", "3852F8", "3895F8", "38AA3C", "38D2CA", "38E08E", "3C5AB4", "3C8BCD", "3CBDC5", "400E85", "40167E", "402C14", "405FBE", "40831D", "40B0FA", "40B837", "40CC52", "444E1A", "4450B1", "44650D", "448500", "44B32D", "44E137", "44F459", "484487", "485A3F", "48ABAA", "48C796", "48D343", "4C0BBE", "4C3C16", "4C6641", "4C7C5F", "501479", "505663", "508569", "509772", "50CB60", "50DCE7", "543530", "544408", "5462A2", "54880E", "5492BE", "54B7E5", "54C630", "54FA3E", "581215", "584822", "586B14", "58A2B5", "58C5CB", "58E28F", "5C0A5B", "5C17D3", "5C313E", "5C5015", "5CA39D", "5CAA22", "5CE8EB", "602102", "606BBD", "60AF6D", "60D9C7", "642737", "644BD0", "646E6C", "647704", "64B4BD", "64C2DE", "64D4BD", "6805CA", "681CA2", "68764F", "68C44D", "6C23B9", "6C72E7", "6C8336", "6CAF05", "700514", "7065ED", "70700D", "70AF24", "74458A", "745F00", "74A528", "74B587", "74B9AB", "74D02B", "781DBA", "78238C", "7840E4", "78471D", "78595E", "786BB9", "78ABBB", "78B3B9", "78D6F0", "7C0191", "7C3E9D", "7C669D", "7CA1AE", "7CAB60", "7CBB8A", "7CD566", "7CFFC3", "801964", "805A04", "80C3B7", "80D21D", "80EA96", "840B2D", "84253F", "843838", "845181", "8455A5", "847412", "848E0C", "84A466", "84C0EF", "88308A", "88329B", "88E603", "8C18D4", "8C3AE3", "8C71F8", "8CC8CD", "90187C", "90F1AA", "94350A", "9463D1", "948BC1", "94E1AC", "9800C5", "981DFA", "983B8F", "984B4A", "98D6F7", "98E7F5", "9C0298", "9C2A70", "9CE635", "A02195", "A0821F", "A09169", "A0B4A5", "A4307A", "A470D6", "A47733", "A48D3B", "A4E4B8", "A80600", "A87C01", "A89FBA", "AC5A14", "ACCF85", "B0144D", "B0C554", "B40B44", "B4527E", "B46293", "B479A7", "B85A73", "B8C68E", "BC14EF", "BC4486", "BC72B1", "BC8556", "BCD177", "C0143D", "C0414F", "C43231", "C43A35", "C4731E", "C81479", "C87E75", "CC07AB", "CCB11A", "D00401", "D0176A", "D0B33F", "D0E782", "D43D7E", "D487D8", "D4E8B2", "D8E0E1", "DC0C5C", "DC7144", "E09971", "E41218", "E4B021", "E8039A", "E8508B", "E8B4F8", "ECE09B", "F02765", "F05A09", "F0E77E", "F409D8", "F4D488", "F8042E", "F84602", "FCA13E" -> "Samsung"

            // Generic Networking
            "00212F", "00259C", "00000C", "000142", "000164", "000197", "000216", "00024A", "0002B9", "000331", "00036B", "000427", "00049A", "00055F", "0005DC", "000628", "0006C1", "000750", "0007B3", "000820", "00087D", "000943", "0009B7", "000A41", "000AB7", "000B46", "000BBE", "000C30", "000CBF", "000D28", "000DBC", "000E83", "000F23", "000FCB", "1005CA", "108CCF", "2401C7", "247E12", "3037A6", "380E4D", "4403A7", "500604", "544A00", "5835D9", "6400F1", "70105C", "74258A", "780CB8", "8056F2", "843497", "881D8A", "9017AC", "981888", "A01438", "A41875", "AC17C8", "B000B4", "B41489", "B83861", "BC1665", "C0255C", "C40A38", "C80084", "CC167E", "D0574C", "D42C44", "D824BD", "DC774C", "E02F6D", "E4115B", "E80462", "EC3091", "F02572", "F40F1B", "F80BBE", "FC58FA" -> "Cisco"
            "001349", "0019CB", "001A2B", "085A11", "107BEF", "4C9EFF", "5067F0", "BC94E3", "E03F49", "F42853" -> "Zyxel"
            "00E04C", "C83A35", "BC5FF4", "D83214", "502AF8", "A0601B", "14CF92", "50D4F7", "C8EE19", "E06290" -> "Tenda/Mercusys"
            "080027" -> "VirtualBox"
            "28D244", "50D2F7", "7085C2", "8416F9", "001D0F", "001F33", "002127", "002369" -> "TP-Link"
            "60A44C", "000C29", "005056" -> "ASUSTek/VMware"
            "B827EB", "DCA632", "E45F01" -> "Raspberry Pi"
            "DC4A3E", "F492BF", "00156D", "002722", "0418D6", "687251", "788A20", "802AA8" -> "Ubiquiti"
            "E43883", "0019C6", "002293", "002512", "00258B", "0026ED", "324074", "304074" -> "ZTE"
            "A42BB0", "6C3B6B", "D4CA6D", "2C91AB", "488AD2", "488F5A", "000C42" -> "MikroTik"
            "C03E50", "00E04C", "000A3A", "000F13" -> "Realtek"
            "0003FF", "000D3A", "00125A", "00155D", "0017FA", "001DD8", "002248", "0025AE", "0050F2", "281878", "6045BD", "70B3D5" -> "Microsoft"
            "00045A", "000625", "000C41", "000F66", "0014BF", "001839", "002129", "00226B", "00236A", "00259C" -> "Linksys"
            "00095B", "000C41", "000FB5", "00146C", "00184D", "001B2F", "001E2A", "00223F", "0024B2", "0026F2", "204E7F", "20B001", "28C68E", "30469A", "4494FC", "506A03", "78D294", "841B5E", "8C3BAD", "9CD36D", "A00460", "A021B7", "A040A0", "A06391", "B0B98A", "C03F0E", "C43DC7", "E0FFF7" -> "Netgear"
            "000B86", "000CE7", "001A1E", "00246C" -> "Aruba"
            "001392", "001D2E", "00227F", "002482", "044BED", "24C9A1", "543D37", "748E75", "74911A", "8C0C90", "9CB377", "ACD428", "C89F1A", "D4C19E" -> "Ruckus Wireless"
            "00D0F8", "58696C", "14144B", "B8107B", "D0C1F2", "800588", "946650", "B8251B", "BC4C6D", "C4B9CD" -> "Ruijie Networks"
            "001018", "00107B", "001BA9", "001BB1", "18AF61", "1C3E84", "2CCF58", "3C2DB7", "40B0FA", "50CB60", "74A2E6", "7CD1C3", "840B2D", "90B21F", "98E7F5", "A0B4A5", "B8AC6F", "C05627", "D0D2B0", "DC080F", "E4115B" -> "Broadcom"
            "001122", "001132" -> "Synology"
            "001217", "001333", "00188B", "0019D1", "001C23", "001E67", "001F3C", "00215D", "002314", "0023AE", "0024D7", "002564", "0026C7", "00270E", "002710", "1002B5", "105F06", "1402EC", "185E0F", "247703", "2816AD", "2C337A", "3413E8", "346895", "3822E2", "3C5282", "3C7C3F", "4025C2", "448500", "484520", "4851B7", "4C3488", "503EAA", "54E1AD", "5891CF", "5CC5D4", "5CE0C5", "605718", "641CAE", "645D86", "681CA2", "6CAF05", "70188B", "704D7B", "7440BB", "7824AF", "80C5F2", "843A35", "84A93E", "88B111", "90E2BA", "94E979", "9822EF", "985FD3", "A08869", "A0C589", "A4C361", "A8A159", "ACD1B8", "B44506", "B8B1E1", "BC8556", "C0B6F9", "C49DED", "CC2DB7", "D0C5F3", "D4FB6A", "D81265", "E006E6", "E4A471", "E82A44", "EC2E98", "F0DEF1", "F43909", "F875A4", "FC7774" -> "Intel"
            "001422", "0015C5", "00188B", "00190D", "001C23", "001D09", "002170", "002219", "0023AE", "0024E8", "002564", "0026B9", "00270E", "109836", "14FEB5", "180373", "18FB7B", "24B6FD", "28F10E", "34DBFD", "40F2E9", "44A842", "4CD98F", "54AB33", "5CF9DD", "64006A", "74867A", "842B2B", "8CEC4B", "90B11C", "98E7F4", "A4BB6D", "AC162D", "B04F13", "B8CA3A", "C4346B", "D067E5", "D481D7", "E454E8", "F01FAF", "F48E38", "F8B156" -> "Dell"
            "001635", "001708", "001E37", "001F29", "002170", "002264", "0023AE", "002481", "002564", "0026B9", "00270E", "040973", "080009", "081196", "0CC47A", "10604B", "186024", "204747", "288023", "308D99", "3464A9", "3CA82A", "40B034", "4437E6", "48BA4E", "5065F3", "5CF3FC", "643150", "6C3BE5", "70106F", "7446A0", "78E3B5", "80C16E", "843497", "8CDCD4", "9457A5", "9C8E99", "A0D3C1", "A45D36", "AC162D", "B05ADA", "B4B52F", "BC305B", "C025A5", "C4346B", "CC1531", "D067E5", "D481D7", "D89D67", "E0DB55", "E4115B", "E83935", "F0921C", "F40669", "F8BC12" -> "HP"
            "0050BA", "00A0C9", "001346", "0015E9", "00179A", "00195B", "001B11", "001CF0", "001E58", "002191", "0022B0", "002401", "00265A", "14D64D", "1C7EE5", "28107B", "340804", "58D56E", "7062B8", "84C9B2", "9094E4", "B0C554", "C4A81D", "D8FEE3", "E8CC18" -> "D-Link"
            "D850E6", "E0D9E3", "001A11", "20DFB9", "3C5AB4", "40831D", "54EE75", "70EE50", "94EBCD", "A47733", "E8039A", "F88FCA" -> "Google"
            "FCFBFB", "40ED98", "000000", "001132", "0014FF", "00163E", "005056", "080027", "0C2915" -> "Virtual/Cloud"
            else -> "Unknown Vendor"
        }
    }

    fun getFrequencyBand(freq: Int): String {
        return when {
            freq in 2400..2500 -> "2.4 GHz"
            freq in 4900..5900 -> "5 GHz"
            freq in 5925..7125 -> "6 GHz"
            else -> "Unknown"
        }
    }

    fun getWifiStandard(result: android.net.wifi.ScanResult): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return when (result.wifiStandard) {
                android.net.wifi.ScanResult.WIFI_STANDARD_11N -> "802.11n (Wi-Fi 4)"
                android.net.wifi.ScanResult.WIFI_STANDARD_11AC -> "802.11ac (Wi-Fi 5)"
                android.net.wifi.ScanResult.WIFI_STANDARD_11AX -> "802.11ax (Wi-Fi 6)"
                android.net.wifi.ScanResult.WIFI_STANDARD_11BE -> "802.11be (Wi-Fi 7)"
                else -> "802.11 b/g"
            }
        }
        return "802.11 b/g/n"
    }

    fun getChannel(freq: Int): Int {
        return when {
            freq == 2484 -> 14
            freq in 2407..2483 -> (freq - 2407) / 5 + 1
            freq in 5170..5825 -> (freq - 5170) / 5 + 34
            freq in 5945..7105 -> (freq - 5945) / 5 + 1
            else -> -1
        }
    }

    fun estimateDistance(rssi: Int, freq: Int): Double {
        if (freq <= 0) return 0.0
        val exp = (27.55 - (20 * kotlin.math.log10(freq.toDouble())) + kotlin.math.abs(rssi)) / 20.0
        return 10.0.pow(exp)
    }

    fun getSignalQuality(rssi: Int): String {
        return when {
            rssi > -60 -> "Excellent"
            rssi > -75 -> "Good"
            rssi > -85 -> "Fair"
            else -> "Poor"
        }
    }
}
