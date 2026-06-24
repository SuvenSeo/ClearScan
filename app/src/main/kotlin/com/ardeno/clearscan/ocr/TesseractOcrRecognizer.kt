package com.ardeno.clearscan.ocr

import android.content.Context
import android.graphics.BitmapFactory
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class TesseractOcrRecognizer(
    private val context: Context
) {
    private val mutex = Mutex()
    private val apiByLanguage = mutableMapOf<OcrLanguage, TessBaseAPI>()

    suspend fun recognizePage(
        language: OcrLanguage,
        pageIndex: Int,
        path: String
    ): PageOcrResult = withContext(Dispatchers.Default) {
        require(language.usesTesseract) { "Tesseract does not support ${language.label}." }

        TessDataInstaller.ensureLanguageData(context, language)
        val api = mutex.withLock { apiFor(language) }
        val bitmap = BitmapFactory.decodeFile(path)
            ?: throw IllegalStateException("Unable to decode page image for OCR.")

        try {
            api.setImage(bitmap)
            val text = api.getUTF8Text().orEmpty().trim()
            val lines = extractLines(api).ifEmpty {
                if (text.isBlank()) {
                    emptyList()
                } else {
                    listOf(
                        OcrLine(
                            text = text,
                            left = 0,
                            top = 0,
                            right = bitmap.width,
                            bottom = bitmap.height
                        )
                    )
                }
            }

            PageOcrResult(
                pageIndex = pageIndex,
                text = text,
                sourceWidth = bitmap.width,
                sourceHeight = bitmap.height,
                lines = lines
            )
        } finally {
            bitmap.recycle()
            api.clear()
        }
    }

    fun close() {
        synchronized(apiByLanguage) {
            apiByLanguage.values.forEach { api ->
                runCatching { api.recycle() }
            }
            apiByLanguage.clear()
        }
    }

    private fun apiFor(language: OcrLanguage): TessBaseAPI {
        return apiByLanguage.getOrPut(language) {
            val tessCode = language.tessCode
                ?: throw IllegalArgumentException("Missing tessdata language code.")
            val dataPath = TessDataInstaller.tessRoot(context).absolutePath
            TessBaseAPI().apply {
                val initialized = init(dataPath, tessCode, TessBaseAPI.OEM_LSTM_ONLY)
                if (!initialized) {
                    recycle()
                    throw IllegalStateException("Tesseract failed to load ${language.label} traineddata.")
                }
                pageSegMode = TessBaseAPI.PageSegMode.PSM_AUTO
            }
        }
    }

    private fun extractLines(api: TessBaseAPI): List<OcrLine> {
        val iterator = api.resultIterator ?: return emptyList()
        val lines = mutableListOf<OcrLine>()
        iterator.begin()
        do {
            val text = iterator.getUTF8Text(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE)
                ?.trim()
                .orEmpty()
            if (text.isBlank()) continue

            val rect = iterator.getBoundingRect(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE)
            lines.add(
                OcrLine(
                    text = text,
                    left = rect.left,
                    top = rect.top,
                    right = rect.right,
                    bottom = rect.bottom
                )
            )
        } while (iterator.next(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE))

        return lines
    }
}
