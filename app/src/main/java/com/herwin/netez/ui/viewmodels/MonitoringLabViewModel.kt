package com.herwin.netez.ui.viewmodels

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.data.*
import com.herwin.netez.entitlement.FeatureAccessManager
import com.herwin.netez.entitlement.FeatureAccessState
import com.herwin.netez.entitlement.FeatureId
import com.herwin.netez.service.MonitoringService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MonitoringLabViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val dao = database.monitoringDao()

    var actionMessage by mutableStateOf<String?>(null)
    var showPaywallDialog by mutableStateOf(false)

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
        val accessManager = FeatureAccessManager.getInstance(getApplication())
        val currentCount = targets.value.size
        val accessState = accessManager.checkAccess(FeatureId.MONITORING_TARGETS_MAX, currentCount)
        if (accessState == FeatureAccessState.LIMIT_REACHED) {
            showPaywallDialog = true
            return
        }

        val clampedInterval = if (accessManager.isPro()) intervalMinutes.coerceAtLeast(1) else intervalMinutes.coerceAtLeast(5)

        viewModelScope.launch {
            val target = MonitoringTarget(
                type = type,
                host = host,
                port = port,
                url = url,
                dnsServer = dnsServer,
                keyword = keyword,
                intervalMillis = clampedInterval * 60000L
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
