package com.ardeno.clearscan.duplicate

import com.ardeno.clearscan.model.ScanDocument
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DuplicateDetectorTest {
    @Test
    fun findsDocumentsWithSimilarPageHashes() {
        val sharedHash = PerceptualHash.toHex(0x0123456789ABCDEFL)
        val documents = listOf(
            sampleDocument("doc-1", listOf(sharedHash)),
            sampleDocument("doc-2", listOf(sharedHash)),
            sampleDocument("doc-3", listOf(PerceptualHash.toHex(0x1111111111111111L)))
        )

        val groups = DuplicateDetector().findDuplicateGroups(documents)

        assertEquals(1, groups.size)
        assertEquals(setOf("doc-1", "doc-2"), groups.first().documentIds.toSet())
    }

    @Test
    fun ignoresDocumentsWithoutHashes() {
        val documents = listOf(
            sampleDocument("doc-1", emptyList()),
            sampleDocument("doc-2", emptyList())
        )

        assertTrue(DuplicateDetector().findDuplicateGroups(documents).isEmpty())
    }

    private fun sampleDocument(id: String, hashes: List<String>): ScanDocument = ScanDocument(
        id = id,
        title = "Scan $id",
        pageCount = 1,
        createdAt = Instant.parse("2026-06-07T00:00:00Z"),
        pdfPath = "/local/$id.pdf",
        pageImagePaths = listOf("/local/$id.jpg"),
        pageHashes = hashes
    )
}
