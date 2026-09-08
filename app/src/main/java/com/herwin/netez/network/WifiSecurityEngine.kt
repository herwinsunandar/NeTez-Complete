package com.herwin.netez.network

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.provider.Settings
import com.herwin.netez.R
import com.herwin.netez.data.AppDatabase
import com.herwin.netez.data.entities.WifiAuditHistory
import com.herwin.netez.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

enum class WifiCheckStatus {
    SAFE, WARNING, CRITICAL, INFO, UNKNOWN
}

enum class WifiCheckAction {
    OPEN_WIFI_SETTINGS, OPEN_ROUTER, OPEN_PRIVATE_DNS_SETTINGS, OPEN_VPN_SETTINGS, OPEN_LAN_SCANNER
}

data class WifiCheckItem(
    val id: String,
    val category: String,
    val nameRes: Int,
    val status: WifiCheckStatus,
    val score: Int?,
    val maxScore: Int,
    val detail: String,
    val recommendation: String?,
    val durationMs: Long?,
    val action: WifiCheckAction? = null
)

data class WifiAuditResult(
    val ssid: String?,
    val bssid: String?,
    val security: String?,
    val frequency: Int?,
    val channel: Int?,
    val rssi: Int?,
    val gateway: String?,
    val dnsServers: List<String>,
    val checks: List<WifiCheckItem>,
    val securityScore: Int,
    val securityGrade: String,
    val criticalCount: Int,
    val warningCount: Int,
    val safeCount: Int,
    val unknownCount: Int,
    val wifiQualityScore: Int?,
    val scannedAt: Long,
    val categoryScores: Map<String, Int>
)

object WifiSecurityEngine {

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    suspend fun auditCurrentWifi(context: Context): WifiAuditResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val wifiInfo = wifiManager.connectionInfo
        val scanResults = try { wifiManager.scanResults } catch (e: Exception) { emptyList() }
        val currentScan = scanResults?.find { it.BSSID == wifiInfo.bssid }
        
        val checks = mutableListOf<WifiCheckItem>()
        
        // Categories
        val CAT_ENCRYPTION = "Encryption & Authentication"
        val CAT_CONFIG = "WiFi Configuration"
        val CAT_TRUST = "Access Point Trust"
        val CAT_NETWORK = "Network Security"
        val CAT_PRIVACY = "Privacy"
        val CAT_EXPOSURE = "Network Exposure"

        // --- CHECK 01 & 02 & 03: Protocol, Cipher, Auth ---
        val encStartTime = System.currentTimeMillis()
        val caps = currentScan?.capabilities ?: ""
        var encStatus = WifiCheckStatus.SAFE
        var encScore = 35
        var encDetail = context.getString(R.string.wifi_audit_enc_unknown_detail)
        var encRec: String? = null
        
        when {
            caps.contains("WPA3") -> {
                encScore = 35
                encDetail = context.getString(R.string.wifi_audit_enc_wpa3_detail)
            }
            caps.contains("WPA2") -> {
                if (caps.contains("CCMP") || caps.contains("AES")) {
                    encScore = 30
                    encDetail = context.getString(R.string.wifi_audit_enc_wpa2_aes_detail)
                    encRec = context.getString(R.string.wifi_audit_enc_wpa2_aes_rec)
                } else if (caps.contains("TKIP")) {
                    encStatus = WifiCheckStatus.WARNING
                    encScore = 15
                    encDetail = context.getString(R.string.wifi_audit_enc_wpa2_tkip_detail)
                    encRec = context.getString(R.string.wifi_audit_enc_wpa2_tkip_rec)
                }
            }
            caps.contains("WEP") -> {
                encStatus = WifiCheckStatus.CRITICAL
                encScore = 3
                encDetail = context.getString(R.string.wifi_audit_enc_wep_detail)
                encRec = context.getString(R.string.wifi_audit_enc_wep_rec)
            }
            caps.contains("ESS") && !caps.contains("WPA") && !caps.contains("WEP") -> {
                encStatus = WifiCheckStatus.CRITICAL
                encScore = 0
                encDetail = context.getString(R.string.wifi_audit_enc_open_detail)
                encRec = context.getString(R.string.wifi_audit_enc_open_rec)
            }
            else -> {
                encStatus = WifiCheckStatus.UNKNOWN
                encScore = 0
                encDetail = context.getString(R.string.wifi_audit_enc_unknown_detail)
            }
        }
        
