package com.herwin.netez.data.daos

import androidx.room.*
import com.herwin.netez.data.entities.UrlHausEntry

@Dao
interface UrlHausDao {
    @Query("SELECT * FROM urlhaus_feed WHERE url LIKE '%' || :query || '%' OR domain = :domain LIMIT 10")
    fun search(query: String, domain: String): List<UrlHausEntry>

    @Query("SELECT EXISTS(SELECT 1 FROM urlhaus_feed WHERE url = :url OR domain = :domain)")
    suspend fun isPhishing(url: String, domain: String): Boolean

    @Query("SELECT COUNT(*) FROM urlhaus_feed")
    suspend fun getCount(): Int

    @Query("DELETE FROM urlhaus_feed")
    suspend fun clear()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<UrlHausEntry>)

    @Transaction
    suspend fun replaceAll(entries: List<UrlHausEntry>) {
        clear()
        insertAll(entries)
    }
}
