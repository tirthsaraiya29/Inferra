package com.inferra.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

// ==============================================================================
// Hugging Face Models DTOs
// ==============================================================================

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

// ==============================================================================
// Hugging Face Datasets DTOs
// ==============================================================================

@Serializable
data class HuggingFaceDatasetDto(
    @SerialName("id") val id: String,
    @SerialName("author") val author: String? = null,
    @SerialName("downloads") val downloads: Long? = 0,
    @SerialName("likes") val likes: Long? = 0,
    @SerialName("lastModified") val lastModified: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("private") val isPrivate: Boolean? = false,
    @SerialName("disabled") val isDisabled: Boolean? = false,
    @SerialName("gated") val gated: JsonElement? = null,
    @SerialName("tags") val tags: List<String>? = emptyList(),
    @SerialName("siblings") val siblings: List<HfSiblingDto>? = emptyList(),
    @SerialName("cardData") val cardData: HfCardDataDto? = null
)

// ==============================================================================
// Hugging Face Spaces DTOs
// ==============================================================================

@Serializable
data class HuggingFaceSpaceDto(
    @SerialName("id") val id: String,
    @SerialName("author") val author: String? = null,
    @SerialName("downloads") val downloads: Long? = 0,
    @SerialName("likes") val likes: Long? = 0,
    @SerialName("lastModified") val lastModified: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("private") val isPrivate: Boolean? = false,
    @SerialName("disabled") val isDisabled: Boolean? = false,
    @SerialName("gated") val gated: JsonElement? = null,
    @SerialName("sdk") val sdk: String? = null,
    @SerialName("tags") val tags: List<String>? = emptyList(),
    @SerialName("siblings") val siblings: List<HfSiblingDto>? = emptyList(),
    @SerialName("cardData") val cardData: HfCardDataDto? = null
)

// ==============================================================================
// File Tree DTOs
// ==============================================================================

@Serializable
data class HfFileTreeItemDto(
    @SerialName("path") val path: String,
    @SerialName("type") val type: String = "file",
    @SerialName("size") val size: Long? = null,
    @SerialName("lfs") val lfs: JsonElement? = null,
    @SerialName("oid") val oid: String? = null
)

// ==============================================================================
// User & Auth DTOs
// ==============================================================================

@Serializable
data class HfWhoamiUserDto(
    @SerialName("type") val type: String? = "user",
    @SerialName("name") val name: String,
    @SerialName("fullname") val fullname: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("avatarUrl") val avatarUrl: String? = null,
    @SerialName("isPro") val isPro: Boolean = false,
    @SerialName("periodEnd") val periodEnd: String? = null
)

@Serializable
data class HfWhoamiResponseDto(
    @SerialName("type") val type: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("fullname") val fullname: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("avatarUrl") val avatarUrl: String? = null,
    @SerialName("isPro") val isPro: Boolean = false,
    @SerialName("auth") val auth: JsonElement? = null
)

@Serializable
data class HfUserOverviewDto(
    @SerialName("type") val type: String? = "user",
    @SerialName("name") val name: String,
    @SerialName("fullname") val fullname: String? = null,
    @SerialName("avatarUrl") val avatarUrl: String? = null,
    @SerialName("isPro") val isPro: Boolean = false,
    @SerialName("description") val description: String? = null
)

// ==============================================================================
// Retrofit API Definition
// ==============================================================================

interface HuggingFaceApi {

    // --- Models ---
    @GET("api/models")
    suspend fun getModels(
        @Header("Authorization") token: String? = null,
        @Query("search") search: String? = null,
        @Query("author") author: String? = null,
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

    @GET("api/models/{id}/tree/{revision}")
    suspend fun getModelTree(
        @Header("Authorization") token: String? = null,
        @Path(value = "id", encoded = true) id: String,
        @Path("revision") revision: String = "main"
    ): List<HfFileTreeItemDto>

    @GET("api/models/{id}/tree/{revision}/{path}")
    suspend fun getModelTreePath(
        @Header("Authorization") token: String? = null,
        @Path(value = "id", encoded = true) id: String,
        @Path("revision") revision: String = "main",
        @Path(value = "path", encoded = true) path: String
    ): List<HfFileTreeItemDto>

    // --- Datasets ---
    @GET("api/datasets")
    suspend fun getDatasets(
        @Header("Authorization") token: String? = null,
        @Query("search") search: String? = null,
        @Query("author") author: String? = null,
        @Query("filter") filter: String? = null,
        @Query("sort") sort: String? = "downloads",
        @Query("direction") direction: Int? = -1,
        @Query("limit") limit: Int? = 30,
        @Query("p") page: Int? = null,
        @Query("full") full: Boolean? = true,
        @Query("expand") expand: List<String>? = listOf(
            "downloads", "likes", "tags", "lastModified", "createdAt", "siblings", "cardData"
        )
    ): List<HuggingFaceDatasetDto>

    @GET("api/datasets/{id}")
    suspend fun getDatasetDetail(
        @Header("Authorization") token: String? = null,
        @Path(value = "id", encoded = true) id: String
    ): HuggingFaceDatasetDto

    @GET("api/datasets/{id}/tree/{revision}")
    suspend fun getDatasetTree(
        @Header("Authorization") token: String? = null,
        @Path(value = "id", encoded = true) id: String,
        @Path("revision") revision: String = "main"
    ): List<HfFileTreeItemDto>

    @GET("api/datasets/{id}/tree/{revision}/{path}")
    suspend fun getDatasetTreePath(
        @Header("Authorization") token: String? = null,
        @Path(value = "id", encoded = true) id: String,
        @Path("revision") revision: String = "main",
        @Path(value = "path", encoded = true) path: String
    ): List<HfFileTreeItemDto>

    // --- Spaces ---
    @GET("api/spaces")
    suspend fun getSpaces(
        @Header("Authorization") token: String? = null,
        @Query("search") search: String? = null,
        @Query("author") author: String? = null,
        @Query("filter") filter: String? = null,
        @Query("sort") sort: String? = "likes",
        @Query("direction") direction: Int? = -1,
        @Query("limit") limit: Int? = 30,
        @Query("p") page: Int? = null,
        @Query("full") full: Boolean? = true
    ): List<HuggingFaceSpaceDto>

    @GET("api/spaces/{id}")
    suspend fun getSpaceDetail(
        @Header("Authorization") token: String? = null,
        @Path(value = "id", encoded = true) id: String
    ): HuggingFaceSpaceDto

    @GET("api/spaces/{id}/tree/{revision}")
    suspend fun getSpaceTree(
        @Header("Authorization") token: String? = null,
        @Path(value = "id", encoded = true) id: String,
        @Path("revision") revision: String = "main"
    ): List<HfFileTreeItemDto>

    @GET("api/spaces/{id}/tree/{revision}/{path}")
    suspend fun getSpaceTreePath(
        @Header("Authorization") token: String? = null,
        @Path(value = "id", encoded = true) id: String,
        @Path("revision") revision: String = "main",
        @Path(value = "path", encoded = true) path: String
    ): List<HfFileTreeItemDto>

    // --- Authentication / Whoami ---
    @GET("api/whoami-v2")
    suspend fun whoami(
        @Header("Authorization") token: String
    ): HfWhoamiResponseDto

    // --- Users & Organizations ---
    @GET("api/users/{username}/overview")
    suspend fun getUserOverview(
        @Path("username") username: String
    ): HfUserOverviewDto

    // --- Raw Text / README Content ---
    @GET
    suspend fun getRawFileContent(
        @Url url: String,
        @Header("Authorization") token: String? = null
    ): String
}
