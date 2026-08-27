package com.herwin.netez.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.utils.NetworkUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class DashboardState(
    val connectionType: String = "Detecting...",
    val localIp: String = "0.0.0.0",
    val publicIp: String = "Loading...",
    val gateway: String = "0.0.0.0",
    val ssid: String = "Unknown",
    val signalStrength: String = "0 dBm",
    val linkSpeed: String = "0 Mbps",
    val securityType: String = "Unknown",
    val operatorName: String = "Unknown",
    val mobileNetworkType: String = "Unknown",
    val bssid: String = "",
    val isp: String = "Detecting...",
    val downloadHistory: List<Float> = List(60) { 0f },
    val uploadHistory: List<Float> = List(60) { 0f },
    val currentDownRate: Long = 0,
    val currentUpRate: Long = 0,
    val todayDownBytes: Long = 0,
    val todayUpBytes: Long = 0
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(DashboardState())
    val uiState: StateFlow<DashboardState> = _uiState

    private var lastRxBytes = android.net.TrafficStats.getTotalRxBytes()
    private var lastTxBytes = android.net.TrafficStats.getTotalTxBytes()

    init {
        refresh()
        startUsageMonitoring()
    }

    private fun startUsageMonitoring() {
        viewModelScope.launch {
            while (true) {
                val currentRx = android.net.TrafficStats.getTotalRxBytes()
                val currentTx = android.net.TrafficStats.getTotalTxBytes()
                
                val rxRate = if (lastRxBytes > 0) (currentRx - lastRxBytes).coerceAtLeast(0) else 0
                val txRate = if (lastTxBytes > 0) (currentTx - lastTxBytes).coerceAtLeast(0) else 0
                
                lastRxBytes = currentRx
                lastTxBytes = currentTx
                
                val newDownHistory = (_uiState.value.downloadHistory + rxRate.toFloat()).takeLast(60)
                val newUpHistory = (_uiState.value.uploadHistory + txRate.toFloat()).takeLast(60)
                
                _uiState.value = _uiState.value.copy(
                    downloadHistory = newDownHistory,
                    uploadHistory = newUpHistory,
                    currentDownRate = rxRate,
                    currentUpRate = txRate,
                    // Simulate today's usage for now based on session + random offset or persistent storage
                    // Ideally this should come from a DataUsageManager
                    todayDownBytes = _uiState.value.todayDownBytes + rxRate,
                    todayUpBytes = _uiState.value.todayUpBytes + txRate
                )
                
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            val type = NetworkUtils.getConnectionType(context)
            val ip = NetworkUtils.getLocalIpAddress() ?: "0.0.0.0"
            val gateway = NetworkUtils.getGateway(context)
            
            if (type == "WiFi") {
                val details = NetworkUtils.getWifiDetails(context)
                _uiState.value = _uiState.value.copy(
                    connectionType = type,
                    localIp = ip,
                    gateway = gateway,
                    ssid = details["SSID"] ?: "Unknown",
                    signalStrength = details["Signal Strength"] ?: "0 dBm",
                    linkSpeed = details["Link Speed"] ?: "0 Mbps",
                    securityType = details["Security"] ?: "Unknown",
                    bssid = details["BSSID"] ?: ""
                )
            } else if (type == "Cellular") {
                val details = NetworkUtils.getCellularDetails(context)
                _uiState.value = _uiState.value.copy(
                    connectionType = type,
                    localIp = ip,
                    gateway = "N/A (Mobile)",
                    ssid = details["Operator"] ?: "Unknown",
                    signalStrength = details["Signal Strength"] ?: "N/A",
                    linkSpeed = details["Network Type"] ?: "Unknown",
                    securityType = "Encrypted",
                    operatorName = details["Operator"] ?: "Unknown",
                    mobileNetworkType = details["Network Type"] ?: "Unknown",
                    bssid = ""
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    connectionType = type,
                    localIp = ip,
                    gateway = gateway,
                    ssid = "Disconnected",
                    signalStrength = "0 dBm",
                    linkSpeed = "0 Mbps",
                    securityType = "None",
                    bssid = ""
                )
            }

            // Fetch public IP and ISP in background
            launch {
                val pubIp = NetworkUtils.getPublicIp()
                val isp = NetworkUtils.getIspInfo()
                _uiState.value = _uiState.value.copy(publicIp = pubIp, isp = isp)
            }
        }
    }
}
