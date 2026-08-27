package com.herwin.netez.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.network.DnsManager
import com.herwin.netez.network.DnsRecord
import com.herwin.netez.network.DnsRecordType
import com.herwin.netez.network.DnsResponse
import com.herwin.netez.network.DnsServer
import com.herwin.netez.network.PublicDnsServers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class DnsState(
    val domain: String = "google.com",
    val recordType: DnsRecordType = DnsRecordType.A,
    val selectedServer: DnsServer = PublicDnsServers[0],
    val response: DnsResponse? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val counts: Map<DnsRecordType, Int> = emptyMap()
)

class DnsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(DnsState())
    val uiState: StateFlow<DnsState> = _uiState

    fun updateDomain(newDomain: String) {
        _uiState.value = _uiState.value.copy(domain = newDomain)
    }

    fun updateRecordType(type: DnsRecordType) {
        _uiState.value = _uiState.value.copy(recordType = type)
        if (_uiState.value.domain.isNotEmpty()) {
            lookup()
        }
    }

    fun updateServer(server: DnsServer) {
        _uiState.value = _uiState.value.copy(selectedServer = server)
        if (_uiState.value.domain.isNotEmpty()) {
            lookup()
        }
    }

    fun lookup() {
        if (_uiState.value.domain.isEmpty()) return
        
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val response = DnsManager.lookup(
                domain = _uiState.value.domain, 
                type = _uiState.value.recordType,
                server = _uiState.value.selectedServer
            )
            
            val newCounts = _uiState.value.counts.toMutableMap()
            newCounts[_uiState.value.recordType] = response.records.size
            
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                response = response,
                counts = newCounts,
                error = if (response.records.isEmpty()) "No records found" else null
            )
        }
    }
}
