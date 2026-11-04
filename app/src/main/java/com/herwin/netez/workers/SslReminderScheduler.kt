package com.herwin.netez.workers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.herwin.netez.data.SecurityPreferences
import java.util.Calendar

object SslReminderScheduler {

    private const val TAG = "SslReminderScheduler"
    private const val ALARM_REQUEST_CODE = 1001

    fun scheduleExactAlarm(context: Context) {
        val securityPrefs = SecurityPreferences(context)
        if (!securityPrefs.isSslReminderEnabled) {
            cancelAlarm(context)
            return
        }

        val timeParts = securityPrefs.sslCheckTime.split(":")
        val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 8
        val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val now = System.currentTimeMillis()
        if (calendar.timeInMillis <= now) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, SslReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Exact SSL Reminder Alarm scheduled for ${calendar.time} (Trigger at $hour:$minute)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule exact alarm, falling back to setAndAllowWhileIdle", e)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } catch (e2: Exception) {
                Log.e(TAG, "Fallback alarm failed", e2)
            }
        }
    }

    fun cancelAlarm(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, SslReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "Exact SSL Reminder Alarm cancelled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel alarm", e)
        }
    }
}
