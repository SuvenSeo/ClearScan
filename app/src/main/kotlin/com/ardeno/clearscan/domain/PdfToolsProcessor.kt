package com.ardeno.clearscan.domain

import com.ardeno.clearscan.data.LocalDocumentRepository
import com.ardeno.clearscan.model.PageAnnotation
import com.ardeno.clearscan.model.ScanDocument
import com.ardeno.clearscan.ocr.IdRedactionSuggester
import com.ardeno.clearscan.ocr.IdRedactionSuggestion
import com.ardeno.clearscan.pdf.PdfCompressQuality
import com.ardeno.clearscan.pdf.PdfToolEngine
import com.ardeno.clearscan.pdf.PdfToolOutput
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class PdfToolsProcessor(
    private val scope: CoroutineScope,
    private val repository: LocalDocumentRepository,
    private val pdfToolEngine: PdfToolEngine,
    private val getDocuments: () -> List<ScanDocument>,
    private val getSelectedIds: () -> Set<String>,
    private val getSignatureText: () -> String,
    private val getPdfPassword: () -> String,
    private val getCompressQuality: () -> PdfCompressQuality,
    private val getIdRedactionSuggestions: () -> Map<String, IdRedactionSuggestion>,
    private val onPdfToolRunningChanged: (Boolean) -> Unit,
    private val onDocumentsUpdated: (List<ScanDocument>, String?, String) -> Unit,
    private val onMessage: (String) -> Unit,
    private val onExitSelectionMode: () -> Unit
) {
    fun mergeSelectedDocuments() {
        val selectedDocuments = getDocuments().filter { it.id in getSelectedIds() }
        if (selectedDocuments.size < 2) {
            onMessage("Select at least two documents to merge.")
            return
        }
        runPdfTool("Merged ${selectedDocuments.size} selected scans.", selectedDocuments) { workingDir ->
            listOf(repository.createGeneratedDocument(pdfToolEngine.merge(selectedDocuments, workingDir), selectedDocuments))
        }
        onExitSelectionMode()
    }

    fun mergeAllDocuments() {
        val documents = getDocuments()
        if (documents.size < 2) {
            onMessage("Add at least two scans before merging.")
            return
        }
        runPdfTool("Merged ${documents.size} scans.", documents) { workingDir ->
            listOf(repository.createGeneratedDocument(pdfToolEngine.merge(documents, workingDir), documents))
        }
    }

    fun splitDocument(document: ScanDocument) {
        runPdfTool("Split ${document.title} into single-page PDFs.", listOf(document)) { workingDir ->
            pdfToolEngine.split(document, workingDir).map { repository.createGeneratedDocument(it, listOf(document)) }
        }
    }

    fun rotateDocument(document: ScanDocument) = runSingleDocumentTool(document, "Created rotated copy.") {
        pdfToolEngine.rotateClockwise(document, it)
    }

    fun signDocument(document: ScanDocument) = runSingleDocumentTool(document, "Created signed copy.") {
        pdfToolEngine.sign(document, getSignatureText().ifBlank { "ClearScan" }, it)
    }

    fun redactDocument(document: ScanDocument) = runSingleDocumentTool(document, "Created redacted copy.") {
        pdfToolEngine.redactHeader(document, it)
    }

    fun redactIdSensitiveFields(document: ScanDocument) {
        val suggestion = getIdRedactionSuggestions()[document.id] ?: IdRedactionSuggester.suggestFromText(document.ocrText)
        if (suggestion == null) {
            onMessage("No sensitive ID fields were detected to redact.")
            return
        }
        runSingleDocumentTool(document, "Created ID-redacted copy.") {
            pdfToolEngine.redactIdSensitiveFields(document, suggestion.regions, it)
        }
    }

    fun applyAnnotations(
        document: ScanDocument,
        annotationsByPage: Map<Int, List<PageAnnotation>>,
        onReplaceDocument: (ScanDocument) -> Unit
    ) {
        scope.launch {
            val pageAnnotations = List(document.pageCount) { annotationsByPage[it].orEmpty() }
            val updatedSource = repository.updatePageAnnotations(document.id, pageAnnotations)
            updatedSource?.let { onReplaceDocument(it) }
            runSingleDocumentTool(updatedSource ?: document, "Created annotated copy.") {
                pdfToolEngine.applyAnnotations(updatedSource ?: document, annotationsByPage, it)
            }
        }
    }

    fun passwordProtectDocument(document: ScanDocument) = runSingleDocumentTool(document, "Created password-protected PDF.") {
        pdfToolEngine.passwordProtect(document, getPdfPassword(), it)
    }

    fun reorderDocument(document: ScanDocument, pageOrder: List<Int>) = runSingleDocumentTool(document, "Created reordered copy.") {
        pdfToolEngine.reorderPages(document, pageOrder, it)
    }

    fun deletePagesFromDocument(document: ScanDocument, pageIndicesToKeep: List<Int>) =
        runSingleDocumentTool(document, "Created copy with selected pages removed.") {
            pdfToolEngine.deletePages(document, pageIndicesToKeep, it)
        }

    fun compressDocument(document: ScanDocument) {
        val quality = getCompressQuality()
        runSingleDocumentTool(document, "Created compressed copy (${quality.label.lowercase()}).") {
            pdfToolEngine.compress(document, quality, it)
        }
    }

    fun runSingleDocumentTool(document: ScanDocument, successMessage: String, output: suspend (File) -> PdfToolOutput) {
        runPdfTool(successMessage, listOf(document)) { workingDir ->
            listOf(repository.createGeneratedDocument(output(workingDir), listOf(document)))
        }
    }

    private fun runPdfTool(successMessage: String, sourceDocuments: List<ScanDocument>, block: suspend (File) -> List<ScanDocument>) {
        scope.launch {
            onPdfToolRunningChanged(true)
            val workingDir = repository.newWorkingDirectory("pdf-tool")
            runCatching { block(workingDir) }
                .onSuccess { generatedDocuments ->
                    workingDir.deleteRecursively()
                    onDocumentsUpdated(generatedDocuments + getDocuments(), generatedDocuments.firstOrNull()?.id, successMessage)
                    onPdfToolRunningChanged(false)
                }
                .onFailure { error ->
                    workingDir.deleteRecursively()
                    onPdfToolRunningChanged(false)
                    onMessage(error.localizedMessage ?: "PDF tool failed.")
                }
        }
    }
}
