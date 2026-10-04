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

@Dao
interface ModelDao {
    @Query("SELECT * FROM models ORDER BY downloadsCount DESC")
    fun getAllModels(): Flow<List<ModelEntity>>

    @Query("SELECT * FROM models WHERE id = :modelId LIMIT 1")
    suspend fun getModelById(modelId: String): ModelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModels(models: List<ModelEntity>)

    @Query("DELETE FROM models")
    suspend fun clearAll()
}

@Dao
interface CanonicalModelDao {
    @Query("SELECT * FROM canonical_models WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CanonicalModelEntity?

    @Query("SELECT * FROM canonical_models")
    fun getAllCanonicalModels(): Flow<List<CanonicalModelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCanonicalModels(models: List<CanonicalModelEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFamily(family: ModelFamilyEntity)

    @Query("SELECT canonicalId FROM model_aliases WHERE aliasId = :aliasId LIMIT 1")
    suspend fun resolveAlias(aliasId: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAliases(aliases: List<ModelAliasEntity>)
}

@Dao
interface ProviderDao {
    @Query("SELECT * FROM providers")
    fun getAllProviders(): Flow<List<ProviderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProviders(providers: List<ProviderEntity>)

    @Query("SELECT * FROM provider_deployments WHERE canonicalId = :canonicalId")
    fun getDeploymentsForModel(canonicalId: String): Flow<List<ProviderDeploymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeployments(deployments: List<ProviderDeploymentEntity>)

    @Query("SELECT * FROM pricing_records WHERE deploymentId = :deploymentId ORDER BY effectiveDate DESC LIMIT 1")
    suspend fun getLatestPricing(deploymentId: String): PricingRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPricing(pricing: List<PricingRecordEntity>)
}

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist ORDER BY addedAtEpochMs DESC")
    fun getWatchlist(): Flow<List<WatchlistEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE modelId = :modelId)")
    fun isWatchlisted(modelId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addWatchlist(item: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE modelId = :modelId")
    suspend fun removeWatchlist(modelId: String)
}

@Dao
interface DownloadJobDao {
    @Query("SELECT * FROM download_jobs ORDER BY id DESC")
    fun getAllJobs(): Flow<List<DownloadJobEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: DownloadJobEntity)

    @Query("DELETE FROM download_jobs WHERE id = :jobId")
    suspend fun deleteJob(jobId: String)
}

@Dao
interface HardwareProfileDao {
    @Query("SELECT * FROM hardware_profiles")
    fun getAllProfiles(): Flow<List<HardwareProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: HardwareProfileEntity)

    @Query("DELETE FROM hardware_profiles WHERE id = :id")
    suspend fun deleteProfile(id: String)
}

@Dao
interface DeviceTargetDao {
    @Query("SELECT * FROM device_targets")
    fun getAllDevices(): Flow<List<DeviceTargetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: DeviceTargetEntity)
}

@Dao
interface LocalModelDao {
    @Query("SELECT * FROM local_models ORDER BY installedAtEpochMs DESC")
    fun getAllLocalModels(): Flow<List<LocalModelEntity>>

    @Query("SELECT * FROM local_models WHERE id = :id LIMIT 1")
    suspend fun getLocalModelById(id: String): LocalModelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocalModel(model: LocalModelEntity)

    @Query("DELETE FROM local_models WHERE id = :id")
    suspend fun deleteLocalModel(id: String)
}
