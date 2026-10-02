package com.inferra.data.network

import com.inferra.domain.model.AiModel
import com.inferra.domain.model.CapabilityMatrix
import com.inferra.domain.model.LicenseType
import com.inferra.domain.model.LineageInfo
import com.inferra.domain.model.Modality
import com.inferra.domain.model.ModelTask
import com.inferra.domain.model.QuantizationInfo
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale

object NetworkToDomainMapper {

    fun mapToDomain(dto: HuggingFaceModelDto): AiModel {
        val fullId = dto.id
        val parts = fullId.split("/")
        val authorStr = dto.author ?: if (parts.size > 1) parts[0] else "Community"
        val modelNameStr = if (parts.size > 1) parts[1] else fullId

        val (totalParams, activeParams, isMoe) = extractParams(modelNameStr, dto.tags, dto.config)
        val contextLen = extractContextLength(dto, modelNameStr)

        val modalities = extractModalities(dto.pipelineTag, dto.tags)
        val tasks = extractTasks(dto.pipelineTag, modelNameStr, dto.tags)

        val (licenseType, licenseName) = extractLicense(dto.tags, dto.cardData)
        val quantizations = extractQuantizations(fullId, dto.siblings, totalParams)

        val downloads = dto.downloads ?: 0L
        val likes = dto.likes ?: 0L

        val descriptionText = buildDescription(
            author = authorStr,
            pipelineTag = dto.pipelineTag,
            totalParamsBillion = totalParams,
            contextLenTokens = contextLen,
            downloads = downloads,
            likes = likes
        )

        return AiModel(
            id = fullId,
            name = modelNameStr,
            author = authorStr,
            description = descriptionText,
            architecture = dto.config?.architectures?.firstOrNull() ?: dto.config?.modelType ?: dto.libraryName ?: "Transformer",
            totalParamsBillion = totalParams,
            activeParamsBillion = activeParams,
            isMoe = isMoe,
            contextLengthTokens = contextLen,
            modalities = modalities,
            tasks = tasks,
            license = licenseType,
            licenseName = licenseName,
            downloadsCount = downloads,
            likesCount = likes,
            updatedAt = dto.lastModified?.take(10) ?: "Recently",
            quantizations = quantizations,
            capabilities = CapabilityMatrix(
                coding = if (tasks.contains(ModelTask.CODING)) 100f else 0f,
                reasoning = if (tasks.contains(ModelTask.REASONING)) 100f else 0f,
                math = if (tasks.contains(ModelTask.MATH)) 100f else 0f,
                vision = if (modalities.contains(Modality.VISION)) 100f else 0f,
                agentic = if (tasks.contains(ModelTask.AGENTIC)) 100f else 0f,
                toolCalling = if (tasks.contains(ModelTask.TOOL_CALLING)) 100f else 0f,
                multilingual = 0f, // 0f indicates unmeasured
                longContext = if (contextLen >= 32768) 100f else 0f
            ),
            lineage = LineageInfo(
                baseModelId = extractBaseModelId(dto.tags, modelNameStr, fullId)
            ),
            isFeatured = downloads > 500000 || likes > 2000,
            isTrending = likes > 500,
            isNew = dto.createdAt?.contains("2025") == true || dto.createdAt?.contains("2026") == true,
            repoUrl = "https://huggingface.co/$fullId",
            avatarUrl = null
        )
    }

    private fun extractParams(
        name: String,
        tags: List<String>?,
        config: HfConfigDto?
    ): Triple<Float, Float, Boolean> {
        val lower = name.lowercase(Locale.US)

        val moeMatch = Regex("(\\d+)x(\\d+\\.?\\d*)b").find(lower)
        if (moeMatch != null) {
            val numExperts = moeMatch.groupValues[1].toFloatOrNull() ?: 8f
            val expertSize = moeMatch.groupValues[2].toFloatOrNull() ?: 7f
            val total = numExperts * expertSize
            val active = expertSize * 2f
            return Triple(total, active, true)
        }

        val paramMatch = Regex("(\\d+\\.?\\d*)b").find(lower)
        if (paramMatch != null) {
            val p = paramMatch.groupValues[1].toFloatOrNull() ?: 0f
            if (p > 0f) {
                return Triple(p, p, false)
            }
        }

        tags?.forEach { tag ->
            val tagLower = tag.lowercase(Locale.US)
            val tagMatch = Regex("^(\\d+\\.?\\d*)b$").find(tagLower)
            if (tagMatch != null) {
                val p = tagMatch.groupValues[1].toFloatOrNull() ?: 0f
                if (p > 0f) return Triple(p, p, false)
            }
        }

        if (config != null) {
            val layers = config.numLayers
            val hidden = config.hiddenSize
            val inter = config.intermediateSize
            if (layers != null && hidden != null && inter != null && layers > 0 && hidden > 0) {
                val estParams = (layers * (12.0 * hidden * hidden + 2.0 * hidden * inter) / 1e9).toFloat()
                if (estParams > 0.1f) {
                    val rounded = (estParams * 10).toInt() / 10f
                    return Triple(rounded, rounded, false)
                }
            }
        }

        return Triple(0f, 0f, false)
    }

