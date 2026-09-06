package com.herwin.netez.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.data.AppDatabase
import com.herwin.netez.data.TestResult
import com.herwin.netez.network.SpeedTestManager
import com.herwin.netez.utils.NetworkUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

data class SpeedTestState(
    val pingMs: Double = 0.0,
    val jitterMs: Double = 0.0,
    val downloadSpeed: Double = 0.0,
    val uploadSpeed: Double = 0.0,
    val progress: Float = 0f,
    val isRunning: Boolean = false,
    val stage: SpeedTestStage = SpeedTestStage.IDLE,
    val selectedServer: String = "speed.cloudflare.com",
    val isp: String = "Detecting...",
    val error: String? = null,
    val downloadHistory: List<Float> = emptyList(),
    val uploadHistory: List<Float> = emptyList()
)

enum class SpeedTestStage {
    IDLE, PING, DOWNLOAD, UPLOAD, FINISHED
}

class SpeedTestViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(SpeedTestState())
    val uiState: StateFlow<SpeedTestState> = _uiState
    private val dao = AppDatabase.getDatabase(application).testResultDao()

    init {
        detectIsp()
    }

    private fun detectIsp() {
        viewModelScope.launch {
            val isp = NetworkUtils.getIspInfo()
            _uiState.value = _uiState.value.copy(isp = isp)
        }
    }

    fun updateServer(server: String) {
        _uiState.value = _uiState.value.copy(selectedServer = server)
    }

    private var testJob: Job? = null

    fun stopTest() {
        testJob?.cancel()
        _uiState.value = _uiState.value.copy(isRunning = false, stage = SpeedTestStage.IDLE)
    }

    fun startTest() {
        val server = _uiState.value.selectedServer
        val downloadUrl = if (server.contains("http")) server else "https://$server/__down?bytes=50000000"
        val uploadUrl = if (server.contains("http")) server else "https://$server/__up"

        _uiState.value = _uiState.value.copy(
            isRunning = true, 
            stage = SpeedTestStage.PING,
            downloadSpeed = 0.0,
            uploadSpeed = 0.0,
            pingMs = 0.0,
            jitterMs = 0.0,
            progress = 0f,
            downloadHistory = emptyList(),
            uploadHistory = emptyList()
        )

        testJob = viewModelScope.launch {
            try {
                // 1. Ping & Jitter
                val (ping, jitter) = SpeedTestManager.runPingTest()
                _uiState.value = _uiState.value.copy(pingMs = ping, jitterMs = jitter, stage = SpeedTestStage.DOWNLOAD)
                
                // 2. Download
                SpeedTestManager.runDownload(downloadUrl) { speed, progress ->
                    _uiState.value = _uiState.value.copy(
                        downloadSpeed = speed, 
                        progress = progress,
                        downloadHistory = (_uiState.value.downloadHistory + speed.toFloat()).takeLast(60)
                    )
                }
                
                // 3. Upload
                _uiState.value = _uiState.value.copy(stage = SpeedTestStage.UPLOAD, progress = 0f)
                SpeedTestManager.runUpload(uploadUrl) { speed, progress ->
                    _uiState.value = _uiState.value.copy(
                        uploadSpeed = speed, 
                        progress = progress,
                        uploadHistory = (_uiState.value.uploadHistory + speed.toFloat()).takeLast(60)
                    )
                }
                
                _uiState.value = _uiState.value.copy(isRunning = false, stage = SpeedTestStage.FINISHED, progress = 1f)
                saveToHistory()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isRunning = false, error = e.message)
            }
        }
    }

    private fun saveToHistory() {
        val state = _uiState.value
        viewModelScope.launch {
            dao.insertResult(TestResult(
                type = "SpeedTest",
                target = state.selectedServer,
                result = String.format(Locale.US, "P: %.0fms | D: %.1f | U: %.1f Mbps", state.pingMs, state.downloadSpeed, state.uploadSpeed)
            ))
        }
    }
}
