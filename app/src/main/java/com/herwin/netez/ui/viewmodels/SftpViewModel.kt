package com.herwin.netez.ui.viewmodels

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.data.AppDatabase
import com.herwin.netez.data.SshHost
import com.herwin.netez.network.SftpManager
import com.herwin.netez.network.SftpRemoteFile
import kotlinx.coroutines.launch
import net.schmizz.sshj.common.StreamCopier
import net.schmizz.sshj.xfer.TransferListener
import java.util.*

class SftpViewModel(application: Application) : AndroidViewModel(application) {
    private val hostDao = AppDatabase.getDatabase(application).sshHostDao()
    private val sftpManager = SftpManager(application)

    val allHosts = hostDao.getAllHosts()

    var isConnected by mutableStateOf(false)
        private set

    var currentPath by mutableStateOf("/")
        private set

    var files = mutableStateListOf<SftpRemoteFile>()
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var selectedHost by mutableStateOf<SshHost?>(null)
        private set

    var transferProgress by mutableStateOf(0f)
        private set

    var isTransferring by mutableStateOf(false)
        private set

    fun connect(host: SshHost) {
        selectedHost = host
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                sftpManager.connect(host)
                isConnected = true
                currentPath = "/"
                refreshFiles()
                hostDao.updateLastConnected(host.id, System.currentTimeMillis())
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: ""
                errorMessage = if (msg.contains("X25519", ignoreCase = true)) {
                    "Server uses modern encryption (X25519) not supported by system default. BouncyCastle provider update may be required."
                } else {
                    "Connection failed: ${e.localizedMessage}"
                }
                isConnected = false
            } finally {
                isLoading = false
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            sftpManager.disconnect()
            isConnected = false
            files.clear()
            selectedHost = null
            currentPath = "/"
        }
    }

    fun navigateTo(path: String) {
        currentPath = path
        refreshFiles()
    }

    fun navigateUp() {
        if (currentPath == "/" || currentPath.isEmpty()) return
        val path = if (currentPath.endsWith("/")) currentPath.dropLast(1) else currentPath
        val lastSlash = path.lastIndexOf('/')
        val newPath = if (lastSlash == -1) "/" else path.substring(0, lastSlash + 1)
        navigateTo(newPath)
    }

    fun refreshFiles() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val list = sftpManager.listFiles(currentPath)
                files.clear()
                files.addAll(list.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase(Locale.ROOT) })))
            } catch (e: Exception) {
                errorMessage = "Failed to list files: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }

    fun createDirectory(name: String) {
        viewModelScope.launch {
            try {
                val path = if (currentPath.endsWith("/")) "$currentPath$name" else "$currentPath/$name"
                sftpManager.createDirectory(path)
                refreshFiles()
            } catch (e: Exception) {
                errorMessage = "Failed to create directory: ${e.localizedMessage}"
            }
        }
    }

    fun deleteFile(file: SftpRemoteFile) {
        viewModelScope.launch {
            try {
                if (file.isDirectory) {
                    sftpManager.deleteDirectory(file.path)
                } else {
                    sftpManager.deleteFile(file.path)
                }
                refreshFiles()
            } catch (e: Exception) {
                errorMessage = "Failed to delete: ${e.localizedMessage}"
            }
        }
    }

    fun rename(file: SftpRemoteFile, newName: String) {
        viewModelScope.launch {
            try {
                val parent = file.path.substring(0, file.path.lastIndexOf('/') + 1)
                val newPath = "$parent$newName"
                sftpManager.rename(file.path, newPath)
                refreshFiles()
            } catch (e: Exception) {
                errorMessage = "Failed to rename: ${e.localizedMessage}"
            }
        }
    }

    fun downloadFile(file: SftpRemoteFile, localPath: String) {
        viewModelScope.launch {
            isTransferring = true
            transferProgress = 0f
            try {
                sftpManager.downloadFile(file.path, localPath, object : TransferListener {
                    override fun directory(name: String?): TransferListener {
                        return this
                    }

                    override fun file(name: String?, size: Long): StreamCopier.Listener {
                        return object : StreamCopier.Listener {
                            override fun reportProgress(transferred: Long) {
                                if (file.size > 0) {
                                    transferProgress = transferred.toFloat() / file.size
                                }
                            }
                        }
                    }
                })
            } catch (e: Exception) {
                errorMessage = "Download failed: ${e.localizedMessage}"
            } finally {
                isTransferring = false
            }
        }
    }

    fun clearError() {
        errorMessage = null
    }

    fun saveHost(host: SshHost) {
        viewModelScope.launch {
            hostDao.insertHost(host)
        }
    }

    fun updateHost(host: SshHost) {
        viewModelScope.launch {
            hostDao.updateHost(host)
        }
    }

    fun deleteHost(host: SshHost) {
        viewModelScope.launch {
            hostDao.deleteHost(host)
        }
    }
}
