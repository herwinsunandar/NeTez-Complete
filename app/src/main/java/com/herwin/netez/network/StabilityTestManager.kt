package com.herwin.netez.network

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.sync.Semaphore
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.system.measureTimeMillis

data class StabilityUpdate(
    val latency: Double,
    val isSuccess: Boolean,
    val progress: Float,
    val packetLoss: Int,
    val avgLatency: Double
)

object StabilityTestManager {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val testUpdates = MutableSharedFlow<StabilityUpdate>()
    
    private var testJob: Job? = null

    fun isPrivateIp(ip: String): Boolean {
        val parts = ip.split(".").mapNotNull { it.toIntOrNull() }
        if (parts.size != 4) return false
        return (parts[0] == 192 && parts[1] == 168) ||
                (parts[0] == 10) ||
                (parts[0] == 172 && parts[1] in 16..31)
    }

    suspend fun stopTest() {
        testJob?.cancelAndJoin()
        testJob = null
    }

    fun runLocalLoad(
        target: String,
        threads: Int,
        durationSeconds: Int,
        onComplete: (avg: Double, max: Double, loss: Int, rate: Double) -> Unit
    ) {
        testJob = CoroutineScope(Dispatchers.IO).launch {
            if (!isPrivateIp(target)) {
                Log.e("StabilityTest", "Non-private IP blocked for Local Load")
                return@launch
            }

            val startTime = System.currentTimeMillis()
            val durationMs = durationSeconds * 1000L
            val semaphore = Semaphore(threads)
            val successCount = AtomicInteger(0)
            val totalCount = AtomicInteger(0)
            val latencies = mutableListOf<Double>()
            var maxLatency = 0.0

            while (isActive && (System.currentTimeMillis() - startTime) < durationMs) {
                semaphore.acquire()
                launch {
                    try {
                        val latency = measureConnection(target)
                        totalCount.incrementAndGet()
                        if (latency >= 0) {
                            successCount.incrementAndGet()
                            synchronized(latencies) {
                                latencies.add(latency)
                                if (latency > maxLatency) maxLatency = latency
                            }
                        }
                        
                        val avg = synchronized(latencies) { if (latencies.isEmpty()) 0.0 else latencies.average() }
                        val progress = ((System.currentTimeMillis() - startTime).toFloat() / durationMs).coerceIn(0f, 1f)
                        val loss = totalCount.get() - successCount.get()
                        testUpdates.emit(StabilityUpdate(if (latency >= 0) latency else 0.0, latency >= 0, progress, loss, avg))
                    } finally {
                        semaphore.release()
                    }
                }
                // Optimization: Increase delay to prevent high CPU usage during debug
                delay(50) 
            }
            
            (this.coroutineContext[Job] as? Job)?.children?.forEach { it.join() }

            val finalAvg = synchronized(latencies) { if (latencies.isEmpty()) 0.0 else latencies.average() }
            val finalLoss = totalCount.get() - successCount.get()
            val finalSuccessRate = if (totalCount.get() > 0) (successCount.get().toDouble() / totalCount.get()) * 100 else 0.0
            
            onComplete(finalAvg, maxLatency, finalLoss, finalSuccessRate)
        }
    }

    private fun measureConnection(target: String): Double {
        val ports = listOf(80, 443)
        for (port in ports) {
            try {
                val socket = Socket()
                val time = measureTimeMillis {
                    socket.connect(InetSocketAddress(target, port), 1500)
                }
                socket.close()
                return time.toDouble()
            } catch (e: Exception) {
                continue
            }
        }
        return -1.0
    }

    fun runBufferbloat(
        url: String,
        durationSeconds: Int,
        onComplete: (idle: Double, loaded: Double, bloat: Double, max: Double, loss: Int, rate: Double) -> Unit
    ) {
        testJob = CoroutineScope(Dispatchers.IO).launch {
            val idleLatencies = mutableListOf<Double>()
            repeat(5) {
                val res = PingManager.ping("8.8.8.8")
                if (res.isSuccessful) idleLatencies.add(res.time)
                delay(200)
            }
            val idleLatency = if (idleLatencies.isNotEmpty()) idleLatencies.average() else 0.0

            val startTime = System.currentTimeMillis()
            val durationMs = durationSeconds * 1000L
            val loadedLatencies = mutableListOf<Double>()
            val totalCount = AtomicInteger(0)
            val successCount = AtomicInteger(0)
            var maxLatency = 0.0

            val downloadJob = launch {
                try {
                    while (isActive && (System.currentTimeMillis() - startTime) < durationMs) {
                        val request = Request.Builder().url(url).build()
                        client.newCall(request).execute().use { response ->
                            val body = response.body ?: return@use
                            val inputStream = body.byteStream()
                            val buffer = ByteArray(128 * 1024)
                            while (isActive && inputStream.read(buffer) != -1) {
                                if ((System.currentTimeMillis() - startTime) > durationMs) break
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("StabilityTest", "Download error: ${e.message}")
                }
            }

            while (isActive && (System.currentTimeMillis() - startTime) < durationMs) {
                val res = PingManager.ping("8.8.8.8")
                totalCount.incrementAndGet()
                if (res.isSuccessful) {
                    successCount.incrementAndGet()
                    loadedLatencies.add(res.time)
                    if (res.time > maxLatency) maxLatency = res.time
                }
                
                val currentLoadedAvg = if (loadedLatencies.isNotEmpty()) loadedLatencies.average() else 0.0
                val progress = ((System.currentTimeMillis() - startTime).toFloat() / durationMs).coerceIn(0f, 1f)
                val loss = totalCount.get() - successCount.get()
                testUpdates.emit(StabilityUpdate(if(res.isSuccessful) res.time else 0.0, res.isSuccessful, progress, loss, currentLoadedAvg))
                
                delay(1000)
            }

            downloadJob.cancelAndJoin()
            
            val loadedLatency = if (loadedLatencies.isNotEmpty()) loadedLatencies.average() else 0.0
            val bufferbloat = (loadedLatency - idleLatency).coerceAtLeast(0.0)
            val finalLoss = totalCount.get() - successCount.get()
            val finalSuccessRate = if (totalCount.get() > 0) (successCount.get().toDouble() / totalCount.get()) * 100 else 0.0
            
            onComplete(idleLatency, loadedLatency, bufferbloat, maxLatency, finalLoss, finalSuccessRate)
        }
    }
}
