package com.herwin.netez.device.checks

import android.content.Context
import android.os.Build
import android.os.SystemClock
import com.herwin.netez.R
import com.herwin.netez.device.DeviceCheckAction
import com.herwin.netez.device.DeviceCheckItem
import com.herwin.netez.device.DeviceCheckStatus
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class SecurityPatchCheck : DeviceChecker {
    override suspend fun check(context: Context): DeviceCheckItem {
        val startTime = SystemClock.elapsedRealtime()
        val patchDateStr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Build.VERSION.SECURITY_PATCH
        } else {
            null
        }

        if (patchDateStr.isNullOrEmpty()) {
            return DeviceCheckItem(
                id = "security_patch",
                category = "Patch & Update Status",
                name = context.getString(R.string.check_patch),
                status = DeviceCheckStatus.UNKNOWN,
                score = 15, // Neutral
                maxScore = 15,
                detail = "Security patch information unavailable.",
                recommendation = "Check for system updates manually.",
                evidence = null,
                latencyMs = SystemClock.elapsedRealtime() - startTime,
                action = DeviceCheckAction.OPEN_SYSTEM_UPDATE_SETTINGS
            )
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val patchDate = try { sdf.parse(patchDateStr) } catch (e: Exception) { null }
        
        if (patchDate == null) {
            return DeviceCheckItem(
                id = "security_patch",
                category = "Patch & Update Status",
                name = context.getString(R.string.check_patch),
                status = DeviceCheckStatus.UNKNOWN,
                score = 15,
                maxScore = 15,
                detail = "Security patch date: $patchDateStr (Format unrecognized)",
                recommendation = "Check for system updates manually.",
                evidence = null,
                latencyMs = SystemClock.elapsedRealtime() - startTime,
                action = DeviceCheckAction.OPEN_SYSTEM_UPDATE_SETTINGS
            )
        }

        val diff = System.currentTimeMillis() - patchDate.time
        val daysOld = TimeUnit.MILLISECONDS.toDays(diff)
        
        val (status, score) = when {
            daysOld <= 90 -> DeviceCheckStatus.SAFE to 15
            daysOld <= 180 -> DeviceCheckStatus.WARNING to 10
            else -> DeviceCheckStatus.CRITICAL to 5
        }

        return DeviceCheckItem(
            id = "security_patch",
            category = "Patch & Update Status",
            name = context.getString(R.string.check_patch),
            status = status,
            score = score,
            maxScore = 15,
            detail = "Security patch: $patchDateStr ($daysOld days old)",
            recommendation = if (status == DeviceCheckStatus.SAFE) "System is up to date." else "Check for system updates to receive the latest security improvements.",
            evidence = null,
            latencyMs = SystemClock.elapsedRealtime() - startTime,
            action = DeviceCheckAction.OPEN_SYSTEM_UPDATE_SETTINGS
        )
    }
}
