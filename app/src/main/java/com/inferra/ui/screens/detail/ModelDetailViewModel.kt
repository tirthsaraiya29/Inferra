package com.inferra.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inferra.data.local.ModelWithDetails
import com.inferra.data.repository.ModelRepository
import com.inferra.domain.model.BenchmarkScoreUiModel
import com.inferra.domain.model.ProvenanceInfo
import com.inferra.domain.model.ProvenanceResolver
import com.inferra.domain.model.UiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ModelDetailViewState(
    val detailsState: UiState<ModelWithDetails> = UiState.Loading,
    val benchmarkScores: List<BenchmarkScoreUiModel> = emptyList(),
    val provenanceInfo: ProvenanceInfo? = null,
    val isWatchlisted: Boolean = false,
)

class ModelDetailViewModel(
    private val modelId: String,
    private val modelRepository: ModelRepository,
) : ViewModel() {

    val viewState: StateFlow<ModelDetailViewState> = combine(
        modelRepository.observeAndroidModelDetails(modelId),
        modelRepository.observeBenchmarkScores(modelId),
        modelRepository.isWatchlisted(modelId),
    ) { detailsState, scores, isWatchlisted ->
        val provInfo = if (detailsState is UiState.Success) {
            val (model) = detailsState.data
            ProvenanceResolver.resolveProvenance(
                modelId = model.id,
                organization = model.organization,
                isOpenWeights = model.isOpenWeights == 1,
                licenseName = model.license,
            )
        } else null

        ModelDetailViewState(
            detailsState = detailsState,
            benchmarkScores = scores,
            provenanceInfo = provInfo,
            isWatchlisted = isWatchlisted,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ModelDetailViewState(),
    )

    fun toggleWatchlist() {
        viewModelScope.launch {
            modelRepository.toggleWatchlist(modelId)
        }
    }
}
