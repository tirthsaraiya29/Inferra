package com.inferra.data.repository

import android.util.Log
import com.inferra.data.local.ModelDao
import com.inferra.data.local.ModelEntity
import com.inferra.data.local.WatchlistDao
import com.inferra.data.local.WatchlistEntity
import com.inferra.data.network.HuggingFaceApi
import com.inferra.data.network.NetworkToDomainMapper
import com.inferra.domain.model.AiModel
import com.inferra.domain.model.CapabilityMatrix
import com.inferra.domain.model.LicenseType
import com.inferra.domain.model.LineageInfo
import com.inferra.domain.model.Modality
import com.inferra.domain.model.ModelTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ModelRepository(
    private val api: HuggingFaceApi?,
    private val modelDao: ModelDao,
    private val watchlistDao: WatchlistDao
) {
    private companion object {
        const val TAG = "HuggingFaceApi"
    }

    suspend fun getModels(forceRefresh: Boolean = false): List<AiModel> = withContext(Dispatchers.IO) {
        val localEntities = modelDao.getAllModels().first()
        if (localEntities.isNotEmpty() && !forceRefresh) {
            Log.d(TAG, "Returning ${localEntities.size} models from local database cache")
            return@withContext localEntities.map { parseEntity(it) }
        }

        if (api == null) {
            Log.e(TAG, "HuggingFaceApi instance is null. Cannot fetch models.")
            throw IllegalStateException("Network client not configured")
        }

        Log.d(TAG, "Dispatching live getModels() request to Hugging Face API...")
        try {
            val dtos = api.getModels(limit = 40, sort = "downloads")
            Log.d(TAG, "Received ${dtos.size} model DTOs from Hugging Face API")
            val domainModels = dtos.map { NetworkToDomainMapper.mapToDomain(it) }
            if (domainModels.isNotEmpty()) {
                saveToLocalDb(domainModels)
                Log.d(TAG, "Saved ${domainModels.size} live models to database cache")
                return@withContext domainModels
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch live models from Hugging Face API: ${e.localizedMessage}", e)
            if (localEntities.isNotEmpty()) {
                Log.d(TAG, "Falling back to ${localEntities.size} cached local database models")
                return@withContext localEntities.map { parseEntity(it) }
            }
            throw e
        }

        emptyList()
    }

    suspend fun getModelById(id: String): AiModel? = withContext(Dispatchers.IO) {
        val entity = modelDao.getModelById(id)
        if (entity != null) {
            return@withContext parseEntity(entity)
        }

        if (api != null && id.contains("/")) {
            try {
                val parts = id.split("/", limit = 2)
                Log.d(TAG, "Fetching live detail for model '${parts[0]}/${parts[1]}' from Hugging Face...")
                val dto = api.getModelDetail(author = parts[0], modelName = parts[1])
                val domainModel = NetworkToDomainMapper.mapToDomain(dto)
                saveToLocalDb(listOf(domainModel))
                return@withContext domainModel
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch model detail for '$id': ${e.localizedMessage}")
            }
        }

        val localEntities = modelDao.getAllModels().first()
        localEntities.map { parseEntity(it) }.find { it.id == id || it.name.equals(id, ignoreCase = true) }
    }

    suspend fun searchModels(
        query: String,
        selectedTask: ModelTask? = null,
        maxParams: Float? = null,
        minParams: Float? = null,
        isGgufOnly: Boolean = false
    ): List<AiModel> = withContext(Dispatchers.IO) {
        var baseModels: List<AiModel> = emptyList()

        if (api != null && query.isNotBlank()) {
            try {
                Log.d(TAG, "Dispatching live search query '$query' to Hugging Face API...")
                val dtos = api.getModels(search = query, limit = 30, sort = "downloads")
                Log.d(TAG, "Live search for '$query' returned ${dtos.size} DTOs from Hugging Face")
                baseModels = dtos.map { NetworkToDomainMapper.mapToDomain(it) }
            } catch (e: Exception) {
                Log.e(TAG, "Live search for '$query' failed: ${e.localizedMessage}")
            }
        }

        if (baseModels.isEmpty()) {
            val localEntities = modelDao.getAllModels().first()
            baseModels = localEntities.map { parseEntity(it) }
        }

        baseModels.filter { model ->
            val matchesQuery = query.isBlank() || 
                model.name.contains(query, ignoreCase = true) ||
                model.author.contains(query, ignoreCase = true) ||
                model.description.contains(query, ignoreCase = true) ||
                model.architecture.contains(query, ignoreCase = true)

            val matchesTask = selectedTask == null || model.tasks.contains(selectedTask)
            val matchesMaxParams = maxParams == null || model.totalParamsBillion <= maxParams
            val matchesMinParams = minParams == null || model.totalParamsBillion >= minParams
            val matchesGguf = !isGgufOnly || model.quantizations.any { it.format.equals("GGUF", true) }

            matchesQuery && matchesTask && matchesMaxParams && matchesMinParams && matchesGguf
        }
    }

    fun getWatchlist(): Flow<List<String>> {
        return watchlistDao.getWatchlist().map { items -> items.map { it.modelId } }
    }

    fun isWatchlisted(modelId: String): Flow<Boolean> {
        return watchlistDao.isWatchlisted(modelId)
    }

    suspend fun toggleWatchlist(modelId: String) = withContext(Dispatchers.IO) {
        val isSaved = watchlistDao.isWatchlisted(modelId).first()
        if (isSaved) {
            watchlistDao.removeWatchlist(modelId)
        } else {
            watchlistDao.addWatchlist(
                WatchlistEntity(
                    modelId = modelId,
                    addedAtEpochMs = System.currentTimeMillis()
                )
            )
        }
    }

    private suspend fun saveToLocalDb(models: List<AiModel>) {
        val entities = models.map { model ->
            ModelEntity(
                id = model.id,
                name = model.name,
                author = model.author,
                description = model.description,
                architecture = model.architecture,
                totalParamsBillion = model.totalParamsBillion,
                activeParamsBillion = model.activeParamsBillion,
                isMoe = model.isMoe,
                contextLengthTokens = model.contextLengthTokens,
                licenseName = model.licenseName,
                downloadsCount = model.downloadsCount,
                likesCount = model.likesCount,
                updatedAt = model.updatedAt,
                isFeatured = model.isFeatured,
                isTrending = model.isTrending,
                isNew = model.isNew,
                repoUrl = model.repoUrl,
                rawJson = ""
            )
        }
        modelDao.insertModels(entities)
    }

    private fun parseEntity(entity: ModelEntity): AiModel {
        return AiModel(
            id = entity.id,
            name = entity.name,
            author = entity.author,
            description = entity.description,
            architecture = entity.architecture,
            totalParamsBillion = entity.totalParamsBillion,
            activeParamsBillion = entity.activeParamsBillion,
            isMoe = entity.isMoe,
            contextLengthTokens = entity.contextLengthTokens,
            modalities = listOf(Modality.TEXT),
            tasks = listOf(ModelTask.GENERAL_TEXT),
            license = LicenseType.APACHE_2,
            licenseName = entity.licenseName,
            downloadsCount = entity.downloadsCount,
            likesCount = entity.likesCount,
            updatedAt = entity.updatedAt,
            quantizations = emptyList(),
            benchmarks = emptyList(),
            capabilities = CapabilityMatrix(80f, 80f, 80f, 0f, 80f, 80f, 80f, 80f),
            lineage = LineageInfo(),
            isFeatured = entity.isFeatured,
            isTrending = entity.isTrending,
            isNew = entity.isNew,
            repoUrl = entity.repoUrl
        )
    }
}
