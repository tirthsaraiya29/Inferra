package com.inferra

import com.inferra.data.network.HfSiblingDto
import com.inferra.data.network.HuggingFaceApi
import com.inferra.data.network.HuggingFaceModelDto
import com.inferra.data.repository.QuantDiscoveryRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuantDiscoveryTest {

    @Test
    fun testParseGgufSiblingsFromModelDto() = runBlocking {
        val fakeBaseDto = HuggingFaceModelDto(
            id = "Qwen/Qwen2.5-32B-Instruct",
            author = "Qwen",
            downloads = 500000,
            likes = 3000,
            siblings = listOf(
                HfSiblingDto("Qwen2.5-32B-Instruct-Q4_K_M.gguf", size = 19500000000L),
                HfSiblingDto("Qwen2.5-32B-Instruct-Q8_0.gguf", size = 34000000000L),
                HfSiblingDto("README.md", size = 5000L)
            )
        )

        // Dummy client mock that returns fake base dto
        val dummyApi = object : HuggingFaceApi {
            override suspend fun getModels(
                token: String?,
                search: String?,
                pipelineTag: String?,
                filter: String?,
                sort: String?,
                direction: Int?,
                limit: Int?,
                page: Int?,
                full: Boolean?,
                expand: List<String>?
            ): List<HuggingFaceModelDto> {
                return emptyList()
            }

            override suspend fun getModelDetail(token: String?, id: String): HuggingFaceModelDto {
                return fakeBaseDto
            }
        }

        val repo = QuantDiscoveryRepository(dummyApi)
        val discovered = repo.discoverQuantizations(fakeBaseDto, 32.5f)

        assertNotNull(discovered)
        assertTrue(discovered.isNotEmpty())

        val q4 = discovered.find { it.quantType == "Q4_K_M" }
        assertNotNull(q4)
        assertEquals("GGUF", q4!!.format)
        assertEquals(19500000000L, q4.fileSizeBytes)
        assertTrue(q4.downloadUrl.endsWith("Qwen2.5-32B-Instruct-Q4_K_M.gguf"))

        val q8 = discovered.find { it.quantType == "Q8_0" }
        assertNotNull(q8)
        assertEquals("GGUF", q8!!.format)
        assertEquals(34000000000L, q8.fileSizeBytes)
    }
}
