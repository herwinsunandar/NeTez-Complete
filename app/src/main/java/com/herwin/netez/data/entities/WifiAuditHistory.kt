package com.herwin.netez.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wifi_audit_history")
data class WifiAuditHistory(
    @PrimaryKey val bssid: String,
    val ssid: String?,
    val timestamp: Long,
    val score: Int,
    val grade: String,
    val criticalCount: Int,
    val warningCount: Int,
    val safeCount: Int
)
