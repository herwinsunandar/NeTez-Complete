package com.herwin.netez.ui.viewmodels

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.util.Patterns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.R
import com.herwin.netez.data.SettingsManager
import com.herwin.netez.network.*
import com.herwin.netez.network.api.*
import com.herwin.netez.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.*

import com.herwin.netez.entitlement.FeatureAccessManager
import com.herwin.netez.entitlement.FeatureAccessState
import com.herwin.netez.entitlement.FeatureId

class BlacklistViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsManager = SettingsManager(application)
    private val abuseIpDbService = AbuseIpDbService.create()
    private val shodanService = ShodanService.create()

    var inputQuery by mutableStateOf("")
    var detectedType by mutableStateOf("IP")
    var resolvedIp by mutableStateOf<String?>(null)
    var scannedIp by mutableStateOf("")
    var isScanning by mutableStateOf(false)
    var results by mutableStateOf<List<BlacklistResult>>(emptyList())
    var summary by mutableStateOf("")
    var error by mutableStateOf<String?>(null)
    var ipInfo by mutableStateOf<IpInfo?>(null)
    var riskScore by mutableFloatStateOf(0f)
    var abuseIpDbResponse by mutableStateOf<AbuseIpDbResponse?>(null)
    var shodanResponse by mutableStateOf<ShodanResponse?>(null)
    var showPaywallDialog by mutableStateOf(false)
    var showGlobalSupportDialog by mutableStateOf(false)

    fun onQueryChange(query: String) {
        inputQuery = query
        detectedType = if (Patterns.IP_ADDRESS.matcher(query.trim()).matches()) "IP" else "Domain"
    }

    fun fetchMyIp() {
        viewModelScope.launch {
            val ip = NetworkUtils.getPublicIp()
            if (ip != "Unknown") {
                inputQuery = ip
                detectedType = "IP"
            }
        }
    }

    fun checkIp(query: String) {
        val input = query.trim()
        if (input.isBlank()) {
            error = "Please enter an IP or Domain"
            return
        }

        val accessManager = FeatureAccessManager.getInstance(getApplication())
        val accessState = accessManager.checkAccess(FeatureId.BLACKLIST_CHECK_DAILY)
        if (accessState == FeatureAccessState.LIMIT_REACHED) {
            showPaywallDialog = true
            return
        }
        accessManager.consumeQuota(FeatureId.BLACKLIST_CHECK_DAILY)
        
        inputQuery = input
        isScanning = true
        error = null
        results = emptyList()
        summary = ""
        ipInfo = null
        riskScore = 0f
        resolvedIp = null
        abuseIpDbResponse = null
        shodanResponse = null

        viewModelScope.launch {
            val isIpInput = Patterns.IP_ADDRESS.matcher(input).matches()
            detectedType = if (isIpInput) "IP" else "Domain"
            
            // Guard for Private IP
            if (isIpInput) {
                val isPrivate = withContext(Dispatchers.IO) {
                    try {
                        val addr = InetAddress.getByName(input)
                        addr.isSiteLocalAddress || addr.isLoopbackAddress || addr.isLinkLocalAddress
                    } catch (e: Exception) { false }
                }
                if (isPrivate) {
                    error = getApplication<Application>().getString(R.string.blacklist_private_ip)
                    isScanning = false
                    return@launch
                }
            }

            val targetIp: String?
            val domainResults = mutableListOf<BlacklistResult>()
            
            if (isIpInput) {
                targetIp = input
            } else {
                val cleanDomain = input.lowercase()
                    .removePrefix("https://").removePrefix("http://")
                    .split("/")[0]
                
                // 1. Resolve Domain to IP
                targetIp = withContext(Dispatchers.IO) {
                    try {
                        val addr = InetAddress.getByName(cleanDomain)
                        // Also check if the resolved IP is private
                        if (addr.isSiteLocalAddress || addr.isLoopbackAddress || addr.isLinkLocalAddress) {
                            null
                        } else {
                            addr.hostAddress
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
                
                if (targetIp == null && !input.contains(".")) {
                    error = "Invalid domain or IP"
                    isScanning = false
                    return@launch
                }
                
                resolvedIp = targetIp
                
                // 2. Check Domain Blacklists
                domainResults.addAll(BlacklistManager.checkDomain(cleanDomain))
            }

            // 3. Check IP Blacklists (either the input IP or the resolved IP)
            val ipResults = mutableListOf<BlacklistResult>()
            if (targetIp != null) {
                scannedIp = targetIp
                
                // Fetch IP Info
                launch(Dispatchers.IO) {
                    val infoRes = WhoisManager.getInfo(targetIp)
                    if (infoRes is WhoisResult.Ip) {
                        withContext(Dispatchers.Main) {
                            ipInfo = infoRes.info
                        }
                    }
                }

                // Check DNSBL IP
                val rblResults = withContext(Dispatchers.IO) {
                    BlacklistManager.checkIp(targetIp)
                }
                ipResults.addAll(rblResults)

                // AbuseIPDB check
                launch(Dispatchers.IO) {
                    val apiKey = settingsManager.abuseIpDbKey.first()
                    if (apiKey.isNotEmpty()) {
                        try {
                            val response = abuseIpDbService.checkIp(apiKey, targetIp)
                            withContext(Dispatchers.Main) {
                                abuseIpDbResponse = response
                                // Recalculate risk score when pro info arrives
                                riskScore = BlacklistManager.calculateRiskScore(results, response.data.abuseConfidenceScore)
                            }
                        } catch (e: Exception) {}
                    }
                }

                // Shodan check
                launch(Dispatchers.IO) {
                    val sKey = settingsManager.shodanKey.first()
                    if (sKey.isNotEmpty()) {
                        try {
                            val response = shodanService.getHostInfo(targetIp, sKey)
                            withContext(Dispatchers.Main) {
                                shodanResponse = response
                            }
                        } catch (e: Exception) {}
                    }
                }
            }

            val allResults = domainResults + ipResults
            
            // Sort LISTED first
            results = allResults.sortedWith(compareByDescending<BlacklistResult> { it.status == BlacklistStatus.LISTED }.thenByDescending { it.weight })
            
            val listedCount = allResults.count { it.status == BlacklistStatus.LISTED }
            val cleanCount = allResults.count { it.status == BlacklistStatus.CLEAN }
            summary = "$cleanCount Clean / $listedCount Listed"
            
            // Calculate Initial Risk Score
            riskScore = BlacklistManager.calculateRiskScore(allResults, abuseIpDbResponse?.data?.abuseConfidenceScore)

            if (allResults.isNotEmpty() && allResults.all { it.status == BlacklistStatus.ERROR }) {
                error = allResults.firstOrNull()?.reason ?: "Unknown error"
            }
            
            isScanning = false
            if (accessManager.trackAndCheckGlobalToolUsage()) {
                showGlobalSupportDialog = true
            }
        }
    }

    fun getShareText(): String {
        if (results.isEmpty()) return ""
        
        val sb = StringBuilder("⚡ NeTez Blacklist Check Report\n")
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        sb.append("📅 Date: ${sdf.format(Date())}\n")
        sb.append("─────────────────────\n")
        sb.append("🎯 Target IP: $scannedIp\n")
        
        ipInfo?.let { info ->
            sb.append("📡 ISP: ${info.isp}\n")
            sb.append("📍 Location: ${info.city}, ${info.countryCode}\n")
            sb.append("📱 Type: ${if(info.mobile) "Mobile" else "Broadband"}\n")
        }
        
        sb.append("📊 Summary: $summary\n\n")
        
        val isListed = results.any { it.status == BlacklistStatus.LISTED }
        sb.append("🛡️ Overall Status: ${if (isListed) "🔴 LISTED / BLOCKED" else "🟢 CLEAN"}\n\n")
        
        sb.append("📍 Check Results:\n")
        results.forEachIndexed { i, res ->
            val statusEmoji = when(res.status) {
                BlacklistStatus.LISTED -> "🔴"
                BlacklistStatus.CLEAN -> "🟢"
                else -> "⚪"
            }
            sb.append("   [${i + 1}] ${res.name}: $statusEmoji ${res.status}\n")
            if ((res.resultIp != null) && (res.status == BlacklistStatus.LISTED)) {
                sb.append("       Ref IP: ${res.resultIp}\n")
            }
        }
        
        sb.append("─────────────────────\n")
        sb.append("Sent from NeTez Network Tools")
        return sb.toString()
    }

    fun copyToClipboard() {
        val context = getApplication<Application>()
        val text = getShareText()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Blacklist Check Results", text)
        clipboard.setPrimaryClip(clip)
    }

    fun shareResults(context: Context) {
        val text = getShareText()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share Blacklist Results"))
    }
}
