package com.inferra.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "models")
data class ModelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val author: String,
    val description: String,
    val architecture: String,
    val totalParamsBillion: Float,
    val activeParamsBillion: Float,
    val isMoe: Boolean,
    val contextLengthTokens: Int,
    val licenseName: String,
    val downloadsCount: Long,
    val likesCount: Long,
    val updatedAt: String,
    val isFeatured: Boolean,
    val isTrending: Boolean,
    val isNew: Boolean,
    val repoUrl: String,
    val rawJson: String // Serialized AiModel JSON for complete fidelity
)

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val modelId: String,
    val addedAtEpochMs: Long,
    val notes: String = "",
    val notifyOnUpdate: Boolean = true
)

@Entity(tableName = "download_jobs")
data class DownloadJobEntity(
    @PrimaryKey val id: String,
    val modelId: String,
    val modelName: String,
    val author: String,
    val quantType: String,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val statusStr: String,
    val targetDeviceId: String,
    val targetDeviceName: String,
    val speedBytesPerSec: Long,
    val etaSeconds: Long,
    val errorMessage: String? = null,
    val manifestJson: String
)

@Entity(tableName = "hardware_profiles")
data class HardwareProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val deviceTypeStr: String,
    val cpuName: String,
    val gpuName: String,
    val vramGb: Float,
    val ramGb: Float,
    val osName: String,
    val preferredRuntime: String,
    val availableStorageGb: Float,
    val isLocalDevice: Boolean
)

@Entity(tableName = "device_targets")
data class DeviceTargetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val ipAddress: String,
    val port: Int,
    val osName: String,
    val isOnline: Boolean,
    val lastSeenEpochMs: Long,
    val isPaired: Boolean,
    val runtimesCsv: String
)

@Entity(tableName = "local_models")
data class LocalModelEntity(
    @PrimaryKey val id: String,
    val modelId: String,
    val modelName: String,
    val fileName: String,
    val filePath: String,
    val fileSizeBytes: Long,
    val quantType: String,
    val format: String,
    val sourceRepo: String,
    val installedAtEpochMs: Long,
    val sha256Checksum: String? = null
)
