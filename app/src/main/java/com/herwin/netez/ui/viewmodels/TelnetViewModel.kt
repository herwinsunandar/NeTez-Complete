package com.herwin.netez.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.utils.TerminalUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

class TelnetViewModel(application: Application) : AndroidViewModel(application) {

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _connectionState = MutableStateFlow("Disconnected")
    val connectionState: StateFlow<String> = _connectionState

    private val _terminalOutput = MutableStateFlow(AnnotatedString(""))
    val terminalOutput: StateFlow<AnnotatedString> = _terminalOutput

    var errorMessage by mutableStateOf<String?>(null)

    private var socket: Socket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var connectionJob: Job? = null

    fun connect(host: String, portStr: String) {
        val targetHost = host.trim()
        val targetPort = portStr.trim().toIntOrNull() ?: 23

        if (targetHost.isBlank()) {
            errorMessage = "Please enter a valid Host or IP address"
            return
        }

        connectionJob?.cancel()
        connectionJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                _connectionState.value = "Connecting..."
                updateOutput("Connecting to TCP Socket $targetHost:$targetPort...\n")

                socket = Socket()
                socket?.soTimeout = 0 // Infinite read timeout for interactive session
                socket?.connect(InetSocketAddress(targetHost, targetPort), 10000) // 10s connection timeout

                inputStream = socket?.getInputStream()
                outputStream = socket?.getOutputStream()

                _isConnected.value = true
                _connectionState.value = "Connected"
                updateOutput("Connected to $targetHost:$targetPort.\n\n")

                val buffer = ByteArray(4096)
                while (isActive && socket?.isConnected == true && !socket!!.isClosed) {
                    val available = try { inputStream?.available() ?: 0 } catch (e: Exception) { 0 }
                    if (available > 0) {
                        val count = inputStream?.read(buffer) ?: -1
                        if (count > 0) {
                            val text = String(buffer, 0, count)
                            updateOutput(text)
                        } else if (count == -1) {
                            break
                        }
                    } else {
                        delay(50)
                    }
                }
            } catch (e: Exception) {
                val rawMsg = e.message ?: ""
                val friendlyError = when {
                    rawMsg.contains("timeout", ignoreCase = true) -> "Connection timeout: $targetHost:$targetPort is unreachable."
                    rawMsg.contains("refused", ignoreCase = true) -> "Connection refused: Port $targetPort closed or Telnet service down."
                    else -> "Telnet connection failed: ${e.localizedMessage ?: "Network error"}"
                }
                updateOutput("\nError: $friendlyError\n")
                _connectionState.value = "Error: $friendlyError"
                errorMessage = friendlyError
                _isConnected.value = false
            } finally {
                closeSocketsOnly()
            }
        }
    }

    private fun updateOutput(text: String) {
        val current = _terminalOutput.value.text
        val newText = if (current.length > 20000) {
            current.substring(current.length - 10000) + text
        } else {
            current + text
        }
        _terminalOutput.value = TerminalUtils.parseAnsi(newText)
    }

    fun sendCommand(cmd: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                outputStream?.write((cmd + "\r\n").toByteArray())
                outputStream?.flush()
            } catch (e: Exception) {
                updateOutput("\nFailed to send command: ${e.message}\n")
            }
        }
    }

    fun sendSpecialKey(key: String, currentInput: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (currentInput.isNotEmpty() && (key == "TAB" || key == "ENTER ↵" || key == "▲ UP" || key == "▼ DOWN")) {
                    outputStream?.write(currentInput.toByteArray())
                    outputStream?.flush()
                }

                val bytes = when (key) {
                    "ENTER ↵" -> "\r\n".toByteArray()
                    "ESC" -> byteArrayOf(27)
                    "TAB" -> byteArrayOf(9)
                    "CTRL-C" -> byteArrayOf(3)
                    "CTRL-D" -> byteArrayOf(4)
                    "CTRL-Z" -> byteArrayOf(26)
                    "CTRL-L" -> byteArrayOf(12)
                    "▲ UP" -> byteArrayOf(27, 91, 65)
                    "▼ DOWN" -> byteArrayOf(27, 91, 66)
                    "PIPE" -> "|".toByteArray()
                    else -> null
                }
                bytes?.let {
                    outputStream?.write(it)
                    outputStream?.flush()
                }
            } catch (e: Exception) {
                Log.e("TelnetViewModel", "Failed to send special key: ${e.message}")
            }
        }
    }

    private fun closeSocketsOnly() {
        try {
            inputStream?.close()
            outputStream?.close()
            socket?.close()
        } catch (e: Exception) {}
        inputStream = null
        outputStream = null
        socket = null
    }

    fun disconnect() {
        viewModelScope.launch(Dispatchers.IO) {
            closeSocketsOnly()
            _isConnected.value = false
            _connectionState.value = "Disconnected"
        }
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}
