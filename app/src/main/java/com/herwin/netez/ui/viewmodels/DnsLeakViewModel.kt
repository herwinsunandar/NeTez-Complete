package com.herwin.netez.ui.viewmodels

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.R
import com.herwin.netez.network.DnsLeakEngine
import com.herwin.netez.network.DnsLeakResult
import kotlinx.coroutines.launch

class DnsLeakViewModel(application: Application) : AndroidViewModel(application) {
    var result by mutableStateOf<DnsLeakResult?>(null)
    var isTesting by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    fun startTest() {
        viewModelScope.launch {
            isTesting = true
            error = null
            try {
                result = DnsLeakEngine.testDnsLeak(getApplication())
            } catch (e: Exception) {
                error = e.localizedMessage ?: "Unknown error"
            } finally {
                isTesting = false
            }
        }
    }

    private fun getShareText(context: Context): String {
        val res = result ?: return ""
        val vpnStatus = if (res.vpnActive) "ON" else "OFF"
        val dnsInfo = res.dnsServers.firstOrNull()?.let { "${it.ip} ${it.isp}" } ?: "Unknown"
        
        return context.getString(
            R.string.dns_leak_share_report,
            res.leakCount,
            dnsInfo,
            res.isp,
            vpnStatus
        )
    }

    fun copyToClipboard(context: Context) {
        val text = getShareText(context)
        if (text.isEmpty()) return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("DNS Leak Report", text)
        clipboard.setPrimaryClip(clip)
    }

    fun shareResults(context: Context) {
        val text = getShareText(context)
        if (text.isEmpty()) return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share DNS Leak Results"))
    }
}
