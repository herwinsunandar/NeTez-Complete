package com.herwin.netez.network

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.herwin.netez.R
import com.herwin.netez.data.AppPermissionInfo
import com.herwin.netez.data.RiskCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppScannerManager(private val context: Context) {

    suspend fun scanApps(): List<AppPermissionInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val result = mutableListOf<AppPermissionInfo>()

        for (app in apps) {
            try {
                val packageInfo = pm.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS)
                val permissions = packageInfo.requestedPermissions?.toList() ?: emptyList()
                
                val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0 || 
                              (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                val isTrusted = isTrustedPublisher(app.packageName)
                val appLabel = pm.getApplicationLabel(app).toString()
                
                val (riskScore, category, reasonRes) = calculateRisk(permissions, appLabel, isSystem, isTrusted)
                
                result.add(
                    AppPermissionInfo(
                        appName = appLabel,
                        packageName = app.packageName,
                        icon = pm.getApplicationIcon(app),
                        permissions = permissions,
                        riskScore = riskScore,
                        riskCategory = category,
                        isSystem = isSystem,
                        isTrusted = isTrusted,
                        riskReasonRes = reasonRes
                    )
                )
            } catch (e: Exception) {
                // Skip if package info not found
            }
        }
        
        // Sorting: Dangerous User > Suspicious User > Safe User > System
        result.sortedWith(compareByDescending<AppPermissionInfo> { !it.isSystem }
            .thenByDescending { it.riskScore }
            .thenBy { it.appName })
    }

    private fun isTrustedPublisher(pkg: String): Boolean {
        return pkg.startsWith("com.google") || 
               pkg.startsWith("com.android") || 
               pkg.startsWith("com.miui") || 
               pkg.startsWith("com.xiaomi") || 
               pkg.startsWith("com.sec.android")
    }

    private fun calculateRisk(
        permissions: List<String>, 
        appName: String, 
        isSystem: Boolean, 
        isTrusted: Boolean
    ): Triple<Int, RiskCategory, Int> {
        var score = 0
        var reasonRes = 0
        
        val hasSms = permissions.contains("android.permission.READ_SMS")
        val hasContacts = permissions.contains("android.permission.READ_CONTACTS")
        val hasAudio = permissions.contains("android.permission.RECORD_AUDIO")
        val hasCamera = permissions.contains("android.permission.CAMERA")
        val hasLocation = permissions.contains("android.permission.ACCESS_FINE_LOCATION") || 
                         permissions.contains("android.permission.ACCESS_COARSE_LOCATION")

        // READ_SMS + READ_CONTACTS + RECORD_AUDIO together = 95 risk (pinjol suspect)
        if (hasSms && hasContacts && hasAudio) {
            score = 95
            reasonRes = R.string.app_scan_suspicious_comb
        } else if (hasCamera && hasLocation) {
            // CAMERA + LOCATION without need = 60
            score = 60
            reasonRes = R.string.app_scan_camera_location
        } else if (hasSms || hasContacts || hasAudio) {
            score = 30
            reasonRes = R.string.app_scan_sensitive_access
        }

        // Fix 1: Filter & Tag System Apps
        if (isSystem) {
            // Exception: if system app name contains calculator, flashlight, etc but requests READ_SMS -> risk = 80 SUSPICIOUS
            val lowerName = appName.lowercase()
            if ((lowerName.contains("kalkulator") || lowerName.contains("calculator") || 
                 lowerName.contains("senter") || lowerName.contains("flashlight")) && hasSms) {
                score = 80
                reasonRes = R.string.app_scan_system_sms
            } else {
                score = (score * 0.3).toInt()
                if (isTrusted) {
                    score = Math.min(score, 20)
                }
                reasonRes = if (isTrusted) R.string.app_scan_trusted_system else R.string.app_scan_system_app
            }
        }

        // Limit risk for system apps unless it's the edge case
        if (isSystem && score < 80) {
            score = Math.min(score, 40)
        }

        val category = when {
            score >= 90 -> RiskCategory.DANGEROUS
            score >= 50 -> RiskCategory.SUSPICIOUS
            else -> RiskCategory.CLEAN
        }

        return Triple(score, category, reasonRes)
    }
}
