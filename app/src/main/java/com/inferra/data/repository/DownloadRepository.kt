package com.inferra.data.repository

import com.inferra.data.local.DownloadJobDao
import com.inferra.data.local.DownloadJobEntity
import com.inferra.data.local.SeedData
import com.inferra.domain.model.AiModel
import com.inferra.domain.model.DeviceTarget
import com.inferra.domain.model.DownloadJob
import com.inferra.domain.model.DownloadManifest
import com.inferra.domain.model.DownloadStatus
import com.inferra.domain.model.QuantizationInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DownloadRepository(
    private val downloadJobDao: DownloadJobDao
) {
    private val json = Json { ignoreUnknownKeys = true }

    val jobsFlow: Flow<List<DownloadJob>> = downloadJobDao.getAllJobs().map { entities ->
        if (entities.isEmpty()) {
            SeedData.initialDownloadJobs
        } else {
            entities.map { it.toDomain() }
        }
    }

    suspend fun createSendToPcJob(
        model: AiModel,
        quantization: QuantizationInfo,
        targetDevice: DeviceTarget
    ): DownloadJob = withContext(Dispatchers.IO) {
        val jobId = "job-${System.currentTimeMillis()}"
        val manifest = DownloadManifest(
            modelId = model.id,
            repository = model.id,
            exactRevision = "main",
            fileName = quantization.fileName,
            expectedSizeBytes = quantization.fileSizeBytes,
            sourceUrl = quantization.downloadUrl,
            targetDeviceId = targetDevice.id,
            requestedFormat = quantization.format,
            createdAtEpochMs = System.currentTimeMillis()
        )

        val job = DownloadJob(
            id = jobId,
            modelId = model.id,
            modelName = model.name,
            author = model.author,
            quantType = quantization.quantType,
            totalBytes = quantization.fileSizeBytes,
            downloadedBytes = 0,
            status = if (targetDevice.isOnline) DownloadStatus.QUEUED else DownloadStatus.WAITING_FOR_DEVICE,
            targetDeviceId = targetDevice.id,
            targetDeviceName = targetDevice.name,
            speedBytesPerSec = 0,
            etaSeconds = 0,
            manifest = manifest
        )

        downloadJobDao.insertJob(job.toEntity())
        job
    }

    suspend fun cancelJob(jobId: String) = withContext(Dispatchers.IO) {
        downloadJobDao.deleteJob(jobId)
    }

    private fun DownloadJobEntity.toDomain(): DownloadJob {
        val parsedManifest = try {
            json.decodeFromString<DownloadManifest>(manifestJson)
        } catch (_: Exception) {
            DownloadManifest(
                modelId = modelId,
                repository = modelId,
                exactRevision = "main",
                fileName = "model.gguf",
                expectedSizeBytes = totalBytes,
                sourceUrl = "https://huggingface.co",
                targetDeviceId = targetDeviceId,
                requestedFormat = "GGUF",
                createdAtEpochMs = System.currentTimeMillis()
            )
        }

        return DownloadJob(
            id = id,
            modelId = modelId,
            modelName = modelName,
            author = author,
            quantType = quantType,
            totalBytes = totalBytes,
            downloadedBytes = downloadedBytes,
            status = DownloadStatus.valueOf(statusStr),
            targetDeviceId = targetDeviceId,
            targetDeviceName = targetDeviceName,
            speedBytesPerSec = speedBytesPerSec,
            etaSeconds = etaSeconds,
            errorMessage = errorMessage,
            manifest = parsedManifest
        )
    }

    private fun DownloadJob.toEntity(): DownloadJobEntity {
        return DownloadJobEntity(
            id = id,
            modelId = modelId,
            modelName = modelName,
            author = author,
            quantType = quantType,
            totalBytes = totalBytes,
            downloadedBytes = downloadedBytes,
            statusStr = status.name,
            targetDeviceId = targetDeviceId,
            targetDeviceName = targetDeviceName,
            speedBytesPerSec = speedBytesPerSec,
            etaSeconds = etaSeconds,
            errorMessage = errorMessage,
            manifestJson = json.encodeToString(manifest)
        )
    }
}
