package com.herwin.netez.utils

import java.util.Locale

data class SubnetInfo(
    val networkId: String,
    val broadcast: String,
    val firstHost: String,
    val lastHost: String,
    val mask: String,
    val wildcard: String,
    val totalHosts: Long,
    val ipClass: String,
    val isPrivate: Boolean,
    val binaryMask: String,
    val binaryIp: String,
    val hexIp: String,
    val cidr: Int
)

data class VlsmSubnet(
    val subnet: String,
    val range: String,
    val usable: Long
)

object IpCalculator {
    fun calculate(ip: String, cidr: Int): SubnetInfo? {
        try {
            val ipInt = ipToLong(ip)
            val maskInt = (0xffffffff shl (32 - cidr)).toInt()
            val networkId = ipInt.toInt() and maskInt
            val wildcard = maskInt.inv()
            val broadcast = networkId or wildcard
            
            val totalHosts = when {
                cidr == 32 -> 1L
                cidr == 31 -> 2L
                else -> {
                    val count = (1L shl (32 - cidr)) - 2
                    if (count < 0) 0 else count
                }
            }

            val firstHost = if (cidr >= 31) networkId.toLong() and 0xffffffffL else (networkId.toLong() and 0xffffffffL) + 1
            val lastHost = if (cidr >= 31) broadcast.toLong() and 0xffffffffL else (broadcast.toLong() and 0xffffffffL) - 1

            val firstOctet = (ipInt shr 24) and 0xFF
            val ipClass = when {
                firstOctet in 1..126 -> "Class A"
                firstOctet in 128..191 -> "Class B"
                firstOctet in 192..223 -> "Class C"
                firstOctet in 224..239 -> "Class D"
                else -> "Class E"
            }

            val isPrivate = when (firstOctet.toInt()) {
                10 -> true
                172 -> {
                    val secondOctet = (ipInt shr 16) and 0xFF
                    secondOctet in 16..31
                }
                192 -> {
                    val secondOctet = (ipInt shr 16) and 0xFF
                    secondOctet == 168L
                }
                else -> false
            }

            val binaryMask = String.format("%32s", Integer.toBinaryString(maskInt)).replace(' ', '0')
            val binaryIp = String.format("%32s", java.lang.Long.toBinaryString(ipInt)).replace(' ', '0')
            val hexIp = "0x" + java.lang.Long.toHexString(ipInt).uppercase()

            return SubnetInfo(
                networkId = longToIp(networkId.toLong() and 0xffffffffL),
                broadcast = longToIp(broadcast.toLong() and 0xffffffffL),
                firstHost = longToIp(firstHost),
                lastHost = longToIp(lastHost),
                mask = longToIp(maskInt.toLong() and 0xffffffffL),
                wildcard = longToIp(wildcard.toLong() and 0xffffffffL),
                totalHosts = totalHosts,
                ipClass = ipClass,
                isPrivate = isPrivate,
                binaryMask = binaryMask,
                binaryIp = binaryIp,
                hexIp = hexIp,
                cidr = cidr
            )
        } catch (e: Exception) {
            return null
        }
    }

    fun splitSubnet(network: String, cidr: Int): List<VlsmSubnet> {
        if (cidr >= 32) return emptyList()
        val nextCidr = cidr + 1
        val ipLong = ipToLong(network)
        val size = 1L shl (32 - nextCidr)
        
        val sub1 = calculate(longToIp(ipLong), nextCidr)
        val sub2 = calculate(longToIp(ipLong + size), nextCidr)
        
        return listOfNotNull(
            sub1?.let { VlsmSubnet(it.networkId + "/" + nextCidr, "${it.firstHost} - ${it.lastHost}", it.totalHosts) },
            sub2?.let { VlsmSubnet(it.networkId + "/" + nextCidr, "${it.firstHost} - ${it.lastHost}", it.totalHosts) }
        )
    }

    fun ipToLong(ip: String): Long {
        val parts = ip.split(".")
        if (parts.size != 4) return 0L
        var res = 0L
        for (i in 0..3) {
            res = res or (parts[i].toLong() shl (24 - i * 8))
        }
        return res
    }

    fun longToIp(ip: Long): String {
        return String.format(
            Locale.US,
            "%d.%d.%d.%d",
            (ip shr 24) and 0xff,
            (ip shr 16) and 0xff,
            (ip shr 8) and 0xff,
            ip and 0xff
        )
    }
}
