package com.herwin.netez.device.checks

import android.content.Context
import android.os.SystemClock
import com.herwin.netez.R
import com.herwin.netez.device.DeviceCheckItem
import com.herwin.netez.device.DeviceCheckStatus
import java.io.BufferedReader
import java.io.InputStreamReader

class VerifiedBootCheck : DeviceChecker {
    override suspend fun check(context: Context): DeviceCheckItem {
        val startTime = SystemClock.elapsedRealtime()
        
        // Check system properties for bootloader status
        var state = "unknown"
        try {
            val process = Runtime.getRuntime().exec("getprop ro.boot.verifiedbootstate")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            state = reader.readLine() ?: "unknown"
            reader.close()
        } catch (e: Exception) {}

        val status = when (state) {
            "green" -> DeviceCheckStatus.SAFE
            "yellow", "orange" -> DeviceCheckStatus.WARNING
            "red" -> DeviceCheckStatus.CRITICAL
            else -> DeviceCheckStatus.UNKNOWN
        }

        return DeviceCheckItem(
            id = "verified_boot",
            category = "Device Integrity",
            name = context.getString(R.string.check_verified_boot),
            status = status,
            score = when(status) {
                DeviceCheckStatus.SAFE -> 10
                DeviceCheckStatus.WARNING -> 5
                DeviceCheckStatus.CRITICAL -> 0
                else -> 10
            },
            maxScore = 10,
            detail = "Verified boot state: $state",
            recommendation = if (status == DeviceCheckStatus.SAFE) "Bootloader is locked and verified." else "A non-standard bootloader state may indicate a compromised system integrity.",
            evidence = "ro.boot.verifiedbootstate: $state",
            latencyMs = SystemClock.elapsedRealtime() - startTime,
            action = null
        )
    }
}
