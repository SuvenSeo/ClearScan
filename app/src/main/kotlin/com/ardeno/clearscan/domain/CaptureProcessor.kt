package com.ardeno.clearscan.domain

import android.app.Application
import android.net.Uri
import com.ardeno.clearscan.R
import com.ardeno.clearscan.data.AppPreferences
import com.ardeno.clearscan.data.LocalDocumentRepository
import com.ardeno.clearscan.model.ScanDocument
import com.ardeno.clearscan.model.ScanMode
import com.ardeno.clearscan.scanner.FileImportResolver
import com.ardeno.clearscan.scanner.ScannerImport
import com.ardeno.clearscan.ui.UiStrings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class CaptureProcessor(
    private val application: Application,
    private val scope: CoroutineScope,
    private val repository: LocalDocumentRepository,
    private val appPreferences: AppPreferences,
    private val uiStrings: UiStrings,
    private val onSavingChanged: (Boolean) -> Unit,
    private val onDocumentCaptured: (ScanDocument, String) -> Unit,
    private val onCaptureFailed: (String) -> Unit,
    private val runOcr: (ScanDocument) -> Unit
) {
    fun importFiles(uris: List<Uri>) {
        if (uris.isEmpty()) {
            onCaptureFailed(uiStrings.noFilesSelected())
            return
        }
        scope.launch {
            onSavingChanged(true)
            runCatching {
                repository.createDocument(
                    import = FileImportResolver.resolve(application, uris).copy(
                        enhanceImages = appPreferences.imageEnhancementEnabled
                    ),
                    ocrLanguage = appPreferences.defaultOcrLanguage,
                    titlePrefix = application.getString(R.string.document_title_import),
                    colorFilter = appPreferences.scanColorFilter
                )
            }.onSuccess { document ->
                onDocumentCaptured(document, uiStrings.importedPages(document.pageCount))
                runOcr(document)
            }.onFailure { error ->
                onSavingChanged(false)
                onCaptureFailed(error.localizedMessage ?: uiStrings.importFailed())
            }
        }
    }

    fun saveScan(import: ScannerImport) {
        scope.launch {
            onSavingChanged(true)
            runCatching {
                repository.createDocument(
                    import = import.copy(enhanceImages = appPreferences.imageEnhancementEnabled),
                    ocrLanguage = appPreferences.defaultOcrLanguage,
                    colorFilter = appPreferences.scanColorFilter
                )
            }.onSuccess { document ->
                val message = when (import.scanMode) {
                    ScanMode.IdCard -> uiStrings.savedIdScan(document.pageCount)
                    ScanMode.Document -> uiStrings.savedDocumentScan(document.pageCount)
                }
                onDocumentCaptured(document, message)
                runOcr(document)
            }.onFailure { error ->
                onSavingChanged(false)
                onCaptureFailed(error.localizedMessage ?: uiStrings.saveScanFailed())
            }
        }
    }

    fun savePageTurnCapture(pagePaths: List<String>) {
        if (pagePaths.isEmpty()) {
            onCaptureFailed(uiStrings.noPagesCaptured())
            return
        }
        scope.launch {
            onSavingChanged(true)
            runCatching {
                repository.createDocumentFromPagePaths(
                    pagePaths = pagePaths,
                    enhanceImages = appPreferences.imageEnhancementEnabled,
                    ocrLanguage = appPreferences.defaultOcrLanguage,
                    colorFilter = appPreferences.scanColorFilter
                )
            }.onSuccess { document ->
                onDocumentCaptured(document, uiStrings.savedAutoCapture(document.pageCount))
                runOcr(document)
            }.onFailure { error ->
                onSavingChanged(false)
                onCaptureFailed(error.localizedMessage ?: uiStrings.pageTurnSaveFailed())
            }
        }
    }
}
