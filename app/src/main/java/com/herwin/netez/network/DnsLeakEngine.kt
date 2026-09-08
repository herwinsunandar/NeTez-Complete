package com.herwin.netez.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.herwin.netez.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class DnsLeakStatus {
    SAFE, LEAKED, INFO
}

data class DnsLeakItem(
    val ip: String,
    val isp: String,
    val country: String,
    val status: DnsLeakStatus,
    val detail: String,
    val latency: Long = 0
)

data class DnsLeakResult(
    val publicIp: String,
    val isp: String,
    val dnsServers: List<DnsLeakItem>,
    val leakCount: Int,
    val score: Int,
    val status: String, // "SAFE", "WARNING", "CRITICAL"
    val vpnActive: Boolean,
    val testDurationMs: Long
)

object DnsLeakEngine {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun testDnsLeak(context: Context): DnsLeakResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        
        // 1. Get VPN Status
        val vpnActive = isVpnActive(context)
        
        // 2. Get Public IP and ISP
        val publicIp = NetworkUtils.getPublicIp()
        val ispData = NetworkUtils.getIspData()
        val publicIsp = ispData["isp"] ?: "Unknown"
        
        // 3. Get current DNS servers from system
        val dnsServers = getCurrentDnsServers(context)
        
        val checks = dnsServers.map { dnsIp ->
            async {
                val dnsStartTime = System.currentTimeMillis()
                try {
                    // Get ISP of the DNS server
                    val url = "http://ip-api.com/json/$dnsIp?fields=org,country"
                    val request = Request.Builder().url(url).build()
                    val response = client.newCall(request).execute()
                    val body = response.body?.string()
                    val latency = System.currentTimeMillis() - dnsStartTime
                    
                    if (body != null) {
                        val json = JSONObject(body)
                        val dnsIsp = json.optString("org", "Unknown")
                        val country = json.optString("country", "Unknown")
                        
                        val isPublicDns = isKnownPublicDns(dnsIp, dnsIsp)
                        
                        // Logic: If VPN is active but DNS ISP matches public ISP and not a known public DNS -> LEAK
                        val status = when {
                            vpnActive && dnsIsp.contains(publicIsp, ignoreCase = true) && !isPublicDns -> DnsLeakStatus.LEAKED
                            !vpnActive -> DnsLeakStatus.INFO
                            else -> DnsLeakStatus.SAFE
                        }
                        
                        DnsLeakItem(dnsIp, dnsIsp, country, status, "DNS $dnsIp - $dnsIsp", latency)
                    } else {
                        DnsLeakItem(dnsIp, "Unknown", "Unknown", DnsLeakStatus.SAFE, "DNS $dnsIp", latency)
                    }
                } catch (e: Exception) {
                    DnsLeakItem(dnsIp, "Unknown", "Unknown", DnsLeakStatus.SAFE, "DNS $dnsIp (Error: ${e.localizedMessage})", System.currentTimeMillis() - dnsStartTime)
                }
            }
        }.awaitAll()
        
        val leakCount = checks.count { it.status == DnsLeakStatus.LEAKED }
        val score = when {
            leakCount == 0 -> 0
            leakCount <= 2 -> 50
            else -> 90
        }
        val statusText = when {
            leakCount == 0 -> "SAFE"
            leakCount <= 2 -> "WARNING"
            else -> "CRITICAL"
        }
        
        DnsLeakResult(
            publicIp = publicIp,
            isp = publicIsp,
            dnsServers = checks,
            leakCount = leakCount,
            score = score,
            status = statusText,
            vpnActive = vpnActive,
            testDurationMs = System.currentTimeMillis() - startTime
        )
    }

    private fun isVpnActive(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
    }

    private fun getCurrentDnsServers(context: Context): List<String> {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val linkProps = cm.getLinkProperties(cm.activeNetwork)
        return linkProps?.dnsServers?.mapNotNull { it.hostAddress } ?: emptyList()
    }

    private fun isKnownPublicDns(ip: String, isp: String): Boolean {
        val publicIspKeywords = listOf("Google", "Cloudflare", "OpenDNS", "Quad9", "NextDNS", "Cisco")
        return publicIspKeywords.any { isp.contains(it, ignoreCase = true) } ||
               ip == "8.8.8.8" || ip == "8.8.4.4" || ip == "1.1.1.1" || ip == "1.0.0.1" || ip == "9.9.9.9"
    }
}
