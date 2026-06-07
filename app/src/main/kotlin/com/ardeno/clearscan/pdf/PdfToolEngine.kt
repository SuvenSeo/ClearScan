package com.ardeno.clearscan.pdf

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import com.ardeno.clearscan.model.ScanDocument
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PdfToolOutput(
    val title: String,
    val toolName: String,
    val workingDir: File,
    val pdfFile: File?,
    val pageImageFiles: List<File>,
    val pageCount: Int,
    val tags: List<String>,
    val ocrText: String = "",
    val searchablePdfReady: Boolean = false,
    val pdfFileName: String = "edited.pdf"
) {
    fun deleteWorkingFiles() {
        workingDir.deleteRecursively()
    }
}

class PdfToolEngine {
    suspend fun merge(
        documents: List<ScanDocument>,
        workingDir: File
    ): PdfToolOutput = withContext(Dispatchers.IO) {
        require(documents.size >= 2) { "Select at least two scans to merge." }
        val pages = copyPages(
            sourcePaths = documents.flatMap { it.pageImagePaths },
            targetDir = workingDir
        )
        require(pages.isNotEmpty()) { "These scans do not have page images to merge." }
        val pdf = writePdfFromImages(pages, File(workingDir, "merged.pdf"))
        PdfToolOutput(
            title = "Merged scans",
            toolName = "Merge",
            workingDir = workingDir,
            pdfFile = pdf,
            pageImageFiles = pages,
            pageCount = pages.size,
            tags = listOf("merged", "pdf-tool"),
            ocrText = documents.joinToString(separator = "\n\n") { it.ocrText }.trim(),
            searchablePdfReady = false,
            pdfFileName = "merged.pdf"
        )
    }

    suspend fun split(
        document: ScanDocument,
        rootDir: File
    ): List<PdfToolOutput> = withContext(Dispatchers.IO) {
        require(document.pageImagePaths.isNotEmpty()) { "This scan does not have page images to split." }
        document.pageImagePaths.mapIndexed { index, pagePath ->
            val pageDir = File(rootDir, "page-${index + 1}").apply { mkdirs() }
            val page = copyPages(listOf(pagePath), pageDir).single()
            val pdf = writePdfFromImages(listOf(page), File(pageDir, "split-page-${index + 1}.pdf"))
            PdfToolOutput(
                title = "${document.title} page ${index + 1}",
                toolName = "Split",
                workingDir = pageDir,
                pdfFile = pdf,
                pageImageFiles = listOf(page),
                pageCount = 1,
                tags = listOf("split", "pdf-tool"),
                ocrText = document.ocrText,
                pdfFileName = "split-page-${index + 1}.pdf"
            )
        }
    }

    suspend fun rotateClockwise(
        document: ScanDocument,
        workingDir: File
    ): PdfToolOutput = transformPages(
        document = document,
        workingDir = workingDir,
        title = "${document.title} rotated",
        toolName = "Rotate",
        tags = listOf("rotated", "pdf-tool"),
        pdfFileName = "rotated.pdf"
    ) { source ->
        Bitmap.createBitmap(
            source,
            0,
            0,
            source.width,
            source.height,
            Matrix().apply { postRotate(90f) },
            true
        )
    }

