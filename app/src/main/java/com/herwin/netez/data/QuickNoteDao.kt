package com.herwin.netez.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface QuickNoteDao {
    @Query("SELECT * FROM quick_notes ORDER BY isPinned DESC, timestamp DESC")
    fun getAllNotes(): Flow<List<QuickNote>>

    @Query("SELECT * FROM quick_notes WHERE title LIKE '%' || :query || '%' OR note LIKE '%' || :query || '%' OR tag LIKE '%' || :query || '%' ORDER BY isPinned DESC, timestamp DESC")
    fun searchNotes(query: String): Flow<List<QuickNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: QuickNote)

    @Update
    suspend fun update(note: QuickNote)

    @Delete
    suspend fun delete(note: QuickNote)

    @Query("SELECT * FROM quick_notes WHERE id = :id")
    suspend fun getNoteById(id: Int): QuickNote?
}
