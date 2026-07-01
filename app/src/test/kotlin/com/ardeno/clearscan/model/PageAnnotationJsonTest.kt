package com.ardeno.clearscan.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PageAnnotationJsonTest {
    @Test
    fun encodesAndDecodesAllAnnotationTypes() {
        val pages = listOf(
            listOf(
                PageAnnotation.FreehandSignature(
                    points = listOf(NormalizedPoint(0.1f, 0.2f), NormalizedPoint(0.3f, 0.4f)),
                    strokeWidthRatio = 0.005f
                ),
                PageAnnotation.Highlight(
                    rect = NormalizedRect(0.1f, 0.2f, 0.5f, 0.6f)
                )
            ),
            listOf(
                PageAnnotation.StickyNote(
                    anchor = NormalizedPoint(0.7f, 0.8f),
                    text = "Review later"
                ),
                PageAnnotation.Redaction(
                    rect = NormalizedRect(0.0f, 0.0f, 0.2f, 0.2f)
                )
            )
        )

        val decoded = PageAnnotationJson.decodePages(PageAnnotationJson.encodePages(pages))

        assertEquals(2, decoded.size)
        assertEquals(2, decoded[0].size)
        assertEquals(2, decoded[1].size)

        val signature = decoded[0][0] as PageAnnotation.FreehandSignature
        assertEquals(2, signature.points.size)
        assertEquals(0.005f, signature.strokeWidthRatio, 0.0001f)

        val highlight = decoded[0][1] as PageAnnotation.Highlight
        assertEquals(0.5f, highlight.rect.right, 0.0001f)

        val note = decoded[1][0] as PageAnnotation.StickyNote
        assertEquals("Review later", note.text)

        val redaction = decoded[1][1] as PageAnnotation.Redaction
        assertTrue(redaction.rect.isLargeEnough())
    }

    @Test
    fun decodeBlankReturnsEmptyList() {
        assertTrue(PageAnnotationJson.decodePages(null).isEmpty())
        assertTrue(PageAnnotationJson.decodePages("").isEmpty())
    }
}
