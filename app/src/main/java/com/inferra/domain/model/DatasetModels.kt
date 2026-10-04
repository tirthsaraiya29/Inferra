package com.inferra.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class DatasetSplit(
    val name: String,
    val numBytes: Long = 0L,
    val numExamples: Long = 0L
)

@Immutable
@Serializable
data class DatasetFeature(
    val name: String,
    val dataType: String
)

@Immutable
@Serializable
data class DatasetFile(
    val filename: String,
    val sizeBytes: Long = 0L,
    val downloadUrl: String = ""
)

@Immutable
@Serializable
data class AiDataset(
    val id: String,
    val name: String,
    val author: String,
    val description: String,
    val downloadsCount: Long = 0L,
    val likesCount: Long = 0L,
    val updatedAt: String = "",
    val totalSizeBytes: Long = 0L,
    val totalSampleCount: Long = 0L,
    val modalities: List<Modality> = emptyList(),
    val tasks: List<ModelTask> = emptyList(),
    val languages: List<String> = emptyList(),
    val license: String = "Apache 2.0",
    val splits: List<DatasetSplit> = emptyList(),
    val features: List<DatasetFeature> = emptyList(),
    val files: List<DatasetFile> = emptyList(),
    val relatedModelIds: List<String> = emptyList(),
    val repoUrl: String = "",
    val tags: List<String> = emptyList()
)
