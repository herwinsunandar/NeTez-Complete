package com.herwin.netez.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "test_results")
data class TestResult(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String,
    val target: String,
    val result: String,
    val timestamp: Long = System.currentTimeMillis()
)
