package com.ardeno.clearscan.domain

import com.ardeno.clearscan.data.LocalDocumentRepository
import com.ardeno.clearscan.model.PageAnnotation
import com.ardeno.clearscan.model.ScanDocument
import com.ardeno.clearscan.ocr.IdRedactionSuggester
import com.ardeno.clearscan.ocr.IdRedactionSuggestion
import com.ardeno.clearscan.pdf.PdfCompressQuality
import com.ardeno.clearscan.pdf.PdfToolEngine
import com.ardeno.clearscan.pdf.PdfToolOutput
import com.ardeno.clearscan.ui.UiStrings
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class PdfToolsProcessor(
    private val scope: CoroutineScope,
    private val repository: LocalDocumentRepository,
    private val pdfToolEngine: PdfToolEngine,
    private val uiStrings: UiStrings,
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
            onMessage(uiStrings.mergeSelectTwo())
            return
        }
        runPdfTool(uiStrings.mergedSelected(selectedDocuments.size), selectedDocuments) { workingDir ->
            listOf(repository.createGeneratedDocument(pdfToolEngine.merge(selectedDocuments, workingDir), selectedDocuments))
        }
        onExitSelectionMode()
    }

    fun mergeAllDocuments() {
        val documents = getDocuments()
        if (documents.size < 2) {
            onMessage(uiStrings.mergeNeedTwo())
            return
        }
        runPdfTool(uiStrings.mergedScans(documents.size), documents) { workingDir ->
            listOf(repository.createGeneratedDocument(pdfToolEngine.merge(documents, workingDir), documents))
        }
    }

    fun splitDocument(document: ScanDocument) {
        runPdfTool(uiStrings.splitDocument(document.title), listOf(document)) { workingDir ->
            pdfToolEngine.split(document, workingDir).map { repository.createGeneratedDocument(it, listOf(document)) }
        }
    }

    fun rotateDocument(document: ScanDocument) = runSingleDocumentTool(document, uiStrings.createdRotatedCopy()) {
        pdfToolEngine.rotateClockwise(document, it)
    }

    fun signDocument(document: ScanDocument) = runSingleDocumentTool(document, uiStrings.createdSignedCopy()) {
        pdfToolEngine.sign(document, getSignatureText().ifBlank { "ClearScan" }, it)
    }

    fun redactDocument(document: ScanDocument) = runSingleDocumentTool(document, uiStrings.createdRedactedCopy()) {
        pdfToolEngine.redactHeader(document, it)
    }

    fun redactIdSensitiveFields(document: ScanDocument) {
        val suggestion = getIdRedactionSuggestions()[document.id] ?: IdRedactionSuggester.suggestFromText(document.ocrText)
        if (suggestion == null) {
            onMessage(uiStrings.noIdFieldsToRedact())
            return
        }
        runSingleDocumentTool(document, uiStrings.createdIdRedactedCopy()) {
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
            runSingleDocumentTool(updatedSource ?: document, uiStrings.createdAnnotatedCopy()) {
                pdfToolEngine.applyAnnotations(updatedSource ?: document, annotationsByPage, it)
            }
        }
    }

    fun passwordProtectDocument(document: ScanDocument) = runSingleDocumentTool(document, uiStrings.createdPasswordProtected()) {
        pdfToolEngine.passwordProtect(document, getPdfPassword(), it)
    }

    fun reorderDocument(document: ScanDocument, pageOrder: List<Int>) = runSingleDocumentTool(document, uiStrings.createdReorderedCopy()) {
        pdfToolEngine.reorderPages(document, pageOrder, it)
    }

    fun deletePagesFromDocument(document: ScanDocument, pageIndicesToKeep: List<Int>) =
        runSingleDocumentTool(document, uiStrings.createdPagesRemovedCopy()) {
            pdfToolEngine.deletePages(document, pageIndicesToKeep, it)
        }

    fun compressDocument(document: ScanDocument) {
        val quality = getCompressQuality()
        runSingleDocumentTool(document, uiStrings.createdCompressedCopy(quality)) {
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
                    onMessage(error.localizedMessage ?: uiStrings.pdfToolFailed())
                }
        }
    }
}
