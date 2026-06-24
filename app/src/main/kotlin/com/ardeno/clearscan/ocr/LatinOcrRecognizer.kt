package com.ardeno.clearscan.ocr

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class LatinOcrRecognizer(
    private val context: Context
) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognizePage(
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

    fun close() {
        recognizer.close()
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
