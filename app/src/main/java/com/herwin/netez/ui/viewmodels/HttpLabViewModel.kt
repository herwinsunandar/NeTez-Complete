package com.herwin.netez.ui.viewmodels

import android.app.Application
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.data.AppDatabase
import com.herwin.netez.data.HttpHistory
import com.herwin.netez.network.HttpLabManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Response
import java.io.IOException

data class HttpLabState(
    val method: String = "GET",
    val url: String = "https://",
    val params: List<Pair<String, String>> = listOf("" to ""),
    val headers: List<Pair<String, String>> = listOf("" to ""),
    val body: String = "",
    val contentType: String = "application/json",
    val authType: String = "None",
    val authValue: String = "",
    val allowSelfSigned: Boolean = false,
    val isLoading: Boolean = false,
    val response: HttpResponseData? = null,
    val error: String? = null,
    val history: List<HttpHistory> = emptyList()
)

data class HttpResponseData(
    val code: Int,
    val message: String,
    val time: Long,
    val size: Long,
    val headers: Map<String, String>,
    val body: String
)

class HttpLabViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(HttpLabState())
    val uiState: StateFlow<HttpLabState> = _uiState.asStateFlow()

    private val manager = HttpLabManager()
    private val db = AppDatabase.getDatabase(application)
    private val dao = db.httpHistoryDao()

    init {
        viewModelScope.launch {
            dao.getRecentHistory().collect { historyList ->
                _uiState.value = _uiState.value.copy(history = historyList)
            }
        }
    }

    fun updateMethod(method: String) = updateState { it.copy(method = method) }
    fun updateUrl(url: String) = updateState { it.copy(url = url) }
    fun updateBody(body: String) = updateState { it.copy(body = body) }
    fun updateContentType(contentType: String) = updateState { it.copy(contentType = contentType) }
    fun updateAuthType(type: String) = updateState { it.copy(authType = type) }
    fun updateAuthValue(value: String) = updateState { it.copy(authValue = value) }
    fun updateAllowSelfSigned(allow: Boolean) = updateState { it.copy(allowSelfSigned = allow) }
    
    fun addParam() = updateState { it.copy(params = it.params + ("" to "")) }
    fun updateParam(index: Int, key: String, value: String) {
        val newParams = _uiState.value.params.toMutableList()
        newParams[index] = key to value
        updateState { it.copy(params = newParams) }
    }
    fun removeParam(index: Int) = updateState { it.copy(params = it.params.filterIndexed { i, _ -> i != index }) }

    fun addHeader() = updateState { it.copy(headers = it.headers + ("" to "")) }
    fun updateHeader(index: Int, key: String, value: String) {
        val newHeaders = _uiState.value.headers.toMutableList()
        newHeaders[index] = key to value
        updateState { it.copy(headers = newHeaders) }
    }
    fun removeHeader(index: Int) = updateState { it.copy(headers = it.headers.filterIndexed { i, _ -> i != index }) }

    private fun updateState(transform: (HttpLabState) -> HttpLabState) {
        _uiState.value = transform(_uiState.value)
    }

    fun sendRequest() {
        val state = _uiState.value
        if (state.url.isBlank()) return

        updateState { it.copy(isLoading = true, response = null, error = null) }

        viewModelScope.launch(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            try {
                val rawUrl = state.url.trim()
                val formattedUrl = if (!rawUrl.startsWith("http://", ignoreCase = true) && !rawUrl.startsWith("https://", ignoreCase = true)) {
                    "https://$rawUrl"
                } else {
                    rawUrl
                }

                // Construct URL with params
                val urlBuilder = formattedUrl.toHttpUrlOrNull()?.newBuilder()
                state.params.forEach { (k, v) ->
                    if (k.isNotBlank()) urlBuilder?.addQueryParameter(k.trim(), v.trim())
                }
                val finalUrl = urlBuilder?.build()?.toString() ?: formattedUrl

                // Construct headers
                val headerMap = state.headers.filter { it.first.isNotBlank() }.toMap().toMutableMap()
                when (state.authType) {
                    "Bearer" -> if (state.authValue.isNotBlank()) {
                        headerMap["Authorization"] = "Bearer ${state.authValue.trim()}"
                    }
                    "Basic" -> if (state.authValue.isNotBlank()) {
                        val encoded = if (state.authValue.contains(":")) {
                            Base64.encodeToString(state.authValue.toByteArray(), Base64.NO_WRAP)
                        } else {
                            state.authValue.trim()
                        }
                        headerMap["Authorization"] = "Basic $encoded"
                    }
                    "API Key" -> if (state.authValue.isNotBlank()) {
                        val parts = state.authValue.split(":", limit = 2)
                        if (parts.size == 2) {
                            headerMap[parts[0].trim()] = parts[1].trim()
                        } else {
                            headerMap["X-API-Key"] = parts[0].trim()
                        }
                    }
                }

                val response = manager.executeRequest(
                    method = state.method,
                    url = finalUrl,
                    headers = headerMap,
                    body = if (state.method in listOf("POST", "PUT", "PATCH", "DELETE")) state.body else null,
                    contentType = state.contentType,
                    allowSelfSigned = state.allowSelfSigned
                )

                val endTime = System.currentTimeMillis()
                val responseBody = response.body?.string() ?: ""
                val responseData = HttpResponseData(
                    code = response.code,
                    message = response.message,
                    time = endTime - startTime,
                    size = response.body?.contentLength() ?: responseBody.length.toLong(),
                    headers = response.headers.toMap(),
                    body = responseBody
                )

                dao.insert(HttpHistory(
                    method = state.method,
                    url = state.url,
                    responseCode = response.code,
                    responseTime = endTime - startTime
                ))
                dao.clearOldHistory()

                _uiState.value = _uiState.value.copy(isLoading = false, response = responseData)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Unknown error")
            }
        }
    }

    fun parseCurl(curl: String) {
        // Simple cURL parser
        try {
            val method = if (curl.contains("-X ")) curl.substringAfter("-X ").substringBefore(" ").trim('"', '\'') else "GET"
            val url = curl.substringAfter("'").substringBefore("'").takeIf { it.startsWith("http") }
                ?: curl.substringAfter("\"").substringBefore("\"").takeIf { it.startsWith("http") }
                ?: ""
            
            val headers = mutableListOf<Pair<String, String>>()
            val headerMatches = Regex("-H [\"'](.+?)[\"']").findAll(curl)
            headerMatches.forEach { match ->
                val header = match.groupValues[1]
                val parts = header.split(":", limit = 2)
                if (parts.size == 2) headers.add(parts[0].trim() to parts[1].trim())
            }

            val body = if (curl.contains("-d ")) curl.substringAfter("-d ").substringBefore(" -").trim('"', '\'') else ""

            updateState { it.copy(
                method = method,
                url = url,
                headers = if (headers.isEmpty()) listOf("" to "") else headers,
                body = body
            ) }
        } catch (e: Exception) {
            updateState { it.copy(error = "Failed to parse cURL") }
        }
    }
}
