package com.inferra.data.local

import com.inferra.domain.model.AiModel
import com.inferra.domain.model.CapabilityMatrix
import com.inferra.domain.model.DeviceTarget
import com.inferra.domain.model.DeviceType
import com.inferra.domain.model.DownloadJob
import com.inferra.domain.model.DownloadManifest
import com.inferra.domain.model.DownloadStatus
import com.inferra.domain.model.HardwareProfile
import com.inferra.domain.model.LicenseType
import com.inferra.domain.model.LineageInfo
import com.inferra.domain.model.Modality
import com.inferra.domain.model.ModelTask
import com.inferra.domain.model.QuantizationInfo

object SeedData {

    val initialHardwareProfiles = listOf(
        HardwareProfile(
            id = "profile-local-android",
            name = "Android Phone (Local)",
            deviceType = DeviceType.LOCAL_ANDROID,
            cpuName = "Snapdragon 8 Gen 3 / Tensor G4",
            gpuName = "Adreno 750 / Immortalis",
            vramGb = 4.0f,
            ramGb = 12.0f,
            osName = "Android 15",
            preferredRuntime = "llama.cpp (Vulkan)",
            availableStorageGb = 128f,
            isLocalDevice = true
        ),
        HardwareProfile(
            id = "profile-pc-primary",
            name = "Desktop Workstation",
            deviceType = DeviceType.DESKTOP_PC,
            cpuName = "Intel Core i9 / Ryzen 9",
            gpuName = "NVIDIA GeForce RTX 4090 24GB",
            vramGb = 24.0f,
            ramGb = 64.0f,
            osName = "Windows 11 Pro",
            preferredRuntime = "llama.cpp (CUDA)",
            availableStorageGb = 2000f,
            isLocalDevice = false
        ),
        HardwareProfile(
            id = "profile-macbook-m3",
            name = "MacBook Pro",
            deviceType = DeviceType.LAPTOP,
            cpuName = "Apple M3 Max",
            gpuName = "Apple M3 Max GPU",
            vramGb = 96.0f,
            ramGb = 96.0f,
            osName = "macOS Sequoia",
            preferredRuntime = "Ollama (Metal)",
            availableStorageGb = 1000f,
            isLocalDevice = false
        )
    )

    val initialDeviceTargets = listOf(
        DeviceTarget(
            id = "dev-target-1",
            name = "Desktop Workstation",
            ipAddress = "192.168.1.105",
            port = 8443,
            osName = "Windows 11 Pro",
            isOnline = true,
            lastSeenEpochMs = System.currentTimeMillis(),
            isPaired = true,
            supportedRuntimes = listOf("llama.cpp", "Ollama", "vLLM", "LM Studio")
        ),
        DeviceTarget(
            id = "dev-target-2",
            name = "MacBook Pro",
            ipAddress = "192.168.1.112",
            port = 8443,
            osName = "macOS Sequoia",
            isOnline = false,
            lastSeenEpochMs = System.currentTimeMillis() - 3600000 * 5,
            isPaired = true,
            supportedRuntimes = listOf("Ollama", "llama.cpp")
        )
    )

