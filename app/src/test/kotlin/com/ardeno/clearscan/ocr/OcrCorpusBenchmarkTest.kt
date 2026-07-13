package com.ardeno.clearscan.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrCorpusBenchmarkTest {
    @Test
    fun loadsExpandedCorpusFromTestResources() {
        val entries = OcrCorpusBenchmark.loadFromClasspath(javaClass.classLoader!!)

        assertTrue("Expected at least 25 Sinhala entries", entries.count { it.language == BenchmarkLanguage.Sinhala } >= 25)
        assertTrue("Expected at least 25 Tamil entries", entries.count { it.language == BenchmarkLanguage.Tamil } >= 25)
        assertTrue(entries.any { it.id == "sinhala-synthetic-01" })
        assertTrue(entries.any { it.id == "tamil-synthetic-01" })
    }

    @Test
    fun evaluatesClasspathCorpusWithCerAndWer() {
        val entries = OcrCorpusBenchmark.loadFromClasspath(javaClass.classLoader!!)
        val metrics = OcrCorpusBenchmark.evaluate(entries)

        assertEquals(entries.count { it.actualText != null }, metrics.size)

        val sinhala = metrics.first { it.sampleName == "sinhala-synthetic-01" }
        assertEquals(0.0, sinhala.characterErrorRate, 0.0)

        val tamil = metrics.first { it.sampleName == "tamil-synthetic-01" }
        assertTrue(tamil.characterErrorRate > 0.0)
    }

    @Test
    fun summaryIncludesBothLanguages() {
        val summary = OcrCorpusBenchmark.summary(
            OcrCorpusBenchmark.evaluateClasspathCorpus(javaClass.classLoader!!)
        )

        assertTrue(summary.contains("Sinhala"))
        assertTrue(summary.contains("Tamil"))
    }

    @Test
    fun missingCorpusDirectoryReturnsEmptyList() {
        val entries = OcrCorpusBenchmark.loadFromClasspath(
            classLoader = javaClass.classLoader!!,
            resourceDir = "ocr-corpus-missing"
        )

        assertTrue(entries.isEmpty())
    }
}
