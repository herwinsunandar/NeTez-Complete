package com.herwin.netez.network

import com.herwin.netez.ui.viewmodels.PhishingCheckItem
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

object UrlHausService {
    private val client = OkHttpClient()

    suspend fun checkUrl(url: String): PhishingCheckItem = withContext(Dispatchers.IO) {
        return@withContext try {
            val start = System.currentTimeMillis()
            
            val formBody = FormBody.Builder()
                .add("url", url)
                .build()

            val request = Request.Builder()
                .url("https://urlhaus-api.abuse.ch/v1/url/")
                .post(formBody)
                .header("User-Agent", "Netez/1.0")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            val json = JSONObject(body)
            val queryStatus = json.optString("query_status", "")
            val latency = System.currentTimeMillis() - start

            when (queryStatus) {
                "ok" -> {
                    val threat = json.optString("threat", "malware")
                    val tags = json.optString("tags", "")
                    val urlStatus = json.optString("url_status", "online")
                    val ref = json.optString("urlhaus_reference", "https://urlhaus.abuse.ch/")
                    
                    val payloads = json.optJSONArray("payloads")?.let { arr ->
                        (0 until arr.length()).map { arr.getJSONObject(it).optString("signature", "") }
                            .filter { it.isNotEmpty() }
                            .joinToString(", ")
                    } ?: ""

                    PhishingCheckItem(
                        name = "URLhaus by Abuse.ch",
                        status = "PHISHING",
                        latency = latency,
                        description = "Threat: $threat • Status: $urlStatus • Tags: $tags • Payload: $payloads",
                        detailUrl = ref,
                        isVerified = true
                    )
                }
                "no_results" -> {
                    PhishingCheckItem(
                        name = "URLhaus by Abuse.ch",
                        status = "SAFE",
                        latency = latency,
                        description = "Not in 200k+ malware DB - Aman dari URLhaus"
                    )
                }
                else -> {
                    PhishingCheckItem(
                        name = "URLhaus by Abuse.ch",
                        status = "ERROR",
                        latency = latency,
                        description = "Response: $queryStatus"
                    )
                }
            }
        } catch (e: Exception) {
            PhishingCheckItem(
                name = "URLhaus by Abuse.ch",
                status = "ERROR",
                latency = 0,
                description = "Error: ${e.message} - offline feed tetap digunakan"
            )
        }
    }

    suspend fun checkPhishStats(url: String): PhishingCheckItem = withContext(Dispatchers.IO) {
        return@withContext try {
            val start = System.currentTimeMillis()
            val encodedUrl = URLEncoder.encode(url, "UTF-8")
            
            val request = Request.Builder()
                .url("https://api.phishstats.info/api/phishing?_where=(url,eq,$encodedUrl)")
                .get()
                .header("User-Agent", "Netez/1.0")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "[]"
            val jsonArray = JSONArray(body)
            val latency = System.currentTimeMillis() - start

            if (jsonArray.length() > 0) {
                val match = jsonArray.getJSONObject(0)
                val ip = match.optString("ip", "Unknown")
                val country = match.optString("country_name", "Unknown")
                val title = match.optString("title", "No Title")
                
                PhishingCheckItem(
                    name = "PhishStats.info",
                    status = "PHISHING",
                    latency = latency,
                    description = "LISTED! Host IP: $ip • Country: $country • Title: $title",
                    isVerified = true
                )
            } else {
                PhishingCheckItem(
                    name = "PhishStats.info",
                    status = "SAFE",
                    latency = latency,
                    description = "Not found in PhishStats real-time feed"
                )
            }
        } catch (e: Exception) {
            PhishingCheckItem(
                name = "PhishStats.info",
                status = "ERROR",
                latency = 0,
                description = "Error: ${e.message}"
            )
        }
    }
}
