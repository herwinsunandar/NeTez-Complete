package com.herwin.netez.device.checks

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import android.text.TextUtils
import com.herwin.netez.R
import com.herwin.netez.device.DeviceCheckAction
import com.herwin.netez.device.DeviceCheckItem
import com.herwin.netez.device.DeviceCheckStatus

class AccessibilityCheck : DeviceChecker {
    override suspend fun check(context: Context): DeviceCheckItem {
        val startTime = SystemClock.elapsedRealtime()
        
        val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        val hasServices = !TextUtils.isEmpty(enabledServices)
        
        return DeviceCheckItem(
            id = "accessibility",
            category = "Application & Debug Security",
            name = context.getString(R.string.check_accessibility),
            status = if (hasServices) DeviceCheckStatus.WARNING else DeviceCheckStatus.SAFE,
            score = if (hasServices) 3 else 5,
            maxScore = 5,
            detail = if (hasServices) "One or more accessibility services are enabled." else "No accessibility services are currently enabled.",
            recommendation = "Disable accessibility services that you do not recognize or require as they can observe UI interactions.",
            evidence = enabledServices,
            latencyMs = SystemClock.elapsedRealtime() - startTime,
            action = DeviceCheckAction.OPEN_ACCESSIBILITY_SETTINGS
        )
    }
}
