package com.herwin.netez.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.herwin.netez.data.wifi.VendorDatabase
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

class OUIUpdateWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    override suspend fun doWork(): Result {
        return try {
            val request = Request.Builder()
                .url("https://standards-oui.ieee.org/oui/oui.txt")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return Result.failure()
                    if (body.length > 1024 * 1024) { // > 1MB
                        processAndSave(body)
                        saveTimestamp()
                        Result.success()
                    } else {
                        Result.failure()
                    }
                } else {
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun processAndSave(data: String) {
        val filteredLines = mutableListOf<String>()
        data.lineSequence().forEach { line ->
            if (line.contains("(hex)")) {
                val parts = line.split("(hex)")
                if (parts.size >= 2) {
                    val macDash = parts[0].trim()
                    val vendorFull = parts[1].trim()
                    val shortName = VendorDatabase.shortenVendorName(vendorFull)
                    filteredLines.add("$macDash (hex) $shortName")
                }
            }
        }

        if (filteredLines.isNotEmpty()) {
            val cacheFile = File(applicationContext.filesDir, "oui_cached.txt")
            cacheFile.writeText(filteredLines.joinToString("\n"))
        }
    }

    private fun saveTimestamp() {
        val prefs = applicationContext.getSharedPreferences("netez_prefs", Context.MODE_PRIVATE)
        prefs.edit().putLong("oui_last_update", System.currentTimeMillis()).apply()
    }
}
