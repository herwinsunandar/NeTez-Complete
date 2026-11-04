package com.herwin.netez.data

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Keep
@Entity(tableName = "passwords")
data class PasswordEntry(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    // Core Metadata
    val title: String = "",                         // Title / Label (e.g. "Production Ubuntu", "Google Account")
    val credentialType: String = "WEBSITE",        // "WEBSITE", "SERVER", "SSH", "SFTP", "API", "DATABASE", "NETWORK", "NOTES", "CUSTOM"
    val categoryTag: String = "General",
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    // Website / App fields (Backward compatible with PasswordEntry)
    val appName: String = "",                      // Legacy appName
    val appPackage: String? = null,
    val websiteUrl: String? = null,
    val username: String = "",
    val passwordEncrypted: String = "",            // AES-256-GCM Encrypted
    val notesEncrypted: String? = null,            // AES-256-GCM Encrypted
    val notes: String? = null,                     // Legacy unencrypted notes field

    // Server / SSH / SFTP / Network Device fields
    val hostnameOrIp: String? = null,
    val port: Int? = null,
    val protocol: String? = null,                  // "SSH", "SFTP", "RDP", "WinRM", "HTTPS", "HTTP", "Telnet", "SNMP"
    val authType: String? = null,                  // "PASSWORD", "PRIVATE_KEY", "API_KEY", "BEARER", "BASIC", "OAUTH"
    val privateKeyEncrypted: String? = null,       // AES-256-GCM Encrypted SSH Private Key
    val keyPassphraseEncrypted: String? = null,   // AES-256-GCM Encrypted Private Key Passphrase

    // API & Database Credential fields
    val apiEndpoint: String? = null,
    val apiTokenEncrypted: String? = null,         // AES-256-GCM Encrypted API Key / Bearer / Secret
    val databaseType: String? = null,              // "MySQL", "PostgreSQL", "MSSQL", "Oracle", "MongoDB", "Other"
    val databaseName: String? = null,
    val vendorName: String? = null,                // For Network Devices (e.g. Cisco, MikroTik)

    // Security Metrics
    val iconUrl: String? = null,                   // Legacy iconUrl
    val strengthScore: Int = 80,
    val isCustomSensitive: Boolean = false,
    val customFieldsJsonEncrypted: String? = null  // AES-256-GCM Encrypted JSON for custom secret fields
)
