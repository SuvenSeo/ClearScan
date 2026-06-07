package com.ardeno.clearscan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ardeno.clearscan.data.LocalDocumentRepository
import com.ardeno.clearscan.model.OcrStatus
import com.ardeno.clearscan.model.ScanDocument
import com.ardeno.clearscan.ocr.OcrEngine
import com.ardeno.clearscan.pdf.SearchablePdfWriter
import com.ardeno.clearscan.scanner.ScannerImport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ClearScanUiState(
    val documents: List<ScanDocument> = emptyList(),
    val isSaving: Boolean = false,
    val isOcrRunning: Boolean = false,
    val query: String = "",
    val expandedDocumentId: String? = null,
    val message: String? = null
)

class ClearScanViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LocalDocumentRepository(application)
    private val ocrEngine = OcrEngine(application)
    private val searchablePdfWriter = SearchablePdfWriter()
    private val _uiState = MutableStateFlow(ClearScanUiState())
    private var activeOcrJobs = 0

    val uiState: StateFlow<ClearScanUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val documents = repository.loadDocuments()
            _uiState.update { current ->
                current.copy(documents = documents)
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

    fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query) }
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
                _uiState.update { current ->
                    current.copy(
                        documents = current.documents.filterNot { it.id == document.id },
                        expandedDocumentId = current.expandedDocumentId.takeUnless { it == document.id },
                        message = "Deleted ${document.title}."
                    )
                }
            }
        }
    }

    fun retryOcr(document: ScanDocument) {
        runOcr(document.copy(ocrStatus = OcrStatus.Queued))
    }

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
                val result = ocrEngine.recognize(document)
                val searchablePdf = searchablePdfWriter.write(
                    document = document,
                    ocrResult = result,
                    targetDir = repository.documentDirectory(document)
                )
                repository.updateOcrResult(
                    id = document.id,
                    ocrText = result.text,
                    searchablePdfPath = searchablePdf?.absolutePath,
                    status = OcrStatus.Ready
                )
            }.onSuccess { updatedDocument ->
                updatedDocument?.let { replaceDocument(it) }
                _uiState.update { current ->
                    current.copy(message = "OCR finished for ${document.title}.")
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
                repository.createDocument(import)
            }.onSuccess { document ->
                _uiState.update { current ->
                    current.copy(
                        documents = listOf(document) + current.documents,
                        isSaving = false,
                        expandedDocumentId = document.id,
                        message = "Saved ${document.pageCount} page scan. OCR is starting."
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

    fun reportMessage(message: String) {
        _uiState.update { it.copy(message = message) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun replaceDocument(document: ScanDocument) {
        _uiState.update { current ->
            current.copy(
                documents = current.documents.map { existing ->
                    if (existing.id == document.id) document else existing
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
