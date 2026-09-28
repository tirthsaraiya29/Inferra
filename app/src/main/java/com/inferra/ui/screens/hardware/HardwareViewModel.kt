package com.inferra.ui.screens.hardware

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inferra.data.repository.HardwareRepository
import com.inferra.domain.model.DeviceType
import com.inferra.domain.model.HardwareProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HardwareUiState(
    val profiles: List<HardwareProfile> = emptyList(),
    val activeProfile: HardwareProfile? = null,
    val localDetectedProfile: HardwareProfile? = null
)

class HardwareViewModel(
    private val hardwareRepository: HardwareRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HardwareUiState())
    val uiState: StateFlow<HardwareUiState> = _uiState.asStateFlow()

    init {
        loadProfiles()
    }

    fun addCustomPcProfile(
        name: String,
        gpuName: String,
        vramGb: Float,
        ramGb: Float,
        cpuName: String
    ) {
        viewModelScope.launch {
            val newProfile = HardwareProfile(
                id = "custom-${System.currentTimeMillis()}",
                name = name,
                deviceType = DeviceType.DESKTOP_PC,
                cpuName = cpuName.ifBlank { "x86_64 CPU" },
                gpuName = gpuName.ifBlank { "NVIDIA GPU" },
                vramGb = vramGb,
                ramGb = ramGb,
                osName = "Windows 11 / Linux",
                preferredRuntime = "llama.cpp (CUDA)",
                availableStorageGb = 1000f,
                isLocalDevice = false
            )
            hardwareRepository.addCustomProfile(newProfile)
            loadProfiles()
        }
    }

    fun deleteProfile(id: String) {
        viewModelScope.launch {
            hardwareRepository.deleteProfile(id)
            loadProfiles()
        }
    }

    private fun loadProfiles() {
        viewModelScope.launch {
            hardwareRepository.profilesFlow.collect { profiles ->
                val detected = hardwareRepository.detectLocalAndroidHardware()
                val active = profiles.find { !it.isLocalDevice } ?: detected
                _uiState.update {
                    it.copy(
                        profiles = profiles,
                        activeProfile = active,
                        localDetectedProfile = detected
                    )
                }
            }
        }
    }
}
