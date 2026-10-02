package com.inferra.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inferra.data.network.HuggingFaceClient
import com.inferra.data.repository.CompanionRepository
import com.inferra.data.repository.DownloadRepository
import com.inferra.data.repository.HardwareRepository
import com.inferra.data.repository.ModelDownloader
import com.inferra.data.repository.ModelRepository
import com.inferra.data.repository.ProviderRepository
import com.inferra.data.repository.QuantDiscoveryRepository
import com.inferra.domain.model.AiModel
import com.inferra.domain.model.CanonicalModel
import com.inferra.domain.model.DeviceTarget
import com.inferra.domain.model.DownloadJob
import com.inferra.domain.model.HardwareCompatibilityResult
import com.inferra.domain.model.HardwareProfile
import com.inferra.domain.model.ProviderMeasurement
import com.inferra.domain.model.QuantizationInfo
import com.inferra.domain.usecase.CanonicalModelResolver
import com.inferra.domain.usecase.HardwareFitCalculator
import com.inferra.ui.components.ProviderRowItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class ModelDetailUiState(
    val isLoading: Boolean = true,
    val model: AiModel? = null,
    val canonicalModel: CanonicalModel? = null,
    val providerRows: List<ProviderRowItem> = emptyList(),
    val localFilePath: String? = null,
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
    private val providerRepository: ProviderRepository? = null,
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

        // Check if file exists locally
        val downloadDir = modelDownloader?.getDownloadDirectory()
        val file = if (downloadDir != null) File(downloadDir, quant.fileName) else null
        val localPath = if (file != null && file.exists()) file.absolutePath else null

        _uiState.update {
            it.copy(
                selectedQuantization = quant,
                compatibilityResult = comp,
                localFilePath = localPath
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
                    val canonicalModel = modelRepository.getCanonicalModel(modelId)
                    val canonicalId = canonicalModel.id

                    val deployments = providerRepository?.getDeploymentsForModel(canonicalId)?.firstOrNull() ?: emptyList()
                    val providerRows = deployments.map { dep ->
                        val pricing = providerRepository?.getPricingForDeployment(dep.id)
                        val meas = providerRepository?.getMeasurementsForDeployment(dep.id)
                            ?: ProviderMeasurement("m:default", dep.id)
                        val providerName = when {
                            dep.providerId.contains("groq") -> "Groq"
                            dep.providerId.contains("together") -> "Together AI"
                            dep.providerId.contains("fireworks") -> "Fireworks AI"
                            dep.providerId.contains("deepinfra") -> "DeepInfra"
                            else -> "Cloud Provider"
                        }
                        ProviderRowItem(
                            deployment = dep,
                            providerName = providerName,
                            pricing = pricing,
                            measurement = meas
                        )
                    }

                    // Dynamically discover quantized variants from Hugging Face
                    val dto = try { HuggingFaceClient.api.getModelDetail(id = modelId) } catch (_: Exception) { null }
                    val discoveredQuants = if (dto != null) {
                        try {
                            quantDiscoveryRepository.discoverQuantizations(dto, baseModel.totalParamsBillion)
                        } catch (_: Exception) {
                            baseModel.quantizations
                        }
                    } else {
                        baseModel.quantizations
                    }

                    val finalQuants = discoveredQuants.ifEmpty { baseModel.quantizations }
                    val enrichedModel = baseModel.copy(quantizations = finalQuants)

                    val primaryQuant = finalQuants.firstOrNull()
                    val comp = HardwareFitCalculator.calculate(enrichedModel, primaryQuant, profile)

                    val downloadDir = modelDownloader?.getDownloadDirectory()
                    val file = if (downloadDir != null && primaryQuant != null) File(downloadDir, primaryQuant.fileName) else null
                    val localPath = if (file != null && file.exists()) file.absolutePath else null

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            model = enrichedModel,
                            canonicalModel = canonicalModel,
                            providerRows = providerRows,
                            localFilePath = localPath,
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
