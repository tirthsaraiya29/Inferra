package com.inferra.data.repository

import android.content.Context
import com.inferra.core.security.SecureTokenStorage
import com.inferra.data.local.BookmarkDao
import com.inferra.data.local.BookmarkEntity
import com.inferra.data.local.CachedRepoDao
import com.inferra.data.network.HuggingFaceApi
import com.inferra.data.network.HuggingFaceClient
import com.inferra.data.network.NetworkToDomainMapper
import com.inferra.domain.model.AuthState
import com.inferra.domain.model.Bookmark
import com.inferra.domain.model.FileTreeItem
import com.inferra.domain.model.RepoDetail
import com.inferra.domain.model.RepoItem
import com.inferra.domain.model.RepoType
import com.inferra.domain.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class HubRepository(
    private val api: HuggingFaceApi,
    private val cachedRepoDao: CachedRepoDao,
    private val bookmarkDao: BookmarkDao,
    context: Context
) {
    private val secureTokenStorage = SecureTokenStorage(context)
    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        HuggingFaceClient.setTokenProvider { secureTokenStorage.getToken() }
        val storedToken = secureTokenStorage.getToken()
        if (!storedToken.isNullOrBlank()) {
            _authState.value = AuthState(token = storedToken, isAuthenticated = true)
        }
    }

    fun getStoredToken(): String? = secureTokenStorage.getToken()

    suspend fun verifyAndSaveToken(token: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        if (token.isBlank()) {
            secureTokenStorage.clearToken()
            _authState.value = AuthState()
            return@withContext Result.failure(Exception("Token cleared."))
        }

        try {
            _authState.value = _authState.value.copy(isLoading = true, errorMessage = null)
            val res = api.whoami("Bearer ${token.trim()}")
            val profile = NetworkToDomainMapper.mapWhoamiToUserProfile(res)
            secureTokenStorage.saveToken(token.trim())
            _authState.value = AuthState(token = token.trim(), user = profile, isAuthenticated = true, isLoading = false)
            Result.success(profile)
        } catch (e: Exception) {
            val userMsg = "Invalid Hugging Face token or network error: ${e.message}"
            _authState.value = AuthState(token = null, user = null, isAuthenticated = false, isLoading = false, errorMessage = userMsg)
            Result.failure(Exception(userMsg, e))
        }
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        secureTokenStorage.clearToken()
        _authState.value = AuthState()
    }

    // --- Search / Discovery Methods ---
    suspend fun searchRepos(
        query: String = "",
        type: RepoType = RepoType.MODEL,
        author: String? = null,
        pipelineTag: String? = null,
        filter: String? = null,
        sort: String = "downloads",
        page: Int = 1,
        limit: Int = 30
    ): List<RepoItem> = withContext(Dispatchers.IO) {
        val token = getStoredToken()?.let { "Bearer $it" }
        when (type) {
            RepoType.MODEL -> {
                val dtos = api.getModels(
                    token = token,
                    search = query.ifBlank { null },
                    author = author,
                    pipelineTag = pipelineTag,
                    filter = filter,
                    sort = sort,
                    page = page,
                    limit = limit
                )
                dtos.map { NetworkToDomainMapper.mapModelToRepoItem(it) }
            }
            RepoType.DATASET -> {
                val dtos = api.getDatasets(
                    token = token,
                    search = query.ifBlank { null },
                    author = author,
                    filter = filter,
                    sort = sort,
                    page = page,
                    limit = limit
                )
                dtos.map { NetworkToDomainMapper.mapDatasetToRepoItem(it) }
            }
            RepoType.SPACE -> {
                val dtos = api.getSpaces(
                    token = token,
                    search = query.ifBlank { null },
                    author = author,
                    filter = filter,
                    sort = sort,
                    page = page,
                    limit = limit
                )
                dtos.map { NetworkToDomainMapper.mapSpaceToRepoItem(it) }
            }
        }
    }

    // --- Detail & File Browser Methods ---
    suspend fun getRepoDetail(
        repoId: String,
        type: RepoType,
        revision: String = "main"
    ): RepoDetail = withContext(Dispatchers.IO) {
        val token = getStoredToken()?.let { "Bearer $it" }
        val item: RepoItem
        val siblings: List<FileTreeItem>

        when (type) {
            RepoType.MODEL -> {
                val dto = api.getModelDetail(token, repoId)
                item = NetworkToDomainMapper.mapModelToRepoItem(dto)
                siblings = dto.siblings?.map { FileTreeItem(path = it.filename ?: "", size = it.size) } ?: emptyList()
            }
            RepoType.DATASET -> {
                val dto = api.getDatasetDetail(token, repoId)
                item = NetworkToDomainMapper.mapDatasetToRepoItem(dto)
                siblings = dto.siblings?.map { FileTreeItem(path = it.filename ?: "", size = it.size) } ?: emptyList()
            }
            RepoType.SPACE -> {
                val dto = api.getSpaceDetail(token, repoId)
                item = NetworkToDomainMapper.mapSpaceToRepoItem(dto)
                siblings = dto.siblings?.map { FileTreeItem(path = it.filename ?: "", size = it.size) } ?: emptyList()
            }
        }

        // Fetch README
        val readmeText = try {
            val prefix = when (type) {
                RepoType.MODEL -> ""
                RepoType.DATASET -> "datasets/"
                RepoType.SPACE -> "spaces/"
            }
            api.getRawFileContent("https://huggingface.co/${prefix}${repoId}/raw/${revision}/README.md", token)
        } catch (_: Exception) {
            try {
                val prefix = when (type) {
                    RepoType.MODEL -> ""
                    RepoType.DATASET -> "datasets/"
                    RepoType.SPACE -> "spaces/"
                }
                api.getRawFileContent("https://huggingface.co/${prefix}${repoId}/raw/${revision}/readme.md", token)
            } catch (_: Exception) {
                null
            }
        }

        RepoDetail(
            repo = item,
            siblings = siblings,
            readmeContent = readmeText,
            selectedRevision = revision
        )
    }

    suspend fun getFileTree(
        repoId: String,
        type: RepoType,
        revision: String = "main",
        path: String = ""
    ): List<FileTreeItem> = withContext(Dispatchers.IO) {
        val token = getStoredToken()?.let { "Bearer $it" }
        val dtos = when (type) {
            RepoType.MODEL -> if (path.isBlank()) api.getModelTree(token, repoId, revision) else api.getModelTreePath(token, repoId, revision, path)
            RepoType.DATASET -> if (path.isBlank()) api.getDatasetTree(token, repoId, revision) else api.getDatasetTreePath(token, repoId, revision, path)
            RepoType.SPACE -> if (path.isBlank()) api.getSpaceTree(token, repoId, revision) else api.getSpaceTreePath(token, repoId, revision, path)
        }
        dtos.map { NetworkToDomainMapper.mapFileTreeItem(it) }
    }

    suspend fun getRawFileContent(
        repoId: String,
        type: RepoType,
        filePath: String,
        revision: String = "main"
    ): String = withContext(Dispatchers.IO) {
        val token = getStoredToken()?.let { "Bearer $it" }
        val prefix = when (type) {
            RepoType.MODEL -> ""
            RepoType.DATASET -> "datasets/"
            RepoType.SPACE -> "spaces/"
        }
        api.getRawFileContent("https://huggingface.co/${prefix}${repoId}/raw/${revision}/${filePath}", token)
    }

    suspend fun getUserProfile(username: String): UserProfile = withContext(Dispatchers.IO) {
        val dto = api.getUserOverview(username)
        UserProfile(
            username = dto.name,
            fullname = dto.fullname,
            avatarUrl = dto.avatarUrl,
            isOrg = dto.type == "org",
            bio = dto.description,
            isPro = dto.isPro
        )
    }

    // --- Bookmarks ---
    val bookmarksFlow: Flow<List<Bookmark>> = bookmarkDao.getAllBookmarks().map { list ->
        list.map { Bookmark(it.repoId, RepoType.valueOf(it.repoTypeStr), it.name, it.author, it.addedAtEpochMs) }
    }

    fun isBookmarkedFlow(repoId: String): Flow<Boolean> = bookmarkDao.isBookmarked(repoId)

    suspend fun toggleBookmark(repo: RepoItem) = withContext(Dispatchers.IO) {
        val exists = bookmarkDao.isBookmarkedSync(repo.id)
        if (exists) {
            bookmarkDao.removeBookmark(repo.id)
        } else {
            bookmarkDao.addBookmark(
                BookmarkEntity(
                    repoId = repo.id,
                    repoTypeStr = repo.type.name,
                    name = repo.name,
                    author = repo.author,
                    addedAtEpochMs = System.currentTimeMillis()
                )
            )
        }
    }
}
