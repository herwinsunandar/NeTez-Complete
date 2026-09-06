package com.herwin.netez.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monitoring_logs")
data class MonitoringLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val targetId: Long,
    val timestamp: Long,
    val status: String,
    val latency: Long,
    val message: String? = null
)
