package com.herwin.netez.network.api

import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path

interface HibpService {
    @GET("range/{prefix}")
    suspend fun getPwnedRange(@Path("prefix") prefix: String): ResponseBody

    companion object {
        private const val BASE_URL = "https://api.pwnedpasswords.com/"

        fun create(): HibpService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .build()
                .create(HibpService::class.java)
        }
    }
}
