package com.inferra

import com.inferra.domain.model.AiModel
import com.inferra.domain.model.CapabilityMatrix
import com.inferra.domain.model.DeviceType
import com.inferra.domain.model.FitGrade
import com.inferra.domain.model.HardwareProfile
import com.inferra.domain.model.LicenseType
import com.inferra.domain.model.LineageInfo
import com.inferra.domain.model.Modality
import com.inferra.domain.model.ModelTask
import com.inferra.domain.model.QuantizationInfo
import com.inferra.domain.usecase.HardwareFitCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class HardwareFitCalculatorTest {

    private val test32BModel = AiModel(
        id = "test/model-32b",
        name = "Model-32B",
        author = "TestAuthor",
        description = "Test model",
        architecture = "Transformer",
        totalParamsBillion = 32.5f,
        activeParamsBillion = 32.5f,
        isMoe = false,
        contextLengthTokens = 32768,
        modalities = listOf(Modality.TEXT),
        tasks = listOf(ModelTask.GENERAL_TEXT),
        license = LicenseType.APACHE_2,
        licenseName = "Apache 2.0",
        downloadsCount = 1000,
        likesCount = 500,
        updatedAt = "2025-01-01",
        quantizations = listOf(
            QuantizationInfo(
                id = "q4",
                format = "GGUF",
                quantType = "Q4_K_M",
                fileSizeBytes = 19000000000L,
                downloadUrl = "",
                fileName = "model.gguf",
                sourceRepo = "test/model-32b-gguf",
                estimatedRamMb = 19000,
                estimatedVramMb = 18000,
                relativeQualityScore = 95f
            )
        ),
        capabilities = CapabilityMatrix(80f, 80f, 80f, 0f, 80f, 80f, 80f, 80f),
        lineage = LineageInfo(),
        repoUrl = "https://huggingface.co"
    )

    private val test70BModel = AiModel(
        id = "test/model-70b",
        name = "Model-70B",
        author = "TestAuthor",
        description = "Test model 70B",
        architecture = "Transformer",
        totalParamsBillion = 70.0f,
        activeParamsBillion = 70.0f,
        isMoe = false,
        contextLengthTokens = 32768,
        modalities = listOf(Modality.TEXT),
        tasks = listOf(ModelTask.GENERAL_TEXT),
        license = LicenseType.APACHE_2,
        licenseName = "Apache 2.0",
        downloadsCount = 1000,
        likesCount = 500,
        updatedAt = "2025-01-01",
        quantizations = listOf(
            QuantizationInfo(
                id = "q4",
                format = "GGUF",
                quantType = "Q4_K_M",
                fileSizeBytes = 42000000000L,
                downloadUrl = "",
                fileName = "model.gguf",
                sourceRepo = "test/model-70b-gguf",
                estimatedRamMb = 42000,
                estimatedVramMb = 40000,
                relativeQualityScore = 95f
            )
        ),
        capabilities = CapabilityMatrix(80f, 80f, 80f, 0f, 80f, 80f, 80f, 80f),
        lineage = LineageInfo(),
        repoUrl = "https://huggingface.co"
    )

    @Test
    fun test32BModelFitsIn32GBVramWithQ4() {
        val rtx6000 = HardwareProfile(
            id = "test-6000",
            name = "Test RTX 6000 Ada",
            deviceType = DeviceType.WORKSTATION_SERVER,
            cpuName = "Threadripper PRO",
            gpuName = "RTX 6000 Ada",
            vramGb = 32.0f,
            ramGb = 128.0f,
            osName = "Ubuntu 24.04",
            preferredRuntime = "llama.cpp"
        )

        val quantQ4 = test32BModel.quantizations.first { it.quantType == "Q4_K_M" }
        val result = HardwareFitCalculator.calculate(test32BModel, quantQ4, rtx6000)

        assertEquals(FitGrade.EXCELLENT, result.fitGrade)
        assertEquals(100, result.offloadPercentage)
        assertNotNull(result.explanation)
        org.junit.Assert.assertTrue(result.estimatedTokensPerSec > 0f)
        org.junit.Assert.assertTrue(result.estimatedTtftMs > 0f)
    }

    @Test
    fun test70BModelOn12GBVramIsBorderlineWithOffload() {
        val rtx4070 = HardwareProfile(
            id = "test-4070",
            name = "Test RTX 4070",
            deviceType = DeviceType.DESKTOP_PC,
            cpuName = "i7-13700K",
            gpuName = "RTX 4070",
            vramGb = 12.0f,
            ramGb = 64.0f,
            osName = "Windows 11",
            preferredRuntime = "llama.cpp"
        )

        val quantQ4 = test70BModel.quantizations.first { it.quantType == "Q4_K_M" }
        val result = HardwareFitCalculator.calculate(test70BModel, quantQ4, rtx4070)

        assertEquals(FitGrade.BORDERLINE, result.fitGrade)
        assertNotNull(result.explanation)
        org.junit.Assert.assertTrue(result.estimatedTokensPerSec > 0f)
        org.junit.Assert.assertTrue(result.estimatedTtftMs > 0f)
    }
}
