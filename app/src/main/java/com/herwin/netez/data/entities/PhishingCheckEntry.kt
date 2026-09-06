package com.herwin.netez.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "phishing_checks")
data class PhishingCheckEntry(
    @PrimaryKey val url: String,
    val domain: String,
    val status: String, // SAFE, SUSPICIOUS, PHISHING
    val riskScore: Int,
    val lastChecked: Long,
    val reasons: String // JSON array of reasons
)
