package com.herwin.netez.device

data class DeviceSecurityAuditResult(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val sdkVersion: Int,
    val securityPatch: String?,
    val checks: List<DeviceCheckItem>,
    val securityScore: Int,
    val grade: String,
    val criticalCount: Int,
    val warningCount: Int,
    val safeCount: Int,
    val infoCount: Int,
    val unknownCount: Int,
    val scannedAt: Long
)
