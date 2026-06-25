package com.ardeno.clearscan.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.ardeno.clearscan.model.ScanDocument
import java.io.File
import java.time.Instant

object TextExportHelper {
    fun writeTextFile(context: Context, document: ScanDocument): File? {
        val text = document.ocrText.trim()
        if (text.isBlank()) return null

        val cacheDir = File(context.cacheDir, "text-exports").apply { mkdirs() }
        val safeTitle = document.title
            .replace(Regex("[^A-Za-z0-9._-]+"), "_")
            .trim('_')
            .ifBlank { "scan" }
        val file = File(cacheDir, "$safeTitle-${Instant.now().toEpochMilli()}.txt")
        file.writeText(text)
        return file
    }

    fun createShareIntent(context: Context, document: ScanDocument): Intent? {
        val file = writeTextFile(context, document) ?: return null
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, document.ocrText.trim())
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
