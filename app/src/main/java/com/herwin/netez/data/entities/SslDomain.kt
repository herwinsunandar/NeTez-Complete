package com.herwin.netez.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ssl_domains")
data class SslDomain(
    @PrimaryKey val domain: String,
    val groupName: String = "Default",
    val expiryDate: Long,
    val daysLeft: Int,
    val issuer: String,
    val lastChecked: Long,
    val reminderDays: String = "1,3,7,14,30",
    val notifiedDays: String = ""
)
