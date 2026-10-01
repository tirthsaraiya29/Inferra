package com.inferra.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Serializable
enum class MeasurementType {
    NUMERIC_SCORE,
    PERPLEXITY,
    PASS_RATE,
    ELO_RATING
}

@Serializable
enum class BenchmarkCategory {
    CODING,
    REASONING,
    MATH,
    SCIENCE,
    AGENTIC,
    VISION,
    LONG_CONTEXT,
    INSTRUCTION_FOLLOWING,
    TOOL_CALLING
}

@Serializable
enum class EvaluationMethodologyType {
    EXACT_MATCH,
    PASS_AT_K,
    LLM_AS_JUDGE,
    HUMAN_EVAL,
    PERPLEXITY
}

@Serializable
enum class ProvenanceType {
    SELF_REPORTED_AUTHOR,
    INDEPENDENT_BENCHMARK,
    COMMUNITY_MEASURED,
    INFERRA_ON_DEVICE
}

@Immutable
@Serializable
data class BenchmarkDefinition(
    val id: String,                         // e.g. "bench:mmlu"
    val name: String,                       // e.g. "MMLU"
    val shortName: String,                  // e.g. "MMLU"
    val category: BenchmarkCategory,
    val description: String,
    val primaryMetric: MeasurementType = MeasurementType.NUMERIC_SCORE,
    val higherIsBetter: Boolean = true
)

@Immutable
@Serializable
data class BenchmarkVersion(
    val id: String,                         // e.g. "benchver:mmlu-v1.0"
    val benchmarkId: String,
    val versionName: String,                // e.g. "v1.0 (5-shot)"
    val releaseDate: String = "",
    val specificationUrl: String = ""
)

@Immutable
@Serializable
data class EvaluationMethodology(
    val id: String,
    val name: String,
    val type: EvaluationMethodologyType,
    val details: String = ""
)

@Immutable
@Serializable
data class EvaluationConfiguration(
    val id: String,
    val numShots: Int = 0,
    val promptTemplate: String = "",
    val temperature: Float = 0.0f,
    val systemPrompt: String = "",
    val decodingStrategy: String = "Greedy",
    val environmentDetails: String = ""
)

@Immutable
@Serializable
data class BenchmarkProvenance(
    val provenanceType: ProvenanceType,
    val publisher: String,                  // e.g. "Hugging Face Open LLM Leaderboard", "LiveBench", "Author Paper"
    val sourceUrl: String = "",
    val verifiedByInferra: Boolean = false,
    val retrievalDate: String = ""
)

@Immutable
@Serializable
data class BenchmarkResult(
    val id: String,
    val canonicalId: String,
    val modelRevision: String = "main",
    val quantizationType: String = "FP16",  // e.g. "FP16", "Q4_K_M"
    val benchmarkVersionId: String,
    val methodologyId: String,
    val configurationId: String,
    val score: Float,
    val maxScore: Float = 100.0f,
    val scoreNormalized: Float = score,
    val provenance: BenchmarkProvenance
)

@Immutable
@Serializable
data class ComparabilityCheckResult(
    val isComparable: Boolean,
    val warnings: List<String> = emptyList(),
    val comparisonNotes: String = ""
)