    private fun extractContextLength(dto: HuggingFaceModelDto, name: String): Int {
        try {
            val configMax = dto.config?.maxPositionEmbeddings?.jsonPrimitive?.intOrNull
                ?: dto.config?.maxPositionEmbeddings?.jsonPrimitive?.content?.toIntOrNull()
            if (configMax != null && configMax > 0) return configMax
        } catch (_: Exception) { }

        val lower = name.lowercase(Locale.US)
        if (lower.contains("128k") || lower.contains("131k")) return 131072
        if (lower.contains("64k")) return 65536
        if (lower.contains("256k")) return 262144
        if (lower.contains("1m")) return 1048576
        if (lower.contains("16k")) return 16384
        if (lower.contains("8k")) return 8192
        if (lower.contains("4k")) return 4096

        return 0
    }

    private fun extractModalities(pipelineTag: String?, tags: List<String>?): List<Modality> {
        val list = mutableListOf(Modality.TEXT)
        val tagList = tags ?: emptyList()

        if (pipelineTag in listOf("image-to-text", "visual-question-answering", "image-text-to-text", "image-classification") ||
            tagList.any { it.contains("vision") || it.contains("vl") }) {
            list.add(Modality.VISION)
        }
        if (pipelineTag in listOf("automatic-speech-recognition", "text-to-speech", "audio-classification") ||
            tagList.contains("audio")) {
            list.add(Modality.AUDIO)
        }
        if (tagList.contains("code") || tagList.contains("coder")) {
            list.add(Modality.CODE)
        }

        return list.distinct()
    }

    private fun extractTasks(pipelineTag: String?, name: String, tags: List<String>?): List<ModelTask> {
        val list = mutableListOf<ModelTask>()
        val lower = name.lowercase(Locale.US)
        val tagList = tags ?: emptyList()

        if (lower.contains("coder") || lower.contains("code") || tagList.contains("code")) {
            list.add(ModelTask.CODING)
        }
        if (lower.contains("math") || lower.contains("gsm8k")) {
            list.add(ModelTask.MATH)
        }
        if (lower.contains("vision") || lower.contains("vl") || pipelineTag?.contains("image") == true) {
            list.add(ModelTask.VISION)
        }
        if (lower.contains("instruct") || lower.contains("chat") || lower.contains("agent")) {
            list.add(ModelTask.REASONING)
            list.add(ModelTask.AGENTIC)
            list.add(ModelTask.TOOL_CALLING)
        }
        if (list.isEmpty()) {
            list.add(ModelTask.GENERAL_TEXT)
        }

        return list.distinct()
    }

    private fun extractLicense(tags: List<String>?, cardData: HfCardDataDto?): Pair<LicenseType, String> {
        val licTag = tags?.find { it.startsWith("license:") }
        if (licTag != null) {
            val licName = licTag.removePrefix("license:")
            return when {
                licName.contains("apache-2.0", true) -> Pair(LicenseType.APACHE_2, "Apache 2.0")
                licName.contains("mit", true) -> Pair(LicenseType.MIT, "MIT License")
                licName.contains("llama", true) -> Pair(LicenseType.LLAMA_COMMUNITY, "Llama License")
                licName.contains("qwen", true) -> Pair(LicenseType.QWEN_RESEARCH, "Qwen License")
                licName.contains("gemma", true) -> Pair(LicenseType.RESTRICTED, "Gemma Terms of Use")
                else -> Pair(LicenseType.PERMISSIVE_OTHER, licName.uppercase(Locale.US))
            }
        }

        val cardLic = cardData?.license?.jsonPrimitive?.content
        if (!cardLic.isNullOrBlank()) {
            return Pair(LicenseType.PERMISSIVE_OTHER, cardLic.uppercase(Locale.US))
        }

        return Pair(LicenseType.PERMISSIVE_OTHER, "Not specified")
    }

