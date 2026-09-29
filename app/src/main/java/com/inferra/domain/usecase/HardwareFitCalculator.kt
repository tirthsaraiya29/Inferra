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
        profile: HardwareProfile?
    ): HardwareCompatibilityResult {
        if (profile == null) {
            return HardwareCompatibilityResult(
                fitGrade = FitGrade.UNKNOWN,
                requiredVramGb = 0f,
                requiredRamGb = 0f,
                estimatedTokensPerSec = 0f,
                estimatedTtftMs = 0f,
                offloadPercentage = 0,
                explanation = "Hardware profile not configured",
                profileName = "Unconfigured"
            )
        }

        val quantMultiplier = when (quantization?.quantType?.uppercase(Locale.US)) {
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
            availableVram > 0f && totalMemoryRequiredGb <= availableVram * 0.90f -> {
                Triple(
                    FitGrade.EXCELLENT,
                    100,
                    "Fits inside VRAM ($reqGbStr GB required / ${profile.vramGb} GB VRAM available)."
                )
            }
            availableVram > 0f && totalMemoryRequiredGb <= availableVram -> {
                Triple(
                    FitGrade.EXCELLENT,
                    100,
                    "Fits in VRAM ($reqGbStr GB required)."
                )
            }
            totalAvailableMem > 0f && totalMemoryRequiredGb <= totalAvailableMem * 0.85f -> {
                val offload = if (availableVram > 0f) ((availableVram / totalMemoryRequiredGb) * 100).toInt().coerceIn(0, 95) else 0
                Triple(
                    FitGrade.BORDERLINE,
                    offload,
                    "Requires RAM offloading ($reqGbStr GB required vs $availMemStr GB available)."
                )
            }
            else -> {
                Triple(
                    FitGrade.INSUFFICIENT,
                    0,
                    "Exceeds memory ($reqGbStr GB required vs $availMemStr GB system memory)."
                )
            }
        }

        val baseSpeed = 25f
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
