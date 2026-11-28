package com.inferra.data.download

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.inferra.data.local.AppDatabase
import com.inferra.data.local.LocalFileEntity
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
        val fileName = inputData.getString("FILE_NAME") ?: "file.bin"
        val expectedSize = inputData.getLong("EXPECTED_SIZE", 0L)
        val expectedSha256 = inputData.getString("EXPECTED_SHA256")
        val repoId = inputData.getString("REPO_ID") ?: "repo"
        val repoType = inputData.getString("REPO_TYPE") ?: "MODEL"

        Log.i(TAG, "Starting background download worker for job '$jobId' ($fileName)...")

        val db = AppDatabase.getDatabase(applicationContext)
        val repo = DownloadRepository(db.downloadJobDao(), applicationContext)
        val downloadManager = RealDownloadManager(applicationContext)

        val downloadsDir = File(applicationContext.filesDir, "hf_downloads")
        if (!downloadsDir.exists()) downloadsDir.mkdirs()
        val targetFile = File(downloadsDir, fileName)

        val success = downloadManager.downloadFile(
            url = url,
            targetFile = targetFile,
            expectedSizeBytes = expectedSize,
            expectedSha256 = expectedSha256,
            isCancelled = { isStopped || kotlinx.coroutines.runBlocking { repo.getJobById(jobId) == null } }
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

        return if (success && targetFile.exists() && (expectedSize == 0L || targetFile.length() == expectedSize)) {
            val checksum = downloadManager.calculateSha256(targetFile)

            val localFile = LocalFileEntity(
                id = "local-${System.currentTimeMillis()}",
                repoId = repoId,
                repoTypeStr = repoType,
                filePath = fileName,
                fileName = fileName,
                fileSizeBytes = targetFile.length(),
                localPath = targetFile.absolutePath,
                installedAtEpochMs = System.currentTimeMillis(),
                sha256Checksum = checksum
            )
            db.localFileDao().insertLocalFile(localFile)

            val job = repo.getJobById(jobId)
            if (job != null) {
                repo.updateJobStatus(job.copy(status = DownloadStatus.COMPLETED, downloadedBytes = targetFile.length(), localPath = targetFile.absolutePath))
            }

            Log.i(TAG, "Worker completed download job '$jobId' successfully")
            Result.success()
        } else {
            val job = repo.getJobById(jobId)
            if (job != null) {
                repo.updateJobStatus(job.copy(status = DownloadStatus.FAILED, errorMessage = "Network, hash verification, or storage error"))
            }
            Result.failure()
        }
    }
}
