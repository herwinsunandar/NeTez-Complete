package com.herwin.netez.device

import android.content.Context
import android.os.Build
import com.herwin.netez.data.AppDatabase
import com.herwin.netez.data.entities.DeviceSecurityHistory
import com.herwin.netez.device.checks.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DeviceSecurityAuditEngine {

    suspend fun runAudit(context: Context): DeviceSecurityAuditResult = withContext(Dispatchers.IO) {
        val checkers = listOf(
            RootCheck(),
            ScreenLockCheck(),
            SecurityPatchCheck(),
            DeveloperOptionsCheck(),
            UsbDebuggingCheck(),
            EncryptionCheck(),
            AndroidVersionCheck(),
            StorageCheck(),
            PlayProtectCheck(),
            HookingCheck(),
            AccessibilityCheck(),
            UnknownAppsCheck(),
            VerifiedBootCheck(),
            BiometricCheck()
        )

        val checkResults = checkers.map { it.check(context) }

        // Calculate score
        val categoryScores = mutableMapOf<String, Int>()
        val categoryMaxScores = mutableMapOf<String, Int>()

        checkResults.forEach {
            categoryScores[it.category] = (categoryScores[it.category] ?: 0) + it.score
            categoryMaxScores[it.category] = (categoryMaxScores[it.category] ?: 0) + it.maxScore
        }

        // Weighted categories (as per spec)
        val weights = mapOf(
            "Device Integrity" to 30,
            "Authentication & Lock" to 20,
            "Patch & Update Status" to 15,
            "Application & Debug Security" to 15,
            "Encryption & Data Protection" to 10,
            "Device Recovery & Tracking" to 5,
            "Device Hygiene" to 5
        )

        var finalScore = 0.0
        weights.forEach { (category, weight) ->
            val score = categoryScores[category] ?: 0
            val max = categoryMaxScores[category] ?: 0
            if (max > 0) {
                finalScore += (score.toDouble() / max.toDouble()) * weight
            } else {
                finalScore += weight.toDouble()
            }
        }

        // Score Caps
        if (checkResults.any { it.id == "root" && it.status == DeviceCheckStatus.CRITICAL }) {
            finalScore = finalScore.coerceAtMost(59.0)
        }
        if (checkResults.any { it.id == "screen_lock" && it.status == DeviceCheckStatus.CRITICAL }) {
            finalScore = finalScore.coerceAtMost(59.0)
        }

        val roundedScore = finalScore.toInt().coerceIn(0, 100)
        val grade = when {
            roundedScore >= 90 -> "EXCELLENT"
            roundedScore >= 75 -> "GOOD"
            roundedScore >= 60 -> "FAIR"
            roundedScore >= 40 -> "WEAK"
            else -> "CRITICAL"
        }

        val result = DeviceSecurityAuditResult(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            androidVersion = Build.VERSION.RELEASE,
            sdkVersion = Build.VERSION.SDK_INT,
            securityPatch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Build.VERSION.SECURITY_PATCH else null,
            checks = checkResults,
            securityScore = roundedScore,
            grade = grade,
            criticalCount = checkResults.count { it.status == DeviceCheckStatus.CRITICAL },
            warningCount = checkResults.count { it.status == DeviceCheckStatus.WARNING },
            safeCount = checkResults.count { it.status == DeviceCheckStatus.SAFE },
            infoCount = checkResults.count { it.status == DeviceCheckStatus.INFO },
            unknownCount = checkResults.count { it.status == DeviceCheckStatus.UNKNOWN },
            scannedAt = System.currentTimeMillis()
        )

        // Save to history
        try {
            val db = AppDatabase.getDatabase(context)
            db.deviceSecurityDao().insertHistory(
                DeviceSecurityHistory(
                    timestamp = result.scannedAt,
                    deviceModel = "${result.manufacturer} ${result.model}",
                    androidVersion = result.androidVersion,
                    securityPatch = result.securityPatch,
                    score = result.securityScore,
                    grade = result.grade,
                    criticalCount = result.criticalCount,
                    warningCount = result.warningCount,
                    safeCount = result.safeCount,
                    infoCount = result.infoCount,
                    unknownCount = result.unknownCount
                )
            )
        } catch (e: Exception) {}

        result
    }
}
