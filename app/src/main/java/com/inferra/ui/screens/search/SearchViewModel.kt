package com.inferra.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inferra.data.repository.HardwareRepository
import com.inferra.data.repository.ModelRepository
import com.inferra.domain.model.AiModel
import com.inferra.domain.model.HardwareCompatibilityResult
import com.inferra.domain.model.HardwareProfile
import com.inferra.domain.model.ModelTask
import com.inferra.domain.usecase.HardwareFitCalculator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val currentPage: Int = 0,
    val canLoadMore: Boolean = true,
    val searchResults: List<AiModel> = emptyList(),
    val selectedTask: ModelTask? = null,
    val maxParamsBillion: Float? = null,
    val isGgufOnly: Boolean = false,
    val activeHardwareProfile: HardwareProfile? = null,
    val compatibilityMap: Map<String, HardwareCompatibilityResult> = emptyMap(),
    val errorMessage: String? = null,
)

class SearchViewModel(
    private val modelRepository: ModelRepository,
    private val hardwareRepository: HardwareRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadProfileAndPerformSearch()
    }

    fun onQueryChanged(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
        triggerSearchDebounced()
    }

    fun onTaskFilterSelected(task: ModelTask?) {
        _uiState.update { it.copy(selectedTask = if (it.selectedTask == task) null else task) }
        performSearch()
    }

    fun onMaxParamsChanged(maxParams: Float?) {
        _uiState.update { it.copy(maxParamsBillion = maxParams) }
        performSearch()
    }

    fun onGgufToggle() {
        _uiState.update { it.copy(isGgufOnly = !it.isGgufOnly) }
        performSearch()
    }

    fun retry() {
        performSearch()
    }

    private fun triggerSearchDebounced() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(250.milliseconds) // 250ms debounce
            performSearch()
        }
    }

    fun loadNextPage() {
        val (query, isLoading, isLoadingMore, currentPage, canLoadMore, searchResults, selectedTask, maxParamsBillion, isGgufOnly, activeHardwareProfile) = _uiState.value
        if (isLoading || isLoadingMore || !canLoadMore) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            try {
                val nextPage = currentPage + 1
                val newResults = modelRepository.searchModels(
                    query = query,
                    selectedTask = selectedTask,
                    maxParams = maxParamsBillion,
                    isGgufOnly = isGgufOnly,
                    page = nextPage
                )

                val existingIds = searchResults.map { it.id }.toSet()
                val distinctNew = newResults.filter { !existingIds.contains(it.id) }
                val combined = searchResults + distinctNew

                val profile = activeHardwareProfile ?: hardwareRepository.getActiveProfile()
                val compMap = if (profile != null) {
                    combined.associateBy(keySelector = { it.id }) { model ->
                        HardwareFitCalculator.calculate(model, model.quantizations.firstOrNull(), profile)
                    }
                } else {
                    emptyMap()
                }

                _uiState.update {
                    it.copy(
                        isLoadingMore = false,
                        currentPage = nextPage,
                        canLoadMore = newResults.isNotEmpty(),
                        searchResults = combined,
                        compatibilityMap = compMap
                    )
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoadingMore = false, canLoadMore = false) }
            }
        }
    }

    private fun loadProfileAndPerformSearch() {
        viewModelScope.launch {
            val profile = hardwareRepository.getActiveProfile()
            _uiState.update { it.copy(activeHardwareProfile = profile) }
            performSearch()
        }
    }

    private fun performSearch() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, currentPage = 0, canLoadMore = true, errorMessage = null) }
            try {
                val state = _uiState.value
                val results = modelRepository.searchModels(
                    query = state.query,
                    selectedTask = state.selectedTask,
                    maxParams = state.maxParamsBillion,
                    isGgufOnly = state.isGgufOnly,
                    page = 0
                )

                val profile = state.activeHardwareProfile ?: hardwareRepository.getActiveProfile()
                val compMap = if (profile != null) {
                    results.associateBy(keySelector = { it.id }) { model ->
                        HardwareFitCalculator.calculate(model, model.quantizations.firstOrNull(), profile)
                    }
                } else {
                    emptyMap()
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentPage = 0,
                        searchResults = results,
                        compatibilityMap = compMap
                    )
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        searchResults = emptyList(),
                        errorMessage = "Unable to search Hugging Face. Please check your network connection."
                    )
                }
            }
        }
    }
}
