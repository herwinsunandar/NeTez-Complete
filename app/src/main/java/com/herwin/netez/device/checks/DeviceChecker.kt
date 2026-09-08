package com.herwin.netez.device.checks

import android.content.Context
import com.herwin.netez.device.DeviceCheckItem

interface DeviceChecker {
    suspend fun check(context: Context): DeviceCheckItem
}
