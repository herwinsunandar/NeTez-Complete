package com.herwin.netez.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.herwin.netez.utils.IpCalculator
import com.herwin.netez.utils.SubnetInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class IpCalculatorState(
    val ip: String = "192.168.1.1",
    val cidr: String = "24",
    val fullInput: String = "192.168.1.1/24",
    val result: SubnetInfo? = null
)

class IpCalculatorViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(IpCalculatorState())
    val uiState: StateFlow<IpCalculatorState> = _uiState

    init {
        calculate()
    }

    fun updateIp(newInput: String) {
        _uiState.value = _uiState.value.copy(fullInput = newInput)
        if (newInput.contains("/")) {
            val parts = newInput.split("/")
            val ipPart = parts[0]
            val cidrPart = parts.getOrNull(1)?.filter { it.isDigit() } ?: _uiState.value.cidr
            _uiState.value = _uiState.value.copy(ip = ipPart, cidr = cidrPart)
        } else {
            _uiState.value = _uiState.value.copy(ip = newInput)
        }
        calculate()
    }

    fun updateCidr(newCidr: String) {
        val filtered = newCidr.filter { it.isDigit() }
        _uiState.value = _uiState.value.copy(cidr = filtered, fullInput = "${_uiState.value.ip}/$filtered")
        calculate()
    }

    private fun calculate() {
        val cidrInt = _uiState.value.cidr.toIntOrNull() ?: 24
        val info = IpCalculator.calculate(_uiState.value.ip, cidrInt)
        _uiState.value = _uiState.value.copy(result = info)
    }
}
