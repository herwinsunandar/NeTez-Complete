package com.herwin.netez.network

import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

data class SnmpDevice(
    val ip: String,
    val sysDescr: String,
    val responseTime: Long
)

object SnmpManager {
    private const val SNMP_PORT = 161
    private const val TIMEOUT = 1000

    // SNMP v2c GET Request for sysDescr (.1.3.6.1.2.1.1.1.0) with community "public"
    private val SNMP_GET_SYSDESCR = byteArrayOf(
        0x30, 0x29,                          // Sequence, length 41
        0x02, 0x01, 0x01,                    // Version v2c (1)
        0x04, 0x06, 0x70, 0x75, 0x62, 0x6c, 0x69, 0x63, // Community "public"
        0xa0.toByte(), 0x1c,                 // GetRequest PDU, length 28
        0x02, 0x04, 0x00, 0x00, 0x00, 0x01,  // Request ID 1
        0x02, 0x01, 0x00,                    // Error status 0
        0x02, 0x01, 0x00,                    // Error index 0
        0x30, 0x0e,                          // Varbind list, length 14
        0x30, 0x0c,                          // Varbind, length 12
        0x06, 0x08, 0x2b, 0x06, 0x01, 0x02, 0x01, 0x01, 0x01, 0x00, // OID .1.3.6.1.2.1.1.1.0
        0x05, 0x00                           // Null value
    )

    suspend fun scanSubnet(
        subnet: String,
        onProgress: (Int) -> Unit
    ): List<SnmpDevice> = withContext(Dispatchers.IO) {
        val results = mutableListOf<Deferred<SnmpDevice?>>()
        var completed = 0

        (1..254).forEach { i ->
            results.add(async {
                val ip = "$subnet.$i"
                val device = querySnmp(ip)
                synchronized(this@withContext) {
                    completed++
                    onProgress(completed)
                }
                device
            })
        }

        results.awaitAll().filterNotNull()
    }

    private fun querySnmp(ip: String): SnmpDevice? {
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket()
            socket.soTimeout = TIMEOUT
            val address = InetAddress.getByName(ip)
            val packet = DatagramPacket(SNMP_GET_SYSDESCR, SNMP_GET_SYSDESCR.size, address, SNMP_PORT)
            
            val startTime = System.currentTimeMillis()
            socket.send(packet)

            val buffer = ByteArray(1024)
            val response = DatagramPacket(buffer, buffer.size)
            socket.receive(response)
            val endTime = System.currentTimeMillis()

            val responseData = response.data.take(response.length).toByteArray()
            val sysDescr = parseSysDescr(responseData)
            
            if (sysDescr != null) {
                return SnmpDevice(ip, sysDescr, endTime - startTime)
            }
        } catch (e: SocketTimeoutException) {
            // Ignore
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            socket?.close()
        }
        return null
    }

    private fun parseSysDescr(data: ByteArray): String? {
        try {
            // Very basic BER parsing to find the response value
            // We expect a sequence (0x30)
            if (data.isEmpty() || data[0] != 0x30.toByte()) return null
            
            // Find the Varbind list (0x30)
            // The response PDU is at a0 (GetResponse) or a2 (Response)
            // Let's search for the OID we sent: 06 08 2b 06 01 02 01 01 01 00
            val oid = byteArrayOf(0x06, 0x08, 0x2b, 0x06, 0x01, 0x02, 0x01, 0x01, 0x01, 0x00)
            val oidIndex = indexOf(data, oid)
            if (oidIndex == -1) return null

            // After OID, we expect the value. 
            // The tag for Octet String is 0x04.
            val valueIndex = oidIndex + oid.size
            if (valueIndex >= data.size) return null
            
            if (data[valueIndex] == 0x04.toByte()) {
                val length = data[valueIndex + 1].toInt() and 0xFF
                // Simple length parsing (doesn't handle long form)
                if (valueIndex + 2 + length <= data.size) {
                    return String(data, valueIndex + 2, length)
                }
            }
        } catch (e: Exception) {}
        return null
    }

    private fun indexOf(data: ByteArray, target: ByteArray): Int {
        if (target.isEmpty()) return 0
        outer@ for (i in 0 until data.size - target.size + 1) {
            for (j in target.indices) {
                if (data[i + j] != target[j]) continue@outer
            }
            return i
        }
        return -1
    }
}
