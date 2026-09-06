package com.herwin.netez.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class StabilityMode {
    LOCAL_LOAD,
    BUFFERBLOAT
}

@Entity(tableName = "stability_results")
data class StabilityResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val mode: StabilityMode,
    val target: String,
    val customUrl: String? = null,
    val threads: Int,
    val duration: Int,
    val avgLatency: Double,
    val maxLatency: Double,
    val idleLatency: Double = 0.0,
    val loadedLatency: Double = 0.0,
    val bufferbloat: Double = 0.0,
    val packetLoss: Int,
    val successRate: Double
)
