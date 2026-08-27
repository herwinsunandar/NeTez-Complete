package com.herwin.netez.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TestResultDao {
    @Query("SELECT * FROM test_results ORDER BY timestamp DESC")
    fun getAllResults(): Flow<List<TestResult>>

    @Insert
    suspend fun insertResult(result: TestResult)

    @Query("DELETE FROM test_results")
    suspend fun deleteAll()
}
