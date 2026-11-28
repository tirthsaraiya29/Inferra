package com.inferra.ui.screens.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inferra.data.repository.DownloadRepository
import com.inferra.domain.model.DownloadJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DownloadsViewModel(
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    val jobsState: StateFlow<List<DownloadJob>> = downloadRepository.jobsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun cancelJob(jobId: String) {
        viewModelScope.launch {
            downloadRepository.cancelJob(jobId)
        }
    }
}
