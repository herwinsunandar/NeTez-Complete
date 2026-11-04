package com.herwin.netez.engine.intelligence

import android.content.Context
import com.herwin.netez.data.AppDatabase
import com.herwin.netez.engine.SslTlsEngine
import com.herwin.netez.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.util.Scanner

object SecurityIntelligenceEngine {

    fun assessTargetStream(
        context: Context,
        target: ParsedTarget
    ): Flow<SecurityIntelligenceResult> = flow {
        val startTime = System.currentTimeMillis()
        val categories = mutableListOf<CategoryCheckStatus>()

        // 1. Initial State
        when (target.type) {
            TargetType.DOMAIN, TargetType.URL -> {
                categories.add(CategoryCheckStatus(CheckCategoryType.DOMAIN_REPUTATION, "Domain Reputation", CheckExecutionStatus.WAITING, "Waiting to analyze...", sourceProvider = "NeTez DNS Intelligence"))
                categories.add(CategoryCheckStatus(CheckCategoryType.BLACKLIST, "Blacklist Check", CheckExecutionStatus.WAITING, "Waiting to analyze...", sourceProvider = "Global RBLs & AbuseIPDB"))
                categories.add(CategoryCheckStatus(CheckCategoryType.SSL_TLS, "SSL/TLS Security", CheckExecutionStatus.WAITING, "Waiting to analyze...", sourceProvider = "X.509 TLS Inspector"))
                categories.add(CategoryCheckStatus(CheckCategoryType.DNS_SECURITY, "DNS Security", CheckExecutionStatus.WAITING, "Waiting to analyze...", sourceProvider = "Cloudflare 1.1.1.1 Public DNS"))
                categories.add(CategoryCheckStatus(CheckCategoryType.PHISHING, "Phishing Indicators", CheckExecutionStatus.WAITING, "Waiting to analyze...", sourceProvider = "URLhaus (abuse.ch) & OpenPhish"))
                categories.add(CategoryCheckStatus(CheckCategoryType.EXPOSURE, "Exposure & IP Info", CheckExecutionStatus.WAITING, "Waiting to analyze...", sourceProvider = "InetAddress Resolver"))
            }
            TargetType.EMAIL -> {
                categories.add(CategoryCheckStatus(CheckCategoryType.BREACH_LEAK, "Breach Exposure", CheckExecutionStatus.WAITING, "Waiting to analyze...", sourceProvider = "XposedOrNot Leak DB"))
                categories.add(CategoryCheckStatus(CheckCategoryType.DOMAIN_REPUTATION, "Domain Security", CheckExecutionStatus.WAITING, "Waiting to analyze...", sourceProvider = "NeTez DNS Intelligence"))
                categories.add(CategoryCheckStatus(CheckCategoryType.MAIL_SECURITY, "Mail Security (SPF/DMARC)", CheckExecutionStatus.WAITING, "Waiting to analyze...", sourceProvider = "Cloudflare 1.1.1.1 Public DNS"))
            }
            TargetType.INVALID -> return@flow
        }

        emit(
            SecurityIntelligenceResult(
                target = target,
                timestamp = startTime,
                overallStatus = "ANALYZING",
                categoryStatuses = categories.toList(),
                scanDurationMs = 0L
            )
        )

        val technicalDetails = mutableMapOf<String, Any>()
        var resolvedIp: String? = null

        if (target.type == TargetType.DOMAIN || target.type == TargetType.URL) {
            val domain = target.host

            // Update Domain Reputation -> Running
            updateCategory(categories, CheckCategoryType.DOMAIN_REPUTATION, CheckExecutionStatus.RUNNING, "Analyzing reputation...")
            emitCurrentResult(target, startTime, categories, technicalDetails, this@flow)

            // Resolve IP
            resolvedIp = withContext(Dispatchers.IO) {
                try {
                    val addr = InetAddress.getByName(domain)
                    addr.hostAddress
                } catch (e: Exception) { null }
            }
            if (resolvedIp != null) technicalDetails["Resolved IP"] = resolvedIp

            updateCategory(categories, CheckCategoryType.DOMAIN_REPUTATION, CheckExecutionStatus.SUCCESS, "Domain Active · Host: $domain")
            updateCategory(categories, CheckCategoryType.BLACKLIST, CheckExecutionStatus.RUNNING, "Checking global RBLs...")
            updateCategory(categories, CheckCategoryType.SSL_TLS, CheckExecutionStatus.RUNNING, "Inspecting SSL/TLS certificate...")
            updateCategory(categories, CheckCategoryType.DNS_SECURITY, CheckExecutionStatus.RUNNING, "Querying DNS A/MX/TXT records...")
            updateCategory(categories, CheckCategoryType.PHISHING, CheckExecutionStatus.RUNNING, "Scanning URLhaus & OpenPhish...")
            updateCategory(categories, CheckCategoryType.EXPOSURE, CheckExecutionStatus.RUNNING, "Gathering passive exposure...")
            emitCurrentResult(target, startTime, categories, technicalDetails, this@flow)

            // Asynchronous Execution
            val sslResultMap = mutableMapOf<String, String>()
            var blacklistDetections = 0
            var phishingStatus = "SAFE"
            val dnsRecords = mutableMapOf<String, String>()

            coroutineScope {
                // SSL/TLS Check
                val sslJob = async(Dispatchers.IO) {
                    try {
                        val sslRes = SslTlsEngine.checkDomain(domain)
                        val isValid = sslRes.certificate.valid && !sslRes.certificate.issuer.startsWith("Error:", ignoreCase = true)
                        sslResultMap["valid"] = isValid.toString()
                        sslResultMap["daysLeft"] = sslRes.certificate.expiresInDays.toString()
                        sslResultMap["grade"] = sslRes.grade.letter
                        sslResultMap["issuer"] = sslRes.certificate.issuer
                        sslResultMap["tls"] = if (sslRes.tlsVersions.tls13) "TLS 1.3" else "TLS 1.2"

                        technicalDetails["SSL Issuer"] = sslRes.certificate.issuer
                        technicalDetails["SSL Expires Days"] = sslRes.certificate.expiresInDays
                        technicalDetails["TLS Version"] = sslResultMap["tls"] ?: "TLS 1.2"

                        val status = if (!isValid) CheckExecutionStatus.CRITICAL else if (sslRes.certificate.expiresInDays <= 30 || sslRes.grade.letter == "F") CheckExecutionStatus.WARNING else CheckExecutionStatus.SUCCESS
                        val summary = if (isValid) "Valid · Expires in ${sslRes.certificate.expiresInDays} days · ${sslResultMap["tls"]}" else "Invalid / Expired Certificate"
                        updateCategory(categories, CheckCategoryType.SSL_TLS, status, summary, sslResultMap)
                    } catch (e: Exception) {
                        updateCategory(categories, CheckCategoryType.SSL_TLS, CheckExecutionStatus.UNAVAILABLE, "SSL Check Unavailable")
                    }
                }

                // Blacklist Check
                val blacklistJob = async(Dispatchers.IO) {
                    try {
                        val domainRbl = BlacklistManager.checkDomain(domain)
                        val ipRbl = if (resolvedIp != null) BlacklistManager.checkIp(resolvedIp) else emptyList()
                        val allRbl = domainRbl + ipRbl
                        blacklistDetections = allRbl.count { it.status == BlacklistStatus.LISTED }

                        val status = if (blacklistDetections > 0) CheckExecutionStatus.WARNING else CheckExecutionStatus.SUCCESS
                        val summary = if (blacklistDetections > 0) "$blacklistDetections detection(s) found" else "Clean · No RBL detections"
                        updateCategory(categories, CheckCategoryType.BLACKLIST, status, summary)
                    } catch (e: Exception) {
                        updateCategory(categories, CheckCategoryType.BLACKLIST, CheckExecutionStatus.UNAVAILABLE, "Blacklist Check Unavailable")
                    }
                }

                // DNS Security Check (A, MX, TXT for SPF/DMARC)
                val dnsJob = async(Dispatchers.IO) {
                    try {
                        val txtRes = DnsManager.lookup(domain, DnsRecordType.TXT, PublicDnsServers[0])
                        val mxRes = DnsManager.lookup(domain, DnsRecordType.MX, PublicDnsServers[0])
                        val aRes = DnsManager.lookup(domain, DnsRecordType.A, PublicDnsServers[0])

                        val spf = txtRes.records.find { it.data.contains("v=spf1", ignoreCase = true) }?.data ?: ""
                        val dmarcRes = DnsManager.lookup("_dmarc.$domain", DnsRecordType.TXT, PublicDnsServers[0])
                        val dmarc = dmarcRes.records.find { it.data.contains("v=DMARC1", ignoreCase = true) }?.data ?: ""

                        if (spf.isNotBlank()) dnsRecords["SPF"] = spf
                        if (dmarc.isNotBlank()) dnsRecords["DMARC"] = dmarc
                        if (mxRes.records.isNotEmpty()) dnsRecords["MX"] = mxRes.records.joinToString(", ") { it.data }
                        if (aRes.records.isNotEmpty()) dnsRecords["A"] = aRes.records.joinToString(", ") { it.data }

                        technicalDetails["DNS Records"] = dnsRecords

                        val hasSpf = spf.isNotBlank()
                        val hasDmarc = dmarc.isNotBlank()
                        val status = if (!hasSpf || !hasDmarc || dmarc.contains("p=none", ignoreCase = true)) CheckExecutionStatus.WARNING else CheckExecutionStatus.SUCCESS
                        val summary = "SPF: ${if (hasSpf) "Configured" else "Missing"} · DMARC: ${if (hasDmarc) "Configured" else "Missing"}"
                        updateCategory(categories, CheckCategoryType.DNS_SECURITY, status, summary)
                    } catch (e: Exception) {
                        updateCategory(categories, CheckCategoryType.DNS_SECURITY, CheckExecutionStatus.UNAVAILABLE, "DNS Check Unavailable")
                    }
                }

                // Phishing Check
                val phishingJob = async(Dispatchers.IO) {
                    try {
                        val db = AppDatabase.getDatabase(context)
                        val isPhishing = db.phishingDao().isDomainInFeed(domain)
                        phishingStatus = if (isPhishing) "PHISHING" else "SAFE"

                        val status = if (phishingStatus == "PHISHING") CheckExecutionStatus.CRITICAL else CheckExecutionStatus.SUCCESS
                        val summary = if (phishingStatus == "PHISHING") "Threat Detected (URLhaus/Phishing)" else "No known phishing indicators detected"
                        updateCategory(categories, CheckCategoryType.PHISHING, status, summary)
                    } catch (e: Exception) {
                        updateCategory(categories, CheckCategoryType.PHISHING, CheckExecutionStatus.SUCCESS, "No known phishing indicators detected")
                    }
                }

                // Exposure Check
                val exposureJob = async(Dispatchers.IO) {
                    try {
                        val summary = if (resolvedIp != null) "Resolved IP: $resolvedIp (Passive Inspection)" else "No public IP resolved"
                        updateCategory(categories, CheckCategoryType.EXPOSURE, CheckExecutionStatus.SUCCESS, summary)
                    } catch (e: Exception) {
                        updateCategory(categories, CheckCategoryType.EXPOSURE, CheckExecutionStatus.UNAVAILABLE, "Exposure Check Unavailable")
                    }
                }

                sslJob.await()
                blacklistJob.await()
                dnsJob.await()
                phishingJob.await()
                exposureJob.await()
            }

            // Build Findings & Passed Checks
            val (findings, passed) = FindingsEngine.buildDomainFindings(
                sslResult = sslResultMap,
                blacklistDetections = blacklistDetections,
                phishingStatus = phishingStatus,
                dnsRecords = dnsRecords,
                ip = resolvedIp
            )

            val criticals = findings.count { it.severity == FindingSeverity.CRITICAL }
            val warnings = findings.count { it.severity == FindingSeverity.HIGH || it.severity == FindingSeverity.MEDIUM }
            val overallStatus = when {
                criticals > 0 -> "CRITICAL RISK"
                warnings > 1 -> "HIGH RISK"
                warnings == 1 -> "MODERATE RISK"
                else -> "GOOD"
            }

            emit(
                SecurityIntelligenceResult(
                    target = target,
                    timestamp = System.currentTimeMillis(),
                    overallStatus = overallStatus,
                    checksCompleted = categories.count { it.status == CheckExecutionStatus.SUCCESS || it.status == CheckExecutionStatus.WARNING || it.status == CheckExecutionStatus.CRITICAL },
                    warningCount = warnings,
                    criticalCount = criticals,
                    categoryStatuses = categories.toList(),
                    findings = findings,
                    passedChecks = passed,
                    technicalDetails = technicalDetails,
                    scanDurationMs = System.currentTimeMillis() - startTime
                )
            )

        } else if (target.type == TargetType.EMAIL) {
            val email = target.rawInput
            val domain = target.emailDomain

            updateCategory(categories, CheckCategoryType.BREACH_LEAK, CheckExecutionStatus.RUNNING, "Checking breach databases...")
            updateCategory(categories, CheckCategoryType.DOMAIN_REPUTATION, CheckExecutionStatus.RUNNING, "Analyzing mail domain...")
            updateCategory(categories, CheckCategoryType.MAIL_SECURITY, CheckExecutionStatus.RUNNING, "Querying SPF & DMARC...")
            emitCurrentResult(target, startTime, categories, technicalDetails, this@flow)

            var breachCount = 0
            val breachSources = mutableListOf<String>()
            val mailSecRecords = mutableMapOf<String, String>()

            coroutineScope {
                val breachJob = async(Dispatchers.IO) {
                    try {
                        val breachRes = performLeakCheck(email)
                        breachCount = breachRes.first
                        breachSources.addAll(breachRes.second)

                        technicalDetails["Breaches Count"] = breachCount
                        if (breachSources.isNotEmpty()) technicalDetails["Breached Platforms"] = breachSources.joinToString(", ")

                        val status = if (breachCount > 0) CheckExecutionStatus.WARNING else CheckExecutionStatus.SUCCESS
                        val summary = if (breachCount > 0) "$breachCount data breach(es) detected" else "Clean · No breach exposure detected"
                        updateCategory(categories, CheckCategoryType.BREACH_LEAK, status, summary)
                    } catch (e: Exception) {
                        updateCategory(categories, CheckCategoryType.BREACH_LEAK, CheckExecutionStatus.UNAVAILABLE, "Leak Check Unavailable")
                    }
                }

                val mailJob = async(Dispatchers.IO) {
                    try {
                        val txtRes = DnsManager.lookup(domain, DnsRecordType.TXT, PublicDnsServers[0])
                        val spf = txtRes.records.find { it.data.contains("v=spf1", ignoreCase = true) }?.data ?: ""
                        val dmarcRes = DnsManager.lookup("_dmarc.$domain", DnsRecordType.TXT, PublicDnsServers[0])
                        val dmarc = dmarcRes.records.find { it.data.contains("v=DMARC1", ignoreCase = true) }?.data ?: ""

                        if (spf.isNotBlank()) mailSecRecords["SPF"] = spf
                        if (dmarc.isNotBlank()) mailSecRecords["DMARC"] = dmarc

                        technicalDetails["Mail Domain"] = domain
                        technicalDetails["SPF Record"] = spf.ifBlank { "None" }
                        technicalDetails["DMARC Record"] = dmarc.ifBlank { "None" }

                        updateCategory(categories, CheckCategoryType.DOMAIN_REPUTATION, CheckExecutionStatus.SUCCESS, "Domain $domain Active")
                        val status = if (spf.isBlank() || dmarc.isBlank()) CheckExecutionStatus.WARNING else CheckExecutionStatus.SUCCESS
                        val summary = "SPF: ${if (spf.isNotBlank()) "Configured" else "Missing"} · DMARC: ${if (dmarc.isNotBlank()) "Configured" else "Missing"}"
                        updateCategory(categories, CheckCategoryType.MAIL_SECURITY, status, summary)
                    } catch (e: Exception) {
                        updateCategory(categories, CheckCategoryType.DOMAIN_REPUTATION, CheckExecutionStatus.SUCCESS, "Domain $domain")
                        updateCategory(categories, CheckCategoryType.MAIL_SECURITY, CheckExecutionStatus.UNAVAILABLE, "Mail Security Check Unavailable")
                    }
                }

                breachJob.await()
                mailJob.await()
            }

            val (findings, passed) = FindingsEngine.buildEmailFindings(
                email = email,
                breachesCount = breachCount,
                breachSources = breachSources,
                mailSecurityRecords = mailSecRecords
            )

            val criticals = findings.count { it.severity == FindingSeverity.CRITICAL }
            val warnings = findings.count { it.severity == FindingSeverity.HIGH || it.severity == FindingSeverity.MEDIUM }
            val overallStatus = when {
                criticals > 0 -> "CRITICAL RISK"
                warnings > 1 -> "HIGH RISK"
                warnings == 1 -> "MODERATE RISK"
                else -> "GOOD"
            }

            emit(
                SecurityIntelligenceResult(
                    target = target,
                    timestamp = System.currentTimeMillis(),
                    overallStatus = overallStatus,
                    checksCompleted = categories.count { it.status == CheckExecutionStatus.SUCCESS || it.status == CheckExecutionStatus.WARNING || it.status == CheckExecutionStatus.CRITICAL },
                    warningCount = warnings,
                    criticalCount = criticals,
                    categoryStatuses = categories.toList(),
                    findings = findings,
                    passedChecks = passed,
                    technicalDetails = technicalDetails,
                    scanDurationMs = System.currentTimeMillis() - startTime
                )
            )
        }
    }.flowOn(Dispatchers.Default)

