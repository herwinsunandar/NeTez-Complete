package com.herwin.netez.data

import androidx.room.*

@Dao
interface OuiDao {
    @Query("SELECT vendor FROM oui_table WHERE macPrefix = :prefix LIMIT 1")
    suspend fun getVendor(prefix: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<OuiEntity>)

    @Query("SELECT COUNT(*) FROM oui_table")
    suspend fun getCount(): Int
}

@Dao
interface VendorOverrideDao {
    @Query("SELECT * FROM vendor_overrides WHERE mac = :mac")
    suspend fun getOverride(mac: String): VendorOverrideEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOverride(entity: VendorOverrideEntity)

    @Delete
    suspend fun deleteOverride(entity: VendorOverrideEntity)
}
