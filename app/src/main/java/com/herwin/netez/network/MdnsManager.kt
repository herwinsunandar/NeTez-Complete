package com.herwin.netez.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.concurrent.ConcurrentHashMap

data class MdnsService(
    val name: String,
    val type: String,
    val host: String? = null,
    val port: Int = 0,
    val ipAddress: String? = null
)

class MdnsManager(context: Context) {
    private val nsdManager = context.applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val resolvedServices = ConcurrentHashMap<String, MdnsService>()
    private val resolveQueue = Channel<NsdServiceInfo>(Channel.UNLIMITED)
    private var isScanning = false
    private var scanJob: Job? = null

    private val serviceTypes = listOf(
        "_http._tcp.",
        "_printer._tcp.",
        "_workstation._tcp.",
        "_googlecast._tcp.",
        "_spotify-connect._tcp.",
        "_ipp._tcp.",
        "_airplay._tcp.",
        "_raop._tcp.",
        "_ssh._tcp.",
        "_smb._tcp."
    )

    private val discoveryListeners = mutableMapOf<String, NsdManager.DiscoveryListener>()

    interface MdnsCallback {
        fun onServiceFound(service: MdnsService)
        fun onScanFinished()
    }

    suspend fun startDiscovery(callback: MdnsCallback) = coroutineScope {
        if (isScanning) return@coroutineScope
        isScanning = true
        resolvedServices.clear()

        // Worker for resolving services one by one
        scanJob = launch(Dispatchers.IO) {
            for (serviceInfo in resolveQueue) {
                if (!isScanning) break
                resolveService(serviceInfo, callback)
                delay(100) // Small delay between resolves
            }
        }

        serviceTypes.forEach { type ->
            val listener = createDiscoveryListener(callback)
            discoveryListeners[type] = listener
            try {
                nsdManager.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, listener)
            } catch (e: Exception) {
                Log.e("MdnsManager", "Failed to start discovery for $type", e)
            }
        }

        // Auto stop after 10 seconds
        delay(10000)
        stopDiscovery()
        callback.onScanFinished()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun stopDiscovery() {
        if (!isScanning) return
        isScanning = false
        scanJob?.cancel()
        
        discoveryListeners.forEach { (_, listener) ->
            try {
                nsdManager.stopServiceDiscovery(listener)
            } catch (e: Exception) {
                Log.e("MdnsManager", "Failed to stop discovery", e)
            }
        }
        discoveryListeners.clear()
        
        // Drain the channel
        while (!resolveQueue.isEmpty) {
            resolveQueue.tryReceive()
        }
    }

    private fun createDiscoveryListener(callback: MdnsCallback): NsdManager.DiscoveryListener {
        return object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.e("MdnsManager", "Discovery failed: $errorCode")
            }

            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.e("MdnsManager", "Stop discovery failed: $errorCode")
            }

            override fun onDiscoveryStarted(serviceType: String?) {
                Log.d("MdnsManager", "Discovery started: $serviceType")
            }

            override fun onDiscoveryStopped(serviceType: String?) {
                Log.d("MdnsManager", "Discovery stopped: $serviceType")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Log.d("MdnsManager", "Service found: ${serviceInfo.serviceName}")
                resolveQueue.trySend(serviceInfo)
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Log.d("MdnsManager", "Service lost: ${serviceInfo.serviceName}")
            }
        }
    }

    private suspend fun resolveService(serviceInfo: NsdServiceInfo, callback: MdnsCallback) {
        val completer = CompletableDeferred<NsdServiceInfo?>()
        val listener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                Log.e("MdnsManager", "Resolve failed: $errorCode")
                if (!completer.isCompleted) completer.complete(null)
            }

            override fun onServiceResolved(resolvedInfo: NsdServiceInfo) {
                Log.d("MdnsManager", "Service resolved: ${resolvedInfo.serviceName} at ${resolvedInfo.host}")
                if (!completer.isCompleted) completer.complete(resolvedInfo)
            }
        }

        try {
            nsdManager.resolveService(serviceInfo, listener)
            val result = withTimeoutOrNull(2000) { completer.await() }
            if (result != null) {
                val mdnsService = MdnsService(
                    name = result.serviceName,
                    type = result.serviceType,
                    host = result.host?.hostName,
                    port = result.port,
                    ipAddress = result.host?.hostAddress
                )
                val key = "${mdnsService.name}_${mdnsService.type}_${mdnsService.ipAddress}"
                if (!resolvedServices.containsKey(key)) {
                    resolvedServices[key] = mdnsService
                    withContext(Dispatchers.Main) {
                        callback.onServiceFound(mdnsService)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MdnsManager", "Error resolving service", e)
            if (!completer.isCompleted) completer.complete(null)
        }
    }
}
