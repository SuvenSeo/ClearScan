package com.ardeno.clearscan.pdf

import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import com.ardeno.clearscan.model.ScanDocument
import com.ardeno.clearscan.ocr.DocumentOcrResult
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SearchablePdfWriter {
    suspend fun write(
        document: ScanDocument,
        ocrResult: DocumentOcrResult,
        targetDir: File
    ): File? = withContext(Dispatchers.IO) {
        if (document.pageImagePaths.isEmpty()) return@withContext null

        val pageOcr = ocrResult.pages.associateBy { it.pageIndex }
        val output = File(targetDir, "searchable.pdf")
        val pdf = PdfDocument()
        var pagesWritten = 0

        try {
            document.pageImagePaths.forEachIndexed { index, path ->
                val bitmap = BitmapFactory.decodeFile(path) ?: return@forEachIndexed
                val pageSize = pageSizeFor(bitmap.width, bitmap.height)
                val pageInfo = PdfDocument.PageInfo.Builder(pageSize.width, pageSize.height, index + 1).create()
                val page = pdf.startPage(pageInfo)

                page.canvas.drawBitmap(bitmap, null, Rect(0, 0, pageSize.width, pageSize.height), null)
                bitmap.recycle()

                pageOcr[index]?.let { result ->
                    drawInvisibleTextLayer(
                        canvas = page.canvas,
                        pageWidth = pageSize.width,
                        pageHeight = pageSize.height,
                        result = result
                    )
                }

                pdf.finishPage(page)
                pagesWritten += 1
            }

            if (pagesWritten == 0) {
                output.delete()
                return@withContext null
            }

            output.outputStream().use { stream ->
                pdf.writeTo(stream)
            }
        } finally {
            pdf.close()
        }

        output
    }

    private fun drawInvisibleTextLayer(
        canvas: android.graphics.Canvas,
        pageWidth: Int,
        pageHeight: Int,
        result: com.ardeno.clearscan.ocr.PageOcrResult
    ) {
        val scaleX = pageWidth.toFloat() / result.sourceWidth.toFloat()
        val scaleY = pageHeight.toFloat() / result.sourceHeight.toFloat()
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(1, 0, 0, 0)
            isSubpixelText = true
        }

        result.lines.forEach { line ->
            textPaint.textSize = ((line.bottom - line.top) * scaleY * 0.9f).coerceIn(4f, 32f)
            canvas.drawText(
                line.text,
                line.left * scaleX,
                line.bottom * scaleY,
                textPaint
            )
        }
    }

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
