package com.inferra.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.inferra.data.local.DownloadJobDao
import com.inferra.data.local.DownloadJobEntity
import com.inferra.domain.model.DownloadJob
import com.inferra.domain.model.DownloadStatus
import com.inferra.domain.model.RepoItem
import com.inferra.domain.model.RepoType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

// ==============================================================================
// Download Jobs Repository
// ==============================================================================

class DownloadRepository(
    private val downloadJobDao: DownloadJobDao,
    private val context: Context? = null
) {
    val jobsFlow: Flow<List<DownloadJob>> = downloadJobDao.getAllJobs().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun createDownloadJob(
        context: Context,
        repo: RepoItem,
        filePath: String,
        fileName: String,
        totalBytes: Long,
        expectedSha256: String? = null,
        revision: String = "main"
    ): DownloadJob = withContext(Dispatchers.IO) {
        val jobId = "job-${System.currentTimeMillis()}"

        val prefix = when (repo.type) {
            RepoType.MODEL -> ""
            RepoType.DATASET -> "datasets/"
            RepoType.SPACE -> "spaces/"
        }
        val downloadUrl = "https://huggingface.co/${prefix}${repo.id}/resolve/${revision}/${filePath}"

        val job = DownloadJob(
            id = jobId,
            repoId = repo.id,
            repoType = repo.type,
            revision = revision,
            filePath = filePath,
            fileName = fileName,
            totalBytes = totalBytes,
            downloadedBytes = 0L,
            status = DownloadStatus.QUEUED,
            speedBytesPerSec = 0L,
            etaSeconds = 0L,
            expectedSha256 = expectedSha256,
            createdAtEpochMs = System.currentTimeMillis()
        )

        downloadJobDao.insertJob(job.toEntity())

        // Enqueue WorkManager background worker
        try {
            val workData = androidx.work.workDataOf(
                "JOB_ID" to jobId,
                "DOWNLOAD_URL" to downloadUrl,
                "FILE_NAME" to fileName,
                "EXPECTED_SIZE" to totalBytes,
                "EXPECTED_SHA256" to (expectedSha256 ?: ""),
                "REPO_ID" to repo.id,
                "REPO_TYPE" to repo.type.name
            )
            val request = androidx.work.OneTimeWorkRequestBuilder<com.inferra.data.download.ModelDownloadWorker>()
                .setInputData(workData)
                .addTag(jobId)
                .build()
            androidx.work.WorkManager.getInstance(context).enqueue(request)
        } catch (e: Exception) {
            android.util.Log.e("DownloadRepository", "Failed to enqueue WorkManager job: ${e.message}")
        }

        job
    }

    suspend fun getJobById(jobId: String): DownloadJob? = withContext(Dispatchers.IO) {
        val entity = downloadJobDao.getJobById(jobId)
        entity?.toDomain()
    }

    suspend fun updateJobStatus(job: DownloadJob) = withContext(Dispatchers.IO) {
        downloadJobDao.insertJob(job.toEntity())
    }

    fun updateJobProgressSync(
        jobId: String,
        downloadedBytes: Long,
        totalBytes: Long,
        speedBytesPerSec: Long,
        etaSeconds: Long,
        statusStr: String
    ) {
        downloadJobDao.updateProgressSync(jobId, downloadedBytes, totalBytes, speedBytesPerSec, etaSeconds, statusStr)
    }

    suspend fun cancelJob(jobId: String) = withContext(Dispatchers.IO) {
        downloadJobDao.deleteJob(jobId)
        context?.let { ctx ->
            try {
                androidx.work.WorkManager.getInstance(ctx).cancelAllWorkByTag(jobId)
            } catch (e: Exception) {
                android.util.Log.e("DownloadRepository", "Failed to cancel WorkManager job '$jobId': ${e.message}")
            }
        }
    }

    private fun DownloadJobEntity.toDomain(): DownloadJob {
        return DownloadJob(
            id = id,
            repoId = repoId,
            repoType = try { RepoType.valueOf(repoTypeStr) } catch (_: Exception) { RepoType.MODEL },
            revision = revision,
            filePath = filePath,
            fileName = fileName,
            totalBytes = totalBytes,
            downloadedBytes = downloadedBytes,
            status = try { DownloadStatus.valueOf(statusStr) } catch (_: Exception) { DownloadStatus.QUEUED },
            speedBytesPerSec = speedBytesPerSec,
            etaSeconds = etaSeconds,
            expectedSha256 = expectedSha256,
            localPath = localPath,
            errorMessage = errorMessage,
            createdAtEpochMs = createdAtEpochMs
        )
    }

    private fun DownloadJob.toEntity(): DownloadJobEntity {
        return DownloadJobEntity(
            id = id,
            repoId = repoId,
            repoTypeStr = repoType.name,
            revision = revision,
            filePath = filePath,
            fileName = fileName,
            totalBytes = totalBytes,
            downloadedBytes = downloadedBytes,
            statusStr = status.name,
            speedBytesPerSec = speedBytesPerSec,
            etaSeconds = etaSeconds,
            expectedSha256 = expectedSha256,
            localPath = localPath,
            errorMessage = errorMessage,
            createdAtEpochMs = createdAtEpochMs
        )
    }
}

// ==============================================================================
// User Settings Repository
// ==============================================================================

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

data class UserSettings(
    val isDarkMode: Boolean = true,
    val autoRefreshData: Boolean = true,
)

class SettingsRepository(private val context: Context) {

    private companion object {
        val KEY_IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
    }

    val userSettingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            isDarkMode = prefs[KEY_IS_DARK_MODE] ?: true
        )
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_DARK_MODE] = enabled
        }
    }
}
