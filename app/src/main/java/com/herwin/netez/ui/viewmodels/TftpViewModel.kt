package com.herwin.netez.ui.viewmodels

import android.app.Application
import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.network.TftpManager
import com.herwin.netez.network.TftpMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class TftpHistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val host: String,
    val file: String,
    val type: String, // GET/PUT
    val status: String // SUCCESS/FAILED
)

data class TftpState(
    val serverIp: String = "",
    val port: String = "69",
    val remoteFileName: String = "",
    val localPath: String = "",
    val mode: TftpMode = TftpMode.OCTET,
    val isTransferring: Boolean = false,
    val progress: Float = 0f,
    val log: String = "Ready to transfer...",
    val history: List<TftpHistoryItem> = emptyList(),
    val error: String? = null
)

class TftpViewModel(application: Application) : AndroidViewModel(application) {
    private val manager = TftpManager(application)
    private val prefs = application.getSharedPreferences("tftp_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(TftpState())
    val uiState: StateFlow<TftpState> = _uiState

    init {
        loadHistory()
    }

    fun updateServerIp(ip: String) { _uiState.update { it.copy(serverIp = ip) } }
    fun updatePort(port: String) { _uiState.update { it.copy(port = port) } }
    fun updateRemoteFileName(name: String) { _uiState.update { it.copy(remoteFileName = name) } }
    fun updateLocalPath(path: String) { _uiState.update { it.copy(localPath = path) } }
    fun updateMode(mode: TftpMode) { _uiState.update { it.copy(mode = mode) } }

    fun startTransfer(isGet: Boolean) {
        val state = _uiState.value
        if (state.serverIp.isBlank() || state.remoteFileName.isBlank() || state.localPath.isBlank()) {
            _uiState.update { it.copy(error = "Please fill all fields") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(
                isTransferring = true, 
                progress = 0.1f, 
                log = "TFTP ${if (isGet) "GET" else "PUT"} started...\nConnecting to ${state.serverIp}:${state.port}",
                error = null
            ) }

            val localFile = File(state.localPath)
            val result = if (isGet) {
                manager.getFile(state.serverIp, state.port.toIntOrNull() ?: 69, state.remoteFileName, localFile, state.mode) {
                }
            } else {
                manager.putFile(state.serverIp, state.port.toIntOrNull() ?: 69, state.remoteFileName, localFile, state.mode) {
                }
            }

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(
                        isTransferring = false,
                        progress = 1.0f,
                        log = "Transfer successful!\nFile: ${state.remoteFileName}\nPath: ${state.localPath}"
                    ) }
                    saveHistory(state.serverIp, state.remoteFileName, if (isGet) "GET" else "PUT", "SUCCESS")
                },
                onFailure = { e ->
                    val errorMsg = when {
                        e.message?.contains("File not found", true) == true -> "File not found on server"
                        e.message?.contains("Access violation", true) == true -> "Access violation"
                        else -> e.localizedMessage ?: "Unknown error"
                    }
                    _uiState.update { it.copy(
                        isTransferring = false,
                        progress = 0f,
                        log = "Transfer failed!\nError: $errorMsg",
                        error = errorMsg
                    ) }
                    saveHistory(state.serverIp, state.remoteFileName, if (isGet) "GET" else "PUT", "FAILED")
                }
            )
        }
    }

    private fun saveHistory(host: String, file: String, type: String, status: String) {
        val newItem = TftpHistoryItem(host = host, file = file, type = type, status = status)
        val current = _uiState.value.history.toMutableList()
        current.add(0, newItem)
        val limited = current.take(10)
        
        _uiState.update { it.copy(history = limited) }
        
        val historyStrings = limited.map { "${it.timestamp}|${it.host}|${it.file}|${it.type}|${it.status}" }.toSet()
        prefs.edit().putStringSet("history", historyStrings).apply()
    }

    private fun loadHistory() {
        val historyStrings = prefs.getStringSet("history", emptySet()) ?: emptySet()
        val loaded = historyStrings.map {
            val parts = it.split("|")
            TftpHistoryItem(
                timestamp = parts[0].toLong(),
                host = parts[1],
                file = parts[2],
                type = parts[3],
                status = parts[4]
            )
        }.sortedByDescending { it.timestamp }
        _uiState.update { it.copy(history = loaded) }
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }
}
