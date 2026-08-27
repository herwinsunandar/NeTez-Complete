package com.herwin.netez.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.herwin.netez.utils.NetworkUtils
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern

data class Device(
    val ip: String,
    val hostname: String = "Unknown",
    val mac: String = "",
    val vendor: String = "Network Device",
    val deviceType: String = "Unknown",
    val os: String = "Unknown",
    val latencyMs: Long = 0,
    val isGateway: Boolean = false,
    val isOnline: Boolean = false,
    val openPorts: List<Int> = emptyList(),
    val ttl: Int = 0,
    val connectionType: String = "WiFi",
    val signalStrength: String? = null,
    val firstSeen: Long = System.currentTimeMillis(),
    val lastSeen: Long = System.currentTimeMillis()
)

object LanScanner {

    private val TOP_PORTS = listOf(21, 22, 23, 25, 53, 80, 110, 135, 137, 138, 139, 143, 443, 445, 993, 995, 1723, 3306, 3389, 5000, 5900, 62078, 8008, 8080)

    suspend fun scanSubnet(
        context: Context,
        subnet: String,
        gatewayIp: String = "",
        onDeviceFound: (Device) -> Unit = {},
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): List<Device> = supervisorScope {
        val completed = AtomicInteger(0)
        val foundDevices = ConcurrentHashMap<String, Device>()
        val dispatcher = Dispatchers.IO.limitedParallelism(30)
        
        val ips = (1..254).map { "$subnet.$it" }
        val total = ips.size

        try {
            withTimeout(6000) {
                val jobs = ips.map { host ->
                    async(dispatcher + SupervisorJob()) {
                        try {
                            val device = probeHostRobust(host, gatewayIp)
                            if (device != null) {
                                foundDevices[host] = device
                                onDeviceFound(device)
                            }
                        } catch (e: Exception) {
                            // Single IP failure ignored
                        } finally {
                            onProgress(completed.incrementAndGet(), total)
                        }
                    }
                }
                jobs.awaitAll()
            }
        } catch (e: Exception) {
            // Force progress update on global timeout
        } finally {
            onProgress(total, total)
        }

        val results = foundDevices.values.toList()
        
        // Phase 2: Background Enrichment
        results.forEach { device ->
            launch(Dispatchers.IO) {
                try {
                    val name = getHostnameFast(device.ip)
                    if (name != "Unknown" && name != device.ip) {
                        onDeviceFound(device.copy(hostname = name))
                    }
                } catch (e: Exception) {}
            }
        }

        results.sortedBy { NetworkUtils.ipToLong(it.ip) }
    }

    suspend fun scanRange(
        startIp: String,
        endIp: String,
        gatewayIp: String = "",
        onDeviceFound: (Device) -> Unit = {},
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): List<Device> = supervisorScope {
        val startLong = NetworkUtils.ipToLong(startIp)
        val endLong = NetworkUtils.ipToLong(endIp)
        if (startLong > endLong) return@supervisorScope emptyList()
        
        val ips = (startLong..endLong).map { longToIp(it) }
        val totalCount = ips.size
        val completed = AtomicInteger(0)
        val foundDevices = ConcurrentHashMap<String, Device>()
        val dispatcher = Dispatchers.IO.limitedParallelism(30)

        try {
            withTimeout(8000) {
                val jobs = ips.map { host ->
                    async(dispatcher + SupervisorJob()) {
                        try {
                            val device = probeHostRobust(host, gatewayIp)
                            if (device != null) {
                                foundDevices[host] = device
                                onDeviceFound(device)
                            }
                        } catch (e: Exception) {
                        } finally {
                            onProgress(completed.incrementAndGet(), totalCount)
                        }
                    }
                }
                jobs.awaitAll()
            }
        } catch (e: Exception) {
        } finally {
            onProgress(totalCount, totalCount)
        }
        
        val results = foundDevices.values.toList()
        results.forEach { device ->
            launch(Dispatchers.IO) {
                try {
                    val name = getHostnameFast(device.ip)
                    if (name != "Unknown") onDeviceFound(device.copy(hostname = name))
                } catch (e: Exception) {}
            }
        }
        results.sortedBy { NetworkUtils.ipToLong(it.ip) }
    }

    private suspend fun probeHostRobust(host: String, gatewayIp: String): Device? = coroutineScope {
        val startTime = System.currentTimeMillis()
        val ownIp = NetworkUtils.getLocalIpAddress()
        
        if (host == ownIp) {
            return@coroutineScope Device(
                ip = host,
                hostname = "This Device",
                latencyMs = 0,
                isGateway = host == gatewayIp,
                isOnline = true,
                lastSeen = System.currentTimeMillis()
            )
        }

        // Parallel Probes for speed and bypass
        val probes = listOf(
            async { icmpPing(host) },
            async { tcpPing(host, 445, 400) },  // SMB
            async { tcpPing(host, 139, 400) },  // NetBIOS
            async { tcpPing(host, 135, 400) },  // RPC
            async { tcpPing(host, 80, 400) },   // HTTP
            async { tcpPing(host, 3389, 400) }, // RDP
            async { probeNetbios(host) }
        )

        val isAlive = probes.any { it.await() }
        
        // MAC check even if pings fail (firewall)
        val mac = getMacFromArpDirect(host)
        val hasMac = mac != "00:00:00:00:00:00" && mac != "02:00:00:00:00:00"

        if (isAlive || hasMac) {
            return@coroutineScope Device(
                ip = host,
                hostname = "Device",
                mac = mac,
                vendor = if (hasMac) NetworkUtils.getVendor(mac) else "Network Device",
                latencyMs = if (isAlive) System.currentTimeMillis() - startTime else 0,
                isGateway = host == gatewayIp,
                isOnline = isAlive,
                lastSeen = System.currentTimeMillis()
            )
        }
        null
    }

