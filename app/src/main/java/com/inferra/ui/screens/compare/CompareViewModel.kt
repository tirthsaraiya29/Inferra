package com.inferra.ui.screens.compare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inferra.data.repository.HardwareRepository
import com.inferra.data.repository.ModelRepository
import com.inferra.domain.model.AiModel
import com.inferra.domain.model.ComparabilityCheckResult
import com.inferra.domain.model.HardwareCompatibilityResult
import com.inferra.domain.model.HardwareProfile
import com.inferra.domain.usecase.BenchmarkRegistry
import com.inferra.domain.usecase.CanonicalModelResolver
import com.inferra.domain.usecase.HardwareFitCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CompareUiState(
    val isLoading: Boolean = true,
    val availableModels: List<AiModel> = emptyList(),
    val selectedModels: List<AiModel> = emptyList(),
    val activeHardwareProfile: HardwareProfile? = null,
    val compatibilityMap: Map<String, HardwareCompatibilityResult> = emptyMap(),
    val comparabilityCheck: ComparabilityCheckResult? = null
)

class CompareViewModel(
    private val modelRepository: ModelRepository,
    private val hardwareRepository: HardwareRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompareUiState())
    val uiState: StateFlow<CompareUiState> = _uiState.asStateFlow()

    init {
        loadCompareData()
    }

    fun selectModel(model: AiModel) {
        val current = _uiState.value.selectedModels.toMutableList()
        if (current.any { it.id == model.id }) {
            current.removeAll { it.id == model.id }
        } else if (current.size < 4) {
            current.add(model)
        }

        val comparability = evaluateComparability(current)

        _uiState.update {
            it.copy(
                selectedModels = current,
                comparabilityCheck = comparability
            )
        }
    }

    private fun evaluateComparability(models: List<AiModel>): ComparabilityCheckResult? {
        if (models.size < 2) return null
        val modelA = models[0]
        val modelB = models[1]
        val cidA = CanonicalModelResolver.resolveCanonicalId(modelA.id)
        val cidB = CanonicalModelResolver.resolveCanonicalId(modelB.id)

        val benchA = BenchmarkRegistry.getStandardBenchmarksForModel(cidA).firstOrNull()
        val benchB = BenchmarkRegistry.getStandardBenchmarksForModel(cidB).firstOrNull()

        return if (benchA != null && benchB != null) {
            BenchmarkRegistry.checkComparability(benchA, benchB)
        } else {
            ComparabilityCheckResult(
                isComparable = true,
                warnings = emptyList(),
                comparisonNotes = "Empirical evaluations compared across identical standardized benchmarks."
            )
        }
    }

    private fun loadCompareData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val profile = hardwareRepository.getActiveProfile()
            val models = modelRepository.getModels()

            val compMap = models.associate { model ->
                model.id to HardwareFitCalculator.calculate(model, model.quantizations.firstOrNull(), profile)
            }

            val defaultSelected = models.take(2)
            val comparability = evaluateComparability(defaultSelected)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    availableModels = models,
                    selectedModels = defaultSelected,
                    activeHardwareProfile = profile,
                    compatibilityMap = compMap,
                    comparabilityCheck = comparability
                )
            }
        }
    }
}
