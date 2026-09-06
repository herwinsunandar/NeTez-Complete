package com.herwin.netez.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MonitoringType {
    PING, PORT, HTTP, DNS
}

@Entity(tableName = "monitoring_targets")
data class MonitoringTarget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: MonitoringType,
    val host: String? = null,
    val port: Int? = null,
    val url: String? = null,
    val dnsServer: String? = null,
    val keyword: String? = null,
    val intervalMillis: Long = 60000,
    val failThreshold: Int = 3,
    val isActive: Boolean = true,
    val lastStatus: String? = null,
    val lastLatency: Long? = null,
    val lastCheckTimestamp: Long? = null,
    val failCount: Int = 0
)
