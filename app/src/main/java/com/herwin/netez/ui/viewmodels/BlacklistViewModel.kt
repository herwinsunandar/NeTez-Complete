package com.herwin.netez.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.network.BlacklistManager
import com.herwin.netez.network.BlacklistResult
import com.herwin.netez.network.BlacklistStatus
import com.herwin.netez.utils.NetworkUtils
import kotlinx.coroutines.launch

class BlacklistViewModel : ViewModel() {
    var scannedIp by mutableStateOf("")
    var isScanning by mutableStateOf(false)
    var results by mutableStateOf<List<BlacklistResult>>(emptyList())
    var summary by mutableStateOf("")
    var error by mutableStateOf<String?>(null)

    fun fetchMyIp() {
        viewModelScope.launch {
            val ip = NetworkUtils.getPublicIp()
            if (ip != "Unknown") {
                scannedIp = ip
            }
        }
    }

    fun checkIp(ip: String) {
        if (ip.isBlank()) {
            error = "Please enter an IP address"
            return
        }
        
        scannedIp = ip
        isScanning = true
        error = null
        results = emptyList()
        summary = ""

        viewModelScope.launch {
            val checkResults = BlacklistManager.checkIp(ip)
            results = checkResults
            isScanning = false
            
            val listedCount = checkResults.count { it.status == BlacklistStatus.LISTED }
            val cleanCount = checkResults.count { it.status == BlacklistStatus.CLEAN }
            summary = "$listedCount Listed, $cleanCount Clean"
            
            if (checkResults.any { it.status == BlacklistStatus.ERROR }) {
                error = checkResults.first { it.status == BlacklistStatus.ERROR }.resultIp
            }
        }
    }
}
