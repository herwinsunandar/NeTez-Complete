package com.herwin.netez.data.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.herwin.netez.data.entities.DeviceSecurityHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceSecurityDao {
    @Query("SELECT * FROM device_security_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<DeviceSecurityHistory>>

    @Query("SELECT * FROM device_security_history ORDER BY timestamp DESC")
    suspend fun getAllHistoryList(): List<DeviceSecurityHistory>

    @Insert
    suspend fun insertHistory(history: DeviceSecurityHistory)

    @Query("DELETE FROM device_security_history")
    suspend fun clearHistory()
}
