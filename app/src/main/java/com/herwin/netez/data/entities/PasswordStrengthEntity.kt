package com.herwin.netez.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "password_strength_history")
data class PasswordStrengthHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val score: Int,
    val length: Int,
    val crackTime: String,
    val timestamp: Long = System.currentTimeMillis()
)
