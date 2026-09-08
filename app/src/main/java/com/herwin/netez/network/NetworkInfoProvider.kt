package com.herwin.netez.network

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.TelephonyManager
import com.herwin.netez.utils.NetworkUtils
import java.net.Inet4Address

data class UnifiedNetworkInfo(
    val ssidOrOperator: String,
    val networkType: String,
    val localIp: String,
    val publicIp: String,
    val gateway: String,
    val isp: String,
    val isWifi: Boolean,
    val wifiCaps: String?
)

object NetworkInfoProvider {

    @SuppressLint("MissingPermission")
    suspend fun getUnifiedNetworkInfo(context: Context): UnifiedNetworkInfo {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetwork
        val caps = cm.getNetworkCapabilities(activeNetwork)
        val linkProps = cm.getLinkProperties(activeNetwork)
        
        val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val isCellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        
        val localIp = linkProps?.linkAddresses?.firstOrNull { 
            it.address is Inet4Address && !it.address.isLoopbackAddress 
        }?.address?.hostAddress ?: "0.0.0.0"
        
        val gatewayIp = if (isWifi) {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            val dhcpGateway = NetworkUtils.formatIp(wm.dhcpInfo.gateway)
            if (dhcpGateway != "0.0.0.0") dhcpGateway else {
                linkProps?.routes?.firstOrNull()?.gateway?.hostAddress ?: "-"
            }
        } else {
            linkProps?.routes?.firstOrNull { it.gateway != null }?.gateway?.hostAddress ?: "-"
        }
        
        val ssidOrOperator = when {
            isWifi -> {
                val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                @Suppress("DEPRECATION")
                val info = wm.connectionInfo
                var ssid = info.ssid?.removeSurrounding("\"") ?: ""
                if (ssid == "<unknown ssid>" || ssid.isEmpty()) {
                    val scanResults = try { wm.scanResults } catch (e: Exception) { emptyList() }
                    val current = scanResults.find { it.BSSID == info.bssid }
                    current?.SSID ?: "WiFi Network"
                } else ssid
            }
            isCellular -> {
                val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                tm.networkOperatorName?.takeIf { it.isNotEmpty() } ?: "Cellular Network"
            }
            else -> "Disconnected"
        }
        
        val networkType = when {
            isWifi -> {
                val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                @Suppress("DEPRECATION")
                val info = wm.connectionInfo
                val freq = info.frequency
                if (freq > 5000) "WiFi 5GHz" else if (freq > 0) "WiFi 2.4GHz" else "WiFi"
            }
            isCellular -> {
                val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                getMobileNetworkType(tm)
            }
            else -> "None"
        }
        
        val publicIp = NetworkUtils.getPublicIp()
        val isp = NetworkUtils.getIspInfo()
        
        return UnifiedNetworkInfo(
            ssidOrOperator = ssidOrOperator,
            networkType = networkType,
            localIp = localIp,
            publicIp = publicIp,
            gateway = gatewayIp,
            isp = if (isCellular && (isp.isEmpty() || isp.contains("Unknown"))) ssidOrOperator else isp,
            isWifi = isWifi,
            wifiCaps = if (isWifi) getCurrentWifiCaps(context) else null
        )
    }

    @SuppressLint("MissingPermission")
    private fun getMobileNetworkType(tm: TelephonyManager): String {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) tm.dataNetworkType else tm.networkType
        return when (type) {
            TelephonyManager.NETWORK_TYPE_GPRS, TelephonyManager.NETWORK_TYPE_EDGE, TelephonyManager.NETWORK_TYPE_CDMA, 
            TelephonyManager.NETWORK_TYPE_1xRTT, TelephonyManager.NETWORK_TYPE_IDEN -> "2G"
            
            TelephonyManager.NETWORK_TYPE_UMTS, TelephonyManager.NETWORK_TYPE_HSDPA, TelephonyManager.NETWORK_TYPE_HSUPA, 
            TelephonyManager.NETWORK_TYPE_HSPA, TelephonyManager.NETWORK_TYPE_EVDO_0, TelephonyManager.NETWORK_TYPE_EVDO_A, 
            TelephonyManager.NETWORK_TYPE_EVDO_B, TelephonyManager.NETWORK_TYPE_EHRPD, TelephonyManager.NETWORK_TYPE_HSPAP -> "3G"
            
            TelephonyManager.NETWORK_TYPE_LTE -> "4G"
            TelephonyManager.NETWORK_TYPE_NR -> "5G"
            else -> "Cellular"
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentWifiCaps(context: Context): String {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        @Suppress("DEPRECATION")
        val info = wifiManager.connectionInfo
        val bssid = info.bssid ?: return ""
        return wifiManager.scanResults?.find { it.BSSID == bssid }?.capabilities ?: ""
    }
}
