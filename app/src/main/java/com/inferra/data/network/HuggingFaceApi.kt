package com.inferra.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
data class HuggingFaceModelDto(
    @SerialName("id") val id: String,
    @SerialName("author") val author: String? = null,
    @SerialName("downloads") val downloads: Long? = 0,
    @SerialName("likes") val likes: Long? = 0,
    @SerialName("lastModified") val lastModified: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("private") val isPrivate: Boolean? = false,
    @SerialName("disabled") val isDisabled: Boolean? = false,
    @SerialName("gated") val gated: JsonElement? = null,
    @SerialName("pipeline_tag") val pipelineTag: String? = null,
    @SerialName("library_name") val libraryName: String? = null,
    @SerialName("tags") val tags: List<String>? = emptyList(),
    @SerialName("siblings") val siblings: List<HfSiblingDto>? = emptyList(),
    @SerialName("config") val config: HfConfigDto? = null,
    @SerialName("cardData") val cardData: HfCardDataDto? = null
)

@Serializable
data class HfSiblingDto(
    @SerialName("rfilename") val filename: String? = "",
    @SerialName("size") val size: Long? = null,
    @SerialName("lfs") val lfs: JsonElement? = null
)

@Serializable
data class HfConfigDto(
    @SerialName("architectures") val architectures: List<String>? = emptyList(),
    @SerialName("model_type") val modelType: String? = null,
    @SerialName("num_hidden_layers") val numLayers: Int? = null,
    @SerialName("max_position_embeddings") val maxPositionEmbeddings: JsonElement? = null,
    @SerialName("vocab_size") val vocabSize: Int? = null,
    @SerialName("hidden_size") val hiddenSize: Int? = null,
    @SerialName("intermediate_size") val intermediateSize: Int? = null
)

@Serializable
data class HfCardDataDto(
    @SerialName("license") val license: JsonElement? = null,
    @SerialName("language") val language: JsonElement? = null
)

interface HuggingFaceApi {

    @GET("api/models")
    suspend fun getModels(
        @Header("Authorization") token: String? = null,
        @Query("search") search: String? = null,
        @Query("pipeline_tag") pipelineTag: String? = null,
        @Query("filter") filter: String? = null,
        @Query("sort") sort: String? = "downloads",
        @Query("direction") direction: Int? = -1,
        @Query("limit") limit: Int? = 30,
        @Query("p") page: Int? = null,
        @Query("full") full: Boolean? = true,
        @Query("expand") expand: List<String>? = listOf(
            "downloads", "likes", "pipeline_tag", "tags",
            "lastModified", "createdAt", "config", "siblings", "cardData",
        ),
    ): List<HuggingFaceModelDto>

    @GET("api/models/{id}")
    suspend fun getModelDetail(
        @Header("Authorization") token: String? = null,
        @Path(value = "id", encoded = true) id: String
    ): HuggingFaceModelDto
}
