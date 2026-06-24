package com.ardeno.clearscan.scanner

import android.net.Uri
import com.ardeno.clearscan.model.ScanMode

data class ScannerImport(
    val pdfUri: Uri?,
    val pageUris: List<Uri>,
    val scanMode: ScanMode = ScanMode.Document,
    val tags: List<String> = emptyList(),
    val enhanceImages: Boolean = true
)
