package com.ardeno.clearscan.data

import android.content.Context
import android.net.Uri
import com.ardeno.clearscan.data.db.*
import com.ardeno.clearscan.image.ImageEnhancer
import com.ardeno.clearscan.model.DocumentFolder
import com.ardeno.clearscan.model.OcrStatus
import com.ardeno.clearscan.model.PageAnnotation
import com.ardeno.clearscan.model.PageAnnotationJson
import com.ardeno.clearscan.model.ScanDocument
import com.ardeno.clearscan.model.ScanMode
import com.ardeno.clearscan.ocr.OcrLanguage
import com.ardeno.clearscan.pdf.PdfToolOutput
import com.ardeno.clearscan.scanner.ScannerImport
import com.ardeno.clearscan.vault.EncryptedFileStore
import com.ardeno.clearscan.vault.VaultCrypto
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.ardeno.clearscan.model.ReceiptFields
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
    context: Context,
    private val vaultCrypto: VaultCrypto = VaultCrypto(),
    private val encryptedFileStore: EncryptedFileStore = EncryptedFileStore(context, vaultCrypto)
) {
    private val appContext = context.applicationContext
    private val db = ScanDatabase.getInstance(context)

    private val documentsRoot: File
        get() = encryptedFileStore.storageRoot()

    private val indexFile: File
        get() = File(documentsRoot, "index.json")

    private val foldersFile: File
        get() = File(documentsRoot, "folders.json")

    suspend fun loadFolders(): List<DocumentFolder> = withContext(Dispatchers.IO) {
        val roomFolders = db.documentFolderDao().getAll()
        if (roomFolders.isNotEmpty()) {
            roomFolders.map { it.toFolder() }
        } else {
            readFolders().also { folders -> syncFoldersToRoom(folders) }
        }
    }

    suspend fun loadDocuments(): List<ScanDocument> = withContext(Dispatchers.IO) {
        vaultCrypto.ensureVaultKey()
        migrateLegacyPlaintextFiles()
        val roomDocs = db.scanDocumentDao().getAll()
        if (roomDocs.isNotEmpty()) {
            roomDocs.map { it.toDocument() }.map(::toReadableDocument)
        } else {
            readIndex().also { docs -> syncDocumentsToRoom(docs) }.map(::toReadableDocument)
        }
    }

    suspend fun createDocument(
        import: ScannerImport,
        ocrLanguage: OcrLanguage = OcrLanguage.Latin,
        titlePrefix: String = "Scan"
    ): ScanDocument = withContext(Dispatchers.IO) {
        vaultCrypto.ensureVaultKey()
        val createdAt = Instant.now()
        val id = "${createdAt.toEpochMilli()}-${UUID.randomUUID().toString().take(8)}"
        val documentDir = File(documentsRoot, id).apply { mkdirs() }
        val pageFiles = import.pageUris.mapIndexed { index, uri ->
            val file = File(documentDir, "page-${index + 1}.jpg")
            copyUriToFile(uri, file)
            if (import.enhanceImages) {
                enhanceImageFile(file)
            }
            encryptedFileStore.encryptPlaintextFile(file).absolutePath
        }
        val pdfPath = import.pdfUri?.let { uri ->
            val file = File(documentDir, "scan.pdf")
            copyUriToFile(uri, file)
            encryptedFileStore.encryptPlaintextFile(file).absolutePath
        }
        val pageCount = pageFiles.size.coerceAtLeast(1)
        val resolvedPrefix = when (import.scanMode) {
            ScanMode.IdCard -> "ID scan"
            ScanMode.Document -> titlePrefix
        }
        val title = "$resolvedPrefix ${titleFormatter.format(createdAt.atZone(ZoneId.systemDefault()))}"
        val tags = buildList {
            addAll(import.tags)
            if (import.scanMode == ScanMode.IdCard) add("id-card")
        }
        val storedDocument = ScanDocument(
            id = id,
            title = title,
            pageCount = pageCount,
            createdAt = createdAt,
            pdfPath = pdfPath,
            pageImagePaths = pageFiles,
            tags = tags,
            ocrStatus = OcrStatus.Queued,
            ocrLanguage = ocrLanguage,
            scanMode = import.scanMode
        )

        writeIndex(listOf(storedDocument) + readIndex())
        toReadableDocument(storedDocument)
    }

    suspend fun createDocumentFromPagePaths(
        pagePaths: List<String>,
        enhanceImages: Boolean = true,
        ocrLanguage: OcrLanguage = OcrLanguage.Latin
    ): ScanDocument = withContext(Dispatchers.IO) {
        vaultCrypto.ensureVaultKey()
        val createdAt = Instant.now()
        val id = "${createdAt.toEpochMilli()}-${UUID.randomUUID().toString().take(8)}"
        val documentDir = File(documentsRoot, id).apply { mkdirs() }
        val pageFiles = pagePaths.mapIndexed { index, sourcePath ->
            val source = File(sourcePath)
            val file = File(documentDir, "page-${index + 1}.jpg")
            source.copyTo(file, overwrite = true)
            if (enhanceImages) {
                enhanceImageFile(file)
            }
            encryptedFileStore.encryptPlaintextFile(file).absolutePath
        }
        val pageCount = pageFiles.size.coerceAtLeast(1)
        val title = "Scan ${titleFormatter.format(createdAt.atZone(ZoneId.systemDefault()))}"
        val storedDocument = ScanDocument(
            id = id,
            title = title,
            pageCount = pageCount,
            createdAt = createdAt,
            pdfPath = null,
            pageImagePaths = pageFiles,
            tags = listOf("page-turn"),
            ocrStatus = OcrStatus.Queued,
            scanMode = ScanMode.Document,
            ocrLanguage = ocrLanguage
        )

        writeIndex(listOf(storedDocument) + readIndex())
        pagePaths.forEach { path -> File(path).delete() }
        toReadableDocument(storedDocument)
    }

    suspend fun createGeneratedDocument(
        output: PdfToolOutput,
        sourceDocuments: List<ScanDocument>
    ): ScanDocument = withContext(Dispatchers.IO) {
        vaultCrypto.ensureVaultKey()
        val createdAt = Instant.now()
        val id = "${createdAt.toEpochMilli()}-${UUID.randomUUID().toString().take(8)}"
        val documentDir = File(documentsRoot, id).apply { mkdirs() }
        val pageFiles = output.pageImageFiles.mapIndexed { index, source ->
            val target = File(documentDir, "page-${index + 1}.jpg")
            source.copyTo(target, overwrite = true)
            encryptedFileStore.encryptPlaintextFile(target).absolutePath
        }
        val pdfPath = output.pdfFile?.let { source ->
            val target = File(documentDir, output.pdfFileName)
            source.copyTo(target, overwrite = true)
            encryptedFileStore.encryptPlaintextFile(target).absolutePath
        }
        val storedDocument = ScanDocument(
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

        writeIndex(listOf(storedDocument) + readIndex())
        output.deleteWorkingFiles()
        toReadableDocument(storedDocument)
    }

    fun newWorkingDirectory(prefix: String): File {
        val safePrefix = prefix.filter { it.isLetterOrDigit() || it == '-' }.ifBlank { "tool" }
        return File(appContext.cacheDir, "pdf-tools/$safePrefix-${UUID.randomUUID()}").apply { mkdirs() }
    }

    suspend fun updateOcrResult(
        id: String,
        ocrText: String,
        searchablePdfPath: String?,
        status: OcrStatus,
        tags: List<String>? = null,
        receiptFields: ReceiptFields? = null
    ): ScanDocument? = withContext(Dispatchers.IO) {
        val encryptedSearchablePath = searchablePdfPath?.let { path ->
            val source = File(path)
            if (encryptedFileStore.isEncryptedPath(path)) {
                path
            } else {
                encryptedFileStore.encryptPlaintextFile(source).absolutePath
            }
        }

        updateDocument(id) { document ->
            val mergedTags = tags?.let { (document.tags + it).distinct() } ?: document.tags
            document.copy(
                updatedAt = Instant.now(),
                ocrText = ocrText,
                ocrStatus = status,
                searchablePdfPath = encryptedSearchablePath,
                searchablePdfReady = encryptedSearchablePath != null && status == OcrStatus.Ready,
                tags = mergedTags,
                receiptFields = receiptFields ?: document.receiptFields
            )
        }?.let(::toReadableDocument)
    }

    suspend fun markOcrProcessing(id: String): ScanDocument? = withContext(Dispatchers.IO) {
        updateDocument(id) { document ->
            document.copy(
                updatedAt = Instant.now(),
                ocrStatus = OcrStatus.Processing
            )
        }?.let(::toReadableDocument)
    }

    suspend fun updateOcrLanguage(
        id: String,
        language: OcrLanguage
    ): ScanDocument? = withContext(Dispatchers.IO) {
        updateDocument(id) { document ->
            document.copy(
                updatedAt = Instant.now(),
                ocrLanguage = language
            )
        }
    }

    suspend fun markOcrFailed(id: String): ScanDocument? = withContext(Dispatchers.IO) {
        updateDocument(id) { document ->
            document.copy(
                updatedAt = Instant.now(),
                ocrStatus = OcrStatus.Failed
            )
        }?.let(::toReadableDocument)
    }

    suspend fun deleteDocument(id: String): Boolean = withContext(Dispatchers.IO) {
        deleteDocuments(setOf(id)) == 1
    }

    suspend fun deleteDocuments(ids: Set<String>): Int = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext 0
        val documents = readIndex()
        val toDelete = documents.filter { it.id in ids }
        if (toDelete.isEmpty()) return@withContext 0
        toDelete.forEach { document ->
            File(documentsRoot, document.id).deleteRecursively()
            encryptedFileStore.clearReadableCache(document.id)
        }
        writeIndex(documents.filterNot { it.id in ids })
        runCatching { db.scanDocumentDao().deleteById(it) }

        toDelete.size
    }

    suspend fun createFolder(name: String): DocumentFolder = withContext(Dispatchers.IO) {
        val cleanName = name.trim()
        require(cleanName.isNotBlank()) { "Folder name cannot be empty." }
        val folder = DocumentFolder(
            id = UUID.randomUUID().toString(),
            name = cleanName,
            createdAt = Instant.now()
        )
        writeFolders(listOf(folder) + readFolders())
        folder
    }

    suspend fun renameFolder(folderId: String, name: String): DocumentFolder? = withContext(Dispatchers.IO) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return@withContext null
        var updated: DocumentFolder? = null
        val folders = readFolders().map { folder ->
            if (folder.id == folderId) {
                folder.copy(name = cleanName).also { updated = it }
            } else {
                folder
            }
        }
        if (updated != null) writeFolders(folders)
        updated
    }

    suspend fun deleteFolder(folderId: String): Boolean = withContext(Dispatchers.IO) {
        val folders = readFolders()
        if (folders.none { it.id == folderId }) return@withContext false
        writeFolders(folders.filterNot { it.id == folderId })
        val documents = readIndex().map { document ->
            if (document.folderId == folderId) document.copy(folderId = null) else document
        }
        writeIndex(documents)
        true
    }

    suspend fun updateDocumentTags(id: String, tags: List<String>): ScanDocument? = withContext(Dispatchers.IO) {
        updateDocument(id) { document ->
            document.copy(updatedAt = Instant.now(), tags = tags)
        }?.let(::toReadableDocument)
    }

    suspend fun setDocumentFavorite(id: String, isFavorite: Boolean): ScanDocument? = withContext(Dispatchers.IO) {
        updateDocument(id) { document ->
            document.copy(updatedAt = Instant.now(), isFavorite = isFavorite)
        }?.let(::toReadableDocument)
    }

    suspend fun moveDocumentToFolder(id: String, folderId: String?): ScanDocument? = withContext(Dispatchers.IO) {
        if (folderId != null && readFolders().none { it.id == folderId }) return@withContext null
        updateDocument(id) { document ->
            document.copy(updatedAt = Instant.now(), folderId = folderId)
        }?.let(::toReadableDocument)
    }

    fun documentDirectory(document: ScanDocument): File = File(documentsRoot, document.id).apply { mkdirs() }

    fun storageLocation(): String = documentsRoot.absolutePath

    suspend fun updatePageAnnotations(
        id: String,
        pageAnnotations: List<List<PageAnnotation>>
    ): ScanDocument? = withContext(Dispatchers.IO) {
        updateDocument(id) { document ->
            document.copy(
                updatedAt = Instant.now(),
                pageAnnotations = pageAnnotations
            )
        }?.let(::toReadableDocument)
    }

    fun hasStoredDocuments(): Boolean {
        if (!indexFile.exists()) return false
        return readIndex().isNotEmpty()
    }

    suspend fun writeBackupMetadataFiles(stagingRoot: File) = withContext(Dispatchers.IO) {
        stagingRoot.mkdirs()
        val documents = readIndex()
        val folders = readFolders()
        File(stagingRoot, "index.json").writeText(documentsToJsonArray(documents).toString(2))
        File(stagingRoot, "folders.json").writeText(foldersToJsonArray(folders).toString(2))
    }

    fun invalidateIndexCache() {
        // Index is persisted on disk; Room migration may add caching later.
    }

    suspend fun clearReadableCache() = withContext(Dispatchers.IO) {
        encryptedFileStore.clearAllReadableCache()
    }

    private fun toReadableDocument(document: ScanDocument): ScanDocument =
        document.copy(
            pdfPath = document.pdfPath?.let { encryptedFileStore.decryptToCache(it, document.id).absolutePath },
            searchablePdfPath = document.searchablePdfPath?.let {
                encryptedFileStore.decryptToCache(it, document.id).absolutePath
            },
            pageImagePaths = document.pageImagePaths.map { path ->
                encryptedFileStore.decryptToCache(path, document.id).absolutePath
            }
        )

    private fun migrateLegacyPlaintextFiles() {
        if (!indexFile.exists()) return

        var changed = false
        val migrated = readIndex().map { document ->
            val documentDir = File(documentsRoot, document.id)
            if (!documentDir.exists()) return@map document

            val migratedPdf = document.pdfPath?.let { path ->
                migrateStoredPath(path)?.also { changed = changed || it != path }
            }
            val migratedSearchable = document.searchablePdfPath?.let { path ->
                migrateStoredPath(path)?.also { changed = changed || it != path }
            }
            val migratedPages = document.pageImagePaths.map { path ->
                migrateStoredPath(path)?.also { changed = changed || it != path } ?: path
            }

            document.copy(
                pdfPath = migratedPdf,
                searchablePdfPath = migratedSearchable,
                pageImagePaths = migratedPages
            )
        }

        if (changed) {
            writeIndex(migrated)
        }
    }

    private fun migrateStoredPath(path: String): String? {
        val file = File(path)
        if (!file.exists()) return path.takeIf { encryptedFileStore.isEncryptedPath(path) }
        if (encryptedFileStore.isEncryptedPath(path)) return path
        return encryptedFileStore.encryptPlaintextFile(file).absolutePath
    }

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
        appContext.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Unable to read scanner output." }
            target.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun enhanceImageFile(file: File) {
        val original = BitmapFactory.decodeFile(file.absolutePath) ?: return
        val enhanced = ImageEnhancer.enhance(original)
        if (enhanced !== original) {
            original.recycle()
        }
        file.outputStream().use { output ->
            enhanced.compress(Bitmap.CompressFormat.JPEG, 92, output)
        }
        enhanced.recycle()
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
        indexFile.writeText(documentsToJsonArray(documents).toString(2))
        syncDocumentsToRoom(documents)
    }

    private fun syncDocumentsToRoom(documents: List<ScanDocument>) {
        val entities = documents.map { it.toEntity() }
        runCatching { db.scanDocumentDao().upsertAll(entities) }
    }

    private fun documentsToJsonArray(documents: List<ScanDocument>): JSONArray {
        val array = JSONArray()
        documents.forEach { document ->
            array.put(document.toJson())
        }
        return array
    }

    private fun foldersToJsonArray(folders: List<DocumentFolder>): JSONArray {
        val array = JSONArray()
        folders.forEach { folder ->
            array.put(
                JSONObject()
                    .put("id", folder.id)
                    .put("name", folder.name)
                    .put("createdAt", folder.createdAt.toString())
            )
        }
        return array
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
        .put("ocrLanguage", ocrLanguage.name)
        .put("scanMode", scanMode.name)
        .put("folderId", folderId)
        .put("isFavorite", isFavorite)
        .put("pageHashes", JSONArray(pageHashes))
        .put("receiptFields", receiptFields?.toJson())
        .put("pageAnnotations", JSONArray(PageAnnotationJson.encodePages(pageAnnotations)))

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
        searchablePdfReady = optBoolean("searchablePdfReady", false),
        ocrLanguage = OcrLanguage.fromName(optString("ocrLanguage")),
        scanMode = runCatching { ScanMode.valueOf(optString("scanMode", ScanMode.Document.name)) }
            .getOrDefault(ScanMode.Document),
        folderId = optString("folderId").takeUnless { it.isBlank() || it == "null" },
        isFavorite = optBoolean("isFavorite", false),
        pageHashes = optJSONArray("pageHashes")?.toStringList().orEmpty(),
        receiptFields = optJSONObject("receiptFields")?.toReceiptFields(),
        pageAnnotations = PageAnnotationJson.decodePages(optString("pageAnnotations").takeUnless { it.isBlank() || it == "null" })
    )

    private fun ReceiptFields.toJson(): JSONObject = JSONObject()
        .put("merchant", merchant)
        .put("amount", amount)
        .put("date", date)

    private fun JSONObject.toReceiptFields(): ReceiptFields = ReceiptFields(
        merchant = optString("merchant").takeUnless { it.isBlank() || it == "null" },
        amount = optString("amount").takeUnless { it.isBlank() || it == "null" },
        date = optString("date").takeUnless { it.isBlank() || it == "null" }
    )

    private fun JSONArray.toStringList(): List<String> = buildList {
        for (index in 0 until length()) {
            add(getString(index))
        }
    }

    private fun readFolders(): List<DocumentFolder> {
        if (!foldersFile.exists()) return emptyList()
        val raw = foldersFile.readText()
        if (raw.isBlank()) return emptyList()
        val array = JSONArray(raw)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    DocumentFolder(
                        id = item.getString("id"),
                        name = item.getString("name"),
                        createdAt = Instant.parse(item.getString("createdAt"))
                    )
                )
            }
        }
    }

    private fun writeFolders(folders: List<DocumentFolder>) {
        foldersFile.writeText(foldersToJsonArray(folders).toString(2))
        syncFoldersToRoom(folders)
    }

    private fun syncFoldersToRoom(folders: List<DocumentFolder>) {
        val entities = folders.map { it.toEntity() }
        runCatching { db.documentFolderDao().upsertAll(entities) }
    }

    private companion object {
        val titleFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
    }
}
