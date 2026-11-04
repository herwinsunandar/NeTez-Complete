package com.herwin.netez.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scanned_ble_devices")
data class BleDeviceEntity(
    @PrimaryKey val mac: String,
    val name: String,
    val vendor: String,
    val rssi: Int,
    val deviceType: String,
    val isPaired: Boolean,
    val isEverConnected: Boolean = false,
    val firstSeen: Long = System.currentTimeMillis(),
    val lastSeen: Long = System.currentTimeMillis()
)
