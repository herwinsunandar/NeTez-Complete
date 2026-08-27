package com.herwin.netez.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quick_notes")
data class QuickNote(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val note: String,
    val tag: String,
    val type: String, // IP, DOMAIN, NOTE
    val isPinned: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
