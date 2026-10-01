package com.inferra.domain.usecase

import com.inferra.domain.model.AliasType
import com.inferra.domain.model.CanonicalModel
import com.inferra.domain.model.LicenseType
import com.inferra.domain.model.Modality
import com.inferra.domain.model.ModelAlias
import com.inferra.domain.model.ModelFamily
import com.inferra.domain.model.ModelTask
import com.inferra.domain.model.ModelVariant
import java.util.Locale

object CanonicalModelResolver {

    fun resolveCanonicalId(rawInputId: String): String {
        val cleaned = rawInputId.trim().lowercase(Locale.US)
            .replace(Regex("^https?://huggingface\\.co/"), "")
            .replace(Regex("^canonical:"), "")

        return when {
            cleaned.contains("qwen2.5-coder-32b") || cleaned.contains("qwen-2.5-coder-32b") -> "canonical:qwen-qwen2.5-coder-32b-instruct"
            cleaned.contains("llama-3.3-70b") || cleaned.contains("llama3.3-70b") -> "canonical:meta-llama-3.3-70b-instruct"
            cleaned.contains("mistral-small-24b") -> "canonical:mistralai-mistral-small-24b-instruct"
            cleaned.contains("deepseek-v3") -> "canonical:deepseek-ai-deepseek-v3"
            cleaned.contains("deepseek-r1") -> "canonical:deepseek-ai-deepseek-r1"
            cleaned.contains("gemma-2-9b") -> "canonical:google-gemma-2-9b-it"
            cleaned.contains("phi-4") -> "canonical:microsoft-phi-4"
            else -> {
                val sanitized = cleaned.replace('/', '-').replace(':', '-')
                "canonical:$sanitized"
            }
        }
    }

    fun getCanonicalModel(canonicalId: String, fallbackDisplayName: String = "", totalParams: Float = 0f): CanonicalModel {
        return when (canonicalId) {
            "canonical:qwen-qwen2.5-coder-32b-instruct" -> CanonicalModel(
                id = "canonical:qwen-qwen2.5-coder-32b-instruct",
                familyId = "family:qwen-2.5",
                variantType = ModelVariant.INSTRUCT,
                name = "Qwen2.5-Coder-32B-Instruct",
                author = "Qwen / Alibaba Cloud",
                description = "State-of-the-art open code reasoning model with 128K context window.",
                baseModelId = "canonical:qwen-qwen2.5-coder-32b",
                totalParamsBillion = 32.5f,
                activeParamsBillion = 32.5f,
                isMoe = false,
                architecture = "Qwen2ForCausalLM",
                contextLengthTokens = 131072,
                modalities = listOf(Modality.TEXT, Modality.CODE),
                tasks = listOf(ModelTask.CODING, ModelTask.REASONING, ModelTask.AGENTIC),
                license = LicenseType.QWEN_RESEARCH,
                licenseName = "Qwen Research License",
                createdDate = "2024-11-12"
            )
            "canonical:meta-llama-3.3-70b-instruct" -> CanonicalModel(
                id = "canonical:meta-llama-3.3-70b-instruct",
                familyId = "family:llama-3.3",
                variantType = ModelVariant.INSTRUCT,
                name = "Llama-3.3-70B-Instruct",
                author = "Meta",
                description = "Flagship 70B open-weights model delivering 405B-class performance.",
                baseModelId = "canonical:meta-llama-3.3-70b",
                totalParamsBillion = 70.0f,
                activeParamsBillion = 70.0f,
                isMoe = false,
                architecture = "LlamaForCausalLM",
                contextLengthTokens = 131072,
                modalities = listOf(Modality.TEXT),
                tasks = listOf(ModelTask.GENERAL_TEXT, ModelTask.REASONING, ModelTask.TOOL_CALLING),
                license = LicenseType.LLAMA_COMMUNITY,
                licenseName = "Llama 3.3 Community License",
                createdDate = "2024-12-06"
            )
            "canonical:deepseek-ai-deepseek-v3" -> CanonicalModel(
                id = "canonical:deepseek-ai-deepseek-v3",
                familyId = "family:deepseek-v3",
                variantType = ModelVariant.BASE,
                name = "DeepSeek-V3",
                author = "DeepSeek AI",
                description = "671B MoE model with Multi-head Latent Attention (MLA) and 37B active parameters.",
                totalParamsBillion = 671.0f,
                activeParamsBillion = 37.0f,
                isMoe = true,
                architecture = "DeepseekV3ForCausalLM",
                contextLengthTokens = 131072,
                modalities = listOf(Modality.TEXT, Modality.CODE),
                tasks = listOf(ModelTask.GENERAL_TEXT, ModelTask.CODING, ModelTask.REASONING),
                license = LicenseType.MIT,
                licenseName = "MIT License",
                createdDate = "2024-12-25"
            )
            else -> {
                val name = fallbackDisplayName.ifBlank { canonicalId.removePrefix("canonical:").replace('-', ' ').replaceFirstChar { it.uppercase() } }
                val author = if (canonicalId.contains('-')) canonicalId.substringAfter("canonical:").substringBefore('-').replaceFirstChar { it.uppercase() } else "Community"
                CanonicalModel(
                    id = canonicalId,
                    familyId = "family:generic",
                    variantType = ModelVariant.INSTRUCT,
                    name = name,
                    author = author,
                    description = "Open-weights artificial intelligence model.",
                    totalParamsBillion = totalParams.coerceAtLeast(7.0f),
                    activeParamsBillion = totalParams.coerceAtLeast(7.0f),
                    isMoe = false,
                    architecture = "Transformer",
                    contextLengthTokens = 32768,
                    modalities = listOf(Modality.TEXT),
                    tasks = listOf(ModelTask.GENERAL_TEXT),
                    license = LicenseType.APACHE_2,
                    licenseName = "Apache 2.0"
                )
            }
        }
    }

