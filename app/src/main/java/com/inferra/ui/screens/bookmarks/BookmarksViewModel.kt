package com.inferra.ui.screens.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inferra.data.repository.HubRepository
import com.inferra.domain.model.Bookmark
import com.inferra.domain.model.RepoItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BookmarksViewModel(
    private val hubRepository: HubRepository
) : ViewModel() {

    val bookmarksState: StateFlow<List<Bookmark>> = hubRepository.bookmarksFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun toggleBookmark(repo: RepoItem) {
        viewModelScope.launch {
            hubRepository.toggleBookmark(repo)
        }
    }
}
