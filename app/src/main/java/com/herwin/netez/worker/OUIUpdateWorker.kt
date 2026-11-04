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
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    override suspend fun doWork(): Result {
        val urls = listOf(
            "https://standards-oui.ieee.org/oui/oui.txt",
            "https://gitlab.com/wireshark/wireshark/-/raw/master/manuf",
            "https://raw.githubusercontent.com/manuf-db/manuf/master/manuf"
        )

        for (url in urls) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Netez/1.0")
                    .build()

                val response = client.newCall(request).execute()
                response.use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string() ?: return@use
                        if (body.length > 100 * 1024) { // > 100KB
                            val savedCount = processAndSave(body)
                            if (savedCount > 0) {
                                saveStats(savedCount)
                                VendorDatabase.reloadCache(applicationContext)
                                return Result.success()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return Result.failure()
    }

    private fun processAndSave(data: String): Int {
        val filteredLines = mutableListOf<String>()
        data.lineSequence().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEach

            if (trimmed.contains("(hex)")) {
                val parts = trimmed.split("(hex)")
                if (parts.size >= 2) {
                    val macDash = parts[0].trim()
                    val vendorFull = parts[1].trim()
                    val shortName = VendorDatabase.shortenVendorName(vendorFull)
                    filteredLines.add("$macDash (hex) $shortName")
                }
            } else if (trimmed.contains("\t") || trimmed.contains(" ")) {
                val parts = trimmed.split(Regex("\\s+"), 2)
                if (parts.size >= 2) {
                    val prefix = parts[0].trim()
                    if (prefix.length in 6..8 && prefix.contains(":")) {
                        val vendorFull = parts[1].trim()
                        val shortName = VendorDatabase.shortenVendorName(vendorFull)
                        filteredLines.add("$prefix (hex) $shortName")
                    }
                }
            }
        }

        if (filteredLines.isNotEmpty()) {
            val cacheFile = File(applicationContext.filesDir, "oui_cached.txt")
            cacheFile.writeText(filteredLines.joinToString("\n"))
        }

        return filteredLines.size
    }

    private fun saveStats(count: Int) {
        val prefs = applicationContext.getSharedPreferences("netez_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putLong("oui_last_update", System.currentTimeMillis())
            .putInt("oui_record_count", count)
            .apply()
    }
}
