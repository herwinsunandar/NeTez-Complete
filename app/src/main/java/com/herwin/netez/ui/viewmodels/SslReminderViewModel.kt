package com.herwin.netez.ui.viewmodels

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.data.AppDatabase
import com.herwin.netez.data.entities.SslDomain
import com.herwin.netez.network.SslManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class SslReminderViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val sslDao = db.sslDomainDao()
    private val sslManager = SslManager()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm:ss z", Locale.ENGLISH)

    var inputQuery by mutableStateOf("")
    var isScanning by mutableStateOf(false)
    var scanProgress by mutableStateOf(Pair(0, 0))
    var filterState by mutableStateOf("All")

    val domains = sslDao.getAllDomains()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onQueryChange(newQuery: String) {
        inputQuery = newQuery
    }

    fun addDomain(domain: String) {
        if (domain.isBlank()) return
        viewModelScope.launch {
            isScanning = true
            try {
                val info = withContext(Dispatchers.IO) { sslManager.checkSsl(domain.trim()) }
                val expiryDateLong = try {
                    dateFormat.parse(info.expiryDate)?.time ?: 0L
                } catch (e: Exception) { 0L }

                val sslDomain = SslDomain(
                    domain = domain.trim(),
                    expiryDate = expiryDateLong,
                    daysLeft = info.expiryDays.toInt(),
                    issuer = info.issuer,
                    lastChecked = System.currentTimeMillis()
                )
                sslDao.insertDomain(sslDomain)
                inputQuery = ""
            } catch (e: Exception) {
            } finally {
                isScanning = false
            }
        }
    }
    
    fun bulkAdd(domainsStr: String) {
        val domainList = domainsStr.split(Regex("[,\\n]")).map { it.trim() }.filter { it.isNotBlank() }
        if (domainList.isEmpty()) return
        
        viewModelScope.launch {
            isScanning = true
            scanProgress = Pair(0, domainList.size)
            
            domainList.forEachIndexed { index, domain ->
                try {
                    val info = withContext(Dispatchers.IO) { sslManager.checkSsl(domain) }
                    val expiryDateLong = try {
                        dateFormat.parse(info.expiryDate)?.time ?: 0L
                    } catch (e: Exception) { 0L }

                    val sslDomain = SslDomain(
                        domain = domain,
                        expiryDate = expiryDateLong,
                        daysLeft = info.expiryDays.toInt(),
                        issuer = info.issuer,
                        lastChecked = System.currentTimeMillis()
                    )
                    sslDao.insertDomain(sslDomain)
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
}

// Helper to handle Date parsing for SslDomain if SslManager returns string
// Actually SslManager returns String for dates. Let's adjust SslDomain or parsing.
