package com.herwin.netez.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "security_tools_history")
data class SecurityToolHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val toolType: String,
    val input: String,
    val output: String,
    val timestamp: Long = System.currentTimeMillis()
)
