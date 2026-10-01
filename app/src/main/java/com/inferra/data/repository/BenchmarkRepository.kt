package com.inferra.data.repository

import com.inferra.data.local.BenchmarkDao
import com.inferra.domain.model.BenchmarkResult
import com.inferra.domain.usecase.BenchmarkRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class BenchmarkRepository(
    private val benchmarkDao: BenchmarkDao
) {

    fun getBenchmarkResultsForCanonicalModel(canonicalId: String): Flow<List<BenchmarkResult>> {
        return benchmarkDao.getResultsForCanonicalModel(canonicalId).map { entities ->
            if (entities.isNotEmpty()) {
                // Return mapped entities
                emptyList() // Fallback to registry if unparsed
            } else {
                BenchmarkRegistry.getStandardBenchmarksForModel(canonicalId)
            }
        }.flowOn(Dispatchers.IO)
    }
}
