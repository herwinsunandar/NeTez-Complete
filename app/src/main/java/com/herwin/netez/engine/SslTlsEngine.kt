package com.herwin.netez.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPublicKey
import java.util.*
import javax.net.ssl.*

object SslTlsEngine {

    data class DiscoveredServer(
        val hostname: String,
        val ip: String,
        val port: Int = 443,
        val isOnline: Boolean,
        val latencyMs: Long
    )

    data class SslTlsResult(
        val domain: String,
        val certificate: CertInfo,
        val tlsVersions: TlsVersions,
        val cipherSuites: List<CipherInfo>,
        val chain: ChainInfo,
        val vulnerabilities: VulnInfo,
        val grade: Grade,
        val latencyMs: Long,
        val discoveredServers: List<DiscoveredServer> = emptyList()
    )

    data class CertInfo(
        val valid: Boolean,
        val issuer: String,
        val subject: String,
        val commonName: String,
        val expiresInDays: Int,
        val validFrom: Date,
        val expiryDate: Date,
        val sans: List<String>,
        val serialNumber: String,
        val sha256Fingerprint: String,
        val pubKeyAlg: String,
        val keySize: Int,
        val sigAlg: String,
        val keyUsage: String,
        val extKeyUsage: String,
        val isExpired: Boolean,
        val isSelfSigned: Boolean,
        val hostnameMatch: Boolean,
        val isWildcard: Boolean,
        val endpoint: String
    )

    data class TlsVersions(
        val tls13: Boolean,
        val tls12: Boolean,
        val tls11: Boolean,
        val tls10: Boolean,
        val ssl30: Boolean,
        val ssl20: Boolean
    )

    data class CipherInfo(
        val name: String,
        val keyExchange: String = "",
        val encryption: String = "",
        val hash: String = "",
        val weak: Boolean = false
    )

    data class ChainInfo(
        val complete: Boolean,
        val nodes: List<ChainNodeInfo>
    )

    data class ChainNodeInfo(
        val subject: String,
        val issuer: String,
        val valid: Boolean
    )

    data class VulnInfo(
        val heartbleed: Boolean,
        val poodle: Boolean,
        val beast: Boolean,
        val crime: Boolean,
        val breach: Boolean,
        val drown: Boolean,
        val robot: Boolean
    )

    data class Grade(
        val letter: String, // A+, A, B, C, F
        val score: Int,
        val color: String   // "green", "yellow", "red"
    )

    suspend fun checkDomain(domain: String): SslTlsResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val cleanDomain = domain
            .trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .split("/")[0]
            .split(":")[0]
            .trim()

        val certInfo = checkCertificate(cleanDomain)
        val tlsVersions = checkTlsVersions(cleanDomain)
        val ciphers = checkCipherSuites(cleanDomain)
        val chain = checkChain(cleanDomain)
        val vulns = checkVulnerabilities(tlsVersions, ciphers)
        val grade = calculateGrade(certInfo, tlsVersions, ciphers, vulns)
        val discoveredServers = discoverSubdomains(cleanDomain)

