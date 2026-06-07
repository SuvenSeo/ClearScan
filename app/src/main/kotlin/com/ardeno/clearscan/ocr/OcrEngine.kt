package com.ardeno.clearscan.ocr

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.ardeno.clearscan.model.ScanDocument
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

data class DocumentOcrResult(
    val text: String,
    val pages: List<PageOcrResult>
)

data class PageOcrResult(
    val pageIndex: Int,
    val text: String,
    val sourceWidth: Int,
    val sourceHeight: Int,
    val lines: List<OcrLine>
)

data class OcrLine(
    val text: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)

class OcrEngine(
    private val context: Context
) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognize(document: ScanDocument): DocumentOcrResult = withContext(Dispatchers.Default) {
        val pages = buildList {
            document.pageImagePaths.forEachIndexed { index, path ->
                add(recognizePage(index, path))
            }
        }

        DocumentOcrResult(
            text = pages.joinToString(separator = "\n\n") { it.text }.trim(),
            pages = pages
        )
    }

    fun close() {
        recognizer.close()
    }

    private suspend fun recognizePage(
        pageIndex: Int,
        path: String
    ): PageOcrResult {
        val file = File(path)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val width = bounds.outWidth.takeIf { it > 0 } ?: 1
        val height = bounds.outHeight.takeIf { it > 0 } ?: 1
        val result = process(InputImage.fromFilePath(context, Uri.fromFile(file)))

        return PageOcrResult(
            pageIndex = pageIndex,
            text = result.text,
            sourceWidth = width,
            sourceHeight = height,
            lines = result.textBlocks.flatMap { block ->
                block.lines.mapNotNull { line ->
                    val box = line.boundingBox ?: return@mapNotNull null
                    val trimmed = line.text.trim()
                    if (trimmed.isBlank()) return@mapNotNull null
                    OcrLine(
                        text = trimmed,
                        left = box.left,
                        top = box.top,
                        right = box.right,
                        bottom = box.bottom
                    )
                }
            }
        )
    }

    private suspend fun process(image: InputImage): Text = suspendCancellableCoroutine { continuation ->
        recognizer.process(image)
            .addOnSuccessListener { text ->
                if (continuation.isActive) continuation.resume(text)
            }
            .addOnFailureListener { error ->
                if (continuation.isActive) continuation.resumeWithException(error)
            }
    }
}
