package com.ardeno.clearscan.pdf

import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PdfToolEngineTest {
    private lateinit var workingDir: File

    @Before
    fun setUp() {
        workingDir = createTempDir("pdf-tool-engine")
    }

    @After
    fun tearDown() {
        workingDir.deleteRecursively()
    }

    @Test
    fun validatePageIndices_acceptsValidReorder() {
        validatePageIndices(pageIndices = listOf(2, 0, 1), sourcePageCount = 3)
    }

    @Test(expected = IllegalArgumentException::class)
    fun validatePageIndices_rejectsEmptySelection() {
        validatePageIndices(pageIndices = emptyList(), sourcePageCount = 3)
    }

    @Test(expected = IllegalArgumentException::class)
    fun validatePageIndices_rejectsOutOfRangeIndex() {
        validatePageIndices(pageIndices = listOf(0, 3), sourcePageCount = 3)
    }

    @Test(expected = IllegalArgumentException::class)
    fun validatePageIndices_rejectsDuplicateIndices() {
        validatePageIndices(pageIndices = listOf(1, 1), sourcePageCount = 3)
    }

    @Test
    fun computePdfPageSize_usesLandscapeLongEdgeForWideBitmaps() {
        val (width, height) = computePdfPageSize(bitmapWidth = 2000, bitmapHeight = 1000)

        assertEquals(842, width)
        assertEquals(421, height)
    }

    @Test
    fun computePdfPageSize_usesPortraitShortEdgeForTallBitmaps() {
        val (width, height) = computePdfPageSize(bitmapWidth = 1000, bitmapHeight = 2000)

        assertEquals(595, width)
        assertEquals(1190, height)
    }

    @Test
    fun computePdfPageSize_enforcesMinimumShortEdge() {
        val (width, height) = computePdfPageSize(bitmapWidth = 4000, bitmapHeight = 500)

        assertEquals(842, width)
        assertEquals(595, height)
    }

    @Test
    fun pdfToolOutput_deleteWorkingFiles_removesWorkingDirectory() {
        val nestedFile = File(workingDir, "nested/page-1.jpg").apply {
            parentFile?.mkdirs()
            writeText("fixture")
        }
        val output = PdfToolOutput(
            title = "Test",
            toolName = "Reorder",
            workingDir = workingDir,
            pdfFile = nestedFile,
            pageImageFiles = listOf(nestedFile),
            pageCount = 1,
            tags = listOf("reordered", "pdf-tool")
        )

        output.deleteWorkingFiles()

        assertFalse(workingDir.exists())
    }
}