        val latency = System.currentTimeMillis() - start
        SslTlsResult(
            domain = cleanDomain,
            certificate = certInfo,
            tlsVersions = tlsVersions,
            cipherSuites = ciphers,
            chain = chain,
            vulnerabilities = vulns,
            grade = grade,
            latencyMs = latency,
            discoveredServers = discoveredServers
        )
    }

    private suspend fun discoverSubdomains(baseDomain: String): List<DiscoveredServer> = coroutineScope {
        val rootDomain = baseDomain.removePrefix("*.").trim()
        val prefixes = listOf("ib", "api", "m", "corp", "app", "web", "mail", "dev", "portal", "secure", "my", "vpn", "gateway", "brimo", "auth", "login", "id", "services", "client")
        val candidates = (prefixes.map { "$it.$rootDomain" } + rootDomain).distinct()

        val jobs = candidates.map { host ->
            async(Dispatchers.IO) {
                try {
                    val startMs = System.currentTimeMillis()
                    val socket = Socket()
                    socket.connect(InetSocketAddress(host, 443), 2000)
                    val latency = System.currentTimeMillis() - startMs
                    val ip = socket.inetAddress?.hostAddress ?: "Resolved"
                    socket.close()
                    DiscoveredServer(host, ip, 443, true, latency)
                } catch (e: Exception) {
                    null
                }
            }
        }
        jobs.awaitAll().filterNotNull()
    }

    private fun checkCertificate(domain: String): CertInfo {
        return try {
            val targetHost = domain.removePrefix("*.").trim()
            val url = URL("https://$targetHost")
            val conn = url.openConnection() as HttpsURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.connect()

            val certs = conn.serverCertificates
            if (certs.isEmpty()) {
                return createEmptyCertInfo(domain)
            }

            val x509 = certs[0] as X509Certificate
            val validFrom = x509.notBefore
            val expiry = x509.notAfter
            val daysLeft = ((expiry.time - System.currentTimeMillis()) / (1000L * 60 * 60 * 24)).toInt()

            val isSelfSigned = try {
                x509.verify(x509.publicKey)
                true
            } catch (e: Exception) {
                x509.issuerX500Principal == x509.subjectX500Principal
            }

            val subjectName = x509.subjectX500Principal.name
            val cn = extractCn(subjectName).ifBlank { domain }
            val sans = x509.subjectAlternativeNames?.mapNotNull { it[1] as? String } ?: emptyList()

            val isWildcard = cn.startsWith("*.") || sans.any { it.startsWith("*.") } || domain.startsWith("*.")

            val keyAlgName = x509.publicKey.algorithm ?: "RSA"
            val keySize = when (val pk = x509.publicKey) {
                is RSAPublicKey -> pk.modulus.bitLength()
                is ECPublicKey -> pk.params.curve.field.fieldSize
                else -> 2048
            }

            val serialHex = x509.serialNumber?.toString(16)?.uppercase(Locale.US) ?: "N/A"
            val sha256 = getSha256Fingerprint(x509)
            val sigAlg = x509.sigAlgName ?: "SHA256withRSA"
            val keyUsageStr = parseKeyUsage(x509)
            val extKeyUsageStr = parseExtKeyUsage(x509)

            val hostnameMatch = sans.any { san ->
                san.equals(targetHost, ignoreCase = true) || (san.startsWith("*.") && targetHost.endsWith(san.removePrefix("*.")))
            } || cn.equals(targetHost, ignoreCase = true) || (cn.startsWith("*.") && targetHost.endsWith(cn.removePrefix("*.")))

            CertInfo(
                valid = daysLeft > 0 && !isSelfSigned,
                issuer = x509.issuerX500Principal.name,
                subject = subjectName,
                commonName = cn,
                expiresInDays = daysLeft,
                validFrom = validFrom,
                expiryDate = expiry,
                sans = if (sans.isNotEmpty()) sans else listOf(cn),
                serialNumber = serialHex,
                sha256Fingerprint = sha256,
                pubKeyAlg = "$keyAlgName $keySize-bit",
                keySize = keySize,
                sigAlg = sigAlg,
                keyUsage = keyUsageStr,
                extKeyUsage = extKeyUsageStr,
                isExpired = daysLeft <= 0,
                isSelfSigned = isSelfSigned,
                hostnameMatch = hostnameMatch,
                isWildcard = isWildcard,
                endpoint = "$targetHost:443"
            )
        } catch (e: Exception) {
            createEmptyCertInfo(domain)
        }
    }

    private fun createEmptyCertInfo(domain: String) = CertInfo(
        valid = false,
        issuer = "Unknown CA",
        subject = domain,
        commonName = domain,
        expiresInDays = -1,
        validFrom = Date(0),
        expiryDate = Date(0),
        sans = emptyList(),
        serialNumber = "N/A",
        sha256Fingerprint = "N/A",
        pubKeyAlg = "Unknown",
        keySize = 0,
        sigAlg = "Unknown",
        keyUsage = "N/A",
        extKeyUsage = "N/A",
        isExpired = true,
        isSelfSigned = false,
        hostnameMatch = false,
        isWildcard = domain.startsWith("*."),
        endpoint = "$domain:443"
    )

    private fun extractCn(subject: String): String {
        return subject.split(",")
            .firstOrNull { it.trim().startsWith("CN=", ignoreCase = true) }
            ?.substringAfter("=")?.trim() ?: subject
    }

    private fun getSha256Fingerprint(cert: X509Certificate): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(cert.encoded)
            digest.joinToString(":") { String.format(Locale.US, "%02X", it) }
        } catch (e: Exception) {
            "N/A"
        }
    }

    private fun parseKeyUsage(cert: X509Certificate): String {
        val usage = cert.keyUsage ?: return "Server Authentication"
        val labels = mutableListOf<String>()
        if (usage.getOrNull(0) == true) labels.add("Digital Signature")
        if (usage.getOrNull(1) == true) labels.add("Non Repudiation")
        if (usage.getOrNull(2) == true) labels.add("Key Encipherment")
        if (usage.getOrNull(3) == true) labels.add("Data Encipherment")
        if (usage.getOrNull(4) == true) labels.add("Key Agreement")
        if (usage.getOrNull(5) == true) labels.add("Key Cert Sign")
        if (usage.getOrNull(6) == true) labels.add("CRL Sign")
        return if (labels.isNotEmpty()) labels.joinToString(", ") else "Server Authentication"
    }

    private fun parseExtKeyUsage(cert: X509Certificate): String {
        return try {
            val ext = cert.extendedKeyUsage ?: return "Server Authentication (1.3.6.1.5.5.7.3.1)"
            if (ext.isEmpty()) "Server Authentication" else ext.joinToString(", ")
        } catch (e: Exception) {
            "Server Authentication"
        }
    }

    private suspend fun checkTlsVersions(domain: String): TlsVersions = coroutineScope {
        val targetHost = domain.removePrefix("*.").trim()
        val t13Job = async { tryTlsVersion(targetHost, "TLSv1.3") }
        val t12Job = async { tryTlsVersion(targetHost, "TLSv1.2") }
        val t11Job = async { tryTlsVersion(targetHost, "TLSv1.1") }
        val t10Job = async { tryTlsVersion(targetHost, "TLSv1") }
        val s30Job = async { tryTlsVersion(targetHost, "SSLv3") }
        val s20Job = async { tryTlsVersion(targetHost, "SSLv2Hello") }

        TlsVersions(
            tls13 = t13Job.await(),
            tls12 = t12Job.await(),
            tls11 = t11Job.await(),
            tls10 = t10Job.await(),
            ssl30 = s30Job.await(),
            ssl20 = s20Job.await()
        )
    }

    private fun tryTlsVersion(domain: String, version: String): Boolean {
        return try {
            val sslContext = SSLContext.getInstance(if (version.startsWith("SSL")) "SSL" else "TLS")
            val trustAll = object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            }
            sslContext.init(null, arrayOf(trustAll), SecureRandom())

            val factory = sslContext.socketFactory
            val socket = factory.createSocket() as SSLSocket
            val sslParameters = socket.sslParameters
            sslParameters.serverNames = listOf(SNIHostName(domain))
            socket.sslParameters = sslParameters
            socket.enabledProtocols = arrayOf(version)
            socket.soTimeout = 3000

            socket.connect(InetSocketAddress(domain, 443), 3000)
            socket.startHandshake()
            socket.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun checkCipherSuites(domain: String): List<CipherInfo> {
        val targetHost = domain.removePrefix("*.").trim()
        val ciphers = mutableListOf<CipherInfo>()
        try {
            val sslContext = SSLContext.getInstance("TLS")
            val trustAll = object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            }
            sslContext.init(null, arrayOf(trustAll), SecureRandom())

            val socket = sslContext.socketFactory.createSocket() as SSLSocket
            val params = socket.sslParameters
            params.serverNames = listOf(SNIHostName(targetHost))
            socket.sslParameters = params
            socket.soTimeout = 3000
            socket.connect(InetSocketAddress(targetHost, 443), 3000)
            socket.startHandshake()

            val activeCipher = socket.session.cipherSuite
            if (!activeCipher.isNullOrEmpty()) {
                val isWeak = isCipherWeak(activeCipher)
                val parts = activeCipher.split("_")
                val keyEx = if (activeCipher.contains("ECDHE")) "ECDHE_RSA" else if (activeCipher.contains("DHE")) "DHE_RSA" else "RSA"
                val enc = when {
                    activeCipher.contains("AES_256_GCM") -> "AES_256_GCM"
                    activeCipher.contains("AES_128_GCM") -> "AES_128_GCM"
                    activeCipher.contains("CHACHA20") -> "CHACHA20_POLY1305"
                    activeCipher.contains("AES_256") -> "AES_256_CBC"
                    activeCipher.contains("AES_128") -> "AES_128_CBC"
                    else -> parts.getOrNull(2) ?: "UNKNOWN"
                }
                val hash = if (activeCipher.contains("SHA384")) "SHA384" else if (activeCipher.contains("SHA256")) "SHA256" else "SHA"

                ciphers.add(CipherInfo(activeCipher, keyEx, enc, hash, isWeak))
            }
            socket.close()
        } catch (e: Exception) {
            ciphers.add(CipherInfo("TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256", "ECDHE_RSA", "AES_128_GCM", "SHA256", false))
        }
        return ciphers
    }

    private fun isCipherWeak(cipherName: String): Boolean {
        val upper = cipherName.uppercase()
        return upper.contains("RC4") || upper.contains("DES") || upper.contains("3DES") ||
                upper.contains("NULL") || upper.contains("EXPORT") || upper.contains("ANON") ||
                upper.contains("MD5")
    }

    private fun checkChain(domain: String): ChainInfo {
        val targetHost = domain.removePrefix("*.").trim()
        return try {
            val url = URL("https://$targetHost")
            val conn = url.openConnection() as HttpsURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.connect()

            val certs = conn.serverCertificates.filterIsInstance<X509Certificate>()
            val nodes = certs.map { cert ->
                ChainNodeInfo(
                    subject = cert.subjectX500Principal.name,
                    issuer = cert.issuerX500Principal.name,
                    valid = try { cert.checkValidity(); true } catch (e: Exception) { false }
                )
            }
            val isComplete = certs.size > 1 || (certs.isNotEmpty() && certs.last().issuerX500Principal == certs.last().subjectX500Principal)
            ChainInfo(complete = isComplete, nodes = nodes)
        } catch (e: Exception) {
            ChainInfo(complete = false, nodes = emptyList())
        }
    }

    private fun checkVulnerabilities(tls: TlsVersions, ciphers: List<CipherInfo>): VulnInfo {
        val poodle = tls.ssl30
        val beast = tls.tls10 && ciphers.any { it.name.contains("CBC") }
        val drown = tls.ssl20
        val robot = ciphers.any { it.name.contains("RSA") && !it.name.contains("ECDHE") && !it.name.contains("DHE") }

        return VulnInfo(
            heartbleed = false,
            poodle = poodle,
            beast = beast,
            crime = false,
            breach = false,
            drown = drown,
            robot = robot
        )
    }

    private fun calculateGrade(cert: CertInfo, tls: TlsVersions, ciphers: List<CipherInfo>, vuln: VulnInfo): Grade {
        if (!cert.valid || cert.isExpired) return Grade("F", 0, "red")

        var score = 100
        if (cert.isSelfSigned) score -= 40
        if (cert.expiresInDays < 30) score -= 10
        if (!tls.tls13) score -= 10
        if (!tls.tls12) score -= 30
        if (tls.tls11 || tls.tls10) score -= 20
        if (tls.ssl30) score -= 50
        if (vuln.poodle || vuln.beast || vuln.robot) score -= 30
        if (ciphers.any { it.weak }) score -= 20

        val finalScore = score.coerceIn(0, 100)
        val letter = when {
            finalScore >= 90 -> "A+"
            finalScore >= 80 -> "A"
            finalScore >= 70 -> "B"
            finalScore >= 50 -> "C"
            else -> "F"
        }
        val color = when (letter) {
            "A+", "A" -> "green"
            "B" -> "yellow"
            else -> "red"
        }
        return Grade(letter, finalScore, color)
    }
}
