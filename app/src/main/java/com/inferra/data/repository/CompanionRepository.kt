package com.inferra.data.repository

import com.inferra.data.local.DeviceTargetDao
import com.inferra.data.local.DeviceTargetEntity
import com.inferra.domain.model.DeviceTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

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
