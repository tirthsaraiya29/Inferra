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

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val searchResults: List<AiModel> = emptyList(),
    val selectedTask: ModelTask? = null,
    val maxParamsBillion: Float? = null,
    val isGgufOnly: Boolean = false,
    val activeHardwareProfile: HardwareProfile? = null,
    val compatibilityMap: Map<String, HardwareCompatibilityResult> = emptyMap(),
    val errorMessage: String? = null
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
            delay(250) // 250ms debounce
            performSearch()
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
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val state = _uiState.value
                val results = modelRepository.searchModels(
                    query = state.query,
                    selectedTask = state.selectedTask,
                    maxParams = state.maxParamsBillion,
                    isGgufOnly = state.isGgufOnly
                )

                val profile = state.activeHardwareProfile ?: hardwareRepository.getActiveProfile()
                val compMap = if (profile != null) {
                    results.associate { model ->
                        model.id to HardwareFitCalculator.calculate(model, model.quantizations.firstOrNull(), profile)
                    }
                } else {
                    emptyMap()
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
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
