package com.inferra

import com.inferra.domain.model.EvidenceStrength
import com.inferra.domain.usecase.QualityEvidenceEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QualityEvidenceEngineTest {

    @Test
    fun testExtractsMmluAndGsm8kRetention() {
        val readme = """
            # Evaluation Benchmarks
            | Model | Precision | MMLU | GSM8K | HumanEval |
            | Qwen2.5-32B | FP16 | 82.1 | 85.0 | 78.5 |
            | Qwen2.5-32B-Q4_K_M | Q4_K_M | 81.4 | 83.5 | 77.8 |
        """.trimIndent()

        val evidence = QualityEvidenceEngine.extractQualityEvidence(
            baseRepoId = "Qwen/Qwen2.5-32B-Instruct",
            quantRepoId = "bartowski/Qwen2.5-32B-Instruct-GGUF",
            quantType = "Q4_K_M",
            readmeText = readme
        )

        assertNotNull(evidence)
        assertTrue(evidence.retentions.isNotEmpty())

        val mmlu = evidence.retentions.find { it.benchmarkName == "MMLU" }
        assertNotNull(mmlu)
        // 81.4 / 82.1 * 100 = 99.15%
        assertTrue("MMLU retention should be around 99%", mmlu!!.retentionPercentage in 98f..100f)

        val gsm8k = evidence.retentions.find { it.benchmarkName == "GSM8K" }
        assertNotNull(gsm8k)
        // 83.5 / 85.0 * 100 = 98.23%
        assertTrue("GSM8K retention should be around 98%", gsm8k!!.retentionPercentage in 97f..100f)

        assertEquals(EvidenceStrength.STRONG, evidence.strength)
    }

    @Test
    fun testHandlesMissingEvidenceGracefully() {
        val evidence = QualityEvidenceEngine.extractQualityEvidence(
            baseRepoId = "some/unknown-model",
            quantRepoId = "some/unknown-model-gguf",
            quantType = "Q4_K_M",
            readmeText = "No benchmark tables in this readme file."
        )

        assertNotNull(evidence)
        assertTrue(evidence.retentions.isEmpty())
        assertEquals(EvidenceStrength.INSUFFICIENT, evidence.strength)
    }
}
