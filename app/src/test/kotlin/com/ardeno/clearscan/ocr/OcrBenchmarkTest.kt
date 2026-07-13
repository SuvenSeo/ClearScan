package com.ardeno.clearscan.ocr

import com.ardeno.clearscan.testing.RobolectricUnitTest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrBenchmarkTest : RobolectricUnitTest() {
    @Test
    fun exactMatchHasZeroErrorRates() {
        val metrics = OcrBenchmark.evaluate(
            OcrBenchmarkCase(
                language = BenchmarkLanguage.Sinhala,
                sampleName = "sinhala-exact",
                expectedText = "සිංහල ලිපිය",
                actualText = "සිංහල ලිපිය"
            )
        )

        assertEquals(0.0, metrics.characterErrorRate, 0.0)
        assertEquals(0.0, metrics.wordErrorRate, 0.0)
    }

    @Test
    fun mismatchProducesNonZeroErrorRates() {
        val metrics = OcrBenchmark.evaluate(
            OcrBenchmarkCase(
                language = BenchmarkLanguage.Tamil,
                sampleName = "tamil-mismatch",
                expectedText = "தமிழ் ஆவணம்",
                actualText = "தமிழ்"
            )
        )

        assertTrue(metrics.characterErrorRate > 0.0)
        assertTrue(metrics.wordErrorRate > 0.0)
    }

    @Test
    fun summaryGroupsByLanguage() {
        val summary = OcrBenchmark.summary(
            listOf(
                OcrBenchmark.evaluate(
                    OcrBenchmarkCase(
                        language = BenchmarkLanguage.Sinhala,
                        sampleName = "si",
                        expectedText = "සිංහල",
                        actualText = "සිංහල"
                    )
                ),
                OcrBenchmark.evaluate(
                    OcrBenchmarkCase(
                        language = BenchmarkLanguage.Tamil,
                        sampleName = "ta",
                        expectedText = "தமிழ்",
                        actualText = ""
                    )
                )
            )
        )

        assertTrue(summary.contains("Sinhala"))
        assertTrue(summary.contains("Tamil"))
    }

    @Test
    fun ocrLanguageFromNameFallsBackToLatin() {
        assertEquals(OcrLanguage.Latin, OcrLanguage.fromName(null))
        assertEquals(OcrLanguage.Sinhala, OcrLanguage.fromName("Sinhala"))
        assertEquals(OcrLanguage.Tamil, OcrLanguage.fromName("Tamil"))
    }
}
