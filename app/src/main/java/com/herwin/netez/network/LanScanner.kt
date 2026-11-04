package com.herwin.netez.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import com.herwin.netez.data.wifi.VendorData
import com.herwin.netez.data.wifi.VendorDatabase
import com.herwin.netez.utils.NetworkUtils
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

data class Device(
    val ip: String,
    val hostname: String = "",
    val netbiosName: String? = null,
    val netbiosDomain: String? = null,
    val mac: String = "",
    val vendor: String = "Unknown",
    val deviceCategory: String = "Unknown",
    val deviceType: String = "Unknown",
    val os: String = "Unknown",
    val latencyMs: Long = 0,
    val isGateway: Boolean = false,
    val isThisDevice: Boolean = false,
    val isOnline: Boolean = false,
    val openPorts: List<Int> = emptyList(),
    val ttl: Int = 0,
    val connectionType: String = "WiFi",
    val signalStrength: String? = null,
    val firstSeen: Long = System.currentTimeMillis(),
    val lastSeen: Long = System.currentTimeMillis(),
    val isRandomMac: Boolean = false,
    val hasPerformedPortScan: Boolean = false,
    val hostnameSource: String? = null, // mDNS, NetBIOS, DNS, DHCP
    val discoveryMethods: List<String> = emptyList(),
    val riskScore: Int = 0,
    val bluetoothName: String? = null,
    val bluetoothMac: String? = null,
    val bluetoothRssi: Int? = null,
    val bluetoothDeviceType: String? = null,
    val isBluetoothNearby: Boolean = false
)

object LanScanner {

    // Discovery Ports for multi-probe: Windows SMB, NetBIOS, RPC, RDP, SSH, HTTP/HTTPS, ADB, Printers, Apple, RTSP CCTV
    private val DISCOVERY_PORTS = intArrayOf(445, 139, 135, 3389, 80, 443, 22, 62078, 5555, 8080, 9100, 554, 53, 5357)
    private val SCAN_PORTS = intArrayOf(
        445, 139, 135, // SMB / NetBIOS / RPC
        5357, 5985,    // WSD / WinRM
        22, 80, 443,   // SSH / HTTP / HTTPS
        3389, 8080,    // RDP / Alternative HTTP
        53             // DNS
    )
    private val mDnsCache = ConcurrentHashMap<String, MdnsInfo>() // IP -> Info
    private val ssdpCache = ConcurrentHashMap<String, UpnpDevice>() // IP -> Info
    private val arpCache = ConcurrentHashMap<String, String>() // IP -> MAC
    private val netbiosCache = ConcurrentHashMap<String, NetBiosResolver.NetBiosInfo>()

    data class MdnsInfo(
        val name: String,
        val type: String,
        val host: String? = null,
        val attributes: Map<String, String> = emptyMap()
    )

    private const val FAST_PROBE_TIMEOUT_MS = 300
    private const val ENRICHMENT_TIMEOUT_MS = 250L
    private var multicastLock: WifiManager.MulticastLock? = null
    private val activeNsdListeners = ConcurrentHashMap<String, Pair<NsdManager, NsdManager.DiscoveryListener>>()

