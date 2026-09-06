package com.herwin.netez.data.daos

import androidx.room.*
import com.herwin.netez.data.entities.SslDomain
import kotlinx.coroutines.flow.Flow

@Dao
interface SslDomainDao {
    @Query("SELECT * FROM ssl_domains ORDER BY daysLeft ASC")
    fun getAllDomains(): Flow<List<SslDomain>>

    @Query("SELECT * FROM ssl_domains")
    suspend fun getAllDomainsList(): List<SslDomain>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDomain(domain: SslDomain)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDomains(domains: List<SslDomain>)

    @Delete
    suspend fun deleteDomain(domain: SslDomain)

    @Query("SELECT COUNT(*) FROM ssl_domains WHERE daysLeft <= 14")
    fun getExpiringCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM ssl_domains WHERE daysLeft <= 14")
    suspend fun getExpiringCountSync(): Int
    
    @Query("SELECT COUNT(*) FROM ssl_domains WHERE daysLeft <= 0")
    fun getExpiredCount(): Flow<Int>

    @Query("SELECT * FROM ssl_domains WHERE domain = :domain LIMIT 1")
    suspend fun getDomain(domain: String): SslDomain?
}
