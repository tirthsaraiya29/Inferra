package com.inferra.ui.screens.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inferra.data.local.AndroidModelEntity
import com.inferra.data.repository.ModelRepository
import com.inferra.domain.model.BenchmarkScoreUiModel
import com.inferra.domain.model.UiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class CatalogFilterState(
    val searchQuery: String = "",
    val minParamsBillion: Int? = null,
    val maxParamsBillion: Int? = null,
    val minContextLength: Int? = null,
    val isOpenWeightsOnly: Boolean? = null,
    val licenseQuery: String? = null,
    val sortBy: String = "updated_at",
)

class DiscoveryViewModel(
    private val modelRepository: ModelRepository
) : ViewModel() {

    private val _filterState = MutableStateFlow(CatalogFilterState())
    val filterState: StateFlow<CatalogFilterState> = _filterState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val catalogUiState: StateFlow<UiState<List<AndroidModelEntity>>> = _filterState
        .flatMapLatest { filter ->
            modelRepository.observeAndroidModels(
                query = filter.searchQuery,
                minParams = filter.minParamsBillion,
                maxParams = filter.maxParamsBillion,
                minContext = filter.minContextLength,
                isOpenWeights = filter.isOpenWeightsOnly,
                license = filter.licenseQuery,
                sortBy = filter.sortBy
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UiState.Loading
        )

    fun onSearchQueryChanged(query: String) {
        _filterState.value = _filterState.value.copy(searchQuery = query)
    }

    fun onParamsRangeChanged(minParams: Int?, maxParams: Int?) {
        _filterState.value = _filterState.value.copy(
            minParamsBillion = minParams,
            maxParamsBillion = maxParams
        )
    }

    fun onOpenWeightsToggled(isOpenOnly: Boolean?) {
        _filterState.value = _filterState.value.copy(isOpenWeightsOnly = isOpenOnly)
    }

    fun onMinContextChanged(minContext: Int?) {
        _filterState.value = _filterState.value.copy(minContextLength = minContext)
    }

    fun onSortByChanged(sortBy: String) {
        _filterState.value = _filterState.value.copy(sortBy = sortBy)
    }

    fun resetFilters() {
        _filterState.value = CatalogFilterState()
    }

    fun getTopScoresForModel(modelId: String): StateFlow<List<BenchmarkScoreUiModel>> {
        return modelRepository.observeBenchmarkScores(modelId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }
}
