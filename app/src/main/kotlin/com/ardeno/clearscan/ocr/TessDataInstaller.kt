package com.ardeno.clearscan.ocr

import android.content.Context
import java.io.File
import java.io.IOException

object TessDataInstaller {
    private const val TESS_ROOT = "tesseract"
    private const val TESSDATA_DIR = "tessdata"

    fun tessRoot(context: Context): File = File(context.filesDir, TESS_ROOT)

    fun tessDataDirectory(context: Context): File =
        File(tessRoot(context), TESSDATA_DIR).apply { mkdirs() }

    @Throws(IOException::class)
    fun ensureLanguageData(
        context: Context,
        language: OcrLanguage
    ) {
        val tessCode = language.tessCode ?: return
        val target = File(tessDataDirectory(context), "$tessCode.traineddata")
        if (target.exists() && target.length() > 0L) return

        val assetPath = "$TESSDATA_DIR/$tessCode.traineddata"
        context.assets.open(assetPath).use { input ->
            target.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    fun isLanguageDataReady(
        context: Context,
        language: OcrLanguage
    ): Boolean {
        val tessCode = language.tessCode ?: return true
        val target = File(tessDataDirectory(context), "$tessCode.traineddata")
        return target.exists() && target.length() > 0L
    }
}
