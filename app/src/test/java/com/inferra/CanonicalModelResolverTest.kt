package com.inferra

import com.inferra.domain.model.ModelVariant
import com.inferra.domain.usecase.CanonicalModelResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CanonicalModelResolverTest {

    @Test
    fun testResolvesHuggingFaceIdToCanonicalId() {
        val rawHf = "Qwen/Qwen2.5-Coder-32B-Instruct"
        val canonicalId = CanonicalModelResolver.resolveCanonicalId(rawHf)

        assertEquals("canonical:qwen-qwen2.5-coder-32b-instruct", canonicalId)

        val canonicalModel = CanonicalModelResolver.getCanonicalModel(canonicalId)
        assertNotNull(canonicalModel)
        assertEquals("Qwen2.5-Coder-32B-Instruct", canonicalModel.name)
        assertEquals(ModelVariant.INSTRUCT, canonicalModel.variantType)
        assertEquals(32.5f, canonicalModel.totalParamsBillion)
    }

    @Test
    fun testResolvesLlamaToCanonicalId() {
        val rawHf = "meta-llama/Llama-3.3-70B-Instruct"
        val canonicalId = CanonicalModelResolver.resolveCanonicalId(rawHf)

        assertEquals("canonical:meta-llama-3.3-70b-instruct", canonicalId)

        val canonicalModel = CanonicalModelResolver.getCanonicalModel(canonicalId)
        assertEquals("Llama-3.3-70B-Instruct", canonicalModel.name)
        assertEquals(70.0f, canonicalModel.totalParamsBillion)
    }
}
