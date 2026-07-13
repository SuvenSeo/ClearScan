package com.ardeno.clearscan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.ardeno.clearscan.backup.BackupRestoreManager
import com.ardeno.clearscan.data.AppPreferences
import com.ardeno.clearscan.data.LocalDocumentRepository
import com.ardeno.clearscan.data.SelfHostSettings
import com.ardeno.clearscan.duplicate.DuplicateDetector
import com.ardeno.clearscan.export.SelfHostExporter
import com.ardeno.clearscan.intelligence.DocumentTagger
import com.ardeno.clearscan.intelligence.ReceiptFieldExtractor
import com.ardeno.clearscan.model.DocumentFolder
import com.ardeno.clearscan.model.LibraryViewMode
import com.ardeno.clearscan.model.OcrStatus
import com.ardeno.clearscan.model.PageAnnotation
import com.ardeno.clearscan.model.ReceiptFields
import com.ardeno.clearscan.model.ScanDocument
import com.ardeno.clearscan.model.ScanMode
import com.ardeno.clearscan.ocr.IdRedactionSuggester
import com.ardeno.clearscan.ocr.IdRedactionSuggestion
import com.ardeno.clearscan.ocr.DocumentOcrResult
import com.ardeno.clearscan.ocr.OcrEngine
import com.ardeno.clearscan.ocr.OcrLanguage
import com.ardeno.clearscan.pdf.PdfCompressQuality
import com.ardeno.clearscan.pdf.PdfToolEngine
import com.ardeno.clearscan.pdf.SearchablePdfWriter
import com.ardeno.clearscan.scanner.FileImportResolver
import com.ardeno.clearscan.scanner.ScannerImport
import com.ardeno.clearscan.update.ApkUpdateManager
import com.ardeno.clearscan.ui.settings.BackupImportReload
import com.ardeno.clearscan.ui.settings.SettingsUiState
import com.ardeno.clearscan.ui.settings.SettingsViewModel
import com.ardeno.clearscan.vault.EncryptedFileStore
import com.ardeno.clearscan.vault.ExportAuditLog
import com.ardeno.clearscan.vault.PrivacyStatusProvider
import com.ardeno.clearscan.vault.VaultCrypto
import com.ardeno.clearscan.vault.VaultKeyMigration
import com.ardeno.clearscan.vault.VaultSettings
import java.io.File
import javax.crypto.Cipher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ClearScanUiState(
    val documents: List<ScanDocument> = emptyList(),
    val folders: List<DocumentFolder> = emptyList(),
    val selectedFolderId: String? = null,
    val showFavoritesOnly: Boolean = false,
    val selectionMode: Boolean = false,
    val selectedDocumentIds: Set<String> = emptySet(),
    val duplicateDocumentIds: Set<String> = emptySet(),
    val isSaving: Boolean = false,
    val isOcrRunning: Boolean = false,
    val isPdfToolRunning: Boolean = false,
    val query: String = "",
    val signatureText: String = "",
    val pdfPassword: String = "",
    val compressQuality: PdfCompressQuality = PdfCompressQuality.Balanced,
    val expandedDocumentId: String? = null,
    val settings: SettingsUiState = SettingsUiState(),
    val message: String? = null,
    val hasCompletedOnboarding: Boolean = false,
    val libraryViewMode: LibraryViewMode = LibraryViewMode.List,
    val isSelfHostUploading: Boolean = false,
    val idRedactionSuggestions: Map<String, IdRedactionSuggestion> = emptyMap()
)

