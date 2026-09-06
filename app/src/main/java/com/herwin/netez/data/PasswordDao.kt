package com.herwin.netez.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PasswordDao {
    @Query("SELECT * FROM passwords ORDER BY isFavorite DESC, appName ASC")
    fun getAllPasswords(): Flow<List<PasswordEntry>>

    @Query("SELECT * FROM passwords WHERE id = :id")
    suspend fun getPasswordById(id: String): PasswordEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPassword(entry: PasswordEntry)

    @Delete
    suspend fun deletePassword(entry: PasswordEntry)

    @Update
    suspend fun updatePassword(entry: PasswordEntry)
    
    @Query("SELECT COUNT(*) FROM passwords")
    fun getCount(): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM passwords")
    suspend fun getCountSync(): Int

    @Query("SELECT COUNT(*) FROM passwords WHERE strengthScore < 50")
    fun getWeakCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM passwords WHERE strengthScore < 50")
    suspend fun getWeakCountSync(): Int
}
