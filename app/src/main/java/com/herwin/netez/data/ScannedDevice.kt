package com.herwin.netez.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "scanned_devices")
data class ScannedDevice(
    @PrimaryKey val mac: String,
    val ip: String,
    val hostname: String,
    val vendor: String,
    val deviceType: String,
    val os: String,
    val firstSeen: Long,
    val lastSeen: Long
)

@Dao
interface ScannedDeviceDao {
    @Query("SELECT * FROM scanned_devices WHERE mac = :mac")
    suspend fun getDeviceByMac(mac: String): ScannedDevice?

    @Query("SELECT * FROM scanned_devices")
    suspend fun getAllDevices(): List<ScannedDevice>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: ScannedDevice)

    @Query("UPDATE scanned_devices SET lastSeen = :lastSeen, ip = :ip WHERE mac = :mac")
    suspend fun updateLastSeen(mac: String, ip: String, lastSeen: Long)
}
