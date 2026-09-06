package com.herwin.netez.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.herwin.netez.data.AppDatabase
import com.herwin.netez.service.MonitoringProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MonitoringWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val processor = MonitoringProcessor(applicationContext)
        val dao = AppDatabase.getDatabase(applicationContext).monitoringDao()
        
        val targets = dao.getActiveTargets().filter { it.intervalMillis >= 900000 } // >= 15 mins
        
        targets.forEach { target ->
            processor.processTarget(target)
        }
        
        Result.success()
    }
}
