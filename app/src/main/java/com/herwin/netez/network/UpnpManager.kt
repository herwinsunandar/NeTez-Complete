package com.herwin.netez.network

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.util.UUID

data class UpnpDevice(
    val ip: String,
    val location: String,
    val server: String = "Unknown",
    var searchTarget: String = "Unknown",
    val usn: String = "Unknown",
    var friendlyName: String = "UPnP Device",
    var modelName: String = "",
    var modelNumber: String = "",
    var manufacturer: String = "",
    var deviceType: String = ""
)

object UpnpManager {
    private const val SSDP_MULTICAST_ADDRESS = "239.255.255.250"
    private const val SSDP_PORT = 1900
    private const val WAIT_TIME_MS = 1200

    suspend fun scan(context: Context): List<UpnpDevice> = withContext(Dispatchers.IO) {
        val devices = mutableMapOf<String, UpnpDevice>()
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val lock = try {
            wifi?.createMulticastLock("UpnpScanLock")?.apply {
                setReferenceCounted(false)
                acquire()
            }
        } catch (_: Exception) { null }
        
        val socket = DatagramSocket()
        socket.soTimeout = WAIT_TIME_MS

        try {
            val query = StringBuilder()
            query.append("M-SEARCH * HTTP/1.1\r\n")
            query.append("HOST: $SSDP_MULTICAST_ADDRESS:$SSDP_PORT\r\n")
            query.append("MAN: \"ssdp:discover\"\r\n")
            query.append("MX: 2\r\n")
            query.append("ST: ssdp:all\r\n")
            query.append("\r\n")

            val group = InetAddress.getByName(SSDP_MULTICAST_ADDRESS)
            val sendPacket = DatagramPacket(query.toString().toByteArray(), query.length, group, SSDP_PORT)
            socket.send(sendPacket)

            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < WAIT_TIME_MS) {
                val buffer = ByteArray(2048)
                val receivePacket = DatagramPacket(buffer, buffer.size)
                try {
                    socket.receive(receivePacket)
                    val response = String(receivePacket.data, 0, receivePacket.length)
                    val device = parseSsdpResponse(response, receivePacket.address.hostAddress)
                    if (device != null) {
                        devices[device.usn] = device
                    }
                } catch (e: Exception) {
                    // Timeout or end of packet
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (lock?.isHeld == true) {
                try { lock.release() } catch (_: Exception) {}
            }
            socket.close()
        }

        val resultList = devices.values.toList()
        // Concurrent XML detail fetching
        coroutineScope {
            resultList.map { device ->
                async(Dispatchers.IO) {
                    fetchDeviceDetails(device)
                }
            }.awaitAll()
        }
        
        resultList
    }

    private fun parseSsdpResponse(response: String, ip: String): UpnpDevice? {
        val headers = response.split("\r\n")
        var location = ""
        var server = "Unknown"
        var st = "Unknown"
        var usn = ""

        headers.forEach { header ->
            val parts = header.split(": ", limit = 2)
            if (parts.size == 2) {
                when (parts[0].uppercase()) {
                    "LOCATION" -> location = parts[1]
                    "SERVER" -> server = parts[1]
                    "ST" -> st = parts[1]
                    "USN" -> usn = parts[1]
                }
            }
        }

        if (location.isEmpty()) return null
        
        return UpnpDevice(
            ip = ip,
            location = location,
            server = server,
            searchTarget = st,
            usn = if (usn.isEmpty()) UUID.randomUUID().toString() else usn
        )
    }

    private suspend fun fetchDeviceDetails(device: UpnpDevice) = withContext(Dispatchers.IO) {
        try {
            val url = URL(device.location)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 2000
            conn.readTimeout = 2000
            val xml = conn.inputStream.bufferedReader().use { it.readText() }
            
            // More robust XML parsing for UPnP
            device.friendlyName = extractXmlValue(xml, "friendlyName") ?: device.friendlyName
            device.modelName = extractXmlValue(xml, "modelName") ?: ""
            device.modelNumber = extractXmlValue(xml, "modelNumber") ?: ""
            device.manufacturer = extractXmlValue(xml, "manufacturer") ?: ""
            device.deviceType = extractXmlValue(xml, "deviceType") ?: ""
        } catch (_: Exception) {
        }
    }

    private fun extractXmlValue(xml: String, tag: String): String? {
        val startTag = "<$tag>"
        val endTag = "</$tag>"
        val start = xml.indexOf(startTag)
        if (start == -1) return null
        val end = xml.indexOf(endTag, start + startTag.length)
        if (end == -1) return null
        return xml.substring(start + startTag.length, end).trim()
    }
}
