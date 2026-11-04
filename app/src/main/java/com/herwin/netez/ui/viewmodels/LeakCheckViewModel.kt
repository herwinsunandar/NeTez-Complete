package com.herwin.netez.ui.viewmodels

import android.accounts.AccountManager
import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Patterns
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.data.AppDatabase
import com.herwin.netez.data.entities.LeakCheckEntry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

import com.herwin.netez.entitlement.FeatureAccessManager
import com.herwin.netez.entitlement.FeatureAccessState
import com.herwin.netez.entitlement.FeatureId

class LeakCheckViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val leakDao = db.leakCheckDao()
    private val client = OkHttpClient()

    var inputEmails by mutableStateOf("")
    var isScanning by mutableStateOf(false)
    var scanProgress by mutableStateOf(Pair(0, 0))
    var breachedSoFar by mutableStateOf(0)
    var batchResults = mutableStateListOf<LeakCheckResult>()

    var csvPreviewList = mutableStateListOf<CsvEmail>()
    var csvInvalidCount by mutableIntStateOf(0)
    var showCsvPreview by mutableStateOf(false)
    var showPaywallDialog by mutableStateOf(false)
    var showGlobalSupportDialog by mutableStateOf(false)
    var paywallText by mutableStateOf("")

    val history = leakDao.getAllLeakChecks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onEmailChange(newEmails: String) {
        inputEmails = newEmails
    }

    fun getDetectedEmailCount(): Int {
        return inputEmails.split(Regex("[,\\s\\n]")).filter { it.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(it.trim()).matches() }.size
    }

    fun checkMyEmail() {
        val accountManager = AccountManager.get(getApplication())
        val accounts = accountManager.getAccountsByType("com.google")
        if (accounts.isNotEmpty()) {
            val myEmail = accounts[0].name
            if (inputEmails.isNotBlank()) {
                inputEmails += "\n$myEmail"
            } else {
                inputEmails = myEmail
            }
        }
    }

    fun parseCsvFile(uri: Uri) {
        val accessManager = FeatureAccessManager.getInstance(getApplication())
        if (!accessManager.isPro()) {
            paywallText = "Bulk CSV Email Leak Check is available in NeTez PRO."
            showPaywallDialog = true
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = getApplication<Application>().contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val emails = mutableListOf<CsvEmail>()
                    var invalidCount = 0
                    var isFirstLine = true
                    
                    reader.forEachLine { line ->
                        if (isFirstLine && line.contains("email", ignoreCase = true)) {
                            isFirstLine = false
                            return@forEachLine
                        }
                        val parts = line.split(",", ";").map { it.trim().removeSurrounding("\"") }
                        val email = parts.firstOrNull { it.contains("@") }
                        if (email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                            emails.add(CsvEmail(email, parts.getOrNull(1), parts.getOrNull(2)))
                        } else if (line.isNotBlank()) {
                            invalidCount++
                        }
                    }
                    
                    withContext(Dispatchers.Main) {
                        csvPreviewList.clear()
                        csvPreviewList.addAll(emails.distinctBy { it.email.lowercase() })
                        csvInvalidCount = invalidCount
                        showCsvPreview = true
                    }
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun startBatchCheck(emails: List<CsvEmail>) {
        val accessManager = FeatureAccessManager.getInstance(getApplication())
        val accessState = accessManager.checkAccess(FeatureId.LEAK_CHECK_DAILY)
        if (accessState == FeatureAccessState.LIMIT_REACHED) {
            paywallText = "Free plan supports up to 3 email leak checks per day. Upgrade to NeTez PRO or watch an ad to get +1 bonus check."
            showPaywallDialog = true
            return
        }
        accessManager.consumeQuota(FeatureId.LEAK_CHECK_DAILY)

        viewModelScope.launch {
            isScanning = true
            batchResults.clear()
            breachedSoFar = 0
            scanProgress = Pair(0, emails.size)
            
            val results = checkMultiple(emails)
            batchResults.addAll(results)
            isScanning = false
            if (accessManager.trackAndCheckGlobalToolUsage()) {
                showGlobalSupportDialog = true
            }
        }
    }

    fun startManualCheck() {
        val emails = inputEmails.split(Regex("[,\\s\\n]"))
            .filter { it.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(it.trim()).matches() }
            .map { CsvEmail(it.trim(), null, null) }
        
        if (emails.isNotEmpty()) {
            startBatchCheck(emails)
        }
    }

    fun getExportCsvContent(): String {
        val sb = StringBuilder()
        sb.append("email,status,breach_count,breaches,last_checked\n")
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        batchResults.forEach { res ->
            val breachesStr = res.breaches.joinToString("|") { it.name }
            val status = if (res.isBreached) "BREACHED" else "CLEAN"
            sb.append("${res.email},$status,${res.breaches.size},\"$breachesStr\",${sdf.format(Date())}\n")
        }
        return sb.toString()
    }

    private suspend fun checkMultiple(emails: List<CsvEmail>): List<LeakCheckResult> = coroutineScope {
        val results = mutableListOf<LeakCheckResult>()
        val semaphore = Semaphore(3)
        val batchId = UUID.randomUUID().toString()

        emails.chunked(10).forEach { chunk ->
            val deferred = chunk.map { csvEmail ->
                async {
                    semaphore.withPermit {
                        // Rate limit: ~2.5 req/sec
                        delay(400)
                        val res = performLeakCheck(csvEmail.email)
                        
                        // Save to DB
                        val entry = LeakCheckEntry(
                            email = csvEmail.email,
                            breachesJson = JSONObject().apply { 
                                val arr = JSONArray()
                                res.breaches.forEach { b ->
                                    val bObj = JSONObject().apply {
                                        put("name", b.name)
                                        put("year", b.year)
                                        put("data", b.data)
                                    }
                                    arr.put(bObj)
                                }
                                put("breaches", arr)
                            }.toString(),
                            lastChecked = System.currentTimeMillis(),
                            isBreached = res.isBreached,
                            breachCount = res.breaches.size,
                            name = csvEmail.name,
                            dept = csvEmail.dept,
                            batchId = batchId
                        )
                        leakDao.insertLeakCheck(entry)
                        
                        withContext(Dispatchers.Main) {
                            if (res.isBreached) breachedSoFar++
                            scanProgress = Pair(results.size + 1, emails.size)
                        }
                        res
                    }
                }
            }
            results.addAll(deferred.awaitAll())
        }
        results
    }

    private suspend fun performLeakCheck(email: String): LeakCheckResult = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.xposedornot.com/v1/check-email/$email")
            .build()
            
        try {
            val response = client.newCall(request).execute()
            if (response.code == 200) {
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)
                val breachesArray = json.optJSONArray("breaches")
                val breaches = mutableListOf<BreachInfo>()
                if (breachesArray != null) {
                    for (i in 0 until breachesArray.length()) {
                        val breach = breachesArray.getJSONArray(i)
                        breaches.add(BreachInfo(
                            name = breach.optString(0),
                            year = breach.optString(1),
                            data = "Email, Password"
                        ))
                    }
                }
                LeakCheckResult(email, true, breaches)
            } else if (response.code == 404) {
                LeakCheckResult(email, false, emptyList())
            } else {
                LeakCheckResult(email, false, emptyList(), "API Error: ${response.code}")
            }
        } catch (e: Exception) {
            LeakCheckResult(email, false, emptyList(), e.message)
        }
    }
}

data class CsvEmail(val email: String, val name: String?, val dept: String?)

data class LeakCheckResult(
    val email: String,
    val isBreached: Boolean,
    val breaches: List<BreachInfo>,
    val error: String? = null,
    val riskScore: Int = if (isBreached) (breaches.size * 25).coerceAtMost(100) else 0,
    val name: String? = null,
    val dept: String? = null
)

data class BreachInfo(
    val name: String,
    val year: String,
    val data: String,
    val description: String = ""
)
