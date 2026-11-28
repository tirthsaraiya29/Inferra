package com.inferra.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_repos")
data class CachedRepoEntity(
    @PrimaryKey val id: String,
    val name: String,
    val author: String,
    val repoTypeStr: String, // "MODEL", "DATASET", "SPACE"
    val description: String? = null,
    val downloads: Long = 0,
    val likes: Long = 0,
    val lastModified: String? = null,
    val createdAt: String? = null,
    val isPrivate: Boolean = false,
    val isGated: Boolean = false,
    val pipelineTag: String? = null,
    val libraryName: String? = null,
    val sdk: String? = null,
    val tagsCsv: String = "",
    val license: String? = null,
    val rawJson: String = "",
    val cachedAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val repoId: String,
    val repoTypeStr: String,
    val name: String,
    val author: String,
    val addedAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "download_jobs")
data class DownloadJobEntity(
    @PrimaryKey val id: String,
    val repoId: String,
    val repoTypeStr: String,
    val revision: String = "main",
    val filePath: String,
    val fileName: String,
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val statusStr: String = "QUEUED",
    val speedBytesPerSec: Long = 0L,
    val etaSeconds: Long = 0L,
    val expectedSha256: String? = null,
    val localPath: String? = null,
    val errorMessage: String? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "local_files")
data class LocalFileEntity(
    @PrimaryKey val id: String,
    val repoId: String,
    val repoTypeStr: String,
    val filePath: String,
    val fileName: String,
    val fileSizeBytes: Long = 0L,
    val localPath: String,
    val installedAtEpochMs: Long = System.currentTimeMillis(),
    val sha256Checksum: String? = null
)
