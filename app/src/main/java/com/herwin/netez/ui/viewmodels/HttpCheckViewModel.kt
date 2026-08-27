package com.herwin.netez.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.data.AppDatabase
import com.herwin.netez.data.TestResult
import com.herwin.netez.network.HttpCheckManager
import com.herwin.netez.network.HttpCheckResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

data class HttpCheckState(
    val url: String = "google.com",
    val result: HttpCheckResult? = null,
    val history: List<HttpCheckResult> = emptyList(),
    val recentHosts: List<String> = listOf("google.com", "facebook.com", "github.com", "microsoft.com"),
    val isLoading: Boolean = false
)

class HttpCheckViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(HttpCheckState())
    val uiState: StateFlow<HttpCheckState> = _uiState
    private val dao = AppDatabase.getDatabase(application).testResultDao()

    fun updateUrl(newUrl: String) {
        _uiState.value = _uiState.value.copy(url = newUrl)
    }

    fun runCheck() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            val res = HttpCheckManager.checkWebsite(_uiState.value.url)
            val newHistory = (listOf(res) + _uiState.value.history).take(7)
            
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                result = res,
                history = newHistory
            )
            
            saveToDb(res)
        }
    }

    private fun saveToDb(res: HttpCheckResult) {
        viewModelScope.launch {
            dao.insertResult(TestResult(
                type = "HTTP",
                target = res.url,
                result = if (res.isAvailable) "ONLINE (${res.statusCode})" else "DOWN"
            ))
        }
    }
}
