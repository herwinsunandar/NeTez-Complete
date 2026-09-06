package com.herwin.netez.network

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap

data class BlacklistResult(
    val name: String,
    val domain: String,
    val status: BlacklistStatus,
    val responseTime: Long,
    val resultIp: String? = null,
    val reason: String? = null,
    val weight: Int = 0,
    val category: BlacklistCategory = BlacklistCategory.IP
)

enum class BlacklistCategory {
    IP, DOMAIN
}

enum class BlacklistStatus {
    LISTED, CLEAN, TIMEOUT, ERROR
}

data class RblInfo(val name: String, val domain: String, val weight: Int)

object BlacklistManager {
    private val ipRbls = listOf(
        RblInfo("Spamhaus ZEN", "zen.spamhaus.org", 40),
        RblInfo("Spamcop", "bl.spamcop.net", 20),
        RblInfo("Sorbs Aggregate", "dnsbl.sorbs.net", 15),
        RblInfo("Composite Blocking List", "cbl.abuseat.org", 10),
        RblInfo("Hostkarma", "host.junkemailfilter.com", 5),
        RblInfo("Uceprotect L1", "dnsbl-1.uceprotect.net", 15),
        RblInfo("Inps DNSBL", "dnsbl.inps.de", 5),
        RblInfo("NiX Spam", "ix.dnsbl.manitu.net", 5),
        RblInfo("Blocklist.de", "bl.blocklist.de", 5)
    )

    private val domainRbls = listOf(
        RblInfo("SURBL (Multi)", "multi.surbl.org", 30),
        RblInfo("SURBL Spam", "ws.surbl.org", 20),
        RblInfo("Spamhaus DBL", "dbl.spamhaus.org", 40),
        RblInfo("Spamhaus SBL", "multi.sbl.spamhaus.org", 30),
        RblInfo("Woody Domain BL", "blacklist.woody.ch", 10),
        RblInfo("SPFBL", "dnsbl.spfbl.net", 10)
    )

    private val cache = ConcurrentHashMap<String, CachedResult>()
    private const val CACHE_DURATION = 3600 * 1000L // 1 hour

    data class CachedResult(val results: List<BlacklistResult>, val timestamp: Long)

    suspend fun checkIp(ip: String): List<BlacklistResult> = coroutineScope {
        if (!isValidIp(ip)) return@coroutineScope emptyList()
        
        // Check Cache
        val now = System.currentTimeMillis()
        cache[ip]?.let { cached ->
            if (now - cached.timestamp < CACHE_DURATION) {
                return@coroutineScope cached.results
            }
        }

        if (isPrivateIp(ip)) return@coroutineScope ipRbls.map { 
            BlacklistResult(it.name, it.domain, BlacklistStatus.ERROR, 0, reason = "Private IP", category = BlacklistCategory.IP) 
        }

        val reversedIp = reverseIp(ip)
        
        val results = ipRbls.map { rbl ->
            async {
                val queryDomain = "$reversedIp.${rbl.domain}"
                val startTime = System.currentTimeMillis()
                
                val response = withTimeoutOrNull(3000) {
                    DnsManager.lookup(queryDomain)
                }
                
                val endTime = System.currentTimeMillis()
                val latency = endTime - startTime
                
                if (response == null) {
                    BlacklistResult(rbl.name, rbl.domain, BlacklistStatus.TIMEOUT, latency, weight = rbl.weight, category = BlacklistCategory.IP)
                } else if (response.status == 3 || response.records.isEmpty()) {
                    BlacklistResult(rbl.name, rbl.domain, BlacklistStatus.CLEAN, latency, weight = rbl.weight, category = BlacklistCategory.IP)
                } else {
                    val resultIp = response.records.first().data
                    parseDnsblResponse(rbl, resultIp, latency, BlacklistCategory.IP)
                }
            }
        }.awaitAll()

        // Update Cache
        cache[ip] = CachedResult(results, now)
        
        results
    }

