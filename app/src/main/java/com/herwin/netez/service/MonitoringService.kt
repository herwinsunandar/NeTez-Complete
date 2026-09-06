package com.herwin.netez.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.herwin.netez.data.AppDatabase
import kotlinx.coroutines.*

class MonitoringService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var job: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MonitoringNotificationHelper.createNotificationChannels(this)
        
        val notification = MonitoringNotificationHelper.getServiceNotification(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1001, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(1001, notification)
        }
        
        Log.d("MonitoringService", "Service started")
        startMonitoring()
        return START_STICKY
    }

    private fun startMonitoring() {
        job?.cancel()
        job = serviceScope.launch {
            val processor = MonitoringProcessor(this@MonitoringService)
            val database = AppDatabase.getDatabase(this@MonitoringService)
            val dao = database.monitoringDao()
            
            while (isActive) {
                val targets = dao.getActiveTargets()
                val now = System.currentTimeMillis()
                
                Log.d("MonitoringService", "Processing ${targets.size} active targets")
                
                targets.forEach { target ->
                    val lastCheck = target.lastCheckTimestamp ?: 0L
                    if (now - lastCheck >= target.intervalMillis) {
                        launch { processor.processTarget(target) }
                    }
                }
                
                delay(10000)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
