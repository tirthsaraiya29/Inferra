package com.inferra

import com.inferra.domain.model.AiModel
import com.inferra.domain.model.AttentionArchitecture
import com.inferra.domain.model.CapabilityMatrix
import com.inferra.domain.model.LicenseType
import com.inferra.domain.model.LineageInfo
import com.inferra.domain.model.Modality
import com.inferra.domain.model.ModelTask
import com.inferra.domain.model.QuantizationInfo
import com.inferra.domain.usecase.MemoryEstimationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryEstimationEngineTest {

    private val testModel = AiModel(
        id = "canonical:qwen-qwen2.5-coder-32b-instruct",
        name = "Qwen2.5-Coder-32B-Instruct",
        author = "Qwen",
        description = "Test model",
        architecture = "Qwen2ForCausalLM",
        totalParamsBillion = 32.5f,
        activeParamsBillion = 32.5f,
        isMoe = false,
        contextLengthTokens = 131072,
        modalities = listOf(Modality.TEXT),
        tasks = listOf(ModelTask.GENERAL_TEXT),
        license = LicenseType.APACHE_2,
        licenseName = "Apache 2.0",
        downloadsCount = 1000,
        likesCount = 500,
        updatedAt = "2025-01-01",
        quantizations = emptyList(),
        benchmarks = emptyList(),
        capabilities = CapabilityMatrix(80f, 80f, 80f, 0f, 80f, 80f, 80f, 80f),
        lineage = LineageInfo(),
        repoUrl = "https://huggingface.co"
    )

    @Test
    fun testGqaMemoryEstimationBreakdown() {
        val quantQ4 = QuantizationInfo(
            id = "q4",
            format = "GGUF",
            quantType = "Q4_K_M",
            fileSizeBytes = 19500000000L,
            downloadUrl = "",
            fileName = "model.gguf",
            sourceRepo = "test/qwen-gguf",
            estimatedRamMb = 19500,
            estimatedVramMb = 18000,
            relativeQualityScore = 95f
        )

        val breakdown = MemoryEstimationEngine.estimate(
            model = testModel,
            quantization = quantQ4,
            contextLengthTokens = 32768
        )

        assertNotNull(breakdown)
        assertEquals(AttentionArchitecture.GQA, breakdown.attentionArchitecture)
        assertTrue("Weight memory should be derived from GGUF size (~18596 MB)", breakdown.weightMemoryMb > 18000)
        assertTrue("KV Cache memory should be positive for 32K context", breakdown.kvCacheMemoryMb > 0)
        assertTrue("Total memory in GB should be greater than 19 GB", breakdown.totalMemoryGb > 19f)
    }
}
