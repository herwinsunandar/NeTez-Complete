package com.herwin.netez.workers

import android.content.Context
import androidx.work.*
import com.herwin.netez.data.AppDatabase
import com.herwin.netez.data.SecurityPreferences
import com.herwin.netez.data.entities.UrlHausEntry
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URL
import java.util.concurrent.TimeUnit

class UrlHausFeedWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private val client by lazy { OkHttpClient() }

    override suspend fun doWork(): Result {
        return try {
            val csvUrl = "https://urlhaus.abuse.ch/downloads/csv_recent/"
            val request = Request.Builder().url(csvUrl).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return Result.retry()

            val csvText = response.body?.string() ?: return Result.failure()
            val lines = csvText.lines().filter { !it.startsWith("#") && it.isNotBlank() }
            
            val db = AppDatabase.getDatabase(applicationContext)
            val entries = lines.mapNotNull { line ->
                try {
                    val parts = parseCsvLine(line)
                    if (parts.size >= 8) {
                        val url = parts[2].removeSurrounding("\"")
                        val threat = parts[5].removeSurrounding("\"")
                        val tags = parts[6].removeSurrounding("\"")
                        val ref = parts[7].removeSurrounding("\"")
                        val date = parts[1].removeSurrounding("\"")
                        
                        UrlHausEntry(
                            url = url.lowercase(),
                            domain = extractDomain(url),
                            threat = threat,
                            tags = tags,
                            reference = ref,
                            dateAdded = date
                        )
                    } else null
                } catch (e: Exception) {
                    null
                }
            }.take(30000)

            db.urlHausDao().replaceAll(entries)
            
            val prefs = SecurityPreferences(applicationContext)
            prefs.urlHausLastUpdate = System.currentTimeMillis()

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                inQuotes = !inQuotes
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString())
                current = StringBuilder()
            } else {
                current.append(c)
            }
            i++
        }
        result.add(current.toString())
        return result
    }

    private fun extractDomain(url: String): String {
        return try {
            val uri = URL(url)
            uri.host
        } catch (e: Exception) {
            url
        }
    }

    companion object {
        fun schedule(context: Context, immediate: Boolean = false) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<UrlHausFeedWorker>(12, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "UrlHausFeedWork",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )

            if (immediate) {
                val oneTimeRequest = OneTimeWorkRequestBuilder<UrlHausFeedWorker>()
                    .setConstraints(constraints)
                    .build()
                WorkManager.getInstance(context).enqueue(oneTimeRequest)
            }
        }
    }
}
