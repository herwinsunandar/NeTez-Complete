package com.herwin.netez.service

import android.content.Context
import android.util.Log
import com.herwin.netez.data.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

class MonitoringProcessor(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val dao = database.monitoringDao()
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun processTarget(target: MonitoringTarget) {
        val startTime = System.currentTimeMillis()
        var isUp = false
        var message = ""
        var latency = 0L

        try {
            when (target.type) {
                MonitoringType.PING -> {
                    // address.isReachable is unreliable on Android. Use shell ping or TCP connect.
                    isUp = try {
                        val process = Runtime.getRuntime().exec("ping -c 1 -W 2 ${target.host}")
                        process.waitFor() == 0
                    } catch (e: Exception) {
                        // Fallback to TCP connect on port 80/443 if ping command fails
                        tryConnect(target.host ?: "", 80) || tryConnect(target.host ?: "", 443)
                    }
                    latency = System.currentTimeMillis() - startTime
                    message = if (isUp) "Host is reachable" else "Host unreachable (Timeout)"
                }
                MonitoringType.PORT -> {
                    isUp = tryConnect(target.host ?: "", target.port ?: 80)
                    latency = System.currentTimeMillis() - startTime
                    message = if (isUp) "Port ${target.port} is open" else "Port ${target.port} connection refused/timeout"
                }
                MonitoringType.HTTP -> {
                    val request = Request.Builder().url(target.url ?: "").build()
                    client.newCall(request).execute().use { response ->
                        isUp = response.isSuccessful
                        val body = response.peekBody(1024 * 10).string()
                        if (isUp && !target.keyword.isNullOrEmpty()) {
                            isUp = body.contains(target.keyword, ignoreCase = true)
                            message = if (isUp) "HTTP ${response.code} • Keyword found" else "HTTP ${response.code} • Keyword NOT found"
                        } else {
                            message = "HTTP ${response.code} • ${response.message}"
                        }
                    }
                    latency = System.currentTimeMillis() - startTime
                }
                MonitoringType.DNS -> {
                    val address = InetAddress.getByName(target.host)
                    isUp = address.hostAddress != null
                    latency = System.currentTimeMillis() - startTime
                    message = "Resolved to ${address.hostAddress}"
                }
            }
        } catch (e: Exception) {
            isUp = false
            message = e.localizedMessage ?: "Connection error"
            latency = System.currentTimeMillis() - startTime
        }

        val status = if (isUp) "UP" else "DOWN"
        val now = System.currentTimeMillis()
        
        Log.d("MonitoringProcessor", "Target ${target.host ?: target.url} is $status (Latency: ${latency}ms)")
        
        // Update Target
        val newFailCount = if (isUp) 0 else target.failCount + 1
        val updatedTarget = target.copy(
            lastStatus = status,
            lastLatency = latency,
            lastCheckTimestamp = now,
            failCount = newFailCount
        )
        dao.updateTarget(updatedTarget)

        // Log result
        val logEntry = MonitoringLog(
            targetId = target.id,
            timestamp = now,
            status = status,
            latency = latency,
            message = message
        )
        dao.insertAndPruneLog(logEntry)

        // Trigger notification if status changed
        if (!isUp && newFailCount >= target.failThreshold && target.lastStatus == "UP") {
            MonitoringNotificationHelper.showDownAlert(context, target, message)
        } else if (isUp && target.lastStatus == "DOWN") {
            MonitoringNotificationHelper.showUpAlert(context, target)
        }
    }

    private fun tryConnect(host: String, port: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 4000)
                true
            }
        } catch (e: Exception) {
            false
        }
    }
}
