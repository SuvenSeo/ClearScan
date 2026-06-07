package com.ardeno.clearscan.model

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanDocumentTest {
    @Test
    fun newDocumentDefaultsToPrivateLocalState() {
        val document = ScanDocument(
            id = "doc-1",
            title = "Scan",
            pageCount = 2,
            createdAt = Instant.parse("2026-06-07T00:00:00Z"),
            pdfPath = "/local/scan.pdf",
            pageImagePaths = listOf("/local/page-1.jpg", "/local/page-2.jpg")
        )

        assertEquals(OcrStatus.NotStarted, document.ocrStatus)
        assertFalse(document.searchablePdfReady)
        assertEquals(2, document.pageCount)
        assertEquals("", document.ocrText)
        assertEquals(document.createdAt, document.updatedAt)
    }

    @Test
    fun matchesQueryChecksTitleTagsAndOcrText() {
        val document = ScanDocument(
            id = "doc-1",
            title = "Invoice Scan",
            pageCount = 1,
            createdAt = Instant.parse("2026-06-07T00:00:00Z"),
            pdfPath = "/local/scan.pdf",
            pageImagePaths = listOf("/local/page-1.jpg"),
            tags = listOf("tax"),
            ocrText = "Supplier total 1500"
        )

        assertTrue(document.matchesQuery(""))
        assertTrue(document.matchesQuery("invoice"))
        assertTrue(document.matchesQuery("tax"))
        assertTrue(document.matchesQuery("supplier"))
        assertFalse(document.matchesQuery("passport"))
    }
}
