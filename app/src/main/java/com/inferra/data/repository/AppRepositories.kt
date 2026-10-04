package com.inferra.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.inferra.data.hardware.HardwareDetector
import com.inferra.data.local.DeviceTargetDao
import com.inferra.data.local.DeviceTargetEntity
import com.inferra.data.local.DownloadJobDao
import com.inferra.data.local.DownloadJobEntity
import com.inferra.data.local.HardwareProfileDao
import com.inferra.data.local.HardwareProfileEntity
import com.inferra.data.local.PricingRecordEntity
import com.inferra.data.local.ProviderDao
import com.inferra.data.local.ProviderDeploymentEntity
import com.inferra.data.local.ProviderEntity
import com.inferra.domain.model.AiModel
import com.inferra.domain.model.DeviceTarget
import com.inferra.domain.model.DeviceType
import com.inferra.domain.model.DownloadJob
import com.inferra.domain.model.DownloadManifest
import com.inferra.domain.model.DownloadStatus
import com.inferra.domain.model.HardwareProfile
import com.inferra.domain.model.PricingRecord
import com.inferra.domain.model.Provider
import com.inferra.domain.model.ProviderDeployment
import com.inferra.domain.model.ProviderMeasurement
import com.inferra.domain.model.QuantizationInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

// ==============================================================================
// Companion / Network Device Repository
// ==============================================================================

class CompanionRepository(
    private val deviceTargetDao: DeviceTargetDao
) {
    val devicesFlow: Flow<List<DeviceTarget>> = deviceTargetDao.getAllDevices().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun pairNewDevice(
        name: String,
        ipAddress: String,
        port: Int = 8443
    ): DeviceTarget = withContext(Dispatchers.IO) {
        val device = DeviceTarget(
            id = "target-${System.currentTimeMillis()}",
            name = name,
            ipAddress = ipAddress,
            port = port,
            osName = "Companion Device",
            isOnline = true,
            lastSeenEpochMs = System.currentTimeMillis(),
            isPaired = true,
            supportedRuntimes = listOf("llama.cpp", "Ollama", "vLLM", "LM Studio")
        )
        deviceTargetDao.insertDevice(device.toEntity())
        device
    }

    private fun DeviceTargetEntity.toDomain() = DeviceTarget(
        id = id,
        name = name,
        ipAddress = ipAddress,
        port = port,
        osName = osName,
        isOnline = isOnline,
        lastSeenEpochMs = lastSeenEpochMs,
        isPaired = isPaired,
        supportedRuntimes = runtimesCsv.split(",")
    )

    private fun DeviceTarget.toEntity() = DeviceTargetEntity(
        id = id,
        name = name,
        ipAddress = ipAddress,
        port = port,
        osName = osName,
        isOnline = isOnline,
        lastSeenEpochMs = lastSeenEpochMs,
        isPaired = isPaired,
        runtimesCsv = supportedRuntimes.joinToString(",")
    )
}

// ==============================================================================
// Download Jobs Repository
// ==============================================================================

