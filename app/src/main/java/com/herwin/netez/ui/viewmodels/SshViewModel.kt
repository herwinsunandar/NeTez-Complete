package com.herwin.netez.ui.viewmodels

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jcraft.jsch.ChannelShell
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintStream

class SshViewModel : ViewModel() {
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    val output = mutableStateListOf<String>()
    
    private var session: Session? = null
    private var channel: ChannelShell? = null
    private var outputStream: OutputStream? = null

    fun connect(host: String, user: String, pass: String, port: Int = 22) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsch = JSch()
                session = jsch.getSession(user, host, port)
                session?.setPassword(pass)
                session?.setConfig("StrictHostKeyChecking", "no")
                session?.connect(15000)

                channel = session?.openChannel("shell") as ChannelShell
                val inputStream = channel?.inputStream
                outputStream = channel?.outputStream
                channel?.connect()

                _isConnected.value = true
                
                // Read thread
                launch {
                    val buffer = ByteArray(1024)
                    var i: Int
                    while (channel?.isConnected == true) {
                        i = inputStream?.read(buffer) ?: -1
                        if (i <= 0) break
                        val text = String(buffer, 0, i)
                        withContext(Dispatchers.Main) {
                            output.add(text)
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    output.add("Error: ${e.message}")
                }
            }
        }
    }

    fun sendCommand(cmd: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                outputStream?.write((cmd + "\n").toByteArray())
                outputStream?.flush()
            } catch (e: Exception) {}
        }
    }

    fun disconnect() {
        channel?.disconnect()
        session?.disconnect()
        _isConnected.value = false
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}
