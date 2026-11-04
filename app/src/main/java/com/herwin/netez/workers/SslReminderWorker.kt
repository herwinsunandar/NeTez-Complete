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
import com.herwin.netez.engine.SslTlsEngine
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

        val domains = sslDao.getAllDomainsList()
        if (domains.isEmpty()) return Result.success()

        domains.forEach { domain ->
            try {
                val res = SslTlsEngine.checkDomain(domain.domain)

                val isResultValid = res.certificate.valid &&
                        !res.certificate.issuer.startsWith("Error", ignoreCase = true) &&
                        res.certificate.expiresInDays >= 0 &&
                        res.certificate.expiryDate.time > 0L

                val updatedDomain = if (isResultValid) {
                    val vulnCount = listOf(res.vulnerabilities.poodle, res.vulnerabilities.beast, res.vulnerabilities.robot).count { it }
                    domain.copy(
                        expiryDate = res.certificate.expiryDate.time,
                        daysLeft = res.certificate.expiresInDays,
                        issuer = res.certificate.issuer.substringBefore(','),
                        tlsVersion = if (res.tlsVersions.tls13) "TLS 1.3" else if (res.tlsVersions.tls12) "TLS 1.2" else "TLS 1.0",
                        grade = res.grade.letter,
                        score = res.grade.score,
                        cipher = res.cipherSuites.firstOrNull()?.name ?: domain.cipher,
                        vulnCount = vulnCount,
                        chainValid = res.chain.complete,
                        lastChecked = System.currentTimeMillis(),
                        lastVulnCheck = System.currentTimeMillis()
                    )
                } else {
                    // Protect existing valid domain record from background network timeouts/glitches
                    domain.copy(lastChecked = System.currentTimeMillis())
                }

                sslDao.insertDomain(updatedDomain)

                if (isResultValid) {
                    checkAndNotify(updatedDomain, securityPrefs)
                }
            } catch (e: Exception) {
                // If background check failed, keep existing valid domain record
            }
        }

        return Result.success()
    }

    private fun checkAndNotify(domain: SslDomain, securityPrefs: SecurityPreferences) {
        val configDays = securityPrefs.defaultReminderDays.split(',').mapNotNull { it.trim().toIntOrNull() }
        val domainDays = domain.reminderDays.split(',').mapNotNull { it.trim().toIntOrNull() }

        val reminderDays = if (configDays.isNotEmpty()) configDays else domainDays
        val currentDaysLeft = domain.realDaysLeft

        if (currentDaysLeft <= 0) {
            sendNotification(
                id = domain.domain.hashCode(),
                title = "🔴 SSL Expired: ${domain.domain}",
                message = "Certificate for ${domain.domain} has expired! Please renew immediately."
            )
            return
        }

        if (domain.grade == "F") {
            sendNotification(
                id = domain.domain.hashCode() + 999,
                title = "🚨 SSL/TLS Grade F: ${domain.domain}",
                message = "Critical vulnerability or SSL configuration issue detected."
            )
        }

        // Active Threshold check: If current days left falls within configured threshold (e.g. <= 14 days)
        val activeThreshold = reminderDays.filter { currentDaysLeft <= it }.minOrNull()
        if (activeThreshold != null) {
            val title = if (currentDaysLeft <= 7) {
                "⚠️ Urgent SSL Renewal: ${domain.domain}"
            } else {
                "⚠️ Pending SSL Renewal: ${domain.domain}"
            }
            val msg = "Expires in $currentDaysLeft days (Threshold <= $activeThreshold days). Please renew certificate."

            sendNotification(
                id = domain.domain.hashCode() + currentDaysLeft,
                title = title,
                message = msg
            )
        }
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
        fun schedule(context: Context, forceReschedule: Boolean = false) {
            SslReminderScheduler.scheduleExactAlarm(context)

            val policy = if (forceReschedule) {
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
            } else {
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
            }

            val workRequest = PeriodicWorkRequestBuilder<SslReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(calculateInitialDelay(context), TimeUnit.MILLISECONDS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "SslReminderWork",
                policy,
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
