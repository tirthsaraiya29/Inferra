package com.inferra.data.download

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.inferra.data.local.AppDatabase
import com.inferra.data.local.LocalModelEntity
import com.inferra.data.repository.DownloadRepository
import com.inferra.domain.model.DownloadStatus
import java.io.File

class ModelDownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private companion object {
        const val TAG = "ModelDownloadWorker"
    }

    override suspend fun doWork(): Result {
        val jobId = inputData.getString("JOB_ID") ?: return Result.failure()
        val url = inputData.getString("DOWNLOAD_URL") ?: return Result.failure()
        val fileName = inputData.getString("FILE_NAME") ?: "model.gguf"
        val expectedSize = inputData.getLong("EXPECTED_SIZE", 0L)
        val modelId = inputData.getString("MODEL_ID") ?: "model"
        val modelName = inputData.getString("MODEL_NAME") ?: "Model"
        val quantType = inputData.getString("QUANT_TYPE") ?: "Q4_K_M"

        Log.i(TAG, "Starting background download worker for job '$jobId' ($fileName)...")

        val db = AppDatabase.getDatabase(applicationContext)
        val repo = DownloadRepository(db.downloadJobDao())
        val downloadManager = RealDownloadManager(applicationContext)

        val modelsDir = File(applicationContext.filesDir, "models")
        if (!modelsDir.exists()) modelsDir.mkdirs()
        val targetFile = File(modelsDir, fileName)

        val success = downloadManager.downloadFile(
            url = url,
            targetFile = targetFile,
            expectedSizeBytes = expectedSize
        ) { progress ->
            repo.updateJobProgressSync(
                jobId = jobId,
                downloadedBytes = progress.downloadedBytes,
                totalBytes = progress.totalBytes,
                speedBytesPerSec = progress.speedBytesPerSec,
                etaSeconds = progress.etaSeconds,
                statusStr = DownloadStatus.DOWNLOADING.name
            )
        }

        return if (success && targetFile.exists()) {
            val checksum = downloadManager.calculateSha256(targetFile)

            val localModel = LocalModelEntity(
                id = "local-${System.currentTimeMillis()}",
                modelId = modelId,
                modelName = modelName,
                fileName = fileName,
                filePath = targetFile.absolutePath,
                fileSizeBytes = targetFile.length(),
                quantType = quantType,
                format = "GGUF",
                sourceRepo = modelId,
                installedAtEpochMs = System.currentTimeMillis(),
                sha256Checksum = checksum
            )
            db.localModelDao().insertLocalModel(localModel)

            val job = repo.getJobById(jobId)
            if (job != null) {
                repo.updateJobStatus(job.copy(status = DownloadStatus.COMPLETED, downloadedBytes = targetFile.length()))
            }

            Log.i(TAG, "Worker completed download job '$jobId' successfully")
            Result.success()
        } else {
            val job = repo.getJobById(jobId)
            if (job != null) {
                repo.updateJobStatus(job.copy(status = DownloadStatus.FAILED, errorMessage = "Network or storage error"))
            }
            Result.failure()
        }
    }
}