    suspend fun scanSubnet(
        context: Context,
        subnet: String,
        gatewayIp: String = "",
        onDeviceFound: (Device) -> Unit = {},
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): List<Device> = coroutineScope {
        mDnsCache.clear()
        ssdpCache.clear()
        arpCache.clear()
        netbiosCache.clear()
        stopMdnsDiscovery()

        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        multicastLock = try {
            wifi?.createMulticastLock("LanScannerMulticastLock")?.apply {
                setReferenceCounted(false)
                acquire()
            }
        } catch (_: Exception) { null }

        val subnetInfo = NetworkUtils.getSubnetInfo(context)
        val ownIp = NetworkUtils.getLocalIpAddress()

        val ips = if (subnetInfo != null && (subnetInfo.ip.startsWith(subnet) || subnet.startsWith(subnetInfo.networkAddress.take(3)))) {
            val startLong = NetworkUtils.ipToLong(subnetInfo.startIp)
            val endLong = NetworkUtils.ipToLong(subnetInfo.endIp)
            val count = (endLong - startLong).toInt()

            if (count > 1024) {
                val ownLong = NetworkUtils.ipToLong(ownIp ?: subnetInfo.startIp)
                val windowStart = (ownLong - 512).coerceAtLeast(startLong)
                val windowEnd = (windowStart + 1023).coerceAtMost(endLong)

                val ipSet = mutableSetOf<Long>()
                val gatewayLong = NetworkUtils.ipToLong(gatewayIp.ifEmpty { subnetInfo.startIp })
                if (gatewayLong in startLong..endLong) ipSet.add(gatewayLong)
                ipSet.add(startLong)
                (windowStart..windowEnd).forEach { ipSet.add(it) }

                ipSet.map { NetworkUtils.longToIp(it) }.filter { it != ownIp }
            } else {
                (startLong..endLong).map { NetworkUtils.longToIp(it) }.filter { it != ownIp }
            }
        } else {
            (1..254).map { "$subnet.$it" }.filter { it != ownIp }
        }

        val total = ips.size
        val networkLabel = "${subnetInfo?.networkAddress ?: subnet}/${subnetInfo?.prefixLength ?: 24}"
        Log.d("LanScanner", "SCAN_START network=$networkLabel")
        val startTime = System.currentTimeMillis()

        // 1. Dual-Pass Active Pre-Warming (Delay 250ms for Wi-Fi DTIM beacon frames)
        triggerArpCache(ips)
        populateArpCache()

        val foundDevices = ConcurrentHashMap<String, Device>()

        // Add Local Device Immediately
        if (ownIp != null) {
            val thisDevice = Device(
                ip = ownIp,
                hostname = "${Build.MODEL} (${Build.MANUFACTURER})",
                mac = NetworkUtils.getMacAddress() ?: "",
                vendor = Build.MANUFACTURER,
                isThisDevice = true,
                isOnline = true,
                deviceType = "Phone",
                latencyMs = 0,
                discoveryMethods = listOf("Local")
            )
            foundDevices[ownIp] = thisDevice
            onDeviceFound(thisDevice)
        }

        // 2. FAST PASSIVE EMIT: Instantly emit devices from ARP, mDNS, SSDP Cache
        ips.forEach { host ->
            val isArp = arpCache.containsKey(host)
            val isMdns = mDnsCache.containsKey(host)
            val isSsdp = ssdpCache.containsKey(host)

            if (isArp || isMdns || isSsdp) {
                val methods = mutableListOf<String>()
                if (isArp) methods.add("ARP")
                if (isMdns) methods.add("mDNS")
                if (isSsdp) methods.add("SSDP")

                val mac = getBestMacAddress(host)
                val device = assembleInitialDevice(host, mac, gatewayIp, 0L, methods)
                foundDevices[host] = device
                onDeviceFound(device)

                launch(Dispatchers.IO) {
                    val enriched = enrichDevice(context, device, gatewayIp)
                    foundDevices[host] = enriched
                    onDeviceFound(enriched)
                }
            }
        }

        // 3. Start Multicast Discovery (mDNS & SSDP) in parallel
        startMdnsDiscovery(context)
        startDirectMdnsQuery()
        startSsdpDiscovery(context)

        // 4. High-Concurrency Multi-Probe Active Discovery (128 parallel host coroutines)
        val completed = AtomicInteger(0)
        val dispatcher = Dispatchers.IO.limitedParallelism(128)

        try {
            val jobs = ips.map { host ->
                async(dispatcher) {
                    try {
                        if (!foundDevices.containsKey(host)) {
                            val discovery = fastMultiProbeHost(host)
                            if (discovery.first) {
                                val mac = getBestMacAddress(host)
                                val device = assembleInitialDevice(host, mac, gatewayIp, discovery.second, discovery.third)
                                foundDevices[host] = device
                                onDeviceFound(device)

                                launch(Dispatchers.IO) {
                                    val enriched = enrichDevice(context, device, gatewayIp)
                                    foundDevices[host] = enriched
                                    onDeviceFound(enriched)
                                }
                            }
                        }
                    } catch (_: Exception) {
                    } finally {
                        val current = completed.incrementAndGet()
                        if (current % 10 == 0 || current == total) {
                            onProgress(current, total)
                        }
                    }
                }
            }
            jobs.awaitAll()
        } catch (_: Exception) {
        } finally {
            // POST-PROBE ARP KERNEL SWEEP PASS
            populateArpCache()
            ips.forEach { host ->
                if (!foundDevices.containsKey(host) && arpCache.containsKey(host)) {
                    val mac = getBestMacAddress(host)
                    val device = assembleInitialDevice(host, mac, gatewayIp, 40L, listOf("ARP"))
                    foundDevices[host] = device
                    onDeviceFound(device)
                    launch(Dispatchers.IO) {
                        val enriched = enrichDevice(context, device, gatewayIp)
                        foundDevices[host] = enriched
                        onDeviceFound(enriched)
                    }
                }
            }

            val totalDuration = System.currentTimeMillis() - startTime
            Log.d("LanScanner", "SCAN_COMPLETE duration=${totalDuration}ms hosts=${foundDevices.size}")

            onProgress(total, total)
            stopMdnsDiscovery()
            if (multicastLock?.isHeld == true) {
                try { multicastLock?.release() } catch (_: Exception) {}
            }
        }

        foundDevices.values.toList().sortedBy { NetworkUtils.ipToLong(it.ip) }
    }

