package com.herwin.netez.engine.intelligence

object FindingsEngine {

    fun buildDomainFindings(
        sslResult: Map<String, String>?,
        blacklistDetections: Int,
        phishingStatus: String,
        dnsRecords: Map<String, String>,
        ip: String?
    ): Pair<List<SecurityFinding>, List<PassedCheck>> {
        val findings = mutableListOf<SecurityFinding>()
        val passedChecks = mutableListOf<PassedCheck>()

        // 1. Blacklist Findings
        if (blacklistDetections > 0) {
            findings.add(
                SecurityFinding(
                    id = "blacklist_detected",
                    category = CheckCategoryType.BLACKLIST,
                    severity = FindingSeverity.HIGH,
                    title = "Domain / Resolved IP Detected on Blacklists",
                    currentState = "$blacklistDetections detection(s) found",
                    riskImpact = "Domain or its IP is listed on global RBL/spam blacklists. Email or web traffic may be blocked.",
                    recommendation = "Check RBL list details and apply for delisting via the respective blacklisting authority.",
                    sourceProvider = "Global RBLs & AbuseIPDB",
                    technicalDetails = mapOf("Detections" to blacklistDetections.toString())
                )
            )
        } else {
            passedChecks.add(
                PassedCheck(
                    id = "blacklist_clean",
                    category = CheckCategoryType.BLACKLIST,
                    title = "No Blacklist Detections",
                    detail = "Clean status across monitored global RBL databases.",
                    sourceProvider = "Global RBLs & AbuseIPDB"
                )
            )
        }

        // 2. SSL/TLS Findings
        if (sslResult != null) {
            val isValid = sslResult["valid"] == "true"
            val daysLeft = sslResult["daysLeft"]?.toIntOrNull() ?: 999
            val grade = sslResult["grade"] ?: "A+"
            val tls = sslResult["tls"] ?: "TLS 1.3"

            if (!isValid) {
                findings.add(
                    SecurityFinding(
                        id = "ssl_invalid",
                        category = CheckCategoryType.SSL_TLS,
                        severity = FindingSeverity.CRITICAL,
                        title = "SSL/TLS Certificate Expired or Invalid",
                        currentState = "Invalid / Expired Certificate",
                        riskImpact = "Browsers will block visitors with security warnings (NET::ERR_CERT_DATE_INVALID).",
                        recommendation = "Renew and re-install a valid SSL/TLS certificate immediately.",
                        sourceProvider = "X.509 TLS Inspector"
                    )
                )
            } else {
                passedChecks.add(
                    PassedCheck(
                        id = "ssl_valid",
                        category = CheckCategoryType.SSL_TLS,
                        title = "Valid SSL/TLS Certificate",
                        detail = "Certificate is valid and active ($tls).",
                        sourceProvider = "X.509 TLS Inspector"
                    )
                )

                if (daysLeft in 1..30) {
                    findings.add(
                        SecurityFinding(
                            id = "ssl_expiring_soon",
                            category = CheckCategoryType.SSL_TLS,
                            severity = if (daysLeft <= 7) FindingSeverity.HIGH else FindingSeverity.LOW,
                            title = "SSL Certificate Expiring Soon",
                            currentState = "Expires in $daysLeft days",
                            riskImpact = "Service outage will occur if certificate expires before renewal.",
                            recommendation = "Initiate certificate renewal workflow before expiration.",
                            sourceProvider = "X.509 TLS Inspector"
                        )
                    )
                }

                if (grade == "F") {
                    findings.add(
                        SecurityFinding(
                            id = "ssl_grade_f",
                            category = CheckCategoryType.SSL_TLS,
                            severity = FindingSeverity.HIGH,
                            title = "Insecure SSL/TLS Configuration (Grade F)",
                            currentState = "Grade F (Vulnerable ciphers/protocols)",
                            riskImpact = "Susceptible to MITM or protocol downgrade attacks.",
                            recommendation = "Disable legacy TLS protocols (SSL 3.0 / TLS 1.0 / 1.1) and weak ciphers.",
                            sourceProvider = "X.509 TLS Inspector"
                        )
                    )
                }
            }
        }

        // 3. DNS Security Findings (SPF & DMARC)
        val spfRecord = dnsRecords["SPF"] ?: ""
        val dmarcRecord = dnsRecords["DMARC"] ?: ""

        if (spfRecord.isNotBlank()) {
            passedChecks.add(
                PassedCheck(
                    id = "dns_spf_ok",
                    category = CheckCategoryType.DNS_SECURITY,
                    title = "SPF Email Authentication Configured",
                    detail = spfRecord,
                    sourceProvider = "Cloudflare 1.1.1.1 Public DNS"
                )
            )
        } else {
            findings.add(
                SecurityFinding(
                    id = "dns_spf_missing",
                    category = CheckCategoryType.DNS_SECURITY,
                    severity = FindingSeverity.MEDIUM,
                    title = "SPF Record Not Configured",
                    currentState = "Missing v=spf1 TXT record",
                    riskImpact = "Attackers can spoof emails sent from this domain.",
                    recommendation = "Add an SPF TXT record in your DNS settings (e.g., v=spf1 mx ~all).",
                    sourceProvider = "Cloudflare 1.1.1.1 Public DNS"
                )
            )
        }

        if (dmarcRecord.isNotBlank()) {
            if (dmarcRecord.contains("p=none", ignoreCase = true)) {
                findings.add(
                    SecurityFinding(
                        id = "dns_dmarc_none",
                        category = CheckCategoryType.DNS_SECURITY,
                        severity = FindingSeverity.LOW,
                        title = "DMARC Policy Set to 'none'",
                        currentState = "p=none",
                        riskImpact = "Spoofed emails are monitored but not actively rejected or quarantined by receivers.",
                        recommendation = "Consider upgrading DMARC policy to 'p=quarantine' or 'p=reject' after testing.",
                        sourceProvider = "Cloudflare 1.1.1.1 Public DNS"
                    )
                )
            } else {
                passedChecks.add(
                    PassedCheck(
                        id = "dns_dmarc_ok",
                        category = CheckCategoryType.DNS_SECURITY,
                        title = "Strict DMARC Policy Enforced",
                        detail = dmarcRecord,
                        sourceProvider = "Cloudflare 1.1.1.1 Public DNS"
                    )
                )
            }
        } else {
            findings.add(
                SecurityFinding(
                    id = "dns_dmarc_missing",
                    category = CheckCategoryType.DNS_SECURITY,
                    severity = FindingSeverity.MEDIUM,
                    title = "DMARC Record Not Configured",
                    currentState = "Missing _dmarc TXT record",
                    riskImpact = "Receiving mail servers cannot verify authenticity of emails from this domain.",
                    recommendation = "Publish a DMARC TXT record under _dmarc.yourdomain.com.",
                    sourceProvider = "Cloudflare 1.1.1.1 Public DNS"
                )
            )
        }

        // 4. Phishing Findings
        if (phishingStatus == "PHISHING") {
            findings.add(
                SecurityFinding(
                    id = "phishing_active",
                    category = CheckCategoryType.PHISHING,
                    severity = FindingSeverity.CRITICAL,
                    title = "Active Phishing Threat Detected",
                    currentState = "Listed on threat intelligence feeds",
                    riskImpact = "Target is confirmed as a malicious phishing / malware URL.",
                    recommendation = "Do not interact with or enter credentials on this site.",
                    sourceProvider = "URLhaus (abuse.ch) & OpenPhish"
                )
            )
        } else {
            passedChecks.add(
                PassedCheck(
                    id = "phishing_clean",
                    category = CheckCategoryType.PHISHING,
                    title = "No Known Phishing Indicators",
                    detail = "No malicious flags in URLhaus & OpenPhish feeds.",
                    sourceProvider = "URLhaus (abuse.ch) & OpenPhish"
                )
            )
        }

        // 5. Exposure
        if (!ip.isNullOrBlank()) {
            passedChecks.add(
                PassedCheck(
                    id = "exposure_ip_resolved",
                    category = CheckCategoryType.EXPOSURE,
                    title = "Resolved Public IP",
                    detail = ip,
                    sourceProvider = "InetAddress Resolver"
                )
            )
        }

        return Pair(findings, passedChecks)
    }

