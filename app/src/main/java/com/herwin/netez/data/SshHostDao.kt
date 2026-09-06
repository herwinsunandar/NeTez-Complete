package com.herwin.netez.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SshHostDao {
    @Query("SELECT * FROM ssh_hosts ORDER BY lastConnected DESC")
    fun getAllHosts(): Flow<List<SshHost>>

    @Query("SELECT * FROM ssh_hosts WHERE id = :id")
    suspend fun getHostById(id: Long): SshHost?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHost(host: SshHost): Long

    @Update
    suspend fun updateHost(host: SshHost)

    @Delete
    suspend fun deleteHost(host: SshHost)

    @Query("UPDATE ssh_hosts SET lastConnected = :timestamp WHERE id = :id")
    suspend fun updateLastConnected(id: Long, timestamp: Long)
}
