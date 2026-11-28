package com.inferra

import com.inferra.domain.model.RepoItem
import com.inferra.domain.model.RepoType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ModelRepositoryTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun testRepoItemSerializationRoundTrip() {
        val originalRepo = RepoItem(
            id = "meta-llama/Llama-3.1-8B-Instruct",
            name = "Llama-3.1-8B-Instruct",
            author = "meta-llama",
            type = RepoType.MODEL,
            description = "State-of-the-art open model",
            downloads = 2500000L,
            likes = 12000L,
            pipelineTag = "text-generation",
            tags = listOf("llama", "text-generation", "en"),
            license = "llama3.1"
        )

        val jsonString = json.encodeToString(originalRepo)
        assertNotNull(jsonString)

        val deserializedRepo = json.decodeFromString<RepoItem>(jsonString)

        assertEquals(originalRepo.id, deserializedRepo.id)
        assertEquals(originalRepo.name, deserializedRepo.name)
        assertEquals(originalRepo.author, deserializedRepo.author)
        assertEquals(originalRepo.type, deserializedRepo.type)
        assertEquals(originalRepo.downloads, deserializedRepo.downloads)
        assertEquals(originalRepo.likes, deserializedRepo.likes)
    }
}
