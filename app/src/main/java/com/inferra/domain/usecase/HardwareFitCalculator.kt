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

        val breakdown = MemoryEstimationEngine.estimate(
            model = model,
            quantization = quantization
        )

        val totalMemoryRequiredGb = breakdown.totalMemoryGb
        val weightsSizeGb = breakdown.weightMemoryMb / 1024f

        val availableVram = profile.vramGb
        val availableRam = profile.ramGb
        val totalAvailableMem = if (availableVram > 0f) availableVram + availableRam else availableRam

        val reqGbStr = String.format(Locale.US, "%.1f", totalMemoryRequiredGb)
        val availMemStr = String.format(Locale.US, "%.1f", totalAvailableMem)

        val (fitGrade, offloadPct, explanation) = when {
            availableVram > 0f && totalMemoryRequiredGb <= availableVram * 0.90f -> {
                Triple(
                    FitGrade.EXCELLENT,
                    100,
                    "Fits comfortably in VRAM ($reqGbStr GB required [Weights: ${String.format(Locale.US, "%.1f", weightsSizeGb)}GB, KV-Cache: ${breakdown.kvCacheMemoryMb}MB] / ${profile.vramGb} GB VRAM)."
                )
            }
            availableVram > 0f && totalMemoryRequiredGb > availableVram && totalMemoryRequiredGb <= totalAvailableMem * 0.90f -> {
                val offload = ((availableVram / totalMemoryRequiredGb) * 100).toInt().coerceIn(0, 95)
                Triple(
                    FitGrade.BORDERLINE,
                    offload,
                    "Requires RAM offloading ($reqGbStr GB required vs ${profile.vramGb} GB VRAM / $availMemStr GB total memory)."
                )
            }
            availableRam > 0f && totalMemoryRequiredGb <= availableRam * 0.85f -> {
                Triple(
                    FitGrade.EXCELLENT,
                    0,
                    "Fits comfortably in system RAM ($reqGbStr GB required / $availMemStr GB available)."
                )
            }
            totalAvailableMem > 0f && totalMemoryRequiredGb <= totalAvailableMem * 0.95f -> {
                val offload = if (availableVram > 0f) ((availableVram / totalMemoryRequiredGb) * 100).toInt().coerceIn(0, 95) else 0
                Triple(
                    FitGrade.BORDERLINE,
                    offload,
                    "Tight memory fit ($reqGbStr GB required vs $availMemStr GB system memory)."
                )
            }
            else -> {
                Triple(
                    FitGrade.INSUFFICIENT,
                    0,
                    "Exceeds available memory ($reqGbStr GB required vs $availMemStr GB system memory)."
                )
            }
        }

        return HardwareCompatibilityResult(
            fitGrade = fitGrade,
            requiredVramGb = if (fitGrade == FitGrade.EXCELLENT && availableVram > 0f) totalMemoryRequiredGb else availableVram,
            requiredRamGb = (totalMemoryRequiredGb - availableVram).coerceAtLeast(0f),
            estimatedTokensPerSec = 0f,
            estimatedTtftMs = 0f,
            offloadPercentage = offloadPct,
            explanation = explanation,
            profileName = profile.name
        )
    }
}
