package com.inferra

import com.inferra.domain.model.BenchmarkDomainCategory
import com.inferra.domain.model.BenchmarkScoreUiModel
import com.inferra.domain.model.ProvenanceResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProvenanceResolverTest {

    @Test
    fun testResolveOpenWeightsProvenance() {
        val prov = ProvenanceResolver.resolveProvenance(
            modelId = "meta/llama-3.3-70b-instruct",
            organization = "Meta",
            isOpenWeights = true,
            licenseName = "Llama Community License"
        )

        assertEquals("Meta", prov.organization)
        assertEquals("meta-llama/llama-3.3-70b-instruct", prov.repositoryPath)
        assertEquals("https://huggingface.co/meta-llama/llama-3.3-70b-instruct", prov.officialReleaseUrl)
        assertTrue(prov.isOpenWeights)
    }

    @Test
    fun testResolveProprietaryProvenance() {
        val prov = ProvenanceResolver.resolveProvenance(
            modelId = "anthropic/claude-3-5-sonnet",
            organization = "Anthropic",
            isOpenWeights = false,
            licenseName = "Proprietary"
        )

        assertEquals("https://www.anthropic.com/news", prov.officialReleaseUrl)
        assertFalse(prov.isOpenWeights)
    }

    @Test
    fun testResolveArtifactUrl() {
        val url = ProvenanceResolver.resolveArtifactUrl(
            repositoryId = "bartowski/Llama-3.3-70B-Instruct-GGUF",
            filePath = "Llama-3.3-70B-Instruct-Q4_K_M.gguf"
        )

        assertEquals("https://huggingface.co/bartowski/Llama-3.3-70B-Instruct-GGUF/blob/main/Llama-3.3-70B-Instruct-Q4_K_M.gguf", url)
    }

    @Test
    fun testBenchmarkScoreUiModelFormatting() {
        val mmlu = BenchmarkScoreUiModel(
            benchmarkId = "mmlu_pro",
            name = "MMLU-Pro",
            domain = "REASONING",
            metricName = "Accuracy",
            rawScore = 71.2,
            normalizedScore = 0.712,
            measurementType = "LEADERBOARD_MEASURED"
        )
        assertEquals("71.2", mmlu.scoreFormatted)
        assertEquals(BenchmarkDomainCategory.REASONING_SCIENCE, mmlu.category)

        val unmeasured = BenchmarkScoreUiModel(
            benchmarkId = "simpleqa",
            name = "SimpleQA",
            domain = "FACTUALITY",
            metricName = "Correctness Rate",
            rawScore = null,
            normalizedScore = null,
            measurementType = "UNMEASURED"
        )
        assertEquals("—", unmeasured.scoreFormatted)
        assertEquals(BenchmarkDomainCategory.FACTUALITY, unmeasured.category)

        val elo = BenchmarkScoreUiModel(
            benchmarkId = "arena_elo",
            name = "Chatbot Arena Elo",
            domain = "HUMAN_PREFERENCE",
            metricName = "Bradley-Terry Elo",
            rawScore = 1362.4,
            normalizedScore = 0.885,
            measurementType = "HUMAN_PREFERENCE"
        )
        assertEquals("1362", elo.scoreFormatted)
        assertEquals(BenchmarkDomainCategory.HUMAN_PREFERENCE, elo.category)
    }
}
