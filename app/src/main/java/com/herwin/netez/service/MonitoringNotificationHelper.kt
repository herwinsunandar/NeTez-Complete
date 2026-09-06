package com.herwin.netez.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.herwin.netez.MainActivity
import com.herwin.netez.R
import com.herwin.netez.data.MonitoringTarget
import com.herwin.netez.data.MonitoringType

object MonitoringNotificationHelper {
    private const val CHANNEL_ID = "monitoring_alerts"
    private const val SERVICE_CHANNEL_ID = "monitoring_service"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Monitoring Alerts"
            val descriptionText = "Alerts when a target goes DOWN or UP"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            
            val serviceName = "Monitoring Service"
            val serviceChannel = NotificationChannel(SERVICE_CHANNEL_ID, serviceName, NotificationManager.IMPORTANCE_LOW)

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }

    fun showDownAlert(context: Context, target: MonitoringTarget, message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Target DOWN: ${getTargetName(target)}")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(target.id.toInt(), builder.build())
    }

    fun showUpAlert(context: Context, target: MonitoringTarget) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Target UP: ${getTargetName(target)}")
            .setContentText("Status restored to UP")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(target.id.toInt(), builder.build())
    }

    fun getServiceNotification(context: Context) = NotificationCompat.Builder(context, SERVICE_CHANNEL_ID)
        .setContentTitle("NeTez Monitoring")
        .setContentText("Background monitoring is active")
        .setSmallIcon(android.R.drawable.stat_notify_sync)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()

    private fun getTargetName(target: MonitoringTarget): String {
        return when (target.type) {
            MonitoringType.PING -> target.host ?: ""
            MonitoringType.PORT -> "${target.host}:${target.port}"
            MonitoringType.HTTP -> target.url ?: ""
            MonitoringType.DNS -> "DNS: ${target.host}"
        }
    }
}
