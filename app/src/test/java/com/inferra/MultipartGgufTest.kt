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

class MultipartGgufTest {

    @Test
    fun testGroupsSplitGgufFilesIntoSingleQuantOption() = runBlocking {
        val fakeModelDto = HuggingFaceModelDto(
            id = "Qwen/Qwen2.5-72B-Instruct",
            author = "Qwen",
            downloads = 800000,
            likes = 5000,
            siblings = listOf(
                HfSiblingDto("qwen2.5-72b-instruct-q4_k_m-00001-of-00003.gguf", size = 15000000000L),
                HfSiblingDto("qwen2.5-72b-instruct-q4_k_m-00002-of-00003.gguf", size = 15000000000L),
                HfSiblingDto("qwen2.5-72b-instruct-q4_k_m-00003-of-00003.gguf", size = 12000000000L),
                HfSiblingDto("README.md", size = 10000L)
            )
        )

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
                return fakeModelDto
            }
        }

        val repository = QuantDiscoveryRepository(dummyApi)
        val discovered = repository.discoverQuantizations(fakeModelDto, 72.0f)

        assertNotNull(discovered)
        assertEquals(1, discovered.size)

        val quant = discovered.first()
        assertEquals("GGUF", quant.format)
        assertEquals("Q4_K_M", quant.quantType)
        assertEquals(42000000000L, quant.fileSizeBytes)
        assertTrue(quant.fileName.contains("3 split parts"))
    }
}
