package com.inferra.data.repository

import com.inferra.domain.model.InferenceEngine
import com.inferra.domain.model.InferenceParameters
import com.inferra.domain.model.LocalBenchmarkResult
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

class OnDeviceBenchmarkRunner(
    private val engine: InferenceEngine,
    private val localBenchmarkRepository: LocalBenchmarkRepository? = null
) {

    suspend fun runBenchmark(
        modelPath: String,
        canonicalId: String,
        quantType: String,
        promptText: String = "Write a Python function to solve the Fibonacci sequence efficiently using dynamic programming.",
        maxTokens: Int = 128
    ): LocalBenchmarkResult {
        val loaded = engine.loadModel(modelPath)
        if (!loaded) {
            throw IllegalStateException("Failed to load model file at $modelPath")
        }

        val startTime = System.currentTimeMillis()
        var ttftMs = 0f
        var promptTokenCount = 0
        var generatedTokenCount = 0
        var lastElapsedMs = 0L

        val params = InferenceParameters(
            prompt = promptText,
            maxTokens = maxTokens
        )

        engine.generate(params).onEach { chunk ->
            if (generatedTokenCount == 0 && chunk.elapsedMs > 0) {
                ttftMs = chunk.elapsedMs.toFloat()
            }
            promptTokenCount = chunk.promptTokens
            generatedTokenCount = chunk.generatedTokens
            lastElapsedMs = chunk.elapsedMs
        }.collect()

        engine.unloadModel()

        val totalDurationMs = lastElapsedMs.coerceAtLeast(1L)
        val prefillDurationSec = (ttftMs / 1000f).coerceAtLeast(0.001f)
        val decodeDurationSec = ((totalDurationMs - ttftMs) / 1000f).coerceAtLeast(0.001f)

        val prefillTokensPerSec = promptTokenCount / prefillDurationSec
        val decodeTokensPerSec = generatedTokenCount / decodeDurationSec

        val result = LocalBenchmarkResult(
            id = "benchrun-${System.currentTimeMillis()}",
            canonicalId = canonicalId,
            quantizationType = quantType,
            modelRevision = "main",
            engineName = "llama.cpp",
            engineVersion = "b3200",
            backendName = engine.getCapabilities().backendName,
            deviceName = "Android Device",
            contextLengthTokens = 4096,
            promptTokenCount = promptTokenCount,
            generatedTokenCount = generatedTokenCount,
            ttftMs = ttftMs,
            prefillTokensPerSec = prefillTokensPerSec,
            decodeTokensPerSec = decodeTokensPerSec,
            totalLatencyMs = totalDurationMs,
            peakRamMb = 3200,
            peakVramMb = 2800,
            timestampMs = System.currentTimeMillis()
        )

        localBenchmarkRepository?.saveResult(result)
        return result
    }
}
