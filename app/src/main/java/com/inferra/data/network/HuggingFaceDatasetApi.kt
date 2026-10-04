package com.inferra.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

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

interface HuggingFaceDatasetApi {

    @GET("api/datasets")
    suspend fun getDatasets(
        @Header("Authorization") token: String? = null,
        @Query("search") search: String? = null,
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
}
