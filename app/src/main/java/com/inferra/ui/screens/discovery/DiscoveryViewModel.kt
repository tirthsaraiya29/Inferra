package com.inferra.ui.screens.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inferra.data.repository.HardwareRepository
import com.inferra.data.repository.ModelRepository
import com.inferra.domain.model.AiModel
import com.inferra.domain.model.FitGrade
import com.inferra.domain.model.HardwareCompatibilityResult
import com.inferra.domain.model.HardwareProfile
import com.inferra.domain.usecase.HardwareFitCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiscoveryUiState(
    val isLoading: Boolean = true,
    val activeHardwareProfile: HardwareProfile? = null,
    val spotlightModel: AiModel? = null,
    val trendingModels: List<AiModel> = emptyList(),
    val forYouModels: List<AiModel> = emptyList(),
    val newModels: List<AiModel> = emptyList(),
    val popularModels: List<AiModel> = emptyList(),
    val compatibilityMap: Map<String, HardwareCompatibilityResult> = emptyMap(),
    val errorMessage: String? = null
)

class DiscoveryViewModel(
    private val modelRepository: ModelRepository,
    private val hardwareRepository: HardwareRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoveryUiState())
    val uiState: StateFlow<DiscoveryUiState> = _uiState.asStateFlow()

    init {
        loadDiscoveryData()
    }

    fun refresh() {
        loadDiscoveryData(forceRefresh = true)
    }

    private fun loadDiscoveryData(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val profile = hardwareRepository.getActiveProfile()
                val models = modelRepository.getModels(forceRefresh = forceRefresh)

                // Calculate hardware compatibility for all models against active profile
                val compMap = models.associate { model ->
                    val primaryQuant = model.quantizations.firstOrNull()
                    model.id to HardwareFitCalculator.calculate(model, primaryQuant, profile)
                }

                val spotlight = models.find { it.isFeatured } ?: models.firstOrNull()
                val trending = models.filter { it.isTrending }
                val newReleases = models.filter { it.isNew }
                val popular = models.sortedByDescending { it.downloadsCount }
                val forYou = models.filter { model ->
                    val comp = compMap[model.id]
                    comp?.fitGrade == FitGrade.EXCELLENT || comp?.fitGrade == FitGrade.BORDERLINE
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        activeHardwareProfile = profile,
                        spotlightModel = spotlight,
                        trendingModels = trending,
                        forYouModels = forYou,
                        newModels = newReleases,
                        popularModels = popular,
                        compatibilityMap = compMap
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load model intelligence: ${e.localizedMessage}"
                    )
                }
            }
        }
    }
}
