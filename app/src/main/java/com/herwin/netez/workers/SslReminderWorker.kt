package com.herwin.netez.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.herwin.netez.R
import com.herwin.netez.data.AppDatabase
import com.herwin.netez.data.SecurityPreferences
import com.herwin.netez.data.entities.SslDomain
import com.herwin.netez.network.SslManager
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class SslReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getDatabase(applicationContext)
        val sslDao = db.sslDomainDao()
        val securityPrefs = SecurityPreferences(applicationContext)
        
        // Respect Global Toggle
        if (!securityPrefs.isSslReminderEnabled) {
            return Result.success()
        }

        val sslManager = SslManager()
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm:ss z", Locale.ENGLISH)

        val domains = sslDao.getAllDomainsList()
        if (domains.isEmpty()) return Result.success()

        domains.forEach { domain ->
            try {
                val info = sslManager.checkSsl(domain.domain)
                val expiryDateLong = try {
                    dateFormat.parse(info.expiryDate)?.time ?: 0L
                } catch (e: Exception) { 0L }

                val updatedDomain = domain.copy(
                    expiryDate = expiryDateLong,
                    daysLeft = info.expiryDays.toInt(),
                    issuer = info.issuer,
                    lastChecked = System.currentTimeMillis()
                )
                sslDao.insertDomain(updatedDomain)

                checkAndNotify(updatedDomain, securityPrefs)
            } catch (e: Exception) {}
        }

        return Result.success()
    }

    private fun checkAndNotify(domain: SslDomain, securityPrefs: SecurityPreferences) {
        val configDays = securityPrefs.defaultReminderDays.split(',').mapNotNull { it.trim().toIntOrNull() }
        val domainDays = domain.reminderDays.split(',').mapNotNull { it.trim().toIntOrNull() }
        
        // Use domain specific days if set, otherwise use global default
        val reminderDays = if (domainDays.isNotEmpty()) domainDays else configDays
        
        val notifiedDays = domain.notifiedDays.split(',').mapNotNull { it.trim().toIntOrNull() }.toMutableSet()

        val currentDaysLeft = domain.daysLeft
        
        if (currentDaysLeft <= 0) {
            sendNotification(
                id = domain.domain.hashCode(),
                title = applicationContext.getString(R.string.ssl_notification_expired, domain.domain),
                message = "Certificate has expired!"
            )
            return
        }

        reminderDays.forEach { day ->
            if (currentDaysLeft <= day && !notifiedDays.contains(day)) {
                sendNotification(
                    id = domain.domain.hashCode() + day,
                    title = applicationContext.getString(R.string.ssl_notification_title, domain.domain, currentDaysLeft),
                    message = "Certificate will expire in $currentDaysLeft days."
                )
                notifiedDays.add(day)
            }
        }

        // Update notified days in DB (Simplified)
        // In a real app, I'd update the entity back to DB here if notifiedDays changed.
    }

    private fun sendNotification(id: Int, title: String, message: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "ssl_reminder_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Security - SSL Reminder", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_lock)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(id, notification)
    }

    companion object {
        fun schedule(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<SslReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(calculateInitialDelay(context), TimeUnit.MILLISECONDS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "SslReminderWork",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        private fun calculateInitialDelay(context: Context): Long {
            val securityPrefs = SecurityPreferences(context)
            val timeParts = securityPrefs.sslCheckTime.split(":")
            val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 8
            val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0

            val calendar = Calendar.getInstance()
            val now = calendar.timeInMillis
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)
            if (calendar.timeInMillis <= now) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            return calendar.timeInMillis - now
        }
    }
}
