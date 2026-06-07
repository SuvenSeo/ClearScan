package com.ardeno.clearscan.data

import android.content.Context
import android.net.Uri
import com.ardeno.clearscan.model.OcrStatus
import com.ardeno.clearscan.model.ScanDocument
import com.ardeno.clearscan.pdf.PdfToolOutput
import com.ardeno.clearscan.scanner.ScannerImport
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class LocalDocumentRepository(
    private val context: Context
) {
    private val documentsRoot: File
        get() = File(context.filesDir, "documents").apply { mkdirs() }

    private val indexFile: File
        get() = File(documentsRoot, "index.json")

    suspend fun loadDocuments(): List<ScanDocument> = withContext(Dispatchers.IO) {
        readIndex()
    }

    suspend fun createDocument(import: ScannerImport): ScanDocument = withContext(Dispatchers.IO) {
        val createdAt = Instant.now()
        val id = "${createdAt.toEpochMilli()}-${UUID.randomUUID().toString().take(8)}"
        val documentDir = File(documentsRoot, id).apply { mkdirs() }
        val pageFiles = import.pageUris.mapIndexed { index, uri ->
            val file = File(documentDir, "page-${index + 1}.jpg")
            copyUriToFile(uri, file)
            file.absolutePath
        }
        val pdfPath = import.pdfUri?.let { uri ->
            val file = File(documentDir, "scan.pdf")
            copyUriToFile(uri, file)
            file.absolutePath
        }
        val pageCount = pageFiles.size.coerceAtLeast(1)
        val title = "Scan ${titleFormatter.format(createdAt.atZone(ZoneId.systemDefault()))}"
        val document = ScanDocument(
            id = id,
            title = title,
            pageCount = pageCount,
            createdAt = createdAt,
            pdfPath = pdfPath,
            pageImagePaths = pageFiles,
            ocrStatus = OcrStatus.Queued
        )

        writeIndex(listOf(document) + readIndex())
        document
    }

    suspend fun createGeneratedDocument(
        output: PdfToolOutput,
        sourceDocuments: List<ScanDocument>
    ): ScanDocument = withContext(Dispatchers.IO) {
        val createdAt = Instant.now()
        val id = "${createdAt.toEpochMilli()}-${UUID.randomUUID().toString().take(8)}"
        val documentDir = File(documentsRoot, id).apply { mkdirs() }
        val pageFiles = output.pageImageFiles.mapIndexed { index, source ->
            val target = File(documentDir, "page-${index + 1}.jpg")
            source.copyTo(target, overwrite = true)
            target.absolutePath
        }
        val pdfPath = output.pdfFile?.let { source ->
            val target = File(documentDir, output.pdfFileName)
            source.copyTo(target, overwrite = true)
            target.absolutePath
        }
        val document = ScanDocument(
            id = id,
            title = output.title,
            pageCount = output.pageCount,
            createdAt = createdAt,
            pdfPath = pdfPath,
            searchablePdfPath = pdfPath.takeIf { output.searchablePdfReady },
            pageImagePaths = pageFiles,
            tags = output.tags,
            toolName = output.toolName,
            sourceDocumentIds = sourceDocuments.map { it.id },
            ocrText = output.ocrText,
            ocrStatus = if (output.ocrText.isBlank()) OcrStatus.NotStarted else OcrStatus.Ready,
            searchablePdfReady = output.searchablePdfReady
        )

        writeIndex(listOf(document) + readIndex())
        output.deleteWorkingFiles()
        document
    }

    fun newWorkingDirectory(prefix: String): File {
        val safePrefix = prefix.filter { it.isLetterOrDigit() || it == '-' }.ifBlank { "tool" }
        return File(context.cacheDir, "pdf-tools/$safePrefix-${UUID.randomUUID()}").apply { mkdirs() }
    }

    suspend fun updateOcrResult(
        id: String,
        ocrText: String,
        searchablePdfPath: String?,
        status: OcrStatus
    ): ScanDocument? = withContext(Dispatchers.IO) {
        updateDocument(id) { document ->
            document.copy(
                updatedAt = Instant.now(),
                ocrText = ocrText,
                ocrStatus = status,
                searchablePdfPath = searchablePdfPath,
                searchablePdfReady = searchablePdfPath != null && status == OcrStatus.Ready
            )
        }
    }

    suspend fun markOcrProcessing(id: String): ScanDocument? = withContext(Dispatchers.IO) {
        updateDocument(id) { document ->
            document.copy(
                updatedAt = Instant.now(),
                ocrStatus = OcrStatus.Processing
            )
        }
    }

    suspend fun markOcrFailed(id: String): ScanDocument? = withContext(Dispatchers.IO) {
        updateDocument(id) { document ->
            document.copy(
                updatedAt = Instant.now(),
                ocrStatus = OcrStatus.Failed
            )
        }
    }

    suspend fun deleteDocument(id: String): Boolean = withContext(Dispatchers.IO) {
        val documents = readIndex()
        val document = documents.firstOrNull { it.id == id } ?: return@withContext false
        File(documentsRoot, document.id).deleteRecursively()
        writeIndex(documents.filterNot { it.id == id })
        true
    }

    fun documentDirectory(document: ScanDocument): File = File(documentsRoot, document.id).apply { mkdirs() }

    private fun updateDocument(
        id: String,
        transform: (ScanDocument) -> ScanDocument
    ): ScanDocument? {
        var updated: ScanDocument? = null
        val documents = readIndex().map { document ->
            if (document.id == id) {
                transform(document).also { updated = it }
            } else {
                document
            }
        }

        if (updated != null) {
            writeIndex(documents)
        }

        return updated
    }

    private fun copyUriToFile(uri: Uri, target: File) {
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Unable to read scanner output." }
            target.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun readIndex(): List<ScanDocument> {
        if (!indexFile.exists()) return emptyList()

        val raw = indexFile.readText()
        if (raw.isBlank()) return emptyList()

        val array = JSONArray(raw)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(item.toScanDocument())
            }
        }
    }

    private fun writeIndex(documents: List<ScanDocument>) {
        val array = JSONArray()
        documents.forEach { document ->
            array.put(document.toJson())
        }
        indexFile.writeText(array.toString(2))
    }

    private fun ScanDocument.toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("title", title)
        .put("pageCount", pageCount)
        .put("createdAt", createdAt.toString())
        .put("updatedAt", updatedAt.toString())
        .put("pdfPath", pdfPath)
        .put("searchablePdfPath", searchablePdfPath)
        .put("pageImagePaths", JSONArray(pageImagePaths))
        .put("tags", JSONArray(tags))
        .put("toolName", toolName)
        .put("sourceDocumentIds", JSONArray(sourceDocumentIds))
        .put("ocrText", ocrText)
        .put("ocrStatus", ocrStatus.name)
        .put("searchablePdfReady", searchablePdfReady)

    private fun JSONObject.toScanDocument(): ScanDocument = ScanDocument(
        id = getString("id"),
        title = getString("title"),
        pageCount = getInt("pageCount"),
        createdAt = Instant.parse(getString("createdAt")),
        updatedAt = runCatching { Instant.parse(getString("updatedAt")) }.getOrDefault(Instant.parse(getString("createdAt"))),
        pdfPath = optString("pdfPath").takeUnless { it.isBlank() || it == "null" },
        searchablePdfPath = optString("searchablePdfPath").takeUnless { it.isBlank() || it == "null" },
        pageImagePaths = getJSONArray("pageImagePaths").toStringList(),
        tags = optJSONArray("tags")?.toStringList().orEmpty(),
        toolName = optString("toolName").takeUnless { it.isBlank() || it == "null" },
        sourceDocumentIds = optJSONArray("sourceDocumentIds")?.toStringList().orEmpty(),
        ocrText = optString("ocrText"),
        ocrStatus = runCatching { OcrStatus.valueOf(getString("ocrStatus")) }.getOrDefault(OcrStatus.NotStarted),
        searchablePdfReady = optBoolean("searchablePdfReady", false)
    )

    private fun JSONArray.toStringList(): List<String> = buildList {
        for (index in 0 until length()) {
            add(getString(index))
        }
    }

    private companion object {
        val titleFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
    }
}
