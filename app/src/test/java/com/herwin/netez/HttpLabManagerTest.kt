package com.herwin.netez

import com.herwin.netez.network.HttpLabManager
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.junit.Assert.*
import org.junit.Test

class HttpLabManagerTest {

    private val manager = HttpLabManager()

    @Test
    fun testGetRequest() {
        val response = manager.executeRequest(
            method = "GET",
            url = "https://httpbin.org/get?source=netez&version=1",
            headers = mapOf("X-Test-Header" to "NeTezValue")
        )

        assertEquals(200, response.code)
        val body = response.body?.string() ?: ""
        assertTrue(body.contains("NeTezValue"))
        assertTrue(body.contains("source"))
    }

    @Test
    fun testPostRequestWithJson() {
        val jsonBody = """{"appName": "NeTez", "module": "HttpLab"}"""
        val response = manager.executeRequest(
            method = "POST",
            url = "https://httpbin.org/post",
            headers = mapOf("Content-Type" to "application/json"),
            body = jsonBody,
            contentType = "application/json"
        )

        assertEquals(200, response.code)
        val body = response.body?.string() ?: ""
        assertTrue(body.contains("NeTez"))
    }

    @Test
    fun testPutRequest() {
        val response = manager.executeRequest(
            method = "PUT",
            url = "https://httpbin.org/put",
            body = """{"updated": true}""",
            contentType = "application/json"
        )

        assertEquals(200, response.code)
        val body = response.body?.string() ?: ""
        assertTrue(body.contains("updated"))
    }

    @Test
    fun testDeleteRequest() {
        val response = manager.executeRequest(
            method = "DELETE",
            url = "https://httpbin.org/delete"
        )

        assertEquals(200, response.code)
    }

    @Test
    fun testBearerAuthHeader() {
        val token = "sample_bearer_token_123"
        val response = manager.executeRequest(
            method = "GET",
            url = "https://httpbin.org/headers",
            headers = mapOf("Authorization" to "Bearer $token")
        )

        assertEquals(200, response.code)
        val body = response.body?.string() ?: ""
        assertTrue(body.contains("Bearer sample_bearer_token_123"))
    }

    @Test
    fun testUrlBuilderWithParams() {
        val baseUrl = "https://httpbin.org/get"
        val urlBuilder = baseUrl.toHttpUrlOrNull()?.newBuilder()
        urlBuilder?.addQueryParameter("param1", "val1")
        urlBuilder?.addQueryParameter("param2", "val 2 spaces")
        val finalUrl = urlBuilder?.build()?.toString() ?: baseUrl

        assertEquals("https://httpbin.org/get?param1=val1&param2=val%202%20spaces", finalUrl)

        val response = manager.executeRequest(
            method = "GET",
            url = finalUrl
        )

        assertEquals(200, response.code)
        val body = response.body?.string() ?: ""
        assertTrue(body.contains("param1"))
        assertTrue(body.contains("val1"))
    }
}
