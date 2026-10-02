package com.inferra.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class Provider(
    val id: String,                         // e.g. "provider:groq", "provider:together", "provider:fireworks"
    val name: String,                       // e.g. "Groq", "Together AI", "Fireworks AI"
    val logoUrl: String = "",
    val websiteUrl: String = "",
    val isSelfHosted: Boolean = false,
)

@Immutable
@Serializable
data class ProviderDeployment(
    val id: String,                         // e.g. "deploy:groq-qwen-32b"
    val providerId: String,
    val canonicalId: String,
    val providerModelId: String,            // e.g. "qwen/qwen-2.5-coder-32b-instruct"
    val supportedContextTokens: Int = 131072,
    val isAvailable: Boolean = true,
    val region: String = "Global"
)

@Immutable
@Serializable
data class PricingRecord(
    val id: String,
    val deploymentId: String,
    val inputPricePerMToken: Double,        // e.g. $0.59 per 1M tokens
    val outputPricePerMToken: Double,       // e.g. $0.79 per 1M tokens
    val cachedInputPricePerMToken: Double? = null,
    val batchPricePerMToken: Double? = null,
    val currency: String = "USD",
    val effectiveDate: String = "",
    val sourceUrl: String = ""
)

@Immutable
@Serializable
data class ProviderMeasurement(
    val id: String,
    val deploymentId: String,
    val prefillTokensPerSec: Float = 0f,
    val decodeTokensPerSec: Float = 0f,
    val ttftMs: Float = 0f,
    val measuredAtEpochMs: Long = System.currentTimeMillis(),
    val source: String = "Provider Docs / Independent Benchmark"
)
