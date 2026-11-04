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
    val notifiedDays: String = "",
    val tlsVersion: String = "TLS 1.3",
    val grade: String = "A+",
    val score: Int = 95,
    val cipher: String = "",
    val vulnCount: Int = 0,
    val chainValid: Boolean = true,
    val lastVulnCheck: Long = 0L
) {
    val realDaysLeft: Int
        get() {
            if (expiryDate <= 0L) return daysLeft
            val diff = expiryDate - System.currentTimeMillis()
            return (diff / (1000L * 60 * 60 * 24)).toInt()
        }

    val isExpired: Boolean
        get() = realDaysLeft <= 0

    val isExpiringSoon: Boolean
        get() = realDaysLeft in 1..14
}
