package com.inferra.data.hardware

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.inferra.domain.model.DeviceType
import com.inferra.domain.model.HardwareProfile
import java.io.File
import java.util.Locale

class HardwareDetector(private val context: Context) {

    fun detectHardware(): HardwareProfile {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memInfo)

        val totalRamBytes = memInfo.totalMem
        val totalRamGb = if (totalRamBytes > 0) totalRamBytes / (1024f * 1024f * 1024f) else 8.0f

        val cores = Runtime.getRuntime().availableProcessors()
        val cpuName = buildCpuName(cores)

        val hasVulkan = context.packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL)
        val gpuName = if (hasVulkan) "System GPU (Vulkan 1.x Accelerated)" else "System GPU (GLES Acceleration)"

        val mfrRaw = Build.MANUFACTURER ?: "Android"
        val mfr = if (mfrRaw.isNotEmpty()) mfrRaw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() } else "Android"
        val model = Build.MODEL ?: "Device"
        val deviceName = "$mfr $model"

        val osName = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}, ${Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"})"

        val storageGb = getAvailableStorageGb()

        val preferredRuntime = if (hasVulkan) "llama.cpp (Vulkan Acceleration)" else "llama.cpp (CPU OpenMP)"

        return HardwareProfile(
            id = "profile-local-detected",
            name = deviceName,
            deviceType = DeviceType.LOCAL_ANDROID,
            cpuName = cpuName,
            gpuName = gpuName,
            vramGb = 0f, // Shared system RAM on Android mobile
            ramGb = (totalRamGb * 10f).toInt() / 10f,
            osName = osName,
            preferredRuntime = preferredRuntime,
            availableStorageGb = storageGb,
            isLocalDevice = true
        )
    }

    private fun buildCpuName(cores: Int): String {
        val hw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MODEL.ifBlank { Build.HARDWARE }
        } else {
            Build.HARDWARE
        }
        return if (hw.isNotBlank() && !hw.equals("unknown", ignoreCase = true)) {
            "$hw ($cores Cores)"
        } else {
            "ARM64 CPU ($cores Cores)"
        }
    }

    private fun getAvailableStorageGb(): Float {
        return try {
            val path: File = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
            (availableBytes / (1024f * 1024f * 1024f) * 10f).toInt() / 10f
        } catch (_: Exception) {
            64.0f
        }
    }
}
