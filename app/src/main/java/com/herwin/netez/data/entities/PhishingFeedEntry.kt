package com.herwin.netez.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "phishing_feed")
data class PhishingFeedEntry(
    @PrimaryKey val url: String
)
