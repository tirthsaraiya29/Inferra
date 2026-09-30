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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ModelRepository(
    private val api: HuggingFaceApi?,
    private val modelDao: ModelDao,
    private val watchlistDao: WatchlistDao,
    private val settingsRepository: SettingsRepository? = null
) {
    private companion object {
        const val TAG = "HuggingFaceApi"
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    }

    private suspend fun getAuthHeader(): String? {
        val token = settingsRepository?.userSettingsFlow?.first()?.hfToken?.trim() ?: ""
        return if (token.isNotBlank()) {
            if (token.startsWith("Bearer ", ignoreCase = true)) token else "Bearer $token"
        } else null
    }

    suspend fun getModels(forceRefresh: Boolean = false, limit: Int = 40, page: Int = 0): List<AiModel> = withContext(Dispatchers.IO) {
        if (api == null) {
            throw IllegalStateException("Hugging Face API client not configured")
        }

        if (!forceRefresh) {
            val cached = modelDao.getAllModels().first()
            if (cached.isNotEmpty()) {
                Log.d(TAG, "Returning ${cached.size} models from database cache")
                return@withContext cached.map { parseEntity(it) }
            }
        }

        Log.d(TAG, "Dispatching live getModels(limit=$limit, page=$page) to Hugging Face API...")
        val dtos = api.getModels(token = getAuthHeader(), limit = limit, page = page, sort = "downloads")
        Log.d(TAG, "Received ${dtos.size} model DTOs from Hugging Face API")
        val domainModels = dtos.map { NetworkToDomainMapper.mapToDomain(it) }
        if (domainModels.isNotEmpty()) {
            saveToLocalDb(domainModels)
        }
        domainModels
    }

    suspend fun getModelById(id: String): AiModel? = withContext(Dispatchers.IO) {
        val cached = modelDao.getModelById(id)
        if (cached != null) {
            return@withContext parseEntity(cached)
        }

        if (api == null) {
            throw IllegalStateException("Hugging Face API client not configured")
        }

        Log.d(TAG, "Fetching live detail for model '$id' from Hugging Face...")
        val dto = api.getModelDetail(token = getAuthHeader(), id = id)
        val domainModel = NetworkToDomainMapper.mapToDomain(dto)
        saveToLocalDb(listOf(domainModel))
        domainModel
    }

    suspend fun searchModels(
        query: String,
        selectedTask: ModelTask? = null,
        maxParams: Float? = null,
        minParams: Float? = null,
        isGgufOnly: Boolean = false,
        page: Int = 0,
        limit: Int = 30
    ): List<AiModel> = withContext(Dispatchers.IO) {
        if (api == null) {
            throw IllegalStateException("Hugging Face API client not configured")
        }

        val pipelineTag = mapTaskToPipelineTag(selectedTask)
        Log.d(TAG, "Dispatching live search query '$query' (pipelineTag=$pipelineTag, page=$page) to Hugging Face API...")

        val dtos = api.getModels(
            token = getAuthHeader(),
            search = query.ifBlank { null },
            pipelineTag = pipelineTag,
            limit = limit,
            page = page,
            sort = "downloads"
        )
        Log.d(TAG, "Live search returned ${dtos.size} DTOs from Hugging Face")
        val domainModels = dtos.map { NetworkToDomainMapper.mapToDomain(it) }
        if (domainModels.isNotEmpty()) {
            saveToLocalDb(domainModels)
        }

        domainModels.filter { model ->
            val matchesMaxParams = maxParams == null || model.totalParamsBillion <= 0f || model.totalParamsBillion <= maxParams
            val matchesMinParams = minParams == null || model.totalParamsBillion <= 0f || model.totalParamsBillion >= minParams
            val matchesGguf = !isGgufOnly || model.quantizations.any { it.format.equals("GGUF", true) }
            matchesMaxParams && matchesMinParams && matchesGguf
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

    private fun mapTaskToPipelineTag(task: ModelTask?): String? {
        return when (task) {
            ModelTask.GENERAL_TEXT -> "text-generation"
            ModelTask.CODING -> null
            ModelTask.REASONING -> "text-generation"
            ModelTask.MATH -> "text-generation"
            ModelTask.VISION -> "image-to-text"
            ModelTask.AGENTIC -> "text-generation"
            ModelTask.TOOL_CALLING -> "text-generation"
            ModelTask.EMBEDDINGS -> "feature-extraction"
            null -> null
        }
    }

    private suspend fun saveToLocalDb(models: List<AiModel>) {
        val entities = models.map { model ->
            val serializedJson = try {
                json.encodeToString(model)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to serialize AiModel '${model.id}': ${e.message}")
                ""
            }

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
                rawJson = serializedJson
            )
        }
        modelDao.insertModels(entities)
    }

    private fun parseEntity(entity: ModelEntity): AiModel {
        if (entity.rawJson.isNotBlank()) {
            try {
                return json.decodeFromString<AiModel>(entity.rawJson)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to deserialize rawJson for model '${entity.id}', falling back to entity columns: ${e.message}")
            }
        }

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
