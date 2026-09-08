package com.herwin.netez.device.checks

import android.app.KeyguardManager
import android.content.Context
import android.os.SystemClock
import com.herwin.netez.R
import com.herwin.netez.device.DeviceCheckAction
import com.herwin.netez.device.DeviceCheckItem
import com.herwin.netez.device.DeviceCheckStatus

class ScreenLockCheck : DeviceChecker {
    override suspend fun check(context: Context): DeviceCheckItem {
        val startTime = SystemClock.elapsedRealtime()
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        
        val isSecure = km.isDeviceSecure
        
        return DeviceCheckItem(
            id = "screen_lock",
            category = "Authentication & Lock",
            name = context.getString(R.string.check_screen_lock),
            status = if (isSecure) DeviceCheckStatus.SAFE else DeviceCheckStatus.CRITICAL,
            score = if (isSecure) 20 else 0,
            maxScore = 20,
            detail = if (isSecure) "Screen lock is enabled." else "No secure screen lock is configured.",
            recommendation = if (isSecure) "Maintain a strong screen lock." else "Enable a PIN, password, pattern, or supported biometric protection.",
            evidence = null,
            latencyMs = SystemClock.elapsedRealtime() - startTime,
            action = if (isSecure) null else DeviceCheckAction.OPEN_SECURITY_SETTINGS
        )
    }
}
