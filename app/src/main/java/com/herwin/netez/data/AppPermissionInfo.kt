package com.herwin.netez.data

import android.graphics.drawable.Drawable
import androidx.annotation.StringRes

data class AppPermissionInfo(
    val appName: String,
    val packageName: String,
    val icon: Drawable?,
    val permissions: List<String>,
    val riskScore: Int,
    val riskCategory: RiskCategory,
    val isSystem: Boolean,
    val isTrusted: Boolean,
    @StringRes val riskReasonRes: Int = 0,
    val vendor: String = "Unknown Vendor"
)

enum class RiskCategory {
    CLEAN,
    SUSPICIOUS,
    DANGEROUS
}
