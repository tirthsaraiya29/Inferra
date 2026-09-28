package com.inferra.data.local

import com.inferra.domain.model.AiModel
import com.inferra.domain.model.BenchmarkScore
import com.inferra.domain.model.CapabilityMatrix
import com.inferra.domain.model.DataCategory
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
            name = "Tirth Workstation (Desktop PC)",
            deviceType = DeviceType.DESKTOP_PC,
            cpuName = "Intel Core i9-14900K (24C/32T)",
            gpuName = "NVIDIA GeForce RTX 4090 24GB",
            vramGb = 24.0f,
            ramGb = 64.0f,
            osName = "Windows 11 Pro 64-bit",
            preferredRuntime = "llama.cpp (CUDA)",
            availableStorageGb = 2000f,
            isLocalDevice = false
        ),
        HardwareProfile(
            id = "profile-macbook-m3",
            name = "MacBook Pro M3 Max",
            deviceType = DeviceType.LAPTOP,
            cpuName = "Apple M3 Max (16-core CPU)",
            gpuName = "Apple M3 Max (40-core GPU)",
            vramGb = 96.0f, // Unified Memory
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
            name = "Tirth-PC Workstation",
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
            name = "MacBook-Pro-M3",
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
            description = "State-of-the-art 32B open coding model matching GPT-4o on HumanEval and MBPP code synthesis benchmarks.",
            architecture = "Qwen2ForCausalLM",
            totalParamsBillion = 32.5f,
            activeParamsBillion = 32.5f,
            isMoe = false,
            contextLengthTokens = 131072, // 128K
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
            benchmarks = listOf(
                BenchmarkScore("HumanEval", 92.7f, 100f, "Coding", "Independent Measurement", DataCategory.MEASUREMENT),
                BenchmarkScore("MBPP Sanitized", 88.4f, 100f, "Coding", "Author Published", DataCategory.SOURCE_FACT),
                BenchmarkScore("LiveCodeBench", 54.2f, 100f, "Coding", "Community Benchmark", DataCategory.MEASUREMENT),
                BenchmarkScore("MMLU-Pro", 71.8f, 100f, "General Knowledge", "Independent Measurement", DataCategory.MEASUREMENT)
            ),
            capabilities = CapabilityMatrix(
                coding = 96.5f,
                reasoning = 89.0f,
                math = 88.2f,
                vision = 0.0f,
                agentic = 86.0f,
                toolCalling = 91.5f,
                multilingual = 82.0f,
                longContext = 94.0f
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
            avatarUrl = "https://avatars.githubusercontent.com/u/8268805?s=200&v=4"
        ),
        AiModel(
            id = "deepseek-ai/DeepSeek-R1",
            name = "DeepSeek-R1",
            author = "DeepSeek",
            description = "First open reasoning model utilizing大规模强化学习 Reinforcement Learning for advanced chain-of-thought mathematical proof and logic synthesis.",
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
            benchmarks = listOf(
                BenchmarkScore("AIME 2024", 79.8f, 100f, "Math Proofs", "Author Published", DataCategory.SOURCE_FACT),
                BenchmarkScore("MATH-500", 97.3f, 100f, "Math Reasoning", "Independent Measurement", DataCategory.MEASUREMENT),
                BenchmarkScore("Codeforces", 96.3f, 100f, "Competitive Coding", "Community Benchmark", DataCategory.MEASUREMENT),
                BenchmarkScore("GPQA Diamond", 71.5f, 100f, "Graduate Science", "Independent Measurement", DataCategory.MEASUREMENT)
            ),
            capabilities = CapabilityMatrix(
                coding = 95.0f,
                reasoning = 99.2f,
                math = 98.8f,
                vision = 0.0f,
                agentic = 92.0f,
                toolCalling = 88.0f,
                multilingual = 90.0f,
                longContext = 92.0f
            ),
            lineage = LineageInfo(
                baseModelId = "deepseek-ai/DeepSeek-V3",
                distilledFrom = "DeepSeek-R1-Zero"
            ),
            isFeatured = true,
            isTrending = true,
            isNew = true,
            repoUrl = "https://huggingface.co/deepseek-ai/DeepSeek-R1",
            avatarUrl = "https://avatars.githubusercontent.com/u/148383773?s=200&v=4"
        ),
        AiModel(
            id = "meta-llama/Llama-3.3-70B-Instruct",
            name = "Llama-3.3-70B-Instruct",
            author = "Meta",
            description = "Industry standard 70B parameter dense model with performance matching Llama-3.1 405B at a fraction of inference cost.",
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
            benchmarks = listOf(
                BenchmarkScore("MMLU-Pro", 72.6f, 100f, "General Knowledge", "Author Published", DataCategory.SOURCE_FACT),
                BenchmarkScore("IFEval", 88.6f, 100f, "Instruction Following", "Independent Measurement", DataCategory.MEASUREMENT),
                BenchmarkScore("MATH", 70.3f, 100f, "Math Reasoning", "Independent Measurement", DataCategory.MEASUREMENT)
            ),
            capabilities = CapabilityMatrix(
                coding = 88.0f,
                reasoning = 91.5f,
                math = 86.0f,
                vision = 0.0f,
                agentic = 89.0f,
                toolCalling = 93.0f,
                multilingual = 87.0f,
                longContext = 93.5f
            ),
            lineage = LineageInfo(
                baseModelId = "meta-llama/Llama-3.3-70B",
                fineTunesCount = 380
            ),
            isFeatured = true,
            isTrending = false,
            isNew = false,
            repoUrl = "https://huggingface.co/meta-llama/Llama-3.3-70B-Instruct",
            avatarUrl = "https://avatars.githubusercontent.com/u/69631?s=200&v=4"
        ),
        AiModel(
            id = "Qwen/Qwen2.5-VL-72B-Instruct",
            name = "Qwen2.5-VL-72B-Instruct",
            author = "Qwen",
            description = "State-of-the-art vision-language flagship with hour-long video understanding, visual grounding, and document parsing.",
            architecture = "Qwen2VLForConditionalGeneration",
            totalParamsBillion = 72.0f,
            activeParamsBillion = 72.0f,
            isMoe = false,
            contextLengthTokens = 131072,
            modalities = listOf(Modality.TEXT, Modality.VISION, Modality.CODE),
            tasks = listOf(ModelTask.VISION, ModelTask.REASONING, ModelTask.AGENTIC),
            license = LicenseType.APACHE_2,
            licenseName = "Apache 2.0",
            downloadsCount = 980000L,
            likesCount = 3400L,
            updatedAt = "2025-01-28",
            quantizations = listOf(
                QuantizationInfo("q4km-vl72", "GGUF", "Q4_K_M", 43800000000L, "https://huggingface.co/Qwen/Qwen2.5-VL-72B-Instruct-GGUF", "Qwen2.5-VL-72B-Instruct-Q4_K_M.gguf", 44500, 43000, 94.8f)
            ),
            benchmarks = listOf(
                BenchmarkScore("MMBench V1.1", 86.8f, 100f, "Multimodal", "Author Published", DataCategory.SOURCE_FACT),
                BenchmarkScore("MathVista", 74.2f, 100f, "Visual Math", "Independent Measurement", DataCategory.MEASUREMENT),
                BenchmarkScore("DocVQA", 96.1f, 100f, "Document OCR", "Independent Measurement", DataCategory.MEASUREMENT)
            ),
            capabilities = CapabilityMatrix(
                coding = 82.0f,
                reasoning = 90.0f,
                math = 84.0f,
                vision = 98.2f,
                agentic = 91.0f,
                toolCalling = 89.0f,
                multilingual = 86.0f,
                longContext = 92.0f
            ),
            lineage = LineageInfo(
                baseModelId = "Qwen/Qwen2.5-VL-72B"
            ),
            isFeatured = true,
            isTrending = true,
            isNew = true,
            repoUrl = "https://huggingface.co/Qwen/Qwen2.5-VL-72B-Instruct"
        ),
        AiModel(
            id = "mistralai/Mistral-Small-24B-Instruct-2501",
            name = "Mistral-Small-24B-Instruct",
            author = "Mistral",
            description = "Ultra efficient 24B parameter model built specifically for fast enterprise edge deployment and agentic workflows.",
            architecture = "MistralForCausalLM",
            totalParamsBillion = 24.0f,
            activeParamsBillion = 24.0f,
            isMoe = false,
            contextLengthTokens = 32768,
            modalities = listOf(Modality.TEXT, Modality.CODE),
            tasks = listOf(ModelTask.GENERAL_TEXT, ModelTask.AGENTIC, ModelTask.TOOL_CALLING),
            license = LicenseType.APACHE_2,
            licenseName = "Apache 2.0",
            downloadsCount = 640000L,
            likesCount = 1820L,
            updatedAt = "2025-01-30",
            quantizations = listOf(
                QuantizationInfo("q4km-mistral24", "GGUF", "Q4_K_M", 14800000000L, "https://huggingface.co/mistralai/Mistral-Small-24B-Instruct-GGUF", "Mistral-Small-24B-Instruct-Q4_K_M.gguf", 15200, 14000, 94.0f)
            ),
            benchmarks = listOf(
                BenchmarkScore("MMLU", 78.4f, 100f, "General Knowledge", "Author Published", DataCategory.SOURCE_FACT),
                BenchmarkScore("HumanEval", 81.2f, 100f, "Coding", "Independent Measurement", DataCategory.MEASUREMENT)
            ),
            capabilities = CapabilityMatrix(
                coding = 84.0f,
                reasoning = 86.0f,
                math = 81.0f,
                vision = 0.0f,
                agentic = 92.0f,
                toolCalling = 94.5f,
                multilingual = 88.0f,
                longContext = 85.0f
            ),
            lineage = LineageInfo(
                baseModelId = "mistralai/Mistral-Small-24B-Base"
            ),
            isFeatured = false,
            isTrending = true,
            isNew = true,
            repoUrl = "https://huggingface.co/mistralai/Mistral-Small-24B-Instruct-2501"
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
            targetDeviceName = "Tirth-PC Workstation",
            speedBytesPerSec = 78500000L, // 78.5 MB/s
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