    fun getAliases(canonicalId: String): List<ModelAlias> {
        return when (canonicalId) {
            "canonical:qwen-qwen2.5-coder-32b-instruct" -> listOf(
                ModelAlias("Qwen/Qwen2.5-Coder-32B-Instruct", canonicalId, AliasType.HUGGING_FACE),
                ModelAlias("qwen/qwen-2.5-coder-32b-instruct", canonicalId, AliasType.PROVIDER_MODEL_ID),
                ModelAlias("Qwen2.5-Coder-32B-Instruct", canonicalId, AliasType.DISPLAY_NAME)
            )
            "canonical:meta-llama-3.3-70b-instruct" -> listOf(
                ModelAlias("meta-llama/Llama-3.3-70B-Instruct", canonicalId, AliasType.HUGGING_FACE),
                ModelAlias("meta-llama/llama-3.3-70b-instruct", canonicalId, AliasType.PROVIDER_MODEL_ID),
                ModelAlias("Llama-3.3-70B-Instruct", canonicalId, AliasType.DISPLAY_NAME)
            )
            else -> listOf(
                ModelAlias(canonicalId.removePrefix("canonical:"), canonicalId, AliasType.DISPLAY_NAME)
            )
        }
    }

    fun getFamily(familyId: String): ModelFamily {
        return when (familyId) {
            "family:qwen-2.5" -> ModelFamily("family:qwen-2.5", "Qwen 2.5", "Qwen / Alibaba Cloud", "Full-spectrum open weights LLM series.", "https://qwenlm.github.io")
            "family:llama-3.3" -> ModelFamily("family:llama-3.3", "Llama 3.3", "Meta AI", "Open state-of-the-art foundation model family.", "https://llama.meta.com")
            "family:deepseek-v3" -> ModelFamily("family:deepseek-v3", "DeepSeek V3", "DeepSeek AI", "Efficient MoE architectures featuring MLA.", "https://deepseek.com")
            else -> ModelFamily("family:generic", "Generic Family", "Open Community", "Open weight model family.", "")
        }
    }
}
