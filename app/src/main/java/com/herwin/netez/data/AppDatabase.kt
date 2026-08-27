package com.herwin.netez.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TestResult::class, ScannedDevice::class, HttpHistory::class, QuickNote::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun testResultDao(): TestResultDao
    abstract fun scannedDeviceDao(): ScannedDeviceDao
    abstract fun httpHistoryDao(): HttpHistoryDao
    abstract fun quickNoteDao(): QuickNoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "netez_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
