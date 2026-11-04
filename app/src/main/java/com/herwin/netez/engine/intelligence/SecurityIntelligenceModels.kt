package com.herwin.netez.engine.intelligence

import androidx.annotation.Keep

enum class TargetType {
    DOMAIN,
    URL,
    EMAIL,
    INVALID
}

enum class FindingSeverity {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW,
    INFO
}

enum class CheckCategoryType {
    DOMAIN_REPUTATION,
    BLACKLIST,
    SSL_TLS,
    DNS_SECURITY,
    PHISHING,
    EXPOSURE,
    BREACH_LEAK,
    MAIL_SECURITY
}

enum class CheckExecutionStatus {
    WAITING,
    RUNNING,
    SUCCESS,
    WARNING,
    CRITICAL,
    UNAVAILABLE
}

@Keep
data class ParsedTarget(
    val rawInput: String,
    val type: TargetType,
    val host: String = "",
    val baseDomain: String = "",
    val scheme: String = "",
    val urlPath: String = "",
    val emailUser: String = "",
    val emailDomain: String = "",
    val isValid: Boolean = true,
    val errorMessage: String? = null
)

@Keep
data class CategoryCheckStatus(
    val type: CheckCategoryType,
    val title: String,
    val status: CheckExecutionStatus,
    val summary: String,
    val sourceProvider: String = "",
    val details: Map<String, String> = emptyMap()
)

@Keep
data class SecurityFinding(
    val id: String,
    val category: CheckCategoryType,
    val severity: FindingSeverity,
    val title: String,
    val currentState: String = "",
    val riskImpact: String,
    val recommendation: String,
    val sourceProvider: String = "",
    val technicalDetails: Map<String, String> = emptyMap()
)

@Keep
data class PassedCheck(
    val id: String,
    val category: CheckCategoryType,
    val title: String,
    val detail: String = "",
    val sourceProvider: String = ""
)

@Keep
data class SecurityIntelligenceResult(
    val target: ParsedTarget,
    val timestamp: Long = System.currentTimeMillis(),
    val overallStatus: String = "GOOD", // e.g. "GOOD", "MODERATE RISK", "HIGH RISK", "CRITICAL RISK"
    val checksCompleted: Int = 0,
    val warningCount: Int = 0,
    val criticalCount: Int = 0,
    val categoryStatuses: List<CategoryCheckStatus> = emptyList(),
    val findings: List<SecurityFinding> = emptyList(),
    val passedChecks: List<PassedCheck> = emptyList(),
    val technicalDetails: Map<String, Any> = emptyMap(),
    val scanDurationMs: Long = 0L,
    val isPartial: Boolean = false
)
