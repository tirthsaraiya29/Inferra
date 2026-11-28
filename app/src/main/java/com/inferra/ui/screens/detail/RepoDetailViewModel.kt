package com.inferra.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inferra.data.repository.DownloadRepository
import com.inferra.data.repository.HubRepository
import com.inferra.domain.model.FileTreeItem
import com.inferra.domain.model.RepoDetail
import com.inferra.domain.model.RepoItem
import com.inferra.domain.model.RepoType
import com.inferra.domain.model.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class RepoDetailUiState(
    val detailState: UiState<RepoDetail> = UiState.Loading,
    val isBookmarked: Boolean = false,
    val selectedTab: Int = 0, // 0: README/Overview, 1: Files & Tree, 2: Metadata
    val currentTreePath: String = "",
    val treeItems: List<FileTreeItem> = emptyList(),
    val isTreeLoading: Boolean = false,
    val selectedFilePreview: Pair<String, String>? = null
)

class RepoDetailViewModel(
    val repoId: String,
    val repoType: RepoType,
    private val hubRepository: HubRepository,
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RepoDetailUiState())
    val uiState: StateFlow<RepoDetailUiState> = _uiState.asStateFlow()

    init {
        loadDetail()
        observeBookmarkStatus()
    }

    fun loadDetail(revision: String = "main") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(detailState = UiState.Loading)
            try {
                val detail = hubRepository.getRepoDetail(repoId, repoType, revision)
                _uiState.value = _uiState.value.copy(detailState = UiState.Success(detail))
                loadTreePath(revision = revision, path = "")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    detailState = UiState.Error("Failed to load repository detail: ${e.message}", e)
                )
            }
        }
    }

    private fun observeBookmarkStatus() {
        viewModelScope.launch {
            hubRepository.isBookmarkedFlow(repoId).collectLatest { bookmarked ->
                _uiState.value = _uiState.value.copy(isBookmarked = bookmarked)
            }
        }
    }

    fun toggleBookmark(repoItem: RepoItem) {
        viewModelScope.launch {
            hubRepository.toggleBookmark(repoItem)
        }
    }

    fun selectTab(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tabIndex)
    }

    fun loadTreePath(revision: String = "main", path: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTreeLoading = true, currentTreePath = path)
            try {
                val items = hubRepository.getFileTree(repoId, repoType, revision, path)
                _uiState.value = _uiState.value.copy(treeItems = items, isTreeLoading = false)
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isTreeLoading = false)
            }
        }
    }

    fun previewFileContent(filePath: String, revision: String = "main") {
        viewModelScope.launch {
            try {
                val text = hubRepository.getRawFileContent(repoId, repoType, filePath, revision)
                _uiState.value = _uiState.value.copy(selectedFilePreview = Pair(filePath.substringAfterLast('/'), text))
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(selectedFilePreview = Pair(filePath.substringAfterLast('/'), "Unable to load text preview for this file."))
            }
        }
    }

    fun clearFilePreview() {
        _uiState.value = _uiState.value.copy(selectedFilePreview = null)
    }

    fun downloadFile(context: android.content.Context, repo: RepoItem, fileItem: FileTreeItem) {
        viewModelScope.launch {
            downloadRepository.createDownloadJob(
                context = context,
                repo = repo,
                filePath = fileItem.path,
                fileName = fileItem.path.substringAfterLast('/'),
                totalBytes = fileItem.size ?: 0L,
                expectedSha256 = fileItem.sha256
            )
        }
    }
}
