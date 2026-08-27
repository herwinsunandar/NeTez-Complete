package com.herwin.netez.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class DnsRecordType(val value: Int) {
    A(1),
    AAAA(28),
    MX(15),
    TXT(16),
    CNAME(5),
    NS(2),
    PTR(12)
}

data class DnsServer(
    val name: String,
    val ip: String,
    val apiUrl: String? = null, // For DNS over HTTPS JSON APIs
    val countryCode: String = ""
)

val PublicDnsServers = listOf(
    DnsServer("Cloudflare", "1.1.1.1", "https://cloudflare-dns.com/dns-query", "US"),
    DnsServer("Google", "8.8.8.8", "https://dns.google/resolve", "US"),
    DnsServer("Quad9", "9.9.9.9", "https://dns.quad9.net:5053/dns-query", "CH"),
    DnsServer("Local ISP", "System", countryCode = "")
)

data class DnsRecord(
    val data: String,
    val ttl: Int,
    val type: Int,
    var provider: String = "Unknown Provider",
    var countryCode: String = "",
    var countryName: String = ""
)

data class DnsResponse(
    val records: List<DnsRecord>,
    val resolver: String,
    val resolverIp: String,
    val latency: Long = 0
)

object DnsManager {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun lookup(
        domain: String, 
        type: DnsRecordType = DnsRecordType.A,
        server: DnsServer = PublicDnsServers[0]
    ): DnsResponse = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val url = if (server.apiUrl != null) {
                "${server.apiUrl}?name=$domain&type=${type.name}"
            } else {
                // Fallback to Cloudflare if no API URL (Local ISP logic usually needs JNDI or native calls which are complex in Android)
                "https://cloudflare-dns.com/dns-query?name=$domain&type=${type.name}"
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("Accept", "application/dns-json")
                .build()
            
            client.newCall(request).execute().use { response ->
                val endTime = System.currentTimeMillis()
                val latency = endTime - startTime
                
                if (!response.isSuccessful) return@withContext DnsResponse(emptyList(), server.name, server.ip, latency)
                
                val body = response.body?.string() ?: return@withContext DnsResponse(emptyList(), server.name, server.ip, latency)
                val json = JSONObject(body)
                val answer = json.optJSONArray("Answer") ?: return@withContext DnsResponse(emptyList(), server.name, server.ip, latency)
                
                val results = mutableListOf<DnsRecord>()
                for (i in 0 until answer.length()) {
                    val recordJson = answer.getJSONObject(i)
                    val data = recordJson.getString("data")
                    val ttl = recordJson.getInt("TTL")
                    val recordType = recordJson.getInt("type")
                    
                    val record = DnsRecord(data, ttl, recordType)
                    
                    // Fetch GeoIP info for IP records
                    if (recordType == 1 || recordType == 28) {
                        fetchGeoInfo(record)
                    }
                    
                    results.add(record)
                }
                DnsResponse(results, server.name, server.ip, latency)
            }
        } catch (e: Exception) {
            DnsResponse(emptyList(), server.name, server.ip, 0)
        }
    }

    private fun fetchGeoInfo(record: DnsRecord) {
        try {
            // Basic check if data is IP
            if (!record.data.contains(".") && !record.data.contains(":")) return
            
            val geoUrl = "http://ip-api.com/json/${record.data}"
            val request = Request.Builder().url(geoUrl).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return
                    val json = JSONObject(body)
                    if (json.getString("status") == "success") {
                        record.provider = json.optString("isp", "Unknown ISP")
                        record.countryCode = json.optString("countryCode", "")
                        record.countryName = json.optString("country", "")
                    }
                }
            }
        } catch (e: Exception) {}
    }
}
