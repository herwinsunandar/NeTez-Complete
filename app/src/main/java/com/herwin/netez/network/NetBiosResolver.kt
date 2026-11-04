package com.herwin.netez.network

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

object NetBiosResolver {
    private const val NETBIOS_PORT = 137
    private val QUERY_PACKET = byteArrayOf(
        0x80.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x01.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x20.toByte(), 0x43.toByte(), 0x4b.toByte(), 0x41.toByte(),
        0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(),
        0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(),
        0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(),
        0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x00.toByte(), 0x00.toByte(), 0x21.toByte(),
        0x00.toByte(), 0x01.toByte()
    )

    data class NetBiosInfo(
        val name: String,
        val domain: String? = null,
        val mac: String? = null
    )

    fun resolve(ip: String, timeout: Int = 350): NetBiosInfo? {
        try {
            DatagramSocket().use { socket ->
                socket.soTimeout = timeout
                val packet = DatagramPacket(QUERY_PACKET, QUERY_PACKET.size, InetAddress.getByName(ip), NETBIOS_PORT)
                socket.send(packet)

                val responseBuffer = ByteArray(1024)
                val responsePacket = DatagramPacket(responseBuffer, responseBuffer.size)
                socket.receive(responsePacket)
                return parseResponse(responsePacket.data, responsePacket.length)
            }
        } catch (_: Exception) {
            return null
        }
    }

    private fun parseResponse(data: ByteArray, length: Int): NetBiosInfo? {
        if (length < 57) return null
        
        val numberOfNames = data[56].toInt() and 0xFF
        val names = mutableListOf<Triple<String, Int, Boolean>>() // Name, Type, isGroup
        
        var offset = 57
        for (unused in 0 until numberOfNames) {
            if (offset + 18 > length) break
            
            val nameBytes = ByteArray(15)
            System.arraycopy(data, offset, nameBytes, 0, 15)
            val name = String(nameBytes).trim()
            val type = data[offset + 15].toInt() and 0xFF
            val flags = ((data[offset + 16].toInt() and 0xFF) shl 8) or (data[offset + 17].toInt() and 0xFF)
            val isGroup = (flags and 0x8000) != 0

            if (name.isNotEmpty()) {
                names.add(Triple(name, type, isGroup))
            }
            offset += 18
        }

        // Adapter Status (MAC Address) is usually after the name table
        var mac: String? = null
        if (offset + 6 <= length) {
            val sb = StringBuilder()
            for (i in 0 until 6) {
                sb.append(String.format("%02X", data[offset + i]))
                if (i < 5) sb.append(":")
            }
            val foundMac = sb.toString()
            if (foundMac != "00:00:00:00:00:00") {
                mac = foundMac
            }
        }

        val computerName = names.find { (name, type, isGroup) ->
            !isGroup && (type == 0x00 || type == 0x03 || type == 0x20) &&
            !name.startsWith("__MSBROWSE__")
        }?.first

        val domainName = names.find { (name, type, isGroup) ->
            isGroup && type == 0x00 && !name.startsWith("__MSBROWSE__")
        }?.first

        return if (computerName != null || domainName != null || mac != null) {
            NetBiosInfo(computerName ?: "", domainName, mac)
        } else null
    }
}