    private fun icmpPing(host: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("ping -c 1 -W 0.4 $host")
            process.waitFor() == 0
        } catch (e: Exception) { false }
    }

    private fun tcpPing(host: String, port: Int, timeout: Int): Boolean {
        return try {
            Socket().use { it.connect(InetSocketAddress(host, port), timeout) }
            true
        } catch (e: Exception) { false }
    }

    private fun getMacFromArpDirect(ip: String): String {
        try {
            val file = java.io.File("/proc/net/arp")
            if (file.exists()) {
                file.bufferedReader().use { reader ->
                    reader.readLine()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        if (line!!.startsWith("$ip ")) {
                            val parts = line!!.split(Pattern.compile("\\s+")).filter { it.isNotBlank() }
                            if (parts.size >= 4) return parts[3].uppercase()
                        }
                    }
                }
            }
        } catch (e: Exception) {}
        
        // Fallback neighbor check
        try {
            val process = Runtime.getRuntime().exec("ip neighbor show $ip")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val parts = line!!.split(" ")
                if (parts.size >= 5) {
                    val foundMac = parts[4]
                    if (foundMac.contains(":") && foundMac.length == 17) return foundMac.uppercase()
                }
            }
        } catch (e: Exception) {}
        
        return "00:00:00:00:00:00"
    }

    private suspend fun getHostnameFast(host: String): String = coroutineScope {
        val dns = async(Dispatchers.IO) {
            try {
                val addr = InetAddress.getByName(host)
                val name = addr.canonicalHostName
                if (name != host) name else null
            } catch (e: Exception) { null }
        }
        val netbios = async(Dispatchers.IO) { getNetbiosName(host) }

        withTimeoutOrNull(800) { dns.await() } 
            ?: withTimeoutOrNull(800) { netbios.await() } 
            ?: "Unknown"
    }

    private fun getNetbiosName(ip: String): String? {
        try {
            val socket = DatagramSocket()
            socket.soTimeout = 400
            val buffer = byteArrayOf(
                0x80.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x01.toByte(), 0x00.toByte(), 0x00.toByte(),
                0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x20.toByte(), 0x43.toByte(), 0x4b.toByte(), 0x41.toByte(),
                0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(),
                0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(),
                0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(),
                0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x00.toByte(), 0x00.toByte(), 0x21.toByte(),
                0x00.toByte(), 0x01.toByte()
            )
            val packet = DatagramPacket(buffer, buffer.size, InetAddress.getByName(ip), 137)
            socket.send(packet)
            
            val responseBuffer = ByteArray(1024)
            val responsePacket = DatagramPacket(responseBuffer, responseBuffer.size)
            socket.receive(responsePacket)
            
            val data = responsePacket.data
            if (responsePacket.length > 57) {
                val name = String(data, 57, 15).trim()
                if (name.isNotEmpty() && name.all { it.isLetterOrDigit() || it == '-' || it == '_' || it == ' ' }) {
                    return name
                }
            }
        } catch (e: Exception) {}
        return null
    }

    private fun probeNetbios(ip: String): Boolean {
        try {
            val socket = DatagramSocket()
            socket.soTimeout = 300
            val buffer = byteArrayOf(
                0x80.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x01.toByte(), 0x00.toByte(), 0x00.toByte(),
                0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x20.toByte(), 0x43.toByte(), 0x4b.toByte(), 0x41.toByte(),
                0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(),
                0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(),
                0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(),
                0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x00.toByte(), 0x00.toByte(), 0x21.toByte(),
                0x00.toByte(), 0x01.toByte()
            )
            val packet = DatagramPacket(buffer, buffer.size, InetAddress.getByName(ip), 137)
            socket.send(packet)
            
            val responseBuffer = ByteArray(1024)
            val responsePacket = DatagramPacket(responseBuffer, responseBuffer.size)
            socket.receive(responsePacket)
            return responsePacket.length > 50
        } catch (e: Exception) {}
        return false
    }

    private fun longToIp(ip: Long): String {
        return "${(ip shr 24) and 0xFF}.${(ip shr 16) and 0xFF}.${(ip shr 8) and 0xFF}.${ip and 0xFF}"
    }

    suspend fun scanPorts(ip: String): List<Int> = withContext(Dispatchers.IO) {
        TOP_PORTS.map { port ->
            async {
                try {
                    Socket().use { it.connect(InetSocketAddress(ip, port), 250) }
                    port
                } catch (e: Exception) { null }
            }
        }.awaitAll().filterNotNull()
    }
}
