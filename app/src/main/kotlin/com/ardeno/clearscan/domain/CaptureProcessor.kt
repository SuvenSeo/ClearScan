package com.ardeno.clearscan.domain

import android.app.Application
import android.net.Uri
import com.ardeno.clearscan.data.AppPreferences
import com.ardeno.clearscan.data.LocalDocumentRepository
import com.ardeno.clearscan.model.ScanDocument
import com.ardeno.clearscan.model.ScanMode
import com.ardeno.clearscan.scanner.FileImportResolver
import com.ardeno.clearscan.scanner.ScannerImport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class CaptureProcessor(
    private val application: Application,
    private val scope: CoroutineScope,
    private val repository: LocalDocumentRepository,
    private val appPreferences: AppPreferences,
    private val onSavingChanged: (Boolean) -> Unit,
    private val onDocumentCaptured: (ScanDocument, String) -> Unit,
    private val onCaptureFailed: (String) -> Unit,
    private val runOcr: (ScanDocument) -> Unit
) {
    fun importFiles(uris: List<Uri>) {
        if (uris.isEmpty()) {
            onCaptureFailed("No files were selected.")
            return
        }
        scope.launch {
            onSavingChanged(true)
            runCatching {
                repository.createDocument(
                    import = FileImportResolver.resolve(application, uris),
                    ocrLanguage = appPreferences.defaultOcrLanguage,
                    titlePrefix = "Import"
                )
            }.onSuccess { document ->
                onDocumentCaptured(document, "Imported ${document.pageCount} page${if (document.pageCount == 1) "" else "s"}. OCR is starting.")
                runOcr(document)
            }.onFailure { error ->
                onSavingChanged(false)
                onCaptureFailed(error.localizedMessage ?: "Could not import the selected files.")
            }
        }
    }

    fun saveScan(import: ScannerImport) {
        scope.launch {
            onSavingChanged(true)
            runCatching {
                repository.createDocument(
                    import = import.copy(enhanceImages = appPreferences.imageEnhancementEnabled),
                    ocrLanguage = appPreferences.defaultOcrLanguage
                )
            }.onSuccess { document ->
                val message = when (import.scanMode) {
                    ScanMode.IdCard -> "Saved ${document.pageCount} page ID scan. OCR is starting."
                    ScanMode.Document -> "Saved ${document.pageCount} page scan. OCR is starting."
                }
                onDocumentCaptured(document, message)
                runOcr(document)
            }.onFailure { error ->
                onSavingChanged(false)
                onCaptureFailed(error.localizedMessage ?: "Could not save this scan.")
            }
        }
    }

    fun savePageTurnCapture(pagePaths: List<String>) {
        if (pagePaths.isEmpty()) {
            onCaptureFailed("No pages were captured.")
            return
        }
        scope.launch {
            onSavingChanged(true)
            runCatching {
                repository.createDocumentFromPagePaths(
                    pagePaths = pagePaths,
                    enhanceImages = appPreferences.imageEnhancementEnabled,
                    ocrLanguage = appPreferences.defaultOcrLanguage
                )
            }.onSuccess { document ->
                onDocumentCaptured(document, "Saved ${document.pageCount} auto-captured page${if (document.pageCount == 1) "" else "s"}. OCR is starting.")
                runOcr(document)
            }.onFailure { error ->
                onSavingChanged(false)
                onCaptureFailed(error.localizedMessage ?: "Could not save page-turn capture.")
            }
        }
    }
}
