package com.herwin.netez.data.daos

import androidx.room.*
import com.herwin.netez.data.entities.PhishingCheckEntry
import com.herwin.netez.data.entities.PhishingFeedEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface PhishingDao {
    @Query("SELECT * FROM phishing_checks ORDER BY lastChecked DESC")
    fun getAllPhishingChecks(): Flow<List<PhishingCheckEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhishingCheck(entry: PhishingCheckEntry)

    @Delete
    suspend fun deletePhishingCheck(entry: PhishingCheckEntry)

    @Query("SELECT COUNT(*) FROM phishing_checks WHERE status = 'PHISHING'")
    fun getPhishingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM phishing_checks")
    fun getTotalCount(): Flow<Int>

    // Feed management
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeed(urls: List<PhishingFeedEntry>)

    @Query("DELETE FROM phishing_feed")
    suspend fun clearFeed()

    @Query("SELECT EXISTS(SELECT 1 FROM phishing_feed WHERE url = :url LIMIT 1)")
    suspend fun isPhishingInFeed(url: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM phishing_feed WHERE url LIKE '%' || :domain || '%' LIMIT 1)")
    suspend fun isDomainInFeed(domain: String): Boolean
}
