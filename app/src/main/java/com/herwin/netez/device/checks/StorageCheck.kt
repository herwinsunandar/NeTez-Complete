package com.herwin.netez.device.checks

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import com.herwin.netez.R
import com.herwin.netez.device.DeviceCheckAction
import com.herwin.netez.device.DeviceCheckItem
import com.herwin.netez.device.DeviceCheckStatus
import java.util.*

class StorageCheck : DeviceChecker {
    override suspend fun check(context: Context): DeviceCheckItem {
        val startTime = SystemClock.elapsedRealtime()
        
        val stat = StatFs(Environment.getDataDirectory().path)
        val bytesAvailable = stat.blockSizeLong * stat.availableBlocksLong
        val bytesTotal = stat.blockSizeLong * stat.blockCountLong
        
        val freePercentage = (bytesAvailable.toDouble() / bytesTotal.toDouble()) * 100.0
        val freeGb = bytesAvailable / (1024 * 1024 * 1024)
        
        val (status, score) = when {
            freePercentage > 10.0 -> DeviceCheckStatus.SAFE to 5
            freePercentage > 5.0 -> DeviceCheckStatus.WARNING to 3
            else -> DeviceCheckStatus.WARNING to 1
        }

        return DeviceCheckItem(
            id = "storage",
            category = "Device Hygiene",
            name = context.getString(R.string.check_storage),
            status = status,
            score = score,
            maxScore = 5,
            detail = String.format(Locale.US, "%.1f GB (%.1f%%) free storage remains.", freeGb.toDouble(), freePercentage),
            recommendation = if (status == DeviceCheckStatus.SAFE) "Storage levels are healthy." else "Free storage space to maintain normal device operation.",
            evidence = null,
            latencyMs = SystemClock.elapsedRealtime() - startTime,
            action = DeviceCheckAction.OPEN_STORAGE_SETTINGS
        )
    }
}