        checks.add(WifiCheckItem(
            id = "encryption",
            category = CAT_ENCRYPTION,
            nameRes = R.string.wifi_audit_encryption,
            status = encStatus,
            score = if (encStatus == WifiCheckStatus.UNKNOWN) null else encScore,
            maxScore = 35,
            detail = encDetail,
            recommendation = encRec,
            durationMs = System.currentTimeMillis() - encStartTime
        ))

        // --- CHECK 04: WPS ---
        val wpsStartTime = System.currentTimeMillis()
        val wpsEnabled = caps.contains("WPS")
        checks.add(WifiCheckItem(
            id = "wps",
            category = CAT_CONFIG,
            nameRes = R.string.wifi_audit_wps,
            status = if (wpsEnabled) WifiCheckStatus.WARNING else WifiCheckStatus.SAFE,
            score = if (wpsEnabled) 0 else 5,
            maxScore = 5,
            detail = if (wpsEnabled) context.getString(R.string.wifi_audit_wps_on_detail) else context.getString(R.string.wifi_audit_wps_off_detail),
            recommendation = if (wpsEnabled) context.getString(R.string.wifi_audit_wps_on_rec) else null,
            durationMs = System.currentTimeMillis() - wpsStartTime,
            action = if (wpsEnabled) WifiCheckAction.OPEN_ROUTER else null
        ))

        // --- CHECK 05: PMF ---
        val pmfStartTime = System.currentTimeMillis()
        val hasPmf = caps.contains("MFPC") || caps.contains("MFPR")
        checks.add(WifiCheckItem(
            id = "pmf",
            category = CAT_CONFIG,
            nameRes = R.string.wifi_audit_pmf,
            status = if (hasPmf) WifiCheckStatus.SAFE else WifiCheckStatus.UNKNOWN,
            score = if (hasPmf) 5 else 0,
            maxScore = 5,
            detail = if (hasPmf) context.getString(R.string.wifi_audit_pmf_on_detail) else context.getString(R.string.wifi_audit_pmf_off_detail),
            recommendation = if (!hasPmf) context.getString(R.string.wifi_audit_pmf_off_rec) else null,
            durationMs = System.currentTimeMillis() - pmfStartTime
        ))

        // --- CHECK 06: SSID ---
        val ssid = wifiInfo.ssid.removeSurrounding("\"")
        val isDefaultSsid = ssid.matches(Regex("(TP-LINK|TPLINK|MyRepublic|Indihome|FirstMedia|ZTE|Huawei|Linksys|D-Link).*", RegexOption.IGNORE_CASE))
        checks.add(WifiCheckItem(
            id = "ssid",
            category = CAT_CONFIG,
            nameRes = R.string.wifi_audit_ssid_pass,
            status = if (isDefaultSsid) WifiCheckStatus.WARNING else WifiCheckStatus.SAFE,
            score = if (isDefaultSsid) 5 else 10,
            maxScore = 10,
            detail = if (isDefaultSsid) context.getString(R.string.wifi_audit_ssid_default_detail) else context.getString(R.string.wifi_audit_ssid_custom_detail),
            recommendation = if (isDefaultSsid) context.getString(R.string.wifi_audit_ssid_default_rec) else null,
            durationMs = 5
        ))

        // --- CHECK 07: Rogue AP Detection ---
        val rogueStartTime = System.currentTimeMillis()
        val sameSsidDifferentSecurity = scanResults?.any { it.SSID == ssid && it.capabilities != caps } ?: false
        checks.add(WifiCheckItem(
            id = "rogue_ap",
            category = CAT_TRUST,
            nameRes = R.string.wifi_audit_rogue_ap,
            status = if (sameSsidDifferentSecurity) WifiCheckStatus.CRITICAL else WifiCheckStatus.SAFE,
            score = if (sameSsidDifferentSecurity) 0 else 15,
            maxScore = 15,
            detail = if (sameSsidDifferentSecurity) context.getString(R.string.wifi_audit_rogue_ap_warn_detail) else context.getString(R.string.wifi_audit_rogue_ap_safe_detail),
            recommendation = if (sameSsidDifferentSecurity) context.getString(R.string.wifi_audit_rogue_ap_warn_rec) else null,
            durationMs = System.currentTimeMillis() - rogueStartTime
        ))