    suspend fun sign(
        document: ScanDocument,
        signatureText: String,
        workingDir: File
    ): PdfToolOutput {
        val cleanSignature = signatureText.trim().ifBlank { "ClearScan" }
        return transformPages(
            document = document,
            workingDir = workingDir,
            title = "${document.title} signed",
            toolName = "Signature",
            tags = listOf("signed", "pdf-tool"),
            pdfFileName = "signed.pdf"
        ) { source ->
            source.copy(Bitmap.Config.ARGB_8888, true).apply {
                val canvas = Canvas(this)
                val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(32, 48, 72)
                    strokeWidth = width.coerceAtLeast(height) / 250f
                }
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(32, 48, 72)
                    textSize = width.coerceAtLeast(height) / 28f
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.ITALIC)
                }
                val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(64, 78, 100)
                    textSize = width.coerceAtLeast(height) / 54f
                }
                val margin = width / 12f
                val baseline = height - margin
                val lineStart = width * 0.52f
                canvas.drawLine(lineStart, baseline - textPaint.textSize, width - margin, baseline - textPaint.textSize, linePaint)
                canvas.drawText(cleanSignature, lineStart, baseline - textPaint.textSize * 1.25f, textPaint)
                canvas.drawText("Signed in ClearScan", lineStart, baseline, labelPaint)
            }
        }
    }

    suspend fun redactHeader(
        document: ScanDocument,
        workingDir: File
    ): PdfToolOutput = transformPages(
        document = document,
        workingDir = workingDir,
        title = "${document.title} redacted",
        toolName = "Redaction",
        tags = listOf("redacted", "pdf-tool"),
        pdfFileName = "redacted.pdf"
    ) { source ->
        source.copy(Bitmap.Config.ARGB_8888, true).apply {
            val canvas = Canvas(this)
            val margin = width / 14f
            val top = height / 14f
            val bottom = top + (height * 0.16f)
            val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = width.coerceAtLeast(height) / 42f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            canvas.drawRect(margin, top, width - margin, bottom, boxPaint)
            canvas.drawText("REDACTED", margin + width / 30f, top + (bottom - top) * 0.62f, textPaint)
        }
    }

    suspend fun passwordProtect(
        document: ScanDocument,
        password: String,
        workingDir: File
    ): PdfToolOutput = withContext(Dispatchers.IO) {
        val cleanPassword = password.trim()
        require(cleanPassword.length >= 6) { "Use at least 6 characters for the PDF password." }
        val sourcePdf = document.bestPdfFile() ?: createBasePdf(document, workingDir)
        val output = File(workingDir, "password-protected.pdf")

        PDDocument.load(sourcePdf).use { pdf ->
            val permissions = AccessPermission()
            val policy = StandardProtectionPolicy(cleanPassword, cleanPassword, permissions).apply {
                encryptionKeyLength = 128
            }
            pdf.protect(policy)
            pdf.save(output)
        }

        PdfToolOutput(
            title = "${document.title} locked",
            toolName = "Password",
            workingDir = workingDir,
            pdfFile = output,
            pageImageFiles = emptyList(),
            pageCount = document.pageCount,
            tags = listOf("password", "pdf-tool"),
            ocrText = "",
            searchablePdfReady = false,
            pdfFileName = "password-protected.pdf"
        )
    }

    private suspend fun transformPages(
        document: ScanDocument,
        workingDir: File,
        title: String,
        toolName: String,
        tags: List<String>,
        pdfFileName: String,
        transform: (Bitmap) -> Bitmap
    ): PdfToolOutput = withContext(Dispatchers.IO) {
        require(document.pageImagePaths.isNotEmpty()) { "This scan does not have page images for $toolName." }
        val pages = document.pageImagePaths.mapIndexed { index, path ->
            val original = BitmapFactory.decodeFile(path) ?: error("Could not read page ${index + 1}.")
            val edited = transform(original)
            if (edited !== original) original.recycle()
            val target = File(workingDir, "page-${index + 1}.jpg")
            target.outputStream().use { output ->
                edited.compress(Bitmap.CompressFormat.JPEG, 94, output)
            }
            edited.recycle()
            target
        }
        val pdf = writePdfFromImages(pages, File(workingDir, pdfFileName))
        PdfToolOutput(
            title = title,
            toolName = toolName,
            workingDir = workingDir,
            pdfFile = pdf,
            pageImageFiles = pages,
            pageCount = pages.size,
            tags = tags,
            ocrText = document.ocrText,
            searchablePdfReady = false,
            pdfFileName = pdfFileName
        )
    }

    private fun copyPages(
        sourcePaths: List<String>,
        targetDir: File
    ): List<File> = sourcePaths.mapIndexed { index, path ->
        val target = File(targetDir, "page-${index + 1}.jpg")
        File(path).copyTo(target, overwrite = true)
        target
    }

    private fun createBasePdf(
        document: ScanDocument,
        workingDir: File
    ): File {
        require(document.pageImagePaths.isNotEmpty()) { "No PDF or page images are available for password protection." }
        val pages = copyPages(document.pageImagePaths, workingDir)
        return writePdfFromImages(pages, File(workingDir, "base.pdf"))
    }

    private fun writePdfFromImages(
        imageFiles: List<File>,
        output: File
    ): File {
        val pdf = PdfDocument()
        var pagesWritten = 0
        try {
            imageFiles.forEachIndexed { index, file ->
                val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return@forEachIndexed
                val pageSize = pageSizeFor(bitmap.width, bitmap.height)
                val pageInfo = PdfDocument.PageInfo.Builder(pageSize.width, pageSize.height, index + 1).create()
                val page = pdf.startPage(pageInfo)
                page.canvas.drawBitmap(bitmap, null, Rect(0, 0, pageSize.width, pageSize.height), null)
                bitmap.recycle()
                pdf.finishPage(page)
                pagesWritten += 1
            }
            require(pagesWritten > 0) { "No readable pages were available." }
            output.outputStream().use { stream ->
                pdf.writeTo(stream)
            }
        } finally {
            pdf.close()
        }
        return output
    }

    private fun ScanDocument.bestPdfFile(): File? =
        listOfNotNull(searchablePdfPath, pdfPath)
            .map(::File)
            .firstOrNull { it.exists() }

    private fun pageSizeFor(
        width: Int,
        height: Int
    ): PageSize {
        val longEdge = 842
        val shortEdge = 595
        if (width > height) {
            return PageSize(
                width = longEdge,
                height = ((height.toFloat() / width.toFloat()) * longEdge).roundToInt().coerceAtLeast(shortEdge)
            )
        }

        return PageSize(
            width = shortEdge,
            height = ((height.toFloat() / width.toFloat()) * shortEdge).roundToInt().coerceAtLeast(shortEdge)
        )
    }

    private data class PageSize(
        val width: Int,
        val height: Int
    )
}
