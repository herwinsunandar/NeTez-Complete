package com.herwin.netez.engine

import android.app.KeyguardManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import com.herwin.netez.data.AppDatabase
import com.herwin.netez.data.RiskCategory
import com.herwin.netez.network.AppScannerManager
import com.herwin.netez.network.NetworkInfoProvider
import com.herwin.netez.network.UnifiedNetworkInfo
import com.herwin.netez.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

data class SecurityPostureResult(
    val overallScore: Int,
    val overallStatus: String,
    val categories: List<CategoryScore>,
    val issues: List<Issue>,
    val lastScan: Long,
    val netInfo: UnifiedNetworkInfo
)

data class CategoryScore(
    val name: String,
    val score: Int,
    val status: DotStatus,
    val summary: String
)

data class Issue(
    val severity: Severity,
    val title: String,
    val desc: String,
    val action: Action,
    val risk: Int
)

enum class DotStatus { SAFE, WARNING, CRITICAL }
enum class Severity { CRITICAL, WARNING }
enum class Action {
    OPEN_WIFI_AUDIT, OPEN_DEVICE_CHECK, OPEN_APP_SCANNER, OPEN_SSL_REMINDER,
    OPEN_PASSWORD_MANAGER, OPEN_DNS_LEAK
}

object SecurityPostureEngine {

    suspend fun calculate(context: Context): SecurityPostureResult = withContext(Dispatchers.IO) {
        val netInfo = NetworkInfoProvider.getUnifiedNetworkInfo(context)
        val db = AppDatabase.getDatabase(context)
        
        // 1. Device Security - Get from history if available
        val lastDeviceCheck = db.deviceSecurityDao().getAllHistoryList()
        val deviceData = if (lastDeviceCheck.isNotEmpty()) {
            val last = lastDeviceCheck.first()
            last.score to "${last.criticalCount} Critical, ${last.warningCount} Warning"
        } else {
            calculateQuickDeviceScore(context)
        }
        val deviceScore = deviceData.first
        val deviceSummary = deviceData.second

        // 2. Network Security - Get from WiFi history if available
        val networkData = if (netInfo.isWifi) {
            val audit = db.wifiAuditDao().getAuditByBssid(netInfo.wifiCaps ?: "") 
                ?: db.wifiAuditDao().getAuditByBssid(netInfo.ssidOrOperator) 
                ?: db.wifiAuditDao().getLastAudit()
            
            if (audit != null && (System.currentTimeMillis() - audit.timestamp < TimeUnit.HOURS.toMillis(24))) {
                audit.score to "${audit.criticalCount} Critical, ${audit.warningCount} Warning"
            } else {
                calculateQuickNetworkScore(netInfo)
            }
        } else {
            95 to "Cellular Protected"
        }
        val networkScore = networkData.first
        val networkSummary = networkData.second

        // 3. App Security
        val scanner = AppScannerManager(context)
        val appsList = scanner.scanApps()
        val dangerousCount = appsList.count { it.riskCategory == RiskCategory.DANGEROUS }
        val appScore = 100 - (dangerousCount * 30).coerceAtMost(90)

        // 4. Privacy
        val weakPasswords = db.passwordDao().getWeakCountSync()
        val breachedCount = db.leakCheckDao().getBreachedCountSync()
        val vpnOn = isVpnActive(context)
        
        val privacyScores = mutableListOf<Int>()
        privacyScores.add((weakPasswords * 15).coerceAtMost(60))
        privacyScores.add((breachedCount * 20).coerceAtMost(80))
        privacyScores.add(if (!vpnOn) 10 else 0)
        val privacyScore = 100 - (privacyScores.average().toInt().coerceIn(0, 100))

        val overall = (deviceScore * 0.30 + networkScore * 0.25 + appScore * 0.25 + privacyScore * 0.20).toInt().coerceIn(0, 100)
        val status = when {
            overall >= 90 -> "Excellent"
            overall >= 70 -> "Good"
            overall >= 50 -> "Fair"
            else -> "Poor"
        }

        // Issues build
        val issues = mutableListOf<Issue>()
        val adbOn = Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
        if (adbOn) issues.add(Issue(Severity.WARNING, "USB Debugging enabled", "Disable in Developer Options", Action.OPEN_DEVICE_CHECK, 60))
        if (netInfo.isWifi && (netInfo.wifiCaps?.contains("WPS") == true)) {
            issues.add(Issue(Severity.CRITICAL, "WPS ON - 2 sec hack", "Disable WPS in router settings", Action.OPEN_WIFI_AUDIT, 70))
        }
        if (dangerousCount > 0) issues.add(Issue(Severity.CRITICAL, "$dangerousCount apps have high-risk permissions", "Review dangerous apps in scanner", Action.OPEN_APP_SCANNER, 70))
        val sslExpiring = db.sslDomainDao().getExpiringCountSync()
        if (sslExpiring > 0) issues.add(Issue(Severity.WARNING, "$sslExpiring SSL will expire", "View expiring certificates", Action.OPEN_SSL_REMINDER, 40))

        SecurityPostureResult(
            overallScore = overall,
            overallStatus = status,
            categories = listOf(
                CategoryScore("Device Security", deviceScore, getStatus(deviceScore), deviceSummary),
                CategoryScore("Network Security", networkScore, getStatus(networkScore), networkSummary),
                CategoryScore("App Security", appScore, getStatus(appScore), "$dangerousCount dangerous apps"),
                CategoryScore("Privacy", privacyScore, getStatus(privacyScore), "$weakPasswords weak, $breachedCount leaks")
            ),
            issues = issues.sortedByDescending { it.risk }.take(3),
            lastScan = System.currentTimeMillis(),
            netInfo = netInfo
        )
    }

    private fun calculateQuickDeviceScore(context: Context): Pair<Int, String> {
        val devicePenalties = mutableListOf<Int>()
        devicePenalties.add(if (checkRootQuick()) 100 else 0)
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        devicePenalties.add(if (!km.isDeviceSecure) 80 else 0)
        val devOn = Settings.Global.getInt(context.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1
        devicePenalties.add(if (devOn) 30 else 0)
        val score = 100 - (devicePenalties.average().toInt().coerceIn(0, 100))
        return score to "Quick Scan Active"
    }

    private fun calculateQuickNetworkScore(netInfo: UnifiedNetworkInfo): Pair<Int, String> {
        val netPenalties = mutableListOf<Int>()
        val caps = netInfo.wifiCaps ?: ""
        netPenalties.add(when {
            caps.contains("WPA3") -> 0
            caps.contains("WPA2") -> 10
            else -> 50
        })
        netPenalties.add(if (caps.contains("WPS")) 70 else 0)
        val score = 100 - (netPenalties.average().toInt().coerceIn(0, 100))
        return score to "Quick Scan Active"
    }

    private fun getStatus(score: Int) = when {
        score >= 70 -> DotStatus.SAFE
        score >= 50 -> DotStatus.WARNING
        else -> DotStatus.CRITICAL
    }

    private fun checkRootQuick(): Boolean {
        val paths = arrayOf("/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su", "/data/local/xbin/su")
        return paths.any { File(it).exists() }
    }

    private fun isVpnActive(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.getNetworkCapabilities(cm.activeNetwork)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ?: false
    }
}
