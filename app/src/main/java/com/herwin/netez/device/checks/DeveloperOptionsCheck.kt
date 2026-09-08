package com.herwin.netez.device.checks

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import com.herwin.netez.R
import com.herwin.netez.device.DeviceCheckAction
import com.herwin.netez.device.DeviceCheckItem
import com.herwin.netez.device.DeviceCheckStatus

class DeveloperOptionsCheck : DeviceChecker {
    override suspend fun check(context: Context): DeviceCheckItem {
        val startTime = SystemClock.elapsedRealtime()
        val devOptions = Settings.Global.getInt(context.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) != 0
        
        return DeviceCheckItem(
            id = "developer_options",
            category = "Application & Debug Security",
            name = context.getString(R.string.check_developer_options),
            status = if (devOptions) DeviceCheckStatus.WARNING else DeviceCheckStatus.SAFE,
            score = if (devOptions) 3 else 5,
            maxScore = 5,
            detail = if (devOptions) "Developer Options are enabled." else "Developer Options are disabled.",
            recommendation = if (devOptions) "Disable Developer Options if they are not required." else "Normal configuration.",
            evidence = null,
            latencyMs = SystemClock.elapsedRealtime() - startTime,
            action = if (devOptions) DeviceCheckAction.OPEN_DEVELOPER_SETTINGS else null
        )
    }
}
