package com.inferra.domain.model

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Empty(val reason: EmptyReason) : UiState<Nothing>
    data class Error(
        val throwable: Throwable? = null,
        val userMessage: String,
        val canRetry: Boolean = true
    ) : UiState<Nothing>
}

enum class EmptyReason {
    NO_RESULTS,
    NO_BENCHMARKS,
    NO_COMPARISON_MODELS,
    CORRUPTED_CACHE
}
