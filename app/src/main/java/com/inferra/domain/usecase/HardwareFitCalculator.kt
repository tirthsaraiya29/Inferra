package com.inferra.domain.usecase

import com.inferra.domain.model.AiModel
import com.inferra.domain.model.FitGrade
import com.inferra.domain.model.HardwareCompatibilityResult
import com.inferra.domain.model.HardwareProfile
import com.inferra.domain.model.QuantizationInfo
import java.util.Locale

object HardwareFitCalculator {

    fun calculate(
        model: AiModel,
        quantization: QuantizationInfo?,
        profile: HardwareProfile
    ): HardwareCompatibilityResult {
        val quantMultiplier = when (quantization?.quantType?.uppercase()) {
            "Q2_K" -> 0.30f
            "Q3_K_M", "Q3_K_S" -> 0.42f
            "Q4_K_M", "Q4_K_S", "Q4_0" -> 0.55f
            "Q5_K_M", "Q5_0" -> 0.68f
            "Q6_K" -> 0.78f
            "Q8_0" -> 1.05f
            "FP16", "BF16" -> 2.05f
            else -> 0.60f
        }

        val effectiveParams = if (model.isMoe) model.activeParamsBillion else model.totalParamsBillion
        val totalWeightsSizeGb = (model.totalParamsBillion * quantMultiplier)
        
        val contextOverheadGb = (model.contextLengthTokens.coerceAtMost(32768) / 8192f) * 0.8f
        val totalMemoryRequiredGb = totalWeightsSizeGb + contextOverheadGb + 0.5f

        val availableVram = profile.vramGb
        val availableRam = profile.ramGb
        val totalAvailableMem = availableVram + availableRam

        val reqGbStr = String.format(Locale.US, "%.1f", totalMemoryRequiredGb)
        val availMemStr = String.format(Locale.US, "%.1f", totalAvailableMem)

        val (fitGrade, offloadPct, explanation) = when {
            totalMemoryRequiredGb <= availableVram * 0.90f -> {
                Triple(
                    FitGrade.EXCELLENT,
                    100,
                    "Fits 100% inside GPU VRAM ($reqGbStr GB required / ${profile.vramGb} GB VRAM available). Maximum inference throughput expected."
                )
            }
            totalMemoryRequiredGb <= availableVram -> {
                Triple(
                    FitGrade.EXCELLENT,
                    100,
                    "Fits completely in VRAM ($reqGbStr GB required) with minor margin."
                )
            }
            totalMemoryRequiredGb <= totalAvailableMem * 0.85f -> {
                val offload = ((availableVram / totalMemoryRequiredGb) * 100).toInt().coerceIn(0, 95)
                Triple(
                    FitGrade.BORDERLINE,
                    offload,
                    "Requires CPU RAM offloading ($reqGbStr GB total needed). ~$offload% of layers can run on ${profile.gpuName}, rest on CPU RAM. Expect lower tokens/sec."
                )
            }
            else -> {
                Triple(
                    FitGrade.INSUFFICIENT,
                    0,
                    "Exceeds system capacity ($reqGbStr GB required vs $availMemStr GB total system memory available)."
                )
            }
        }

        val baseSpeed = when {
            profile.gpuName.contains("4090", ignoreCase = true) -> 125f
            profile.gpuName.contains("4080", ignoreCase = true) || profile.gpuName.contains("3090", ignoreCase = true) -> 90f
            profile.gpuName.contains("4070", ignoreCase = true) -> 65f
            profile.gpuName.contains("3080", ignoreCase = true) || profile.gpuName.contains("4060", ignoreCase = true) -> 45f
            profile.gpuName.contains("Arc", ignoreCase = true) || profile.gpuName.contains("Radeon", ignoreCase = true) -> 35f
            profile.gpuName.contains("Apple", ignoreCase = true) || profile.gpuName.contains("M3", ignoreCase = true) -> 70f
            else -> 20f
        }

        val paramScaling = 10f / effectiveParams.coerceAtLeast(1f)
        val offloadPenalty = if (offloadPct < 100) (offloadPct / 100f) * 0.8f + 0.2f else 1.0f
        
        val estimatedTokSec = (baseSpeed * paramScaling * offloadPenalty).coerceIn(1f, 220f)
        val estimatedTtft = (450f / paramScaling + (100 - offloadPct) * 15f).coerceIn(120f, 4500f)

        return HardwareCompatibilityResult(
            fitGrade = fitGrade,
            requiredVramGb = if (fitGrade == FitGrade.EXCELLENT) totalMemoryRequiredGb else availableVram,
            requiredRamGb = if (fitGrade == FitGrade.EXCELLENT) 0f else (totalMemoryRequiredGb - availableVram).coerceAtLeast(0f),
            estimatedTokensPerSec = estimatedTokSec,
            estimatedTtftMs = estimatedTtft,
            offloadPercentage = offloadPct,
            explanation = explanation,
            profileName = profile.name
        )
    }
}
