package com.herwin.netez.workers

import android.content.Context
import androidx.work.*
import com.herwin.netez.data.AppDatabase
import com.herwin.netez.data.entities.PhishingFeedEntry
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class PhishingFeedWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val client = OkHttpClient()

    override suspend fun doWork(): Result {
        val db = AppDatabase.getDatabase(applicationContext)
        val phishingDao = db.phishingDao()

        val request = Request.Builder()
            .url("https://openphish.com/feed.txt")
            .build()

        return try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val urls = body.lines()
                    .filter { it.isNotBlank() }
                    .map { PhishingFeedEntry(it.trim()) }
                
                if (urls.isNotEmpty()) {
                    phishingDao.clearFeed()
                    phishingDao.insertFeed(urls)
                }
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        fun schedule(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<PhishingFeedWorker>(1, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "PhishingFeedWork",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
