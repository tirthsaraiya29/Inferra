package com.inferra.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class LocalBenchmarkResult(
    val id: String,                         // e.g. "benchrun-1700000000"
    val canonicalId: String,                // e.g. "canonical:qwen-qwen2.5-coder-32b-instruct"
    val quantizationType: String,           // e.g. "Q4_K_M"
    val modelRevision: String = "main",
    val engineName: String = "llama.cpp",
    val engineVersion: String = "b3200",
    val backendName: String = "Vulkan",
    val deviceName: String = "Local Android Device",
    val contextLengthTokens: Int = 4096,
    val promptTokenCount: Int,
    val generatedTokenCount: Int,
    val ttftMs: Float,                      // Time To First Token in ms
    val prefillTokensPerSec: Float,          // Prompt processing speed (tok/s)
    val decodeTokensPerSec: Float,           // Token generation speed (tok/s)
    val totalLatencyMs: Long,
    val peakRamMb: Int = 0,
    val peakVramMb: Int = 0,
    val timestampMs: Long = System.currentTimeMillis()
)
