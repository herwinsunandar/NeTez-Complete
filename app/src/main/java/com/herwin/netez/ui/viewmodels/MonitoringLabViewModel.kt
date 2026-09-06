package com.herwin.netez.ui.viewmodels

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.data.*
import com.herwin.netez.service.MonitoringService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MonitoringLabViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val dao = database.monitoringDao()

    val targets: StateFlow<List<MonitoringTarget>> = dao.getAllTargets()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addTarget(
        type: MonitoringType,
        host: String? = null,
        port: Int? = null,
        url: String? = null,
        dnsServer: String? = null,
        keyword: String? = null,
        intervalMinutes: Int = 1
    ) {
        viewModelScope.launch {
            val target = MonitoringTarget(
                type = type,
                host = host,
                port = port,
                url = url,
                dnsServer = dnsServer,
                keyword = keyword,
                intervalMillis = intervalMinutes * 60000L
            )
            dao.insertTarget(target)
            startServiceIfNeeded()
        }
    }

    fun toggleTarget(target: MonitoringTarget) {
        viewModelScope.launch {
            dao.updateTarget(target.copy(isActive = !target.isActive))
            startServiceIfNeeded()
        }
    }

    fun deleteTarget(target: MonitoringTarget) {
        viewModelScope.launch {
            dao.deleteTarget(target)
            dao.deleteLogsForTarget(target.id)
            startServiceIfNeeded()
        }
    }

    private fun startServiceIfNeeded() {
        try {
            val intent = Intent(getApplication(), MonitoringService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                getApplication<Application>().startForegroundService(intent)
            } else {
                getApplication<Application>().startService(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getLogs(targetId: Long) = dao.getLogsForTarget(targetId)
}
