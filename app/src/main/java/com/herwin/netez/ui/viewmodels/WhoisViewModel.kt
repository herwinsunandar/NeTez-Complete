package com.herwin.netez.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.network.WhoisManager
import com.herwin.netez.network.WhoisResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class WhoisState(
    val query: String = "8.8.8.8",
    val result: WhoisResult? = null,
    val isLoading: Boolean = false
)

class WhoisViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(WhoisState())
    val uiState: StateFlow<WhoisState> = _uiState

    fun updateQuery(newQuery: String) {
        _uiState.value = _uiState.value.copy(query = newQuery)
    }

    fun fetchInfo() {
        if (_uiState.value.query.isBlank()) return
        
        _uiState.value = _uiState.value.copy(isLoading = true, result = null)
        viewModelScope.launch {
            val result = WhoisManager.getInfo(_uiState.value.query)
            _uiState.value = _uiState.value.copy(isLoading = false, result = result)
        }
    }
}
