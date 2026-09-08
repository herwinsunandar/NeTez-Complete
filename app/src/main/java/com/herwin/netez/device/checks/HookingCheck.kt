package com.herwin.netez.device.checks

import android.content.Context
import android.os.SystemClock
import com.herwin.netez.R
import com.herwin.netez.device.DeviceCheckItem
import com.herwin.netez.device.DeviceCheckStatus
import java.io.BufferedReader
import java.io.FileReader

class HookingCheck : DeviceChecker {
    override suspend fun check(context: Context): DeviceCheckItem {
        val startTime = SystemClock.elapsedRealtime()
        val indicators = mutableListOf<String>()
        
        // 1. Check maps for common hooking libs
        try {
            val reader = BufferedReader(FileReader("/proc/self/maps"))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line!!.contains("frida") || line!!.contains("xposed") || line!!.contains("substrate")) {
                    indicators.add("Library: ${line!!.substringAfterLast("/")}")
                    break
                }
            }
            reader.close()
        } catch (e: Exception) {}

        // 2. Check for stack trace elements
        try {
            throw Exception("DUMMY")
        } catch (e: Exception) {
            for (stackTraceElement in e.stackTrace) {
                if (stackTraceElement.className.contains("com.saurik.substrate") || 
                    stackTraceElement.className.contains("de.robv.android.xposed")) {
                    indicators.add("Stack: ${stackTraceElement.className}")
                    break
                }
            }
        }

        val detected = indicators.isNotEmpty()
        
        return DeviceCheckItem(
            id = "hooking",
            category = "Device Integrity",
            name = context.getString(R.string.check_hooking),
            status = if (detected) DeviceCheckStatus.CRITICAL else DeviceCheckStatus.SAFE,
            score = if (detected) 0 else 10,
            maxScore = 10,
            detail = if (detected) "Potential runtime instrumentation framework detected." else "No active hooking frameworks detected.",
            recommendation = if (detected) "Review installed security and development tools. Run the application on a trusted production device if runtime integrity is required." else "Runtime environment appears secure.",
            evidence = if (indicators.isNotEmpty()) indicators.joinToString(", ") else null,
            latencyMs = SystemClock.elapsedRealtime() - startTime,
            action = null
        )
    }
}
