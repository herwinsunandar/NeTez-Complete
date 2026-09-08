package com.herwin.netez.device.checks

import android.content.Context
import android.os.Build
import android.os.SystemClock
import com.herwin.netez.R
import com.herwin.netez.device.DeviceCheckItem
import com.herwin.netez.device.DeviceCheckStatus

class AndroidVersionCheck : DeviceChecker {
    override suspend fun check(context: Context): DeviceCheckItem {
        val startTime = SystemClock.elapsedRealtime()
        val version = Build.VERSION.RELEASE
        val sdk = Build.VERSION.SDK_INT
        
        val isOutdated = sdk < Build.VERSION_CODES.Q // Android 10 is minimum for "SAFE" here as a simple heuristic
        
        return DeviceCheckItem(
            id = "android_version",
            category = "Patch & Update Status",
            name = context.getString(R.string.check_android_version),
            status = if (isOutdated) DeviceCheckStatus.WARNING else DeviceCheckStatus.SAFE,
            score = if (isOutdated) 2 else 5,
            maxScore = 5,
            detail = "Android $version (API $sdk)",
            recommendation = if (isOutdated) "Your Android version may no longer receive regular security updates." else "Your OS version is currently supported.",
            evidence = null,
            latencyMs = SystemClock.elapsedRealtime() - startTime,
            action = null
        )
    }
}
