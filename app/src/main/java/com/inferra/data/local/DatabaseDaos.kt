package com.inferra.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedRepoDao {
    @Query("SELECT * FROM cached_repos WHERE repoTypeStr = :repoTypeStr ORDER BY downloads DESC")
    fun getReposByType(repoTypeStr: String): Flow<List<CachedRepoEntity>>

    @Query("SELECT * FROM cached_repos WHERE id = :id LIMIT 1")
    suspend fun getRepoById(id: String): CachedRepoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepos(repos: List<CachedRepoEntity>)

    @Query("DELETE FROM cached_repos WHERE repoTypeStr = :repoTypeStr")
    suspend fun clearReposByType(repoTypeStr: String)
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY addedAtEpochMs DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE repoId = :repoId)")
    fun isBookmarked(repoId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE repoId = :repoId)")
    suspend fun isBookmarkedSync(repoId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE repoId = :repoId")
    suspend fun removeBookmark(repoId: String)
}

@Dao
interface DownloadJobDao {
    @Query("SELECT * FROM download_jobs ORDER BY createdAtEpochMs DESC")
    fun getAllJobs(): Flow<List<DownloadJobEntity>>

    @Query("SELECT * FROM download_jobs WHERE id = :id LIMIT 1")
    suspend fun getJobById(id: String): DownloadJobEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: DownloadJobEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertJobSync(job: DownloadJobEntity)

    @Query("UPDATE download_jobs SET downloadedBytes = :downloadedBytes, totalBytes = :totalBytes, speedBytesPerSec = :speedBytesPerSec, etaSeconds = :etaSeconds, statusStr = :statusStr WHERE id = :jobId")
    fun updateProgressSync(
        jobId: String,
        downloadedBytes: Long,
        totalBytes: Long,
        speedBytesPerSec: Long,
        etaSeconds: Long,
        statusStr: String
    )

    @Query("DELETE FROM download_jobs WHERE id = :jobId")
    suspend fun deleteJob(jobId: String)
}

@Dao
interface LocalFileDao {
    @Query("SELECT * FROM local_files ORDER BY installedAtEpochMs DESC")
    fun getAllLocalFiles(): Flow<List<LocalFileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocalFile(file: LocalFileEntity)

    @Query("DELETE FROM local_files WHERE id = :id")
    suspend fun deleteLocalFile(id: String)
}
