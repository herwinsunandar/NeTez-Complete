package com.herwin.netez.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

object WolManager {
    suspend fun sendMagicPacket(macAddress: String, ipAddress: String = "255.255.255.255") = withContext(Dispatchers.IO) {
        try {
            val macBytes = getMacBytes(macAddress)
            val bytes = ByteArray(6 + 16 * macBytes.size)
            for (i in 0..5) {
                bytes[i] = 0xff.toByte()
            }
            for (i in 6 until bytes.size step macBytes.size) {
                System.arraycopy(macBytes, 0, bytes, i, macBytes.size)
            }
            
            val address = InetAddress.getByName(ipAddress)
            val packet = DatagramPacket(bytes, bytes.size, address, 9)
            val socket = DatagramSocket()
            socket.send(packet)
            socket.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun getMacBytes(macAddress: String): ByteArray {
        val bytes = ByteArray(6)
        val hex = macAddress.split(":", "-")
        if (hex.size != 6) throw IllegalArgumentException("Invalid MAC address.")
        for (i in 0..5) {
            bytes[i] = hex[i].toInt(16).toByte()
        }
        return bytes
    }
}
