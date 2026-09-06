package com.herwin.netez.ui.viewmodels

import android.app.Application
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.data.AppDatabase
import com.herwin.netez.data.AuthType
import com.herwin.netez.data.SshHost
import com.herwin.netez.utils.EncryptionUtils
import com.herwin.netez.utils.TerminalUtils
import com.jcraft.jsch.ChannelShell
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintStream

class SshViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val sshHostDao = db.sshHostDao()

    val savedHosts = sshHostDao.getAllHosts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _connectionState = MutableStateFlow("Disconnected")
    val connectionState: StateFlow<String> = _connectionState

    private val _terminalOutput = MutableStateFlow(AnnotatedString(""))
    val terminalOutput: StateFlow<AnnotatedString> = _terminalOutput
    
    private var session: Session? = null
    private var channel: ChannelShell? = null
    private var outputStream: OutputStream? = null
    private var connectionJob: Job? = null

    fun connect(host: SshHost) {
        connectionJob?.cancel()
        connectionJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                _connectionState.value = "Connecting..."
                updateOutput("Connecting to ${host.host}:${host.port}...\n")
                
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
                session?.connect(15000)

                _connectionState.value = "Authenticating..."
                
                channel = session?.openChannel("shell") as ChannelShell
                channel?.setPty(true)
                channel?.setPtyType("xterm-256color")
                
                val inputStream = channel?.inputStream
                outputStream = channel?.outputStream
                
                channel?.connect(5000)

                _isConnected.value = true
                _connectionState.value = "Connected"
                updateOutput("Connected.\n")
                
                sshHostDao.updateLastConnected(host.id, System.currentTimeMillis())

                // Read thread
                val buffer = ByteArray(4096)
                while (isActive && channel?.isConnected == true) {
                    if ((inputStream?.available() ?: 0) > 0) {
                        val i = inputStream?.read(buffer) ?: -1
                        if (i > 0) {
                            val text = String(buffer, 0, i)
                            updateOutput(text)
                        }
                    } else {
                        delay(50)
                    }
                    
                    if (channel?.isClosed == true) break
                }
            } catch (e: Exception) {
                updateOutput("\nError: ${e.message}\n")
                _connectionState.value = "Error: ${e.message}"
                _isConnected.value = false
            } finally {
                disconnect()
            }
        }
    }

    private fun updateOutput(text: String) {
        val current = _terminalOutput.value.text
        // Limit buffer size to avoid OOM
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
                outputStream?.write((cmd + "\r").toByteArray())
                outputStream?.flush()
            } catch (e: Exception) {
                updateOutput("\nFailed to send command: ${e.message}\n")
            }
        }
    }
    
    fun sendSpecialKey(key: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bytes = when (key) {
                    "ESC" -> byteArrayOf(27)
                    "TAB" -> byteArrayOf(9)
                    "CTRL-C" -> byteArrayOf(3)
                    "CTRL-D" -> byteArrayOf(4)
                    "CTRL-Z" -> byteArrayOf(26)
                    else -> null
                }
                bytes?.let {
                    outputStream?.write(it)
                    outputStream?.flush()
                }
            } catch (e: Exception) {}
        }
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

    fun disconnect() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                channel?.disconnect()
                session?.disconnect()
            } catch (e: Exception) {}
            _isConnected.value = false
            _connectionState.value = "Disconnected"
        }
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}
