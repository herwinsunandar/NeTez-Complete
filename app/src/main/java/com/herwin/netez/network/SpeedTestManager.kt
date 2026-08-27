package com.herwin.netez.network

import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs

data class AdvancedMetrics(
    val jitterMs: Double = 0.0,
    val packetLoss: Int = 0,
    val isp: String = "Detecting..."
)

object SpeedTestManager {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun runPingTest(host: String = "8.8.8.8"): Pair<Double, Double> = withContext(Dispatchers.IO) {
        val latencies = mutableListOf<Double>()
        repeat(5) {
            val result = PingManager.ping(host)
            if (result.isSuccessful) latencies.add(result.time)
            delay(100)
        }
        
        val avgPing = if (latencies.isNotEmpty()) latencies.average() else 0.0
        val jitter = if (latencies.size > 1) {
            latencies.zipWithNext { a, b -> abs(a - b) }.average()
        } else 0.0
        
        Pair(avgPing, jitter)
    }

    suspend fun runDownload(
        url: String, 
        durationMs: Long = 10000L,
        onUpdate: (Double, Float) -> Unit
    ) = withContext(Dispatchers.IO) {
        val totalBytesRead = AtomicLong(0)
        val startTime = System.currentTimeMillis()
        val numThreads = 4
        
        val jobs = (1..numThreads).map {
            launch {
                try {
                    while (isActive && (System.currentTimeMillis() - startTime) < durationMs) {
                        val request = Request.Builder().url(url).build()
                        client.newCall(request).execute().use { response ->
                            val body = response.body ?: return@use
                            val inputStream = body.byteStream()
                            val buffer = ByteArray(65536)
                            var bytesRead = 0
                            while (isActive && inputStream.read(buffer).also { bytesRead = it } != -1) {
                                totalBytesRead.addAndGet(bytesRead.toLong())
                                if (System.currentTimeMillis() - startTime > durationMs) break
                            }
                        }
                    }
                } catch (e: Exception) {}
            }
        }

        while (System.currentTimeMillis() - startTime < durationMs) {
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed > 100) {
                val speed = (totalBytesRead.get() * 8.0) / (elapsed / 1000.0 * 1_000_000.0)
                onUpdate(speed, (elapsed.toFloat() / durationMs).coerceAtMost(1f))
            }
            delay(100)
        }
        jobs.forEach { it.cancelAndJoin() }
    }

    suspend fun runUpload(
        url: String, 
        durationMs: Long = 10000L,
        onUpdate: (Double, Float) -> Unit
    ) = withContext(Dispatchers.IO) {
        val totalBytesWritten = AtomicLong(0)
        val startTime = System.currentTimeMillis()
        val data = ByteArray(1024 * 1024) 
        val numThreads = 2

        val jobs = (1..numThreads).map {
            launch {
                try {
                    while (isActive && (System.currentTimeMillis() - startTime) < durationMs) {
                        val requestBody = data.toRequestBody("application/octet-stream".toMediaType())
                        val request = Request.Builder().url(url).post(requestBody).build()
                        client.newCall(request).execute().use {
                            totalBytesWritten.addAndGet(data.size.toLong())
                        }
                    }
                } catch (e: Exception) {}
            }
        }

        while (System.currentTimeMillis() - startTime < durationMs) {
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed > 100) {
                val speed = (totalBytesWritten.get() * 8.0) / (elapsed / 1000.0 * 1_000_000.0)
                onUpdate(speed, (elapsed.toFloat() / durationMs).coerceAtMost(1f))
            }
            delay(100)
        }
        jobs.forEach { it.cancelAndJoin() }
    }
}
