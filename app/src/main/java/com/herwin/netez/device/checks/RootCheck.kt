package com.herwin.netez.device.checks

import android.content.Context
import android.os.Build
import android.os.SystemClock
import com.herwin.netez.R
import com.herwin.netez.device.DeviceCheckItem
import com.herwin.netez.device.DeviceCheckStatus
import java.io.File

class RootCheck : DeviceChecker {
    override suspend fun check(context: Context): DeviceCheckItem {
        val startTime = SystemClock.elapsedRealtime()
        
        val rootIndicators = mutableListOf<String>()
        
        // 1. Check su binary paths
        val paths = arrayOf(
            "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su",
            "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
            "/system/bin/failsafe/su", "/data/local/su", "/su/bin/su"
        )
        for (path in paths) {
            if (File(path).exists()) {
                rootIndicators.add("Binary: $path")
            }
        }

        // 2. Check test-keys
        val buildTags = Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) {
            rootIndicators.add("Build Tags: test-keys")
        }

        // 3. Check for root management apps
        val rootPackages = arrayOf(
            "com.noshufou.android.su", "com.thirdparty.superuser", "eu.chainfire.supersu",
            "com.koushikdutta.superuser", "com.zacharee1.systemuituner", "com.topjohnwu.magisk"
        )
        val pm = context.packageManager
        for (pkg in rootPackages) {
            try {
                pm.getPackageInfo(pkg, 0)
                rootIndicators.add("Package: $pkg")
            } catch (e: Exception) { }
        }

        val isRooted = rootIndicators.isNotEmpty()
        val status = if (isRooted) DeviceCheckStatus.CRITICAL else DeviceCheckStatus.SAFE
        
        return DeviceCheckItem(
            id = "root",
            category = "Device Integrity",
            name = context.getString(R.string.check_root),
            status = status,
            score = if (isRooted) 0 else 30,
            maxScore = 30,
            detail = if (isRooted) "Root access indicators detected on this device." else "Root access not detected.",
            recommendation = if (isRooted) "Review root management software and restore the device to an official system image if a non-rooted environment is required." else "Device integrity appears normal.",
            evidence = if (rootIndicators.isNotEmpty()) rootIndicators.joinToString(", ") else null,
            latencyMs = SystemClock.elapsedRealtime() - startTime,
            action = null
        )
    }
}
