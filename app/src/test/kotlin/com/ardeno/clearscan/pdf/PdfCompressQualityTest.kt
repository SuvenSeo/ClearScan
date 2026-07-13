package com.ardeno.clearscan.pdf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfCompressQualityTest {
    @Test
    fun entries_coverAllCompressionPresets() {
        assertEquals(3, PdfCompressQuality.entries.size)
        assertEquals(
            listOf(PdfCompressQuality.High, PdfCompressQuality.Balanced, PdfCompressQuality.Small),
            PdfCompressQuality.entries
        )
    }

    @Test
    fun high_keepsOriginalResolution() {
        val quality = PdfCompressQuality.High

        assertEquals(88, quality.jpegQuality)
        assertNull(quality.maxLongEdge)
        assertEquals("High quality", quality.label)
    }

    @Test
    fun balanced_targetsModerateCompression() {
        val quality = PdfCompressQuality.Balanced

        assertEquals(72, quality.jpegQuality)
        assertEquals(2200, quality.maxLongEdge)
        assertEquals("Balanced", quality.label)
    }

    @Test
    fun small_targetsSmallestOutput() {
        val quality = PdfCompressQuality.Small

        assertEquals(58, quality.jpegQuality)
        assertEquals(1600, quality.maxLongEdge)
        assertEquals("Smallest size", quality.label)
    }

    @Test
    fun jpegQuality_decreasesWithMoreAggressivePresets() {
        val qualities = PdfCompressQuality.entries.map { it.jpegQuality }

        assertTrue(qualities.zipWithNext().all { (previous, next) -> previous > next })
    }

    @Test
    fun maxLongEdge_decreasesWhenPresent() {
        val limits = PdfCompressQuality.entries.mapNotNull { it.maxLongEdge }

        assertEquals(listOf(2200, 1600), limits)
        assertTrue(limits.zipWithNext().all { (previous, next) -> previous > next })
    }
}
