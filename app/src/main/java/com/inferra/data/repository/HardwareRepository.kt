package com.inferra.data.repository

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import com.inferra.data.local.HardwareProfileDao
import com.inferra.data.local.HardwareProfileEntity
import com.inferra.data.local.SeedData
import com.inferra.domain.model.DeviceType
import com.inferra.domain.model.HardwareProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class HardwareRepository(
    private val context: Context,
    private val profileDao: HardwareProfileDao
) {
    val profilesFlow: Flow<List<HardwareProfile>> = profileDao.getAllProfiles().map { entities ->
        if (entities.isEmpty()) {
            SeedData.initialHardwareProfiles
        } else {
            entities.map { it.toDomain() }
        }
    }

    suspend fun getActiveProfile(): HardwareProfile = withContext(Dispatchers.IO) {
        val all = profilesFlow.first()
        all.find { !it.isLocalDevice } ?: all.firstOrNull() ?: detectLocalAndroidHardware()
    }

    suspend fun addCustomProfile(profile: HardwareProfile) = withContext(Dispatchers.IO) {
        profileDao.insertProfile(profile.toEntity())
    }

    suspend fun deleteProfile(id: String) = withContext(Dispatchers.IO) {
        profileDao.deleteProfile(id)
    }

    fun detectLocalAndroidHardware(): HardwareProfile {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)

        val totalRamGb = (memInfo.totalMem / (1024f * 1024f * 1024f))
        val deviceModel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"

        return HardwareProfile(
            id = "profile-local-detected",
            name = "$deviceModel (Active Device)",
            deviceType = DeviceType.LOCAL_ANDROID,
            cpuName = Build.HARDWARE,
            gpuName = "Adreno / Mali GPU",
            vramGb = (totalRamGb * 0.35f).coerceAtMost(6.0f),
            ramGb = totalRamGb,
            osName = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            preferredRuntime = "llama.cpp (Vulkan)",
            availableStorageGb = 64f,
            isLocalDevice = true
        )
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
