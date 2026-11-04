package com.herwin.netez.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "security_intelligence_history")
data class SecurityIntelligenceEntity(
    @PrimaryKey val target: String,
    val targetType: String,
    val timestamp: Long,
    val overallStatus: String,
    val checksCompleted: Int,
    val warningCount: Int,
    val criticalCount: Int,
    val jsonResult: String
)
