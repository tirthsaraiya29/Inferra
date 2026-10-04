package com.inferra.ui.screens.datasets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inferra.data.repository.DatasetRepository
import com.inferra.domain.model.AiDataset
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

data class DatasetsUiState(
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val datasets: List<AiDataset> = emptyList(),
    val errorMessage: String? = null
)

class DatasetsViewModel(
    private val datasetRepository: DatasetRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DatasetsUiState())
    val uiState: StateFlow<DatasetsUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadDatasets()
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(250.milliseconds)
            performSearch(query)
        }
    }

    fun refresh() {
        loadDatasets(forceRefresh = true)
    }

    private fun loadDatasets(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val results = datasetRepository.getDatasets(forceRefresh = forceRefresh)
                _uiState.update { it.copy(isLoading = false, datasets = results) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Unable to load datasets. Please check network connection."
                    )
                }
            }
        }
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val results = if (query.isBlank()) {
                    datasetRepository.getDatasets()
                } else {
                    datasetRepository.searchDatasets(query)
                }
                _uiState.update { it.copy(isLoading = false, datasets = results) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Unable to search datasets. Please check network connection."
                    )
                }
            }
        }
    }
}
