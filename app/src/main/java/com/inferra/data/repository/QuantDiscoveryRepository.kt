package com.inferra.data.repository

import android.util.Log
import com.inferra.data.network.HfSiblingDto
import com.inferra.data.network.HuggingFaceApi
import com.inferra.data.network.HuggingFaceModelDto
import com.inferra.domain.model.QuantizationInfo
import com.inferra.domain.usecase.QualityEvidenceEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class QuantDiscoveryRepository(
    private val api: HuggingFaceApi
) {
    private companion object {
        const val TAG = "QuantDiscovery"
    }

    suspend fun discoverQuantizations(
        baseModelDto: HuggingFaceModelDto,
        totalParamsBillion: Float
    ): List<QuantizationInfo> = withContext(Dispatchers.IO) {
        val baseId = baseModelDto.id
        val parts = baseId.split("/")
        val modelName = if (parts.size > 1) parts[1] else baseId

        Log.d(TAG, "Discovering quantizations dynamically from Hugging Face for model '$baseId'...")

        val reposToInspect = mutableListOf<HuggingFaceModelDto>()
        reposToInspect.add(baseModelDto)

        // Search HF for related quantization repositories
        val searchTerms = listOf("$modelName GGUF", "$modelName AWQ", "$modelName GPTQ", "$modelName EXL2")
        for (term in searchTerms) {
            try {
                val results = api.getModels(search = term, limit = 10, sort = "downloads")
                for (dto in results) {
                    if (isQuantizedVariantOf(dto, modelName, baseId)) {
                        if (reposToInspect.none { it.id.equals(dto.id, ignoreCase = true) }) {
                            reposToInspect.add(dto)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed searching HF for '$term': ${e.message}")
            }
        }

        val discoveredQuants = mutableListOf<QuantizationInfo>()

        for (repo in reposToInspect) {
            val repoId = repo.id
            val siblings = repo.siblings ?: emptyList()

            // Process GGUF files
            val ggufSiblings = siblings.filter { it.filename?.endsWith(".gguf", ignoreCase = true) == true }
            for (sib in ggufSiblings) {
                val fname = sib.filename ?: continue
                val quantInfo = parseGgufSibling(repoId, baseId, fname, sib, totalParamsBillion)
                if (quantInfo != null) {
                    discoveredQuants.add(quantInfo)
                }
            }

            // Process AWQ / GPTQ / EXL2 repositories if no GGUF files found
            if (ggufSiblings.isEmpty()) {
                val format = detectRepoFormat(repoId, repo.tags)
                if (format != null) {
                    val safetensorsSib = siblings.find { it.filename?.endsWith(".safetensors", ignoreCase = true) == true }
                    val fname = safetensorsSib?.filename ?: "model.safetensors"
                    val quantInfo = parseOtherFormatSibling(repoId, baseId, format, fname, safetensorsSib, totalParamsBillion)
                    if (quantInfo != null) {
                        discoveredQuants.add(quantInfo)
                    }
                }
            }
        }

        // Deduplicate equivalent quantizations (e.g. keep best source for Q4_K_M)
        val deduplicated = deduplicateQuants(discoveredQuants)
        Log.d(TAG, "Discovered ${deduplicated.size} unique quantization options for model '$baseId'")
        deduplicated
    }

    private fun isQuantizedVariantOf(dto: HuggingFaceModelDto, baseModelName: String, baseId: String): Boolean {
        if (dto.id.equals(baseId, ignoreCase = true)) return true
        val dtoParts = dto.id.split("/")
        val dtoName = if (dtoParts.size > 1) dtoParts[1] else dto.id

        val nameLower = dtoName.lowercase(Locale.US)
        val baseLower = baseModelName.lowercase(Locale.US)

        val isNameMatch = nameLower.contains(baseLower) || baseLower.contains(nameLower)
        val isQuantRepo = nameLower.contains("gguf") || nameLower.contains("awq") ||
                nameLower.contains("gptq") || nameLower.contains("exl2") ||
                dto.tags?.any { it.contains("gguf") || it.contains("quantized") } == true

        return isNameMatch && isQuantRepo
    }

    private fun parseGgufSibling(
        repoId: String,
        baseRepoId: String,
        filename: String,
        sibling: HfSiblingDto,
        totalParams: Float
    ): QuantizationInfo? {
        val fnameUpper = filename.uppercase(Locale.US)

        val qType = when {
            fnameUpper.contains("Q4_K_M") -> "Q4_K_M"
            fnameUpper.contains("Q8_0") -> "Q8_0"
            fnameUpper.contains("Q5_K_M") -> "Q5_K_M"
            fnameUpper.contains("Q6_K") -> "Q6_K"
            fnameUpper.contains("Q3_K_M") -> "Q3_K_M"
            fnameUpper.contains("IQ3_XS") -> "IQ3_XS"
            fnameUpper.contains("IQ4_XS") -> "IQ4_XS"
            fnameUpper.contains("Q2_K") -> "Q2_K"
            fnameUpper.contains("Q4_0") -> "Q4_0"
            fnameUpper.contains("Q5_0") -> "Q5_0"
            fnameUpper.contains("Q4_K_S") -> "Q4_K_S"
            fnameUpper.contains("Q5_K_S") -> "Q5_K_S"
            fnameUpper.contains("FP16") -> "FP16"
            fnameUpper.contains("BF16") -> "BF16"
            else -> filename.substringAfterLast("-").substringBefore(".gguf").ifBlank { "GGUF" }
        }

        val sizeBytes = sibling.size ?: calculateEstimatedSizeBytes(totalParams, qType, "GGUF")
        val ramMb = if (sizeBytes > 0L) ((sizeBytes / (1024 * 1024)) + 1200).toInt() else 0
        val vramMb = (ramMb * 0.9f).toInt()

        val qualityEvidence = QualityEvidenceEngine.extractQualityEvidence(
            baseRepoId = baseRepoId,
            quantRepoId = repoId,
            quantType = qType,
            readmeText = null
        )

        return QuantizationInfo(
            id = "$repoId#$filename",
            format = "GGUF",
            quantType = qType,
            fileSizeBytes = sizeBytes,
            downloadUrl = "https://huggingface.co/$repoId/resolve/main/$filename",
            fileName = filename,
            sourceRepo = repoId,
            estimatedRamMb = ramMb,
            estimatedVramMb = vramMb,
            relativeQualityScore = qualityEvidence.retentions.firstOrNull()?.retentionPercentage ?: 90f,
            qualityEvidence = qualityEvidence
        )
    }

    private fun parseOtherFormatSibling(
        repoId: String,
        baseRepoId: String,
        format: String,
        filename: String,
        sibling: HfSiblingDto?,
        totalParams: Float
    ): QuantizationInfo? {
        val qType = when (format) {
            "AWQ" -> "AWQ-4bit"
            "GPTQ" -> "GPTQ-4bit"
            "EXL2" -> "EXL2-4.0bpw"
            else -> format
        }

        val sizeBytes = sibling?.size ?: calculateEstimatedSizeBytes(totalParams, qType, format)
        val ramMb = if (sizeBytes > 0L) ((sizeBytes / (1024 * 1024)) + 1500).toInt() else 0

        val qualityEvidence = QualityEvidenceEngine.extractQualityEvidence(
            baseRepoId = baseRepoId,
            quantRepoId = repoId,
            quantType = qType,
            readmeText = null
        )

        return QuantizationInfo(
            id = "$repoId#$filename",
            format = format,
            quantType = qType,
            fileSizeBytes = sizeBytes,
            downloadUrl = "https://huggingface.co/$repoId/resolve/main/$filename",
            fileName = filename,
            sourceRepo = repoId,
            estimatedRamMb = ramMb,
            estimatedVramMb = ramMb,
            relativeQualityScore = qualityEvidence.retentions.firstOrNull()?.retentionPercentage ?: 88f,
            qualityEvidence = qualityEvidence
        )
    }

    private fun detectRepoFormat(repoId: String, tags: List<String>?): String? {
        val lower = repoId.lowercase(Locale.US)
        val tagList = tags ?: emptyList()

        return when {
            lower.contains("awq") || tagList.contains("awq") -> "AWQ"
            lower.contains("gptq") || tagList.contains("gptq") -> "GPTQ"
            lower.contains("exl2") || tagList.contains("exl2") -> "EXL2"
            else -> null
        }
    }

    private fun calculateEstimatedSizeBytes(paramsBillion: Float, qType: String, format: String): Long {
        if (paramsBillion <= 0f) return 0L
        val bytesPerParam = when (qType.uppercase(Locale.US)) {
            "Q2_K" -> 0.32f
            "Q3_K_M", "IQ3_XS" -> 0.42f
            "Q4_K_M", "Q4_0", "Q4_K_S", "IQ4_XS", "AWQ-4BIT", "GPTQ-4BIT", "EXL2-4.0BPW" -> 0.55f
            "Q5_K_M", "Q5_0", "Q5_K_S" -> 0.68f
            "Q6_K" -> 0.78f
            "Q8_0", "GPTQ-8BIT" -> 1.05f
            "FP16", "BF16" -> 2.05f
            else -> 0.55f
        }
        return (paramsBillion * bytesPerParam * 1024 * 1024 * 1024).toLong()
    }

    private fun deduplicateQuants(quants: List<QuantizationInfo>): List<QuantizationInfo> {
        val map = linkedMapOf<String, QuantizationInfo>()
        for (q in quants) {
            val key = "${q.format}-${q.quantType}"
            if (!map.containsKey(key)) {
                map[key] = q
            } else {
                // If existing quant has size 0 but new one has real size, replace it
                val existing = map[key]!!
                if (existing.fileSizeBytes == 0L && q.fileSizeBytes > 0L) {
                    map[key] = q
                }
            }
        }
        return map.values.toList()
    }
}
