package com.inferra.domain.usecase

import com.inferra.domain.model.BenchmarkCategory
import com.inferra.domain.model.BenchmarkDefinition
import com.inferra.domain.model.BenchmarkProvenance
import com.inferra.domain.model.BenchmarkResult
import com.inferra.domain.model.BenchmarkVersion
import com.inferra.domain.model.ComparabilityCheckResult
import com.inferra.domain.model.EvaluationConfiguration
import com.inferra.domain.model.EvaluationMethodology
import com.inferra.domain.model.EvaluationMethodologyType
import com.inferra.domain.model.MeasurementType
import com.inferra.domain.model.ProvenanceType

object BenchmarkRegistry {

    val MMLU_DEF = BenchmarkDefinition("bench:mmlu", "MMLU", "MMLU", BenchmarkCategory.REASONING, "Massive Multitask Language Understanding across 57 subjects.", MeasurementType.NUMERIC_SCORE, true)
    val HUMANEVAL_DEF = BenchmarkDefinition("bench:humaneval", "HumanEval", "HumanEval", BenchmarkCategory.CODING, "Python code synthesis evaluation created by OpenAI.", MeasurementType.PASS_RATE, true)
    val GSM8K_DEF = BenchmarkDefinition("bench:gsm8k", "GSM8K", "GSM8K", BenchmarkCategory.MATH, "Grade School Math 8K word problems.", MeasurementType.NUMERIC_SCORE, true)
    val MATH_DEF = BenchmarkDefinition("bench:math", "MATH", "MATH", BenchmarkCategory.MATH, "Challenging competition-level mathematics problems.", MeasurementType.NUMERIC_SCORE, true)
    val GPQA_DEF = BenchmarkDefinition("bench:gpqa", "GPQA", "GPQA", BenchmarkCategory.SCIENCE, "Graduate-Level Google-Proof Q&A Benchmark.", MeasurementType.NUMERIC_SCORE, true)
    val LIVEBENCH_DEF = BenchmarkDefinition("bench:livebench", "LiveBench", "LiveBench", BenchmarkCategory.REASONING, "Contamination-free LLM benchmark updated monthly.", MeasurementType.NUMERIC_SCORE, true)
    val CHATBOT_ARENA_DEF = BenchmarkDefinition("bench:arena", "Chatbot Arena ELO", "Arena ELO", BenchmarkCategory.INSTRUCTION_FOLLOWING, "LMSYS Crowdsourced Human Preference Leaderboard.", MeasurementType.ELO_RATING, true)
    val SWE_BENCH_DEF = BenchmarkDefinition("bench:swe-bench", "SWE-bench Verified", "SWE-bench", BenchmarkCategory.AGENTIC, "Software Engineering resolve rate on real GitHub issues.", MeasurementType.PASS_RATE, true)

    val DEFINITIONS = listOf(
        MMLU_DEF, HUMANEVAL_DEF, GSM8K_DEF, MATH_DEF, GPQA_DEF, LIVEBENCH_DEF, CHATBOT_ARENA_DEF, SWE_BENCH_DEF
    )

    val V1_5_SHOT = BenchmarkVersion("benchver:standard-5shot", "bench:generic", "Standard 5-shot", "2024-01-01")
    val V1_ZERO_SHOT = BenchmarkVersion("benchver:standard-0shot", "bench:generic", "Standard 0-shot", "2024-01-01")

    val METH_EXACT_MATCH = EvaluationMethodology("meth:exact-match", "Exact Match Parsing", EvaluationMethodologyType.EXACT_MATCH)
    val METH_PASS_AT_1 = EvaluationMethodology("meth:pass-at-1", "Pass@1 Execution", EvaluationMethodologyType.PASS_AT_K)
    val METH_LLM_JUDGE = EvaluationMethodology("meth:llm-judge", "LLM-as-a-Judge (GPT-4o)", EvaluationMethodologyType.LLM_AS_JUDGE)

    val CONFIG_5SHOT_GREEDY = EvaluationConfiguration("config:5shot-greedy", 5, temperature = 0.0f, decodingStrategy = "Greedy")
    val CONFIG_0SHOT_GREEDY = EvaluationConfiguration("config:0shot-greedy", 0, temperature = 0.0f, decodingStrategy = "Greedy")

