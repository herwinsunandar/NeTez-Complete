package com.herwin.netez.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object HttpHeaderManager {
    suspend fun getHeaders(urlString: String): Map<String?, List<String>> = withContext(Dispatchers.IO) {
        try {
            val url = if (urlString.startsWith("http")) URL(urlString) else URL("http://$urlString")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "HEAD"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.headerFields
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
