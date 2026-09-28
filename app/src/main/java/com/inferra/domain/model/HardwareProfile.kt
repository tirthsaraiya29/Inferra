package com.inferra.domain.model

enum class DeviceType {
    LOCAL_ANDROID,
    DESKTOP_PC,
    LAPTOP,
    WORKSTATION_SERVER
}

enum class FitGrade {
    EXCELLENT,      // Comfortably fits in VRAM/RAM with headroom
    BORDERLINE,     // Fits in combined RAM/VRAM but offloading needed or tight
    INSUFFICIENT,   // Exceeds available RAM/VRAM
    UNKNOWN
}

data class HardwareProfile(
    val id: String,
    val name: String,                  // e.g. "Primary Workstation (Tirth-PC)" or "Pixel 8 Pro"
    val deviceType: DeviceType,
    val cpuName: String,               // e.g. "Intel Core i9-14900K"
    val gpuName: String,               // e.g. "NVIDIA RTX 4090"
    val vramGb: Float,                 // e.g. 24.0f
    val ramGb: Float,                  // e.g. 64.0f
    val osName: String,                // e.g. "Windows 11", "Android 15", "macOS Sequoia"
    val preferredRuntime: String,      // e.g. "llama.cpp (Vulkan)", "Ollama", "vLLM", "LM Studio"
    val availableStorageGb: Float = 500f,
    val isLocalDevice: Boolean = false
)

data class HardwareCompatibilityResult(
    val fitGrade: FitGrade,
    val requiredVramGb: Float,
    val requiredRamGb: Float,
    val estimatedTokensPerSec: Float,
    val estimatedTtftMs: Float,
    val offloadPercentage: Int,        // 0 to 100% layer offload to GPU
    val explanation: String,
    val profileName: String
)
