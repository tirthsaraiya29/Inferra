package com.inferra.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
enum class EngineState {
    UNLOADED,
    LOADING,
    READY,
    GENERATING,
    ERROR
}

@Serializable
enum class AttentionArchitecture {
    MHA,               // Multi-Head Attention
    GQA,               // Grouped-Query Attention
    MQA,               // Multi-Query Attention
    MLA,               // Multi-head Latent Attention (DeepSeek V2/V3)
    SLIDING_WINDOW,    // Sliding Window Attention (Mistral)
    STATE_SPACE_SSM,   // Mamba / Recurrent SSM
    HYBRID             // Hybrid Architecture
}

@Immutable
@Serializable
data class EngineCapabilities(
    val supportedQuantizations: List<String>,
    val maxContextLength: Int,
    val gpuAccelerationSupported: Boolean,
    val backendName: String                     // e.g. "llama.cpp (Vulkan)", "llama.cpp (CPU/NEON)"
)

@Immutable
@Serializable
data class InferenceParameters(
    val prompt: String,
    val maxTokens: Int = 256,
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val topK: Int = 40,
    val stopSequences: List<String> = emptyList()
)

@Immutable
@Serializable
data class GenerationChunk(
    val token: String,
    val isFinished: Boolean = false,
    val promptTokens: Int = 0,
    val generatedTokens: Int = 0,
    val elapsedMs: Long = 0L
)

interface InferenceEngine {
    suspend fun loadModel(modelPath: String, params: Map<String, Any> = emptyMap()): Boolean
    suspend fun unloadModel()
    fun generate(params: InferenceParameters): Flow<GenerationChunk>
    fun getEngineState(): EngineState
    fun getCapabilities(): EngineCapabilities
}
