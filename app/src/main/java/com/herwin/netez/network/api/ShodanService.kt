package com.herwin.netez.network.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ShodanService {
    @GET("shodan/host/{ip}")
    suspend fun getHostInfo(
        @Path("ip") ip: String,
        @Query("key") apiKey: String
    ): ShodanResponse

    companion object {
        private const val BASE_URL = "https://api.shodan.io/"

        fun create(): ShodanService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ShodanService::class.java)
        }
    }
}

data class ShodanResponse(
    val ip_str: String,
    val ports: List<Int>,
    val isp: String? = null,
    val os: String? = null,
    val hostnames: List<String>? = null,
    val data: List<ShodanData>? = null
)

data class ShodanData(
    val port: Int,
    val transport: String,
    val product: String? = null,
    val version: String? = null,
    val extra_info: String? = null
)
