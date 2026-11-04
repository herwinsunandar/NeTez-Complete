package com.herwin.netez.ui.viewmodels

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.TrafficStats
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.data.AppDatabase
import com.herwin.netez.data.SecurityPreferences
import com.herwin.netez.engine.SecurityPostureEngine
import com.herwin.netez.engine.SecurityPostureResult
import com.herwin.netez.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    val todayUpBytes: Long = 0,
    val activeMonitors: Int = 0,
    val expiringSslCount: Int = 0,
    val breachedEmailCount: Int = 0,
    val phishingUrlCount: Int = 0,
    val securityPostureResult: SecurityPostureResult? = null
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val dao = database.monitoringDao()
    private val _uiState = MutableStateFlow(DashboardState())
    val uiState: StateFlow<DashboardState> = _uiState

    private var lastRxBytes = TrafficStats.getTotalRxBytes()
    private var lastTxBytes = TrafficStats.getTotalTxBytes()

    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var networkDebounceJob: Job? = null

    init {
        refresh()
        refreshSecurityPosture()
        startUsageMonitoring()
        registerNetworkCallback()
        observeMonitoring()
        observeSslReminders()
        observeLeakChecks()
        observePhishing()
    }

    private fun registerNetworkCallback() {
        val context = getApplication<Application>().applicationContext
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (cm != null) {
            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    onNetworkChanged()
                }

                override fun onLost(network: Network) {
                    onNetworkChanged()
                }

                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                    onNetworkChanged()
                }
            }
            try {
                cm.registerDefaultNetworkCallback(networkCallback!!)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun onNetworkChanged() {
        networkDebounceJob?.cancel()
        networkDebounceJob = viewModelScope.launch {
            delay(600) // Debounce 600ms to allow network transition to settle safely
            refresh()
            refreshSecurityPosture(quickOnly = true)
        }
    }

    private fun observePhishing() {
        viewModelScope.launch {
            try {
                database.phishingDao().getPhishingCount().collect { count ->
                    _uiState.value = _uiState.value.copy(phishingUrlCount = count)
                }
            } catch (_: Exception) {}
        }
    }

    private fun observeMonitoring() {
        viewModelScope.launch {
            try {
                dao.getActiveCount().collect { count ->
                    _uiState.value = _uiState.value.copy(activeMonitors = count)
                }
            } catch (_: Exception) {}
        }
    }

    private fun observeSslReminders() {
        viewModelScope.launch {
            try {
                val securityPrefs = SecurityPreferences(getApplication())
                val configDays = securityPrefs.defaultReminderDays.split(',').mapNotNull { it.trim().toIntOrNull() }
                val maxDays = configDays.maxOrNull() ?: 14
                database.sslDomainDao().getExpiringCount(maxReminderDays = maxDays).collect { count ->
                    _uiState.value = _uiState.value.copy(expiringSslCount = count)
                }
            } catch (_: Exception) {}
        }
    }

    private fun observeLeakChecks() {
        viewModelScope.launch {
            try {
                database.leakCheckDao().getBreachedCount().collect { count ->
                    _uiState.value = _uiState.value.copy(breachedEmailCount = count)
                }
            } catch (_: Exception) {}
        }
    }

    private fun startUsageMonitoring() {
        viewModelScope.launch {
            while (true) {
                try {
                    val currentRx = TrafficStats.getTotalRxBytes()
                    val currentTx = TrafficStats.getTotalTxBytes()

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
                        todayDownBytes = _uiState.value.todayDownBytes + rxRate,
                        todayUpBytes = _uiState.value.todayUpBytes + txRate
                    )
                } catch (_: Exception) {}

                delay(1000)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            try {
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
                    try {
                        val pubIp = NetworkUtils.getPublicIp()
                        val isp = NetworkUtils.getIspInfo()
                        _uiState.value = _uiState.value.copy(publicIp = pubIp, isp = isp)
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }
    }

    fun refreshSecurityPosture(quickOnly: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>().applicationContext
                val result = SecurityPostureEngine.calculate(context, quickOnly = quickOnly)
                _uiState.value = _uiState.value.copy(securityPostureResult = result)
            } catch (_: Exception) {}
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            networkDebounceJob?.cancel()
            val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            networkCallback?.let { cm?.unregisterNetworkCallback(it) }
        } catch (_: Exception) {}
    }
}