        // --- CHECK 11: Gateway Ports ---
        val dhcp = wifiManager.dhcpInfo
        val gatewayIp = NetworkUtils.formatIp(dhcp.gateway)
        val portsToScan = listOf(22, 23, 80, 443)
        val openPorts = quickScanPorts(gatewayIp, portsToScan)
        val gtwStatus = when {
            openPorts.contains(23) -> WifiCheckStatus.CRITICAL
            openPorts.contains(80) && !openPorts.contains(443) -> WifiCheckStatus.WARNING
            else -> WifiCheckStatus.SAFE
        }
        checks.add(WifiCheckItem(
            id = "gateway_ports",
            category = CAT_NETWORK,
            nameRes = R.string.wifi_audit_gateway,
            status = gtwStatus,
            score = when(gtwStatus) {
                WifiCheckStatus.CRITICAL -> 0
                WifiCheckStatus.WARNING -> 7
                else -> 15
            },
            maxScore = 15,
            detail = context.getString(R.string.wifi_audit_gtw_detail, gatewayIp, openPorts.joinToString()),
            recommendation = if (openPorts.contains(23)) context.getString(R.string.wifi_audit_gtw_telnet_rec) else if (openPorts.contains(80)) context.getString(R.string.wifi_audit_gtw_http_rec) else null,
            durationMs = 300,
            action = WifiCheckAction.OPEN_ROUTER
        ))

        // --- CHECK 13: DNS ---
        val linkProps = connectivityManager.getLinkProperties(connectivityManager.activeNetwork)
        val dnsServers = linkProps?.dnsServers?.map { it.hostAddress ?: "" } ?: emptyList()
        val dnsStatus = if (dnsServers.any { it == "1.1.1.1" || it == "9.9.9.9" }) WifiCheckStatus.SAFE else WifiCheckStatus.INFO
        checks.add(WifiCheckItem(
            id = "dns",
            category = CAT_PRIVACY,
            nameRes = R.string.wifi_audit_dns,
            status = dnsStatus,
            score = if (dnsStatus == WifiCheckStatus.SAFE) 10 else 7,
            maxScore = 10,
            detail = context.getString(R.string.wifi_audit_dns_detail, dnsServers.joinToString()),
            recommendation = context.getString(R.string.wifi_audit_dns_rec),
            durationMs = 10,
            action = WifiCheckAction.OPEN_PRIVATE_DNS_SETTINGS
        ))

        // --- CHECK 14: Private DNS ---
        val privateDnsMode = try {
            Settings.Global.getString(context.contentResolver, "private_dns_mode")
        } catch (e: Exception) { "unknown" }
        
        checks.add(WifiCheckItem(
            id = "private_dns",
            category = CAT_PRIVACY,
            nameRes = R.string.wifi_audit_private_dns,
            status = if (privateDnsMode != "off" && privateDnsMode != "unknown") WifiCheckStatus.SAFE else WifiCheckStatus.WARNING,
            score = if (privateDnsMode != "off" && privateDnsMode != "unknown") 5 else 2,
            maxScore = 5,
            detail = context.getString(R.string.wifi_audit_private_dns_detail, privateDnsMode),
            recommendation = if (privateDnsMode == "off" || privateDnsMode == "unknown") context.getString(R.string.wifi_audit_private_dns_off_rec) else null,
            durationMs = 2,
            action = WifiCheckAction.OPEN_PRIVATE_DNS_SETTINGS
        ))

