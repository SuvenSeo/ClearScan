package com.ardeno.clearscan.model

import com.ardeno.clearscan.ocr.OcrLanguage
import java.time.Instant

enum class OcrStatus {
    NotStarted,
    Queued,
    Processing,
    Ready,
    Failed
}

data class ScanDocument(
    val id: String,
    val title: String,
    val pageCount: Int,
    val createdAt: Instant,
    val updatedAt: Instant = createdAt,
    val pdfPath: String?,
    val searchablePdfPath: String? = null,
    val pageImagePaths: List<String>,
    val tags: List<String> = emptyList(),
    val receiptFields: ReceiptFields? = null,
    val folderId: String? = null,
    val isFavorite: Boolean = false,
    val pageHashes: List<String> = emptyList(),
    val toolName: String? = null,
    val sourceDocumentIds: List<String> = emptyList(),
    val ocrText: String = "",
    val ocrStatus: OcrStatus = OcrStatus.NotStarted,
    val searchablePdfReady: Boolean = false,
    val scanMode: ScanMode = ScanMode.Document,
    val ocrLanguage: OcrLanguage = OcrLanguage.Latin
)

fun ScanDocument.matchesQuery(query: String): Boolean {
    val trimmed = query.trim()
    if (trimmed.isBlank()) return true

    return title.contains(trimmed, ignoreCase = true) ||
        ocrText.contains(trimmed, ignoreCase = true) ||
        tags.any { it.contains(trimmed, ignoreCase = true) } ||
        listOfNotNull(
            receiptFields?.merchant,
            receiptFields?.amount,
            receiptFields?.date
        ).any { it.contains(trimmed, ignoreCase = true) }
}
