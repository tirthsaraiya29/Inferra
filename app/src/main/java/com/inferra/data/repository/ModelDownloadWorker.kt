package com.inferra.data.repository

import android.Manifest
import android.R
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.inferra.data.local.AppDatabase
import com.inferra.data.local.DownloadJobEntity
import com.inferra.data.local.LocalModelEntity
import com.inferra.domain.model.DownloadJob
import com.inferra.domain.model.DownloadManifest
import com.inferra.domain.model.DownloadStatus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest

class ModelDownloadWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val CHANNEL_ID = "inferra_downloads_channel"
        const val NOTIFICATION_ID = 1001

        const val KEY_JOB_ID = "job_id"
        const val KEY_MODEL_ID = "model_id"
        const val KEY_MODEL_NAME = "model_name"
        const val KEY_AUTHOR = "author"
        const val KEY_FILE_NAME = "file_name"
        const val KEY_URL = "url"
        const val KEY_QUANT_TYPE = "quant_type"
        const val KEY_FORMAT = "format"
        const val KEY_SOURCE_REPO = "source_repo"
        const val KEY_EXPECTED_BYTES = "expected_bytes"
    }

    private val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun doWork(): Result {
        val jobId = inputData.getString(KEY_JOB_ID) ?: return Result.failure()
        val modelId = inputData.getString(KEY_MODEL_ID) ?: return Result.failure()
        val modelName = inputData.getString(KEY_MODEL_NAME) ?: "Model"
        val author = inputData.getString(KEY_AUTHOR) ?: "Unknown"
        val fileName = inputData.getString(KEY_FILE_NAME) ?: "model.gguf"
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val quantType = inputData.getString(KEY_QUANT_TYPE) ?: "GGUF"
        val format = inputData.getString(KEY_FORMAT) ?: "GGUF"
        val sourceRepo = inputData.getString(KEY_SOURCE_REPO) ?: modelId
        val expectedBytes = inputData.getLong(KEY_EXPECTED_BYTES, 0L)

        createNotificationChannel()
        try {
            val foregroundInfo = createForegroundInfo("Downloading $fileName")
            setForeground(foregroundInfo)
        } catch (_: Exception) {
            // Foreground info set fallback
        }

        val db = AppDatabase.getDatabase(appContext)
        val downloader = ModelDownloader(appContext)
        val targetFile = File(downloader.getDownloadDirectory(), fileName)

        val manifest = DownloadManifest(
            modelId = modelId,
            repository = sourceRepo,
            exactRevision = "main",
            fileName = fileName,
            expectedSizeBytes = expectedBytes,
            sourceUrl = url,
            targetDeviceId = "local-device",
            requestedFormat = format,
            createdAtEpochMs = System.currentTimeMillis()
        )

        // Initial job update in Room
        db.downloadJobDao().insertJob(
            DownloadJobEntity(
                id = jobId,
                modelId = modelId,
                modelName = modelName,
                author = author,
                quantType = quantType,
                totalBytes = expectedBytes,
                downloadedBytes = 0,
                statusStr = DownloadStatus.DOWNLOADING.name,
                targetDeviceId = "local-device",
                targetDeviceName = "This Device",
                speedBytesPerSec = 0,
                etaSeconds = 0,
                manifestJson = json.encodeToString(manifest)
            )
        )

        val job = DownloadJob(
            id = jobId,
            modelId = modelId,
            modelName = modelName,
            author = author,
            quantType = quantType,
            totalBytes = expectedBytes,
            downloadedBytes = 0,
            status = DownloadStatus.DOWNLOADING,
            targetDeviceId = "local-device",
            targetDeviceName = "This Device",
            manifest = manifest
        )

        downloader.startDownload(job)

        val finalProgress = downloader.downloadProgressFlow.value[jobId]
        if (finalProgress != null && finalProgress.status == DownloadStatus.COMPLETED && targetFile.exists()) {
            val computedSha256 = try { calculateFileSha256(targetFile) } catch (_: Exception) { null }
            val expectedSha256 = manifest.checksumSha256

            if (expectedSha256 != null && computedSha256 != null && !computedSha256.equals(expectedSha256, ignoreCase = true)) {
                targetFile.delete()
                db.downloadJobDao().insertJob(
                    DownloadJobEntity(
                        id = jobId,
                        modelId = modelId,
                        modelName = modelName,
                        author = author,
                        quantType = quantType,
                        totalBytes = expectedBytes,
                        downloadedBytes = 0,
                        statusStr = DownloadStatus.FAILED.name,
                        targetDeviceId = "local-device",
                        targetDeviceName = "This Device",
                        speedBytesPerSec = 0,
                        etaSeconds = 0,
                        errorMessage = "Download corrupted: SHA-256 checksum mismatch",
                        manifestJson = json.encodeToString(manifest)
                    )
                )
                updateNotification("Download Corrupted: $fileName")
                return Result.failure(workDataOf("error" to "Checksum mismatch"))
            }

            // Update Job Status
            db.downloadJobDao().insertJob(
                DownloadJobEntity(
                    id = jobId,
                    modelId = modelId,
                    modelName = modelName,
                    author = author,
                    quantType = quantType,
                    totalBytes = finalProgress.totalBytes,
                    downloadedBytes = finalProgress.downloadedBytes,
                    statusStr = DownloadStatus.COMPLETED.name,
                    targetDeviceId = "local-device",
                    targetDeviceName = "This Device",
                    speedBytesPerSec = 0,
                    etaSeconds = 0,
                    manifestJson = json.encodeToString(manifest.copy(checksumSha256 = computedSha256))
                )
            )

            // Insert into LocalModelDao database registry
            db.localModelDao().insertLocalModel(
                LocalModelEntity(
                    id = "local-$modelId-$quantType",
                    modelId = modelId,
                    modelName = modelName,
                    fileName = fileName,
                    filePath = targetFile.absolutePath,
                    fileSizeBytes = finalProgress.downloadedBytes,
                    quantType = quantType,
                    format = format,
                    sourceRepo = sourceRepo,
                    installedAtEpochMs = System.currentTimeMillis(),
                    sha256Checksum = computedSha256
                )
            )

            updateNotification("Download Complete: $fileName")
            return Result.success(workDataOf("file_path" to targetFile.absolutePath))
        } else {
            val error = finalProgress?.errorMessage ?: "Download interrupted"
            db.downloadJobDao().insertJob(
                DownloadJobEntity(
                    id = jobId,
                    modelId = modelId,
                    modelName = modelName,
                    author = author,
                    quantType = quantType,
                    totalBytes = expectedBytes,
                    downloadedBytes = finalProgress?.downloadedBytes ?: 0L,
                    statusStr = DownloadStatus.FAILED.name,
                    targetDeviceId = "local-device",
                    targetDeviceName = "This Device",
                    speedBytesPerSec = 0,
                    etaSeconds = 0,
                    errorMessage = error,
                    manifestJson = json.encodeToString(manifest)
                )
            )
            updateNotification("Download Failed: $fileName")
            return Result.failure(workDataOf("error" to error))
        }
    }

    private fun calculateFileSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buffer = ByteArray(64 * 1024)
            var bytesRead: Int
            while (stream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Inferra Model Downloads",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows model file download progress"
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun createForegroundInfo(title: String): ForegroundInfo {
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setContentTitle(title)
            .setSmallIcon(R.drawable.stat_sys_download)
            .setProgress(100, 0, true)
            .setOngoing(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    @SuppressLint("MissingPermission")
    private fun updateNotification(title: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        try {
            val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setContentTitle(title)
                .setSmallIcon(R.drawable.stat_sys_download_done)
                .setProgress(100, 100, false)
                .setOngoing(false)
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (_: Exception) {
            // Permission POST_NOTIFICATIONS optional fallback
        }
    }
}
