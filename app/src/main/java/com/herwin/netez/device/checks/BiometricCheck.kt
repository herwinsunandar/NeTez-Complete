package com.herwin.netez.device.checks

import android.content.Context
import android.os.SystemClock
import androidx.biometric.BiometricManager
import com.herwin.netez.R
import com.herwin.netez.device.DeviceCheckAction
import com.herwin.netez.device.DeviceCheckItem
import com.herwin.netez.device.DeviceCheckStatus

class BiometricCheck : DeviceChecker {
    override suspend fun check(context: Context): DeviceCheckItem {
        val startTime = SystemClock.elapsedRealtime()
        val biometricManager = BiometricManager.from(context)
        
        val canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        
        val (status, detail) = when (canAuthenticate) {
            BiometricManager.BIOMETRIC_SUCCESS -> DeviceCheckStatus.SAFE to "Biometric authentication is available and configured."
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> DeviceCheckStatus.INFO to "Biometric hardware is available but no fingerprints/faces are enrolled."
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> DeviceCheckStatus.INFO to "No biometric hardware detected on this device."
            else -> DeviceCheckStatus.UNKNOWN to "Biometric authentication status could not be determined."
        }

        return DeviceCheckItem(
            id = "biometric",
            category = "Authentication & Lock",
            name = context.getString(R.string.check_biometric),
            status = status,
            score = if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) 5 else 5, // Not a penalty if not enrolled/available
            maxScore = 5,
            detail = detail,
            recommendation = if (canAuthenticate == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) "Consider enrolling biometrics for easier and secure access." else "Biometric security provides an additional layer of protection.",
            evidence = "Result Code: $canAuthenticate",
            latencyMs = SystemClock.elapsedRealtime() - startTime,
            action = if (canAuthenticate == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) DeviceCheckAction.OPEN_BIOMETRIC_SETTINGS else null
        )
    }
}
