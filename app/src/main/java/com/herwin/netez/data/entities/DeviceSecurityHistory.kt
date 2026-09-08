package com.herwin.netez.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "device_security_history")
data class DeviceSecurityHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val deviceModel: String,
    val androidVersion: String,
    val securityPatch: String?,
    val score: Int,
    val grade: String,
    val criticalCount: Int,
    val warningCount: Int,
    val safeCount: Int,
    val infoCount: Int,
    val unknownCount: Int
)
