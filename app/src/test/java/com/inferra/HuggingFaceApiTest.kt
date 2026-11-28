package com.inferra

import com.inferra.data.network.HuggingFaceClient
import com.inferra.data.network.NetworkToDomainMapper
import com.inferra.domain.model.RepoType
import kotlinx.coroutines.runBlocking
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
        assertNotNull(firstDto.id)

        val repoItem = NetworkToDomainMapper.mapModelToRepoItem(firstDto)
        assertTrue(repoItem.type == RepoType.MODEL)
        assertNotNull(repoItem.name)
    }

    @Test
    fun testGetDatasetsLiveApi() = runBlocking {
        val dtos = HuggingFaceClient.api.getDatasets(limit = 10, sort = "downloads")
        println("Fetched ${dtos.size} datasets from Hugging Face API")
        assertTrue("Expected non-empty dataset list from HF", dtos.isNotEmpty())

        val firstDto = dtos.first()
        val repoItem = NetworkToDomainMapper.mapDatasetToRepoItem(firstDto)
        assertTrue(repoItem.type == RepoType.DATASET)
        assertNotNull(repoItem.name)
    }

    @Test
    fun testGetSpacesLiveApi() = runBlocking {
        val dtos = HuggingFaceClient.api.getSpaces(limit = 10, sort = "likes")
        println("Fetched ${dtos.size} spaces from Hugging Face API")
        assertTrue("Expected non-empty spaces list from HF", dtos.isNotEmpty())

        val firstDto = dtos.first()
        val repoItem = NetworkToDomainMapper.mapSpaceToRepoItem(firstDto)
        assertTrue(repoItem.type == RepoType.SPACE)
        assertNotNull(repoItem.name)
    }
}
