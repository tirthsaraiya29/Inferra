package com.inferra.domain.model

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
