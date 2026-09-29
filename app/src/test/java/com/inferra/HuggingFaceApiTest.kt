package com.inferra

import com.inferra.data.network.HuggingFaceClient
import com.inferra.data.network.NetworkToDomainMapper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HuggingFaceApiTest {

    @Test
    fun testGetModelsLiveApi() = runBlocking {
        val dtos = HuggingFaceClient.api.getModels(limit = 10, sort = "downloads")
        println("Fetched ${dtos.size} models from Hugging Face API")
        assertTrue("Expected non-empty model list from HF", dtos.isNotEmpty())

        val firstDto = dtos.first()
        println("First model ID: ${firstDto.id}")
        assertNotNull(firstDto.id)

        val domainModel = NetworkToDomainMapper.mapToDomain(firstDto)
        println("Mapped model: name=${domainModel.name}, author=${domainModel.author}, params=${domainModel.totalParamsBillion}B")
        assertNotNull(domainModel.name)
    }

    @Test
    fun testGetModelDetailLiveApi() = runBlocking {
        val modelId = "gpt2"
        val dto = HuggingFaceClient.api.getModelDetail(id = modelId)
        println("Fetched model detail for $modelId: id=${dto.id}, downloads=${dto.downloads}")
        assertNotNull(dto.id)

        val domainModel = NetworkToDomainMapper.mapToDomain(dto)
        println("Mapped detail: name=${domainModel.name}, author=${domainModel.author}, id=${domainModel.id}")
        assertEquals("gpt2", domainModel.name)
        assertEquals("openai-community/gpt2", domainModel.id)
    }

    @Test
    fun testGetModelDetailWithSlashLiveApi() = runBlocking {
        val modelId = "meta-llama/Llama-3.2-1B"
        val dto = HuggingFaceClient.api.getModelDetail(id = modelId)
        println("Fetched model detail for $modelId: id=${dto.id}, downloads=${dto.downloads}")
        assertNotNull(dto.id)

        val domainModel = NetworkToDomainMapper.mapToDomain(dto)
        println("Mapped detail: name=${domainModel.name}, author=${domainModel.author}")
        assertEquals("meta-llama/Llama-3.2-1B", domainModel.id)
        assertEquals("Llama-3.2-1B", domainModel.name)
        assertEquals("meta-llama", domainModel.author)
    }
}
