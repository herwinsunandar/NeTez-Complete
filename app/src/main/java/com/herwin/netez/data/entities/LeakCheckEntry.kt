package com.herwin.netez.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "leak_checks")
data class LeakCheckEntry(
    @PrimaryKey val email: String,
    val breachesJson: String,
    val lastChecked: Long,
    val isBreached: Boolean,
    val breachCount: Int,
    val name: String? = null,
    val dept: String? = null,
    val batchId: String? = null
)
