package com.inferra.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HuggingFaceModelDto(
    @SerialName("id") val id: String,                         // e.g. "Qwen/Qwen2.5-32B-Instruct"
    @SerialName("author") val author: String? = null,
    @SerialName("downloads") val downloads: Long? = 0,
    @SerialName("likes") val likes: Long? = 0,
    @SerialName("lastModified") val lastModified: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("private") val isPrivate: Boolean? = false,
    @SerialName("disabled") val isDisabled: Boolean? = false,
    @SerialName("gated") val gated: Boolean? = false,
    @SerialName("pipeline_tag") val pipelineTag: String? = null,
    @SerialName("tags") val tags: List<String>? = emptyList(),
    @SerialName("siblings") val siblings: List<HfSiblingDto>? = emptyList(),
    @SerialName("config") val config: HfConfigDto? = null
)

@Serializable
data class HfSiblingDto(
    @SerialName("rfilename") val filename: String
)

@Serializable
data class HfConfigDto(
    @SerialName("architectures") val architectures: List<String>? = emptyList(),
    @SerialName("model_type") val modelType: String? = null,
    @SerialName("num_hidden_layers") val numLayers: Int? = null,
    @SerialName("max_position_embeddings") val maxPositionEmbeddings: Int? = null,
    @SerialName("vocab_size") val vocabSize: Int? = null
)
