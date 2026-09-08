package com.herwin.netez.ui.viewmodels

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.R
import com.herwin.netez.data.AppPermissionInfo
import com.herwin.netez.data.RiskCategory
import com.herwin.netez.network.AppScannerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AppPermissionFilter {
    ALL,
    DANGEROUS,
    SUSPICIOUS,
    USER,
    SYSTEM
}

data class AppPermissionUiState(
    val apps: List<AppPermissionInfo> = emptyList(),
    val filteredApps: List<AppPermissionInfo> = emptyList(),
    val isScanning: Boolean = false,
    val hasScanned: Boolean = false,
    val filter: AppPermissionFilter = AppPermissionFilter.ALL,
    val showSystemApps: Boolean = false
)

class AppPermissionViewModel(application: Application) : AndroidViewModel(application) {
    private val scannerManager = AppScannerManager(application)
    
    private val _uiState = MutableStateFlow(AppPermissionUiState())
    val uiState: StateFlow<AppPermissionUiState> = _uiState.asStateFlow()

    fun startScan() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true)
            val result = scannerManager.scanApps()
            _uiState.value = _uiState.value.copy(
                apps = result,
                filteredApps = applyFilters(result, _uiState.value.filter, _uiState.value.showSystemApps),
                isScanning = false,
                hasScanned = true
            )
        }
    }

    fun updateFilter(filter: AppPermissionFilter) {
        _uiState.value = _uiState.value.copy(
            filter = filter,
            filteredApps = applyFilters(_uiState.value.apps, filter, _uiState.value.showSystemApps)
        )
    }

    fun toggleSystemApps() {
        val newValue = !_uiState.value.showSystemApps
        _uiState.value = _uiState.value.copy(
            showSystemApps = newValue,
            filteredApps = applyFilters(_uiState.value.apps, _uiState.value.filter, newValue)
        )
    }

    private fun applyFilters(
        apps: List<AppPermissionInfo>, 
        filter: AppPermissionFilter, 
        showSystem: Boolean
    ): List<AppPermissionInfo> {
        var result = when (filter) {
            AppPermissionFilter.ALL -> apps
            AppPermissionFilter.DANGEROUS -> apps.filter { it.riskCategory == RiskCategory.DANGEROUS }
            AppPermissionFilter.SUSPICIOUS -> apps.filter { it.riskCategory == RiskCategory.SUSPICIOUS }
            AppPermissionFilter.USER -> apps.filter { !it.isSystem }
            AppPermissionFilter.SYSTEM -> apps.filter { it.isSystem }
        }

        if (!showSystem && filter != AppPermissionFilter.SYSTEM) {
            result = result.filter { !it.isSystem }
        }

        return result
    }

    fun copyResultsToClipboard(context: Context) {
        val report = StringBuilder(context.getString(R.string.app_scan_report_header) + "\n\n")
        _uiState.value.filteredApps.forEach { app ->
            val reason = if (app.riskReasonRes != 0) context.getString(app.riskReasonRes) else ""
            report.append("${app.appName} (${app.packageName})\n")
            report.append("Risk: ${app.riskScore} (${app.riskCategory})\n")
            if (reason.isNotEmpty()) report.append("Reason: $reason\n")
            report.append("Permissions: ${app.permissions.joinToString(", ")}\n\n")
        }
        
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("App Scan Results", report.toString())
        clipboard.setPrimaryClip(clip)
    }

    fun shareResults(context: Context) {
        val report = StringBuilder(context.getString(R.string.app_scan_report_header) + "\n\n")
        _uiState.value.filteredApps.forEach { app ->
            report.append("${app.appName} (${app.packageName})\n")
            report.append("Risk Score: ${app.riskScore}\n\n")
        }
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.app_scan_share_subject))
            putExtra(Intent.EXTRA_TEXT, report.toString())
        }
        context.startActivity(Intent.createChooser(intent, "Share Results"))
    }
}
