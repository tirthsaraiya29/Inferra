package com.inferra.data.local

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

// ==============================================================================
// Catalog & Benchmark Entities (From Pre-packaged SQLite Asset)
// ==============================================================================

@Entity(tableName = "android_models")
data class AndroidModelEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "family_name") val familyName: String?,
    @ColumnInfo(name = "organization") val organization: String,
    @ColumnInfo(name = "model_type") val modelType: String,
    @ColumnInfo(name = "parameter_count") val parameterCount: Int?,
    @ColumnInfo(name = "context_length") val contextLength: Int?,
    @ColumnInfo(name = "license") val license: String?,
    @ColumnInfo(name = "is_open_weights") val isOpenWeights: Int,
    @ColumnInfo(name = "updated_at") val updatedAt: String,
)

@Entity(
    tableName = "android_artifacts",
    foreignKeys = [
        ForeignKey(
            entity = AndroidModelEntity::class,
            parentColumns = ["id"],
            childColumns = ["model_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("model_id")]
)
data class AndroidArtifactEntity(
    @PrimaryKey @ColumnInfo(name = "artifact_id") val artifactId: String,
    @ColumnInfo(name = "model_id") val modelId: String,
    @ColumnInfo(name = "format") val format: String,
    @ColumnInfo(name = "quantization") val quantization: String?,
    @ColumnInfo(name = "file_size_bytes") val fileSizeBytes: Long?,
    @ColumnInfo(name = "repository_id") val repositoryId: String,
    @ColumnInfo(name = "file_path") val filePath: String?
)

@Entity(tableName = "android_benchmarks")
data class AndroidBenchmarkEntity(
    @PrimaryKey @ColumnInfo(name = "benchmark_id") val benchmarkId: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "domain") val domain: String,
    @ColumnInfo(name = "metric_name") val metricName: String
)

@Entity(
    tableName = "android_benchmark_scores",
    foreignKeys = [
        ForeignKey(
            entity = AndroidModelEntity::class,
            parentColumns = ["id"],
            childColumns = ["model_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AndroidBenchmarkEntity::class,
            parentColumns = ["benchmark_id"],
            childColumns = ["benchmark_id"]
        )
    ],
    indices = [Index("model_id"), Index("benchmark_id")]
)
data class AndroidBenchmarkScoreEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "model_id") val modelId: String,
    @ColumnInfo(name = "benchmark_id") val benchmarkId: String,
    @ColumnInfo(name = "score") val score: Double,
    @ColumnInfo(name = "score_normalized") val scoreNormalized: Double,
    @ColumnInfo(name = "measurement_type") val measurementType: String
)

@Entity(
    tableName = "android_provider_pricing",
    foreignKeys = [
        ForeignKey(
            entity = AndroidModelEntity::class,
            parentColumns = ["id"],
            childColumns = ["model_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("model_id")]
)
data class AndroidProviderPricingEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "model_id") val modelId: String,
    @ColumnInfo(name = "provider_name") val providerName: String,
    @ColumnInfo(name = "input_cost_per_m") val inputCostPerM: Double?,
    @ColumnInfo(name = "output_cost_per_m") val outputCostPerM: Double?,
    @ColumnInfo(name = "context_window") val contextWindow: Int?,
    @ColumnInfo(name = "updated_at") val updatedAt: String
)

@Entity(
    tableName = "android_models_wide",
    foreignKeys = [
        ForeignKey(
            entity = AndroidModelEntity::class,
            parentColumns = ["id"],
            childColumns = ["model_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class AndroidModelWideEntity(
    @PrimaryKey @ColumnInfo(name = "model_id") val modelId: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "organization") val organization: String,
    @ColumnInfo(name = "parameter_count") val parameterCount: Int?,
    @ColumnInfo(name = "context_length") val contextLength: Int?,
    @ColumnInfo(name = "license") val license: String?,
    @ColumnInfo(name = "mmlu_pro") val mmluPro: Double?,
    @ColumnInfo(name = "gpqa") val gpqa: Double?,
    @ColumnInfo(name = "math_l5") val mathL5: Double?,
    @ColumnInfo(name = "ifeval") val ifeval: Double?,
    @ColumnInfo(name = "musr") val musr: Double?,
    @ColumnInfo(name = "bbh") val bbh: Double?,
    @ColumnInfo(name = "aime") val aime: Double?,
    @ColumnInfo(name = "gsm8k") val gsm8k: Double?,
    @ColumnInfo(name = "swe_bench_verified") val sweBenchVerified: Double?,
    @ColumnInfo(name = "swe_bench_lite") val sweBenchLite: Double?,
    @ColumnInfo(name = "humaneval_plus") val humanevalPlus: Double?,
    @ColumnInfo(name = "mbpp_plus") val mbppPlus: Double?,
    @ColumnInfo(name = "livecodebench") val livecodebench: Double?,
    @ColumnInfo(name = "aider_polyglot") val aiderPolyglot: Double?,
    @ColumnInfo(name = "bfcl") val bfcl: Double?,
    @ColumnInfo(name = "tau_bench") val tauBench: Double?,
    @ColumnInfo(name = "gaia") val gaia: Double?,
    @ColumnInfo(name = "ruler") val ruler: Double?,
    @ColumnInfo(name = "mmmu") val mmmu: Double?,
    @ColumnInfo(name = "mathvista") val mathvista: Double?,
    @ColumnInfo(name = "chartqa") val chartqa: Double?,
    @ColumnInfo(name = "docvqa") val docvqa: Double?,
    @ColumnInfo(name = "video_mme") val videoMme: Double?,
    @ColumnInfo(name = "livebench") val livebench: Double?,
    @ColumnInfo(name = "simpleqa") val simpleqa: Double?,
    @ColumnInfo(name = "arena_elo") val arenaElo: Double?,
    @ColumnInfo(name = "arena_coding_elo") val arenaCodingElo: Double?,
    @ColumnInfo(name = "arena_hard_elo") val arenaHardElo: Double?
)

data class ModelWithDetails(
    @Embedded val model: AndroidModelEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "model_id"
    )
    val artifacts: List<AndroidArtifactEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "model_id"
    )
    val benchmarkScores: List<AndroidBenchmarkScoreEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "model_id"
    )
    val providerPricing: List<AndroidProviderPricingEntity>
)

// ==============================================================================
// Live App State & Local Management Entities
// ==============================================================================

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
    val rawJson: String
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
