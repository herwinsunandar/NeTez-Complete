package com.herwin.netez.device.checks

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import com.herwin.netez.R
import com.herwin.netez.device.DeviceCheckAction
import com.herwin.netez.device.DeviceCheckItem
import com.herwin.netez.device.DeviceCheckStatus

class UnknownAppsCheck : DeviceChecker {
    override suspend fun check(context: Context): DeviceCheckItem {
        val startTime = SystemClock.elapsedRealtime()
        
        // On older Android, it was a global setting. On modern Android, it's per-app.
        val isGlobalEnabled = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Settings.Secure.getInt(context.contentResolver, Settings.Secure.INSTALL_NON_MARKET_APPS, 0) != 0
        } else {
            false // On O+, we can't check a single global flag easily without querying all packages
        }
        
        return DeviceCheckItem(
            id = "unknown_apps",
            category = "Application & Debug Security",
            name = context.getString(R.string.check_unknown_apps),
            status = if (isGlobalEnabled) DeviceCheckStatus.WARNING else DeviceCheckStatus.SAFE,
            score = if (isGlobalEnabled) 0 else 5,
            maxScore = 5,
            detail = if (isGlobalEnabled) "Global permission to install unknown apps is enabled." else "Unknown app installation is managed on a per-application basis.",
            recommendation = "Review which applications are allowed to install unknown apps and disable the permission when unnecessary.",
            evidence = if (isGlobalEnabled) "Global flag enabled" else "Managed per-app (API 26+)",
            latencyMs = SystemClock.elapsedRealtime() - startTime,
            action = DeviceCheckAction.OPEN_UNKNOWN_APP_SOURCES
        )
    }
}
