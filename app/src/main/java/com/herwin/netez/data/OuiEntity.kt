package com.herwin.netez.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "oui_table")
data class OuiEntity(
    @PrimaryKey val macPrefix: String,
    val vendor: String,
    val category: String = "Unknown",
    val type: String = "Unknown"
)

@Entity(tableName = "vendor_overrides")
data class VendorOverrideEntity(
    @PrimaryKey val mac: String,
    val customVendor: String,
    val customType: String? = null,
    val source: String = "USER"
)
