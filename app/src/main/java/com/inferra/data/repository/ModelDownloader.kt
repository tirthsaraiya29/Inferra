package com.inferra.data.repository

import android.content.Context
import android.util.Log
import com.inferra.domain.model.DownloadJob
import com.inferra.domain.model.DownloadStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

class ModelDownloader(
    private val context: Context,
    private val okHttpClient: OkHttpClient = OkHttpClient(),
) {
    private companion object {
        const val TAG = "ModelDownloader"
    }

    private val activeDownloadJobs = ConcurrentHashMap<String, Job>()
    private val _downloadProgressFlow = MutableStateFlow<Map<String, DownloadJob>>(emptyMap())
    val downloadProgressFlow: StateFlow<Map<String, DownloadJob>> = _downloadProgressFlow.asStateFlow()

    fun getDownloadDirectory(): File {
        val dir = File(context.getExternalFilesDir(null), "models")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    suspend fun startDownload(
        job: DownloadJob
    ): Unit = withContext(Dispatchers.IO) {
        val url = job.manifest.sourceUrl
        val fileName = job.manifest.fileName
        val targetFile = File(getDownloadDirectory(), fileName)

        val existingBytes = if (targetFile.exists()) targetFile.length() else 0L
        val requestBuilder = Request.Builder().url(url)

        if (existingBytes > 0L) {
            requestBuilder.addHeader("Range", "bytes=$existingBytes-")
            Log.d(TAG, "Resuming download for $fileName from byte $existingBytes")
        }

        val updatedJob = job.copy(
            downloadedBytes = existingBytes,
            status = DownloadStatus.DOWNLOADING
        )
        updateJobProgress(updatedJob)

        try {
            val response = okHttpClient.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful && (response.code != 206)) {
                updateJobProgress(
                    updatedJob.copy(
                        status = DownloadStatus.FAILED,
                        errorMessage = "Server returned HTTP ${response.code}: ${response.message}"
                    )
                )
                return@withContext
            }

            val body = response.body
            if (body == null) {
                updateJobProgress(
                    updatedJob.copy(
                        status = DownloadStatus.FAILED,
                        errorMessage = "Empty response body received from server"
                    )
                )
                return@withContext
            }

            val totalContentBytes = body.contentLength()
            val totalBytes = if (response.code == 206) existingBytes + totalContentBytes else totalContentBytes.coerceAtLeast(job.totalBytes)

            val outputStream = FileOutputStream(targetFile, response.code == 206)
            val inputStream = body.byteStream()

            val buffer = ByteArray(64 * 1024)
            var bytesRead: Int
            var downloaded = existingBytes
            var lastTime = System.currentTimeMillis()
            var bytesSinceLastTime = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                downloaded += bytesRead
                bytesSinceLastTime += bytesRead

                val currentTime = System.currentTimeMillis()
                val deltaTime = currentTime - lastTime

                if (deltaTime >= 500) {
                    val speed = (bytesSinceLastTime * 1000) / deltaTime
                    val remainingBytes = (totalBytes - downloaded).coerceAtLeast(0L)
                    val eta = if (speed > 0) remainingBytes / speed else 0L

                    updateJobProgress(
                        updatedJob.copy(
                            totalBytes = totalBytes,
                            downloadedBytes = downloaded,
                            speedBytesPerSec = speed,
                            etaSeconds = eta,
                            status = DownloadStatus.DOWNLOADING
                        )
                    )

                    lastTime = currentTime
                    bytesSinceLastTime = 0L
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            updateJobProgress(
                updatedJob.copy(
                    totalBytes = downloaded,
                    downloadedBytes = downloaded,
                    speedBytesPerSec = 0,
                    etaSeconds = 0,
                    status = DownloadStatus.COMPLETED
                )
            )
            Log.d(TAG, "Download completed for $fileName ($downloaded bytes)")

        } catch (e: Exception) {
            Log.e(TAG, "Download failed for $fileName: ${e.message}", e)
            updateJobProgress(
                updatedJob.copy(
                    status = DownloadStatus.FAILED,
                    errorMessage = e.message ?: "Network error during download"
                )
            )
        } finally {
            activeDownloadJobs.remove(job.id)
        }
    }

    fun cancelDownload(jobId: String) {
        val coroutineJob = activeDownloadJobs.remove(jobId)
        coroutineJob?.cancel()
        _downloadProgressFlow.update { map ->
            val existing = map[jobId]
            if (existing != null) {
                map + (jobId to existing.copy(status = DownloadStatus.FAILED, errorMessage = "Cancelled by user"))
            } else {
                map
            }
        }
    }

    private fun updateJobProgress(job: DownloadJob) {
        _downloadProgressFlow.update { it + (job.id to job) }
    }
}
