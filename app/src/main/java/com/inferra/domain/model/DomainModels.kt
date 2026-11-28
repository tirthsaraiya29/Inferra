package com.inferra.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

// ==============================================================================
// UI State & Common Enums
// ==============================================================================

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Empty(val message: String = "No results found.") : UiState<Nothing>
    data class Error(
        val userMessage: String,
        val throwable: Throwable? = null,
        val canRetry: Boolean = true
    ) : UiState<Nothing>
}

enum class RepoType(val apiPath: String, val displayName: String) {
    MODEL("models", "Models"),
    DATASET("datasets", "Datasets"),
    SPACE("spaces", "Spaces")
}

// ==============================================================================
// Hugging Face Repository Models
// ==============================================================================

@Immutable
@Serializable
data class RepoItem(
    val id: String,
    val name: String,
    val author: String,
    val type: RepoType,
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
    val tags: List<String> = emptyList(),
    val license: String? = null,
    val avatarUrl: String? = null
)

@Immutable
@Serializable
data class LfsInfo(
    val oid: String? = null,
    val size: Long? = null,
    val pointerSize: Long? = null
)

@Immutable
@Serializable
data class FileTreeItem(
    val path: String,
    val type: String = "file", // "file" or "directory"
    val size: Long? = null,
    val lfs: LfsInfo? = null,
    val sha256: String? = null
)

@Immutable
@Serializable
data class RepoDetail(
    val repo: RepoItem,
    val siblings: List<FileTreeItem> = emptyList(),
    val configSummary: Map<String, String> = emptyMap(),
    val readmeContent: String? = null,
    val selectedRevision: String = "main"
)

// ==============================================================================
// Users and Organizations
// ==============================================================================

@Immutable
@Serializable
data class UserProfile(
    val username: String,
    val fullname: String? = null,
    val avatarUrl: String? = null,
    val isOrg: Boolean = false,
    val bio: String? = null,
    val isPro: Boolean = false,
    val email: String? = null
)

@Immutable
@Serializable
data class AuthState(
    val token: String? = null,
    val user: UserProfile? = null,
    val isAuthenticated: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

// ==============================================================================
// Bookmarks & Downloads
// ==============================================================================

@Immutable
@Serializable
data class Bookmark(
    val repoId: String,
    val repoType: RepoType,
    val name: String,
    val author: String,
    val addedAtEpochMs: Long = System.currentTimeMillis()
)

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED
}

@Immutable
@Serializable
data class DownloadJob(
    val id: String,
    val repoId: String,
    val repoType: RepoType,
    val revision: String = "main",
    val filePath: String,
    val fileName: String,
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val speedBytesPerSec: Long = 0L,
    val etaSeconds: Long = 0L,
    val expectedSha256: String? = null,
    val localPath: String? = null,
    val errorMessage: String? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)