    private fun extractQuantizations(
        fullId: String,
        siblings: List<HfSiblingDto>?,
        totalParams: Float
    ): List<QuantizationInfo> {
        val quants = mutableListOf<QuantizationInfo>()
        val ggufSiblings = siblings?.filter { it.filename?.endsWith(".gguf", ignoreCase = true) == true } ?: emptyList()

        if (ggufSiblings.isNotEmpty()) {
            ggufSiblings.take(10).forEachIndexed { idx, sib ->
                val fname = sib.filename ?: ""
                val fnameUpper = fname.uppercase(Locale.US)

                val qType = when {
                    fnameUpper.contains("Q4_K_M") -> "Q4_K_M"
                    fnameUpper.contains("Q8_0") -> "Q8_0"
                    fnameUpper.contains("Q5_K_M") -> "Q5_K_M"
                    fnameUpper.contains("Q6_K") -> "Q6_K"
                    fnameUpper.contains("Q3_K_M") -> "Q3_K_M"
                    fnameUpper.contains("Q2_K") -> "Q2_K"
                    fnameUpper.contains("Q4_0") -> "Q4_0"
                    fnameUpper.contains("Q5_0") -> "Q5_0"
                    fnameUpper.contains("Q4_K_S") -> "Q4_K_S"
                    fnameUpper.contains("FP16") -> "FP16"
                    else -> fname.substringAfterLast("-").substringBefore(".gguf").ifBlank { "GGUF" }
                }

                val mult = when (qType) {
                    "Q2_K" -> 0.30f
                    "Q3_K_M" -> 0.42f
                    "Q4_K_M", "Q4_K_S", "Q4_0" -> 0.55f
                    "Q5_K_M", "Q5_0" -> 0.68f
                    "Q6_K" -> 0.78f
                    "Q8_0" -> 1.05f
                    "FP16" -> 2.05f
                    else -> 0.55f
                }

                val sizeBytes = if (totalParams > 0f) (totalParams * mult * 1024 * 1024 * 1024).toLong() else 0L
                val ramMb = if (sizeBytes > 0L) ((sizeBytes / (1024 * 1024)) + 1200).toInt() else 0

                quants.add(
                    QuantizationInfo(
                        id = "quant-$idx-$fname",
                        format = "GGUF",
                        quantType = qType,
                        fileSizeBytes = sizeBytes,
                        downloadUrl = "https://huggingface.co/$fullId/resolve/main/$fname",
                        fileName = fname,
                        estimatedRamMb = ramMb,
                        estimatedVramMb = (ramMb * 0.9f).toInt(),
                        relativeQualityScore = 0f // 0f indicates unmeasured; populated dynamically by QualityEvidenceEngine
                    )
                )
            }
        }

        return quants
    }

    private fun extractBaseModelId(tags: List<String>?, modelName: String, fullId: String): String? {
        val baseTag = tags?.find { it.startsWith("base_model:") }?.removePrefix("base_model:")
        if (!baseTag.isNullOrBlank()) return baseTag

        if (modelName.contains("Instruct", ignoreCase = true) || modelName.contains("Chat", ignoreCase = true)) {
            return fullId.replace("-Instruct", "", ignoreCase = true).replace("-Chat", "", ignoreCase = true)
        }
        return null
    }

    private fun buildDescription(
        author: String,
        pipelineTag: String?,
        totalParamsBillion: Float,
        contextLenTokens: Int,
        downloads: Long,
        likes: Long
    ): String {
        val parts = mutableListOf<String>()
        if (!pipelineTag.isNullOrBlank()) {
            parts.add("Task: $pipelineTag")
        }
        if (totalParamsBillion > 0f) {
            parts.add("${String.format(Locale.US, "%.1f", totalParamsBillion)}B parameters")
        }
        if (contextLenTokens > 0) {
            parts.add("${contextLenTokens / 1024}K context")
        }

        return if (parts.isNotEmpty()) {
            "Open-weight model by $author (${parts.joinToString(" • ")}). Live statistics: ${downloads} downloads, ${likes} likes."
        } else {
            "Model hosted on Hugging Face Hub by $author. Live statistics: ${downloads} downloads, ${likes} likes."
        }
    }
}
