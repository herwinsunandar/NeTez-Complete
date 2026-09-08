package com.herwin.netez.data.daos

import androidx.room.*
import com.herwin.netez.data.entities.WifiAuditHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface WifiAuditDao {
    @Query("SELECT * FROM wifi_audit_history WHERE bssid = :bssid LIMIT 1")
    suspend fun getAuditByBssid(bssid: String): WifiAuditHistory?

    @Query("SELECT * FROM wifi_audit_history ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastAudit(): WifiAuditHistory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudit(audit: WifiAuditHistory)

    @Query("DELETE FROM wifi_audit_history")
    suspend fun clearHistory()
}
