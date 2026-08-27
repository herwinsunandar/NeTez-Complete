package com.herwin.netez.network

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import java.net.InetAddress

data class BlacklistResult(
    val name: String,
    val domain: String,
    val status: BlacklistStatus,
    val responseTime: Long,
    val resultIp: String? = null
)

enum class BlacklistStatus {
    LISTED, CLEAN, ERROR
}

object BlacklistManager {
    private val rbls = listOf(
        "Spamhaus ZEN" to "zen.spamhaus.org",
        "Spamcop" to "bl.spamcop.net",
        "Sorbs Aggregate" to "dnsbl.sorbs.net",
        "Composite Blocking List" to "cbl.abuseat.org",
        "Hostkarma" to "host.junkemailfilter.com",
        "Uceprotect Level 1" to "dnsbl-1.uceprotect.net",
        "Inps DNSBL" to "dnsbl.inps.de",
        "NiX Spam" to "ix.dnsbl.manitu.net"
    )

    suspend fun checkIp(ip: String): List<BlacklistResult> = coroutineScope {
        if (!isValidIp(ip)) return@coroutineScope emptyList()
        if (isPrivateIp(ip)) return@coroutineScope rbls.map { 
            BlacklistResult(it.first, it.second, BlacklistStatus.ERROR, 0, "Private IP") 
        }

        val reversedIp = reverseIp(ip)
        
        rbls.map { (name, domain) ->
            async {
                val queryDomain = "$reversedIp.$domain"
                val startTime = System.currentTimeMillis()
                
                val response = withTimeoutOrNull(3000) {
                    DnsManager.lookup(queryDomain)
                }
                
                val endTime = System.currentTimeMillis()
                val latency = endTime - startTime
                
                if (response == null || response.records.isEmpty()) {
                    BlacklistResult(name, domain, BlacklistStatus.CLEAN, latency)
                } else {
                    val resultIp = response.records.first().data
                    if (resultIp.startsWith("127.0.0.")) {
                        BlacklistResult(name, domain, BlacklistStatus.LISTED, latency, resultIp)
                    } else {
                        BlacklistResult(name, domain, BlacklistStatus.CLEAN, latency, resultIp)
                    }
                }
            }
        }.awaitAll()
    }

    private fun reverseIp(ip: String): String {
        return ip.split(".").reversed().joinToString(".")
    }

    private fun isValidIp(ip: String): Boolean {
        return try {
            val parts = ip.split(".")
            if (parts.size != 4) return false
            parts.all { it.toInt() in 0..255 }
        } catch (e: Exception) {
            false
        }
    }

    private fun isPrivateIp(ip: String): Boolean {
        return try {
            val address = InetAddress.getByName(ip)
            address.isSiteLocalAddress || address.isLoopbackAddress || address.isLinkLocalAddress
        } catch (e: Exception) {
            false
        }
    }
}
