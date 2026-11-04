package com.herwin.netez.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.herwin.netez.entitlement.FeatureAccessManager
import com.herwin.netez.entitlement.FeatureAccessState
import com.herwin.netez.entitlement.FeatureId

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.data.AppDatabase
import com.herwin.netez.data.StabilityMode
import com.herwin.netez.data.StabilityResult
import com.herwin.netez.network.StabilityTestManager
import com.herwin.netez.utils.NetworkUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class StabilityState(
    val isRunning: Boolean = false,
    val mode: StabilityMode = StabilityMode.LOCAL_LOAD,
    val target: String = "",
    val customUrl: String = "https://speed.cloudflare.com/__down?bytes=100000000",
    val threads: Int = 20,
    val duration: Int = 30,
    val progress: Float = 0f,
    val latencyHistory: List<Double> = emptyList(),
    val successHistory: List<Boolean> = emptyList(),
    val avgLatency: Double = 0.0,
    val maxLatency: Double = 0.0,
    val idleLatency: Double = 0.0,
    val loadedLatency: Double = 0.0,
    val bufferbloat: Double = 0.0,
    val packetLoss: Int = 0,
    val successRate: Double = 0.0,
    val showResult: Boolean = false,
    val lastResult: StabilityResult? = null,
    val hasConsent: Boolean = false,
    val isExternalTarget: Boolean = false
)

class StabilityTestViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(StabilityState())
    val uiState: StateFlow<StabilityState> = _uiState

    var showPaywallDialog by mutableStateOf(false)

    private val db = AppDatabase.getDatabase(application)
    private val dao = db.stabilityResultDao()
    private val prefs = application.getSharedPreferences("stability_prefs", Context.MODE_PRIVATE)

    fun getShareText(): String {
        val state = _uiState.value
        val res = state.lastResult ?: return "No stability test results available."

        val sb = StringBuilder("⚡ NeTez Stability Test Report\n")
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        sb.append("📅 Date: ${sdf.format(Date())}\n")
        sb.append("─────────────────────\n")
        sb.append("🎯 Target: ${res.target}\n")
        sb.append("🛠️ Mode: ${if (res.mode == StabilityMode.LOCAL_LOAD) "LOCAL LOAD" else "BUFFERBLOAT"}\n")

        val isStable = res.packetLoss == 0 && res.bufferbloat < 50.0
        val statusText = if (isStable) "🟢 STABLE" else if (res.packetLoss > 5) "🔴 UNSTABLE" else "🟡 CONGESTED"
        sb.append("🛡️ Status: $statusText\n\n")

        sb.append("📊 Stats:\n")
        sb.append("   • Avg Latency: ${String.format("%.2f", res.avgLatency)} ms\n")
        sb.append("   • Max Latency: ${String.format("%.2f", res.maxLatency)} ms\n")

        if (res.mode == StabilityMode.BUFFERBLOAT) {
            sb.append("   • Idle Latency: ${String.format("%.2f", res.idleLatency)} ms\n")
            sb.append("   • Loaded Latency: ${String.format("%.2f", res.loadedLatency)} ms\n")
            sb.append("   • Bufferbloat Δ: ${String.format("%.2f", res.bufferbloat)} ms\n")
        }

        sb.append("   • Success Rate: ${String.format("%.1f", res.successRate)}%\n")
        sb.append("   • Packet Loss: ${res.packetLoss}\n")

        sb.append("─────────────────────\n")
        sb.append("Sent from NeTez Network Tools")
        return sb.toString()
    }

    fun copyToClipboard() {
        val context = getApplication<Application>()
        val text = getShareText()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Stability Test Results", text)
        clipboard.setPrimaryClip(clip)
    }

    fun shareResults(context: Context) {
        val text = getShareText()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share Stability Results"))
    }

    init {
        _uiState.update { it.copy(
            hasConsent = checkConsent()
        ) }

        viewModelScope.launch {
            StabilityTestManager.testUpdates.collect { update ->
                _uiState.update { state ->
                    state.copy(
                        progress = update.progress,
                        latencyHistory = (state.latencyHistory + update.latency).takeLast(60),
                        successHistory = (state.successHistory + update.isSuccess).takeLast(60),
                        avgLatency = update.avgLatency,
                        packetLoss = update.packetLoss
                    )
                }
            }
        }
    }

    private fun checkConsent(): Boolean {
        val consented = prefs.getBoolean("stability_consent", false)
        val consentTime = prefs.getLong("consent_time", 0L)
        val thirtyDaysMillis = 30L * 24 * 60 * 60 * 1000
        return consented && (System.currentTimeMillis() - consentTime < thirtyDaysMillis)
    }

    fun saveConsent() {
        prefs.edit().apply {
            putBoolean("stability_consent", true)
            putLong("consent_time", System.currentTimeMillis())
            apply()
        }
        _uiState.update { it.copy(hasConsent = true) }
    }

    fun revokeConsent() {
        prefs.edit().apply {
            putBoolean("stability_consent", false)
            apply()
        }
        _uiState.update { it.copy(hasConsent = false) }
    }

    fun setMode(mode: StabilityMode) {
        _uiState.update { it.copy(mode = mode, isExternalTarget = checkIsExternal(it.target, mode)) }
    }

    fun updateTarget(target: String) {
        _uiState.update { it.copy(target = target, isExternalTarget = checkIsExternal(target, it.mode)) }
    }

    private fun checkIsExternal(target: String, mode: StabilityMode): Boolean {
        return if (mode == StabilityMode.LOCAL_LOAD) {
            !StabilityTestManager.isPrivateIp(target)
        } else {
            true
        }
    }

    fun updateCustomUrl(url: String) {
        _uiState.update { it.copy(customUrl = url) }
    }

    fun updateThreads(threads: Int) {
        val accessManager = FeatureAccessManager.getInstance(getApplication())
        if (threads > 20 && !accessManager.isPro()) {
            showPaywallDialog = true
            _uiState.update { it.copy(threads = 20) }
            return
        }
        _uiState.update { it.copy(threads = threads) }
    }

    fun updateDuration(duration: Int) {
        _uiState.update { it.copy(duration = duration) }
    }

    fun startTest() {
        val currentState = _uiState.value
        if (currentState.isRunning) return

        val accessManager = FeatureAccessManager.getInstance(getApplication())
        if (accessManager.trackAndCheckGlobalToolUsage()) {
            showPaywallDialog = true
        }

        val accessState = accessManager.checkAccess(FeatureId.STABILITY_TEST_DAILY)
        if (accessState == FeatureAccessState.LIMIT_REACHED) {
            showPaywallDialog = true
            return
        }
        accessManager.consumeQuota(FeatureId.STABILITY_TEST_DAILY)

        val resolvedTarget = currentState.target.trim().ifBlank {
            val gateway = NetworkUtils.getGateway(getApplication())
            if (gateway != "0.0.0.0") gateway else "192.168.1.1"
        }

        _uiState.update { it.copy(
            isRunning = true,
            progress = 0f,
            latencyHistory = emptyList(),
            successHistory = emptyList(),
            avgLatency = 0.0,
            maxLatency = 0.0,
            packetLoss = 0,
            successRate = 0.0,
            showResult = false,
            target = currentState.target.ifBlank { resolvedTarget }
        ) }

        if (currentState.mode == StabilityMode.LOCAL_LOAD) {
            StabilityTestManager.runLocalLoad(
                target = resolvedTarget,
                threads = currentState.threads,
                durationSeconds = currentState.duration,
                onComplete = { avg, max, loss, rate ->
                    handleTestCompletion(avg, max, loss, rate)
                }
            )
        } else {
            StabilityTestManager.runBufferbloat(
                url = currentState.customUrl,
                durationSeconds = currentState.duration,
                onComplete = { idle, loaded, bloat, max, loss, rate ->
                    _uiState.update { it.copy(
                        idleLatency = idle,
                        loadedLatency = loaded,
                        bufferbloat = bloat
                    ) }
                    handleTestCompletion(loaded, max, loss, rate)
                }
            )
        }
    }

    private fun handleTestCompletion(avg: Double, max: Double, loss: Int, rate: Double) {
        val currentState = _uiState.value
        val result = StabilityResult(
            mode = currentState.mode,
            target = if (currentState.mode == StabilityMode.LOCAL_LOAD) currentState.target else "Internet",
            customUrl = currentState.customUrl,
            threads = currentState.threads,
            duration = currentState.duration,
            avgLatency = avg,
            maxLatency = max,
            idleLatency = currentState.idleLatency,
            loadedLatency = currentState.loadedLatency,
            bufferbloat = currentState.bufferbloat,
            packetLoss = loss,
            successRate = rate
        )

        viewModelScope.launch {
            dao.insert(result)
            _uiState.update { it.copy(
                isRunning = false,
                avgLatency = avg,
                maxLatency = max,
                packetLoss = loss,
                successRate = rate,
                showResult = true,
                lastResult = result,
                progress = 1f
            ) }
        }
    }

    fun stopTest() {
        viewModelScope.launch {
            StabilityTestManager.stopTest()
            _uiState.update { it.copy(isRunning = false) }
        }
    }
}