    suspend fun scanRange(
        context: Context,
        startIp: String,
        endIp: String,
        gatewayIp: String = "",
        onDeviceFound: (Device) -> Unit = {},
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): List<Device> = coroutineScope {
        mDnsCache.clear()
        ssdpCache.clear()
        arpCache.clear()
        netbiosCache.clear()
        stopMdnsDiscovery()

        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        multicastLock = try {
            wifi?.createMulticastLock("LanScannerMulticastLock")?.apply {
                setReferenceCounted(false)
                acquire()
            }
        } catch (_: Exception) { null }

        val startLong = NetworkUtils.ipToLong(startIp)
        val endLong = NetworkUtils.ipToLong(endIp)
        if (startLong > endLong) {
            stopMdnsDiscovery()
            if (multicastLock?.isHeld == true) multicastLock?.release()
            return@coroutineScope emptyList()
        }

        val ownIp = NetworkUtils.getLocalIpAddress()
        val count = (endLong - startLong).toInt()
        val ips = if (count > 2048) {
            val ownLong = NetworkUtils.ipToLong(ownIp ?: startIp)
            val windowStart = if (ownLong in startLong..endLong) (ownLong - 1024).coerceAtLeast(startLong) else startLong
            val windowEnd = (windowStart + 2047).coerceAtMost(endLong)

            val ipSet = mutableSetOf<Long>()
            ipSet.add(startLong)
            (windowStart..windowEnd).forEach { ipSet.add(it) }
            ipSet.map { NetworkUtils.longToIp(it) }
        } else {
            (startLong..endLong).map { NetworkUtils.longToIp(it) }
        }

        val startTime = System.currentTimeMillis()
        Log.d("LanScanner", "SCAN_START range=$startIp-$endIp")

        triggerArpCache(ips)
        populateArpCache()

        val foundDevices = ConcurrentHashMap<String, Device>()

        if (ownIp != null && ips.contains(ownIp)) {
            val thisDevice = Device(
                ip = ownIp,
                hostname = "${Build.MODEL} (${Build.MANUFACTURER})",
                mac = NetworkUtils.getMacAddress() ?: "",
                vendor = Build.MANUFACTURER,
                isThisDevice = true,
                isOnline = true,
                deviceType = "Phone",
                latencyMs = 0,
                discoveryMethods = listOf("Local")
            )
            foundDevices[ownIp] = thisDevice
            onDeviceFound(thisDevice)
        }

        val filterIps = ips.filter { it != ownIp }
        val totalCount = filterIps.size

        // FAST PASSIVE EMIT
        filterIps.forEach { host ->
            val isArp = arpCache.containsKey(host)
            val isMdns = mDnsCache.containsKey(host)
            val isSsdp = ssdpCache.containsKey(host)

            if (isArp || isMdns || isSsdp) {
                val methods = mutableListOf<String>()
                if (isArp) methods.add("ARP")
                if (isMdns) methods.add("mDNS")
                if (isSsdp) methods.add("SSDP")

                val mac = getBestMacAddress(host)
                val device = assembleInitialDevice(host, mac, gatewayIp, 0L, methods)
                foundDevices[host] = device
                onDeviceFound(device)

                launch(Dispatchers.IO) {
                    val enriched = enrichDevice(context, device, gatewayIp)
                    foundDevices[host] = enriched
                    onDeviceFound(enriched)
                }
            }
        }

        startMdnsDiscovery(context)
        startSsdpDiscovery(context)

        val completed = AtomicInteger(0)
        val dispatcher = Dispatchers.IO.limitedParallelism(128)

        try {
            val jobs = filterIps.map { host ->
                async(dispatcher) {
                    try {
                        if (!foundDevices.containsKey(host)) {
                            val discovery = fastMultiProbeHost(host)
                            if (discovery.first) {
                                val mac = getBestMacAddress(host)
                                val device = assembleInitialDevice(host, mac, gatewayIp, discovery.second, discovery.third)
                                foundDevices[host] = device
                                onDeviceFound(device)

                                launch(Dispatchers.IO) {
                                    val enriched = enrichDevice(context, device, gatewayIp)
                                    foundDevices[host] = enriched
                                    onDeviceFound(enriched)
                                }
                            }
                        }
                    } catch (_: Exception) {
                    } finally {
                        val current = completed.incrementAndGet()
                        if (current % 10 == 0 || current == totalCount) {
                            onProgress(current, totalCount)
                        }
                    }
                }
            }
            jobs.awaitAll()
        } catch (_: Exception) {
        } finally {
            // POST-PROBE ARP KERNEL SWEEP PASS
            populateArpCache()
            filterIps.forEach { host ->
                if (!foundDevices.containsKey(host) && arpCache.containsKey(host)) {
                    val mac = getBestMacAddress(host)
                    val device = assembleInitialDevice(host, mac, gatewayIp, 40L, listOf("ARP"))
                    foundDevices[host] = device
                    onDeviceFound(device)
                    launch(Dispatchers.IO) {
                        val enriched = enrichDevice(context, device, gatewayIp)
                        foundDevices[host] = enriched
                        onDeviceFound(enriched)
                    }
                }
            }

            val totalDuration = System.currentTimeMillis() - startTime
            Log.d("LanScanner", "SCAN_COMPLETE duration=${totalDuration}ms hosts=${foundDevices.size}")

            onProgress(totalCount, totalCount)
            stopMdnsDiscovery()
            if (multicastLock?.isHeld == true) {
                try { multicastLock?.release() } catch (_: Exception) {}
            }
        }

        foundDevices.values.toList().sortedBy { NetworkUtils.ipToLong(it.ip) }
    }

