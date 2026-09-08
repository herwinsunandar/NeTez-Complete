package com.herwin.netez.device.checks

import android.content.Context
import android.os.SystemClock
import com.herwin.netez.R
import com.herwin.netez.device.DeviceCheckItem
import com.herwin.netez.device.DeviceCheckStatus

class PlayProtectCheck : DeviceChecker {
    override suspend fun check(context: Context): DeviceCheckItem {
        val startTime = SystemClock.elapsedRealtime()
        
        // Google Play Store package
        val playStorePkg = "com.android.vending"
        val pm = context.packageManager
        
        var isPlayStoreInstalled = false
        try {
            pm.getPackageInfo(playStorePkg, 0)
            isPlayStoreInstalled = true
        } catch (e: Exception) {}

        // Note: Real Play Protect status requires Google Play Services APIs (SafetyNet/Integrity)
        // For a non-invasive check, we check if Play Store is present as a proxy for "Certification" 
        // but as per spec we should be careful.
        
        val status = if (isPlayStoreInstalled) DeviceCheckStatus.SAFE else DeviceCheckStatus.UNKNOWN
        
        return DeviceCheckItem(
            id = "play_protect",
            category = "Application & Debug Security",
            name = context.getString(R.string.check_play_protect),
            status = status,
            score = if (isPlayStoreInstalled) 5 else 0,
            maxScore = 5,
            detail = if (isPlayStoreInstalled) "Google Play Store is installed and available." else "Google Play Services or Play Store could not be detected.",
            recommendation = if (isPlayStoreInstalled) "Ensure Google Play Protect is enabled in the Play Store settings." else "Verify device certification status in Play Store settings.",
            evidence = if (isPlayStoreInstalled) "Package com.android.vending found" else "Package not found",
            latencyMs = SystemClock.elapsedRealtime() - startTime,
            action = null
        )
    }
}