    suspend fun checkDomain(domainName: String): List<BlacklistResult> = coroutineScope {
        val cleanDomain = domainName.lowercase().trim()
            .removePrefix("https://").removePrefix("http://")
            .split("/")[0]

        // Check Cache
        val now = System.currentTimeMillis()
        cache[cleanDomain]?.let { cached ->
            if (now - cached.timestamp < CACHE_DURATION) {
                return@coroutineScope cached.results
            }
        }
        
        val results = domainRbls.map { rbl ->
            async {
                val queryDomain = "$cleanDomain.${rbl.domain}"
                val startTime = System.currentTimeMillis()
                
                val response = withTimeoutOrNull(3000) {
                    DnsManager.lookup(queryDomain)
                }
                
                val endTime = System.currentTimeMillis()
                val latency = endTime - startTime
                
                if (response == null) {
                    BlacklistResult(rbl.name, rbl.domain, BlacklistStatus.TIMEOUT, latency, weight = rbl.weight, category = BlacklistCategory.DOMAIN)
                } else if (response.status == 3 || response.records.isEmpty()) {
                    BlacklistResult(rbl.name, rbl.domain, BlacklistStatus.CLEAN, latency, weight = rbl.weight, category = BlacklistCategory.DOMAIN)
                } else {
                    val resultIp = response.records.first().data
                    parseDnsblResponse(rbl, resultIp, latency, BlacklistCategory.DOMAIN)
                }
            }
        }.awaitAll()

        // Update Cache
        cache[cleanDomain] = CachedResult(results, now)
        
        results
    }

    private fun parseDnsblResponse(rbl: RblInfo, resultIp: String, latency: Long, category: BlacklistCategory): BlacklistResult {
        if (resultIp == "127.255.255.254") {
            return BlacklistResult(rbl.name, rbl.domain, BlacklistStatus.TIMEOUT, latency, resultIp, "Rate limited", rbl.weight, category)
        }

        if (!resultIp.startsWith("127.")) {
            return BlacklistResult(rbl.name, rbl.domain, BlacklistStatus.CLEAN, latency, resultIp, weight = rbl.weight, category = category)
        }

        var reason = "Listed"
        if (rbl.domain == "zen.spamhaus.org") {
            reason = when (resultIp) {
                "127.0.0.2" -> "SBL"
                "127.0.0.3" -> "SBL+XBL"
                "127.0.0.4" -> "XBL"
                "127.0.0.9" -> "SBL"
                "127.0.0.10" -> "PBL"
                "127.0.0.11" -> "PBL"
                else -> "Listed"
            }
        } else if (rbl.domain == "dbl.spamhaus.org") {
            reason = when (resultIp) {
                "127.0.1.2" -> "Spam Domain"
                "127.0.1.4" -> "Phishing Domain"
                "127.0.1.5" -> "Malware Domain"
                "127.0.1.6" -> "Botnet C&C Domain"
                "127.0.1.102" -> "Abused Legitimate Spam"
                "127.0.1.104" -> "Abused Legitimate Phishing"
                "127.0.1.105" -> "Abused Legitimate Malware"
                "127.0.1.106" -> "Abused Legitimate Botnet"
                else -> "Listed"
            }
        } else if (rbl.domain.contains("surbl.org")) {
            reason = when (resultIp) {
                "127.0.0.2" -> "Spam (sc)"
                "127.0.0.4" -> "Spam (ws)"
                "127.0.0.8" -> "Phishing (ph)"
                "127.0.0.16" -> "Malware (mw)"
                "127.0.0.32" -> "Abuse (ab)"
                "127.0.0.64" -> "Cracked (cr)"
                else -> "Listed"
            }
        }

        return BlacklistResult(
            name = rbl.name,
            domain = rbl.domain,
            status = BlacklistStatus.LISTED,
            responseTime = latency,
            resultIp = resultIp,
            reason = "$reason ($resultIp)",
            weight = rbl.weight,
            category = category
        )
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

    fun calculateRiskScore(dnsResults: List<BlacklistResult>, abuseIpDbScore: Int?): Float {
        val dnsblSum = dnsResults.filter { it.status == BlacklistStatus.LISTED }.sumOf { it.weight }
        val dnsblScore = minOf(dnsblSum, 100).toFloat()
        
        val abuseScore = abuseIpDbScore?.toFloat() ?: 0f
        
        return (dnsblScore * 0.5f) + (abuseScore * 0.5f)
    }
}
