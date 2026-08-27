package com.herwin.netez.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

data class PingResult(
    val ip: String,
    val time: Double,
    val isSuccessful: Boolean,
    val ttl: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val errorMessage: String? = null
)

object PingManager {
    suspend fun ping(host: String, packetSize: Int = 56): PingResult = withContext(Dispatchers.IO) {
        try {
            // -c 1: count 1, -s size: packet size
            val process = Runtime.getRuntime().exec("ping -c 1 -s $packetSize $host")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            var time = -1.0
            var ttl = 0
            
            while (reader.readLine().also { line = it } != null) {
                line?.let {
                    if (it.contains("time=")) {
                        val timeParts = it.split("time=")
                        if (timeParts.size > 1) {
                            val timeVal = timeParts[1].split(" ")[0]
                            time = timeVal.toDoubleOrNull() ?: -1.0
                        }
                    }
                    if (it.contains("ttl=")) {
                        val ttlParts = it.split("ttl=")
                        if (ttlParts.size > 1) {
                            val ttlVal = ttlParts[1].split(" ")[0]
                            ttl = ttlVal.toIntOrNull() ?: 0
                        }
                    }
                }
            }
            
            val exitCode = process.waitFor()
            if (exitCode == 0 && time >= 0) {
                PingResult(host, time, true, ttl)
            } else {
                PingResult(host, 0.0, false, errorMessage = "Request timeout or host unreachable")
            }
        } catch (e: Exception) {
            PingResult(host, 0.0, false, errorMessage = e.message)
        }
    }
}
