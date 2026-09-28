package com.inferra.data.network

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface HuggingFaceApi {

    @GET("api/models")
    suspend fun getModels(
        @Query("search") search: String? = null,
        @Query("sort") sort: String? = "downloads",
        @Query("direction") direction: Int? = -1,
        @Query("limit") limit: Int = 30,
        @Query("filter") filter: String? = null,
        @Query("full") full: Boolean = true
    ): List<HuggingFaceModelDto>

    @GET("api/models/{author}/{modelName}")
    suspend fun getModelDetail(
        @Path("author") author: String,
        @Path("modelName") modelName: String
    ): HuggingFaceModelDto
}
