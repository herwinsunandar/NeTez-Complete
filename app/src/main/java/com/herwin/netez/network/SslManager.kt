package com.herwin.netez.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.URL
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.*
import javax.net.ssl.*

data class SslCertificateInfo(
    val host: String,
    val port: Int,
    val isValid: Boolean,
    val expiryDays: Long,
    val issuedDate: String,
    val expiryDate: String,
    val subject: String,
    val issuer: String,
    val serialNumber: String,
    val signatureAlgorithm: String,
    val publicKeyInfo: String,
    val san: List<String>,
    val protocol: String,
    val cipherSuite: String,
    val chain: List<CertificateChainNode>,
    val hstsEnabled: Boolean? = null
)

data class CertificateChainNode(
    val subject: String,
    val issuer: String,
    val isValid: Boolean
)

class SslManager {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm:ss z", Locale.ENGLISH)

    suspend fun checkSsl(host: String, port: Int = 443): SslCertificateInfo = withContext(Dispatchers.IO) {
        val trustManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(trustManager), java.security.SecureRandom())

        val factory = sslContext.socketFactory
        val socket = factory.createSocket() as SSLSocket
        
        // Enable SNI
        val sslParameters = socket.sslParameters
        sslParameters.serverNames = listOf(SNIHostName(host))
        socket.sslParameters = sslParameters

        try {
            socket.connect(InetSocketAddress(host, port), 10000)
            socket.startHandshake()

            val session = socket.session
            val chain = session.peerCertificates.filterIsInstance<X509Certificate>()
            val primaryCert = chain[0]

            val now = Date()
            val expiryDate = primaryCert.notAfter
            val diff = expiryDate.time - now.time
            val daysUntilExpiry = diff / (1000 * 60 * 60 * 24)

            val san = primaryCert.subjectAlternativeNames?.mapNotNull { it[1] as? String } ?: emptyList()
            
            val publicKey = primaryCert.publicKey
            val keyInfo = when (publicKey) {
                is java.security.interfaces.RSAPublicKey -> "RSA (${publicKey.modulus.bitLength()} bits)"
                is java.security.interfaces.ECPublicKey -> "ECC (${publicKey.params.curve.field.fieldSize} bits)"
                else -> publicKey.algorithm
            }

            val chainNodes = chain.map { cert ->
                CertificateChainNode(
                    subject = cert.subjectX500Principal.name,
                    issuer = cert.issuerX500Principal.name,
                    isValid = try {
                        cert.checkValidity()
                        true
                    } catch (e: Exception) {
                        false
                    }
                )
            }

            val hsts = checkHsts(host)

            SslCertificateInfo(
                host = host,
                port = port,
                isValid = try { primaryCert.checkValidity(); true } catch (e: Exception) { false },
                expiryDays = daysUntilExpiry,
                issuedDate = dateFormat.format(primaryCert.notBefore),
                expiryDate = dateFormat.format(primaryCert.notAfter),
                subject = primaryCert.subjectX500Principal.name,
                issuer = primaryCert.issuerX500Principal.name,
                serialNumber = primaryCert.serialNumber.toString(16).uppercase(),
                signatureAlgorithm = primaryCert.sigAlgName,
                publicKeyInfo = keyInfo,
                san = san,
                protocol = session.protocol,
                cipherSuite = session.cipherSuite,
                chain = chainNodes,
                hstsEnabled = hsts
            )
        } finally {
            try { socket.close() } catch (e: Exception) {}
        }
    }

    private fun checkHsts(host: String): Boolean? {
        return try {
            val url = URL("https://$host")
            val connection = url.openConnection() as HttpsURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            val hstsHeader = connection.getHeaderField("Strict-Transport-Security")
            hstsHeader != null
        } catch (e: Exception) {
            null
        }
    }
}
