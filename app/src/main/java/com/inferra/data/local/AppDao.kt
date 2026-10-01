package com.inferra.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

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
interface BenchmarkDao {
    @Query("SELECT * FROM benchmark_definitions")
    fun getAllDefinitions(): Flow<List<BenchmarkDefinitionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDefinitions(definitions: List<BenchmarkDefinitionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersions(versions: List<BenchmarkVersionEntity>)

    @Query("SELECT * FROM benchmark_results WHERE canonicalId = :canonicalId")
    fun getResultsForCanonicalModel(canonicalId: String): Flow<List<BenchmarkResultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResults(results: List<BenchmarkResultEntity>)
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
