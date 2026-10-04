package com.inferra.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

// ==============================================================================
// UI State & Common Enums
// ==============================================================================

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

// ==============================================================================
// Benchmark Score Models
// ==============================================================================

enum class BenchmarkDomainCategory(val displayName: String) {
    REASONING_SCIENCE("Reasoning & Science"),
    SOFTWARE_ENGINEERING("Software Engineering"),
    AGENTS_TOOLS("Agents & Tools"),
    CONTEXT_MULTIMODAL("Context & Multimodal"),
    FACTUALITY("Contamination & Factuality"),
    HUMAN_PREFERENCE("Human Preference"),
    OTHER("Other")
}

data class BenchmarkScoreUiModel(
    val benchmarkId: String,
    val name: String,
    val domain: String,
    val metricName: String,
    val rawScore: Double?,
    val normalizedScore: Double?,
    val measurementType: String,
    val scoreFormatted: String = rawScore?.let {
        if (metricName.contains("Elo", ignoreCase = true)) {
            "%.0f".format(it)
        } else if (it <= 1.0 && it > 0.0) {
            "%.1f%%".format(it * 100)
        } else {
            "%.1f".format(it)
        }
    } ?: "—",
    val category: BenchmarkDomainCategory = when (domain.uppercase()) {
        "REASONING", "SCIENCE", "MATHEMATICS", "INSTRUCTION_FOLLOWING" -> BenchmarkDomainCategory.REASONING_SCIENCE
        "CODING" -> BenchmarkDomainCategory.SOFTWARE_ENGINEERING
        "AGENTS" -> BenchmarkDomainCategory.AGENTS_TOOLS
        "LONG_CONTEXT", "MULTIMODAL" -> BenchmarkDomainCategory.CONTEXT_MULTIMODAL
        "FACTUALITY" -> BenchmarkDomainCategory.FACTUALITY
        "HUMAN_PREFERENCE" -> BenchmarkDomainCategory.HUMAN_PREFERENCE
        else -> BenchmarkDomainCategory.OTHER
    }
)

// ==============================================================================
// Hardware Profiles & Devices
// ==============================================================================

enum class DeviceType {
    LOCAL_ANDROID,
    DESKTOP_PC,
    LAPTOP,
    WORKSTATION_SERVER
}

enum class FitGrade {
    EXCELLENT,      // Comfortably fits in VRAM/RAM with headroom
    BORDERLINE,     // Fits in combined RAM/VRAM but offloading needed or tight
    INSUFFICIENT,   // Exceeds available RAM/VRAM
    UNKNOWN
}

data class HardwareProfile(
    val id: String,
    val name: String,
    val deviceType: DeviceType,
    val cpuName: String,
    val gpuName: String,
    val vramGb: Float,
    val ramGb: Float,
    val osName: String,
    val preferredRuntime: String,
    val availableStorageGb: Float = 500f,
    val isLocalDevice: Boolean = false
)

data class HardwareCompatibilityResult(
    val fitGrade: FitGrade,
    val requiredVramGb: Float,
    val requiredRamGb: Float,
    val estimatedTokensPerSec: Float,
    val estimatedTtftMs: Float,
    val offloadPercentage: Int,
    val explanation: String,
    val profileName: String
)

data class DeviceTarget(
    val id: String,
    val name: String,
    val ipAddress: String,
    val port: Int = 8443,
    val osName: String,
    val isOnline: Boolean,
    val lastSeenEpochMs: Long,
    val isPaired: Boolean,
    val supportedRuntimes: List<String>,
)

// ==============================================================================
// Downloads & Manifest
// ==============================================================================

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    WAITING_FOR_DEVICE
}

@Serializable
data class DownloadManifest(
    val modelId: String,
    val repository: String,
    val exactRevision: String,
    val fileName: String,
    val expectedSizeBytes: Long,
    val checksumSha256: String? = null,
    val sourceUrl: String,
    val targetDeviceId: String,
    val requestedFormat: String,
    val createdAtEpochMs: Long
)

data class DownloadJob(
    val id: String,
    val modelId: String,
    val modelName: String,
    val author: String,
    val quantType: String,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val status: DownloadStatus,
    val targetDeviceId: String,
    val targetDeviceName: String,
    val speedBytesPerSec: Long = 0,
    val etaSeconds: Long = 0,
    val errorMessage: String? = null,
    val manifest: DownloadManifest
)

// ==============================================================================
// Provider & Deployments
// ==============================================================================

@Immutable
@Serializable
data class Provider(
    val id: String,
    val name: String,
    val logoUrl: String = "",
    val websiteUrl: String = "",
    val isSelfHosted: Boolean = false,
)

@Immutable
@Serializable
data class ProviderDeployment(
    val id: String,
    val providerId: String,
    val canonicalId: String,
    val providerModelId: String,
    val supportedContextTokens: Int = 131072,
    val isAvailable: Boolean = true,
    val region: String = "Global"
)

@Immutable
@Serializable
data class PricingRecord(
    val id: String,
    val deploymentId: String,
    val inputPricePerMToken: Double,
    val outputPricePerMToken: Double,
    val cachedInputPricePerMToken: Double? = null,
    val batchPricePerMToken: Double? = null,
    val currency: String = "USD",
    val effectiveDate: String = "",
    val sourceUrl: String = ""
)

@Immutable
@Serializable
data class ProviderMeasurement(
    val id: String,
    val deploymentId: String,
    val prefillTokensPerSec: Float = 0f,
    val decodeTokensPerSec: Float = 0f,
    val ttftMs: Float = 0f,
    val measuredAtEpochMs: Long = System.currentTimeMillis(),
    val source: String = "Provider Docs / Independent Benchmark"
)
