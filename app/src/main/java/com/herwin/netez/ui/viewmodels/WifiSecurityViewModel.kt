package com.herwin.netez.ui.viewmodels

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.R
import com.herwin.netez.network.WifiAuditResult
import com.herwin.netez.network.WifiCheckStatus
import com.herwin.netez.network.WifiSecurityEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WifiSecurityUiState(
    val auditResult: WifiAuditResult? = null,
    val isScanning: Boolean = false,
    val error: String? = null
)

class WifiSecurityViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _uiState = MutableStateFlow(WifiSecurityUiState())
    val uiState: StateFlow<WifiSecurityUiState> = _uiState.asStateFlow()

    fun startAudit() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true, error = null)
            try {
                val result = WifiSecurityEngine.auditCurrentWifi(getApplication())
                _uiState.value = _uiState.value.copy(auditResult = result, isScanning = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isScanning = false, error = e.message)
            }
        }
    }

    fun copyBssidToClipboard(context: Context, bssid: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("BSSID", bssid)
        clipboard.setPrimaryClip(clip)
    }

    fun copyReportToClipboard(context: Context) {
        val result = _uiState.value.auditResult ?: return
        val report = StringBuilder(context.getString(R.string.wifi_audit_report_title)).append("\n\n")
        report.append(context.getString(R.string.wifi_audit_report_ssid, result.ssid ?: context.getString(R.string.wifi_audit_unknown_wifi))).append("\n")
        report.append(context.getString(R.string.wifi_audit_report_security, result.security ?: context.getString(R.string.wifi_audit_not_verifiable))).append("\n")
        report.append(context.getString(R.string.wifi_audit_report_frequency, result.frequency ?: 0)).append("\n")
        report.append(context.getString(R.string.wifi_audit_report_channel, result.channel ?: 0)).append("\n\n")
        report.append(context.getString(R.string.wifi_audit_report_score, result.securityScore)).append("\n")
        
        val gradeRes = when(result.securityGrade) {
            "EXCELLENT" -> R.string.wifi_audit_grade_excellent
            "GOOD" -> R.string.wifi_audit_grade_good
            "FAIR" -> R.string.wifi_audit_grade_fair
            "WEAK" -> R.string.wifi_audit_grade_weak
            "CRITICAL" -> R.string.wifi_audit_grade_critical
            else -> R.string.wifi_audit_grade_fair
        }
        report.append(context.getString(R.string.wifi_audit_report_grade, context.getString(gradeRes))).append("\n\n")
        
        report.append(context.getString(R.string.wifi_audit_report_critical, result.criticalCount)).append("\n")
        report.append(context.getString(R.string.wifi_audit_report_warning, result.warningCount)).append("\n\n")
        
        report.append(context.getString(R.string.wifi_audit_report_findings)).append("\n")
        result.checks.forEach { check ->
            if (check.status == WifiCheckStatus.CRITICAL || check.status == WifiCheckStatus.WARNING) {
                report.append("* ${context.getString(check.nameRes)}: ${check.detail}\n")
            }
        }
        
        report.append("\n").append(context.getString(R.string.wifi_audit_report_recommendations)).append("\n")
        result.checks.forEach { check ->
            check.recommendation?.let { rec ->
                report.append("* $rec\n")
            }
        }
        
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("WiFi Audit Report", report.toString())
        clipboard.setPrimaryClip(clip)
    }

    fun shareReport(context: Context) {
        val result = _uiState.value.auditResult ?: return
        val gradeRes = when(result.securityGrade) {
            "EXCELLENT" -> R.string.wifi_audit_grade_excellent
            "GOOD" -> R.string.wifi_audit_grade_good
            "FAIR" -> R.string.wifi_audit_grade_fair
            "WEAK" -> R.string.wifi_audit_grade_weak
            "CRITICAL" -> R.string.wifi_audit_grade_critical
            else -> R.string.wifi_audit_grade_fair
        }
        val report = context.getString(R.string.wifi_audit_report_title) + ": " + 
                     (result.ssid ?: context.getString(R.string.wifi_audit_unknown_wifi)) + " - Score " + 
                     result.securityScore + "/100 (" + context.getString(gradeRes) + ") via NeTez"
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "WiFi Security Audit Report")
            putExtra(Intent.EXTRA_TEXT, report)
        }
        context.startActivity(Intent.createChooser(intent, "Share Report"))
    }
}
