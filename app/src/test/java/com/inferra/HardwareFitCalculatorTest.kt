package com.inferra

import com.inferra.data.local.SeedData
import com.inferra.domain.model.DeviceType
import com.inferra.domain.model.FitGrade
import com.inferra.domain.model.HardwareProfile
import com.inferra.domain.usecase.HardwareFitCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HardwareFitCalculatorTest {

    @Test
    fun test32BModelFitsIn24GBVramWithQ4() {
        val qwenCoder32B = SeedData.seedModels.first { it.name.contains("32B") }
        val rtx4090 = HardwareProfile(
            id = "test-4090",
            name = "Test RTX 4090",
            deviceType = DeviceType.DESKTOP_PC,
            cpuName = "i9-14900K",
            gpuName = "RTX 4090",
            vramGb = 24.0f,
            ramGb = 64.0f,
            osName = "Windows 11",
            preferredRuntime = "llama.cpp"
        )

        val quantQ4 = qwenCoder32B.quantizations.first { it.quantType == "Q4_K_M" }
        val result = HardwareFitCalculator.calculate(qwenCoder32B, quantQ4, rtx4090)

        assertEquals(FitGrade.EXCELLENT, result.fitGrade)
        assertEquals(100, result.offloadPercentage)
        assertTrue(result.estimatedTokensPerSec > 30f)
    }

    @Test
    fun test70BModelOn12GBVramIsBorderlineWithOffload() {
        val llama70B = SeedData.seedModels.first { it.name.contains("70B") }
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

        val quantQ4 = llama70B.quantizations.first { it.quantType == "Q4_K_M" }
        val result = HardwareFitCalculator.calculate(llama70B, quantQ4, rtx4070)

        assertEquals(FitGrade.BORDERLINE, result.fitGrade)
        assertTrue(result.offloadPercentage in 20..40)
    }
}