    private fun updateCategory(
        list: MutableList<CategoryCheckStatus>,
        type: CheckCategoryType,
        status: CheckExecutionStatus,
        summary: String,
        details: Map<String, String> = emptyMap()
    ) {
        val index = list.indexOfFirst { it.type == type }
        if (index != -1) {
            val item = list[index]
            list[index] = item.copy(status = status, summary = summary, details = details)
        }
    }

    private suspend fun emitCurrentResult(
        target: ParsedTarget,
        startTime: Long,
        categories: List<CategoryCheckStatus>,
        techDetails: Map<String, Any>,
        collector: FlowCollector<SecurityIntelligenceResult>
    ) {
        collector.emit(
            SecurityIntelligenceResult(
                target = target,
                timestamp = startTime,
                overallStatus = "ANALYZING",
                checksCompleted = categories.count { it.status == CheckExecutionStatus.SUCCESS || it.status == CheckExecutionStatus.WARNING || it.status == CheckExecutionStatus.CRITICAL },
                warningCount = 0,
                criticalCount = 0,
                categoryStatuses = categories.toList(),
                technicalDetails = techDetails,
                scanDurationMs = System.currentTimeMillis() - startTime,
                isPartial = true
            )
        )
    }

    private suspend fun performLeakCheck(email: String): Pair<Int, List<String>> = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.xposedornot.com/v1/check-email/$email")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            val code = conn.responseCode
            if (code == 200) {
                val scanner = Scanner(conn.inputStream).useDelimiter("\\A")
                val response = if (scanner.hasNext()) scanner.next() else ""
                val json = JSONObject(response)
                val breachesArray = json.optJSONArray("breaches")
                val sources = mutableListOf<String>()
                if (breachesArray != null) {
                    for (i in 0 until breachesArray.length()) {
                        val breach = breachesArray.optJSONArray(i)
                        if (breach != null) sources.add(breach.optString(0))
                    }
                }
                Pair(sources.size, sources)
            } else Pair(0, emptyList())
        } catch (e: Exception) {
            Pair(0, emptyList())
        }
    }
}
