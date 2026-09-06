package com.herwin.netez.device.checks

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import com.herwin.netez.R
import com.herwin.netez.device.DeviceCheckAction
import com.herwin.netez.device.DeviceCheckItem
import com.herwin.netez.device.DeviceCheckStatus

class UsbDebuggingCheck : DeviceChecker {
    override suspend fun check(context: Context): DeviceCheckItem {
        val startTime = SystemClock.elapsedRealtime()
        val adbEnabled = Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) != 0
        
        return DeviceCheckItem(
            id = "usb_debugging",
            category = "Application & Debug Security",
            name = context.getString(R.string.check_usb_debugging),
            status = if (adbEnabled) DeviceCheckStatus.WARNING else DeviceCheckStatus.SAFE,
            score = if (adbEnabled) 0 else 10,
            maxScore = 10,
            detail = if (adbEnabled) "USB debugging is enabled." else "USB debugging is disabled.",
            recommendation = if (adbEnabled) "Disable USB debugging when it is not required." else "Normal configuration.",
            evidence = null,
            latencyMs = SystemClock.elapsedRealtime() - startTime,
            action = if (adbEnabled) DeviceCheckAction.OPEN_DEVELOPER_SETTINGS else null
        )
    }
}
