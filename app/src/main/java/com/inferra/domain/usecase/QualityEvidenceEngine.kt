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
            val pair = parseBenchmarkPair(combinedText, bench, quantType)
            if (pair != null) {
                val (baseVal, quantVal) = pair
                if (baseVal > 0f && quantVal > 0f) {
                    val retention = if (isPpl) {
                        (baseVal / quantVal) * 100f
                    } else {
                        (quantVal / baseVal) * 100f
                    }

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
        quantType: String
    ): Pair<Float, Float>? {
        val lines = text.lines()
        val benchLower = benchmark.lowercase(Locale.US)
        val qTypeLower = quantType.lowercase(Locale.US)

        // 1. Try Markdown Table Parsing
        val tableResult = parseFromMarkdownTable(lines, benchLower, qTypeLower)
        if (tableResult != null) return tableResult

        // 2. Try Line-by-Line Parsing
        var baselineScore: Float? = null
        var quantScore: Float? = null

        for (line in lines) {
            val lineLower = line.lowercase(Locale.US)

            val numbers = Regex("(\\d+\\.\\d+|\\d+)").findAll(line)
                .mapNotNull { it.value.toFloatOrNull() }
                .filter { it in 0.1f..100.0f }
                .toList()

            if (numbers.isEmpty()) continue

            if (lineLower.contains(benchLower)) {
                if (lineLower.contains("fp16") || lineLower.contains("bf16") || lineLower.contains("base") || lineLower.contains("orig")) {
                    if (baselineScore == null) baselineScore = numbers.first()
                }
                if (lineLower.contains(qTypeLower)) {
                    if (quantScore == null) quantScore = numbers.last()
                }
            } else {
                if ((lineLower.contains("fp16") || lineLower.contains("bf16") || lineLower.contains("base")) && baselineScore == null) {
                    baselineScore = numbers.firstOrNull()
                }
                if (lineLower.contains(qTypeLower) && quantScore == null) {
                    quantScore = numbers.lastOrNull()
                }
            }
        }

        if (baselineScore != null && quantScore != null) {
            return Pair(baselineScore, quantScore)
        }

        return null
    }

    private fun parseFromMarkdownTable(
        lines: List<String>,
        benchLower: String,
        qTypeLower: String
    ): Pair<Float, Float>? {
        var benchColIdx = -1
        var baselineScore: Float? = null
        var quantScore: Float? = null

        for (line in lines) {
            val trimmed = line.trim()
            if (!trimmed.startsWith("|") || !trimmed.endsWith("|")) continue

            val cells = trimmed.split("|").map { it.trim().lowercase(Locale.US) }.filter { it.isNotEmpty() }

            // Check if this is header line containing benchmark name
            if (cells.any { it.contains(benchLower) }) {
                benchColIdx = cells.indexOfFirst { it.contains(benchLower) }
                continue
            }

            if (benchColIdx >= 0) {
                val rowCells = trimmed.split("|").map { it.trim() }.filter { it.isNotEmpty() }
                if (rowCells.size > benchColIdx) {
                    val rowText = rowCells.joinToString(" ").lowercase(Locale.US)
                    val cellVal = Regex("(\\d+\\.\\d+|\\d+)").find(rowCells[benchColIdx])?.value?.toFloatOrNull()

                    if (cellVal != null && cellVal in 0.1f..100.0f) {
                        if (rowText.contains("fp16") || rowText.contains("bf16") || rowText.contains("base") || rowText.contains("orig")) {
                            if (baselineScore == null) baselineScore = cellVal
                        }
                        if (rowText.contains(qTypeLower)) {
                            if (quantScore == null) quantScore = cellVal
                        }
                    }
                }
            }
        }

        if (baselineScore != null && quantScore != null) {
            return Pair(baselineScore, quantScore)
        }

        return null
    }
}
