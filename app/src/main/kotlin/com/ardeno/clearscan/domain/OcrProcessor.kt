package com.ardeno.clearscan.domain

import com.ardeno.clearscan.data.LocalDocumentRepository
import com.ardeno.clearscan.intelligence.DocumentTagger
import com.ardeno.clearscan.intelligence.ReceiptFieldExtractor
import com.ardeno.clearscan.model.OcrStatus
import com.ardeno.clearscan.model.ReceiptFields
import com.ardeno.clearscan.model.ScanDocument
import com.ardeno.clearscan.model.ScanMode
import com.ardeno.clearscan.ocr.DocumentOcrResult
import com.ardeno.clearscan.ocr.IdRedactionSuggester
import com.ardeno.clearscan.ocr.IdRedactionSuggestion
import com.ardeno.clearscan.ocr.OcrEngine
import com.ardeno.clearscan.ocr.OcrLanguage
import com.ardeno.clearscan.pdf.SearchablePdfWriter
import com.ardeno.clearscan.ui.UiStrings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class OcrProcessor(
    private val scope: CoroutineScope,
    private val repository: LocalDocumentRepository,
    private val ocrEngine: OcrEngine,
    private val searchablePdfWriter: SearchablePdfWriter,
    private val uiStrings: UiStrings,
    private val onReplaceDocument: (ScanDocument) -> Unit,
    private val onOcrRunningChanged: (Boolean) -> Unit,
    private val onMessage: (String) -> Unit,
    private val onIdRedactionSuggestion: (String, IdRedactionSuggestion) -> Unit
) {
    private var activeOcrJobs = 0

    fun close() {
        ocrEngine.close()
    }

    fun setDocumentOcrLanguage(document: ScanDocument, language: OcrLanguage) {
        if (document.ocrLanguage == language) return
        scope.launch {
            val updated = repository.updateOcrLanguage(document.id, language)
            if (updated != null) {
                onReplaceDocument(updated)
                runOcr(updated.copy(ocrStatus = OcrStatus.Queued))
            }
        }
    }

    fun runOcr(document: ScanDocument) {
        scope.launch {
            incrementOcrJobs()
            repository.markOcrProcessing(document.id)?.let { onReplaceDocument(it) }
            runCatching {
                val result = ocrEngine.recognize(document, document.ocrLanguage)
                val searchablePdf = searchablePdfWriter.write(
                    document = document,
                    ocrResult = result,
                    targetDir = repository.documentDirectory(document)
                )
                val suggestedTags = DocumentTagger.suggestTags(result.text)
                val receiptFields = ReceiptFieldExtractor.extract(result.text).takeIf { it.hasAnyField }
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
                ocrSuccess.updatedDocument?.let { onReplaceDocument(it) }
                val idSuggestion = if (document.scanMode == ScanMode.IdCard || document.tags.contains("id-card")) {
                    IdRedactionSuggester.suggest(ocrSuccess.result.pages)
                        ?: IdRedactionSuggester.suggestFromText(ocrSuccess.result.text)
                } else {
                    null
                }
                val intelligenceNote = buildList {
                    if (ocrSuccess.suggestedTags.isNotEmpty()) add(ocrSuccess.suggestedTags.joinToString())
                    ocrSuccess.receiptFields?.amount?.let { add(uiStrings.ocrAmountTag(it)) }
                }.takeIf { it.isNotEmpty() }?.joinToString(" · ")
                onMessage(
                    when {
                        idSuggestion != null -> uiStrings.ocrFinishedRedaction(document.title)
                        intelligenceNote != null -> uiStrings.ocrFinishedTags(document.title, intelligenceNote)
                        else -> uiStrings.ocrFinished(document.title)
                    }
                )
                if (idSuggestion != null) onIdRedactionSuggestion(document.id, idSuggestion)
            }.onFailure { error ->
                repository.markOcrFailed(document.id)?.let { onReplaceDocument(it) }
                onMessage(error.localizedMessage ?: uiStrings.ocrFailed(document.title))
            }
            decrementOcrJobs()
        }
    }

    private fun incrementOcrJobs() {
        activeOcrJobs += 1
        onOcrRunningChanged(true)
    }

    private fun decrementOcrJobs() {
        activeOcrJobs = (activeOcrJobs - 1).coerceAtLeast(0)
        onOcrRunningChanged(activeOcrJobs > 0)
    }
}

private data class OcrSuccess(
    val updatedDocument: ScanDocument?,
    val result: DocumentOcrResult,
    val suggestedTags: List<String>,
    val receiptFields: ReceiptFields?
)
