package com.herwin.netez.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "urlhaus_feed")
data class UrlHausEntry(
    @PrimaryKey val url: String,
    val domain: String,
    val threat: String, // malware_download, phishing
    val tags: String, // bca,phishing,32bit
    val reference: String, // https://urlhaus.abuse.ch/url/123/
    val dateAdded: String
)
