package com.inferra.ui.screens.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inferra.data.repository.HubRepository
import com.inferra.domain.model.RepoItem
import com.inferra.domain.model.RepoType
import com.inferra.domain.model.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DiscoveryFeedData(
    val trendingModels: List<RepoItem> = emptyList(),
    val popularDatasets: List<RepoItem> = emptyList(),
    val trendingSpaces: List<RepoItem> = emptyList(),
    val recentModels: List<RepoItem> = emptyList()
)

class DiscoveryViewModel(
    private val hubRepository: HubRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<DiscoveryFeedData>>(UiState.Loading)
    val uiState: StateFlow<UiState<DiscoveryFeedData>> = _uiState.asStateFlow()

    init {
        loadDiscoveryFeed()
    }

    fun loadDiscoveryFeed() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val trendingModels = hubRepository.searchRepos(type = RepoType.MODEL, sort = "downloads", limit = 10)
                val popularDatasets = hubRepository.searchRepos(type = RepoType.DATASET, sort = "downloads", limit = 10)
                val trendingSpaces = hubRepository.searchRepos(type = RepoType.SPACE, sort = "likes", limit = 10)
                val recentModels = hubRepository.searchRepos(type = RepoType.MODEL, sort = "lastModified", limit = 10)

                val feed = DiscoveryFeedData(
                    trendingModels = trendingModels,
                    popularDatasets = popularDatasets,
                    trendingSpaces = trendingSpaces,
                    recentModels = recentModels
                )

                _uiState.value = if (trendingModels.isEmpty() && popularDatasets.isEmpty()) {
                    UiState.Empty("No discovery content available.")
                } else {
                    UiState.Success(feed)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to load Hugging Face discovery feed: ${e.message}", e)
            }
        }
    }
}
