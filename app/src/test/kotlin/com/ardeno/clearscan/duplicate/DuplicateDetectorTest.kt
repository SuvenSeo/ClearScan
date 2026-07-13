package com.ardeno.clearscan.duplicate

import com.ardeno.clearscan.model.ScanDocument
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun findsDocumentsNearSimilarityThreshold() {
        val base = 0x0123456789ABCDEFL
        val nearThresholdHash = PerceptualHash.toHex(hashWithBitFlips(base, (0 until 10).toList()))
        val distinctHash = PerceptualHash.toHex(hashWithBitFlips(base, (0 until 20).toList()))

        assertEquals(10, PerceptualHash.hammingDistance(base, PerceptualHash.fromHex(nearThresholdHash)))
        assertTrue(PerceptualHash.isSimilar(base, PerceptualHash.fromHex(nearThresholdHash)))
        assertFalse(PerceptualHash.isSimilar(base, PerceptualHash.fromHex(distinctHash)))

        val documents = listOf(
            sampleDocument("doc-1", listOf(PerceptualHash.toHex(base))),
            sampleDocument("doc-2", listOf(nearThresholdHash)),
            sampleDocument("doc-3", listOf(distinctHash))
        )

        val groups = DuplicateDetector().findDuplicateGroups(documents)

        assertEquals(1, groups.size)
        assertEquals(setOf("doc-1", "doc-2"), groups.first().documentIds.toSet())
    }

    @Test
    fun groupsThreeDocumentDuplicateChain() {
        val base = 0x0123456789ABCDEFL
        val hash1 = PerceptualHash.toHex(base)
        val hash2 = PerceptualHash.toHex(hashWithBitFlips(base, (0 until 10).toList()))
        val hash3Base = hashWithBitFlips(base, (0 until 10).toList())
        val hash3 = PerceptualHash.toHex(hashWithBitFlips(hash3Base, (10 until 20).toList()))

        assertTrue(PerceptualHash.isSimilar(base, PerceptualHash.fromHex(hash2)))
        assertTrue(PerceptualHash.isSimilar(PerceptualHash.fromHex(hash2), PerceptualHash.fromHex(hash3)))
        assertFalse(PerceptualHash.isSimilar(base, PerceptualHash.fromHex(hash3)))

        val documents = listOf(
            sampleDocument("doc-1", listOf(hash1)),
            sampleDocument("doc-2", listOf(hash2)),
            sampleDocument("doc-3", listOf(hash3)),
            sampleDocument("doc-4", listOf(PerceptualHash.toHex(0xFFFFFFFFFFFFFFFFL)))
        )

        val groups = DuplicateDetector().findDuplicateGroups(documents)

        assertEquals(1, groups.size)
        assertEquals(setOf("doc-1", "doc-2", "doc-3"), groups.first().documentIds.toSet())
    }

    private fun hashWithBitFlips(base: Long, bitPositions: List<Int>): Long {
        var result = base
        for (position in bitPositions) {
            result = result xor (1L shl position)
        }
        return result
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
