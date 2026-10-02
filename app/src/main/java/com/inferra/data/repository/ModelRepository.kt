package com.inferra.data.repository

import android.util.Log
import com.inferra.data.local.AndroidBenchmarkEntity
import com.inferra.data.local.AndroidModelDao
import com.inferra.data.local.AndroidModelEntity
import com.inferra.data.local.AndroidModelWideEntity
import com.inferra.data.local.ModelDao
import com.inferra.data.local.ModelEntity
import com.inferra.data.local.ModelWithDetails
import com.inferra.data.local.WatchlistDao
import com.inferra.data.local.WatchlistEntity
import com.inferra.data.network.HuggingFaceApi
import com.inferra.data.network.NetworkToDomainMapper
import com.inferra.domain.model.AiModel
import com.inferra.domain.model.BenchmarkScoreUiModel
import com.inferra.domain.model.CapabilityMatrix
import com.inferra.domain.model.CanonicalModel
import com.inferra.domain.model.EmptyReason
import com.inferra.domain.model.LicenseType
import com.inferra.domain.model.LineageInfo
import com.inferra.domain.model.Modality
import com.inferra.domain.model.ModelTask
import com.inferra.domain.model.UiState
import com.inferra.domain.usecase.CanonicalModelResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class ModelRepository(
    private val api: HuggingFaceApi?,
    private val modelDao: ModelDao,
    private val androidModelDao: AndroidModelDao,
    private val watchlistDao: WatchlistDao,
    private val settingsRepository: SettingsRepository? = null,
) {
    private companion object {
        const val TAG = "ModelRepository"
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    }

    fun observeAndroidModels(
        query: String? = null,
        minParams: Int? = null,
        maxParams: Int? = null,
        minContext: Int? = null,
        isOpenWeights: Boolean? = null,
        license: String? = null,
        sortBy: String = "updated_at",
    ): Flow<UiState<List<AndroidModelEntity>>> {
        val openWeightsInt = isOpenWeights?.let { if (it) 1 else 0 }
        return androidModelDao.searchAndFilterModels(
            query = query?.ifBlank { null },
            minParams = minParams,
            maxParams = maxParams,
            minContext = minContext,
            isOpenWeights = openWeightsInt,
            license = license?.ifBlank { null },
            sortBy = sortBy
        ).map { models ->
            if (models.isEmpty()) {
                UiState.Empty(EmptyReason.NO_RESULTS)
            } else {
                UiState.Success(models)
            }
        }.catch { e ->
            Log.e(TAG, "Error querying android models: ${e.message}", e)
            emit(UiState.Error(e, "Failed to load model catalog. Please try refreshing."))
        }.flowOn(Dispatchers.IO)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeAndroidModelDetails(modelId: String): Flow<UiState<ModelWithDetails>> {
        val canonicalId = CanonicalModelResolver.resolveCanonicalId(modelId)
        val cleanId = modelId.removePrefix("canonical:").substringAfter('/')

        return androidModelDao.getModelWithDetails(modelId, canonicalId, cleanId).flatMapLatest { details ->
            if (details != null) {
                flowOf(UiState.Success(details))
            } else {
                flow<UiState<ModelWithDetails>> {
                    val liveModel = modelDao.getModelById(modelId) ?: modelDao.getModelById(canonicalId)
                    if (liveModel != null) {
                        val entity = AndroidModelEntity(
                            id = modelId,
                            displayName = liveModel.name,
                            familyName = null,
                            organization = liveModel.author,
                            modelType = liveModel.architecture,
                            parameterCount = liveModel.totalParamsBillion.toInt(),
                            contextLength = liveModel.contextLengthTokens,
                            license = liveModel.licenseName,
                            isOpenWeights = 1,
                            updatedAt = liveModel.updatedAt,
                        )
                        androidModelDao.insertModels(listOf(entity))
                    }
                    androidModelDao.getModelWithDetails(modelId, canonicalId, cleanId).collect { syncedDetails ->
                        if (syncedDetails != null) {
                            emit(UiState.Success(syncedDetails))
                        } else {
                            emit(UiState.Empty(EmptyReason.NO_RESULTS))
                        }
                    }
                }
            }
        }.catch { e ->
            Log.e(TAG, "Error querying details for $modelId: ${e.message}", e)
            emit(UiState.Error(e, "Failed to load model details."))
        }.flowOn(Dispatchers.IO)
    }

    fun observeBenchmarkScores(modelId: String): Flow<List<BenchmarkScoreUiModel>> {
        val canonicalId = CanonicalModelResolver.resolveCanonicalId(modelId)
        val cleanId = modelId.removePrefix("canonical:").substringAfter('/')

        return androidModelDao.getBenchmarkScoresForModel(modelId, canonicalId, cleanId).map { queryResults ->
            queryResults.map { (benchmarkId, name, domain, metricName, score, scoreNormalized, measurementType) ->
                BenchmarkScoreUiModel(
                    benchmarkId = benchmarkId,
                    name = name,
                    domain = domain,
                    metricName = metricName,
                    rawScore = score,
                    normalizedScore = scoreNormalized,
                    measurementType = measurementType ?: "UNMEASURED",
                )
            }
        }.catch { e ->
            Log.e(TAG, "Error loading benchmark scores for $modelId: ${e.message}", e)
            emit(emptyList())
        }.flowOn(Dispatchers.IO)
    }

    fun observeWideModels(modelIds: List<String>): Flow<UiState<List<AndroidModelWideEntity>>> {
        return flow {
            emit(UiState.Loading)
            if (modelIds.isEmpty()) {
                androidModelDao.getAllModelsWide().collect { list ->
                    if (list.isEmpty()) emit(UiState.Empty(EmptyReason.NO_COMPARISON_MODELS))
                    else emit(UiState.Success(list))
                }
            } else {
                androidModelDao.getWideModelsByIds(modelIds).collect { list ->
                    if (list.isEmpty()) emit(UiState.Empty(EmptyReason.NO_COMPARISON_MODELS))
                    else emit(UiState.Success(list))
                }
            }
        }.catch { e ->
            Log.e(TAG, "Error loading comparison matrix: ${e.message}", e)
            emit(UiState.Error(e, "Failed to build model comparison matrix."))
        }.flowOn(Dispatchers.IO)
    }

    fun observeAllBenchmarks(): Flow<List<AndroidBenchmarkEntity>> {
        return androidModelDao.getAllBenchmarks().flowOn(Dispatchers.IO)
    }

    private suspend fun getAuthHeader(): String? {
        val token = settingsRepository?.userSettingsFlow?.first()?.hfToken?.trim() ?: ""
        return if (token.isNotBlank()) {
            if (token.startsWith("Bearer ", ignoreCase = true)) token else "Bearer $token"
        } else null
    }

    fun getCanonicalModel(id: String): CanonicalModel {
        val canonicalId = CanonicalModelResolver.resolveCanonicalId(id)
        return CanonicalModelResolver.getCanonicalModel(canonicalId, fallbackDisplayName = id)
    }

    suspend fun getModels(forceRefresh: Boolean = false, limit: Int = 40, page: Int = 0): List<AiModel> = withContext(Dispatchers.IO) {
        if (api == null) {
            val cached = modelDao.getAllModels().first()
            if (cached.isNotEmpty()) return@withContext cached.map { parseEntity(it) }
            return@withContext emptyList()
        }

        if (!forceRefresh) {
            val cached = modelDao.getAllModels().first()
            if (cached.isNotEmpty()) {
                Log.d(TAG, "Returning ${cached.size} models from database cache")
                return@withContext cached.map { parseEntity(it) }
            }
        }

        try {
            Log.d(TAG, "Dispatching live getModels(limit=$limit, page=$page) to Hugging Face API...")
            val dtos = api.getModels(token = getAuthHeader(), limit = limit, page = page, sort = "downloads")
            val domainModels = dtos.map { NetworkToDomainMapper.mapToDomain(it) }
            if (domainModels.isNotEmpty()) {
                saveToLocalDb(domainModels)
            }
            domainModels
        } catch (e: Exception) {
            Log.e(TAG, "Live getModels failed: ${e.message}", e)
            val cached = modelDao.getAllModels().first()
            cached.map { parseEntity(it) }
        }
    }

    suspend fun getModelById(id: String): AiModel? = withContext(Dispatchers.IO) {
        val cached = modelDao.getModelById(id)
        if (cached != null) return@withContext parseEntity(cached)

        if (api == null) return@withContext null

        try {
            Log.d(TAG, "Fetching live detail for model '$id' from Hugging Face...")
            val dto = api.getModelDetail(token = getAuthHeader(), id = id)
            val domainModel = NetworkToDomainMapper.mapToDomain(dto)
            saveToLocalDb(listOf(domainModel))
            domainModel
        } catch (e: Exception) {
            Log.e(TAG, "Live getModelById failed: ${e.message}", e)
            null
        }
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
            val cached = modelDao.getAllModels().first()
            return@withContext cached.map { parseEntity(it) }
        }

        val pipelineTag = mapTaskToPipelineTag(selectedTask)
        try {
            val dtos = api.getModels(
                token = getAuthHeader(),
                search = query.ifBlank { null },
                pipelineTag = pipelineTag,
                limit = limit,
                page = page,
                sort = "downloads"
            )
            val domainModels = dtos.map { NetworkToDomainMapper.mapToDomain(it) }
            if (domainModels.isNotEmpty()) {
                saveToLocalDb(domainModels)
            }

            domainModels.filter { model ->
                val matchesMaxParams = (maxParams == null) || (model.totalParamsBillion <= 0f) || (model.totalParamsBillion <= maxParams)
                val matchesMinParams = (minParams == null) || (model.totalParamsBillion <= 0f) || (model.totalParamsBillion >= minParams)
                val matchesGguf = !isGgufOnly || model.quantizations.any { it.format.equals("GGUF", ignoreCase = true) }
                matchesMaxParams && matchesMinParams && matchesGguf
            }
        } catch (e: Exception) {
            Log.e(TAG, "Live searchModels failed: ${e.message}", e)
            emptyList()
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
            capabilities = CapabilityMatrix(80f, 80f, 80f, 0f, 80f, 80f, 80f, 80f),
            lineage = LineageInfo(),
            isFeatured = entity.isFeatured,
            isTrending = entity.isTrending,
            isNew = entity.isNew,
            repoUrl = entity.repoUrl
        )
    }
}
