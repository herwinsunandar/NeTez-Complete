package com.herwin.netez.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.network.HttpHeaderManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HttpHeaderState(
    val url: String = "google.com",
    val headers: Map<String, List<String>> = emptyMap(),
    val isLoading: Boolean = false
)

class HttpHeaderViewModel : ViewModel() {
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
}