class ClearScanViewModel(application: Application) : AndroidViewModel(application) {
    private val vaultCrypto = VaultCrypto()
    private val encryptedFileStore = EncryptedFileStore(application, vaultCrypto)
    private val repository = LocalDocumentRepository(application, vaultCrypto, encryptedFileStore)
    private val ocrEngine = OcrEngine(application)
    private val pdfToolEngine = PdfToolEngine()
    private val searchablePdfWriter = SearchablePdfWriter()
    private val vaultSettings = VaultSettings(application)
    private val vaultKeyMigration = VaultKeyMigration(application, vaultCrypto, encryptedFileStore, vaultSettings)
    private val exportAuditLog = ExportAuditLog(application)
    private val privacyStatusProvider = PrivacyStatusProvider(application, encryptedFileStore, exportAuditLog, vaultCrypto)
    private val backupRestoreManager = BackupRestoreManager(application, repository, encryptedFileStore, vaultCrypto)
    private val appPreferences = AppPreferences(application)
    private val selfHostExporter = SelfHostExporter(application)
    private val duplicateDetector = DuplicateDetector()
    private val apkUpdateManager = ApkUpdateManager(application)
    private val _uiState = MutableStateFlow(ClearScanUiState())
    private var activeOcrJobs = 0

    private val settingsViewModel = SettingsViewModel(
        application = application,
        scope = viewModelScope,
        repository = repository,
        vaultCrypto = vaultCrypto,
        vaultSettings = vaultSettings,
        vaultKeyMigration = vaultKeyMigration,
        exportAuditLog = exportAuditLog,
        privacyStatusProvider = privacyStatusProvider,
        backupRestoreManager = backupRestoreManager,
        appPreferences = appPreferences,
        selfHostSettings = SelfHostSettings(application),
        apkUpdateManager = apkUpdateManager,
        duplicateDetector = duplicateDetector,
        onMessage = ::reportMessage,
        onBackupImportSuccess = ::applyBackupImport
    )

    val uiState: StateFlow<ClearScanUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { current ->
            current.copy(
                hasCompletedOnboarding = appPreferences.hasCompletedOnboarding,
                libraryViewMode = appPreferences.libraryViewMode
            )
        }

        viewModelScope.launch {
            settingsViewModel.uiState.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }

        settingsViewModel.initializeVaultState()

