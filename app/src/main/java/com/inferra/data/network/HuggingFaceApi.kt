package com.inferra.data.network

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

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
            "lastModified", "createdAt", "config", "siblings", "cardData"
        )
    ): List<HuggingFaceModelDto>

    @GET("api/models/{id}")
    suspend fun getModelDetail(
        @Header("Authorization") token: String? = null,
        @Path(value = "id", encoded = true) id: String
    ): HuggingFaceModelDto
}
