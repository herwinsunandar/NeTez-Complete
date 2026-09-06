package com.herwin.netez.network

import com.herwin.netez.network.api.HibpService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

object HibpManager {
    private val service = HibpService.create()

    suspend fun checkPwnedCount(password: String): Int = withContext(Dispatchers.IO) {
        try {
            val sha1 = sha1(password)
            val prefix = sha1.substring(0, 5)
            val suffix = sha1.substring(5).uppercase()

            val response = service.getPwnedRange(prefix)
            val body = response.string()
            
            body.lineSequence().forEach { line ->
                val parts = line.split(":")
                if (parts.size == 2) {
                    if (parts[0] == suffix) {
                        return@withContext parts[1].trim().toInt()
                    }
                }
            }
            0
        } catch (e: Exception) {
            -1
        }
    }

    private fun sha1(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-1").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
