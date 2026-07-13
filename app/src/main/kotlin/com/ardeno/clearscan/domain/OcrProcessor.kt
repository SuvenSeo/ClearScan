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
import com.ardeno.clearscan.pdf.SearchablePdfWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class OcrProcessor(
    private val scope: CoroutineScope,
    private val repository: LocalDocumentRepository,
    private val ocrEngine: OcrEngine,
    private val searchablePdfWriter: SearchablePdfWriter,
    private val onReplaceDocument: (ScanDocument) -> Unit,
    private val onOcrRunningChanged: (Boolean) -> Unit,
    private val onMessage: (String) -> Unit,
    private val onIdRedactionSuggestion: (String, IdRedactionSuggestion) -> Unit
) {
    private var activeOcrJobs = 0

    fun close() {
        ocrEngine.close()
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
                    ocrSuccess.receiptFields?.amount?.let { add("amount $it") }
                }.takeIf { it.isNotEmpty() }?.joinToString(" · ")
                onMessage(
                    when {
                        idSuggestion != null ->
                            "OCR finished for ${document.title}. Sensitive fields detected — review redaction."
                        intelligenceNote != null ->
                            "OCR finished for ${document.title}. Tags: $intelligenceNote"
                        else -> "OCR finished for ${document.title}."
                    }
                )
                if (idSuggestion != null) onIdRedactionSuggestion(document.id, idSuggestion)
            }.onFailure { error ->
                repository.markOcrFailed(document.id)?.let { onReplaceDocument(it) }
                onMessage(error.localizedMessage ?: "OCR failed for ${document.title}.")
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
