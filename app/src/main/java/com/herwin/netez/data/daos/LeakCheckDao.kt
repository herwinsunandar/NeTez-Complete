package com.herwin.netez.data.daos

import androidx.room.*
import com.herwin.netez.data.entities.LeakCheckEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface LeakCheckDao {
    @Query("SELECT * FROM leak_checks ORDER BY lastChecked DESC")
    fun getAllLeakChecks(): Flow<List<LeakCheckEntry>>

    @Query("SELECT * FROM leak_checks")
    suspend fun getAllLeakChecksList(): List<LeakCheckEntry>

    @Query("SELECT * FROM leak_checks WHERE batchId = :batchId")
    fun getLeakChecksByBatch(batchId: String): Flow<List<LeakCheckEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeakCheck(entry: LeakCheckEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeakChecks(entries: List<LeakCheckEntry>)

    @Delete
    suspend fun deleteLeakCheck(entry: LeakCheckEntry)

    @Query("SELECT * FROM leak_checks WHERE email = :email LIMIT 1")
    suspend fun getLeakCheck(email: String): LeakCheckEntry?

    @Query("SELECT COUNT(*) FROM leak_checks WHERE isBreached = 1")
    fun getBreachedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM leak_checks WHERE isBreached = 1")
    suspend fun getBreachedCountSync(): Int

    @Query("SELECT COUNT(*) FROM leak_checks")
    fun getTotalCount(): Flow<Int>
}
