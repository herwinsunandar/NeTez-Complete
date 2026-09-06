package com.herwin.netez.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AuthType {
    PASSWORD, KEY
}

@Entity(tableName = "ssh_hosts")
data class SshHost(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val host: String,
    val port: Int = 22,
    val username: String,
    val authType: AuthType,
    val passwordEncrypted: String? = null,
    val privateKeyPath: String? = null,
    val tag: String? = null,
    val lastConnected: Long = 0
)
