package com.herwin.netez.device

data class DeviceCheckItem(
    val id: String,
    val category: String,
    val name: String,
    val status: DeviceCheckStatus,
    val score: Int,
    val maxScore: Int,
    val detail: String,
    val recommendation: String,
    val evidence: String?,
    val latencyMs: Long,
    val action: DeviceCheckAction?
)
