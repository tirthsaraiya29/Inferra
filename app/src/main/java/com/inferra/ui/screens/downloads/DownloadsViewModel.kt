package com.inferra.ui.screens.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inferra.data.local.LocalModelDao
import com.inferra.data.local.LocalModelEntity
import com.inferra.data.repository.CompanionRepository
import com.inferra.data.repository.DownloadRepository
import com.inferra.domain.model.DeviceTarget
import com.inferra.domain.model.DownloadJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DownloadsUiState(
    val jobs: List<DownloadJob> = emptyList(),
    val devices: List<DeviceTarget> = emptyList(),
    val installedLocalModels: List<LocalModelEntity> = emptyList(),
    val selectedManifest: String? = null
)

class DownloadsViewModel(
    private val downloadRepository: DownloadRepository,
    private val companionRepository: CompanionRepository,
    private val localModelDao: LocalModelDao? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(DownloadsUiState())
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()

    init {
        loadData()
        observeInstalledLocalModels()
    }

    fun cancelJob(jobId: String) {
        viewModelScope.launch {
            downloadRepository.cancelJob(jobId)
        }
    }

    fun deleteLocalModel(id: String) {
        viewModelScope.launch {
            localModelDao?.deleteLocalModel(id)
        }
    }

    fun pairNewDevice(name: String, ipAddress: String, port: Int) {
        viewModelScope.launch {
            companionRepository.pairNewDevice(name, ipAddress, port)
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            downloadRepository.jobsFlow.collect { jobs ->
                val devices = companionRepository.devicesFlow.firstOrNull() ?: emptyList()
                _uiState.update {
                    it.copy(
                        jobs = jobs,
                        devices = devices
                    )
                }
            }
        }
    }

    private fun observeInstalledLocalModels() {
        localModelDao?.let { dao ->
            viewModelScope.launch {
                dao.getAllLocalModels().collect { localModels ->
                    _uiState.update { it.copy(installedLocalModels = localModels) }
                }
            }
        }
    }
}
