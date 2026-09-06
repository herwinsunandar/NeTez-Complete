package com.herwin.netez.device.checks

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.os.Build
import android.os.SystemClock
import com.herwin.netez.R
import com.herwin.netez.device.DeviceCheckItem
import com.herwin.netez.device.DeviceCheckStatus

class EncryptionCheck : DeviceChecker {
    override suspend fun check(context: Context): DeviceCheckItem {
        val startTime = SystemClock.elapsedRealtime()
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val status = dpm.storageEncryptionStatus
        
        // ENCRYPTION_STATUS_ACTIVE is 2
        // ENCRYPTION_STATUS_ACTIVE_DEFAULT is 1
        // ENCRYPTION_STATUS_ACTIVE_PER_USER is 5
        val isEncrypted = status >= 1 && status != 4 && status != 3 // Basic heuristic if constants fail
                
        val checkStatus = when {
            isEncrypted -> DeviceCheckStatus.SAFE
            status == 4 -> DeviceCheckStatus.UNKNOWN // ENCRYPTION_STATUS_UNSUPPORTED
            else -> DeviceCheckStatus.WARNING
        }

        return DeviceCheckItem(
            id = "encryption",
            category = "Encryption & Data Protection",
            name = context.getString(R.string.check_encryption),
            status = checkStatus,
            score = if (isEncrypted) 10 else 0,
            maxScore = 10,
            detail = when(status) {
                1 -> "Device storage encryption is enabled (default)."
                2 -> "Device storage encryption is enabled."
                5 -> "Device storage encryption is enabled per user."
                0 -> "Encryption is supported but inactive."
                3 -> "Encryption is currently being activated."
                4 -> "Encryption status cannot be determined on this Android version."
                else -> "Encryption status requires verification."
            },
            recommendation = if (isEncrypted) "Your data is protected by device encryption." else "Enable device encryption if supported by your hardware.",
            evidence = "Status Code: $status",
            latencyMs = SystemClock.elapsedRealtime() - startTime,
            action = null
        )
    }
}