        loadDocumentsWhenAccessible()
    }

    fun createVaultDecryptCipher(): Cipher = vaultCrypto.createDecryptCipher()

    fun onVaultCryptoUnlocked() {
        settingsViewModel.onVaultCryptoUnlocked()
        loadDocumentsWhenAccessible()
    }

    private fun loadDocumentsWhenAccessible() {
        viewModelScope.launch {
            if (vaultSettings.isEnabled && vaultCrypto.requiresAuthentication()) {
                return@launch
            }
            val documents = repository.loadDocuments()
            val folders = repository.loadFolders()
            val duplicateIds = duplicateDetector.duplicateDocumentIds(documents)
            _uiState.update { current ->
                current.copy(
                    documents = documents,
                    folders = folders,
                    duplicateDocumentIds = duplicateIds
                )
            }
            documents
                .filter { it.ocrStatus == OcrStatus.Queued || it.ocrStatus == OcrStatus.Processing }
                .forEach { document ->
                    runOcr(document)
                }
        }
    }

    override fun onCleared() {
        ocrEngine.close()
        super.onCleared()
    }

    fun completeOnboarding() {
        appPreferences.setOnboardingComplete()
        _uiState.update { it.copy(hasCompletedOnboarding = true) }
    }

    fun setLibraryViewMode(mode: LibraryViewMode) {
        appPreferences.setLibraryViewMode(mode)
        _uiState.update { it.copy(libraryViewMode = mode) }
    }

    fun setAutoPageTurnEnabled(enabled: Boolean) = settingsViewModel.setAutoPageTurnEnabled(enabled)

    fun setImageEnhancementEnabled(enabled: Boolean) = settingsViewModel.setImageEnhancementEnabled(enabled)

    fun setSelectedFolder(folderId: String?) {
        _uiState.update {
            it.copy(
                selectedFolderId = folderId,
                showFavoritesOnly = false
            )
        }
    }

    fun setShowFavoritesOnly(showFavoritesOnly: Boolean) {
        _uiState.update {
            it.copy(
                showFavoritesOnly = showFavoritesOnly,
                selectedFolderId = if (showFavoritesOnly) null else it.selectedFolderId
            )
        }
    }

    fun enterSelectionMode() {
        _uiState.update { it.copy(selectionMode = true, selectedDocumentIds = emptySet()) }
    }

    fun exitSelectionMode() {
        _uiState.update { it.copy(selectionMode = false, selectedDocumentIds = emptySet()) }
    }

    fun toggleDocumentSelection(documentId: String) {
        _uiState.update { current ->
            val nextSelection = if (documentId in current.selectedDocumentIds) {
                current.selectedDocumentIds - documentId
            } else {
                current.selectedDocumentIds + documentId
            }
            current.copy(selectedDocumentIds = nextSelection)
        }
    }

    fun selectAllVisibleDocuments(visibleDocumentIds: List<String>) {
        _uiState.update { it.copy(selectedDocumentIds = visibleDocumentIds.toSet()) }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            runCatching {
                repository.createFolder(name)
            }.onSuccess { folder ->
                _uiState.update { current ->
                    current.copy(
                        folders = listOf(folder) + current.folders,
                        message = "Created folder \"${folder.name}\"."
                    )
                }
            }.onFailure { error ->
                reportMessage(error.localizedMessage ?: "Could not create folder.")
            }
        }
    }

    fun renameFolder(folderId: String, name: String) {
        viewModelScope.launch {
            runCatching {
                repository.renameFolder(folderId, name)
            }.onSuccess { folder ->
                if (folder != null) {
                    _uiState.update { current ->
                        current.copy(
                            folders = current.folders.map { existing ->
                                if (existing.id == folder.id) folder else existing
                            },
                            message = "Renamed folder to \"${folder.name}\"."
                        )
                    }
                }
            }.onFailure { error ->
                reportMessage(error.localizedMessage ?: "Could not rename folder.")
            }
        }
    }

    fun deleteFolder(folderId: String) {
        viewModelScope.launch {
            val deleted = repository.deleteFolder(folderId)
            if (deleted) {
                _uiState.update { current ->
                    current.copy(
                        folders = current.folders.filterNot { it.id == folderId },
                        documents = current.documents.map { document ->
                            if (document.folderId == folderId) {
                                document.copy(folderId = null)
                            } else {
                                document
                            }
                        },
                        selectedFolderId = current.selectedFolderId.takeUnless { it == folderId },
                        message = "Folder deleted."
                    )
                }
            }
        }
    }

    fun updateDocumentTags(document: ScanDocument, tags: List<String>) {
        viewModelScope.launch {
            repository.updateDocumentTags(document.id, tags)?.let { updated ->
                replaceDocument(updated)
                _uiState.update { it.copy(message = "Tags updated.") }
            }
        }
    }

    fun toggleDocumentFavorite(document: ScanDocument) {
        viewModelScope.launch {
            repository.setDocumentFavorite(document.id, !document.isFavorite)?.let { updated ->
                replaceDocument(updated)
                val label = if (updated.isFavorite) "Added to favorites." else "Removed from favorites."
                _uiState.update { it.copy(message = label) }
            }
        }
    }

    fun moveDocumentToFolder(document: ScanDocument, folderId: String?) {
        viewModelScope.launch {
            repository.moveDocumentToFolder(document.id, folderId)?.let { updated ->
                replaceDocument(updated)
                val folderName = folderId?.let { id ->
                    _uiState.value.folders.find { it.id == id }?.name
                }
                val message = when (folderName) {
                    null -> "Moved to library."
                    else -> "Moved to \"$folderName\"."
                }
                _uiState.update { it.copy(message = message) }
            }
        }
    }

    fun deleteSelectedDocuments() {
        val selectedIds = _uiState.value.selectedDocumentIds
        if (selectedIds.isEmpty()) {
            reportMessage("Select at least one document.")
            return
        }
        viewModelScope.launch {
            val deletedCount = repository.deleteDocuments(selectedIds)
            refreshDocumentsAfterDeletion(deletedIds = selectedIds, deletedCount = deletedCount)
        }
    }

    fun mergeSelectedDocuments() {
        val selectedIds = _uiState.value.selectedDocumentIds
        val selectedDocuments = _uiState.value.documents.filter { it.id in selectedIds }
        if (selectedDocuments.size < 2) {
            reportMessage("Select at least two documents to merge.")
            return
        }
        runPdfTool(
            successMessage = "Merged ${selectedDocuments.size} selected scans.",
            sourceDocuments = selectedDocuments
        ) { workingDir ->
            listOf(
                repository.createGeneratedDocument(
                    output = pdfToolEngine.merge(selectedDocuments, workingDir),
                    sourceDocuments = selectedDocuments
                )
            )
        }
        exitSelectionMode()
    }

    fun exportPathsForSelectedDocuments(): List<Pair<String, String>> {
        val selectedIds = _uiState.value.selectedDocumentIds
        return _uiState.value.documents
            .filter { it.id in selectedIds }
            .mapNotNull { document ->
                exportPathFor(document)?.let { path ->
                    path to exportMimeTypeFor(document)
                }
            }
    }

    fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun updateSignatureText(signatureText: String) {
        _uiState.update { it.copy(signatureText = signatureText) }
    }

    fun updatePdfPassword(pdfPassword: String) {
        _uiState.update { it.copy(pdfPassword = pdfPassword) }
    }

    fun updateCompressQuality(quality: PdfCompressQuality) {
        _uiState.update { it.copy(compressQuality = quality) }
    }

    fun toggleDocumentExpanded(document: ScanDocument) {
        _uiState.update { current ->
            current.copy(
                expandedDocumentId = if (current.expandedDocumentId == document.id) null else document.id
            )
        }
    }

    fun deleteDocument(document: ScanDocument) {
        viewModelScope.launch {
            val deleted = repository.deleteDocument(document.id)
            if (deleted) {
                refreshDocumentsAfterDeletion(deletedIds = setOf(document.id), deletedCount = 1)
            }
        }
    }

    fun setDefaultOcrLanguage(language: OcrLanguage) = settingsViewModel.setDefaultOcrLanguage(language)

    fun setPassphraseBackupEnabled(enabled: Boolean) = settingsViewModel.setPassphraseBackupEnabled(enabled)

    fun setWifiOnlySelfHostUpload(enabled: Boolean) = settingsViewModel.setWifiOnlySelfHostUpload(enabled)

    fun setDocumentOcrLanguage(document: ScanDocument, language: OcrLanguage) {
        if (document.ocrLanguage == language) return

        viewModelScope.launch {
            val updated = repository.updateOcrLanguage(document.id, language)
            if (updated != null) {
                replaceDocument(updated)
                runOcr(updated.copy(ocrStatus = OcrStatus.Queued))
            }
        }
    }

    fun retryOcr(document: ScanDocument) {
        runOcr(document.copy(ocrStatus = OcrStatus.Queued))
    }

    fun setVaultEnabled(enabled: Boolean) = settingsViewModel.setVaultEnabled(enabled)

    fun unlockVault() = settingsViewModel.unlockVault()

    fun lockVault() = settingsViewModel.lockVault()

    fun mergeAllDocuments() {
        val documents = _uiState.value.documents
        if (documents.size < 2) {
            reportMessage("Add at least two scans before merging.")
            return
        }
        runPdfTool(
            successMessage = "Merged ${documents.size} scans.",
            sourceDocuments = documents
        ) { workingDir ->
            listOf(
                repository.createGeneratedDocument(
                    output = pdfToolEngine.merge(documents, workingDir),
                    sourceDocuments = documents
                )
            )
        }
    }

    fun splitDocument(document: ScanDocument) {
        runPdfTool(
            successMessage = "Split ${document.title} into single-page PDFs.",
            sourceDocuments = listOf(document)
        ) { workingDir ->
            pdfToolEngine.split(document, workingDir).map { output ->
                repository.createGeneratedDocument(output, listOf(document))
            }
        }
    }

    fun rotateDocument(document: ScanDocument) {
        runSingleDocumentTool(
            document = document,
            successMessage = "Created rotated copy.",
            output = { workingDir -> pdfToolEngine.rotateClockwise(document, workingDir) }
        )
    }

    fun signDocument(document: ScanDocument) {
        val signature = _uiState.value.signatureText.ifBlank { "ClearScan" }
        runSingleDocumentTool(
            document = document,
            successMessage = "Created signed copy.",
            output = { workingDir -> pdfToolEngine.sign(document, signature, workingDir) }
        )
    }

    fun redactDocument(document: ScanDocument) {
        runSingleDocumentTool(
            document = document,
            successMessage = "Created redacted copy.",
            output = { workingDir -> pdfToolEngine.redactHeader(document, workingDir) }
        )
    }

    fun redactIdSensitiveFields(document: ScanDocument) {
        val suggestion = _uiState.value.idRedactionSuggestions[document.id]
            ?: IdRedactionSuggester.suggestFromText(document.ocrText)
        if (suggestion == null) {
            reportMessage("No sensitive ID fields were detected to redact.")
            return
        }
        runSingleDocumentTool(
            document = document,
            successMessage = "Created ID-redacted copy.",
            output = { workingDir ->
                pdfToolEngine.redactIdSensitiveFields(document, suggestion.regions, workingDir)
            }
        )
    }

    fun updateSelfHostConfig(config: com.ardeno.clearscan.data.SelfHostConfig) =
        settingsViewModel.updateSelfHostConfig(config)

    fun saveSelfHostConfig() = settingsViewModel.saveSelfHostConfig()

    fun uploadToSelfHost(document: ScanDocument) {
        val settings = _uiState.value.settings
        val config = settings.selfHostConfig
        if (!config.enabled) {
            reportMessage("Enable self-host export in Settings first.")
            return
        }
        if (!config.isConfigured) {
            reportMessage("Add your self-host endpoint and credentials in Settings.")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSelfHostUploading = true, message = null) }
            runCatching {
                val exportPath = exportPathFor(document)
                    ?: error("No export file is available for this scan.")
                val exportFile = File(exportPath)
                require(exportFile.exists()) { "The export file is missing." }
                selfHostExporter.export(
                    document,
                    exportFile,
                    config,
                    wifiOnly = settings.wifiOnlySelfHostUpload
                )
            }.onSuccess {
                logDocumentExport(document, exportKind = "self-host")
                _uiState.update { current ->
                    current.copy(
                        isSelfHostUploading = false,
                        message = "Uploaded ${document.title} to your self-host target."
                    )
                }
            }.onFailure { error ->
                _uiState.update { current ->
                    current.copy(
                        isSelfHostUploading = false,
                        message = error.localizedMessage ?: "Self-host upload failed."
                    )
                }
            }
        }
    }

    fun applyAnnotations(
        document: ScanDocument,
        annotationsByPage: Map<Int, List<PageAnnotation>>
    ) {
        viewModelScope.launch {
            val pageAnnotations = List(document.pageCount) { pageIndex ->
                annotationsByPage[pageIndex].orEmpty()
            }
            val updatedSource = repository.updatePageAnnotations(document.id, pageAnnotations)
            if (updatedSource != null) {
                replaceDocument(updatedSource)
            }
            runSingleDocumentTool(
                document = updatedSource ?: document,
                successMessage = "Created annotated copy.",
                output = { workingDir ->
                    pdfToolEngine.applyAnnotations(
                        updatedSource ?: document,
                        annotationsByPage,
                        workingDir
                    )
                }
            )
        }
    }

    fun passwordProtectDocument(document: ScanDocument) {
        val password = _uiState.value.pdfPassword
        runSingleDocumentTool(
            document = document,
            successMessage = "Created password-protected PDF.",
            output = { workingDir -> pdfToolEngine.passwordProtect(document, password, workingDir) }
        )
    }

    fun reorderDocument(document: ScanDocument, pageOrder: List<Int>) {
        runSingleDocumentTool(
            document = document,
            successMessage = "Created reordered copy.",
            output = { workingDir -> pdfToolEngine.reorderPages(document, pageOrder, workingDir) }
        )
    }

    fun deletePagesFromDocument(document: ScanDocument, pageIndicesToKeep: List<Int>) {
        runSingleDocumentTool(
            document = document,
            successMessage = "Created copy with selected pages removed.",
            output = { workingDir -> pdfToolEngine.deletePages(document, pageIndicesToKeep, workingDir) }
        )
    }

    fun compressDocument(document: ScanDocument) {
        val quality = _uiState.value.compressQuality
        runSingleDocumentTool(
            document = document,
            successMessage = "Created compressed copy (${quality.label.lowercase()}).",
            output = { workingDir -> pdfToolEngine.compress(document, quality, workingDir) }
        )
    }

    fun importFiles(uris: List<Uri>) {
        if (uris.isEmpty()) {
            reportMessage("No files were selected.")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, message = null) }

            runCatching {
                val import = FileImportResolver.resolve(getApplication(), uris)
                repository.createDocument(
                    import = import,
                    ocrLanguage = appPreferences.defaultOcrLanguage,
                    titlePrefix = "Import"
                )
            }.onSuccess { document ->
                val duplicateIds = duplicateDetector.duplicateDocumentIds(listOf(document) + _uiState.value.documents)
                _uiState.update { current ->
                    current.copy(
                        documents = listOf(document) + current.documents,
                        duplicateDocumentIds = duplicateIds,
                        isSaving = false,
                        expandedDocumentId = document.id,
                        message = "Imported ${document.pageCount} page${if (document.pageCount == 1) "" else "s"}. OCR is starting."
                    )
                }
                runOcr(document)
            }.onFailure { error ->
                _uiState.update { current ->
                    current.copy(
                        isSaving = false,
                        message = error.localizedMessage ?: "Could not import the selected files."
                    )
                }
            }
        }
    }

    fun runSinhalaTamilBenchmarkSelfCheck() = settingsViewModel.runSinhalaTamilBenchmarkSelfCheck()

    fun refreshPrivacyStatus() = settingsViewModel.refreshPrivacyStatus()

    fun logDocumentExport(document: ScanDocument, exportKind: String = "share") {
        settingsViewModel.logDocumentExport(document.id, document.title, exportKind)
    }

    fun onBackupExportUriSelected(targetUri: Uri) = settingsViewModel.onBackupExportUriSelected(targetUri)

    fun onBackupImportUriSelected(sourceUri: Uri) = settingsViewModel.onBackupImportUriSelected(sourceUri)

    fun submitBackupPassphrase(passphrase: CharArray, confirmation: CharArray?) =
        settingsViewModel.submitBackupPassphrase(passphrase, confirmation)

    fun dismissBackupPassphrase() = settingsViewModel.dismissBackupPassphrase()

    fun exportPathFor(document: ScanDocument): String? =
        document.searchablePdfPath ?: document.pdfPath ?: document.pageImagePaths.firstOrNull()

    fun exportMimeTypeFor(document: ScanDocument): String =
        when {
            document.searchablePdfPath != null || document.pdfPath != null -> "application/pdf"
            else -> "image/jpeg"
        }

    private fun runOcr(document: ScanDocument) {
        viewModelScope.launch {
            incrementOcrJobs()

            repository.markOcrProcessing(document.id)?.let { processingDocument ->
                replaceDocument(processingDocument)
            }

            runCatching {
                val result = ocrEngine.recognize(document, document.ocrLanguage)
                val searchablePdf = searchablePdfWriter.write(
                    document = document,
                    ocrResult = result,
                    targetDir = repository.documentDirectory(document)
                )
                val suggestedTags = DocumentTagger.suggestTags(result.text)
                val receiptFields = ReceiptFieldExtractor.extract(result.text)
                    .takeIf { fields -> fields.hasAnyField }
                val updatedDocument = repository.updateOcrResult(
                    id = document.id,
                    ocrText = result.text,
                    searchablePdfPath = searchablePdf?.absolutePath,
                    status = OcrStatus.Ready,
                    tags = suggestedTags,
                    receiptFields = receiptFields
                )
                OcrSuccess(updatedDocument, result, suggestedTags, receiptFields)
            }.onSuccess { ocrSuccess ->
                ocrSuccess.updatedDocument?.let { replaceDocument(it) }
                val result = ocrSuccess.result
                val suggestedTags = ocrSuccess.suggestedTags
                val receiptFields = ocrSuccess.receiptFields
                val idSuggestion = if (document.scanMode == ScanMode.IdCard || document.tags.contains("id-card")) {
                    IdRedactionSuggester.suggest(result.pages)
                        ?: IdRedactionSuggester.suggestFromText(result.text)
                } else {
                    null
                }
                _uiState.update { current ->
                    val intelligenceNote = buildList {
                        if (suggestedTags.isNotEmpty()) add(suggestedTags.joinToString())
                        receiptFields?.amount?.let { add("amount $it") }
                    }.takeIf { it.isNotEmpty() }?.joinToString(" · ")

                    current.copy(
                        message = when {
                            idSuggestion != null ->
                                "OCR finished for ${document.title}. Sensitive fields detected — review redaction."
                            intelligenceNote != null ->
                                "OCR finished for ${document.title}. Tags: $intelligenceNote"
                            else -> "OCR finished for ${document.title}."
                        },
                        idRedactionSuggestions = if (idSuggestion != null) {
                            current.idRedactionSuggestions + (document.id to idSuggestion)
                        } else {
                            current.idRedactionSuggestions
                        }
                    )
                }
            }.onFailure { error ->
                repository.markOcrFailed(document.id)?.let { failedDocument ->
                    replaceDocument(failedDocument)
                }
                _uiState.update { current ->
                    current.copy(message = error.localizedMessage ?: "OCR failed for ${document.title}.")
                }
            }

            decrementOcrJobs()
        }
    }

    fun saveScan(import: ScannerImport) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, message = null) }

            runCatching {
                repository.createDocument(
                    import = import.copy(enhanceImages = appPreferences.imageEnhancementEnabled),
                    ocrLanguage = appPreferences.defaultOcrLanguage
                )
            }.onSuccess { document ->
                val duplicateIds = duplicateDetector.duplicateDocumentIds(listOf(document) + _uiState.value.documents)
                _uiState.update { current ->
                    current.copy(
                        documents = listOf(document) + current.documents,
                        duplicateDocumentIds = duplicateIds,
                        isSaving = false,
                        expandedDocumentId = document.id,
                        message = when (import.scanMode) {
                            ScanMode.IdCard -> "Saved ${document.pageCount} page ID scan. OCR is starting."
                            ScanMode.Document -> "Saved ${document.pageCount} page scan. OCR is starting."
                        }
                    )
                }
                runOcr(document)
            }.onFailure { error ->
                _uiState.update { current ->
                    current.copy(
                        isSaving = false,
                        message = error.localizedMessage ?: "Could not save this scan."
                    )
                }
            }
        }
    }

    fun savePageTurnCapture(pagePaths: List<String>) {
        if (pagePaths.isEmpty()) {
            reportMessage("No pages were captured.")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, message = null) }

            runCatching {
                repository.createDocumentFromPagePaths(
                    pagePaths = pagePaths,
                    enhanceImages = appPreferences.imageEnhancementEnabled,
                    ocrLanguage = appPreferences.defaultOcrLanguage
                )
            }.onSuccess { document ->
                val duplicateIds = duplicateDetector.duplicateDocumentIds(listOf(document) + _uiState.value.documents)
                _uiState.update { current ->
                    current.copy(
                        documents = listOf(document) + current.documents,
                        duplicateDocumentIds = duplicateIds,
                        isSaving = false,
                        expandedDocumentId = document.id,
                        message = "Saved ${document.pageCount} auto-captured page${if (document.pageCount == 1) "" else "s"}. OCR is starting."
                    )
                }
                runOcr(document)
            }.onFailure { error ->
                _uiState.update { current ->
                    current.copy(
                        isSaving = false,
                        message = error.localizedMessage ?: "Could not save page-turn capture."
                    )
                }
            }
        }
    }

    fun reportMessage(message: String) {
        _uiState.update { it.copy(message = message) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun checkForAppUpdate() = settingsViewModel.checkForAppUpdate()

    fun dismissAppUpdate() = settingsViewModel.dismissAppUpdate()

    fun downloadPendingAppUpdate() = settingsViewModel.downloadPendingAppUpdate()

    private fun applyBackupImport(reload: BackupImportReload) {
        _uiState.update { current ->
            current.copy(
                documents = reload.documents,
                folders = reload.folders,
                duplicateDocumentIds = reload.duplicateDocumentIds,
                expandedDocumentId = null
            )
        }
    }

    private fun runSingleDocumentTool(
        document: ScanDocument,
        successMessage: String,
        output: suspend (java.io.File) -> com.ardeno.clearscan.pdf.PdfToolOutput
    ) {
        runPdfTool(
            successMessage = successMessage,
            sourceDocuments = listOf(document)
        ) { workingDir ->
            listOf(repository.createGeneratedDocument(output(workingDir), listOf(document)))
        }
    }

    private fun runPdfTool(
        successMessage: String,
        sourceDocuments: List<ScanDocument>,
        block: suspend (java.io.File) -> List<ScanDocument>
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPdfToolRunning = true, message = null) }
            val workingDir = repository.newWorkingDirectory("pdf-tool")
            runCatching {
                block(workingDir)
            }.onSuccess { generatedDocuments ->
                workingDir.deleteRecursively()
                val nextDocuments = generatedDocuments + _uiState.value.documents
                val duplicateIds = duplicateDetector.duplicateDocumentIds(nextDocuments)
                _uiState.update { current ->
                    current.copy(
                        documents = nextDocuments,
                        duplicateDocumentIds = duplicateIds,
                        expandedDocumentId = generatedDocuments.firstOrNull()?.id ?: current.expandedDocumentId,
                        isPdfToolRunning = false,
                        message = successMessage
                    )
                }
            }.onFailure { error ->
                workingDir.deleteRecursively()
                _uiState.update { current ->
                    current.copy(
                        isPdfToolRunning = false,
                        message = error.localizedMessage ?: "PDF tool failed."
                    )
                }
            }
        }
    }

    private fun replaceDocument(document: ScanDocument) {
        _uiState.update { current ->
            val nextDocuments = current.documents.map { existing ->
                if (existing.id == document.id) document else existing
            }
            current.copy(
                documents = nextDocuments,
                duplicateDocumentIds = duplicateDetector.duplicateDocumentIds(nextDocuments)
            )
        }
    }

    private fun refreshDocumentsAfterDeletion(deletedIds: Set<String>, deletedCount: Int) {
        _uiState.update { current ->
            val nextDocuments = current.documents.filterNot { it.id in deletedIds }
            current.copy(
                documents = nextDocuments,
                duplicateDocumentIds = duplicateDetector.duplicateDocumentIds(nextDocuments),
                expandedDocumentId = current.expandedDocumentId.takeUnless { it in deletedIds },
                selectedDocumentIds = current.selectedDocumentIds - deletedIds,
                selectionMode = current.selectionMode && (current.selectedDocumentIds - deletedIds).isNotEmpty(),
                message = if (deletedCount == 1) {
                    "Deleted 1 document."
                } else {
                    "Deleted $deletedCount documents."
                }
            )
        }
    }

    private fun incrementOcrJobs() {
        activeOcrJobs += 1
        _uiState.update { it.copy(isOcrRunning = true) }
    }

    private fun decrementOcrJobs() {
        activeOcrJobs = (activeOcrJobs - 1).coerceAtLeast(0)
        _uiState.update { it.copy(isOcrRunning = activeOcrJobs > 0) }
    }
}

private data class OcrSuccess(
    val updatedDocument: ScanDocument?,
    val result: DocumentOcrResult,
    val suggestedTags: List<String>,
    val receiptFields: ReceiptFields?
)
