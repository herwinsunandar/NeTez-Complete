package com.herwin.netez.ui.viewmodels

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.herwin.netez.data.AppDatabase
import com.herwin.netez.data.SecurityPreferences
import com.herwin.netez.data.entities.SecurityIntelligenceEntity
import android.net.Uri
import com.herwin.netez.entitlement.FeatureAccessManager
import com.herwin.netez.entitlement.FeatureAccessState
import com.herwin.netez.entitlement.FeatureId
import com.herwin.netez.utils.SecurityIntelligencePdfReportGenerator
import com.herwin.netez.engine.intelligence.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.StringBuilder
import java.net.InetSocketAddress
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SecurityIntelligenceViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dao = db.securityIntelligenceDao()
    private val securityPrefs = SecurityPreferences(application)
    private val gson = Gson()

    var inputQuery by mutableStateOf("")
    var parsedTarget by mutableStateOf<ParsedTarget?>(null)
    var isScanning by mutableStateOf(false)
    var scanResult by mutableStateOf<SecurityIntelligenceResult?>(null)
    var activeScanRunning by mutableStateOf(false)
    var activeScanProgress by mutableStateOf("")
    var errorMessage by mutableStateOf<String?>(null)
    var actionMessage by mutableStateOf<String?>(null)
    var showPaywallDialog by mutableStateOf(false)
    var showGlobalSupportDialog by mutableStateOf(false)

    private var scanJob: Job? = null

    val recentHistory = dao.getAllHistory()
        .map { list ->
            list.mapNotNull { entity ->
                try {
                    gson.fromJson(entity.jsonResult, SecurityIntelligenceResult::class.java)
                } catch (e: Exception) { null }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onQueryChange(newQuery: String) {
        inputQuery = newQuery
        errorMessage = null
        if (newQuery.isNotBlank()) {
            parsedTarget = InputClassifier.classifyAndNormalize(newQuery)
        } else {
            parsedTarget = null
        }
    }

    fun startAssessment(targetInput: String? = null) {
        val queryToScan = targetInput ?: inputQuery
        val classified = InputClassifier.classifyAndNormalize(queryToScan)
        parsedTarget = classified

        if (!classified.isValid) {
            errorMessage = classified.errorMessage ?: "Invalid target input format."
            return
        }

        val context = getApplication<Application>().applicationContext
        val accessManager = FeatureAccessManager.getInstance(context)
        val accessState = accessManager.checkAccess(FeatureId.SECURITY_INTELLIGENCE_DAILY)
        if (accessState == FeatureAccessState.LIMIT_REACHED) {
            showPaywallDialog = true
            return
        }
        accessManager.consumeQuota(FeatureId.SECURITY_INTELLIGENCE_DAILY)

        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            isScanning = true
            errorMessage = null
            scanResult = null

            SecurityIntelligenceEngine.assessTargetStream(context, classified)
                .catch { e ->
                    errorMessage = "Assessment completed with limited data: ${e.localizedMessage ?: "Connection reset"}"
                    isScanning = false
                }
                .collect { result ->
                    scanResult = result
                    if (!result.isPartial) {
                        isScanning = false
                        saveToHistory(result)
                        if (accessManager.trackAndCheckGlobalToolUsage()) {
                            showGlobalSupportDialog = true
                        }
                    }
                }
        }
    }

    fun rescan(targetString: String) {
        inputQuery = targetString
        startAssessment(targetString)
    }

    fun selectFromHistory(result: SecurityIntelligenceResult) {
        inputQuery = result.target.rawInput
        parsedTarget = result.target
        scanResult = result
    }

    private suspend fun saveToHistory(result: SecurityIntelligenceResult) = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(result)
            val entity = SecurityIntelligenceEntity(
                target = result.target.rawInput,
                targetType = result.target.type.name,
                timestamp = result.timestamp,
                overallStatus = result.overallStatus,
                checksCompleted = result.checksCompleted,
                warningCount = result.warningCount,
                criticalCount = result.criticalCount,
                jsonResult = json
            )
            dao.insertHistory(entity)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteHistoryItem(target: String) {
        viewModelScope.launch {
            dao.deleteHistory(target)
            if (scanResult?.target?.rawInput == target) {
                scanResult = null
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            dao.clearHistory()
            scanResult = null
        }
    }

    fun runActivePortScan() {
        val currentResult = scanResult ?: return
        val targetIp = currentResult.technicalDetails["Resolved IP"] as? String ?: currentResult.target.host
        if (targetIp.isBlank()) return

        viewModelScope.launch {
            activeScanRunning = true
            activeScanProgress = "Scanning common ports on $targetIp..."
            try {
                // Perform quick active scan on ports 80, 443, 22, 21, 3306, 8080
                val openPorts = mutableListOf<Int>()
                val testPorts = listOf(80, 443, 22, 21, 3306, 8080)
                testPorts.forEach { port ->
                    activeScanProgress = "Testing port $port..."
                    val isOpen = withContext(Dispatchers.IO) {
                        try {
                            val socket = Socket()
                            socket.connect(InetSocketAddress(targetIp, port), 250)
                            socket.close()
                            true
                        } catch (e: Exception) { false }
                    }
                    if (isOpen) openPorts.add(port)
                }

                val openPortsText = if (openPorts.isEmpty()) "None detected (Firewalled)" else openPorts.joinToString(", ")
                val updatedTech = currentResult.technicalDetails.toMutableMap()
                updatedTech["Active Port Scan"] = "Open Ports: $openPortsText"

                val updatedCategories = currentResult.categoryStatuses.map { cat ->
                    if (cat.type == CheckCategoryType.EXPOSURE) {
                        cat.copy(summary = "Resolved IP: $targetIp · Open Ports: $openPortsText")
                    } else cat
                }

                scanResult = currentResult.copy(
                    categoryStatuses = updatedCategories,
                    technicalDetails = updatedTech
                )
                actionMessage = "Active scan completed for $targetIp"
            } catch (e: Exception) {
                actionMessage = "Active scan error: ${e.localizedMessage}"
            } finally {
                activeScanRunning = false
                activeScanProgress = ""
            }
        }
    }

    fun getShareSummaryText(): String {
        val res = scanResult ?: return ""
        val sb = StringBuilder("🛡️ NeTez Security Intelligence Report\n")
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        sb.append("📅 Date: ${sdf.format(Date(res.timestamp))}\n")
        sb.append("─────────────────────\n")
        sb.append("🎯 Target: ${res.target.rawInput} (${res.target.type.name})\n")
        sb.append("📊 Status: ${res.overallStatus}\n")
        sb.append("🔍 Checks Completed: ${res.checksCompleted}\n")
        sb.append("⚠️ Warnings: ${res.warningCount} | Critical: ${res.criticalCount}\n\n")

        if (res.findings.isNotEmpty()) {
            sb.append("🚨 Security Findings:\n")
            res.findings.forEach { finding ->
                sb.append("   • [${finding.severity.name}] ${finding.title}\n")
                sb.append("     Recommendation: ${finding.recommendation}\n")
            }
            sb.append("\n")
        }

        if (res.passedChecks.isNotEmpty()) {
            sb.append("✅ Passed Checks (${res.passedChecks.size}):\n")
            res.passedChecks.forEach { passed ->
                sb.append("   ✓ ${passed.title}\n")
            }
            sb.append("\n")
        }

        sb.append("─────────────────────\n")
        sb.append("Sent from NeTez Network Tools")
        return sb.toString()
    }

    fun copySummaryToClipboard(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Security Intelligence Report", getShareSummaryText())
        clipboard.setPrimaryClip(clip)
        actionMessage = "Summary report copied to clipboard"
    }

    fun shareSummary(context: Context) {
        val text = getShareSummaryText()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Security Intelligence Report")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share Security Intelligence Report"))
    }

    fun exportPdfReport(context: Context, uri: Uri) {
        val accessManager = FeatureAccessManager.getInstance(context)
        if (!accessManager.isPro()) {
            actionMessage = "Professional PDF Report export is available in NeTez PRO"
            return
        }
        val res = scanResult ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    SecurityIntelligencePdfReportGenerator.generatePdfReport(context, res, os)
                }
                withContext(Dispatchers.Main) {
                    actionMessage = "PDF Report saved successfully"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    actionMessage = "Failed to export PDF: ${e.localizedMessage ?: "File error"}"
                }
            }
        }
    }
}
