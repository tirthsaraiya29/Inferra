package com.inferra.data.repository

import android.content.Context
import android.os.Build
import com.inferra.data.hardware.HardwareDetector
import com.inferra.data.local.HardwareProfileDao
import com.inferra.data.local.HardwareProfileEntity
import com.inferra.domain.model.DeviceType
import com.inferra.domain.model.HardwareProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Locale

class HardwareRepository(
    private val context: Context,
    private val profileDao: HardwareProfileDao
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
                isLocalDevice = true
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
