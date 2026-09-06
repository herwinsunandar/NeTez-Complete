package com.herwin.netez.network

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.tftp.TFTP
import org.apache.commons.net.tftp.TFTPClient
import java.io.InputStream
import java.io.OutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

enum class TftpMode {
    OCTET, NETASCII
}

class TftpManager(private val context: Context) {

    suspend fun getFile(
        host: String,
        port: Int,
        remoteFile: String,
        localFile: File,
        mode: TftpMode,
        onProgress: (Long) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val client = TFTPClient()
        client.defaultTimeout = 5000
        client.maxTimeouts = 3
        
        try {
            client.open()
            val transferMode = if (mode == TftpMode.OCTET) TFTP.BINARY_MODE else TFTP.ASCII_MODE
            
            FileOutputStream(localFile).use { outputStream ->
                client.receiveFile(remoteFile, transferMode, outputStream, host, port)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            client.close()
        }
    }

    suspend fun putFile(
        host: String,
        port: Int,
        remoteFile: String,
        localFile: File,
        mode: TftpMode,
        onProgress: (Long) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val client = TFTPClient()
        client.defaultTimeout = 5000
        client.maxTimeouts = 3

        try {
            client.open()
            val transferMode = if (mode == TftpMode.OCTET) TFTP.BINARY_MODE else TFTP.ASCII_MODE
            
            FileInputStream(localFile).use { inputStream ->
                client.sendFile(remoteFile, transferMode, inputStream, host, port)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            client.close()
        }
    }
}
