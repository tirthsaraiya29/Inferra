package com.inferra.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inferra.data.repository.CompanionRepository
import com.inferra.data.repository.DownloadRepository
import com.inferra.data.repository.HardwareRepository
import com.inferra.data.repository.ModelRepository
import com.inferra.domain.model.AiModel
import com.inferra.domain.model.DeviceTarget
import com.inferra.domain.model.HardwareCompatibilityResult
import com.inferra.domain.model.HardwareProfile
import com.inferra.domain.model.QuantizationInfo
import com.inferra.domain.usecase.HardwareFitCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ModelDetailUiState(
    val isLoading: Boolean = true,
    val model: AiModel? = null,
    val activeHardwareProfile: HardwareProfile? = null,
    val selectedQuantization: QuantizationInfo? = null,
    val compatibilityResult: HardwareCompatibilityResult? = null,
    val isWatchlisted: Boolean = false,
    val companionDevices: List<DeviceTarget> = emptyList(),
    val sendToPcSuccessMessage: String? = null,
    val errorMessage: String? = null
)

class ModelDetailViewModel(
    private val modelId: String,
    private val modelRepository: ModelRepository,
    private val hardwareRepository: HardwareRepository,
    private val downloadRepository: DownloadRepository,
    private val companionRepository: CompanionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModelDetailUiState())
    val uiState: StateFlow<ModelDetailUiState> = _uiState.asStateFlow()

    init {
        loadModelDetail()
    }

    fun selectQuantization(quant: QuantizationInfo) {
        val model = _uiState.value.model ?: return
        val profile = _uiState.value.activeHardwareProfile ?: return
        val comp = HardwareFitCalculator.calculate(model, quant, profile)
        _uiState.update {
            it.copy(
                selectedQuantization = quant,
                compatibilityResult = comp
            )
        }
    }

    fun toggleWatchlist() {
        val model = _uiState.value.model ?: return
        viewModelScope.launch {
            modelRepository.toggleWatchlist(model.id)
            val updated = modelRepository.isWatchlisted(model.id).first()
            _uiState.update { it.copy(isWatchlisted = updated) }
        }
    }

    fun sendToPc(targetDevice: DeviceTarget) {
        val model = _uiState.value.model ?: return
        val quant = _uiState.value.selectedQuantization ?: model.quantizations.firstOrNull() ?: return
        viewModelScope.launch {
            downloadRepository.createSendToPcJob(model, quant, targetDevice)
            _uiState.update {
                it.copy(sendToPcSuccessMessage = "Download manifest dispatched to ${targetDevice.name} (${quant.quantType})")
            }
        }
    }

    fun dismissSuccessMessage() {
        _uiState.update { it.copy(sendToPcSuccessMessage = null) }
    }

    private fun loadModelDetail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val model = modelRepository.getModelById(modelId)
            val profile = hardwareRepository.getActiveProfile()
            val watchlisted = modelRepository.isWatchlisted(modelId).first()
            val companions = companionRepository.devicesFlow.first()

            if (model != null) {
                val primaryQuant = model.quantizations.firstOrNull()
                val comp = HardwareFitCalculator.calculate(model, primaryQuant, profile)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        model = model,
                        activeHardwareProfile = profile,
                        selectedQuantization = primaryQuant,
                        compatibilityResult = comp,
                        isWatchlisted = watchlisted,
                        companionDevices = companions
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Model not found."
                    )
                }
            }
        }
    }
}
