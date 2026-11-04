package com.herwin.netez.data.daos

import androidx.room.*
import com.herwin.netez.data.entities.SslDomain
import kotlinx.coroutines.flow.Flow

@Dao
interface SslDomainDao {
    @Query("SELECT * FROM ssl_domains")
    fun getAllDomains(): Flow<List<SslDomain>>

    @Query("SELECT * FROM ssl_domains")
    suspend fun getAllDomainsList(): List<SslDomain>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDomain(domain: SslDomain)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDomains(domains: List<SslDomain>)

    @Delete
    suspend fun deleteDomain(domain: SslDomain)

    @Query("SELECT COUNT(*) FROM ssl_domains WHERE (expiryDate > 0 AND (expiryDate - :nowMillis) <= (:maxReminderDays * 86400000)) OR (expiryDate <= 0 AND daysLeft <= :maxReminderDays) OR grade = 'F'")
    fun getExpiringCount(nowMillis: Long = System.currentTimeMillis(), maxReminderDays: Int = 14): Flow<Int>

    @Query("SELECT COUNT(*) FROM ssl_domains WHERE (expiryDate > 0 AND (expiryDate - :nowMillis) <= (:maxReminderDays * 86400000)) OR (expiryDate <= 0 AND daysLeft <= :maxReminderDays) OR grade = 'F'")
    suspend fun getExpiringCountSync(nowMillis: Long = System.currentTimeMillis(), maxReminderDays: Int = 14): Int

    @Query("SELECT COUNT(*) FROM ssl_domains WHERE (expiryDate > 0 AND expiryDate <= :nowMillis) OR (expiryDate <= 0 AND daysLeft <= 0)")
    fun getExpiredCount(nowMillis: Long = System.currentTimeMillis()): Flow<Int>

    @Query("SELECT COUNT(*) FROM ssl_domains WHERE grade = 'F'")
    fun getGradeFCount(): Flow<Int>

    @Query("SELECT * FROM ssl_domains WHERE domain = :domain LIMIT 1")
    suspend fun getDomain(domain: String): SslDomain?
}
