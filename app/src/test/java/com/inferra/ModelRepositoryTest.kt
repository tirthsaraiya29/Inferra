package com.inferra

import com.inferra.domain.model.AiModel
import com.inferra.domain.model.CapabilityMatrix
import com.inferra.domain.model.LicenseType
import com.inferra.domain.model.LineageInfo
import com.inferra.domain.model.Modality
import com.inferra.domain.model.ModelTask
import com.inferra.domain.model.QuantizationInfo
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ModelRepositoryTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun testAiModelSerializationRoundTripFidelity() {
        val originalModel = AiModel(
            id = "Qwen/Qwen2.5-Coder-32B-Instruct",
            name = "Qwen2.5-Coder-32B-Instruct",
            author = "Qwen",
            description = "State of the art open code model",
            architecture = "Qwen2ForCausalLM",
            totalParamsBillion = 32.5f,
            activeParamsBillion = 32.5f,
            isMoe = false,
            contextLengthTokens = 131072,
            modalities = listOf(Modality.TEXT, Modality.CODE),
            tasks = listOf(ModelTask.CODING, ModelTask.REASONING),
            license = LicenseType.QWEN_RESEARCH,
            licenseName = "Qwen Research License",
            downloadsCount = 1250000L,
            likesCount = 8500L,
            updatedAt = "2025-02-15",
            quantizations = listOf(
                QuantizationInfo(
                    id = "quant-q4",
                    format = "GGUF",
                    quantType = "Q4_K_M",
                    fileSizeBytes = 19500000000L,
                    downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-Coder-32B-Instruct-GGUF/resolve/main/q4_k_m.gguf",
                    fileName = "qwen2.5-coder-32b-instruct-q4_k_m.gguf",
                    sourceRepo = "bartowski/Qwen2.5-Coder-32B-Instruct-GGUF",
                    estimatedRamMb = 21000,
                    estimatedVramMb = 19500,
                    relativeQualityScore = 94.5f
                )
            ),
            capabilities = CapabilityMatrix(
                coding = 92f,
                reasoning = 88f,
                math = 85f,
                vision = 0f,
                agentic = 86f,
                toolCalling = 84f,
                multilingual = 80f,
                longContext = 90f
            ),
            lineage = LineageInfo(
                baseModelId = "Qwen/Qwen2.5-Coder-32B",
                fineTunesCount = 12
            ),
            isFeatured = true,
            isTrending = true,
            isNew = false,
            repoUrl = "https://huggingface.co/Qwen/Qwen2.5-Coder-32B-Instruct"
        )

        val jsonString = json.encodeToString(originalModel)
        assertNotNull(jsonString)

        val deserializedModel = json.decodeFromString<AiModel>(jsonString)

        assertEquals(originalModel.id, deserializedModel.id)
        assertEquals(originalModel.name, deserializedModel.name)
        assertEquals(originalModel.license, deserializedModel.license)
        assertEquals(originalModel.licenseName, deserializedModel.licenseName)
        assertEquals(originalModel.modalities, deserializedModel.modalities)
        assertEquals(originalModel.tasks, deserializedModel.tasks)
        assertEquals(originalModel.capabilities, deserializedModel.capabilities)
        assertEquals(originalModel.lineage, deserializedModel.lineage)
        assertEquals(originalModel.quantizations.size, deserializedModel.quantizations.size)
        assertEquals(originalModel.quantizations.first().quantType, deserializedModel.quantizations.first().quantType)
    }
}