    fun buildEmailFindings(
        email: String,
        breachesCount: Int,
        breachSources: List<String>,
        mailSecurityRecords: Map<String, String>
    ): Pair<List<SecurityFinding>, List<PassedCheck>> {
        val findings = mutableListOf<SecurityFinding>()
        val passedChecks = mutableListOf<PassedCheck>()

        if (breachesCount > 0) {
            findings.add(
                SecurityFinding(
                    id = "email_breached",
                    category = CheckCategoryType.BREACH_LEAK,
                    severity = FindingSeverity.HIGH,
                    title = "Email Found in Data Breaches",
                    currentState = "$breachesCount breach(es) detected (${breachSources.take(3).joinToString(", ")})",
                    riskImpact = "Associated credentials or metadata may be exposed in public leak databases.",
                    recommendation = "Change passwords on affected services and enable 2FA / Biometric login.",
                    sourceProvider = "XposedOrNot Leak DB"
                )
            )
        } else {
            passedChecks.add(
                PassedCheck(
                    id = "email_clean",
                    category = CheckCategoryType.BREACH_LEAK,
                    title = "No Known Breach Exposure",
                    detail = "Email address not detected in monitored breach databases.",
                    sourceProvider = "XposedOrNot Leak DB"
                )
            )
        }

        val spf = mailSecurityRecords["SPF"] ?: ""
        val dmarc = mailSecurityRecords["DMARC"] ?: ""

        if (spf.isNotBlank() && dmarc.isNotBlank()) {
            passedChecks.add(
                PassedCheck(
                    id = "mail_sec_ok",
                    category = CheckCategoryType.MAIL_SECURITY,
                    title = "Domain Mail Authentication Configured",
                    detail = "SPF and DMARC active for domain.",
                    sourceProvider = "Cloudflare 1.1.1.1 Public DNS"
                )
            )
        } else {
            findings.add(
                SecurityFinding(
                    id = "mail_sec_warning",
                    category = CheckCategoryType.MAIL_SECURITY,
                    severity = FindingSeverity.LOW,
                    title = "Mail Server Security Policy Improvement Opportunity",
                    currentState = "Incomplete SPF/DMARC configuration",
                    riskImpact = "Spoofed emails targeting or impersonating this address domain could occur.",
                    recommendation = "Ensure domain administrator configures valid SPF and DMARC TXT records.",
                    sourceProvider = "Cloudflare 1.1.1.1 Public DNS"
                )
            )
        }

        return Pair(findings, passedChecks)
    }
}
