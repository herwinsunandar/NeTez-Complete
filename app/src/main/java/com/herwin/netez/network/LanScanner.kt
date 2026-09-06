package com.herwin.netez.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import com.herwin.netez.data.wifi.VendorData
import com.herwin.netez.data.wifi.VendorDatabase
import com.herwin.netez.utils.NetworkUtils
import android.util.Log
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

data class Device(
    val ip: String,
    val hostname: String = "Unknown",
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
    val riskScore: Int = 0
)

object LanScanner {

    private val DISCOVERY_PORTS = intArrayOf(22, 80, 443, 445, 139, 135, 3389) // Expanded discovery ports
    private val SCAN_PORTS = intArrayOf(
        445, 139, 135, // SMB / NetBIOS / RPC
        5357, 5985,    // WSD / WinRM
        22, 80, 443,   // SSH / HTTP / HTTPS
        3389, 8080,    // RDP / Alternative HTTP
        53             // DNS
    )
    private val mDnsCache = ConcurrentHashMap<String, String>() // IP -> Hostname
    private val ssdpCache = ConcurrentHashMap<String, String>() // IP -> Friendly Name
    private val arpCache = ConcurrentHashMap<String, String>() // IP -> MAC
    private val netbiosCache = ConcurrentHashMap<String, NetBiosResolver.NetBiosInfo>()
    
    private const val DISCOVERY_TIMEOUT_MS = 250L // Slightly shorter
    private const val ENRICHMENT_TIMEOUT_MS = 1000L
    private var multicastLock: WifiManager.MulticastLock? = null

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

        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        multicastLock = wifi.createMulticastLock("LanScannerMulticastLock").apply {
            setReferenceCounted(false)
            acquire()
        }

        val subnetInfo = NetworkUtils.getSubnetInfo(context)
        val ownIp = NetworkUtils.getLocalIpAddress()
        
        val ips = if (subnetInfo != null) {
            val startLong = NetworkUtils.ipToLong(subnetInfo.startIp)
            val endLong = NetworkUtils.ipToLong(subnetInfo.endIp)
            (startLong..endLong).map { NetworkUtils.longToIp(it) }.filter { it != ownIp }
        } else {
            (1..254).map { "$subnet.$it" }.filter { it != ownIp }
        }
        
        val total = ips.size
        Log.d("LanScanner", "SCAN_START network=${subnetInfo?.networkAddress ?: subnet}/${subnetInfo?.prefixLength ?: 24} ($total hosts)")
        val startTime = System.currentTimeMillis()

        // 1. Trigger ARP Cache & Populate
        triggerArpCache(ips)
        populateArpCache()
        Log.d("LanScanner", "ARP_CACHE_READ hosts=${arpCache.size}")

        // 2. Start Service Discovery
        startMdnsDiscovery(context)
        startSsdpDiscovery(context)

        val completed = AtomicInteger(0)
        val foundDevices = ConcurrentHashMap<String, Device>()
        val dispatcher = Dispatchers.IO.limitedParallelism(128)
        
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

