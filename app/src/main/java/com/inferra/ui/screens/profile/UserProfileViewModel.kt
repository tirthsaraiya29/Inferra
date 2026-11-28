package com.inferra.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inferra.data.repository.HubRepository
import com.inferra.domain.model.RepoItem
import com.inferra.domain.model.RepoType
import com.inferra.domain.model.UiState
import com.inferra.domain.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UserProfileUiData(
    val profile: UserProfile,
    val models: List<RepoItem> = emptyList(),
    val datasets: List<RepoItem> = emptyList(),
    val spaces: List<RepoItem> = emptyList()
)

class UserProfileViewModel(
    val username: String,
    private val hubRepository: HubRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<UserProfileUiData>>(UiState.Loading)
    val uiState: StateFlow<UiState<UserProfileUiData>> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val profile = hubRepository.getUserProfile(username)
                val userModels = hubRepository.searchRepos(author = username, type = RepoType.MODEL, limit = 20)
                val userDatasets = hubRepository.searchRepos(author = username, type = RepoType.DATASET, limit = 20)
                val userSpaces = hubRepository.searchRepos(author = username, type = RepoType.SPACE, limit = 20)

                val data = UserProfileUiData(
                    profile = profile,
                    models = userModels,
                    datasets = userDatasets,
                    spaces = userSpaces
                )
                _uiState.value = UiState.Success(data)
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to load user profile: ${e.message}", e)
            }
        }
    }
}
