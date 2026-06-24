package com.ardeno.clearscan.scanner

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.rendering.PDFRenderer
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FileImportResolver {
    suspend fun resolve(
        context: Context,
        uris: List<Uri>
    ): ScannerImport = withContext(Dispatchers.IO) {
        require(uris.isNotEmpty()) { "No files were selected." }

        val tempDir = File(context.cacheDir, "file-import/${System.currentTimeMillis()}").apply { mkdirs() }
        val pageUris = mutableListOf<Uri>()
        var pdfUri: Uri? = null

        uris.forEachIndexed { fileIndex, uri ->
            val mimeType = context.contentResolver.getType(uri).orEmpty()
            when {
                mimeType.equals("application/pdf", ignoreCase = true) ||
                    uri.lastPathSegment?.endsWith(".pdf", ignoreCase = true) == true -> {
                    val pdfFile = File(tempDir, "import-${fileIndex + 1}.pdf")
                    copyUriToFile(context, uri, pdfFile)
                    if (uris.size == 1) {
                        pdfUri = uri
                    }
                    pageUris += renderPdfPages(pdfFile, tempDir, "import-${fileIndex + 1}").map { it.toUri() }
                }
                mimeType.startsWith("image/", ignoreCase = true) ||
                    isLikelyImageUri(uri) -> {
                    pageUris += uri
                }
                else -> error("Unsupported file type. Choose PDF or image files.")
            }
        }

        require(pageUris.isNotEmpty()) { "No pages could be read from the selected files." }

        ScannerImport(
            pdfUri = pdfUri,
            pageUris = pageUris
        )
    }

    private fun isLikelyImageUri(uri: Uri): Boolean {
        val path = uri.lastPathSegment.orEmpty().lowercase()
        return path.endsWith(".jpg") ||
            path.endsWith(".jpeg") ||
            path.endsWith(".png") ||
            path.endsWith(".webp") ||
            path.endsWith(".heic") ||
            path.endsWith(".heif")
    }

    private fun copyUriToFile(context: Context, uri: Uri, target: File) {
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Unable to read selected file." }
            target.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun renderPdfPages(
        pdfFile: File,
        targetDir: File,
        prefix: String
    ): List<File> {
        PDDocument.load(pdfFile).use { document ->
            val renderer = PDFRenderer(document)
            return (0 until document.numberOfPages).map { pageIndex ->
                val bitmap = renderer.renderImage(pageIndex, 2f)
                val pageFile = File(targetDir, "$prefix-page-${pageIndex + 1}.jpg")
                pageFile.outputStream().use { output ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, output)
                }
                bitmap.recycle()
                pageFile
            }
        }
    }
}
