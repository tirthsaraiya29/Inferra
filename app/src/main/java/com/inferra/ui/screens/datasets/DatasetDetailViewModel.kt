package com.inferra.ui.screens.datasets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inferra.data.repository.DatasetRepository
import com.inferra.domain.model.AiDataset
import com.inferra.domain.model.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DatasetDetailViewState(
    val datasetState: UiState<AiDataset> = UiState.Loading
)

class DatasetDetailViewModel(
    private val datasetId: String,
    private val datasetRepository: DatasetRepository
) : ViewModel() {

    private val _viewState = MutableStateFlow(DatasetDetailViewState())
    val viewState: StateFlow<DatasetDetailViewState> = _viewState.asStateFlow()

    init {
        loadDatasetDetail()
    }

    private fun loadDatasetDetail() {
        viewModelScope.launch {
            _viewState.update { it.copy(datasetState = UiState.Loading) }
            val dataset = datasetRepository.getDatasetById(datasetId)
            if (dataset != null) {
                _viewState.update { it.copy(datasetState = UiState.Success(dataset)) }
            } else {
                _viewState.update {
                    it.copy(
                        datasetState = UiState.Error(
                            userMessage = "Failed to load dataset details for $datasetId"
                        )
                    )
                }
            }
        }
    }
}
