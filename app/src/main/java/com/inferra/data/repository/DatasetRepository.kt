package com.inferra.data.repository

import android.util.Log
import com.inferra.data.local.DatasetDao
import com.inferra.data.local.DatasetEntity
import com.inferra.data.network.DatasetMapper
import com.inferra.data.network.HuggingFaceDatasetApi
import com.inferra.domain.model.AiDataset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class DatasetRepository(
    private val api: HuggingFaceDatasetApi?,
    private val datasetDao: DatasetDao,
    private val settingsRepository: SettingsRepository? = null,
) {
    private companion object {
        const val TAG = "DatasetRepository"
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    }

    private suspend fun getAuthHeader(): String? {
        val token = settingsRepository?.userSettingsFlow?.first()?.hfToken?.trim() ?: ""
        return if (token.isNotBlank()) {
            if (token.startsWith("Bearer ", ignoreCase = true)) token else "Bearer $token"
        } else null
    }

    suspend fun getDatasets(forceRefresh: Boolean = false, limit: Int = 30, page: Int = 0): List<AiDataset> = withContext(Dispatchers.IO) {
        if (!forceRefresh) {
            val cached = datasetDao.getAllDatasets().first()
            if (cached.isNotEmpty()) {
                Log.d(TAG, "Returning ${cached.size} datasets from Room database cache")
                return@withContext cached.map { parseEntity(it) }
            }
        }

        if (api == null) {
            val cached = datasetDao.getAllDatasets().first()
            return@withContext cached.map { parseEntity(it) }
        }

        try {
            Log.d(TAG, "Fetching live datasets from Hugging Face API (limit=$limit, page=$page)...")
            val dtos = api.getDatasets(token = getAuthHeader(), limit = limit, page = page, sort = "downloads")
            val domainDatasets = dtos.map { DatasetMapper.mapToDomain(it) }
            if (domainDatasets.isNotEmpty()) {
                saveToLocalDb(domainDatasets)
            }
            domainDatasets
        } catch (e: Exception) {
            Log.e(TAG, "Live getDatasets failed: ${e.message}", e)
            val cached = datasetDao.getAllDatasets().first()
            cached.map { parseEntity(it) }
        }
    }

    suspend fun getDatasetById(id: String): AiDataset? = withContext(Dispatchers.IO) {
        val cached = datasetDao.getDatasetById(id)
        if (cached != null) return@withContext parseEntity(cached)

        if (api == null) return@withContext null

        try {
            Log.d(TAG, "Fetching live dataset detail for '$id'...")
            val dto = api.getDatasetDetail(token = getAuthHeader(), id = id)
            val domainDataset = DatasetMapper.mapToDomain(dto)
            saveToLocalDb(listOf(domainDataset))
            domainDataset
        } catch (e: Exception) {
            Log.e(TAG, "Live getDatasetById failed: ${e.message}", e)
            null
        }
    }

    suspend fun searchDatasets(query: String, limit: Int = 30, page: Int = 0): List<AiDataset> = withContext(Dispatchers.IO) {
        if (api == null) {
            val cached = datasetDao.getAllDatasets().first()
            return@withContext cached.map { parseEntity(it) }
        }

        try {
            val dtos = api.getDatasets(
                token = getAuthHeader(),
                search = query.ifBlank { null },
                limit = limit,
                page = page,
                sort = "downloads"
            )
            val domainDatasets = dtos.map { DatasetMapper.mapToDomain(it) }
            if (domainDatasets.isNotEmpty()) {
                saveToLocalDb(domainDatasets)
            }
            domainDatasets
        } catch (e: Exception) {
            Log.e(TAG, "Live searchDatasets failed: ${e.message}", e)
            emptyList()
        }
    }

    private suspend fun saveToLocalDb(datasets: List<AiDataset>) {
        val entities = datasets.map { dataset ->
            val serializedJson = try {
                json.encodeToString(dataset)
            } catch (e: Exception) {
                ""
            }

            DatasetEntity(
                id = dataset.id,
                name = dataset.name,
                author = dataset.author,
                description = dataset.description,
                downloadsCount = dataset.downloadsCount,
                likesCount = dataset.likesCount,
                updatedAt = dataset.updatedAt,
                totalSizeBytes = dataset.totalSizeBytes,
                license = dataset.license,
                rawJson = serializedJson
            )
        }
        datasetDao.insertDatasets(entities)
    }

    private fun parseEntity(entity: DatasetEntity): AiDataset {
        if (entity.rawJson.isNotBlank()) {
            try {
                return json.decodeFromString<AiDataset>(entity.rawJson)
            } catch (_: Exception) { }
        }

        return AiDataset(
            id = entity.id,
            name = entity.name,
            author = entity.author,
            description = entity.description,
            downloadsCount = entity.downloadsCount,
            likesCount = entity.likesCount,
            updatedAt = entity.updatedAt,
            totalSizeBytes = entity.totalSizeBytes,
            license = entity.license,
            repoUrl = "https://huggingface.co/datasets/${entity.id}"
        )
    }
}
