package com.herwin.netez.ui.viewmodels

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.network.HttpHeaderManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HttpHeaderState(
    val url: String = "google.com",
    val headers: Map<String?, List<String>> = emptyMap(),
    val isLoading: Boolean = false
)

class HttpHeaderViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(HttpHeaderState())
    val uiState: StateFlow<HttpHeaderState> = _uiState

    fun updateUrl(newUrl: String) {
        _uiState.value = _uiState.value.copy(url = newUrl)
    }

    fun fetchHeaders() {
        _uiState.value = _uiState.value.copy(isLoading = true, headers = emptyMap())
        viewModelScope.launch {
            val res = HttpHeaderManager.getHeaders(_uiState.value.url)
            _uiState.value = _uiState.value.copy(isLoading = false, headers = res)
        }
    }

    fun getShareText(): String {
        val state = _uiState.value
        if (state.headers.isEmpty()) return ""
        
        val sb = StringBuilder("🌐 NeTez HTTP Headers Report\n")
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        sb.append("📅 Date: ${sdf.format(Date())}\n")
        sb.append("─────────────────────\n")
        sb.append("🎯 Target: ${state.url}\n")
        sb.append("📊 Headers:\n")
        
        state.headers.forEach { (key, values) ->
            sb.append("   • ${key ?: "Status"}: ${values.joinToString(", ")}\n")
        }
        
        sb.append("─────────────────────\n")
        sb.append("Sent from NeTez Network Tools")
        return sb.toString()
    }

    fun copyResultsToClipboard() {
        val context = getApplication<Application>()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("HTTP Headers", getShareText())
        clipboard.setPrimaryClip(clip)
    }

    fun shareResults(context: Context) {
        val text = getShareText()
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(intent, "Share HTTP Headers"))
    }
}