    val seedModels = listOf(
        AiModel(
            id = "Qwen/Qwen2.5-Coder-32B-Instruct",
            name = "Qwen2.5-Coder-32B-Instruct",
            author = "Qwen",
            description = "32B parameter open coding model optimized for code synthesis and agentic reasoning.",
            architecture = "Qwen2ForCausalLM",
            totalParamsBillion = 32.5f,
            activeParamsBillion = 32.5f,
            isMoe = false,
            contextLengthTokens = 131072,
            modalities = listOf(Modality.TEXT, Modality.CODE),
            tasks = listOf(ModelTask.CODING, ModelTask.REASONING, ModelTask.TOOL_CALLING),
            license = LicenseType.APACHE_2,
            licenseName = "Apache 2.0",
            downloadsCount = 1845000L,
            likesCount = 4210L,
            updatedAt = "2025-01-15",
            quantizations = listOf(
                QuantizationInfo("q4km-qwen32", "GGUF", "Q4_K_M", 19800000000L, "https://huggingface.co/Qwen/Qwen2.5-Coder-32B-Instruct-GGUF", "Qwen2.5-Coder-32B-Instruct-Q4_K_M.gguf", 19800, 18500, 94.5f),
                QuantizationInfo("q80-qwen32", "GGUF", "Q8_0", 34500000000L, "https://huggingface.co/Qwen/Qwen2.5-Coder-32B-Instruct-GGUF", "Qwen2.5-Coder-32B-Instruct-Q8_0.gguf", 34800, 33500, 99.1f),
                QuantizationInfo("q5km-qwen32", "GGUF", "Q5_K_M", 23200000000L, "https://huggingface.co/Qwen/Qwen2.5-Coder-32B-Instruct-GGUF", "Qwen2.5-Coder-32B-Instruct-Q5_K_M.gguf", 23500, 22100, 97.0f)
            ),
            benchmarks = emptyList(), // Rule #3: Empty list when benchmark data is unavailable
            capabilities = CapabilityMatrix(
                coding = 95.0f,
                reasoning = 88.0f,
                math = 85.0f,
                vision = 0.0f,
                agentic = 85.0f,
                toolCalling = 90.0f,
                multilingual = 80.0f,
                longContext = 92.0f
            ),
            lineage = LineageInfo(
                baseModelId = "Qwen/Qwen2.5-32B",
                parentModelId = "Qwen/Qwen2.5-32B-Instruct",
                fineTunesCount = 142
            ),
            isFeatured = true,
            isTrending = true,
            isNew = false,
            repoUrl = "https://huggingface.co/Qwen/Qwen2.5-Coder-32B-Instruct",
            avatarUrl = null
        ),
        AiModel(
            id = "deepseek-ai/DeepSeek-R1",
            name = "DeepSeek-R1",
            author = "DeepSeek",
            description = "671B MoE reasoning model utilizing reinforcement learning for complex chain-of-thought logic.",
            architecture = "DeepSeekV3ForCausalLM (MoE)",
            totalParamsBillion = 671.0f,
            activeParamsBillion = 37.0f,
            isMoe = true,
            contextLengthTokens = 131072,
            modalities = listOf(Modality.TEXT, Modality.CODE),
            tasks = listOf(ModelTask.REASONING, ModelTask.MATH, ModelTask.CODING),
            license = LicenseType.MIT,
            licenseName = "MIT License",
            downloadsCount = 4200000L,
            likesCount = 18900L,
            updatedAt = "2025-01-20",
            quantizations = listOf(
                QuantizationInfo("q4km-r1", "GGUF", "Q4_K_M", 380000000000L, "https://huggingface.co/unsloth/DeepSeek-R1-GGUF", "DeepSeek-R1-Q4_K_M.gguf", 385000, 380000, 95.0f),
                QuantizationInfo("q15-r1", "GGUF", "IQ1_S", 155000000000L, "https://huggingface.co/unsloth/DeepSeek-R1-GGUF", "DeepSeek-R1-IQ1_S.gguf", 160000, 155000, 84.0f)
            ),
            benchmarks = emptyList(),
            capabilities = CapabilityMatrix(
                coding = 92.0f,
                reasoning = 98.0f,
                math = 96.0f,
                vision = 0.0f,
                agentic = 90.0f,
                toolCalling = 85.0f,
                multilingual = 88.0f,
                longContext = 90.0f
            ),
            lineage = LineageInfo(
                baseModelId = "deepseek-ai/DeepSeek-V3",
                distilledFrom = "DeepSeek-R1-Zero"
            ),
            isFeatured = true,
            isTrending = true,
            isNew = true,
            repoUrl = "https://huggingface.co/deepseek-ai/DeepSeek-R1",
            avatarUrl = null
        ),
        AiModel(
            id = "meta-llama/Llama-3.3-70B-Instruct",
            name = "Llama-3.3-70B-Instruct",
            author = "Meta",
            description = "70B parameter dense model providing strong reasoning, instruction following, and multilingual support.",
            architecture = "LlamaForCausalLM",
            totalParamsBillion = 70.0f,
            activeParamsBillion = 70.0f,
            isMoe = false,
            contextLengthTokens = 131072,
            modalities = listOf(Modality.TEXT, Modality.CODE),
            tasks = listOf(ModelTask.GENERAL_TEXT, ModelTask.REASONING, ModelTask.CODING, ModelTask.TOOL_CALLING),
            license = LicenseType.LLAMA_COMMUNITY,
            licenseName = "Llama 3.3 Community License",
            downloadsCount = 2890000L,
            likesCount = 8900L,
            updatedAt = "2024-12-06",
            quantizations = listOf(
                QuantizationInfo("q4km-llama70", "GGUF", "Q4_K_M", 42500000000L, "https://huggingface.co/bartowski/Llama-3.3-70B-Instruct-GGUF", "Llama-3.3-70B-Instruct-Q4_K_M.gguf", 43200, 42000, 95.8f),
                QuantizationInfo("q80-llama70", "GGUF", "Q8_0", 74000000000L, "https://huggingface.co/bartowski/Llama-3.3-70B-Instruct-GGUF", "Llama-3.3-70B-Instruct-Q8_0.gguf", 75000, 74000, 99.5f)
            ),
            benchmarks = emptyList(),
            capabilities = CapabilityMatrix(
                coding = 86.0f,
                reasoning = 90.0f,
                math = 84.0f,
                vision = 0.0f,
                agentic = 88.0f,
                toolCalling = 91.0f,
                multilingual = 86.0f,
                longContext = 92.0f
            ),
            lineage = LineageInfo(
                baseModelId = "meta-llama/Llama-3.3-70B",
                fineTunesCount = 380
            ),
            isFeatured = true,
            isTrending = false,
            isNew = false,
            repoUrl = "https://huggingface.co/meta-llama/Llama-3.3-70B-Instruct",
            avatarUrl = null
        )
    )

    val initialDownloadJobs = listOf(
        DownloadJob(
            id = "job-101",
            modelId = "Qwen/Qwen2.5-Coder-32B-Instruct",
            modelName = "Qwen2.5-Coder-32B-Instruct",
            author = "Qwen",
            quantType = "Q4_K_M",
            totalBytes = 19800000000L,
            downloadedBytes = 14200000000L,
            status = DownloadStatus.DOWNLOADING,
            targetDeviceId = "dev-target-1",
            targetDeviceName = "Desktop Workstation",
            speedBytesPerSec = 78500000L,
            etaSeconds = 71L,
            manifest = DownloadManifest(
                modelId = "Qwen/Qwen2.5-Coder-32B-Instruct",
                repository = "Qwen/Qwen2.5-Coder-32B-Instruct-GGUF",
                exactRevision = "main",
                fileName = "Qwen2.5-Coder-32B-Instruct-Q4_K_M.gguf",
                expectedSizeBytes = 19800000000L,
                checksumSha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                sourceUrl = "https://huggingface.co/Qwen/Qwen2.5-Coder-32B-Instruct-GGUF/resolve/main/Qwen2.5-Coder-32B-Instruct-Q4_K_M.gguf",
                targetDeviceId = "dev-target-1",
                requestedFormat = "GGUF",
                createdAtEpochMs = System.currentTimeMillis() - 300000
            )
        )
    )
}
