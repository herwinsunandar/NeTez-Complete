package com.herwin.netez.ui.viewmodels

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import com.herwin.netez.utils.IpCalculator
import com.herwin.netez.utils.SubnetInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class IpCalculatorState(
    val ip: String = "192.168.1.1",
    val cidr: String = "24",
    val fullInput: String = "192.168.1.1/24",
    val result: SubnetInfo? = null
)

class IpCalculatorViewModel(application: Application) : AndroidViewModel(application) {
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

    fun getShareText(): String {
        val res = _uiState.value.result ?: return ""
        val sb = StringBuilder("📋 NeTez IP Calculator Report\n")
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        sb.append("📅 Date: ${sdf.format(Date())}\n")
        sb.append("─────────────────────\n")
        sb.append("🎯 Input: ${res.networkId}/${res.cidr}\n")
        sb.append("🌐 Network Info:\n")
        sb.append("   • Network ID: ${res.networkId}\n")
        sb.append("   • Broadcast: ${res.broadcast}\n")
        sb.append("   • Range: ${res.firstHost} - ${res.lastHost}\n")
        sb.append("   • Netmask: ${res.mask}\n")
        sb.append("   • Wildcard: ${res.wildcard}\n")
        sb.append("   • Total Hosts: ${res.totalHosts}\n")
        sb.append("   • Class: ${res.ipClass} (${if (res.isPrivate) "Private" else "Public"})\n")
        sb.append("\n🔢 Advanced:\n")
        sb.append("   • Binary: ${res.binaryIp.chunked(8).joinToString(".")}\n")
        sb.append("   • Hex: ${res.hexIp}\n")
        sb.append("─────────────────────\n")
        sb.append("Sent from NeTez Network Tools")
        return sb.toString()
    }

    fun copyToClipboard() {
        val context = getApplication<Application>()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("IP Calculator Results", getShareText())
        clipboard.setPrimaryClip(clip)
    }

    fun shareResults(context: Context) {
        val text = getShareText()
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(intent, "Share IP Results"))
    }
}
