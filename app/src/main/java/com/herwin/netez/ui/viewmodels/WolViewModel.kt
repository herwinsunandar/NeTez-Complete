package com.herwin.netez.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.network.WolManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class WolState(
    val mac: String = "",
    val ip: String = "255.255.255.255",
    val status: String = ""
)

class WolViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(WolState())
    val uiState: StateFlow<WolState> = _uiState

    fun updateMac(newMac: String) {
        _uiState.value = _uiState.value.copy(mac = newMac)
    }

    fun updateIp(newIp: String) {
        _uiState.value = _uiState.value.copy(ip = newIp)
    }

    fun sendPacket() {
        viewModelScope.launch {
            val success = WolManager.sendMagicPacket(_uiState.value.mac, _uiState.value.ip)
            _uiState.value = _uiState.value.copy(status = if (success) "Packet sent!" else "Failed to send packet")
        }
    }
}
