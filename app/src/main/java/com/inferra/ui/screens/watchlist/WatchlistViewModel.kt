package com.inferra.ui.screens.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inferra.data.repository.HardwareRepository
import com.inferra.data.repository.ModelRepository
import com.inferra.domain.model.AiModel
import com.inferra.domain.model.HardwareCompatibilityResult
import com.inferra.domain.usecase.HardwareFitCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WatchlistUiState(
    val isLoading: Boolean = true,
    val savedModels: List<AiModel> = emptyList(),
    val compatibilityMap: Map<String, HardwareCompatibilityResult> = emptyMap(),
)

class WatchlistViewModel(
    private val modelRepository: ModelRepository,
    private val hardwareRepository: HardwareRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WatchlistUiState())
    val uiState: StateFlow<WatchlistUiState> = _uiState.asStateFlow()

    init {
        loadWatchlist()
    }

    fun removeWatchlist(modelId: String) {
        viewModelScope.launch {
            modelRepository.toggleWatchlist(modelId)
            loadWatchlist()
        }
    }

    private fun loadWatchlist() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val watchlistIds = modelRepository.getWatchlist().first()
            val allModels = modelRepository.getModels()
            val saved = allModels.filter { (id) -> watchlistIds.contains(id) }

            val profile = hardwareRepository.getActiveProfile()
            val compMap = saved.associateBy(keySelector = { it.id }) { model ->
                HardwareFitCalculator.calculate(model, model.quantizations.firstOrNull(), profile)
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    savedModels = saved,
                    compatibilityMap = compMap,
                )
            }
        }
    }
}
