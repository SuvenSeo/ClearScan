package com.ardeno.clearscan

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ardeno.clearscan.backup.BackupRestoreManager
import com.ardeno.clearscan.data.AppPreferences
import com.ardeno.clearscan.data.LocalDocumentRepository
import com.ardeno.clearscan.data.SelfHostSettings
import com.ardeno.clearscan.domain.CaptureProcessor
import com.ardeno.clearscan.domain.DocumentActionsHandler
import com.ardeno.clearscan.domain.OcrProcessor
import com.ardeno.clearscan.domain.PdfToolsProcessor
import com.ardeno.clearscan.duplicate.DuplicateDetector
import com.ardeno.clearscan.export.SelfHostExporter
import com.ardeno.clearscan.model.LibraryViewMode
import com.ardeno.clearscan.model.OcrStatus
import com.ardeno.clearscan.model.PageAnnotation
import com.ardeno.clearscan.model.ScanDocument
import com.ardeno.clearscan.ocr.OcrEngine
import com.ardeno.clearscan.ocr.OcrLanguage
import com.ardeno.clearscan.pdf.PdfCompressQuality
import com.ardeno.clearscan.pdf.PdfToolEngine
import com.ardeno.clearscan.pdf.SearchablePdfWriter
import com.ardeno.clearscan.scanner.ScannerImport
import com.ardeno.clearscan.ui.library.LibraryViewModel
import com.ardeno.clearscan.ui.settings.SettingsViewModel
import com.ardeno.clearscan.ui.UiStrings
import com.ardeno.clearscan.update.ApkUpdateManager
import com.ardeno.clearscan.vault.EncryptedFileStore
import com.ardeno.clearscan.vault.ExportAuditLog
import com.ardeno.clearscan.vault.PrivacyStatusProvider
import com.ardeno.clearscan.vault.VaultCrypto
import com.ardeno.clearscan.vault.VaultKeyMigration
import com.ardeno.clearscan.vault.VaultSettings
import javax.crypto.Cipher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    private val uiStrings = UiStrings(application)
    private val backupRestoreManager = BackupRestoreManager(application, repository, encryptedFileStore, vaultCrypto, uiStrings)
    private val appPreferences = AppPreferences(application)
    private val selfHostExporter = SelfHostExporter(application)
    private val duplicateDetector = DuplicateDetector()
    private val apkUpdateManager = ApkUpdateManager(application)
    private val _uiState = MutableStateFlow(ClearScanUiState())

    private val libraryViewModel = LibraryViewModel(
        scope = viewModelScope,
        repository = repository,
        duplicateDetector = duplicateDetector,
        uiStrings = uiStrings,
        onMessage = ::reportMessage
    )

    private val ocrProcessor = OcrProcessor(
        context = application,
        scope = viewModelScope,
        repository = repository,
        ocrEngine = ocrEngine,
        searchablePdfWriter = searchablePdfWriter,
        uiStrings = uiStrings,
        onReplaceDocument = libraryViewModel::replaceDocument,
        onOcrRunningChanged = { running -> _uiState.update { it.copy(isOcrRunning = running) } },
        onMessage = ::reportMessage,
        onIdRedactionSuggestion = { docId, suggestion ->
            _uiState.update { it.copy(idRedactionSuggestions = it.idRedactionSuggestions + (docId to suggestion)) }
        }
    )

    private val pdfToolsProcessor = PdfToolsProcessor(
        context = application,
        scope = viewModelScope,
        repository = repository,
        pdfToolEngine = pdfToolEngine,
        uiStrings = uiStrings,
        getDocuments = { libraryViewModel.uiState.value.documents },
        getSelectedIds = { libraryViewModel.uiState.value.selectedDocumentIds },
        getIdRedactionSuggestions = { _uiState.value.idRedactionSuggestions },
        onPdfToolRunningChanged = { running ->
            _uiState.update { it.copy(isPdfToolRunning = running, message = if (running) null else it.message) }
        },
        onDocumentsUpdated = { documents, expandedId, message ->
            libraryViewModel.setDocuments(documents, expandedId)
            reportMessage(message)
        },
        onMessage = ::reportMessage,
        onExitSelectionMode = libraryViewModel::exitSelectionMode
    )

    private val captureProcessor = CaptureProcessor(
        application = application,
        scope = viewModelScope,
        repository = repository,
        appPreferences = appPreferences,
        uiStrings = uiStrings,
        onSavingChanged = { saving ->
            _uiState.update { it.copy(isSaving = saving, message = if (saving) null else it.message) }
        },
        onDocumentCaptured = { document, message ->
            libraryViewModel.addDocuments(listOf(document), document.id)
            _uiState.update { it.copy(isSaving = false) }
            reportMessage(message)
        },
        onCaptureFailed = { message ->
            _uiState.update { it.copy(isSaving = false) }
            reportMessage(message)
        },
        runOcr = ocrProcessor::runOcr
    )

    private val documentActionsHandler = DocumentActionsHandler(
        scope = viewModelScope,
        repository = repository,
        selfHostExporter = selfHostExporter,
        uiStrings = uiStrings,
        getFolders = { libraryViewModel.uiState.value.folders },
        getSelectedIds = { libraryViewModel.uiState.value.selectedDocumentIds },
        getSettings = { _uiState.value.settings },
        onReplaceDocument = libraryViewModel::replaceDocument,
        onRefreshAfterDeletion = libraryViewModel::refreshDocumentsAfterDeletion,
        onMessage = ::reportMessage,
        onSelfHostUploadingChanged = { uploading ->
            _uiState.update { it.copy(isSelfHostUploading = uploading, message = if (uploading) null else it.message) }
        },
        logDocumentExport = { document, exportKind -> logDocumentExport(document, exportKind) }
    )

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
        uiStrings = uiStrings,
        onMessage = ::reportMessage,
        onBackupImportSuccess = libraryViewModel::applyBackupImport
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
            combine(
                libraryViewModel.uiState,
                settingsViewModel.uiState,
                pdfToolsProcessor.uiState
            ) { library, settings, pdfTools -> Triple(library, settings, pdfTools) }
                .collect { (library, settings, pdfTools) ->
                    _uiState.update { current ->
                        current.copy(
                            documents = library.documents,
                            folders = library.folders,
                            selectedFolderId = library.selectedFolderId,
                            showFavoritesOnly = library.showFavoritesOnly,
                            selectionMode = library.selectionMode,
                            selectedDocumentIds = library.selectedDocumentIds,
                            duplicateDocumentIds = library.duplicateDocumentIds,
                            expandedDocumentId = library.expandedDocumentId,
                            query = library.query,
                            settings = settings,
                            signatureText = pdfTools.signatureText,
                            pdfPassword = pdfTools.pdfPassword,
                            compressQuality = pdfTools.compressQuality
                        )
                    }
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
        if (vaultSettings.isEnabled && vaultCrypto.requiresAuthentication()) return
        libraryViewModel.loadInitial { documents ->
            documents
                .filter { it.ocrStatus == OcrStatus.Queued || it.ocrStatus == OcrStatus.Processing }
                .forEach { document -> ocrProcessor.runOcr(document) }
        }
    }

    override fun onCleared() {
        ocrProcessor.close()
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

    fun setSelectedFolder(folderId: String?) = libraryViewModel.setSelectedFolder(folderId)

    fun setShowFavoritesOnly(showFavoritesOnly: Boolean) = libraryViewModel.setShowFavoritesOnly(showFavoritesOnly)

    fun enterSelectionMode() = libraryViewModel.enterSelectionMode()

    fun exitSelectionMode() = libraryViewModel.exitSelectionMode()

    fun toggleDocumentSelection(documentId: String) = libraryViewModel.toggleDocumentSelection(documentId)

    fun selectAllVisibleDocuments(visibleDocumentIds: List<String>) =
        libraryViewModel.selectAllVisibleDocuments(visibleDocumentIds)

    fun createFolder(name: String) = libraryViewModel.createFolder(name)

    fun renameFolder(folderId: String, name: String) = libraryViewModel.renameFolder(folderId, name)

    fun deleteFolder(folderId: String) = libraryViewModel.deleteFolder(folderId)

    fun renameDocument(document: ScanDocument, title: String) = documentActionsHandler.renameDocument(document, title)

    fun updateDocumentTags(document: ScanDocument, tags: List<String>) = documentActionsHandler.updateDocumentTags(document, tags)

    fun toggleDocumentFavorite(document: ScanDocument) = documentActionsHandler.toggleDocumentFavorite(document)

    fun pageImagePathsFor(document: ScanDocument) = documentActionsHandler.pageImagePathsFor(document)

    fun moveDocumentToFolder(document: ScanDocument, folderId: String?) = documentActionsHandler.moveDocumentToFolder(document, folderId)

    fun deleteSelectedDocuments() = documentActionsHandler.deleteSelectedDocuments()

    fun mergeSelectedDocuments() = pdfToolsProcessor.mergeSelectedDocuments()

    fun exportPathsForSelectedDocuments() = libraryViewModel.exportPathsForSelectedDocuments(
        documentActionsHandler::exportPathFor,
        documentActionsHandler::exportMimeTypeFor
    )

    fun updateQuery(query: String) = libraryViewModel.updateQuery(query)

    fun updateSignatureText(signatureText: String) = pdfToolsProcessor.updateSignatureText(signatureText)

    fun updatePdfPassword(pdfPassword: String) = pdfToolsProcessor.updatePdfPassword(pdfPassword)

    fun updateCompressQuality(quality: PdfCompressQuality) = pdfToolsProcessor.updateCompressQuality(quality)

    fun toggleDocumentExpanded(document: ScanDocument) = libraryViewModel.toggleDocumentExpanded(document)

    fun deleteDocument(document: ScanDocument) = documentActionsHandler.deleteDocument(document)

    fun setDefaultOcrLanguage(language: OcrLanguage) = settingsViewModel.setDefaultOcrLanguage(language)

    fun setPassphraseBackupEnabled(enabled: Boolean) = settingsViewModel.setPassphraseBackupEnabled(enabled)

    fun setWifiOnlySelfHostUpload(enabled: Boolean) = settingsViewModel.setWifiOnlySelfHostUpload(enabled)

    fun setDocumentOcrLanguage(document: ScanDocument, language: OcrLanguage) =
        ocrProcessor.setDocumentOcrLanguage(document, language)

    fun retryOcr(document: ScanDocument) =
        ocrProcessor.runOcr(document.copy(ocrStatus = OcrStatus.Queued))

    fun setVaultEnabled(enabled: Boolean) = settingsViewModel.setVaultEnabled(enabled)

    fun unlockVault() = settingsViewModel.unlockVault()

    fun lockVault(announce: Boolean = true) = settingsViewModel.lockVault(announce)

    fun reportVaultAuthError() = settingsViewModel.reportVaultAuthError()

    fun clearVaultAuthError() = settingsViewModel.clearVaultAuthError()

    fun mergeAllDocuments() = pdfToolsProcessor.mergeAllDocuments()

    fun splitDocument(document: ScanDocument) = pdfToolsProcessor.splitDocument(document)

    fun rotateDocument(document: ScanDocument) = pdfToolsProcessor.rotateDocument(document)

    fun signDocument(document: ScanDocument) = pdfToolsProcessor.signDocument(document)

    fun redactDocument(document: ScanDocument) = pdfToolsProcessor.redactDocument(document)

    fun redactIdSensitiveFields(document: ScanDocument) = pdfToolsProcessor.redactIdSensitiveFields(document)

    fun updateSelfHostConfig(config: com.ardeno.clearscan.data.SelfHostConfig) =
        settingsViewModel.updateSelfHostConfig(config)

    fun saveSelfHostConfig() = settingsViewModel.saveSelfHostConfig()

    fun uploadToSelfHost(document: ScanDocument) = documentActionsHandler.uploadToSelfHost(document)

    fun applyAnnotations(document: ScanDocument, annotationsByPage: Map<Int, List<PageAnnotation>>) =
        pdfToolsProcessor.applyAnnotations(document, annotationsByPage, libraryViewModel::replaceDocument)

    fun passwordProtectDocument(document: ScanDocument) = pdfToolsProcessor.passwordProtectDocument(document)

    fun reorderDocument(document: ScanDocument, pageOrder: List<Int>) =
        pdfToolsProcessor.reorderDocument(document, pageOrder)

    fun deletePagesFromDocument(document: ScanDocument, pageIndicesToKeep: List<Int>) =
        pdfToolsProcessor.deletePagesFromDocument(document, pageIndicesToKeep)

    fun compressDocument(document: ScanDocument) = pdfToolsProcessor.compressDocument(document)

    fun importFiles(uris: List<Uri>) = captureProcessor.importFiles(uris)

    fun runSinhalaTamilBenchmarkSelfCheck() = settingsViewModel.runSinhalaTamilBenchmarkSelfCheck()

    fun refreshPrivacyStatus() = settingsViewModel.refreshPrivacyStatus()

    fun logDocumentExport(document: ScanDocument, exportKind: String = "share") =
        settingsViewModel.logDocumentExport(document.id, document.title, exportKind)

    fun onBackupExportUriSelected(targetUri: Uri) = settingsViewModel.onBackupExportUriSelected(targetUri)

    fun onBackupImportUriSelected(sourceUri: Uri) = settingsViewModel.onBackupImportUriSelected(sourceUri)

    fun submitBackupPassphrase(passphrase: CharArray, confirmation: CharArray?) =
        settingsViewModel.submitBackupPassphrase(passphrase, confirmation)

    fun dismissBackupPassphrase() = settingsViewModel.dismissBackupPassphrase()

    fun exportPathFor(document: ScanDocument): String? = documentActionsHandler.exportPathFor(document)

    fun exportMimeTypeFor(document: ScanDocument): String = documentActionsHandler.exportMimeTypeFor(document)

    fun saveScan(import: ScannerImport) = captureProcessor.saveScan(import)

    fun savePageTurnCapture(pagePaths: List<String>) = captureProcessor.savePageTurnCapture(pagePaths)

    fun reportMessage(message: String) = _uiState.update { it.copy(message = message) }

    fun clearMessage() = _uiState.update { it.copy(message = null) }

    fun checkForAppUpdate() = settingsViewModel.checkForAppUpdate()

    fun dismissAppUpdate() = settingsViewModel.dismissAppUpdate()

    fun downloadPendingAppUpdate() = settingsViewModel.downloadPendingAppUpdate()
}
