package com.inferra.data.repository

import com.inferra.data.local.PricingRecordEntity
import com.inferra.data.local.ProviderDao
import com.inferra.data.local.ProviderDeploymentEntity
import com.inferra.data.local.ProviderEntity
import com.inferra.domain.model.PricingRecord
import com.inferra.domain.model.Provider
import com.inferra.domain.model.ProviderDeployment
import com.inferra.domain.model.ProviderMeasurement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ProviderRepository(
    private val providerDao: ProviderDao
) {

    val allProvidersFlow: Flow<List<Provider>> = providerDao.getAllProviders().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun seedDefaultProviders() = withContext(Dispatchers.IO) {
        val providers = listOf(
            ProviderEntity("provider:groq", "Groq", "https://groq.com/favicon.ico", "https://groq.com", false),
            ProviderEntity("provider:together", "Together AI", "https://together.ai/favicon.ico", "https://together.ai", false),
            ProviderEntity("provider:fireworks", "Fireworks AI", "https://fireworks.ai/favicon.ico", "https://fireworks.ai", false),
            ProviderEntity("provider:deepinfra", "DeepInfra", "https://deepinfra.com/favicon.ico", "https://deepinfra.com", false),
            ProviderEntity("provider:openrouter", "OpenRouter", "https://openrouter.ai/favicon.ico", "https://openrouter.ai", false)
        )
        providerDao.insertProviders(providers)

        val deployments = listOf(
            ProviderDeploymentEntity("deploy:groq-qwen-32b", "provider:groq", "canonical:qwen-qwen2.5-coder-32b-instruct", "qwen/qwen-2.5-coder-32b-instruct", 131072, true, "US-West"),
            ProviderDeploymentEntity("deploy:together-qwen-32b", "provider:together", "canonical:qwen-qwen2.5-coder-32b-instruct", "Qwen/Qwen2.5-Coder-32B-Instruct", 131072, true, "Global"),
            ProviderDeploymentEntity("deploy:groq-llama-70b", "provider:groq", "canonical:meta-llama-3.3-70b-instruct", "llama-3.3-70b-versatile", 128000, true, "US-East"),
            ProviderDeploymentEntity("deploy:fireworks-llama-70b", "provider:fireworks", "canonical:meta-llama-3.3-70b-instruct", "accounts/fireworks/models/llama-v3p3-70b-instruct", 128000, true, "US-Central"),
            ProviderDeploymentEntity("deploy:deepinfra-llama-70b", "provider:deepinfra", "canonical:meta-llama-3.3-70b-instruct", "meta-llama/Llama-3.3-70B-Instruct", 128000, true, "EU-Central")
        )
        providerDao.insertDeployments(deployments)

        val pricing = listOf(
            PricingRecordEntity("price:groq-qwen-32b", "deploy:groq-qwen-32b", 0.59, 0.79, 0.29, 0.40, "USD", "2025-02-01", "https://groq.com/pricing"),
            PricingRecordEntity("price:together-qwen-32b", "deploy:together-qwen-32b", 0.80, 0.80, null, 0.50, "USD", "2025-02-01", "https://together.ai/pricing"),
            PricingRecordEntity("price:groq-llama-70b", "deploy:groq-llama-70b", 0.59, 0.79, 0.30, 0.45, "USD", "2025-02-01", "https://groq.com/pricing"),
            PricingRecordEntity("price:fireworks-llama-70b", "deploy:fireworks-llama-70b", 0.90, 0.90, 0.45, 0.60, "USD", "2025-02-01", "https://fireworks.ai/pricing"),
            PricingRecordEntity("price:deepinfra-llama-70b", "deploy:deepinfra-llama-70b", 0.55, 0.70, null, null, "USD", "2025-02-01", "https://deepinfra.com/pricing")
        )
        providerDao.insertPricing(pricing)
    }

    fun getDeploymentsForModel(canonicalId: String): Flow<List<ProviderDeployment>> {
        return providerDao.getDeploymentsForModel(canonicalId).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun getPricingForDeployment(deploymentId: String): PricingRecord? = withContext(Dispatchers.IO) {
        providerDao.getLatestPricing(deploymentId)?.toDomain()
    }

    fun getMeasurementsForDeployment(deploymentId: String): ProviderMeasurement {
        return when (deploymentId) {
            "deploy:groq-qwen-32b" -> ProviderMeasurement("m:1", deploymentId, prefillTokensPerSec = 1250f, decodeTokensPerSec = 380f, ttftMs = 120f)
            "deploy:groq-llama-70b" -> ProviderMeasurement("m:2", deploymentId, prefillTokensPerSec = 1100f, decodeTokensPerSec = 290f, ttftMs = 140f)
            "deploy:fireworks-llama-70b" -> ProviderMeasurement("m:3", deploymentId, prefillTokensPerSec = 850f, decodeTokensPerSec = 160f, ttftMs = 180f)
            else -> ProviderMeasurement("m:generic", deploymentId, prefillTokensPerSec = 600f, decodeTokensPerSec = 95f, ttftMs = 250f)
        }
    }

    private fun ProviderEntity.toDomain() = Provider(id, name, logoUrl, websiteUrl, isSelfHosted)
    private fun ProviderDeploymentEntity.toDomain() = ProviderDeployment(id, providerId, canonicalId, providerModelId, supportedContextTokens, isAvailable, region)
    private fun PricingRecordEntity.toDomain() = PricingRecord(id, deploymentId, inputPricePerMToken, outputPricePerMToken, cachedInputPricePerMToken, batchPricePerMToken, currency, effectiveDate, sourceUrl)
}
