package com.inferra.domain.usecase

import com.inferra.domain.model.AiModel
import com.inferra.domain.model.AttentionArchitecture
import com.inferra.domain.model.QuantizationInfo
import java.util.Locale

enum class EstimationConfidence {
    KNOWN,
    ESTIMATED,
    APPROXIMATE,
    UNSUPPORTED,
    UNKNOWN
}

data class MemoryBreakdown(
    val weightMemoryMb: Int,
    val kvCacheMemoryMb: Int,
    val activationMemoryMb: Int,
    val backendOverheadMb: Int,
    val totalMemoryMb: Int,
    val totalMemoryGb: Float,
    val confidence: EstimationConfidence,
    val attentionArchitecture: AttentionArchitecture,
    val description: String
)

object MemoryEstimationEngine {

    fun estimate(
        model: AiModel,
        quantization: QuantizationInfo?,
        contextLengthTokens: Int = model.contextLengthTokens.coerceAtMost(131072),
        kvCacheQuantType: String = "FP16"
    ): MemoryBreakdown {
        val quantTypeUpper = quantization?.quantType?.uppercase(Locale.US) ?: "Q4_K_M"

        val bytesPerParam = when (quantTypeUpper) {
            "Q2_K" -> 0.32f
            "Q3_K_M", "Q3_K_S", "IQ3_XS" -> 0.44f
            "Q4_K_M", "Q4_K_S", "Q4_0", "IQ4_XS", "AWQ-4BIT", "GPTQ-4BIT" -> 0.58f
            "Q5_K_M", "Q5_0" -> 0.72f
            "Q6_K" -> 0.82f
            "Q8_0", "GPTQ-8BIT" -> 1.08f
            "FP16", "BF16" -> 2.05f
            else -> 0.60f
        }

        // 1. Model Weights Memory
        val weightMemoryMb = if (quantization != null && quantization.fileSizeBytes > 0L) {
            (quantization.fileSizeBytes / (1024 * 1024)).toInt()
        } else {
            (model.totalParamsBillion * bytesPerParam * 1024).toInt()
        }

        // 2. Identify Attention Architecture & Config parameters
        val archLower = model.architecture.lowercase(Locale.US)
        val (attentionArch, numLayers, numHeads, numKvHeads, headDim) = inferArchParams(archLower, model.totalParamsBillion)

        // KV cache byte multiplier (FP16 = 2 bytes, Q8_0 = 1 byte, Q4_0 = 0.5 bytes)
        val kvBytesPerElement: Double = when (kvCacheQuantType.uppercase(Locale.US)) {
            "Q8_0" -> 1.0
            "Q4_0" -> 0.5
            else -> 2.0 // FP16 default
        }

        // 3. Architecture-Aware KV Cache Calculation
        val effectiveContext = contextLengthTokens.coerceAtLeast(1024).toDouble()
        val kvCacheBytes: Double = when (attentionArch) {
            AttentionArchitecture.MHA -> {
                2.0 * numLayers * numHeads * headDim * effectiveContext * kvBytesPerElement
            }
            AttentionArchitecture.GQA -> {
                2.0 * numLayers * numKvHeads * headDim * effectiveContext * kvBytesPerElement
            }
            AttentionArchitecture.MQA -> {
                2.0 * numLayers * 1.0 * headDim * effectiveContext * kvBytesPerElement
            }
            AttentionArchitecture.MLA -> {
                val dLatent = 512.0
                val dRope = 64.0
                (dLatent + dRope) * numLayers * effectiveContext * kvBytesPerElement
            }
            AttentionArchitecture.SLIDING_WINDOW -> {
                val windowSize = 4096.0
                val effWin = effectiveContext.coerceAtMost(windowSize)
                2.0 * numLayers * numKvHeads * headDim * effWin * kvBytesPerElement
            }
            AttentionArchitecture.STATE_SPACE_SSM -> {
                val stateDim = 16.0
                val dModel = (numHeads * headDim).toDouble()
                (numLayers * dModel * stateDim * 2.0).coerceAtLeast(1024.0 * 1024.0)
            }
            AttentionArchitecture.HYBRID -> {
                2.0 * numLayers * numKvHeads * headDim * effectiveContext * kvBytesPerElement
            }
        }

        val kvCacheMemoryMb = (kvCacheBytes / (1024.0 * 1024.0)).toInt().coerceAtLeast(16)

        // 4. Activation Memory
        val activationMemoryMb = ((numLayers * headDim * 4.0) / 1024.0).toInt().coerceIn(128, 1024)

        // 5. Backend Overhead Memory (Vulkan/CUDA/CPU graph allocator overhead)
        val backendOverheadMb = if (weightMemoryMb > 30000) 1024 else 512

        val totalMemoryMb = weightMemoryMb + kvCacheMemoryMb + activationMemoryMb + backendOverheadMb
        val totalMemoryGb = totalMemoryMb / 1024.0f

        val confidence = when {
            quantization != null && quantization.fileSizeBytes > 0L -> EstimationConfidence.KNOWN
            model.totalParamsBillion > 0f -> EstimationConfidence.ESTIMATED
            else -> EstimationConfidence.APPROXIMATE
        }

        val desc = "Architecture: ${attentionArch.name} • Weights: ${weightMemoryMb / 1024f}GB • KV Cache (${effectiveContext.toInt() / 1024}K): ${kvCacheMemoryMb}MB • Backend Overhead: ${backendOverheadMb}MB"

        return MemoryBreakdown(
            weightMemoryMb = weightMemoryMb,
            kvCacheMemoryMb = kvCacheMemoryMb,
            activationMemoryMb = activationMemoryMb,
            backendOverheadMb = backendOverheadMb,
            totalMemoryMb = totalMemoryMb,
            totalMemoryGb = totalMemoryGb,
            confidence = confidence,
            attentionArchitecture = attentionArch,
            description = desc
        )
    }

    private fun inferArchParams(archLower: String, totalParams: Float): Tuple5<AttentionArchitecture, Int, Int, Int, Int> {
        return when {
            archLower.contains("deepseek") && archLower.contains("v3") -> {
                Tuple5(AttentionArchitecture.MLA, 61, 128, 128, 128)
            }
            archLower.contains("qwen2") -> {
                val layers = if (totalParams > 30f) 64 else 28
                val kvHeads = if (totalParams > 30f) 8 else 4
                Tuple5(AttentionArchitecture.GQA, layers, 40, kvHeads, 128)
            }
            archLower.contains("llama") -> {
                val layers = if (totalParams > 60f) 80 else 32
                val kvHeads = if (totalParams > 60f) 8 else 8
                Tuple5(AttentionArchitecture.GQA, layers, 64, kvHeads, 128)
            }
            archLower.contains("mistral") -> {
                Tuple5(AttentionArchitecture.SLIDING_WINDOW, 32, 32, 8, 128)
            }
            archLower.contains("mamba") -> {
                Tuple5(AttentionArchitecture.STATE_SPACE_SSM, 64, 64, 1, 128)
            }
            else -> {
                val layers = if (totalParams > 30f) 60 else 32
                val heads = if (totalParams > 30f) 64 else 32
                val kvHeads = if (totalParams > 30f) 8 else 8
                Tuple5(AttentionArchitecture.GQA, layers, heads, kvHeads, 128)
            }
        }
    }

    private data class Tuple5<A, B, C, D, E>(
        val val1: A, val val2: B, val val3: C, val val4: D, val val5: E
    )
}
