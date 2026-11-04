package com.herwin.netez.data.daos

import androidx.room.*
import com.herwin.netez.data.entities.BleDeviceEntity

@Dao
interface BleDeviceDao {
    @Query("SELECT * FROM scanned_ble_devices ORDER BY lastSeen DESC")
    suspend fun getAllBleDevices(): List<BleDeviceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBleDevice(device: BleDeviceEntity)

    @Query("DELETE FROM scanned_ble_devices WHERE mac = :mac")
    suspend fun deleteBleDevice(mac: String)

    @Query("DELETE FROM scanned_ble_devices")
    suspend fun clearAllBleDevices()
}
