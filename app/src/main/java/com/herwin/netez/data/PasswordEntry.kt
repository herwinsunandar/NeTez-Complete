package com.herwin.netez.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "passwords")
data class PasswordEntry(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val appName: String,
    val appPackage: String?,
    val username: String,
    val passwordEncrypted: String,
    val notes: String?,
    val iconUrl: String?,
    val strengthScore: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
