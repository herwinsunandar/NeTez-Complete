package com.herwin.netez.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL

data class HttpCheckResult(
    val url: String,
    val statusCode: Int,
    val responseTime: Long,
    val isAvailable: Boolean,
    val ipAddress: String = "",
    val errorMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

object HttpCheckManager {
    suspend fun checkWebsite(urlString: String): HttpCheckResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val formattedUrl = if (urlString.startsWith("http")) urlString else "https://$urlString"
            val url = URL(formattedUrl)
            
            // Get IP Address
            val ipAddress = try {
                InetAddress.getByName(url.host).hostAddress ?: ""
            } catch (e: Exception) { "" }

            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            
            val responseCode = conn.responseCode
            val endTime = System.currentTimeMillis()
            
            HttpCheckResult(
                url = urlString,
                statusCode = responseCode,
                responseTime = endTime - startTime,
                isAvailable = responseCode in 200..399,
                ipAddress = ipAddress
            )
        } catch (e: Exception) {
            HttpCheckResult(
                url = urlString,
                statusCode = -1,
                responseTime = System.currentTimeMillis() - startTime,
                isAvailable = false,
                errorMessage = e.localizedMessage
            )
        }
    }
}
