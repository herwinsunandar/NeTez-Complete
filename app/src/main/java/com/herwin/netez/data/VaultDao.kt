package com.herwin.netez.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {
    @Query("SELECT * FROM passwords ORDER BY isFavorite DESC, updatedAt DESC")
    fun getAllEntries(): Flow<List<VaultEntry>>

    @Query("SELECT * FROM passwords ORDER BY isFavorite DESC, updatedAt DESC")
    fun getAllPasswords(): Flow<List<VaultEntry>> = getAllEntries()

    @Query("SELECT * FROM passwords ORDER BY isFavorite DESC, updatedAt DESC")
    suspend fun getAllEntriesSync(): List<VaultEntry>

    @Query("SELECT * FROM passwords WHERE id = :id LIMIT 1")
    suspend fun getEntryById(id: String): VaultEntry?

    @Query("SELECT * FROM passwords WHERE credentialType = :type ORDER BY isFavorite DESC, updatedAt DESC")
    fun getEntriesByType(type: String): Flow<List<VaultEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: VaultEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPassword(entry: PasswordEntry) = insertEntry(entry)

    @Delete
    suspend fun deleteEntry(entry: VaultEntry)

    @Delete
    suspend fun deletePassword(entry: PasswordEntry) = deleteEntry(entry)

    @Update
    suspend fun updateEntry(entry: VaultEntry)

    @Query("SELECT COUNT(*) FROM passwords")
    fun getCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM passwords")
    suspend fun getCountSync(): Int

    @Query("SELECT COUNT(*) FROM passwords WHERE strengthScore < 50")
    fun getWeakCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM passwords WHERE strengthScore < 50")
    suspend fun getWeakCountSync(): Int
}
