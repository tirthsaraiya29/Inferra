package com.inferra.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inferra.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val glassIntensity: Float = 0.8f,
    val animationScale: Float = 1.0f,
    val isDarkMode: Boolean = true,
    val isTelemetryEnabled: Boolean = false,
    val autoRefreshData: Boolean = true,
    val modelStoragePath: String = "",
    val hfToken: String = "",
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        settingsRepository?.let { repo ->
            viewModelScope.launch {
                repo.userSettingsFlow.collect { settings ->
                    _uiState.update {
                        it.copy(
                            glassIntensity = settings.glassIntensity,
                            isDarkMode = settings.isDarkMode,
                            isTelemetryEnabled = settings.isTelemetryEnabled,
                            autoRefreshData = settings.autoRefreshData,
                            modelStoragePath = settings.modelStoragePath,
                            hfToken = settings.hfToken,
                        )
                    }
                }
            }
        }
    }

    fun updateGlassIntensity(value: Float) {
        _uiState.update { it.copy(glassIntensity = value) }
        viewModelScope.launch {
            settingsRepository?.updateGlassIntensity(value)
        }
    }

    fun toggleDarkMode() {
        val newMode = !_uiState.value.isDarkMode
        _uiState.update { it.copy(isDarkMode = newMode) }
        viewModelScope.launch {
            settingsRepository?.setDarkMode(newMode)
        }
    }

    fun toggleTelemetry() {
        val newTelemetry = !_uiState.value.isTelemetryEnabled
        _uiState.update { it.copy(isTelemetryEnabled = newTelemetry) }
        viewModelScope.launch {
            settingsRepository?.setTelemetryEnabled(newTelemetry)
        }
    }

    fun updateModelStoragePath(path: String) {
        _uiState.update { it.copy(modelStoragePath = path) }
        viewModelScope.launch {
            settingsRepository?.setModelStoragePath(path)
        }
    }

    fun updateHfToken(token: String) {
        _uiState.update { it.copy(hfToken = token) }
        viewModelScope.launch {
            settingsRepository?.setHfToken(token)
        }
    }
}
