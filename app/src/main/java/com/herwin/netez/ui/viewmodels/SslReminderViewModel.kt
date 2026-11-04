package com.herwin.netez.ui.viewmodels

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.data.AppDatabase
import com.herwin.netez.data.QuickNote
import com.herwin.netez.data.entities.SslDomain
import com.herwin.netez.engine.SslTlsEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.herwin.netez.entitlement.FeatureAccessManager
import com.herwin.netez.entitlement.FeatureAccessState
import com.herwin.netez.entitlement.FeatureId

class SslReminderViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val sslDao = db.sslDomainDao()
    private val quickNoteDao = db.quickNoteDao()

    var selectedTab by mutableStateOf(0) // 0: My Domains, 1: Instant Check
    var inputQuery by mutableStateOf("")
    var isScanning by mutableStateOf(false)
    var scanProgress by mutableStateOf(Pair(0, 0))
    var filterState by mutableStateOf("All")
    var showPaywallDialog by mutableStateOf(false)

    // Instant Check State
    var instantInput by mutableStateOf("")
    var isInstantChecking by mutableStateOf(false)
    var instantResult by mutableStateOf<SslTlsEngine.SslTlsResult?>(null)
    var instantError by mutableStateOf<String?>(null)
    var actionMessage by mutableStateOf<String?>(null)

    val domains = sslDao.getAllDomains()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onQueryChange(newQuery: String) {
        inputQuery = newQuery
    }

    fun onInstantInputChange(newInput: String) {
        instantInput = newInput
    }

    fun addDomain(domainStr: String) {
        val cleanDomain = cleanDomainString(domainStr)
        if (cleanDomain.isBlank()) return

        val accessManager = FeatureAccessManager.getInstance(getApplication())
        val currentCount = domains.value.size
        val state = accessManager.checkAccess(FeatureId.SSL_REMINDER_DOMAINS_MAX, currentCount)
        if (state == FeatureAccessState.LIMIT_REACHED) {
            showPaywallDialog = true
            return
        }

        viewModelScope.launch {
            isScanning = true
            try {
                val res = SslTlsEngine.checkDomain(cleanDomain)
                saveResultToDb(res)
                inputQuery = ""
            } catch (e: Exception) {
            } finally {
                isScanning = false
            }
        }
    }

    fun performInstantCheck(input: String? = null) {
        val target = input ?: instantInput
        val cleanDomain = cleanDomainString(target)
        if (cleanDomain.isBlank()) {
            instantError = "Please enter a valid domain"
            return
        }

        viewModelScope.launch {
            isInstantChecking = true
            instantError = null
            instantResult = null
            try {
                val res = SslTlsEngine.checkDomain(cleanDomain)
                instantResult = res
            } catch (e: Exception) {
                instantError = e.localizedMessage ?: "Failed to connect to domain"
            } finally {
                isInstantChecking = false
            }
        }
    }

    fun addInstantResultToMyDomains() {
        val res = instantResult ?: return
        viewModelScope.launch {
            saveResultToDb(res)
            actionMessage = "Added ${res.domain} to My Domains"
        }
    }

    private suspend fun saveResultToDb(res: SslTlsEngine.SslTlsResult) = withContext(Dispatchers.IO) {
        val existingDomain = sslDao.getDomain(res.domain)

        // Protect existing valid domain from network connection error overwrite
        if (res.certificate.issuer.startsWith("Error:", ignoreCase = true) && existingDomain != null && existingDomain.expiryDate > 0L) {
            sslDao.insertDomain(
                existingDomain.copy(
                    lastChecked = System.currentTimeMillis()
                )
            )
            return@withContext
        }

        val vulnCount = listOf(
            res.vulnerabilities.poodle,
            res.vulnerabilities.beast,
            res.vulnerabilities.robot
        ).count { it }

        val expiryTime = if (res.certificate.expiryDate.time > 0L && !res.certificate.issuer.startsWith("Error:", ignoreCase = true)) {
            res.certificate.expiryDate.time
        } else {
            existingDomain?.expiryDate ?: 0L
        }

        val sslDomain = SslDomain(
            domain = res.domain,
            groupName = existingDomain?.groupName ?: "Default",
            expiryDate = expiryTime,
            daysLeft = res.certificate.expiresInDays,
            issuer = if (!res.certificate.issuer.startsWith("Error:", ignoreCase = true)) res.certificate.issuer.substringBefore(',') else (existingDomain?.issuer ?: "Unknown"),
            lastChecked = System.currentTimeMillis(),
            reminderDays = existingDomain?.reminderDays ?: "1,3,7,14,30",
            notifiedDays = existingDomain?.notifiedDays ?: "",
            tlsVersion = if (res.tlsVersions.tls13) "TLS 1.3" else if (res.tlsVersions.tls12) "TLS 1.2" else "TLS 1.0",
            grade = if (!res.certificate.issuer.startsWith("Error:", ignoreCase = true)) res.grade.letter else (existingDomain?.grade ?: "F"),
            score = if (!res.certificate.issuer.startsWith("Error:", ignoreCase = true)) res.grade.score else (existingDomain?.score ?: 0),
            cipher = res.cipherSuites.firstOrNull()?.name ?: existingDomain?.cipher ?: "",
            vulnCount = vulnCount,
            chainValid = res.chain.complete,
            lastVulnCheck = System.currentTimeMillis()
        )
        sslDao.insertDomain(sslDomain)
    }

    fun bulkAdd(domainsStr: String) {
        val accessManager = FeatureAccessManager.getInstance(getApplication())
        if (!accessManager.isPro()) {
            showPaywallDialog = true
            return
        }

        val domainList = domainsStr.split(Regex("[,\\n]"))
            .map { cleanDomainString(it) }
            .filter { it.isNotBlank() }
        if (domainList.isEmpty()) return

        viewModelScope.launch {
            isScanning = true
            scanProgress = Pair(0, domainList.size)

            domainList.forEachIndexed { index, domain ->
                try {
                    val res = SslTlsEngine.checkDomain(domain)
                    saveResultToDb(res)
                } catch (e: Exception) {}
                scanProgress = Pair(index + 1, domainList.size)
            }
            isScanning = false
        }
    }

    fun deleteDomain(domain: SslDomain) {
        viewModelScope.launch {
            sslDao.deleteDomain(domain)
        }
    }

    fun recheck(domain: SslDomain) {
        addDomain(domain.domain)
    }

    fun recheckAll() {
        viewModelScope.launch {
            val list = sslDao.getAllDomainsList()
            if (list.isEmpty()) return@launch
            isScanning = true
            scanProgress = Pair(0, list.size)
            list.forEachIndexed { index, item ->
                try {
                    val res = SslTlsEngine.checkDomain(item.domain)
                    saveResultToDb(res)
                } catch (e: Exception) {}
                scanProgress = Pair(index + 1, list.size)
            }
            isScanning = false
        }
    }

    fun saveToQuickNotes() {
        val res = instantResult ?: return
        viewModelScope.launch {
            val reportText = getShareText(res)
            val note = QuickNote(
                title = "SSL/TLS Lab - ${res.domain}",
                note = reportText,
                tag = "SSL/TLS",
                type = "DOMAIN",
                timestamp = System.currentTimeMillis()
            )
            quickNoteDao.insert(note)
            actionMessage = "Report saved to Quick Notes"
        }
    }

    fun getShareText(result: SslTlsEngine.SslTlsResult? = instantResult): String {
        val res = result ?: return ""
        val vulns = mutableListOf<String>()
        if (res.vulnerabilities.poodle) vulns.add("POODLE")
        if (res.vulnerabilities.beast) vulns.add("BEAST")
        if (res.vulnerabilities.robot) vulns.add("ROBOT")
        val vulnText = if (vulns.isEmpty()) "No Vulns 🟢" else "${vulns.size} Vulns 🔴 (${vulns.joinToString(", ")})"

        return """
            ⚡ SSL/TLS Lab Report: ${res.domain}
            ─────────────────────
            🏆 Grade: ${res.grade.letter} (${res.grade.score}/100)
            ⚡ Latency: ${res.latencyMs}ms
            
            📜 Certificate:
            • Status: ${if (res.certificate.valid) "Valid 🟢" else "Expired/Invalid 🔴"}
            • Expiry: ${res.certificate.expiresInDays} days left
            • Issuer: ${res.certificate.issuer}
            • Key: ${res.certificate.keySize}-bit (${res.certificate.sigAlg})
            ${if (res.certificate.sans.isNotEmpty()) "• SANs: ${res.certificate.sans.take(5).joinToString(", ")}" else ""}
            
            🔒 TLS Versions:
            • TLS 1.3: ${if (res.tlsVersions.tls13) "YES 🟢" else "NO 🔴"}
            • TLS 1.2: ${if (res.tlsVersions.tls12) "YES 🟢" else "NO 🔴"}
            • TLS 1.1: ${if (res.tlsVersions.tls11) "YES 🔴" else "NO 🟢"}
            • TLS 1.0: ${if (res.tlsVersions.tls10) "YES 🔴" else "NO 🟢"}
            • SSL 3.0: ${if (res.tlsVersions.ssl30) "YES 🔴" else "NO 🟢"}
            
            🔑 Cipher Suite:
            • Strongest: ${res.cipherSuites.firstOrNull()?.name ?: "N/A"}
            
            ⛓️ Chain: ${if (res.chain.complete) "Complete 🟢" else "Incomplete 🟡"}
            🛡️ Vulnerabilities: $vulnText
            ─────────────────────
            Sent from NeTez Network Tools
        """.trimIndent()
    }

    fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("SSL/TLS Report", text)
        clipboard.setPrimaryClip(clip)
        actionMessage = "Copied to clipboard"
    }

    fun shareReport(context: Context, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "SSL/TLS Lab Report")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share SSL/TLS Report"))
    }

    private fun cleanDomainString(raw: String): String {
        return raw.trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .split("/")[0]
            .split(":")[0]
            .trim()
    }
}
