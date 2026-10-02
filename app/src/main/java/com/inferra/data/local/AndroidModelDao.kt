package com.inferra.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

data class BenchmarkScoreQueryResult(
    val benchmarkId: String,
    val name: String,
    val domain: String,
    val metricName: String,
    val score: Double?,
    val scoreNormalized: Double?,
    val measurementType: String?
)

@Dao
interface AndroidModelDao {

    @Query("SELECT * FROM android_models ORDER BY updated_at DESC")
    fun getAllModels(): Flow<List<AndroidModelEntity>>

    @Query("SELECT * FROM android_models WHERE id = :id LIMIT 1")
    suspend fun getModelById(id: String): AndroidModelEntity?

    @Transaction
    @Query("SELECT * FROM android_models WHERE id = :id OR id = :canonicalId OR id LIKE '%' || :cleanId || '%' OR display_name LIKE '%' || :cleanId || '%' LIMIT 1")
    fun getModelWithDetails(id: String, canonicalId: String = id, cleanId: String = id): Flow<ModelWithDetails?>

    @Query("SELECT * FROM android_artifacts WHERE model_id = :modelId OR model_id = :canonicalId OR model_id LIKE '%' || :cleanId || '%'")
    fun getArtifactsForModel(modelId: String, canonicalId: String = modelId, cleanId: String = modelId): Flow<List<AndroidArtifactEntity>>

    @Query("SELECT * FROM android_provider_pricing WHERE model_id = :modelId OR model_id = :canonicalId OR model_id LIKE '%' || :cleanId || '%' ORDER BY updated_at DESC")
    fun getProviderPricingForModel(modelId: String, canonicalId: String = modelId, cleanId: String = modelId): Flow<List<AndroidProviderPricingEntity>>

    @Query("SELECT * FROM android_benchmarks ORDER BY domain, name")
    fun getAllBenchmarks(): Flow<List<AndroidBenchmarkEntity>>

    @Query("""
        SELECT 
            b.benchmark_id AS benchmarkId,
            b.name AS name,
            b.domain AS domain,
            b.metric_name AS metricName,
            s.score AS score,
            s.score_normalized AS scoreNormalized,
            s.measurement_type AS measurementType
        FROM android_benchmarks b
        LEFT JOIN android_benchmark_scores s 
            ON b.benchmark_id = s.benchmark_id AND (s.model_id = :modelId OR s.model_id = :canonicalId OR s.model_id LIKE '%' || :cleanId || '%')
        ORDER BY b.domain, b.name
    """)
    fun getBenchmarkScoresForModel(modelId: String, canonicalId: String = modelId, cleanId: String = modelId): Flow<List<BenchmarkScoreQueryResult>>

    @Query("""
        SELECT * FROM android_models
        WHERE (:query IS NULL OR :query = '' 
            OR id LIKE '%' || :query || '%' 
            OR display_name LIKE '%' || :query || '%' 
            OR organization LIKE '%' || :query || '%' 
            OR family_name LIKE '%' || :query || '%'
            OR model_type LIKE '%' || :query || '%'
            OR license LIKE '%' || :query || '%')
          AND (:minParams IS NULL OR (parameter_count IS NOT NULL AND parameter_count >= :minParams))
          AND (:maxParams IS NULL OR (parameter_count IS NOT NULL AND parameter_count <= :maxParams))
          AND (:minContext IS NULL OR (context_length IS NOT NULL AND context_length >= :minContext))
          AND (:isOpenWeights IS NULL OR is_open_weights = :isOpenWeights)
          AND (:license IS NULL OR :license = '' OR license LIKE '%' || :license || '%')
        ORDER BY 
            CASE WHEN :sortBy = 'params' THEN parameter_count END DESC,
            CASE WHEN :sortBy = 'context' THEN context_length END DESC,
            updated_at DESC
    """)
    fun searchAndFilterModels(
        query: String?,
        minParams: Int?,
        maxParams: Int?,
        minContext: Int?,
        isOpenWeights: Int?,
        license: String?,
        sortBy: String = "updated_at",
    ): Flow<List<AndroidModelEntity>>

    @Query("SELECT * FROM android_models_wide")
    fun getAllModelsWide(): Flow<List<AndroidModelWideEntity>>

    @Query("SELECT * FROM android_models_wide WHERE model_id IN (:modelIds)")
    fun getWideModelsByIds(modelIds: List<String>): Flow<List<AndroidModelWideEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModels(models: List<AndroidModelEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtifacts(artifacts: List<AndroidArtifactEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBenchmarks(benchmarks: List<AndroidBenchmarkEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScores(scores: List<AndroidBenchmarkScoreEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPricing(pricing: List<AndroidProviderPricingEntity>)
}