        // --- CHECK 15: MAC Randomization ---
        checks.add(WifiCheckItem(
            id = "mac_privacy",
            category = CAT_PRIVACY,
            nameRes = R.string.wifi_audit_mac_privacy,
            status = WifiCheckStatus.INFO,
            score = 5,
            maxScore = 5,
            detail = context.getString(R.string.wifi_audit_mac_privacy_detail),
            recommendation = context.getString(R.string.wifi_audit_mac_privacy_rec),
            durationMs = 1
        ))

        // --- CHECK 17: LAN Exposure ---
        val db = AppDatabase.getDatabase(context)
        val lanDevices = try { db.scannedDeviceDao().getAllDevices().size } catch (e: Exception) { 0 }
        checks.add(WifiCheckItem(
            id = "lan_exposure",
            category = CAT_EXPOSURE,
            nameRes = R.string.wifi_audit_devices,
            status = WifiCheckStatus.INFO,
            score = 5,
            maxScore = 5,
            detail = context.getString(R.string.wifi_audit_lan_exposure_detail, lanDevices),
            recommendation = context.getString(R.string.wifi_audit_lan_exposure_rec),
            durationMs = 5,
            action = WifiCheckAction.OPEN_LAN_SCANNER
        ))

        // --- Weighted Scoring Logic ---
        val categoryWeights = mapOf(
            CAT_ENCRYPTION to 35,
            CAT_CONFIG to 20,
            CAT_TRUST to 15,
            CAT_NETWORK to 15,
            CAT_PRIVACY to 10,
            CAT_EXPOSURE to 5
        )
        
        val categoryScores = mutableMapOf<String, Int>()
        categoryWeights.keys.forEach { cat ->
            val catChecks = checks.filter { it.category == cat }
            if (catChecks.isEmpty()) {
                categoryScores[cat] = categoryWeights[cat] ?: 0
            } else {
                val totalMax = catChecks.sumOf { it.maxScore }
                val totalObtained = catChecks.sumOf { it.score ?: it.maxScore }
                categoryScores[cat] = ((totalObtained.toFloat() / totalMax) * (categoryWeights[cat] ?: 0)).toInt()
            }
        }

        val securityScore = categoryScores.values.sum().coerceIn(0, 100)
        val securityGrade = when {
            securityScore >= 90 -> "EXCELLENT"
            securityScore >= 75 -> "GOOD"
            securityScore >= 60 -> "FAIR"
            securityScore >= 40 -> "WEAK"
            else -> "CRITICAL"
        }

        val result = WifiAuditResult(
            ssid = ssid,
            bssid = wifiInfo.bssid,
            security = caps,
            frequency = wifiInfo.frequency,
            channel = NetworkUtils.getChannel(wifiInfo.frequency),
            rssi = wifiInfo.rssi,
            gateway = gatewayIp,
            dnsServers = dnsServers,
            checks = checks,
            securityScore = securityScore,
            securityGrade = securityGrade,
            criticalCount = checks.count { it.status == WifiCheckStatus.CRITICAL },
            warningCount = checks.count { it.status == WifiCheckStatus.WARNING },
            safeCount = checks.count { it.status == WifiCheckStatus.SAFE },
            unknownCount = checks.count { it.status == WifiCheckStatus.UNKNOWN },
            wifiQualityScore = 80, // Simplified
            scannedAt = System.currentTimeMillis(),
            categoryScores = categoryScores
        )

        // Save result to database
        if (result.bssid != null && result.bssid != "00:00:00:00:00:00") {
            try {
                db.wifiAuditDao().insertAudit(
                    WifiAuditHistory(
                        bssid = result.bssid,
                        ssid = result.ssid,
                        timestamp = result.scannedAt,
                        score = result.securityScore,
                        grade = result.securityGrade,
                        criticalCount = result.criticalCount,
                        warningCount = result.warningCount,
                        safeCount = result.safeCount
                    )
                )
            } catch (e: Exception) {}
        }

        result
    }

    private suspend fun quickScanPorts(ip: String, ports: List<Int>): List<Int> = withContext(Dispatchers.IO) {
        ports.map { port ->
            async {
                try {
                    Socket().use { it.connect(InetSocketAddress(ip, port), 200) }
                    port
                } catch (e: Exception) { null }
            }
        }.awaitAll().filterNotNull()
    }
}
