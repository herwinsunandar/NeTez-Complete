package com.herwin.netez.network

import android.content.Context
import com.herwin.netez.data.AuthType
import com.herwin.netez.data.SshHost
import com.herwin.netez.utils.EncryptionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.xfer.FileSystemFile
import net.schmizz.sshj.xfer.TransferListener
import java.io.File
import java.io.IOException

data class SftpRemoteFile(
    val name: String,
    val path: String,
    val size: Long,
    val isDirectory: Boolean,
    val permissions: Int,
    val lastModified: Long
)

class SftpManager(private val context: Context) {
    private var sshClient: SSHClient? = null
    private var sftpClient: SFTPClient? = null

    suspend fun connect(host: SshHost) = withContext(Dispatchers.IO) {
        sshClient = SSHClient()
        sshClient?.addHostKeyVerifier(PromiscuousVerifier())
        sshClient?.connect(host.host, host.port)

        when (host.authType) {
            AuthType.PASSWORD -> {
                host.passwordEncrypted?.let {
                    val password = EncryptionUtils.decrypt(it)
                    sshClient?.authPassword(host.username, password)
                } ?: throw IOException("Password not provided")
            }
            AuthType.KEY -> {
                host.privateKeyPath?.let { path ->
                    val keyFile = File(path)
                    if (keyFile.exists()) {
                        val keyProvider = sshClient?.loadKeys(path)
                        sshClient?.authPublickey(host.username, keyProvider)
                    } else {
                        throw IOException("Private key file not found: $path")
                    }
                } ?: throw IOException("Private key path not provided")
            }
        }

        sftpClient = sshClient?.newSFTPClient()
    }

    suspend fun listFiles(path: String): List<SftpRemoteFile> = withContext(Dispatchers.IO) {
        val client = sftpClient ?: throw IOException("SFTP client not connected")
        client.ls(path).map { info ->
            SftpRemoteFile(
                name = info.name,
                path = info.path,
                size = info.attributes.size,
                isDirectory = info.isDirectory,
                permissions = info.attributes.mode.mask,
                lastModified = info.attributes.mtime
            )
        }
    }

    suspend fun downloadFile(remotePath: String, localPath: String, listener: TransferListener? = null) = withContext(Dispatchers.IO) {
        val client = sftpClient ?: throw IOException("SFTP client not connected")
        client.getFileTransfer().setTransferListener(listener)
        client.get(remotePath, FileSystemFile(localPath))
    }

    suspend fun uploadFile(localPath: String, remotePath: String, listener: TransferListener? = null) = withContext(Dispatchers.IO) {
        val client = sftpClient ?: throw IOException("SFTP client not connected")
        client.getFileTransfer().setTransferListener(listener)
        client.put(FileSystemFile(localPath), remotePath)
    }

    suspend fun createDirectory(path: String) = withContext(Dispatchers.IO) {
        sftpClient?.mkdir(path)
    }

    suspend fun deleteFile(path: String) = withContext(Dispatchers.IO) {
        sftpClient?.rm(path)
    }

    suspend fun deleteDirectory(path: String) = withContext(Dispatchers.IO) {
        sftpClient?.rmdir(path)
    }

    suspend fun rename(oldPath: String, newPath: String) = withContext(Dispatchers.IO) {
        sftpClient?.rename(oldPath, newPath)
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            sftpClient?.close()
            sshClient?.disconnect()
        } catch (e: Exception) {
            // Ignore closure errors
        } finally {
            sftpClient = null
            sshClient = null
        }
    }

    fun isConnected(): Boolean = sshClient?.isConnected == true && sshClient?.isAuthenticated == true
}
