package com.ardeno.clearscan.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.ardeno.clearscan.model.ScanDocument
import java.io.File
import kotlinx.coroutines.Dispatchers
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
    private val latinRecognizer = LatinOcrRecognizer(context)
    private val tesseractRecognizer = TesseractOcrRecognizer(context)

    suspend fun recognize(
        document: ScanDocument,
        language: OcrLanguage
    ): DocumentOcrResult = withContext(Dispatchers.Default) {
        val pages = buildList {
            document.pageImagePaths.forEachIndexed { index, path ->
                add(recognizePage(language, index, path))
            }
        }

        DocumentOcrResult(
            text = pages.joinToString(separator = "\n\n") { it.text }.trim(),
            pages = pages
        )
    }

    fun close() {
        latinRecognizer.close()
        tesseractRecognizer.close()
    }

    private suspend fun recognizePage(
        language: OcrLanguage,
        pageIndex: Int,
        path: String
    ): PageOcrResult = if (language.usesTesseract) {
        tesseractRecognizer.recognizePage(language, pageIndex, path)
    } else {
        latinRecognizer.recognizePage(pageIndex, path)
    }
}

object OcrBenchmarkRunner {
    data class SyntheticSample(
        val language: OcrLanguage,
        val sampleName: String,
        val expectedText: String
    )

    private val syntheticSamples = listOf(
        SyntheticSample(
            language = OcrLanguage.Sinhala,
            sampleName = "sinhala-synthetic-print",
            expectedText = "සිංහල ලිපිය"
        ),
        SyntheticSample(
            language = OcrLanguage.Tamil,
            sampleName = "tamil-synthetic-print",
            expectedText = "தமிழ் ஆவணம்"
        )
    )

    suspend fun runSyntheticEngineBenchmark(
        context: Context,
        engine: OcrEngine = OcrEngine(context)
    ): List<OcrBenchmarkMetrics> {
        val workingDir = File(context.cacheDir, "ocr-benchmark").apply {
            deleteRecursively()
            mkdirs()
        }

        return try {
            syntheticSamples.map { sample ->
                val imagePath = renderSampleText(
                    targetDir = workingDir,
                    fileName = "${sample.sampleName}.png",
                    text = sample.expectedText
                )
                val document = ScanDocument(
                    id = "benchmark-${sample.sampleName}",
                    title = sample.sampleName,
                    pageCount = 1,
                    createdAt = java.time.Instant.now(),
                    pdfPath = null,
                    pageImagePaths = listOf(imagePath),
                    ocrLanguage = sample.language
                )
                val ocrResult = engine.recognize(document, sample.language)
                val benchmarkLanguage = sample.language.toBenchmarkLanguage()
                    ?: error("Synthetic benchmark requires Sinhala or Tamil.")
                OcrBenchmark.evaluate(
                    OcrBenchmarkCase(
                        language = benchmarkLanguage,
                        sampleName = sample.sampleName,
                        expectedText = sample.expectedText,
                        actualText = ocrResult.text
                    )
                )
            }
        } finally {
            workingDir.deleteRecursively()
            engine.close()
        }
    }

    private suspend fun renderSampleText(
        targetDir: File,
        fileName: String,
        text: String
    ): String = withContext(Dispatchers.Default) {
        val width = 1280
        val height = 360
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 72f
            typeface = Typeface.DEFAULT
        }
        canvas.drawText(text, 48f, height / 2f, paint)

        val output = File(targetDir, fileName)
        output.outputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        bitmap.recycle()
        output.absolutePath
    }
}
