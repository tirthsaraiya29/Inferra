package com.inferra.data.repository

import com.inferra.data.local.LocalBenchmarkDao
import com.inferra.data.local.LocalBenchmarkResultEntity
import com.inferra.domain.model.LocalBenchmarkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class LocalBenchmarkRepository(
    private val localBenchmarkDao: LocalBenchmarkDao
) {

    fun getResultsForModel(canonicalId: String): Flow<List<LocalBenchmarkResult>> {
        return localBenchmarkDao.getResultsForModel(canonicalId).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getAllLocalResults(): Flow<List<LocalBenchmarkResult>> {
        return localBenchmarkDao.getAllLocalResults().map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun saveResult(result: LocalBenchmarkResult) = withContext(Dispatchers.IO) {
        localBenchmarkDao.insertResult(result.toEntity())
    }

    private fun LocalBenchmarkResultEntity.toDomain() = LocalBenchmarkResult(
        id = id,
        canonicalId = canonicalId,
        quantizationType = quantizationType,
        modelRevision = modelRevision,
        engineName = engineName,
        engineVersion = engineVersion,
        backendName = backendName,
        deviceName = deviceName,
        contextLengthTokens = contextLengthTokens,
        promptTokenCount = promptTokenCount,
        generatedTokenCount = generatedTokenCount,
        ttftMs = ttftMs,
        prefillTokensPerSec = prefillTokensPerSec,
        decodeTokensPerSec = decodeTokensPerSec,
        totalLatencyMs = totalLatencyMs,
        peakRamMb = peakRamMb,
        peakVramMb = peakVramMb,
        timestampMs = timestampMs
    )

    private fun LocalBenchmarkResult.toEntity() = LocalBenchmarkResultEntity(
        id = id,
        canonicalId = canonicalId,
        quantizationType = quantizationType,
        modelRevision = modelRevision,
        engineName = engineName,
        engineVersion = engineVersion,
        backendName = backendName,
        deviceName = deviceName,
        contextLengthTokens = contextLengthTokens,
        promptTokenCount = promptTokenCount,
        generatedTokenCount = generatedTokenCount,
        ttftMs = ttftMs,
        prefillTokensPerSec = prefillTokensPerSec,
        decodeTokensPerSec = decodeTokensPerSec,
        totalLatencyMs = totalLatencyMs,
        peakRamMb = peakRamMb,
        peakVramMb = peakVramMb,
        timestampMs = timestampMs
    )
}
