package com.inferra.domain.model

import kotlinx.serialization.Serializable

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    WAITING_FOR_DEVICE
}

@Serializable
data class DownloadManifest(
    val modelId: String,
    val repository: String,
    val exactRevision: String,
    val fileName: String,
    val expectedSizeBytes: Long,
    val checksumSha256: String? = null,
    val sourceUrl: String,
    val targetDeviceId: String,
    val requestedFormat: String,
    val createdAtEpochMs: Long
)

data class DownloadJob(
    val id: String,
    val modelId: String,
    val modelName: String,
    val author: String,
    val quantType: String,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val status: DownloadStatus,
    val targetDeviceId: String,
    val targetDeviceName: String,
    val speedBytesPerSec: Long = 0,
    val etaSeconds: Long = 0,
    val errorMessage: String? = null,
    val manifest: DownloadManifest
)
