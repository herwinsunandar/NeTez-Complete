package com.herwin.netez.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MonitoringDao {
    @Query("SELECT * FROM monitoring_targets")
    fun getAllTargets(): Flow<List<MonitoringTarget>>

    @Query("SELECT * FROM monitoring_targets WHERE isActive = 1")
    fun getActiveTargets(): List<MonitoringTarget>

    @Query("SELECT * FROM monitoring_targets WHERE id = :id")
    suspend fun getTargetById(id: Long): MonitoringTarget?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTarget(target: MonitoringTarget): Long

    @Update
    suspend fun updateTarget(target: MonitoringTarget)

    @Delete
    suspend fun deleteTarget(target: MonitoringTarget)

    @Query("SELECT * FROM monitoring_logs WHERE targetId = :targetId ORDER BY timestamp DESC LIMIT 100")
    fun getLogsForTarget(targetId: Long): Flow<List<MonitoringLog>>

    @Transaction
    suspend fun insertAndPruneLog(log: MonitoringLog) {
        insertLog(log)
        pruneLogsForTarget(log.targetId)
    }

    @Insert
    suspend fun insertLog(log: MonitoringLog)

    @Query("DELETE FROM monitoring_logs WHERE targetId = :targetId")
    suspend fun deleteLogsForTarget(targetId: Long)

    @Query("DELETE FROM monitoring_logs WHERE targetId = :targetId AND id NOT IN (SELECT id FROM (SELECT id FROM monitoring_logs WHERE targetId = :targetId ORDER BY id DESC LIMIT 100))")
    suspend fun pruneLogsForTarget(targetId: Long)
    
    @Query("SELECT * FROM monitoring_logs WHERE targetId = :targetId ORDER BY timestamp DESC LIMIT 10")
    fun getSparklineLogs(targetId: Long): Flow<List<MonitoringLog>>
    
    @Query("SELECT COUNT(*) FROM monitoring_logs WHERE targetId = :targetId AND status = 'UP'")
    fun getUpLogsCount(targetId: Long): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM monitoring_logs WHERE targetId = :targetId")
    fun getTotalLogsCount(targetId: Long): Flow<Int>
    
    @Query("SELECT AVG(latency) FROM monitoring_logs WHERE targetId = :targetId AND status = 'UP'")
    fun getAverageLatency(targetId: Long): Flow<Double?>

    @Query("SELECT COUNT(*) FROM monitoring_targets WHERE isActive = 1")
    fun getActiveCount(): Flow<Int>
}
