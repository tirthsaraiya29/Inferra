package com.inferra.domain.usecase

import com.inferra.domain.model.BenchmarkRetention
import com.inferra.domain.model.EvidenceStrength
import com.inferra.domain.model.QualityEvidence
import java.util.Locale
import kotlin.math.roundToInt

object QualityEvidenceEngine {

    /**
     * Inspects card content, README, or evaluation data from base model and quantization repo
     * to calculate benchmark retention scores compared to the baseline.
     */
    fun extractQualityEvidence(
        baseRepoId: String,
        quantRepoId: String,
        quantType: String,
        readmeText: String?,
        baseReadmeText: String? = null
    ): QualityEvidence {
        val combinedText = listOfNotNull(readmeText, baseReadmeText).joinToString("\n")
        if (combinedText.isBlank()) {
            return QualityEvidence(
                strength = EvidenceStrength.INSUFFICIENT,
                sourceRepo = quantRepoId.ifBlank { baseRepoId }
            )
        }

        val sourceUrl = if (quantRepoId.isNotBlank()) "https://huggingface.co/$quantRepoId" else "https://huggingface.co/$baseRepoId"
        val retentions = mutableListOf<BenchmarkRetention>()

        val benchmarkNames = listOf("MMLU", "GSM8K", "HumanEval", "GPQA", "ARC", "HellaSwag", "TruthfulQA", "MBPP", "Perplexity")

        for (bench in benchmarkNames) {
            val isPpl = bench.equals("Perplexity", ignoreCase = true)
            val pair = parseBenchmarkPair(combinedText, bench, quantType, isPpl)
            if (pair != null) {
                val (baseVal, quantVal) = pair
                if (baseVal > 0f && quantVal > 0f) {
                    val retention = if (isPpl) {
                        // For perplexity, lower is better. Baseline / Quant * 100
                        (baseVal / quantVal) * 100f
                    } else {
                        // Higher is better. Quant / Baseline * 100
                        (quantVal / baseVal) * 100f
                    }

                    // Format to 2 decimal places
                    val roundedRetention = (retention * 100f).roundToInt() / 100f

                    retentions.add(
                        BenchmarkRetention(
                            benchmarkName = bench,
                            quantScore = quantVal,
                            baselineScore = baseVal,
                            retentionPercentage = roundedRetention.coerceIn(0f, 120f),
                            isLowerBetter = isPpl
                        )
                    )
                }
            }
        }

        val strength = when {
            retentions.size >= 3 -> EvidenceStrength.STRONG
            retentions.size in 1..2 -> EvidenceStrength.MODERATE
            retentions.any { it.isLowerBetter } -> EvidenceStrength.LIMITED
            else -> EvidenceStrength.INSUFFICIENT
        }

        return QualityEvidence(
            retentions = retentions,
            strength = strength,
            sourceUrl = sourceUrl,
            sourceRepo = quantRepoId.ifBlank { baseRepoId },
            comparisonBaseline = "FP16 / Full-Precision Base Model"
        )
    }

    private fun parseBenchmarkPair(
        text: String,
        benchmark: String,
        quantType: String,
        isPpl: Boolean
    ): Pair<Float, Float>? {
        val lines = text.lines()
        val benchLower = benchmark.lowercase(Locale.US)
        val qTypeLower = quantType.lowercase(Locale.US)

        var baselineScore: Float? = null
        var quantScore: Float? = null

        // Look for markdown tables or metric lines containing benchmark name
        for (line in lines) {
            val lineLower = line.lowercase(Locale.US)
            if (!lineLower.contains(benchLower)) continue

            // Look for numbers in line
            val numbers = Regex("(\\d+\\.\\d+|\\d+)").findAll(line)
                .mapNotNull { it.value.toFloatOrNull() }
                .filter { it in 0.1f..100.0f }
                .toList()

            if (lineLower.contains("fp16") || lineLower.contains("bf16") || lineLower.contains("base") || lineLower.contains("orig")) {
                if (baselineScore == null && numbers.isNotEmpty()) {
                    baselineScore = numbers.first()
                }
            }

            if (lineLower.contains(qTypeLower)) {
                if (quantScore == null && numbers.isNotEmpty()) {
                    quantScore = numbers.last()
                }
            }
        }

        // If baseline score was not found in specific line, try finding a global baseline table
        if (baselineScore == null) {
            baselineScore = findGlobalBaseline(lines, benchLower)
        }

        if (baselineScore != null && quantScore != null) {
            return Pair(baselineScore, quantScore)
        }

        return null
    }

    private fun findGlobalBaseline(lines: List<String>, benchLower: String): Float? {
        for (line in lines) {
            val l = line.lowercase(Locale.US)
            if (l.contains(benchLower) && (l.contains("fp16") || l.contains("bf16") || l.contains("base") || l.contains("original") || l.contains("70b") || l.contains("32b") || l.contains("8b"))) {
                val num = Regex("(\\d+\\.\\d+)").find(line)?.value?.toFloatOrNull()
                if (num != null && num in 1.0f..100.0f) {
                    return num
                }
            }
        }
        return null
    }
}
