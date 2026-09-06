package com.herwin.netez.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StabilityResultDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: StabilityResult)

    @Query("SELECT * FROM stability_results ORDER BY timestamp DESC")
    fun getAllResults(): Flow<List<StabilityResult>>

    @Query("SELECT * FROM stability_results WHERE id = :id")
    suspend fun getResultById(id: Long): StabilityResult?

    @Delete
    suspend fun delete(result: StabilityResult)

    @Query("DELETE FROM stability_results")
    suspend fun deleteAll()
}
