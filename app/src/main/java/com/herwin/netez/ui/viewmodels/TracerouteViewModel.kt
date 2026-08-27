package com.herwin.netez.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.network.Hop
import com.herwin.netez.network.TracerouteManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class TracerouteState(
    val host: String = "8.8.8.8",
    val hops: List<Hop> = emptyList(),
    val isRunning: Boolean = false
)

class TracerouteViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(TracerouteState())
    val uiState: StateFlow<TracerouteState> = _uiState

    private var traceJob: Job? = null

    fun updateHost(newHost: String) {
        _uiState.value = _uiState.value.copy(host = newHost)
    }

    fun toggleTrace() {
        if (_uiState.value.isRunning) {
            stopTrace()
        } else {
            startTrace()
        }
    }

    private fun startTrace() {
        _uiState.value = _uiState.value.copy(isRunning = true, hops = emptyList())
        traceJob = viewModelScope.launch {
            TracerouteManager.trace(_uiState.value.host).collect { hop ->
                _uiState.value = _uiState.value.copy(
                    hops = _uiState.value.hops + hop
                )
            }
            _uiState.value = _uiState.value.copy(isRunning = false)
        }
    }

    private fun stopTrace() {
        _uiState.value = _uiState.value.copy(isRunning = false)
        traceJob?.cancel()
    }
}
