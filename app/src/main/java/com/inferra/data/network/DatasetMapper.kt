package com.inferra.data.network

import com.inferra.domain.model.AiDataset
import com.inferra.domain.model.DatasetFeature
import com.inferra.domain.model.DatasetFile
import com.inferra.domain.model.DatasetSplit
import com.inferra.domain.model.Modality
import com.inferra.domain.model.ModelTask
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale

object DatasetMapper {

    fun mapToDomain(dto: HuggingFaceDatasetDto): AiDataset {
        val fullId = dto.id
        val parts = fullId.split("/")
        val authorStr = dto.author ?: if (parts.size > 1) parts[0] else "Community"
        val datasetNameStr = if (parts.size > 1) parts[1] else fullId

        val tags = dto.tags ?: emptyList()
        val modalities = extractModalities(tags)
        val tasks = extractTasks(tags)
        val languages = extractLanguages(tags)
        val license = extractLicense(tags, dto.cardData)

        val files = dto.siblings?.mapNotNull { sib ->
            val filename = sib.filename ?: return@mapNotNull null
            DatasetFile(
                filename = filename,
                sizeBytes = sib.size ?: 0L,
                downloadUrl = "https://huggingface.co/datasets/$fullId/resolve/main/$filename"
            )
        } ?: emptyList()

        val downloads = dto.downloads ?: 0L
        val likes = dto.likes ?: 0L
        val totalSize = files.sumOf { it.sizeBytes }

        val desc = "Dataset hosted on Hugging Face Hub by $authorStr (${tags.take(3).joinToString(" • ")}). Live statistics: $downloads downloads, $likes likes."

        return AiDataset(
            id = fullId,
            name = datasetNameStr,
            author = authorStr,
            description = desc,
            downloadsCount = downloads,
            likesCount = likes,
            updatedAt = dto.lastModified?.take(10) ?: "Recently",
            totalSizeBytes = totalSize,
            totalSampleCount = 0L,
            modalities = modalities,
            tasks = tasks,
            languages = languages,
            license = license,
            splits = listOf(
                DatasetSplit(name = "train", numBytes = (totalSize * 0.8).toLong()),
                DatasetSplit(name = "validation", numBytes = (totalSize * 0.1).toLong()),
                DatasetSplit(name = "test", numBytes = (totalSize * 0.1).toLong())
            ),
            features = listOf(
                DatasetFeature(name = "text", dataType = "string"),
                DatasetFeature(name = "label", dataType = "int64")
            ),
            files = files,
            relatedModelIds = emptyList(),
            repoUrl = "https://huggingface.co/datasets/$fullId",
            tags = tags
        )
    }

    private fun extractModalities(tags: List<String>): List<Modality> {
        val list = mutableListOf(Modality.TEXT)
        tags.forEach { tag ->
            val lower = tag.lowercase(Locale.US)
            if (lower.contains("image") || lower.contains("vision")) list.add(Modality.VISION)
            if (lower.contains("audio") || lower.contains("speech")) list.add(Modality.AUDIO)
            if (lower.contains("code")) list.add(Modality.CODE)
        }
        return list.distinct()
    }

    private fun extractTasks(tags: List<String>): List<ModelTask> {
        val list = mutableListOf<ModelTask>()
        tags.forEach { tag ->
            val lower = tag.lowercase(Locale.US)
            if (lower.contains("code")) list.add(ModelTask.CODING)
            if (lower.contains("reason") || lower.contains("math")) list.add(ModelTask.REASONING)
            if (lower.contains("vision") || lower.contains("image")) list.add(ModelTask.VISION)
        }
        if (list.isEmpty()) list.add(ModelTask.GENERAL_TEXT)
        return list.distinct()
    }

    private fun extractLanguages(tags: List<String>): List<String> {
        return tags.filter { it.startsWith("language:") }.map { it.removePrefix("language:").uppercase(Locale.US) }.ifEmpty { listOf("EN") }
    }

    private fun extractLicense(tags: List<String>, cardData: HfCardDataDto?): String {
        val licTag = tags.find { it.startsWith("license:") }
        if (licTag != null) return licTag.removePrefix("license:").uppercase(Locale.US)
        val cardLic = cardData?.license?.jsonPrimitive?.content
        if (!cardLic.isNullOrBlank()) return cardLic.uppercase(Locale.US)
        return "Apache 2.0"
    }
}
