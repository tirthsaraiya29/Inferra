package com.inferra.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * Data provenance classification as mandated by product philosophy.
 */
@Serializable
enum class DataCategory {
    SOURCE_FACT,     // Directly from repository metadata
    MEASUREMENT,     // Empirical benchmark/throughput measurement
    DERIVED          // App-calculated intelligence (hardware fit, estimated speed)
}

@Serializable
enum class Modality {
    TEXT, VISION, AUDIO, MULTIMODAL, CODE
}

@Serializable
enum class ModelTask {
    GENERAL_TEXT, CODING, REASONING, MATH, VISION, AGENTIC, TOOL_CALLING, EMBEDDINGS
}

@Serializable
enum class LicenseType {
    APACHE_2, MIT, LLAMA_COMMUNITY, QWEN_RESEARCH, PERMISSIVE_OTHER, RESTRICTED
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
data class QuantizationInfo(
    val id: String,
    val format: String,             // GGUF, AWQ, EXL2, GPTQ, FP16, BF16
    val quantType: String,          // Q4_K_M, Q8_0, Q5_K_M, FP16, etc.
    val fileSizeBytes: Long,
    val downloadUrl: String,
    val fileName: String,
    val sourceRepo: String = "",
    val estimatedRamMb: Int,
    val estimatedVramMb: Int,
    val relativeQualityScore: Float, // 0.0 to 100.0
    val qualityEvidence: QualityEvidence = QualityEvidence()
)

@Immutable
@Serializable
data class BenchmarkScore(
    val name: String,               // MMLU, HumanEval, GSM8K, MATH, LiveBench, GPQA
    val score: Float,               // e.g. 84.5
    val maxScore: Float = 100f,
    val category: String,
    val provenance: String,          // "Author Published", "Independent Benchmark", "Community Measured"
    val dataCategory: DataCategory = DataCategory.MEASUREMENT
)

@Immutable
@Serializable
data class CapabilityMatrix(
    val coding: Float,             // 0 to 100
    val reasoning: Float,          // 0 to 100
    val math: Float,               // 0 to 100
    val vision: Float,             // 0 to 100
    val agentic: Float,            // 0 to 100
    val toolCalling: Float,        // 0 to 100
    val multilingual: Float,       // 0 to 100
    val longContext: Float         // 0 to 100
)

@Immutable
@Serializable
data class LineageInfo(
    val baseModelId: String? = null,
    val parentModelId: String? = null,
    val fineTunesCount: Int = 0,
    val distilledFrom: String? = null,
    val childModelIds: List<String> = emptyList()
)

@Immutable
@Serializable
data class AiModel(
    val id: String,                         // e.g. "Qwen/Qwen2.5-Coder-32B-Instruct"
    val name: String,                       // e.g. "Qwen2.5-Coder-32B-Instruct"
    val author: String,                     // e.g. "Qwen"
    val description: String,
    val architecture: String,               // e.g. "Qwen2ForCausalLM", "LlamaForCausalLM", "MoE"
    val totalParamsBillion: Float,          // e.g. 32.5f
    val activeParamsBillion: Float,         // e.g. 3.0f for MoE or 32.5f
    val isMoe: Boolean = false,
    val contextLengthTokens: Int,           // e.g. 131072 (128K)
    val modalities: List<Modality>,
    val tasks: List<ModelTask>,
    val license: LicenseType,
    val licenseName: String,
    val downloadsCount: Long,
    val likesCount: Long,
    val updatedAt: String,                  // ISO string or formatted date
    val quantizations: List<QuantizationInfo>,
    val capabilities: CapabilityMatrix,
    val lineage: LineageInfo,
    val isFeatured: Boolean = false,
    val isTrending: Boolean = false,
    val isNew: Boolean = false,
    val repoUrl: String,
    val avatarUrl: String? = null
)