    fun getStandardBenchmarksForModel(canonicalId: String): List<BenchmarkResult> {
        return when (canonicalId) {
            "canonical:qwen-qwen2.5-coder-32b-instruct" -> listOf(
                BenchmarkResult(
                    id = "res-qwen-coder-humaneval",
                    canonicalId = canonicalId,
                    benchmarkVersionId = HUMANEVAL_DEF.id,
                    methodologyId = METH_PASS_AT_1.id,
                    configurationId = CONFIG_0SHOT_GREEDY.id,
                    score = 92.7f,
                    provenance = BenchmarkProvenance(
                        provenanceType = ProvenanceType.INDEPENDENT_BENCHMARK,
                        publisher = "Hugging Face Open LLM Leaderboard",
                        sourceUrl = "https://huggingface.co/spaces/open-llm-leaderboard/open_llm_leaderboard",
                        verifiedByInferra = true,
                        retrievalDate = "2025-02-15"
                    )
                ),
                BenchmarkResult(
                    id = "res-qwen-coder-math",
                    canonicalId = canonicalId,
                    benchmarkVersionId = MATH_DEF.id,
                    methodologyId = METH_EXACT_MATCH.id,
                    configurationId = CONFIG_0SHOT_GREEDY.id,
                    score = 78.4f,
                    provenance = BenchmarkProvenance(
                        provenanceType = ProvenanceType.INDEPENDENT_BENCHMARK,
                        publisher = "LiveBench Leaderboard",
                        verifiedByInferra = true,
                        retrievalDate = "2025-02-15"
                    )
                ),
                BenchmarkResult(
                    id = "res-qwen-coder-mmlu",
                    canonicalId = canonicalId,
                    benchmarkVersionId = MMLU_DEF.id,
                    methodologyId = METH_EXACT_MATCH.id,
                    configurationId = CONFIG_5SHOT_GREEDY.id,
                    score = 83.1f,
                    provenance = BenchmarkProvenance(
                        provenanceType = ProvenanceType.SELF_REPORTED_AUTHOR,
                        publisher = "Alibaba Qwen Technical Report",
                        verifiedByInferra = false
                    )
                ),
                BenchmarkResult(
                    id = "res-qwen-coder-swe",
                    canonicalId = canonicalId,
                    benchmarkVersionId = SWE_BENCH_DEF.id,
                    methodologyId = METH_PASS_AT_1.id,
                    configurationId = CONFIG_0SHOT_GREEDY.id,
                    score = 42.1f,
                    provenance = BenchmarkProvenance(
                        provenanceType = ProvenanceType.INDEPENDENT_BENCHMARK,
                        publisher = "SWE-bench Official Leaderboard",
                        verifiedByInferra = true
                    )
                )
            )
            "canonical:meta-llama-3.3-70b-instruct" -> listOf(
                BenchmarkResult(
                    id = "res-llama-3.3-mmlu",
                    canonicalId = canonicalId,
                    benchmarkVersionId = MMLU_DEF.id,
                    methodologyId = METH_EXACT_MATCH.id,
                    configurationId = CONFIG_5SHOT_GREEDY.id,
                    score = 88.6f,
                    provenance = BenchmarkProvenance(
                        provenanceType = ProvenanceType.INDEPENDENT_BENCHMARK,
                        publisher = "Hugging Face Open LLM Leaderboard",
                        verifiedByInferra = true
                    )
                ),
                BenchmarkResult(
                    id = "res-llama-3.3-humaneval",
                    canonicalId = canonicalId,
                    benchmarkVersionId = HUMANEVAL_DEF.id,
                    methodologyId = METH_PASS_AT_1.id,
                    configurationId = CONFIG_0SHOT_GREEDY.id,
                    score = 88.4f,
                    provenance = BenchmarkProvenance(
                        provenanceType = ProvenanceType.INDEPENDENT_BENCHMARK,
                        publisher = "Hugging Face Open LLM Leaderboard",
                        verifiedByInferra = true
                    )
                ),
                BenchmarkResult(
                    id = "res-llama-3.3-gpqa",
                    canonicalId = canonicalId,
                    benchmarkVersionId = GPQA_DEF.id,
                    methodologyId = METH_EXACT_MATCH.id,
                    configurationId = CONFIG_0SHOT_GREEDY.id,
                    score = 52.8f,
                    provenance = BenchmarkProvenance(
                        provenanceType = ProvenanceType.INDEPENDENT_BENCHMARK,
                        publisher = "Chatbot Arena / LMSYS",
                        verifiedByInferra = true
                    )
                ),
                BenchmarkResult(
                    id = "res-llama-3.3-arena",
                    canonicalId = canonicalId,
                    benchmarkVersionId = CHATBOT_ARENA_DEF.id,
                    methodologyId = METH_LLM_JUDGE.id,
                    configurationId = CONFIG_0SHOT_GREEDY.id,
                    score = 1290f,
                    maxScore = 1500f,
                    provenance = BenchmarkProvenance(
                        provenanceType = ProvenanceType.COMMUNITY_MEASURED,
                        publisher = "LMSYS Chatbot Arena Leaderboard",
                        verifiedByInferra = true
                    )
                )
            )
            else -> emptyList()
        }
    }

    fun checkComparability(resultA: BenchmarkResult, resultB: BenchmarkResult): ComparabilityCheckResult {
        val warnings = mutableListOf<String>()

        if (resultA.benchmarkVersionId != resultB.benchmarkVersionId) {
            warnings.add("Different benchmark definitions or version IDs (${resultA.benchmarkVersionId} vs ${resultB.benchmarkVersionId})")
        }

        if (resultA.methodologyId != resultB.methodologyId) {
            warnings.add("Evaluation methodology mismatch (${resultA.methodologyId} vs ${resultB.methodologyId})")
        }

        if (resultA.configurationId != resultB.configurationId) {
            warnings.add("Prompt shot count / decoding configuration mismatch (${resultA.configurationId} vs ${resultB.configurationId})")
        }

        if (resultA.provenance.provenanceType != resultB.provenance.provenanceType) {
            warnings.add("Provenance source discrepancy (${resultA.provenance.provenanceType} vs ${resultB.provenance.provenanceType})")
        }

        return ComparabilityCheckResult(
            isComparable = warnings.isEmpty(),
            warnings = warnings,
            comparisonNotes = if (warnings.isEmpty()) {
                "Directly comparable across identical benchmark methodology, configuration, and shot count."
            } else {
                "Scores may not be directly comparable due to methodology or prompt differences."
            }
        )
    }
}
