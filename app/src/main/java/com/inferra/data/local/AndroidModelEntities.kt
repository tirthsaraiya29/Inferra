package com.inferra.data.local

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

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
