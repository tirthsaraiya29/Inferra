package com.inferra.domain.model

enum class EvidenceStrength {
    STRONG,      // Multiple standard benchmarks (e.g. MMLU, GSM8K, HumanEval) directly compared
    MODERATE,    // 1-2 standard benchmarks compared against baseline
    LIMITED,     // Perplexity / PPL or single metric compared
    INSUFFICIENT // Insufficient quality evidence
}

data class BenchmarkRetention(
    val benchmarkName: String,       // e.g. "MMLU", "GSM8K", "HumanEval", "GPQA", "ARC", "Perplexity"
    val quantScore: Float,           // e.g. 81.4
    val baselineScore: Float,        // e.g. 82.1
    val retentionPercentage: Float,   // e.g. 99.15 (quantScore / baselineScore * 100)
    val isLowerBetter: Boolean = false // true for perplexity/PPL
)

data class QualityEvidence(
    val retentions: List<BenchmarkRetention> = emptyList(),
    val strength: EvidenceStrength = EvidenceStrength.INSUFFICIENT,
    val sourceUrl: String = "",
    val sourceRepo: String = "",
    val comparisonBaseline: String = "FP16 Base Model"
)