        try {
            val jobs = ips.map { host ->
                async(dispatcher) {
                    try {
                        val discovery = fastProbeHost(host)
                        if (discovery.first) {
                            val mac = getMacFromArpCache(host)
                            val device = assembleInitialDevice(host, mac, gatewayIp, discovery.second, discovery.third)
                            foundDevices[host] = device
                            Log.d("LanScanner", "HOST_DISCOVERED ip=$host mac=$mac methods=${discovery.third.joinToString(",")}")
                            onDeviceFound(device)
                            
                            // Background Enrichment
                            launch(Dispatchers.IO) {
                                val enriched = enrichDevice(context, device, gatewayIp)
                                foundDevices[host] = enriched
                                onDeviceFound(enriched)
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
            val elapsed = System.currentTimeMillis() - startTime
            Log.d("LanScanner", "DISCOVERY_COMPLETE duration=${elapsed}ms hosts=${foundDevices.size}")
            onProgress(total, total)
            if (multicastLock?.isHeld == true) multicastLock?.release()
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

        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        multicastLock = wifi.createMulticastLock("LanScannerMulticastLock").apply {
            setReferenceCounted(false)
            acquire()
        }

        val startLong = NetworkUtils.ipToLong(startIp)
        val endLong = NetworkUtils.ipToLong(endIp)
        if (startLong > endLong) {
            if (multicastLock?.isHeld == true) multicastLock?.release()
            return@coroutineScope emptyList()
        }
        
        val ips = (startLong..endLong).map { NetworkUtils.longToIp(it) }
        val ownIp = NetworkUtils.getLocalIpAddress()
        
        val startTime = System.currentTimeMillis()
        Log.d("LanScanner", "SCAN_START range=$startIp-$endIp")

        triggerArpCache(ips)
        populateArpCache()

        startMdnsDiscovery(context)
        startSsdpDiscovery(context)

        val completed = AtomicInteger(0)
        val foundDevices = ConcurrentHashMap<String, Device>()
        val dispatcher = Dispatchers.IO.limitedParallelism(128)

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

        try {
            val jobs = filterIps.map { host ->
                async(dispatcher) {
                    try {
                        val discovery = fastProbeHost(host)
                        if (discovery.first) {
                            val mac = getMacFromArpCache(host)
                            val device = assembleInitialDevice(host, mac, gatewayIp, discovery.second, discovery.third)
                            foundDevices[host] = device
                            Log.d("LanScanner", "HOST_DISCOVERED ip=$host mac=$mac methods=${discovery.third.joinToString(",")}")
                            onDeviceFound(device)
                            
                            launch(Dispatchers.IO) {
                                val enriched = enrichDevice(context, device, gatewayIp)
                                foundDevices[host] = enriched
                                onDeviceFound(enriched)
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
            val elapsed = System.currentTimeMillis() - startTime
            Log.d("LanScanner", "DISCOVERY_COMPLETE duration=${elapsed}ms hosts=${foundDevices.size}")
            onProgress(totalCount, totalCount)
            if (multicastLock?.isHeld == true) multicastLock?.release()
        }
        
        foundDevices.values.toList().sortedBy { NetworkUtils.ipToLong(it.ip) }
    }

    private suspend fun triggerArpCache(ips: List<String>) = coroutineScope {
        // 1. Broadcast UDP to trigger ARP across subnet
        launch(Dispatchers.IO) {
            try {
                val socket = DatagramSocket()
                socket.broadcast = true
                val data = "ARP_TRIGGER".toByteArray()
                val parts = ips.first().split(".")
                if (parts.size == 4) {
                    val broadcastAddr = "${parts[0]}.${parts[1]}.${parts[2]}.255"
                    val packet = DatagramPacket(data, data.size, InetAddress.getByName(broadcastAddr), 7)
                    socket.send(packet)
                }
                socket.close()
            } catch (_: Exception) {}
        }

        // 2. Fast parallel probes (no waiting for result) to populate system ARP table
        val dispatcher = Dispatchers.IO.limitedParallelism(64)
        ips.forEach { ip ->
            launch(dispatcher) {
                try {
                    Socket().use { it.connect(InetSocketAddress(ip, 7), 50) }
                } catch (_: Exception) {}
            }
        }
        delay(300) // Small wait for ARP replies
    }

    private fun startMdnsDiscovery(context: Context) {
        val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        val services = listOf("_http._tcp.", "_googlecast._tcp.", "_hap._tcp.", "_smb._tcp.", "_printer._tcp.", "_airplay._tcp.", "_workstation._tcp.", "_ssh._tcp.")
        
        services.forEach { serviceType ->
            try {
                nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, object : NsdManager.DiscoveryListener {
                    override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
                    override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
                    override fun onDiscoveryStarted(serviceType: String) {}
                    override fun onDiscoveryStopped(serviceType: String) {}

                    override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                        nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
                            override fun onServiceResolved(resolvedInfo: NsdServiceInfo) {
                                val ip = resolvedInfo.host.hostAddress
                                val name = resolvedInfo.serviceName
                                if (ip != null) {
                                    mDnsCache[ip] = name
                                }
                            }
                        })
                    }

                    override fun onServiceLost(serviceInfo: NsdServiceInfo) {}
                })
            } catch(_: Exception) {}
        }
    }

    private fun startSsdpDiscovery(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val devices = UpnpManager.scan(context)
                devices.forEach { device ->
                    ssdpCache[device.ip] = device.friendlyName
                }
            } catch (_: Exception) {}
        }
    }

    private suspend fun fastProbeHost(host: String): Triple<Boolean, Long, List<String>> = coroutineScope {
        val methods = mutableListOf<String>()
        val start = System.currentTimeMillis()

        // 1. Check ARP Table & Service Caches (Instant)
        if (arpCache.containsKey(host)) methods.add("ARP")
        if (ssdpCache.containsKey(host)) methods.add("SSDP")
        if (mDnsCache.containsKey(host)) methods.add("mDNS")

        // 2. Parallel Active Probes
        val tcpProbes = DISCOVERY_PORTS.map { port ->
            async(Dispatchers.IO) {
                try {
                    Socket().use { it.connect(InetSocketAddress(host, port), DISCOVERY_TIMEOUT_MS.toInt()) }
                    true
                } catch (_: Exception) { false }
            }
        }

        val netbiosJob = async(Dispatchers.IO) {
            val info = NetBiosResolver.resolve(host, DISCOVERY_TIMEOUT_MS.toInt())
            if (info != null) {
                netbiosCache[host] = info
                if (info.mac != null && !arpCache.containsKey(host)) {
                    arpCache[host] = info.mac
                }
                true
            } else false
        }

        val icmpJob = async(Dispatchers.IO) {
            try {
                InetAddress.getByName(host).isReachable(DISCOVERY_TIMEOUT_MS.toInt())
            } catch (_: Exception) { false }
        }

        // Wait for all but we can optimize by checking results as they come
        if (tcpProbes.awaitAll().any { it }) methods.add("TCP")
        if (netbiosJob.await()) methods.add("NetBIOS")
        if (icmpJob.await()) methods.add("ICMP")

        val isAlive = methods.isNotEmpty()
        val latency = if (isAlive) System.currentTimeMillis() - start else 0L
        
        Triple(isAlive, latency, methods)
    }

    private suspend fun enrichDevice(
        context: Context, 
        device: Device, 
        gatewayIp: String
    ): Device = coroutineScope {
        val hostnameJob = async { resolveHostname(device.ip) }
        val portsJob = async { scanPorts(device.ip) }
        
        val hostname = hostnameJob.await()
        val ports = portsJob.await()
        
        val updatedMethods = device.discoveryMethods.toMutableList()
        if (mDnsCache.containsKey(device.ip)) updatedMethods.add("mDNS")
        if (ssdpCache.containsKey(device.ip)) updatedMethods.add("SSDP")
        if (netbiosCache.containsKey(device.ip)) updatedMethods.add("NetBIOS")
        
        val netbios = netbiosCache[device.ip]
        val mac = if (device.mac == "00:00:00:00:00:00") netbios?.mac ?: getMacFromArpCache(device.ip) else device.mac
        
        assembleDevice(context, device.ip, mac, gatewayIp, device.latencyMs, hostname, updatedMethods, ports)
    }

    private fun assembleInitialDevice(
        host: String,
        mac: String,
        gatewayIp: String,
        latency: Long,
        methods: List<String>
    ): Device {
        val netbios = netbiosCache[host]
        return Device(
            ip = host,
            hostname = "Probing...",
            netbiosName = netbios?.name,
            netbiosDomain = netbios?.domain,
            mac = if (mac == "00:00:00:00:00:00") netbios?.mac ?: "00:00:00:00:00:00" else mac,
            vendor = "Resolving...",
            latencyMs = latency,
            isGateway = host == gatewayIp,
            isOnline = true,
            discoveryMethods = methods
        )
    }

    private suspend fun resolveHostname(ip: String): String? = withTimeoutOrNull(ENRICHMENT_TIMEOUT_MS) {
        // 1. Check Caches first (Fast)
        mDnsCache[ip]?.let { return@withTimeoutOrNull it }
        ssdpCache[ip]?.let { return@withTimeoutOrNull it }
        netbiosCache[ip]?.name?.let { if (it != "Unknown") return@withTimeoutOrNull it }

        // 2. Reverse DNS (Potential blocking/slow)
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
        latency: Long,
        resolvedHostname: String?,
        methods: List<String>,
        ports: List<Int> = emptyList()
    ): Device {
        val netbios = netbiosCache[host]
        val finalHostname = resolvedHostname ?: netbios?.name ?: "Unknown"
        val isRandom = VendorDatabase.isRandomized(mac)
        val vData = getVendorEnhanced(context, host, mac, gatewayIp, finalHostname)
        
        var finalVendor = vData.vendor
        if (isRandom && finalVendor == "Unknown") {
            finalVendor = if (finalHostname != "Unknown") "Likely ${finalHostname.split(" ").first()} (Randomized MAC)" else "Randomized MAC (Private)"
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
            latencyMs = latency,
            isGateway = host == gatewayIp,
            isOnline = true,
            lastSeen = System.currentTimeMillis(),
            isRandomMac = isRandom,
            discoveryMethods = methods.distinct(),
            openPorts = ports,
            hasPerformedPortScan = ports.isNotEmpty()
        )
    }

    private suspend fun getVendorEnhanced(context: Context, ip: String, mac: String, gatewayIp: String, hostname: String): VendorData {
        // Fallback Identification
        if (mac == "00:00:00:00:00:00") {
            val h = hostname.lowercase()
            return when {
                h.contains("xiaomi") -> VendorData("Xiaomi", "Mobile", "Phone")
                h.contains("iphone") || h.contains("ipad") || h.contains("apple") -> VendorData("Apple", "Mobile", "Phone")
                h.contains("samsung") || h.contains("galaxy") -> VendorData("Samsung", "Mobile", "Phone")
                h.contains("esp_") || h.contains("espressif") -> VendorData("Espressif", "IoT", "Smart Device")
                h.contains("android") -> VendorData("Android Device", "Mobile", "Phone")
                h.contains("laptop") || h.contains("desktop") -> VendorData("PC/Laptop", "Computer", "Computer")
                h.contains("chromecast") || h.contains("google") -> VendorData("Google", "Multimedia", "Cast Device")
                else -> VendorData("Unknown")
            }
        }

        val baseData = VendorDatabase.getVendorData(context, mac)

        if (baseData.vendor == "Unknown") {
            // Gateway Enrichment
            if (ip == gatewayIp || ip.endsWith(".1")) {
                val banner = NetworkUtils.httpGet("http://$ip/", 800)
                if (banner.contains("ZTE", true) || banner.contains("FiberHome", true) || banner.contains("Huawei", true)) {
                    return VendorData("ISP Gateway", "Infrastructure", "Router")
                }
            }
        }
        
        return baseData
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
            
            // Try ip neighbor as fallback
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
        return arpCache[ip] ?: "00:00:00:00:00:00"
    }

    suspend fun scanPorts(ip: String): List<Int> = withContext(Dispatchers.IO) {
        // Only scan priority ports to be fast
        SCAN_PORTS.map { port ->
            async {
                try {
                    Socket().use { it.connect(InetSocketAddress(ip, port), 250) }
                    port
                } catch (_: Exception) { null }
            }
        }.awaitAll().filterNotNull()
    }
}
