package com.herwin.netez.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HttpHistoryDao {
    @Query("SELECT * FROM http_history ORDER BY timestamp DESC LIMIT 50")
    fun getRecentHistory(): Flow<List<HttpHistory>>

    @Insert
    suspend fun insert(history: HttpHistory)

    @Query("DELETE FROM http_history WHERE id NOT IN (SELECT id FROM http_history ORDER BY timestamp DESC LIMIT 50)")
    suspend fun clearOldHistory()

    @Query("DELETE FROM http_history")
    suspend fun deleteAll()
}
