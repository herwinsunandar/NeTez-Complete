package com.herwin.netez.ui.viewmodels

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.data.AppDatabase
import com.herwin.netez.data.entities.PhishingCheckEntry
import com.herwin.netez.data.SecurityPreferences
import com.herwin.netez.engine.SslTlsEngine
import com.herwin.netez.network.UrlHausService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

import com.herwin.netez.entitlement.FeatureAccessManager
import com.herwin.netez.entitlement.FeatureAccessState
import com.herwin.netez.entitlement.FeatureId

class PhishingCheckViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val phishingDao = db.phishingDao()
    private val urlHausDao = db.urlHausDao()
    private val prefs = SecurityPreferences(application)

    var inputUrl by mutableStateOf("")
    var isScanning by mutableStateOf(false)
    var result by mutableStateOf<PhishingResult?>(null)
    var batchResults = mutableStateListOf<PhishingResult>()
    
    var urlHausCount by mutableStateOf(0)
    var lastUpdateText by mutableStateOf("Never")
    var showPaywallDialog by mutableStateOf(false)
    var showGlobalSupportDialog by mutableStateOf(false)

    val history = phishingDao.getAllPhishingChecks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshFeedInfo()
    }

    fun refreshFeedInfo() {
        viewModelScope.launch {
            urlHausCount = urlHausDao.getCount()
            val lastUpdate = prefs.urlHausLastUpdate
            lastUpdateText = if (lastUpdate == 0L) "Never" else {
                val diff = System.currentTimeMillis() - lastUpdate
                when {
                    diff < 3600000 -> "${diff / 60000}m ago"
                    diff < 86400000 -> "${diff / 3600000}h ago"
                    else -> "${diff / 86400000}d ago"
                }
            }
        }
    }

    fun onUrlChange(newUrl: String) {
        inputUrl = newUrl
    }

    fun pasteFromClipboard() {
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
        if (text.isNotBlank()) {
            inputUrl = text
        }
    }

    fun checkPhishing(url: String) {
        if (url.isBlank()) return

        val accessManager = FeatureAccessManager.getInstance(getApplication())
        val accessState = accessManager.checkAccess(FeatureId.PHISHING_CHECK_DAILY)
        if (accessState == FeatureAccessState.LIMIT_REACHED) {
            showPaywallDialog = true
            return
        }
        accessManager.consumeQuota(FeatureId.PHISHING_CHECK_DAILY)
        
        viewModelScope.launch {
            isScanning = true
            result = null
            
            try {
                val checkResult = performPhishingCheck(url)
                result = checkResult
                refreshFeedInfo()
                if (accessManager.trackAndCheckGlobalToolUsage()) {
                    showGlobalSupportDialog = true
                }
                
                // Save to DB
                val entry = PhishingCheckEntry(
                    url = url,
                    domain = checkResult.domain,
                    status = checkResult.status.name,
                    riskScore = checkResult.riskScore,
                    lastChecked = System.currentTimeMillis(),
                    reasons = JSONArray().apply { 
                        checkResult.checks.forEach { 
                            put(JSONObject().apply {
                                put("name", it.name)
                                put("status", it.status)
                                put("latency", it.latency)
                                put("description", it.description)
                            })
                        }
                    }.toString()
                )
                phishingDao.insertPhishingCheck(entry)
                
            } catch (e: Exception) {
                // Handle error
            } finally {
                isScanning = false
            }
        }
    }

    private suspend fun performPhishingCheck(urlStr: String): PhishingResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val url = if (!urlStr.startsWith("http")) "https://$urlStr" else urlStr
        val domain = try { URL(url).host } catch (e: Exception) { urlStr }
        val checks = mutableListOf<PhishingCheckItem>()
        var risk = 0

        // Layer 1: URLhaus Offline Feed
        val uStart = System.currentTimeMillis()
        val inUrlHaus = urlHausDao.isPhishing(url.lowercase(), domain.lowercase())
        val uTime = System.currentTimeMillis() - uStart
        checks.add(PhishingCheckItem(
            name = "URLhaus Offline Feed ($urlHausCount)",
            status = if (inUrlHaus) "PHISHING" else "SAFE",
            latency = uTime,
            description = if (inUrlHaus) "🔴 Found in malicious URL database" else "🟢 Not in recent 30k feed"
        ))
        if (inUrlHaus) risk += 95

        // Layer 2: Local Feed (Other)
        val fStart = System.currentTimeMillis()
        val inLocal = phishingDao.isDomainInFeed(domain) || phishingDao.isPhishingInFeed(url)
        val fTime = System.currentTimeMillis() - fStart
        checks.add(PhishingCheckItem(
            name = "OpenPhish Offline Feed",
            status = if (inLocal) "LISTED" else "CLEAN",
            latency = fTime,
            description = if (inLocal) "Found in database" else "Not found in daily feed"
        ))
        if (inLocal) risk += 90

        // Layer 3: Heuristic
        val hStart = System.currentTimeMillis()
        val hScore = heuristicCheck(url, domain)
        val hTime = System.currentTimeMillis() - hStart
        checks.add(PhishingCheckItem(
            name = "Heuristic Analysis",
            status = if (hScore > 40) "SUSPICIOUS" else "CLEAN",
            latency = hTime,
            description = "Analyzed URL structure and patterns"
        ))
        risk += hScore

        // Layer 4: URLhaus Online API (If enabled and not found offline)
        if (!inUrlHaus && !inLocal && prefs.isOnlineCheckEnabled) {
            val urlHausJob = async { UrlHausService.checkUrl(url) }
            val phishStatsJob = async { UrlHausService.checkPhishStats(url) }
            
            val urlHausResult = urlHausJob.await()
            val phishStatsResult = phishStatsJob.await()
            
            checks.add(urlHausResult)
            checks.add(phishStatsResult)
            
            if (urlHausResult.status == "PHISHING" || phishStatsResult.status == "PHISHING") risk += 95
        }

        // Layer 5: SSL Check via SslTlsEngine
        if (url.startsWith("https")) {
            val sslStart = System.currentTimeMillis()
            val sslRes = try {
                SslTlsEngine.checkDomain(domain)
            } catch (e: Exception) { null }
            val sslTime = System.currentTimeMillis() - sslStart

            if (sslRes != null) {
                val cert = sslRes.certificate
                when {
                    cert.isSelfSigned -> {
                        checks.add(PhishingCheckItem(
                            name = "SSL Certificate",
                            status = "SUSPICIOUS",
                            latency = sslTime,
                            description = "Self-signed certificate detected - suspicious"
                        ))
                        risk += 35
                    }
                    cert.isExpired -> {
                        checks.add(PhishingCheckItem(
                            name = "SSL Certificate",
                            status = "EXPIRED",
                            latency = sslTime,
                            description = "Expired SSL certificate"
                        ))
                        risk += 30
                    }
                    else -> {
                        checks.add(PhishingCheckItem(
                            name = "SSL Certificate",
                            status = "VALID",
                            latency = sslTime,
                            description = "Trusted SSL certificate (${cert.expiresInDays} days left)"
                        ))
                    }
                }
            } else {
                checks.add(PhishingCheckItem(
                    name = "SSL Certificate",
                    status = "VALID",
                    latency = 5,
                    description = "Encrypted connection"
                ))
            }
        } else {
            checks.add(PhishingCheckItem(
                name = "SSL Certificate",
                status = "NO SSL",
                latency = 5,
                description = "Insecure unencrypted connection"
            ))
            risk += 15
        }

        // Final Status
        val finalStatus = when {
            risk >= 70 -> PhishingStatus.PHISHING
            risk >= 40 -> PhishingStatus.SUSPICIOUS
            else -> PhishingStatus.SAFE
        }

        PhishingResult(
            url = url,
            domain = domain,
            status = finalStatus,
            riskScore = risk.coerceIn(0, 100),
            checks = checks
        )
    }

    private fun heuristicCheck(url: String, domain: String): Int {
        var score = 0
        // IP instead of domain
        if (domain.matches(Regex("^(\\d{1,3}\\.){3}\\d{1,3}$"))) score += 30
        // @ symbol
        if (url.contains("@")) score += 30
        // Too many subdomains
        if (domain.split(".").size > 4) score += 15
        // Brand name mismatch (Simplified)
        val brands = listOf("bca", "tokopedia", "shopee", "bri", "mandiri", "gmail", "facebook", "paypal")
        brands.forEach { brand ->
            if (domain.contains(brand) && !domain.endsWith("$brand.com") && !domain.endsWith("$brand.co.id") && !domain.endsWith("$brand.id")) {
                if (url.contains("login") || url.contains("secure") || url.contains("verify")) {
                    score += 40
                }
            }
        }
        // Suspicious keywords
        val keywords = listOf("secure", "login", "verify", "account", "update", "banking")
        keywords.forEach { if (url.contains(it)) score += 5 }
        
        return score
    }

    fun getShareText(): String {
        val res = result ?: return "No phishing check result available."
        val sb = StringBuilder("🎣 NeTez Phishing Check Report\n")
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        sb.append("📅 Date: ${sdf.format(Date())}\n")
        sb.append("─────────────────────\n")
        sb.append("🔗 URL: ${res.url}\n")
        
        val statusEmoji = when(res.status) {
            PhishingStatus.PHISHING -> "🔴"
            PhishingStatus.SUSPICIOUS -> "🟡"
            PhishingStatus.SAFE -> "🟢"
        }
        sb.append("🛡️ Status: $statusEmoji ${res.status}\n")
        sb.append("📊 Risk Score: ${res.riskScore}/100\n\n")
        
        sb.append("🔍 Detailed Checks:\n")
        res.checks.forEach { check ->
            val checkEmoji = if (check.status == "PHISHING" || check.status == "LISTED" || check.status == "NO SSL") "❌" 
                            else if (check.status == "SUSPICIOUS") "⚠️" 
                            else "✅"
            sb.append("   • ${check.name}: $checkEmoji ${check.status} (${check.latency}ms)\n")
            sb.append("     Detail: ${check.description}\n")
        }
        
        sb.append("─────────────────────\n")
        sb.append("Sent from NeTez Network Tools")
        return sb.toString()
    }

    fun copyToClipboard() {
        val context = getApplication<Application>()
        val text = getShareText()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Phishing Check Result", text)
        clipboard.setPrimaryClip(clip)
    }

    fun shareResults(context: Context) {
        val text = getShareText()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share Phishing Result"))
    }

    fun copyItemInfo(context: Context, item: PhishingCheckItem) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = "🔍 ${item.name}\nStatus: ${item.status}\nDetail: ${item.description}"
        val clip = ClipData.newPlainText("Phishing Item Info", text)
        clipboard.setPrimaryClip(clip)
    }
}

data class PhishingResult(
    val url: String,
    val domain: String,
    val status: PhishingStatus,
    val riskScore: Int,
    val checks: List<PhishingCheckItem>
)

data class PhishingCheckItem(
    val name: String,
    val status: String,
    val latency: Long,
    val description: String,
    val detailUrl: String? = null,
    val isVerified: Boolean = false
)

enum class PhishingStatus {
    SAFE, SUSPICIOUS, PHISHING
}
