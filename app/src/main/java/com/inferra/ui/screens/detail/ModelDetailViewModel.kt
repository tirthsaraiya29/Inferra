package com.inferra.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inferra.data.network.HuggingFaceClient
import com.inferra.data.repository.CompanionRepository
import com.inferra.data.repository.DownloadRepository
import com.inferra.data.repository.HardwareRepository
import com.inferra.data.repository.ModelDownloader
import com.inferra.data.repository.ModelRepository
import com.inferra.data.repository.QuantDiscoveryRepository
import com.inferra.domain.model.AiModel
import com.inferra.domain.model.DeviceTarget
import com.inferra.domain.model.DownloadJob
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
    val activeLocalDownloadJob: DownloadJob? = null,
    val errorMessage: String? = null
)

class ModelDetailViewModel(
    private val modelId: String,
    private val modelRepository: ModelRepository,
    private val hardwareRepository: HardwareRepository,
    private val downloadRepository: DownloadRepository,
    private val companionRepository: CompanionRepository,
    private val quantDiscoveryRepository: QuantDiscoveryRepository = QuantDiscoveryRepository(HuggingFaceClient.api),
    private val modelDownloader: ModelDownloader? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModelDetailUiState())
    val uiState: StateFlow<ModelDetailUiState> = _uiState.asStateFlow()

    init {
        loadModelDetail()

        // Observe active local downloads if downloader is present
        modelDownloader?.let { downloader ->
            viewModelScope.launch {
                downloader.downloadProgressFlow.collect { progressMap ->
                    val currentModelJob = progressMap.values.find { it.modelId == modelId }
                    _uiState.update { it.copy(activeLocalDownloadJob = currentModelJob) }
                }
            }
        }
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

    fun downloadToDevice(quant: QuantizationInfo) {
        val model = _uiState.value.model ?: return
        viewModelScope.launch {
            val job = downloadRepository.createLocalDeviceDownloadJob(model, quant)
            _uiState.update {
                it.copy(
                    activeLocalDownloadJob = job,
                    sendToPcSuccessMessage = "Started direct download of ${quant.fileName}"
                )
            }
            modelDownloader?.startDownload(job)
        }
    }

    fun cancelLocalDownload(jobId: String) {
        modelDownloader?.cancelDownload(jobId)
        viewModelScope.launch {
            downloadRepository.cancelJob(jobId)
            _uiState.update { it.copy(activeLocalDownloadJob = null) }
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

    fun retry() {
        loadModelDetail()
    }

    private fun loadModelDetail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val baseModel = modelRepository.getModelById(modelId)
                val profile = hardwareRepository.getActiveProfile()
                val watchlisted = modelRepository.isWatchlisted(modelId).first()
                val companions = companionRepository.devicesFlow.first()

                if (baseModel != null) {
                    // Dynamically discover quantized variants from Hugging Face
                    val dto = try { HuggingFaceClient.api.getModelDetail(modelId) } catch (_: Exception) { null }
                    val discoveredQuants = if (dto != null) {
                        try {
                            quantDiscoveryRepository.discoverQuantizations(dto, baseModel.totalParamsBillion)
                        } catch (_: Exception) {
                            baseModel.quantizations
                        }
                    } else {
                        baseModel.quantizations
                    }

                    val finalQuants = if (discoveredQuants.isNotEmpty()) discoveredQuants else baseModel.quantizations
                    val enrichedModel = baseModel.copy(quantizations = finalQuants)

                    val primaryQuant = finalQuants.firstOrNull()
                    val comp = HardwareFitCalculator.calculate(enrichedModel, primaryQuant, profile)

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            model = enrichedModel,
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
                            errorMessage = "Model '$modelId' could not be found on Hugging Face."
                        )
                    }
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Unable to load model detail from Hugging Face. Please check your network connection."
                    )
                }
            }
        }
    }
}
