package com.inferra.data.repository

import android.util.Log
import com.inferra.data.network.HfSiblingDto
import com.inferra.data.network.HuggingFaceApi
import com.inferra.data.network.HuggingFaceModelDto
import com.inferra.domain.model.QuantizationInfo
import com.inferra.domain.usecase.QualityEvidenceEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.util.Locale

class QuantDiscoveryRepository(
    private val api: HuggingFaceApi,
) {
    private companion object {
        const val TAG = "QuantDiscovery"

        private fun safeLogD(msg: String) {
            try { Log.d(TAG, msg) } catch (_: Throwable) { println("[$TAG] $msg") }
        }

        private fun safeLogW(msg: String) {
            try { Log.w(TAG, msg) } catch (_: Throwable) { println("[$TAG] WARNING: $msg") }
        }
    }

    suspend fun discoverQuantizations(
        baseModelDto: HuggingFaceModelDto,
        totalParamsBillion: Float
    ): List<QuantizationInfo> = withContext(Dispatchers.IO) {
        val baseId = baseModelDto.id
        val parts = baseId.split("/")
        val modelName = if (parts.size > 1) parts[1] else baseId

        safeLogD("Discovering quantizations dynamically in parallel from Hugging Face for model '$baseId'...")

        val reposToInspect = mutableListOf<HuggingFaceModelDto>()
        reposToInspect.add(baseModelDto)

        // Search HF for related quantization repositories in parallel using async
        val searchTerms = listOf("$modelName GGUF", "$modelName AWQ", "$modelName GPTQ", "$modelName EXL2")
        val searchResults = coroutineScope {
            searchTerms.map { term ->
                async {
                    try {
                        api.getModels(search = term, limit = 10, sort = "downloads")
                    } catch (e: Exception) {
                        safeLogW("Failed searching HF for '$term': ${e.message}")
                        emptyList<HuggingFaceModelDto>()
                    }
                }
            }.flatMap { it.await() }
        }

        for (dto in searchResults) {
            if (isQuantizedVariantOf(dto, modelName, baseId)) {
                if (reposToInspect.none { it.id.equals(dto.id, ignoreCase = true) }) {
                    reposToInspect.add(dto)
                }
            }
        }

        val discoveredQuants = mutableListOf<QuantizationInfo>()

        for (repo in reposToInspect) {
            val repoId = repo.id
            val siblings = repo.siblings ?: emptyList()

            // Process GGUF files (including multipart split archives)
            val ggufSiblings = siblings.filter { it.filename?.endsWith(".gguf", ignoreCase = true) == true }
            val splitGroups = groupSplitGgufs(ggufSiblings)

            for ((baseFname, partsList) in splitGroups) {
                val quantInfo = parseGgufGroup(repoId, baseId, baseFname, partsList, totalParamsBillion)
                discoveredQuants.add(quantInfo)
            }

            // Process AWQ / GPTQ / EXL2 repositories if no GGUF files found
            if (ggufSiblings.isEmpty()) {
                val format = detectRepoFormat(repoId, repo.tags)
                if (format != null) {
                    val safetensorsSib = siblings.find { it.filename?.endsWith(".safetensors", ignoreCase = true) == true }
                    val fname = safetensorsSib?.filename ?: "model.safetensors"
                    val quantInfo = parseOtherFormatSibling(repoId, baseId, format, fname, safetensorsSib, totalParamsBillion)
                    discoveredQuants.add(quantInfo)
                }
            }
        }

        // Deduplicate equivalent quantizations (e.g. keep best source for Q4_K_M)
        val deduplicated = deduplicateQuants(discoveredQuants)
        safeLogD("Discovered ${deduplicated.size} unique quantization options for model '$baseId'")
        deduplicated
    }

    private fun isQuantizedVariantOf(dto: HuggingFaceModelDto, baseModelName: String, baseId: String): Boolean {
        if (dto.id.equals(baseId, ignoreCase = true)) return true
        val dtoParts = dto.id.split("/")
        val dtoName = if (dtoParts.size > 1) dtoParts[1] else dto.id

        val nameLower = dtoName.lowercase(Locale.US)
        val baseLower = baseModelName.lowercase(Locale.US)

        val isNameMatch = nameLower.contains(baseLower) || baseLower.contains(nameLower)
        val isQuantRepo = (nameLower.contains("gguf") || nameLower.contains("awq") ||
                nameLower.contains("gptq") || nameLower.contains("exl2") ||
                (dto.tags?.any { it.contains("gguf") || it.contains("quantized") } == true))

        return isNameMatch && isQuantRepo
    }

    private fun groupSplitGgufs(siblings: List<HfSiblingDto>): Map<String, List<HfSiblingDto>> {
        val map = mutableMapOf<String, MutableList<HfSiblingDto>>()
        val splitRegex = Regex("^(.*?)(?:-\\d{5}-of-\\d{5})?\\.gguf$", RegexOption.IGNORE_CASE)

        for (sib in siblings) {
            val fname = sib.filename ?: continue
            val match = splitRegex.find(fname)
            val baseKey = if (match != null) "${match.groupValues[1]}.gguf" else fname
            map.getOrPut(baseKey) { mutableListOf() }.add(sib)
        }

        return map
    }

    private fun parseGgufGroup(
        repoId: String,
        baseRepoId: String,
        baseFilename: String,
        siblings: List<HfSiblingDto>,
        totalParams: Float
    ): QuantizationInfo {
        val primarySibling = siblings.first()
        val primaryFname = primarySibling.filename ?: baseFilename
        val fnameUpper = baseFilename.uppercase(Locale.US)

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
            else -> baseFilename.substringAfterLast("-").substringBefore(".gguf").ifBlank { "GGUF" }
        }

        val knownTotalSize = siblings.mapNotNull { it.size }.takeIf { it.isNotEmpty() }?.sum()
        val sizeBytes = knownTotalSize ?: calculateEstimatedSizeBytes(totalParams, qType)

        val ramMb = if (sizeBytes > 0L) ((sizeBytes / (1024 * 1024)) + 1200).toInt() else 0
        val vramMb = (ramMb * 0.9f).toInt()

        val qualityEvidence = QualityEvidenceEngine.extractQualityEvidence(
            baseRepoId = baseRepoId,
            quantRepoId = repoId,
            quantType = qType,
            readmeText = null
        )

        val displayName = if (siblings.size > 1) "$baseFilename (${siblings.size} split parts)" else baseFilename

        return QuantizationInfo(
            id = "$repoId#$baseFilename",
            format = "GGUF",
            quantType = qType,
            fileSizeBytes = sizeBytes,
            downloadUrl = "https://huggingface.co/$repoId/resolve/main/$primaryFname",
            fileName = displayName,
            sourceRepo = repoId,
            estimatedRamMb = ramMb,
            estimatedVramMb = vramMb,
            relativeQualityScore = qualityEvidence.retentions.firstOrNull()?.retentionPercentage ?: 0f,
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
    ): QuantizationInfo {
        val qType = when (format) {
            "AWQ" -> "AWQ-4bit"
            "GPTQ" -> "GPTQ-4bit"
            "EXL2" -> "EXL2-4.0bpw"
            else -> format
        }

        val sizeBytes = sibling?.size ?: calculateEstimatedSizeBytes(totalParams, qType)
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
            relativeQualityScore = qualityEvidence.retentions.firstOrNull()?.retentionPercentage ?: 0f,
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

    private fun calculateEstimatedSizeBytes(paramsBillion: Float, qType: String): Long {
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
                if ((existing.fileSizeBytes == 0L) && (q.fileSizeBytes > 0L)) {
                    map[key] = q
                }
            }
        }
        return map.values.toList()
    }
}
