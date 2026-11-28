package com.inferra.ui.screens.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inferra.data.repository.HubRepository
import com.inferra.data.repository.SettingsRepository
import com.inferra.domain.model.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.map

class AccountSettingsViewModel(
    private val hubRepository: HubRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val authState: StateFlow<AuthState> = hubRepository.authState

    val isDarkMode: StateFlow<Boolean> = settingsRepository.userSettingsFlow
        .map { it.isDarkMode }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        ) // fallback mapping

    private val _inputToken = MutableStateFlow(hubRepository.getStoredToken() ?: "")
    val inputToken: StateFlow<String> = _inputToken.asStateFlow()

    fun onTokenInputChanged(token: String) {
        _inputToken.value = token
    }

    fun verifyAndSaveToken() {
        viewModelScope.launch {
            hubRepository.verifyAndSaveToken(_inputToken.value)
        }
    }

    fun logout() {
        viewModelScope.launch {
            hubRepository.logout()
            _inputToken.value = ""
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDarkMode(enabled)
        }
    }
}
