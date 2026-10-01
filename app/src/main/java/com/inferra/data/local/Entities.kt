package com.inferra.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "models")
data class ModelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val author: String,
    val description: String,
    val architecture: String,
    val totalParamsBillion: Float,
    val activeParamsBillion: Float,
    val isMoe: Boolean,
    val contextLengthTokens: Int,
    val licenseName: String,
    val downloadsCount: Long,
    val likesCount: Long,
    val updatedAt: String,
    val isFeatured: Boolean,
    val isTrending: Boolean,
    val isNew: Boolean,
    val repoUrl: String,
    val rawJson: String // Serialized AiModel JSON for complete fidelity
)

@Entity(tableName = "canonical_models")
data class CanonicalModelEntity(
    @PrimaryKey val id: String,
    val familyId: String,
    val variantTypeStr: String,
    val name: String,
    val author: String,
    val description: String,
    val baseModelId: String? = null,
    val distilledFromId: String? = null,
    val totalParamsBillion: Float,
    val activeParamsBillion: Float,
    val isMoe: Boolean,
    val architecture: String,
    val contextLengthTokens: Int,
    val licenseName: String,
    val rawJson: String
)

@Entity(tableName = "model_families")
data class ModelFamilyEntity(
    @PrimaryKey val id: String,
    val name: String,
    val author: String,
    val description: String,
    val websiteUrl: String
)

@Entity(
    tableName = "model_aliases",
    indices = [Index(value = ["canonicalId"]), Index(value = ["aliasId"])]
)
data class ModelAliasEntity(
    @PrimaryKey val aliasId: String,
    val canonicalId: String,
    val aliasTypeStr: String
)

@Entity(tableName = "benchmark_definitions")
data class BenchmarkDefinitionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val shortName: String,
    val categoryStr: String,
    val description: String,
    val primaryMetricStr: String,
    val higherIsBetter: Boolean
)

@Entity(tableName = "benchmark_versions")
data class BenchmarkVersionEntity(
    @PrimaryKey val id: String,
    val benchmarkId: String,
    val versionName: String,
    val releaseDate: String,
    val specificationUrl: String
)

@Entity(
    tableName = "benchmark_results",
    indices = [Index(value = ["canonicalId"]), Index(value = ["benchmarkVersionId"])]
)
data class BenchmarkResultEntity(
    @PrimaryKey val id: String,
    val canonicalId: String,
    val modelRevision: String,
    val quantizationType: String,
    val benchmarkVersionId: String,
    val methodologyId: String,
    val configurationId: String,
    val score: Float,
    val maxScore: Float,
    val scoreNormalized: Float,
    val provenanceTypeStr: String,
    val publisher: String,
    val sourceUrl: String,
    val verifiedByInferra: Boolean,
    val retrievalDate: String,
    val rawJson: String
)

@Entity(tableName = "providers")
data class ProviderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val logoUrl: String,
    val websiteUrl: String,
    val isSelfHosted: Boolean
)

@Entity(
    tableName = "provider_deployments",
    indices = [Index(value = ["canonicalId"]), Index(value = ["providerId"])]
)
data class ProviderDeploymentEntity(
    @PrimaryKey val id: String,
    val providerId: String,
    val canonicalId: String,
    val providerModelId: String,
    val supportedContextTokens: Int,
    val isAvailable: Boolean,
    val region: String
)

@Entity(
    tableName = "pricing_records",
    indices = [Index(value = ["deploymentId"])]
)
data class PricingRecordEntity(
    @PrimaryKey val id: String,
    val deploymentId: String,
    val inputPricePerMToken: Double,
    val outputPricePerMToken: Double,
    val cachedInputPricePerMToken: Double?,
    val batchPricePerMToken: Double?,
    val currency: String,
    val effectiveDate: String,
    val sourceUrl: String
)

@Entity(
    tableName = "local_benchmarks",
    indices = [Index(value = ["canonicalId"])]
)
data class LocalBenchmarkResultEntity(
    @PrimaryKey val id: String,
    val canonicalId: String,
    val quantizationType: String,
    val modelRevision: String,
    val engineName: String,
    val engineVersion: String,
    val backendName: String,
    val deviceName: String,
    val contextLengthTokens: Int,
    val promptTokenCount: Int,
    val generatedTokenCount: Int,
    val ttftMs: Float,
    val prefillTokensPerSec: Float,
    val decodeTokensPerSec: Float,
    val totalLatencyMs: Long,
    val peakRamMb: Int,
    val peakVramMb: Int,
    val timestampMs: Long
)

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val modelId: String,
    val addedAtEpochMs: Long,
    val notes: String = "",
    val notifyOnUpdate: Boolean = true
)

@Entity(tableName = "download_jobs")
data class DownloadJobEntity(
    @PrimaryKey val id: String,
    val modelId: String,
    val modelName: String,
    val author: String,
    val quantType: String,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val statusStr: String,
    val targetDeviceId: String,
    val targetDeviceName: String,
    val speedBytesPerSec: Long,
    val etaSeconds: Long,
    val errorMessage: String? = null,
    val manifestJson: String
)

@Entity(tableName = "hardware_profiles")
data class HardwareProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val deviceTypeStr: String,
    val cpuName: String,
    val gpuName: String,
    val vramGb: Float,
    val ramGb: Float,
    val osName: String,
    val preferredRuntime: String,
    val availableStorageGb: Float,
    val isLocalDevice: Boolean
)

@Entity(tableName = "device_targets")
data class DeviceTargetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val ipAddress: String,
    val port: Int,
    val osName: String,
    val isOnline: Boolean,
    val lastSeenEpochMs: Long,
    val isPaired: Boolean,
    val runtimesCsv: String
)

@Entity(tableName = "local_models")
data class LocalModelEntity(
    @PrimaryKey val id: String,
    val modelId: String,
    val modelName: String,
    val fileName: String,
    val filePath: String,
    val fileSizeBytes: Long,
    val quantType: String,
    val format: String,
    val sourceRepo: String,
    val installedAtEpochMs: Long,
    val sha256Checksum: String? = null
)
