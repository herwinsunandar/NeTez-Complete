package com.herwin.netez.data.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.herwin.netez.data.entities.PasswordStrengthHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface PasswordStrengthDao {
    @Insert
    suspend fun insert(history: PasswordStrengthHistory)

    @Query("SELECT * FROM password_strength_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<PasswordStrengthHistory>>

    @Query("DELETE FROM password_strength_history")
    suspend fun clearHistory()
}
