package com.herwin.netez.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

data class PortResult(
    val port: Int,
    val isOpen: Boolean,
    val protocol: String = "TCP",
    val service: String = "Unknown",
    val latencyMs: Long = 0,
    val banner: String? = null
)

object PortScanner {
    val commonPorts = listOf(21, 22, 23, 25, 53, 80, 110, 143, 443, 445, 3306, 3389, 5432, 8080, 8291)
    val top100Ports = listOf(
        20, 21, 22, 23, 25, 53, 67, 68, 69, 80, 110, 119, 123, 135, 137, 138, 139, 143, 161, 162, 
        179, 389, 443, 445, 465, 514, 515, 548, 554, 587, 631, 636, 993, 995, 1025, 1080, 1433, 
        1434, 1521, 1723, 1812, 1813, 2049, 3306, 3389, 5060, 5061, 5432, 5900, 5985, 5986, 
        8080, 8443, 8291, 9100, 10000
    ) // Simplified top list

    suspend fun scanPorts(
        ip: String, 
        ports: List<Int>, 
        protocol: String = "TCP"
    ): List<PortResult> = coroutineScope {
        ports.map { port ->
            async(Dispatchers.IO) {
                if (protocol == "TCP") {
                    scanTcpPort(ip, port)
                } else {
                    scanUdpPort(ip, port)
                }
            }
        }.awaitAll()
    }

    private fun scanTcpPort(ip: String, port: Int): PortResult {
        var latency: Long = 0
        var banner: String? = null
        val isOpen = try {
            val socket = Socket()
            latency = measureTimeMillis {
                socket.connect(InetSocketAddress(ip, port), 500)
            }
            
            // Basic banner grabbing
            if (port in listOf(21, 22, 23, 25, 80, 443, 8080)) {
                try {
                    socket.soTimeout = 1000
                    val reader = socket.getInputStream().bufferedReader()
                    if (port == 80 || port == 8080 || port == 443) {
                        socket.getOutputStream().write("HEAD / HTTP/1.1\r\nHost: $ip\r\n\r\n".toByteArray())
                    }
                    banner = reader.readLine()?.take(50)?.trim()
                } catch (e: Exception) {}
            }
            
            socket.close()
            true
        } catch (e: Exception) {
            false
        }
        
        return PortResult(
            port = port,
            isOpen = isOpen,
            protocol = "TCP",
            service = getPortService(port),
            latencyMs = latency,
            banner = banner
        )
    }

    private fun scanUdpPort(ip: String, port: Int): PortResult {
        // UDP is harder to detect. Most ports will appear "open|filtered" if no ICMP unreachable is received.
        // We'll do a very basic check.
        val isOpen = try {
            val socket = DatagramSocket()
            socket.soTimeout = 500
            val address = InetAddress.getByName(ip)
            val data = "Hello".toByteArray()
            val packet = DatagramPacket(data, data.size, address, port)
            socket.send(packet)
            
            // Try to receive something (unlikely for random ports, but good for DNS/SNMP)
            val receiveData = ByteArray(1024)
            val receivePacket = DatagramPacket(receiveData, receiveData.size)
            socket.receive(receivePacket)
            true
        } catch (e: java.net.SocketTimeoutException) {
            // In UDP, timeout often means open but silent, or filtered. 
            // For simplicity in a basic tool, we only mark true if we get a response.
            false 
        } catch (e: Exception) {
            false
        }
        
        return PortResult(
            port = port,
            isOpen = isOpen,
            protocol = "UDP",
            service = getPortService(port),
            latencyMs = 0
        )
    }

    private fun getPortService(port: Int): String {
        return when (port) {
            21 -> "FTP"
            22 -> "SSH"
            23 -> "Telnet"
            25 -> "SMTP"
            53 -> "DNS"
            67, 68 -> "DHCP"
            69 -> "TFTP"
            80 -> "HTTP"
            110 -> "POP3"
            123 -> "NTP"
            143 -> "IMAP"
            161, 162 -> "SNMP"
            443 -> "HTTPS"
            445 -> "SMB"
            3306 -> "MySQL"
            3389 -> "RDP"
            5432 -> "PostgreSQL"
            8080 -> "HTTP-Proxy"
            8291 -> "Winbox (Mikrotik)"
            else -> "Unknown"
        }
    }
}
