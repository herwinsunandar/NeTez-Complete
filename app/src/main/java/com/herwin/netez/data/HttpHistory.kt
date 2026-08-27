package com.herwin.netez.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "http_history")
data class HttpHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val method: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis(),
    val requestHeaders: String? = null,
    val requestBody: String? = null,
    val responseCode: Int? = null,
    val responseTime: Long? = null
)
