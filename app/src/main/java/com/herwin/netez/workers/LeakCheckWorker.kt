package com.herwin.netez.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.herwin.netez.R
import com.herwin.netez.data.AppDatabase
import com.herwin.netez.data.entities.LeakCheckEntry
import com.herwin.netez.ui.viewmodels.BreachInfo
import com.herwin.netez.ui.viewmodels.LeakCheckResult
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class LeakCheckWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val client = OkHttpClient()

    override suspend fun doWork(): Result {
        val db = AppDatabase.getDatabase(applicationContext)
        val leakDao = db.leakCheckDao()
        
        val entries = leakDao.getAllLeakChecksList()
        if (entries.isEmpty()) return Result.success()

        entries.forEach { entry ->
            try {
                val checkResult = performLeakCheck(entry.email)
                if (checkResult.isBreached && checkResult.breaches.size > entry.breachCount) {
                    val newBreach = checkResult.breaches.firstOrNull() // Simplified
                    sendNotification(
                        title = "Leak Alert: ${entry.email}",
                        message = "New breach found: ${newBreach?.name ?: "Unknown"}"
                    )
                }
                
                leakDao.insertLeakCheck(entry.copy(
                    breachesJson = JSONObject().apply { put("breaches", checkResult.breaches.toString()) }.toString(),
                    lastChecked = System.currentTimeMillis(),
                    isBreached = checkResult.isBreached,
                    breachCount = checkResult.breaches.size
                ))
            } catch (e: Exception) {}
        }
        
        return Result.success()
    }

    private fun performLeakCheck(email: String): LeakCheckResult {
        val request = Request.Builder()
            .url("https://api.xposedornot.com/v1/check-email/$email")
            .build()
            
        return try {
            val response = client.newCall(request).execute()
            if (response.code == 200) {
                val json = JSONObject(response.body?.string() ?: "")
                val breachesArray = json.optJSONArray("breaches")
                val breaches = mutableListOf<BreachInfo>()
                if (breachesArray != null) {
                    for (i in 0 until breachesArray.length()) {
                        val breach = breachesArray.getJSONArray(i)
                        breaches.add(
                            BreachInfo(
                                name = breach.optString(0),
                                year = breach.optString(1),
                                data = "Email, Password"
                            )
                        )
                    }
                }
                LeakCheckResult(email, true, breaches)
            } else if (response.code == 404) {
                LeakCheckResult(email, false, emptyList())
            } else {
                LeakCheckResult(email, false, emptyList(), "API Error: ${response.code}")
            }
        } catch (e: Exception) {
            LeakCheckResult(email, false, emptyList(), e.message)
        }
    }

    private fun sendNotification(title: String, message: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "leak_check_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Security - Leak Check", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        fun schedule(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<LeakCheckWorker>(7, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "LeakCheckWork",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
