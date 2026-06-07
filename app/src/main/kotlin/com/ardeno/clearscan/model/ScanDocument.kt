package com.ardeno.clearscan.model

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
    val ocrText: String = "",
    val ocrStatus: OcrStatus = OcrStatus.NotStarted,
    val searchablePdfReady: Boolean = false
)

fun ScanDocument.matchesQuery(query: String): Boolean {
    val trimmed = query.trim()
    if (trimmed.isBlank()) return true

    return title.contains(trimmed, ignoreCase = true) ||
        ocrText.contains(trimmed, ignoreCase = true) ||
        tags.any { it.contains(trimmed, ignoreCase = true) }
}
