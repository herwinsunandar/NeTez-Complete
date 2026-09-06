package com.herwin.netez.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.ConnectionPool
import okhttp3.EventListener
import okhttp3.Handshake
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit
import java.security.cert.X509Certificate

data class HttpCheckResult(
    val url: String,
    val statusCode: Int,
    val responseTime: Long,
    val isAvailable: Boolean,
    val ipAddress: String = "",
    val errorMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    // Timings
    val dnsTime: Long = 0,
    val connectTime: Long = 0,
    val tlsTime: Long = 0,
    val ttfbTime: Long = 0,
    // SSL Info
    val hasSsl: Boolean = false,
    val sslSubject: String? = null,
    val sslIssuer: String? = null,
    val sslExpiryDays: Long? = null,
    // Response Data
    val headers: Map<String, String> = emptyMap(),
    val rawResponse: String? = null
)

object HttpCheckManager {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .connectionPool(ConnectionPool(0, 1, TimeUnit.NANOSECONDS))
        .build()

    suspend fun checkWebsite(urlString: String, method: String = "GET"): HttpCheckResult = withContext(Dispatchers.IO) {
        val formattedUrl = if (urlString.startsWith("http")) urlString else "https://$urlString"
        val request = Request.Builder()
            .url(formattedUrl)
            .method(method, null)
            .build()

        val timings = TimingCollector()
        val customClient = client.newBuilder()
            .eventListener(timings)
            .build()

        try {
            customClient.newCall(request).execute().use { response ->
                val handshake = response.handshake
                var sslSubject: String? = null
                var sslIssuer: String? = null
                var sslExpiryDays: Long? = null
                
                if (handshake != null) {
                    try {
                        val cert = handshake.peerCertificates.firstOrNull() as? X509Certificate
                        cert?.let {
                            sslSubject = it.subjectX500Principal.name
                            sslIssuer = it.issuerX500Principal.name
                            val diff = it.notAfter.time - System.currentTimeMillis()
                            sslExpiryDays = TimeUnit.MILLISECONDS.toDays(diff)
                        }
                    } catch (e: Exception) {}
                }

                HttpCheckResult(
                    url = urlString,
                    statusCode = response.code,
                    responseTime = timings.getTotalTime(),
                    isAvailable = response.isSuccessful,
                    ipAddress = timings.ipAddress ?: "",
                    dnsTime = timings.dnsTime,
                    connectTime = timings.connectTime,
                    tlsTime = timings.tlsTime,
                    ttfbTime = timings.ttfbTime,
                    hasSsl = handshake != null,
                    sslSubject = sslSubject,
                    sslIssuer = sslIssuer,
                    sslExpiryDays = sslExpiryDays,
                    headers = response.headers.toMap(),
                    rawResponse = if (method == "GET") response.peekBody(1024 * 10).string() else null
                )
            }
        } catch (e: Exception) {
            HttpCheckResult(
                url = urlString,
                statusCode = -1,
                responseTime = timings.getTotalTime().coerceAtLeast(0),
                isAvailable = false,
                errorMessage = e.localizedMessage,
                ipAddress = timings.ipAddress ?: ""
            )
        }
    }

    private class TimingCollector : EventListener() {
        var dnsStart = 0L
        var dnsTime = 0L
        var connectStart = 0L
        var connectTime = 0L
        var tlsStart = 0L
        var tlsTime = 0L
        var requestStart = 0L
        var ttfbTime = 0L
        var totalStart = 0L
        var ipAddress: String? = null

        override fun callStart(call: Call) { totalStart = System.currentTimeMillis() }
        override fun dnsStart(call: Call, domainName: String) { dnsStart = System.currentTimeMillis() }
        override fun dnsEnd(call: Call, domainName: String, inetAddressList: List<InetAddress>) {
            dnsTime = System.currentTimeMillis() - dnsStart
            ipAddress = inetAddressList.firstOrNull()?.hostAddress
        }
        override fun connectStart(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy) {
            connectStart = System.currentTimeMillis()
        }
        override fun connectEnd(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy, protocol: Protocol?) {
            connectTime = System.currentTimeMillis() - connectStart
        }
        override fun secureConnectStart(call: Call) { tlsStart = System.currentTimeMillis() }
        override fun secureConnectEnd(call: Call, handshake: Handshake?) {
            tlsTime = System.currentTimeMillis() - tlsStart
        }
        override fun requestHeadersStart(call: Call) { requestStart = System.currentTimeMillis() }
        override fun responseHeadersStart(call: Call) {
            ttfbTime = System.currentTimeMillis() - requestStart
        }
        fun getTotalTime() = System.currentTimeMillis() - totalStart
    }
}
