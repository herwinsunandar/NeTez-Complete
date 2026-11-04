package com.herwin.netez.data.daos

import androidx.room.*
import com.herwin.netez.data.entities.SecurityToolHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface SecurityToolHistoryDao {
    @Query("SELECT * FROM security_tools_history WHERE toolType = :toolType ORDER BY timestamp DESC LIMIT 20")
    fun getHistoryByTool(toolType: String): Flow<List<SecurityToolHistory>>

    @Query("SELECT * FROM security_tools_history ORDER BY timestamp DESC LIMIT 50")
    fun getAllHistory(): Flow<List<SecurityToolHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: SecurityToolHistory)

    @Query("DELETE FROM security_tools_history WHERE toolType = :toolType")
    suspend fun clearHistoryForTool(toolType: String)

    @Query("DELETE FROM security_tools_history")
    suspend fun clearAllHistory()
}
