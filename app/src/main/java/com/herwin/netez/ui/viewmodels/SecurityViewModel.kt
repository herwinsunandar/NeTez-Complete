package com.herwin.netez.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.data.AppDatabase
import com.herwin.netez.data.entities.SslDomain
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SecurityViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val sslDao = db.sslDomainDao()
    private val leakDao = db.leakCheckDao()
    private val passwordDao = db.passwordDao()
    private val phishingDao = db.phishingDao()
    private val deviceDao = db.deviceSecurityDao()

    val monitoredDomains = sslDao.getAllDomains()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expiringCount = sslDao.getExpiringCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val expiredCount = sslDao.getExpiredCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val breachedCount = leakDao.getBreachedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val weakPasswordCount = passwordDao.getWeakCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalPasswords = passwordDao.getAllPasswords().map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val securityStats = combine(
        combine(monitoredDomains, breachedCount, weakPasswordCount) { ssl, breached, weak -> Triple(ssl, breached, weak) },
        combine(leakDao.getTotalCount(), phishingDao.getPhishingCount(), deviceDao.getAllHistory()) { leaks, phishing, device -> Triple(leaks, phishing, device) }
    ) { part1, part2 ->
        val domains = part1.first
        val breached = part1.second
        val weak = part1.third
        val totalLeaks = part2.first
        val phishing = part2.second
        val deviceHistory = part2.third
        
        val expiring = domains.count { it.daysLeft in 1..14 }
        val expired = domains.count { it.daysLeft <= 0 }
        val lastScan = deviceHistory.firstOrNull()
        
        SecurityStats(
            totalSsl = domains.size,
            expiringSsl = expiring,
            expiredSsl = expired,
            breaches = breached,
            weakPasswords = weak,
            totalLeaks = totalLeaks,
            phishing = phishing,
            deviceScore = lastScan?.score ?: -1,
            deviceGrade = lastScan?.grade ?: "Unknown",
            deviceCritical = lastScan?.criticalCount ?: 0,
            deviceWarning = lastScan?.warningCount ?: 0,
            dnsLeaks = -1 // Placeholder until we have history for this
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SecurityStats())
}

data class SecurityStats(
    val totalSsl: Int = 0,
    val expiringSsl: Int = 0,
    val expiredSsl: Int = 0,
    val breaches: Int = 0,
    val weakPasswords: Int = 0,
    val blacklisted: Int = 0,
    val totalLeaks: Int = 0,
    val phishing: Int = 0,
    val deviceScore: Int = -1,
    val deviceGrade: String = "Unknown",
    val deviceCritical: Int = 0,
    val deviceWarning: Int = 0,
    val dnsLeaks: Int = -1
)
