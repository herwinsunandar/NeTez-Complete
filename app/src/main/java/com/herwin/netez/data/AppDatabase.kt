package com.herwin.netez.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

import com.herwin.netez.data.entities.PasswordStrengthHistory
import com.herwin.netez.data.daos.PasswordStrengthDao
import com.herwin.netez.data.entities.SslDomain
import com.herwin.netez.data.daos.SslDomainDao
import com.herwin.netez.data.entities.LeakCheckEntry
import com.herwin.netez.data.daos.LeakCheckDao
import com.herwin.netez.data.entities.PhishingCheckEntry
import com.herwin.netez.data.entities.PhishingFeedEntry
import com.herwin.netez.data.daos.PhishingDao
import com.herwin.netez.data.entities.UrlHausEntry
import com.herwin.netez.data.daos.UrlHausDao
import com.herwin.netez.data.entities.DeviceSecurityHistory
import com.herwin.netez.data.daos.DeviceSecurityDao
import com.herwin.netez.data.entities.WifiAuditHistory
import com.herwin.netez.data.daos.WifiAuditDao
import com.herwin.netez.data.entities.SecurityToolHistory
import com.herwin.netez.data.daos.SecurityToolHistoryDao
import com.herwin.netez.data.entities.BleDeviceEntity
import com.herwin.netez.data.daos.BleDeviceDao
import com.herwin.netez.data.entities.SecurityIntelligenceEntity
import com.herwin.netez.data.daos.SecurityIntelligenceDao
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(entities = [TestResult::class, ScannedDevice::class, HttpHistory::class, QuickNote::class, SshHost::class, StabilityResult::class, MonitoringTarget::class, MonitoringLog::class, OuiEntity::class, VendorOverrideEntity::class, IspRating::class, IspReview::class, PasswordStrengthHistory::class, PasswordEntry::class, SslDomain::class, LeakCheckEntry::class, PhishingCheckEntry::class, PhishingFeedEntry::class, UrlHausEntry::class, DeviceSecurityHistory::class, WifiAuditHistory::class, SecurityToolHistory::class, BleDeviceEntity::class, SecurityIntelligenceEntity::class], version = 27, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun passwordDao(): VaultDao
    abstract fun securityIntelligenceDao(): SecurityIntelligenceDao
    abstract fun bleDeviceDao(): BleDeviceDao
    abstract fun testResultDao(): TestResultDao
    abstract fun scannedDeviceDao(): ScannedDeviceDao
    abstract fun httpHistoryDao(): HttpHistoryDao
    abstract fun quickNoteDao(): QuickNoteDao
    abstract fun sshHostDao(): SshHostDao
    abstract fun stabilityResultDao(): StabilityResultDao
    abstract fun monitoringDao(): MonitoringDao
    abstract fun ouiDao(): OuiDao
    abstract fun vendorOverrideDao(): VendorOverrideDao
    abstract fun ispDao(): IspDao
    abstract fun passwordStrengthDao(): PasswordStrengthDao
    abstract fun sslDomainDao(): SslDomainDao
    abstract fun leakCheckDao(): LeakCheckDao
    abstract fun phishingDao(): PhishingDao
    abstract fun urlHausDao(): UrlHausDao
    abstract fun deviceSecurityDao(): DeviceSecurityDao
    abstract fun wifiAuditDao(): WifiAuditDao
    abstract fun securityToolHistoryDao(): SecurityToolHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = try {
                    buildDatabase(context)
                } catch (e: Exception) {
                    Log.e("AppDatabase", "Failed to open database, deleting and recreating", e)
                    try {
                        context.applicationContext.deleteDatabase("netez_v2.db")
                    } catch (_: Exception) {}
                    buildDatabase(context)
                }
                INSTANCE = instance
                instance
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            val passphrase = "netez_internal_secure_storage".toByteArray()
            val factory = SupportOpenHelperFactory(passphrase)

            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "netez_v2.db"
            )
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration()
            .build()
        }
    }
}
