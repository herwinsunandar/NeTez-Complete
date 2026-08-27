package com.herwin.netez.network

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager

object WifiScanner {
    @SuppressLint("MissingPermission")
    fun getWifiScanResults(context: Context): List<ScanResult> {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifiManager.scanResults ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