class DownloadRepository(
    private val downloadJobDao: DownloadJobDao,
) {
    private val json = Json { ignoreUnknownKeys = true }

    val jobsFlow: Flow<List<DownloadJob>> = downloadJobDao.getAllJobs().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun createSendToPcJob(
        model: AiModel,
        quantization: QuantizationInfo,
        targetDevice: DeviceTarget,
    ): DownloadJob = withContext(Dispatchers.IO) {
        val jobId = "job-${System.currentTimeMillis()}"
        val manifest = DownloadManifest(
            modelId = model.id,
            repository = model.id,
            exactRevision = "main",
            fileName = quantization.fileName,
            expectedSizeBytes = quantization.fileSizeBytes,
            sourceUrl = quantization.downloadUrl,
            targetDeviceId = targetDevice.id,
            requestedFormat = quantization.format,
            createdAtEpochMs = System.currentTimeMillis()
        )

        val job = DownloadJob(
            id = jobId,
            modelId = model.id,
            modelName = model.name,
            author = model.author,
            quantType = quantization.quantType,
            totalBytes = quantization.fileSizeBytes,
            downloadedBytes = 0,
            status = if (targetDevice.isOnline) DownloadStatus.QUEUED else DownloadStatus.WAITING_FOR_DEVICE,
            targetDeviceId = targetDevice.id,
            targetDeviceName = targetDevice.name,
            speedBytesPerSec = 0,
            etaSeconds = 0,
            manifest = manifest
        )

        downloadJobDao.insertJob(job.toEntity())
        job
    }

    suspend fun createLocalDeviceDownloadJob(
        model: AiModel,
        quantization: QuantizationInfo
    ): DownloadJob = withContext(Dispatchers.IO) {
        val jobId = "local-job-${System.currentTimeMillis()}"
        val manifest = DownloadManifest(
            modelId = model.id,
            repository = quantization.sourceRepo.ifBlank { model.id },
            exactRevision = "main",
            fileName = quantization.fileName,
            expectedSizeBytes = quantization.fileSizeBytes,
            sourceUrl = quantization.downloadUrl,
            targetDeviceId = "local-device",
            requestedFormat = quantization.format,
            createdAtEpochMs = System.currentTimeMillis()
        )

        val job = DownloadJob(
            id = jobId,
            modelId = model.id,
            modelName = model.name,
            author = model.author,
            quantType = quantization.quantType,
            totalBytes = quantization.fileSizeBytes,
            downloadedBytes = 0,
            status = DownloadStatus.QUEUED,
            targetDeviceId = "local-device",
            targetDeviceName = "This Android Device",
            speedBytesPerSec = 0,
            etaSeconds = 0,
            manifest = manifest
        )

        downloadJobDao.insertJob(job.toEntity())
        job
    }

    suspend fun updateJobStatus(job: DownloadJob) = withContext(Dispatchers.IO) {
        downloadJobDao.insertJob(job.toEntity())
    }

    suspend fun cancelJob(jobId: String) = withContext(Dispatchers.IO) {
        downloadJobDao.deleteJob(jobId)
    }

    private fun DownloadJobEntity.toDomain(): DownloadJob {
        val parsedManifest = try {
            json.decodeFromString<DownloadManifest>(manifestJson)
        } catch (_: Exception) {
            DownloadManifest(
                modelId = modelId,
                repository = modelId,
                exactRevision = "main",
                fileName = "model.gguf",
                expectedSizeBytes = totalBytes,
                sourceUrl = "https://huggingface.co",
                targetDeviceId = targetDeviceId,
                requestedFormat = "GGUF",
                createdAtEpochMs = System.currentTimeMillis()
            )
        }

        return DownloadJob(
            id = id,
            modelId = modelId,
            modelName = modelName,
            author = author,
            quantType = quantType,
            totalBytes = totalBytes,
            downloadedBytes = downloadedBytes,
            status = DownloadStatus.valueOf(statusStr),
            targetDeviceId = targetDeviceId,
            targetDeviceName = targetDeviceName,
            speedBytesPerSec = speedBytesPerSec,
            etaSeconds = etaSeconds,
            errorMessage = errorMessage,
            manifest = parsedManifest
        )
    }

    private fun DownloadJob.toEntity(): DownloadJobEntity {
        return DownloadJobEntity(
            id = id,
            modelId = modelId,
            modelName = modelName,
            author = author,
            quantType = quantType,
            totalBytes = totalBytes,
            downloadedBytes = downloadedBytes,
            statusStr = status.name,
            targetDeviceId = targetDeviceId,
            targetDeviceName = targetDeviceName,
            speedBytesPerSec = speedBytesPerSec,
            etaSeconds = etaSeconds,
            errorMessage = errorMessage,
            manifestJson = json.encodeToString(manifest)
        )
    }
}

// ==============================================================================
// Hardware Profiles Repository
// ==============================================================================

