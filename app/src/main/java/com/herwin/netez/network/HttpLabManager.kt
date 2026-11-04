package com.herwin.netez.network

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class HttpLabManager {
    private val baseClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private fun getUnsafeOkHttpClient(): OkHttpClient {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, SecureRandom())

        return baseClient.newBuilder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .build()
    }

    fun executeRequest(
        method: String,
        url: String,
        headers: Map<String, String> = emptyMap(),
        body: String? = null,
        contentType: String? = "application/json",
        allowSelfSigned: Boolean = false
    ): Response {
        val client = if (allowSelfSigned) getUnsafeOkHttpClient() else baseClient

        val requestBuilder = Request.Builder().url(url)

        if (headers.keys.none { it.equals("User-Agent", ignoreCase = true) }) {
            requestBuilder.addHeader("User-Agent", "NeTez-NetworkTools/7.0")
        }

        headers.forEach { (k, v) -> requestBuilder.addHeader(k, v) }

        val mediaType = (contentType ?: "application/json").toMediaTypeOrNull()
        val requestBody = body?.toRequestBody(mediaType)

        when (method.uppercase()) {
            "GET" -> requestBuilder.get()
            "POST" -> requestBuilder.post(requestBody ?: "".toRequestBody(mediaType))
            "PUT" -> requestBuilder.put(requestBody ?: "".toRequestBody(mediaType))
            "DELETE" -> if (requestBody != null) requestBuilder.delete(requestBody) else requestBuilder.delete()
            "PATCH" -> requestBuilder.patch(requestBody ?: "".toRequestBody(mediaType))
        }

        return client.newCall(requestBuilder.build()).execute()
    }
}
