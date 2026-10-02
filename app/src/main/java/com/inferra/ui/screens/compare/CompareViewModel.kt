package com.inferra.ui.screens.compare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inferra.data.local.AndroidModelWideEntity
import com.inferra.data.repository.ModelRepository
import com.inferra.domain.model.UiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

class CompareViewModel(
    private val modelRepository: ModelRepository,
) : ViewModel() {

    private val _selectedIds = MutableStateFlow<List<String>>(emptyList())
    val selectedIds: StateFlow<List<String>> = _selectedIds.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val matrixState: StateFlow<UiState<List<AndroidModelWideEntity>>> = _selectedIds
        .flatMapLatest { ids ->
            modelRepository.observeWideModels(ids)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UiState.Loading,
        )

    fun toggleModelSelection(modelId: String) {
        val current = _selectedIds.value.toMutableList()
        if (current.contains(modelId)) {
            current.remove(modelId)
        } else if (current.size < 4) {
            current.add(modelId)
        }
        _selectedIds.value = current
    }

    fun clearSelections() {
        _selectedIds.value = emptyList()
    }
}
