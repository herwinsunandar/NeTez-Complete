package com.herwin.netez.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.network.SslCertificateInfo
import com.herwin.netez.network.SslManager
import kotlinx.coroutines.launch

class SslCheckerViewModel : ViewModel() {
    private val sslManager = SslManager()

    var target by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var result by mutableStateOf<SslCertificateInfo?>(null)
    var error by mutableStateOf<String?>(null)

    fun checkSsl() {
        if (target.isBlank()) {
            error = "Please enter a host"
            return
        }

        val parts = target.split(":")
        val host = parts[0]
        val port = if (parts.size > 1) parts[1].toIntOrNull() ?: 443 else 443

        viewModelScope.launch {
            isLoading = true
            error = null
            result = null
            try {
                result = sslManager.checkSsl(host, port)
            } catch (e: Exception) {
                error = e.localizedMessage ?: "Failed to connect"
            } finally {
                isLoading = false
            }
        }
    }
}
