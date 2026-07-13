package com.ardeno.clearscan.data.db

import com.ardeno.clearscan.model.DocumentFolder
import com.ardeno.clearscan.model.NormalizedPoint
import com.ardeno.clearscan.model.NormalizedRect
import com.ardeno.clearscan.model.OcrStatus
import com.ardeno.clearscan.model.PageAnnotation
import com.ardeno.clearscan.model.ReceiptFields
import com.ardeno.clearscan.model.ScanDocument
import com.ardeno.clearscan.model.ScanMode
import com.ardeno.clearscan.ocr.OcrLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ScanDatabaseTest {

    @Test
    fun scanDocumentEntity_roundtrip_allFields() {
        val now = Instant.now()
        val doc = ScanDocument(
            id = "test-123",
            title = "Test Document",
            pageCount = 3,
            createdAt = now,
            updatedAt = now.plusSeconds(60),
            pdfPath = "/docs/test.pdf",
            searchablePdfPath = "/docs/test_searchable.pdf",
            pageImagePaths = listOf("/docs/page1.jpg", "/docs/page2.jpg"),
            tags = listOf("receipt", "important"),
            toolName = "scanner",
            sourceDocumentIds = listOf("src-1"),
            ocrText = "Sample OCR text content",
            ocrStatus = OcrStatus.Ready,
            searchablePdfReady = true,
            ocrLanguage = OcrLanguage.Sinhala,
            scanMode = ScanMode.IdCard,
            folderId = "folder-1",
            isFavorite = true,
            pageHashes = listOf("hash1", "hash2"),
            receiptFields = ReceiptFields(merchant = "Store", amount = "25.00", date = "2026-01-15"),
            pageAnnotations = listOf(
                listOf(
                    PageAnnotation.FreehandSignature(
                        points = listOf(NormalizedPoint(0.1f, 0.2f), NormalizedPoint(0.3f, 0.4f)),
                        strokeWidthRatio = 0.005f
                    ),
                    PageAnnotation.Highlight(rect = NormalizedRect(0.1f, 0.2f, 0.3f, 0.4f))
                ),
                listOf(
                    PageAnnotation.Redaction(rect = NormalizedRect(0.2f, 0.3f, 0.4f, 0.5f))
                )
            )
        )

        val entity = doc.toEntity()
        assertEquals(doc.id, entity.id)
        assertEquals(doc.updatedAt.toEpochMilli(), entity.updatedAt)

        val restored = entity.toDocument()
        assertEquals(doc.id, restored.id)
        assertEquals(doc.title, restored.title)
        assertEquals(doc.pageCount, restored.pageCount)
        assertEquals(doc.pdfPath, restored.pdfPath)
        assertEquals(doc.searchablePdfPath, restored.searchablePdfPath)
        assertEquals(doc.pageImagePaths, restored.pageImagePaths)
        assertEquals(doc.tags, restored.tags)
        assertEquals(doc.toolName, restored.toolName)
        assertEquals(doc.sourceDocumentIds, restored.sourceDocumentIds)
        assertEquals(doc.ocrText, restored.ocrText)
        assertEquals(doc.ocrStatus, restored.ocrStatus)
        assertEquals(doc.searchablePdfReady, restored.searchablePdfReady)
        assertEquals(doc.ocrLanguage, restored.ocrLanguage)
        assertEquals(doc.scanMode, restored.scanMode)
        assertEquals(doc.folderId, restored.folderId)
        assertEquals(doc.isFavorite, restored.isFavorite)
        assertEquals(doc.pageHashes, restored.pageHashes)
        assertEquals(doc.pageAnnotations, restored.pageAnnotations)
        assertNotNull(restored.receiptFields)
        assertEquals("Store", restored.receiptFields!!.merchant)
        assertEquals("25.00", restored.receiptFields!!.amount)
        assertEquals("2026-01-15", restored.receiptFields!!.date)
    }

    @Test
    fun scanDocumentEntity_roundtrip_minimalFields() {
        val now = Instant.now()
        val doc = ScanDocument(
            id = "test-min",
            title = "Minimal",
            pageCount = 1,
            createdAt = now,
            pdfPath = null,
            pageImagePaths = emptyList()
        )

        val entity = doc.toEntity()
        val restored = entity.toDocument()

        assertEquals(doc.id, restored.id)
        assertEquals(doc.title, restored.title)
        assertEquals(doc.pageCount, restored.pageCount)
        assertNull(restored.pdfPath)
        assertNull(restored.searchablePdfPath)
        assertTrue(restored.pageImagePaths.isEmpty())
        assertTrue(restored.tags.isEmpty())
        assertEquals(OcrStatus.NotStarted, restored.ocrStatus)
        assertEquals(OcrLanguage.Latin, restored.ocrLanguage)
        assertEquals(ScanMode.Document, restored.scanMode)
        assertNull(restored.folderId)
        assertEquals(false, restored.isFavorite)
        assertNull(restored.receiptFields)
        assertTrue(restored.pageAnnotations.isEmpty())
        assertTrue(restored.pageHashes.isEmpty())
    }

    @Test
    fun scanDocumentEntity_pageHashes_roundTrip() {
        val now = Instant.now()
        val pageHashes = listOf(
            "a1b2c3d4e5f6789012345678abcdef01",
            "fedcba0987654321fedcba0987654321",
            "0000000000000000"
        )
        val doc = ScanDocument(
            id = "page-hashes-test",
            title = "Page Hashes",
            pageCount = pageHashes.size,
            createdAt = now,
            pdfPath = null,
            pageImagePaths = pageHashes.mapIndexed { index, _ -> "/pages/page$index.jpg" },
            pageHashes = pageHashes
        )

        val entity = doc.toEntity()
        assertTrue(entity.jsonPayload.contains("\"pageHashes\""))
        pageHashes.forEach { hash ->
            assertTrue(entity.jsonPayload.contains(hash))
        }

        val restored = entity.toDocument()
        assertEquals(pageHashes, restored.pageHashes)
        assertEquals(pageHashes.size, restored.pageCount)
    }

    @Test
    fun scanDocumentEntity_pageHashes_emptyList_roundTrip() {
        val now = Instant.now()
        val doc = ScanDocument(
            id = "page-hashes-empty",
            title = "No Hashes",
            pageCount = 1,
            createdAt = now,
            pdfPath = null,
            pageImagePaths = listOf("/pages/page0.jpg")
        )

        val restored = doc.toEntity().toDocument()
        assertTrue(restored.pageHashes.isEmpty())
    }

    @Test
    fun scanDocumentEntity_roundtrip_nullableFields() {
        val now = Instant.now()
        val doc = ScanDocument(
            id = "test-null",
            title = "Nullable",
            pageCount = 1,
            createdAt = now,
            pdfPath = null,
            searchablePdfPath = null,
            pageImagePaths = listOf("/path/to/page.jpg"),
            toolName = null,
            folderId = null
        )

        val restored = doc.toEntity().toDocument()
        assertNull(restored.pdfPath)
        assertNull(restored.searchablePdfPath)
        assertNull(restored.toolName)
        assertNull(restored.folderId)
    }

    @Test
    fun documentFolderEntity_roundtrip() {
        val now = Instant.now()
        val folder = DocumentFolder(
            id = "folder-42",
            name = "Tax Documents",
            createdAt = now
        )

        val entity = folder.toEntity()
        assertEquals(folder.id, entity.id)
        assertEquals(folder.createdAt.toEpochMilli(), entity.updatedAt)

        val restored = entity.toFolder()
        assertEquals(folder.id, restored.id)
        assertEquals(folder.name, restored.name)
    }

    @Test
    fun entity_jsonPayload_structure() {
        val now = Instant.now()
        val doc = ScanDocument(
            id = "json-test-1",
            title = "JSON Payload",
            pageCount = 2,
            createdAt = now,
            pdfPath = "/path/doc.pdf",
            pageImagePaths = listOf("/path/p1.jpg"),
            tags = listOf("a", "b")
        )

        val entity = doc.toEntity()
        assertTrue(entity.jsonPayload.contains("\"id\":\"json-test-1\""))
        assertTrue(entity.jsonPayload.contains("\"title\":\"JSON Payload\""))
        assertTrue(entity.jsonPayload.contains("\"pageCount\":2"))
        assertTrue(entity.jsonPayload.contains("\"pdfPath\":\"/path/doc.pdf\""))
        assertTrue(entity.jsonPayload.contains("\"ocrStatus\":\"NotStarted\""))
    }

    @Test
    fun documentFolderEntity_jsonPayload_structure() {
        val now = Instant.now()
        val folder = DocumentFolder(id = "f-1", name = "Work", createdAt = now)

        val entity = folder.toEntity()
        assertTrue(entity.jsonPayload.contains("\"id\":\"f-1\""))
        assertTrue(entity.jsonPayload.contains("\"name\":\"Work\""))
        assertTrue(entity.jsonPayload.contains("\"createdAt\""))
    }
}