class HardwareRepository(
    private val context: Context,
    private val profileDao: HardwareProfileDao,
) {
    val profilesFlow: Flow<List<HardwareProfile>> = profileDao.getAllProfiles().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun getActiveProfile(): HardwareProfile? = withContext(Dispatchers.IO) {
        val all = profilesFlow.first()
        all.firstOrNull()
    }

    suspend fun addCustomProfile(profile: HardwareProfile) = withContext(Dispatchers.IO) {
        profileDao.insertProfile(profile.toEntity())
    }

    suspend fun deleteProfile(id: String) = withContext(Dispatchers.IO) {
        profileDao.deleteProfile(id)
    }

    fun detectLocalAndroidHardware(): HardwareProfile {
        return try {
            HardwareDetector(context).detectHardware()
        } catch (_: Exception) {
            HardwareProfile(
                id = "profile-local-fallback",
                name = "Android Device",
                deviceType = DeviceType.LOCAL_ANDROID,
                cpuName = "ARM64 CPU",
                gpuName = "Mobile GPU",
                vramGb = 0f,
                ramGb = 8.0f,
                osName = "Android 15",
                preferredRuntime = "llama.cpp (Vulkan)",
                availableStorageGb = 64f,
                isLocalDevice = true,
            )
        }
    }

    private fun HardwareProfileEntity.toDomain() = HardwareProfile(
        id = id,
        name = name,
        deviceType = DeviceType.valueOf(deviceTypeStr),
        cpuName = cpuName,
        gpuName = gpuName,
        vramGb = vramGb,
        ramGb = ramGb,
        osName = osName,
        preferredRuntime = preferredRuntime,
        availableStorageGb = availableStorageGb,
        isLocalDevice = isLocalDevice
    )

    private fun HardwareProfile.toEntity() = HardwareProfileEntity(
        id = id,
        name = name,
        deviceTypeStr = deviceType.name,
        cpuName = cpuName,
        gpuName = gpuName,
        vramGb = vramGb,
        ramGb = ramGb,
        osName = osName,
        preferredRuntime = preferredRuntime,
        availableStorageGb = availableStorageGb,
        isLocalDevice = isLocalDevice
    )
}

// ==============================================================================
// Provider & Pricing Repository
// ==============================================================================

class ProviderRepository(
    private val providerDao: ProviderDao,
) {
    val allProvidersFlow: Flow<List<Provider>> = providerDao.getAllProviders().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun seedDefaultProviders() = withContext(Dispatchers.IO) {
        val providers = listOf(
            ProviderEntity("provider:groq", "Groq", "https://groq.com/favicon.ico", "https://groq.com", isSelfHosted = false),
            ProviderEntity("provider:together", "Together AI", "https://together.ai/favicon.ico", "https://together.ai", isSelfHosted = false),
            ProviderEntity("provider:fireworks", "Fireworks AI", "https://fireworks.ai/favicon.ico", "https://fireworks.ai", isSelfHosted = false),
            ProviderEntity("provider:deepinfra", "DeepInfra", "https://deepinfra.com/favicon.ico", "https://deepinfra.com", isSelfHosted = false),
            ProviderEntity("provider:openrouter", "OpenRouter", "https://openrouter.ai/favicon.ico", "https://openrouter.ai", isSelfHosted = false),
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

// ==============================================================================
// User Settings Repository
// ==============================================================================

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

data class UserSettings(
    val glassIntensity: Float = 0.8f,
    val isDarkMode: Boolean = true,
    val isTelemetryEnabled: Boolean = false,
    val autoRefreshData: Boolean = true,
    val modelStoragePath: String = "",
    val hfToken: String = "",
)

class SettingsRepository(private val context: Context) {

    private companion object {
        val KEY_GLASS_INTENSITY = floatPreferencesKey("glass_intensity")
        val KEY_IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val KEY_IS_TELEMETRY_ENABLED = booleanPreferencesKey("is_telemetry_enabled")
        val KEY_AUTO_REFRESH_DATA = booleanPreferencesKey("auto_refresh_data")
        val KEY_MODEL_STORAGE_PATH = stringPreferencesKey("model_storage_path")
        val KEY_HF_TOKEN = stringPreferencesKey("hf_token")
    }

    val userSettingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            glassIntensity = prefs[KEY_GLASS_INTENSITY] ?: 0.8f,
            isDarkMode = prefs[KEY_IS_DARK_MODE] ?: true,
            isTelemetryEnabled = prefs[KEY_IS_TELEMETRY_ENABLED] ?: false,
            autoRefreshData = prefs[KEY_AUTO_REFRESH_DATA] ?: true,
            modelStoragePath = prefs[KEY_MODEL_STORAGE_PATH] ?: "",
            hfToken = prefs[KEY_HF_TOKEN] ?: ""
        )
    }

    suspend fun updateGlassIntensity(intensity: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_GLASS_INTENSITY] = intensity
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_DARK_MODE] = enabled
        }
    }

    suspend fun setTelemetryEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_TELEMETRY_ENABLED] = enabled
        }
    }

    suspend fun setModelStoragePath(path: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MODEL_STORAGE_PATH] = path
        }
    }

    suspend fun setHfToken(token: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_HF_TOKEN] = token
        }
    }
}
