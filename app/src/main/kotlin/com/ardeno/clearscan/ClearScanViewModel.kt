package com.ardeno.clearscan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ardeno.clearscan.data.AppPreferences
import com.ardeno.clearscan.data.LocalDocumentRepository
import com.ardeno.clearscan.model.LibraryViewMode
import com.ardeno.clearscan.model.OcrStatus
import com.ardeno.clearscan.model.ScanDocument
import com.ardeno.clearscan.ocr.BenchmarkLanguage
import com.ardeno.clearscan.ocr.OcrBenchmark
import com.ardeno.clearscan.ocr.OcrBenchmarkCase
import com.ardeno.clearscan.ocr.OcrEngine
import com.ardeno.clearscan.pdf.PdfToolEngine
import com.ardeno.clearscan.pdf.SearchablePdfWriter
import com.ardeno.clearscan.scanner.ScannerImport
import com.ardeno.clearscan.vault.VaultCrypto
import com.ardeno.clearscan.vault.VaultSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ClearScanUiState(
    val documents: List<ScanDocument> = emptyList(),
    val isSaving: Boolean = false,
    val isOcrRunning: Boolean = false,
    val isPdfToolRunning: Boolean = false,
    val query: String = "",
    val signatureText: String = "",
    val pdfPassword: String = "",
    val expandedDocumentId: String? = null,
    val vaultEnabled: Boolean = false,
    val vaultUnlocked: Boolean = true,
    val benchmarkSummary: String? = null,
    val message: String? = null,
    val hasCompletedOnboarding: Boolean = false,
    val libraryViewMode: LibraryViewMode = LibraryViewMode.List
)

class ClearScanViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LocalDocumentRepository(application)
    private val ocrEngine = OcrEngine(application)
    private val pdfToolEngine = PdfToolEngine()
    private val searchablePdfWriter = SearchablePdfWriter()
    private val vaultSettings = VaultSettings(application)
    private val vaultCrypto = VaultCrypto()
    private val appPreferences = AppPreferences(application)
    private val _uiState = MutableStateFlow(ClearScanUiState())
    private var activeOcrJobs = 0

    val uiState: StateFlow<ClearScanUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { current ->
            current.copy(
                hasCompletedOnboarding = appPreferences.hasCompletedOnboarding,
                libraryViewMode = appPreferences.libraryViewMode
            )
        }

        viewModelScope.launch {
            val documents = repository.loadDocuments()
            _uiState.update { current ->
                val vaultEnabled = vaultSettings.isEnabled
                current.copy(
                    documents = documents,
                    vaultEnabled = vaultEnabled,
                    vaultUnlocked = !vaultEnabled
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

    fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun updateSignatureText(signatureText: String) {
        _uiState.update { it.copy(signatureText = signatureText) }
    }

    fun updatePdfPassword(pdfPassword: String) {
        _uiState.update { it.copy(pdfPassword = pdfPassword) }
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

    fun setVaultEnabled(enabled: Boolean) {
        viewModelScope.launch {
            runCatching {
                vaultCrypto.ensureVaultKey()
                vaultCrypto.healthCheck()
            }.onSuccess { healthy ->
                vaultSettings.setEnabled(enabled)
                _uiState.update { current ->
                    current.copy(
                        vaultEnabled = enabled,
                        vaultUnlocked = !enabled,
                        message = when {
                            enabled && healthy -> "Vault enabled. Biometric unlock will be required."
                            enabled -> "Vault enabled."
                            else -> "Vault disabled."
                        }
                    )
                }
            }.onFailure { error ->
                _uiState.update { current ->
                    current.copy(message = error.localizedMessage ?: "Vault setup failed.")
                }
            }
        }
    }

    fun unlockVault() {
        _uiState.update { it.copy(vaultUnlocked = true, message = "Vault unlocked.") }
    }

    fun lockVault() {
        _uiState.update { current ->
            if (!current.vaultEnabled) current else current.copy(vaultUnlocked = false, message = "Vault locked.")
        }
    }

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

    fun passwordProtectDocument(document: ScanDocument) {
        val password = _uiState.value.pdfPassword
        runSingleDocumentTool(
            document = document,
            successMessage = "Created password-protected PDF.",
            output = { workingDir -> pdfToolEngine.passwordProtect(document, password, workingDir) }
        )
    }

    fun runSinhalaTamilBenchmarkSelfCheck() {
        val metrics = OcrBenchmark.evaluate(
            listOf(
                OcrBenchmarkCase(
                    language = BenchmarkLanguage.Sinhala,
                    sampleName = "sinhala-self-check",
                    expectedText = "සිංහල ලිපිය",
                    actualText = "සිංහල ලිපිය"
                ),
                OcrBenchmarkCase(
                    language = BenchmarkLanguage.Tamil,
                    sampleName = "tamil-self-check",
                    expectedText = "தமிழ் ஆவணம்",
                    actualText = "தமிழ் ஆவணம்"
                )
            )
        )
        val summary = OcrBenchmark.summary(metrics)
        _uiState.update { current ->
            current.copy(
                benchmarkSummary = summary,
                message = "Sinhala/Tamil OCR benchmark harness is ready."
            )
        }
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
                _uiState.update { current ->
                    current.copy(
                        documents = generatedDocuments + current.documents,
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
