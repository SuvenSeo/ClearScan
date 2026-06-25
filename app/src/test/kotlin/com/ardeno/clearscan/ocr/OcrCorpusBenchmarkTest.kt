package com.ardeno.clearscan.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrCorpusBenchmarkTest {
    @Test
    fun loadsPlaceholderCorpusFromTestResources() {
        val entries = OcrCorpusBenchmark.loadFromClasspath(javaClass.classLoader!!)

        assertEquals(2, entries.size)
        assertTrue(entries.any { it.id == "sinhala-synthetic-01" })
        assertTrue(entries.any { it.id == "tamil-synthetic-01" })
    }

    @Test
    fun evaluatesClasspathCorpusWithCerAndWer() {
        val metrics = OcrCorpusBenchmark.evaluateClasspathCorpus(javaClass.classLoader!!)

        assertEquals(2, metrics.size)

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
