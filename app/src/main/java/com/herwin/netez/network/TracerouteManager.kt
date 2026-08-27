package com.herwin.netez.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class Hop(
    val number: Int,
    val ip: String = "*",
    val latency: String = "*",
    val hostname: String = "-"
)

object TracerouteManager {
    private val IP_REGEX = Pattern.compile("((?:[0-9]{1,3}\\.){3}[0-9]{1,3})")
    private val TIME_PATTERN = Pattern.compile("time=([0-9.]+)")

    fun trace(host: String, maxHops: Int = 30): Flow<Hop> = flow {
        for (i in 1..maxHops) {
            val hop = pingAndEnrich(host, i)
            emit(hop)
            if (hop.ip == host || (hop.ip != "*" && isFinalDestination(hop.ip, host))) {
                break
            }
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun pingAndEnrich(host: String, ttl: Int): Hop = withContext(Dispatchers.IO) {
        var hop = ping(host, ttl)
        if (hop.ip != "*" && hop.hostname == "-") {
            try {
                val address = InetAddress.getByName(hop.ip)
                val canonical = address.canonicalHostName
                if (canonical != hop.ip) {
                    hop = hop.copy(hostname = canonical)
                }
            } catch (e: Exception) {}
        }
        hop
    }

    private fun ping(host: String, ttl: Int): Hop {
        val startTime = System.currentTimeMillis()
        try {
            // Use -t for TTL on most Android/Linux ping versions
            val command = "ping -c 1 -t $ttl -W 2 $host"
            val process = Runtime.getRuntime().exec(command)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            var ip = "*"
            var latency = "*"
            var hostname = "-"

            while (reader.readLine().also { line = it } != null) {
                line?.let { l ->
                    val lowerL = l.lowercase()
                    // Detect either destination reached OR intermediate hop (TTL exceeded)
                    if (lowerL.contains("from") || lowerL.contains("time to live exceeded") || lowerL.contains("exceeded")) {
                        val duration = System.currentTimeMillis() - startTime
                        latency = duration.toString()

                        // Extract IP Address using improved regex
                        val ipMatcher = IP_REGEX.matcher(l)
                        if (ipMatcher.find()) {
                            ip = ipMatcher.group(1) ?: "*"
                        }

                        // Try to extract hostname if present in parentheses or before IP
                        if (l.contains("(")) {
                            val parts = l.split(" ")
                            val fromIndex = parts.indexOfFirst { it.equals("from", true) }
                            if (fromIndex != -1 && fromIndex + 1 < parts.size) {
                                val potentialHost = parts[fromIndex + 1]
                                if (potentialHost.contains(".") && !potentialHost.contains("(")) {
                                    hostname = potentialHost
                                }
                            }
                        }
                        
                        // If "time=" is explicitly in the output, use that for better accuracy
                        val timeMatcher = TIME_PATTERN.matcher(l)
                        if (timeMatcher.find()) {
                            latency = timeMatcher.group(1) ?: latency
                        }
                    }
                }
            }
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                process.waitFor(2500, TimeUnit.MILLISECONDS)
            } else {
                process.waitFor()
            }
            
            if (hostname == ip || hostname.isEmpty()) hostname = "-"
            
            return Hop(ttl, ip, latency, hostname)
        } catch (e: Exception) {
            return Hop(ttl, "*", "*", "-")
        }
    }

    private fun isFinalDestination(hopIp: String, target: String): Boolean {
        // Simple string comparison for now
        return hopIp == target
    }
}
