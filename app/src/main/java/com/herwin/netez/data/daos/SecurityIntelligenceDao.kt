package com.herwin.netez.data.daos

import androidx.room.*
import com.herwin.netez.data.entities.SecurityIntelligenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SecurityIntelligenceDao {
    @Query("SELECT * FROM security_intelligence_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<SecurityIntelligenceEntity>>

    @Query("SELECT * FROM security_intelligence_history ORDER BY timestamp DESC LIMIT 10")
    suspend fun getRecentHistory(): List<SecurityIntelligenceEntity>

    @Query("SELECT * FROM security_intelligence_history WHERE target = :target LIMIT 1")
    suspend fun getHistoryForTarget(target: String): SecurityIntelligenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: SecurityIntelligenceEntity)

    @Query("DELETE FROM security_intelligence_history WHERE target = :target")
    suspend fun deleteHistory(target: String)

    @Query("DELETE FROM security_intelligence_history")
    suspend fun clearHistory()
}
