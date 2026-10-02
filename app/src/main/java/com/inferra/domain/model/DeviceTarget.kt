package com.inferra.domain.model

data class DeviceTarget(
    val id: String,
    val name: String,                   // e.g. "Tirth-PC Workstation"
    val ipAddress: String,              // e.g. "192.168.1.105"
    val port: Int = 8443,
    val osName: String,                 // e.g. "Windows 11 Pro"
    val isOnline: Boolean,
    val lastSeenEpochMs: Long,
    val isPaired: Boolean,
    val supportedRuntimes: List<String>,
)
