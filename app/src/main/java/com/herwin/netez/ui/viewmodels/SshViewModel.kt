package com.herwin.netez.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.data.AppDatabase
import com.herwin.netez.data.AuthType
import com.herwin.netez.data.SshHost
import com.herwin.netez.utils.EncryptionUtils
import com.jcraft.jsch.ChannelShell
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

class SshViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val sshHostDao = db.sshHostDao()

    val savedHosts = sshHostDao.getAllHosts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _connectionState = MutableStateFlow("Disconnected")
    val connectionState: StateFlow<String> = _connectionState

    var errorMessage by mutableStateOf<String?>(null)

    var currentHost by mutableStateOf<SshHost?>(null)
        private set

    private var session: Session? = null
    private var channel: ChannelShell? = null
    var inputStream: InputStream? = null
        private set
    var outputStream: OutputStream? = null
        private set

    private var connectionJob: Job? = null
    private var readJob: Job? = null

    // Buffer to capture initial SSH server welcome banner / MOTD before view attaches
    private val pendingDataBuffer = ByteArrayOutputStream()

    // Callback to pass raw network stream bytes to Terminal Emulator
    var onDataReceived: ((ByteArray, Int) -> Unit)? = null
        set(value) {
            field = value
            if (value != null) {
                synchronized(pendingDataBuffer) {
                    val bytes = pendingDataBuffer.toByteArray()
                    if (bytes.isNotEmpty()) {
                        value.invoke(bytes, bytes.size)
                        pendingDataBuffer.reset()
                    }
                }
            }
        }

    fun connect(host: SshHost, initialCols: Int = 80, initialRows: Int = 24) {
        currentHost = host
        synchronized(pendingDataBuffer) {
            pendingDataBuffer.reset()
        }

        connectionJob?.cancel()
        connectionJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                _connectionState.value = "Connecting..."

                val jsch = JSch()

                if (host.authType == AuthType.KEY && host.privateKeyPath != null) {
                    val passphrase = host.passwordEncrypted?.let { EncryptionUtils.decrypt(it) }
                    jsch.addIdentity(host.privateKeyPath, passphrase)
                }

                session = jsch.getSession(host.username, host.host, host.port)

                if (host.authType == AuthType.PASSWORD && host.passwordEncrypted != null) {
                    session?.setPassword(EncryptionUtils.decrypt(host.passwordEncrypted))
                }

                session?.setConfig("StrictHostKeyChecking", "no")
                session?.setServerAliveInterval(30000) // Keep alive every 30s
                session?.connect(10000) // 10s timeout

                _connectionState.value = "Authenticating..."

                channel = session?.openChannel("shell") as ChannelShell
                channel?.setPty(true)
                channel?.setPtyType("xterm-256color")
                channel?.setPtySize(initialCols, initialRows, 0, 0)

                inputStream = channel?.inputStream
                outputStream = channel?.outputStream

                channel?.connect(5000)

                _isConnected.value = true
                _connectionState.value = "Connected"

                sshHostDao.updateLastConnected(host.id, System.currentTimeMillis())

                // Start I/O read loop on IO dispatcher
                readJob = launch(Dispatchers.IO) {
                    val buffer = ByteArray(8192)
                    val inStream = inputStream
                    while (isActive && channel?.isConnected == true) {
                        try {
                            val count = inStream?.read(buffer) ?: -1
                            if (count > 0) {
                                val listener = onDataReceived
                                if (listener != null) {
                                    synchronized(pendingDataBuffer) {
                                        if (pendingDataBuffer.size() > 0) {
                                            val buffered = pendingDataBuffer.toByteArray()
                                            listener.invoke(buffered, buffered.size)
                                            pendingDataBuffer.reset()
                                        }
                                    }
                                    listener.invoke(buffer, count)
                                } else {
                                    synchronized(pendingDataBuffer) {
                                        pendingDataBuffer.write(buffer, 0, count)
                                    }
                                }
                            } else if (count == -1) {
                                break
                            }
                        } catch (e: Exception) {
                            Log.e("SshViewModel", "Read error: ${e.message}")
                            break
                        }
                    }
                    if (_isConnected.value) {
                        _connectionState.value = "Disconnected"
                        _isConnected.value = false
                    }
                }

            } catch (e: Exception) {
                val rawMsg = e.message ?: ""
                val friendlyError = when {
                    rawMsg.contains("Auth fail", ignoreCase = true) || rawMsg.contains("Auth cancel", ignoreCase = true) ->
                        "Authentication failed: Wrong password or invalid key."
                    rawMsg.contains("timeout", ignoreCase = true) || rawMsg.contains("timed out", ignoreCase = true) ->
                        "Connection timeout: Server ${host.host}:${host.port} is unreachable."
                    rawMsg.contains("refused", ignoreCase = true) ->
                        "Connection refused: Port ${host.port} closed or SSH service down."
                    else -> "Connection failed: ${e.localizedMessage ?: "Network error"}"
                }
                _connectionState.value = "Error: $friendlyError"
                errorMessage = friendlyError
                _isConnected.value = false
            }
        }
    }

    fun updateWindowSize(cols: Int, rows: Int, widthPx: Int = 0, heightPx: Int = 0) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (channel?.isConnected == true) {
                    channel?.setPtySize(cols, rows, widthPx, heightPx)
                }
            } catch (e: Exception) {
                Log.e("SshViewModel", "Failed to update window size: ${e.message}")
            }
        }
    }

    fun sendBytes(data: ByteArray) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                outputStream?.write(data)
                outputStream?.flush()
            } catch (e: Exception) {
                Log.e("SshViewModel", "Failed to send bytes: ${e.message}")
            }
        }
    }

    fun sendCommand(cmd: String) {
        sendBytes((cmd + "\r").toByteArray())
    }

    fun saveHost(host: SshHost) {
        viewModelScope.launch(Dispatchers.IO) {
            sshHostDao.insertHost(host)
        }
    }

    fun updateHost(host: SshHost) {
        viewModelScope.launch(Dispatchers.IO) {
            sshHostDao.updateHost(host)
        }
    }

    fun deleteHost(host: SshHost) {
        viewModelScope.launch(Dispatchers.IO) {
            sshHostDao.deleteHost(host)
        }
    }

    private fun closeSocketsOnly() {
        try {
            readJob?.cancel()
            channel?.disconnect()
            session?.disconnect()
        } catch (e: Exception) {}
        channel = null
        session = null
        outputStream = null
        inputStream = null
        synchronized(pendingDataBuffer) {
            pendingDataBuffer.reset()
        }
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
