package com.inferra.data.repository

import com.inferra.domain.model.EngineCapabilities
import com.inferra.domain.model.EngineState
import com.inferra.domain.model.GenerationChunk
import com.inferra.domain.model.InferenceEngine
import com.inferra.domain.model.InferenceParameters
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class LlamaCppEngineAdapter : InferenceEngine {

    private var currentState: EngineState = EngineState.UNLOADED
    private var loadedModelPath: String? = null

    override suspend fun loadModel(modelPath: String, params: Map<String, Any>): Boolean {
        currentState = EngineState.LOADING
        delay(300) // Simulated model initialization and memory allocation
        loadedModelPath = modelPath
        currentState = EngineState.READY
        return true
    }

    override suspend fun unloadModel() {
        currentState = EngineState.UNLOADED
        loadedModelPath = null
    }

    override fun generate(params: InferenceParameters): Flow<GenerationChunk> = flow {
        if (currentState != EngineState.READY && currentState != EngineState.GENERATING) {
            throw IllegalStateException("Engine is not ready to generate tokens.")
        }
        currentState = EngineState.GENERATING

        val promptTokens = (params.prompt.length / 4).coerceAtLeast(1)
        val dummyTokens = listOf("Here", " is", " a", " benchmark", " execution", " response", " generated", " on", " device", ".")
        val startMs = System.currentTimeMillis()

        dummyTokens.forEachIndexed { idx, token ->
            delay(30) // ~33 tok/sec stream
            val elapsed = System.currentTimeMillis() - startMs
            emit(
                GenerationChunk(
                    token = token,
                    isFinished = idx == dummyTokens.lastIndex,
                    promptTokens = promptTokens,
                    generatedTokens = idx + 1,
                    elapsedMs = elapsed
                )
            )
        }

        currentState = EngineState.READY
    }

    override fun getEngineState(): EngineState = currentState

    override fun getCapabilities(): EngineCapabilities = EngineCapabilities(
        supportedQuantizations = listOf("GGUF", "Q4_K_M", "Q8_0", "Q5_K_M", "Q6_K", "FP16"),
        maxContextLength = 131072,
        gpuAccelerationSupported = true,
        backendName = "llama.cpp (Vulkan)"
    )
}
