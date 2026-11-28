package com.inferra.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inferra.data.repository.HubRepository
import com.inferra.domain.model.RepoItem
import com.inferra.domain.model.RepoType
import com.inferra.domain.model.UiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val selectedType: RepoType = RepoType.MODEL,
    val selectedSort: String = "downloads",
    val pipelineTagFilter: String? = null,
    val results: List<RepoItem> = emptyList(),
    val page: Int = 1,
    val canLoadMore: Boolean = true,
    val isLoadingMore: Boolean = false,
    val state: UiState<List<RepoItem>> = UiState.Loading
)

class SearchViewModel(
    private val hubRepository: HubRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        executeSearch()
    }

    fun onQueryChanged(newQuery: String) {
        _uiState.value = _uiState.value.copy(query = newQuery, page = 1)
        debouncedSearch()
    }

    fun onTypeChanged(type: RepoType) {
        _uiState.value = _uiState.value.copy(selectedType = type, page = 1)
        executeSearch()
    }

    fun onSortChanged(sort: String) {
        _uiState.value = _uiState.value.copy(selectedSort = sort, page = 1)
        executeSearch()
    }

    fun onPipelineTagSelected(pipelineTag: String?) {
        _uiState.value = _uiState.value.copy(pipelineTagFilter = pipelineTag, page = 1)
        executeSearch()
    }

    private fun debouncedSearch() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(350)
            executeSearch()
        }
    }

    fun executeSearch() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val curr = _uiState.value
            _uiState.value = curr.copy(state = UiState.Loading)
            try {
                val list = hubRepository.searchRepos(
                    query = curr.query,
                    type = curr.selectedType,
                    pipelineTag = curr.pipelineTagFilter,
                    sort = curr.selectedSort,
                    page = 1,
                    limit = 30
                )
                _uiState.value = _uiState.value.copy(
                    results = list,
                    page = 1,
                    canLoadMore = list.size >= 30,
                    state = if (list.isEmpty()) UiState.Empty("No ${curr.selectedType.displayName} match your query.") else UiState.Success(list)
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    state = UiState.Error("Failed to search Hugging Face Hub: ${e.message}", e)
                )
            }
        }
    }

    fun loadNextPage() {
        val curr = _uiState.value
        if (curr.isLoadingMore || !curr.canLoadMore) return

        viewModelScope.launch {
            _uiState.value = curr.copy(isLoadingMore = true)
            val nextPage = curr.page + 1
            try {
                val nextList = hubRepository.searchRepos(
                    query = curr.query,
                    type = curr.selectedType,
                    pipelineTag = curr.pipelineTagFilter,
                    sort = curr.selectedSort,
                    page = nextPage,
                    limit = 30
                )
                val combined = curr.results + nextList
                _uiState.value = _uiState.value.copy(
                    results = combined,
                    page = nextPage,
                    isLoadingMore = false,
                    canLoadMore = nextList.size >= 30,
                    state = UiState.Success(combined)
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isLoadingMore = false)
            }
        }
    }
}
