package com.herwin.netez.network.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface AbuseIpDbService {
    @GET("api/v2/check")
    suspend fun checkIp(
        @Header("Key") apiKey: String,
        @Query("ipAddress") ipAddress: String,
        @Query("maxAgeInDays") maxAgeInDays: Int = 90,
        @Query("verbose") verbose: Boolean = true
    ): AbuseIpDbResponse

    companion object {
        private const val BASE_URL = "https://api.abuseipdb.com/"

        fun create(): AbuseIpDbService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(AbuseIpDbService::class.java)
        }
    }
}

data class AbuseIpDbResponse(
    val data: AbuseData
)

data class AbuseData(
    val ipAddress: String,
    val isPublic: Boolean,
    val ipVersion: Int,
    val isWhitelisted: Boolean,
    val abuseConfidenceScore: Int,
    val countryCode: String,
    val usageType: String?,
    val isp: String,
    val domain: String?,
    val hostnames: List<String>,
    val totalReports: Int,
    val numDistinctUsers: Int,
    val lastReportedAt: String?,
    val reports: List<AbuseReport>?
)

data class AbuseReport(
    val reportedAt: String,
    val comment: String,
    val categories: List<Int>,
    val reporterId: Int,
    val reporterCountryCode: String,
    val reporterCountryName: String
)
