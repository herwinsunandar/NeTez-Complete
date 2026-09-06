package com.herwin.netez

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.herwin.netez.data.wifi.VendorDatabase
import com.herwin.netez.worker.OUIUpdateWorker
import com.herwin.netez.workers.UrlHausFeedWorker
import kotlinx.coroutines.*
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security
import java.util.concurrent.TimeUnit

class NeTezApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        
        // Initialize SQLCipher libs
        try {
            System.loadLibrary("sqlcipher")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        

        // Setup BouncyCastle for modern SSH algorithms
        setupBouncyCastle()

        // Optimization: Load Vendor Database asynchronously to avoid blocking UI thread
        applicationScope.launch(Dispatchers.IO) {
            VendorDatabase.load(this@NeTezApplication)
        }

        try {
            scheduleOuiUpdate()
            UrlHausFeedWorker.schedule(this, immediate = true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun scheduleOuiUpdate() {
        try {
            val request = PeriodicWorkRequestBuilder<OUIUpdateWorker>(7, TimeUnit.DAYS)
                .addTag("OUI_UPDATE")
                .build()
            
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "OUI_UPDATE_PERIODIC",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupBouncyCastle() {
        // Run this in background too if possible, but BC registration is usually fast enough.
        // If it still feels slow, this could be moved to background before SSH usage.
        applicationScope.launch(Dispatchers.Default) {
            try {
                Security.removeProvider("BC")
                Security.insertProviderAt(BouncyCastleProvider(), 1)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
