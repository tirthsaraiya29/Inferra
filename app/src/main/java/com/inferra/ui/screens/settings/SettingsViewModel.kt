package com.inferra.ui.screens.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SettingsUiState(
    val glassIntensity: Float = 0.8f,
    val animationScale: Float = 1.0f,
    val isDarkMode: Boolean = true,
    val isTelemetryEnabled: Boolean = false,
    val autoRefreshData: Boolean = true
)

class SettingsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun updateGlassIntensity(value: Float) {
        _uiState.update { it.copy(glassIntensity = value) }
    }

    fun toggleDarkMode() {
        _uiState.update { it.copy(isDarkMode = !it.isDarkMode) }
    }

    fun toggleTelemetry() {
        _uiState.update { it.copy(isTelemetryEnabled = !it.isTelemetryEnabled) }
    }
}