    private fun startDirectMdnsQuery() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                MulticastSocket(5353).use { socket ->
                    socket.soTimeout = 500
                    val group = InetAddress.getByName("224.0.0.251")
                    socket.joinGroup(group)

                    val query = byteArrayOf(
                        0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                        0x09, 0x5F, 0x73, 0x65, 0x72, 0x76, 0x69, 0x63, 0x65, 0x73,
                        0x07, 0x5F, 0x64, 0x6E, 0x73, 0x2D, 0x73, 0x64,
                        0x04, 0x5F, 0x75, 0x64, 0x70,
                        0x05, 0x6C, 0x6F, 0x63, 0x61, 0x6C,
                        0x00, 0x00, 0x0C, 0x00, 0x01
                    )

                    val packet = DatagramPacket(query, query.size, group, 5353)
                    socket.send(packet)

                    val buffer = ByteArray(1024)
                    val startTime = System.currentTimeMillis()
                    while (System.currentTimeMillis() - startTime < 500) {
                        try {
                            val recvPacket = DatagramPacket(buffer, buffer.size)
                            socket.receive(recvPacket)
                            val ip = recvPacket.address.hostAddress
                            if (ip != null && !ip.startsWith("127.")) {
                                mDnsCache[ip] = MdnsInfo(name = "mDNS Device", type = "_services._dns-sd._udp")
                            }
                        } catch (_: Exception) { break }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private suspend fun triggerArpCache(ips: List<String>) = coroutineScope {
        // Send fast non-blocking UDP broadcast + unicast to trigger L2 ARP response on multiple ports
        launch(Dispatchers.IO) {
            try {
                DatagramSocket().use { socket ->
                    socket.broadcast = true
                    val data = "ARP_TRIGGER".toByteArray()
                    val parts = ips.firstOrNull()?.split(".")
                    if (parts?.size == 4) {
                        val broadcastAddr = "${parts[0]}.${parts[1]}.${parts[2]}.255"
                        for (port in intArrayOf(7, 137, 5353, 1900)) {
                            try {
                                val packet = DatagramPacket(data, data.size, InetAddress.getByName(broadcastAddr), port)
                                socket.send(packet)
                            } catch (_: Exception) {}
                        }
                    }

                    // Unicast UDP to all targets to wake up Wi-Fi sleep radios
                    ips.forEach { targetIp ->
                        try {
                            val packet = DatagramPacket(data, data.size, InetAddress.getByName(targetIp), 137)
                            socket.send(packet)
                        } catch (_: Exception) {}
                    }
                }
            } catch (_: Exception) {}
        }
        delay(250) // Wait 250ms for Wi-Fi DTIM beacon frames & kernel ARP replies to populate
    }

    private fun startMdnsDiscovery(context: Context) {
        val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        val services = listOf("_http._tcp.", "_googlecast._tcp.", "_hap._tcp.", "_smb._tcp.", "_printer._tcp.", "_airplay._tcp.", "_workstation._tcp.", "_ssh._tcp.", "_ipp._tcp.")

        services.forEach { serviceType ->
            try {
                val listener = object : NsdManager.DiscoveryListener {
                    override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
                    override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
                    override fun onDiscoveryStarted(serviceType: String) {}
                    override fun onDiscoveryStopped(serviceType: String) {}

                    override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                        nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
                            override fun onServiceResolved(resolvedInfo: NsdServiceInfo) {
                                val ip = resolvedInfo.host?.hostAddress
                                if (ip != null) {
                                    mDnsCache[ip] = MdnsInfo(
                                        name = resolvedInfo.serviceName,
                                        type = resolvedInfo.serviceType,
                                        host = resolvedInfo.host.canonicalHostName
                                    )
                                }
                            }
                        })
                    }

                    override fun onServiceLost(serviceInfo: NsdServiceInfo) {}
                }
                activeNsdListeners[serviceType] = Pair(nsdManager, listener)
                nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener)
            } catch (_: Exception) {}
        }
    }

    private fun stopMdnsDiscovery() {
        activeNsdListeners.forEach { (_, pair) ->
            try {
                pair.first.stopServiceDiscovery(pair.second)
            } catch (_: Exception) {}
        }
        activeNsdListeners.clear()
    }

    private fun startSsdpDiscovery(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val devices = UpnpManager.scan(context)
                devices.forEach { device ->
                    ssdpCache[device.ip] = device
                }
            } catch (_: Exception) {}
        }
    }

    // MULTI-PROBE PARALLEL LIVENESS DETECTION PER HOST
    private suspend fun fastMultiProbeHost(host: String): Triple<Boolean, Long, List<String>> = coroutineScope {
        val methods = mutableListOf<String>()
        val start = System.currentTimeMillis()

        // 1. Check Caches First (ARP, mDNS, SSDP)
        if (arpCache.containsKey(host)) methods.add("ARP")
        if (ssdpCache.containsKey(host)) methods.add("SSDP")
        if (mDnsCache.containsKey(host)) methods.add("mDNS")

        if (methods.isNotEmpty()) {
            if (!netbiosCache.containsKey(host)) {
                launch(Dispatchers.IO) {
                    val info = NetBiosResolver.resolve(host, 250)
                    if (info != null) {
                        netbiosCache[host] = info
                        if (!info.mac.isNullOrBlank() && info.mac != "00:00:00:00:00:00") {
                            arpCache[host] = info.mac
                        }
                    }
                }
            }
            return@coroutineScope Triple(true, System.currentTimeMillis() - start, methods)
        }

        // 2. Parallel Multi-Probe (TCP + NetBIOS + ICMP) with Short-Circuit
        val successDeferred = CompletableDeferred<String>()

        // Launch Parallel TCP Probes for Windows SMB/RPC/RDP + Common Services
        DISCOVERY_PORTS.forEach { port ->
            launch(Dispatchers.IO) {
                try {
                    Socket().use { it.connect(InetSocketAddress(host, port), FAST_PROBE_TIMEOUT_MS) }
                    if (!successDeferred.isCompleted) successDeferred.complete("TCP:$port")
                } catch (_: Exception) {}
            }
        }

        // Launch NetBIOS Query (Port 137 UDP - Critical for Windows PCs)
        launch(Dispatchers.IO) {
            val info = NetBiosResolver.resolve(host, FAST_PROBE_TIMEOUT_MS)
            if (info != null) {
                netbiosCache[host] = info
                if (!info.mac.isNullOrBlank() && info.mac != "00:00:00:00:00:00") {
                    arpCache[host] = info.mac
                }
                if (!successDeferred.isCompleted) successDeferred.complete("NetBIOS")
            }
        }

        // Launch ICMP Echo Reachability (InetAddress)
        launch(Dispatchers.IO) {
            try {
                if (InetAddress.getByName(host).isReachable(FAST_PROBE_TIMEOUT_MS)) {
                    if (!successDeferred.isCompleted) successDeferred.complete("ICMP")
                }
            } catch (_: Exception) {}
        }

        var winningMethod = withTimeoutOrNull(FAST_PROBE_TIMEOUT_MS.toLong() + 20) {
            successDeferred.await()
        }

        // 3. Quick 2nd pass retry if 1st pass timed out (wakes sleeping WiFi radios)
        if (winningMethod == null) {
            val retryDeferred = CompletableDeferred<String>()
            launch(Dispatchers.IO) {
                try {
                    if (InetAddress.getByName(host).isReachable(200)) {
                        if (!retryDeferred.isCompleted) retryDeferred.complete("ICMP")
                    }
                } catch (_: Exception) {}
            }
            intArrayOf(445, 135, 139, 80).forEach { port ->
                launch(Dispatchers.IO) {
                    try {
                        Socket().use { it.connect(InetSocketAddress(host, port), 200) }
                        if (!retryDeferred.isCompleted) retryDeferred.complete("TCP:$port")
                    } catch (_: Exception) {}
                }
            }
            winningMethod = withTimeoutOrNull(220) {
                retryDeferred.await()
            }
        }

        if (winningMethod != null) {
            methods.add(winningMethod)
            Triple(true, System.currentTimeMillis() - start, methods)
        } else {
            Triple(false, 0L, emptyList())
        }
    }

    private fun getBestMacAddress(host: String): String {
        val netbiosMac = netbiosCache[host]?.mac
        if (!netbiosMac.isNullOrBlank() && netbiosMac != "00:00:00:00:00:00") {
            return netbiosMac
        }

        val arpMac = getMacFromArpCache(host)
        if (arpMac.isNotBlank() && arpMac != "00:00:00:00:00:00") {
            return arpMac
        }

        return ""
    }

    private suspend fun enrichDevice(
        context: Context,
        device: Device,
        gatewayIp: String
    ): Device = coroutineScope {
        val hostname = resolveHostname(device.ip)

        val updatedMethods = device.discoveryMethods.toMutableList()
        if (mDnsCache.containsKey(device.ip)) updatedMethods.add("mDNS")
        if (ssdpCache.containsKey(device.ip)) updatedMethods.add("SSDP")
        if (netbiosCache.containsKey(device.ip)) updatedMethods.add("NetBIOS")

        val mac = if (device.mac.isBlank() || device.mac == "00:00:00:00:00:00") getBestMacAddress(device.ip) else device.mac

        assembleDevice(context, device.ip, mac, gatewayIp, device.latencyMs, hostname, updatedMethods, device.openPorts)
    }

    private fun assembleInitialDevice(
        host: String,
        mac: String,
        gatewayIp: String,
        latency: Long,
        methods: List<String>
    ): Device {
        val netbios = netbiosCache[host]
        val effectiveMac = if (mac == "00:00:00:00:00:00") netbios?.mac ?: "" else mac
        val initialVendor = if (effectiveMac.isNotBlank() && effectiveMac != "00:00:00:00:00:00") VendorDatabase.getVendorSync(effectiveMac) else "Resolving..."

        return Device(
            ip = host,
            hostname = netbios?.name ?: "",
            netbiosName = netbios?.name,
            netbiosDomain = netbios?.domain,
            mac = effectiveMac,
            vendor = if (initialVendor.isNotEmpty() && initialVendor != "Unknown") initialVendor else "Resolving...",
            latencyMs = latency,
            isGateway = host == gatewayIp,
            isOnline = true,
            discoveryMethods = methods
        )
    }

    // LAYERED HOSTNAME RESOLUTION
    private suspend fun resolveHostname(ip: String): String? = withTimeoutOrNull(ENRICHMENT_TIMEOUT_MS) {
        // 1. mDNS .local name
        mDnsCache[ip]?.name?.let { if (it.isNotBlank() && it != "mDNS Device") return@withTimeoutOrNull it }

        // 2. NetBIOS name
        netbiosCache[ip]?.name?.let { if (it.isNotBlank() && it != "Unknown") return@withTimeoutOrNull it }

        // 3. SSDP friendlyName
        ssdpCache[ip]?.friendlyName?.let { if (it.isNotBlank() && it != "UPnP Device") return@withTimeoutOrNull it }

        // 4. Reverse DNS (PTR) via gateway
        try {
            val addr = InetAddress.getByName(ip)
            val canonical = addr.canonicalHostName
            if (canonical != ip && !canonical.contains(".in-addr.arpa", true)) {
                return@withTimeoutOrNull canonical
            }
        } catch (_: Exception) {}

        null
    }

    private suspend fun assembleDevice(
        context: Context,
        host: String,
        mac: String,
        gatewayIp: String,
        latencyMs: Long,
        resolvedHostname: String?,
        methods: List<String>,
        ports: List<Int> = emptyList()
    ): Device {
        val netbios = netbiosCache[host]
        val mdns = mDnsCache[host]
        val ssdp = ssdpCache[host]

        val finalHostname = resolvedHostname ?: netbios?.name ?: mdns?.name ?: ssdp?.friendlyName ?: ""
        val isRandom = VendorDatabase.isRandomized(mac)
        val vData = getVendorEnhanced(context, host, mac, gatewayIp, finalHostname, ports, mdns, ssdp)

        var finalVendor = vData.vendor
        if (isRandom && finalVendor == "Unknown") {
            finalVendor = if (finalHostname.isNotBlank()) "Likely ${finalHostname.split(" ").first()} (Randomized MAC)" else "Randomized MAC (Private)"
        }

        return Device(
            ip = host,
            hostname = finalHostname,
            netbiosName = netbios?.name,
            netbiosDomain = netbios?.domain,
            mac = mac,
            vendor = finalVendor,
            deviceCategory = vData.category,
            deviceType = vData.type,
            latencyMs = latencyMs,
            isGateway = host == gatewayIp,
            isOnline = true,
            lastSeen = System.currentTimeMillis(),
            isRandomMac = isRandom,
            discoveryMethods = methods.distinct(),
            openPorts = ports,
            hasPerformedPortScan = ports.isNotEmpty()
        )
    }

    private suspend fun getVendorEnhanced(
        context: Context,
        ip: String,
        mac: String,
        gatewayIp: String,
        hostname: String,
        ports: List<Int>,
        mdns: MdnsInfo?,
        ssdp: UpnpDevice?
    ): VendorData {
        // 1. SSDP/UPnP Fingerprinting
        ssdp?.let {
            if (it.manufacturer.isNotEmpty() && it.manufacturer != "Unknown") {
                return VendorData(it.manufacturer, "Digital Home", it.deviceType.split(":").lastOrNull() ?: "Device")
            }
            if (it.friendlyName.contains("Xiaomi", true)) return VendorData("Xiaomi", "Mobile", "Phone")
            if (it.friendlyName.contains("Samsung", true)) return VendorData("Samsung", "Mobile", "Phone")
        }

        // 2. mDNS Fingerprinting
        mdns?.let {
            val type = it.type.lowercase()
            return when {
                type.contains("googlecast") -> VendorData("Google", "Multimedia", "Cast Device")
                type.contains("airplay") || type.contains("raop") -> VendorData("Apple", "Multimedia", "AirPlay Device")
                type.contains("hap") -> VendorData("Apple", "Home Automation", "HomeKit Accessory")
                type.contains("printer") -> VendorData("Network Printer", "Peripherals", "Printer")
                it.name.contains("Xiaomi", true) -> VendorData("Xiaomi", "Mobile", "Phone")
                else -> VendorData("Unknown")
            }.takeIf { v -> v.vendor != "Unknown" } ?: VendorData("Unknown")
        }

        // 3. Port Fingerprinting
        if (ports.contains(445) || ports.contains(139) || ports.contains(3389)) return VendorData("Microsoft / PC", "Computer", "PC")
        if (ports.contains(62078)) return VendorData("Apple", "Mobile", "iPhone/iPad")
        if (ports.contains(8008) || ports.contains(8009)) return VendorData("Google", "Multimedia", "Cast Device")
        if (ports.contains(5000) && ports.contains(7000)) return VendorData("Apple", "Multimedia", "AirPlay Device")
        if (ports.contains(9100)) return VendorData("Network Printer", "Peripherals", "Printer")

        // 4. Hostname / NetBIOS Pattern Matching
        val h = hostname.lowercase()
        val identity = when {
            h.contains("xiaomi") -> VendorData("Xiaomi", "Mobile", "Phone")
            h.contains("iphone") || h.contains("ipad") || h.contains("apple") -> VendorData("Apple", "Mobile", "Phone")
            h.contains("samsung") || h.contains("galaxy") -> VendorData("Samsung", "Mobile", "Phone")
            h.contains("esp_") || h.contains("espressif") -> VendorData("Espressif", "IoT", "Smart Device")
            h.contains("android") -> VendorData("Android Device", "Mobile", "Phone")
            h.contains("laptop") || h.contains("desktop") || h.contains("pc-") || h.contains("-pc") -> VendorData("PC/Laptop", "Computer", "PC")
            h.contains("chromecast") -> VendorData("Google", "Multimedia", "Cast Device")
            h.contains("nvr") || h.contains("dvr") || h.contains("camera") -> VendorData("Surveillance", "Security", "Camera")
            else -> null
        }
        if (identity != null) return identity

        // 5. Local OUI Database Lookup
        if (mac.isNotBlank() && mac != "00:00:00:00:00:00") {
            val baseData = VendorDatabase.getVendorData(context, mac)
            if (baseData.vendor != "Unknown") return baseData
        }

        // 6. HTTP Banner Grabbing
        if (ip == gatewayIp || ip.endsWith(".1")) {
            val response = NetworkUtils.httpGet("http://$ip/", 500)
            val lowerResponse = response.lowercase()
            return when {
                lowerResponse.contains("zte") || lowerResponse.contains("fiberhome") -> VendorData("ZTE FiberHome", "Infrastructure", "Router")
                lowerResponse.contains("huawei") -> VendorData("Huawei", "Infrastructure", "Router")
                lowerResponse.contains("tp-link") -> VendorData("TP-Link", "Infrastructure", "Router")
                lowerResponse.contains("tenda") -> VendorData("Tenda", "Infrastructure", "Router")
                lowerResponse.contains("d-link") -> VendorData("D-Link", "Infrastructure", "Router")
                else -> VendorData("Unknown")
            }
        }

        return VendorData("Unknown")
    }

    private fun populateArpCache() {
        try {
            val file = File("/proc/net/arp")
            if (file.exists()) {
                file.forEachLine { line ->
                    if (line.contains(":")) {
                        val parts = line.split(Regex("\\s+")).filter { it.isNotBlank() }
                        if (parts.size >= 4) {
                            val ip = parts[0]
                            val mac = parts[3]
                            if (mac.contains(":") && mac != "00:00:00:00:00:00") {
                                arpCache[ip] = mac.uppercase()
                            }
                        }
                    }
                }
            }

            val process = Runtime.getRuntime().exec("ip neighbor show")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val lineNotNull = line!!
                val parts = lineNotNull.split(Regex("\\s+")).filter { it.isNotBlank() }
                val ip = parts.firstOrNull() ?: continue
                val macIndex = parts.indexOf("lladdr")
                if (macIndex != -1 && macIndex + 1 < parts.size) {
                    val foundMac = parts[macIndex + 1].uppercase()
                    if (foundMac != "00:00:00:00:00:00") arpCache[ip] = foundMac
                }
            }
        } catch (_: Exception) {}
    }

    private fun getMacFromArpCache(ip: String): String {
        return arpCache[ip] ?: ""
    }

    suspend fun scanPorts(ip: String): List<Int> = withContext(Dispatchers.IO) {
        SCAN_PORTS.map { port ->
            async {
                try {
                    Socket().use { it.connect(InetSocketAddress(ip, port), 200) }
                    port
                } catch (_: Exception) { null }
            }
        }.awaitAll().filterNotNull()
    }
}
