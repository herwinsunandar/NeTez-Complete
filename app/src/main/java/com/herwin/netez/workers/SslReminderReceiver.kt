package com.herwin.netez.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.herwin.netez.R
import com.herwin.netez.data.AppDatabase
import com.herwin.netez.data.SecurityPreferences
import com.herwin.netez.data.entities.SslDomain
import com.herwin.netez.engine.SslTlsEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SslReminderReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("SslReminderReceiver", "SSL Reminder Exact Alarm Triggered at scheduled time!")

        val pendingResult = goAsync()

        scope.launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val sslDao = db.sslDomainDao()
                val securityPrefs = SecurityPreferences(context)

                if (!securityPrefs.isSslReminderEnabled) {
                    return@launch
                }

                val domains = sslDao.getAllDomainsList()
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
                            checkAndNotify(context, updatedDomain, securityPrefs)
                        }
                    } catch (e: Exception) {
                        Log.e("SslReminderReceiver", "Error checking domain ${domain.domain}", e)
                    }
                }
            } catch (e: Exception) {
                Log.e("SslReminderReceiver", "Error executing SSL Reminder Receiver", e)
            } finally {
                // Auto Reschedule for same time tomorrow
                SslReminderScheduler.scheduleExactAlarm(context)
                pendingResult.finish()
            }
        }
    }

    private fun checkAndNotify(context: Context, domain: SslDomain, securityPrefs: SecurityPreferences) {
        val configDays = securityPrefs.defaultReminderDays.split(',').mapNotNull { it.trim().toIntOrNull() }
        val domainDays = domain.reminderDays.split(',').mapNotNull { it.trim().toIntOrNull() }

        val reminderDays = if (configDays.isNotEmpty()) configDays else domainDays
        val currentDaysLeft = domain.realDaysLeft

        if (currentDaysLeft <= 0) {
            sendNotification(
                context = context,
                id = domain.domain.hashCode(),
                title = "🔴 SSL Expired: ${domain.domain}",
                message = "Certificate for ${domain.domain} has expired! Please renew immediately."
            )
            return
        }

        if (domain.grade == "F") {
            sendNotification(
                context = context,
                id = domain.domain.hashCode() + 999,
                title = "🚨 SSL/TLS Grade F: ${domain.domain}",
                message = "Critical vulnerability or SSL configuration issue detected."
            )
        }

        val activeThreshold = reminderDays.filter { currentDaysLeft <= it }.minOrNull()
        if (activeThreshold != null) {
            val title = if (currentDaysLeft <= 7) {
                "⚠️ Urgent SSL Renewal: ${domain.domain}"
            } else {
                "⚠️ Pending SSL Renewal: ${domain.domain}"
            }
            val msg = "Expires in $currentDaysLeft days (Threshold <= $activeThreshold days). Please renew certificate."

            sendNotification(
                context = context,
                id = domain.domain.hashCode() + currentDaysLeft,
                title = title,
                message = msg
            )
        }
    }

    private fun sendNotification(context: Context, id: Int, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "ssl_reminder_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Security - SSL Reminder", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_lock)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(id, notification)
    }
}
