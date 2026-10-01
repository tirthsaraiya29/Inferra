package com.inferra

import com.inferra.domain.usecase.BenchmarkRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BenchmarkRegistryTest {

    @Test
    fun testRetrievesStandardBenchmarksForCanonicalModel() {
        val cid = "canonical:qwen-qwen2.5-coder-32b-instruct"
        val results = BenchmarkRegistry.getStandardBenchmarksForModel(cid)

        assertNotNull(results)
        assertTrue(results.isNotEmpty())

        val humanEval = results.find { it.benchmarkVersionId == BenchmarkRegistry.HUMANEVAL_DEF.id }
        assertNotNull(humanEval)
        assertEquals(92.7f, humanEval!!.score)
    }

    @Test
    fun testComparabilityCheckReportsComparableForIdenticalConfigs() {
        val cid = "canonical:qwen-qwen2.5-coder-32b-instruct"
        val results = BenchmarkRegistry.getStandardBenchmarksForModel(cid)
        val resA = results.first()
        val resB = resA.copy()

        val check = BenchmarkRegistry.checkComparability(resA, resB)
        assertTrue(check.isComparable)
        assertTrue(check.warnings.isEmpty())
    }
}
